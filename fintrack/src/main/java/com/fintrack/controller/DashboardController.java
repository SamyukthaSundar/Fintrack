package com.fintrack.controller;

import com.fintrack.model.Group;
import com.fintrack.model.User;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.service.BalanceCalculationService;
import com.fintrack.service.GroupService;
import com.fintrack.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardController — Owner: Saanvi Kakkar
 * Updated: Now uses BalanceCalculationService for consistent balance calculations
 */
@Controller
public class DashboardController {

    private final UserService userService;
    private final GroupService groupService;
    private final NotificationRepository notificationRepository;
    private final BalanceCalculationService balanceCalculationService;

    public DashboardController(UserService userService,
                                GroupService groupService,
                                NotificationRepository notificationRepository,
                                BalanceCalculationService balanceCalculationService) {
        this.userService = userService;
        this.groupService = groupService;
        this.notificationRepository = notificationRepository;
        this.balanceCalculationService = balanceCalculationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User current = userService.getCurrentUser();
        List<Group> groups = groupService.findGroupsForUser(current.getId());
        
        // Use centralized BalanceCalculationService for ALL calculations
        // This ensures consistency with Settlement page and proper accounting for partial settlements
        
        // Get outstanding debts using the centralized service
        List<BalanceCalculationService.OutstandingDebt> outstandingDebts = 
            balanceCalculationService.getOutstandingDebtsForUser(current.getId());
        
        // Convert to the format expected by the template
        List<Map<String, Object>> expenseLevelDebts = new ArrayList<>();
        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalOwedToMe = BigDecimal.ZERO;
        
        for (BalanceCalculationService.OutstandingDebt debt : outstandingDebts) {
            Map<String, Object> debtInfo = new HashMap<>();
            debtInfo.put("group", debt.group());
            debtInfo.put("expense", debt.split().getExpense());
            debtInfo.put("expenseTitle", debt.expenseTitle());
            debtInfo.put("payee", debt.type().equals("OWE") ? debt.otherPerson() : null);
            debtInfo.put("payer", debt.type().equals("OWED") ? debt.otherPerson() : null);
            debtInfo.put("amount", debt.remainingAmount());  // Use remainingAmount, not original amount
            debtInfo.put("originalAmount", debt.originalAmount());
            debtInfo.put("settledAmount", debt.settledAmount());
            debtInfo.put("type", debt.type());
            debtInfo.put("expenseDate", debt.split().getExpense().getExpenseDate());
            expenseLevelDebts.add(debtInfo);
            
            if (debt.type().equals("OWE")) {
                totalOwed = totalOwed.add(debt.remainingAmount());
            } else {
                totalOwedToMe = totalOwedToMe.add(debt.remainingAmount());
            }
        }
        
        // Also get totals directly from service for consistency
        BigDecimal serviceTotalOwed = balanceCalculationService.getTotalOwedByUser(current.getId());
        BigDecimal serviceTotalOwedToMe = balanceCalculationService.getTotalOwedToUser(current.getId());
        
        model.addAttribute("user", current);
        model.addAttribute("groups", groups);
        model.addAttribute("unreadCount",
                notificationRepository.countByUserIdAndIsReadFalse(current.getId()));
        model.addAttribute("expenseLevelDebts", expenseLevelDebts);
        model.addAttribute("totalOwed", serviceTotalOwed);
        model.addAttribute("totalOwedToMe", serviceTotalOwedToMe);
        model.addAttribute("netBalance", serviceTotalOwedToMe.subtract(serviceTotalOwed));
        return "dashboard/home";
    }
}
