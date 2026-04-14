package com.fintrack.controller;

import com.fintrack.dto.ExpenseCreateDto;
import com.fintrack.model.*;
import com.fintrack.service.ExpenseService;
import com.fintrack.service.GroupService;
import com.fintrack.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExpenseController — Owner: Samyuktha S
 */
@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final GroupService groupService;
    private final UserService userService;

    public ExpenseController(ExpenseService expenseService,
                              GroupService groupService,
                              UserService userService) {
        this.expenseService = expenseService;
        this.groupService   = groupService;
        this.userService    = userService;
    }

    @GetMapping("/group/{groupId}")
    public String listExpenses(@PathVariable Long groupId, Model model) {
        User current = userService.getCurrentUser();
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        model.addAttribute("group", group);
        model.addAttribute("expenses", expenseService.findByGroupId(groupId));
        model.addAttribute("members", groupService.getMembers(groupId));
        model.addAttribute("currentUser", current);
        model.addAttribute("splitTypes", Expense.SplitType.values());
        model.addAttribute("categories", Expense.Category.values());
        return "expense/list";
    }

    @GetMapping("/group/{groupId}/new")
    public String newExpenseForm(@PathVariable Long groupId, Model model) {
        User current = userService.getCurrentUser();
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        model.addAttribute("group", group);
        model.addAttribute("members", groupService.getMembers(groupId));
        model.addAttribute("currentUser", current);
        model.addAttribute("splitTypes", Expense.SplitType.values());
        model.addAttribute("categories", Expense.Category.values());
        return "expense/new";
    }

    @PostMapping("/group/{groupId}/new")
    public String createExpense(@PathVariable Long groupId,
                                @RequestParam String title,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal totalAmount,
                                @RequestParam String splitType,
                                @RequestParam String category,
                                @RequestParam(required = false) String expenseDate,
                                @RequestParam Long paidById,
                                @RequestParam(required = false) List<Long> participantIds,
                                @RequestParam(required = false) MultipartFile receiptFile,
                                HttpServletRequest request,
                                RedirectAttributes ra) {
        User current = userService.getCurrentUser();

        // Parse splitData manually from request params: splitData[userId] = value
        Map<Long, BigDecimal> splitData = new HashMap<>();
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("splitData[") && key.endsWith("]")) {
                try {
                    Long userId = Long.parseLong(key.substring(10, key.length() - 1));
                    String val = entry.getValue()[0];
                    if (val != null && !val.trim().isEmpty()) {
                        splitData.put(userId, new BigDecimal(val.trim()));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        ExpenseCreateDto dto = new ExpenseCreateDto();
        dto.setGroupId(groupId);
        dto.setPaidById(paidById);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setTotalAmount(totalAmount);
        dto.setSplitType(splitType);
        dto.setCategory(category);
        dto.setExpenseDate(expenseDate != null && !expenseDate.isEmpty()
                ? LocalDate.parse(expenseDate) : LocalDate.now());
        dto.setParticipantIds(participantIds);
        dto.setSplitData(splitData.isEmpty() ? null : splitData);

        try {
            Expense expense = (receiptFile != null && !receiptFile.isEmpty())
                    ? expenseService.createExpenseWithReceipt(dto, receiptFile, current.getId())
                    : expenseService.createExpense(dto, current.getId());
            ra.addFlashAttribute("successMsg", "Expense '" + expense.getTitle() + "' added!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Error: " + e.getMessage());
        }
        return "redirect:/expenses/group/" + groupId;
    }

    @GetMapping("/{expenseId}/edit")
    public String editExpenseForm(@PathVariable Long expenseId, Model model) {
        User current = userService.getCurrentUser();
        Expense expense = expenseService.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found."));
        
        // Only owner can edit
        if (!expense.getPaidBy().getId().equals(current.getId())) {
            throw new IllegalArgumentException("Only the expense owner can edit this expense.");
        }
        
        Group group = expense.getGroup();
        model.addAttribute("expense", expense);
        model.addAttribute("group", group);
        model.addAttribute("members", groupService.getMembers(group.getId()));
        model.addAttribute("currentUser", current);
        model.addAttribute("splitTypes", Expense.SplitType.values());
        model.addAttribute("categories", Expense.Category.values());
        return "expense/edit";
    }

    @PostMapping("/{expenseId}/edit")
    public String updateExpense(@PathVariable Long expenseId,
                                @RequestParam Long groupId,
                                @RequestParam String title,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal totalAmount,
                                @RequestParam String splitType,
                                @RequestParam String category,
                                @RequestParam(required = false) String expenseDate,
                                @RequestParam Long paidById,
                                @RequestParam(required = false) List<Long> participantIds,
                                HttpServletRequest request,
                                RedirectAttributes ra) {
        User current = userService.getCurrentUser();

        // Parse splitData manually from request params: splitData[userId] = value
        Map<Long, BigDecimal> splitData = new HashMap<>();
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("splitData[") && key.endsWith("]")) {
                try {
                    Long userId = Long.parseLong(key.substring(10, key.length() - 1));
                    String val = entry.getValue()[0];
                    if (val != null && !val.trim().isEmpty()) {
                        splitData.put(userId, new BigDecimal(val.trim()));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        ExpenseCreateDto dto = new ExpenseCreateDto();
        dto.setGroupId(groupId);
        dto.setPaidById(paidById);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setTotalAmount(totalAmount);
        dto.setSplitType(splitType);
        dto.setCategory(category);
        dto.setExpenseDate(expenseDate != null && !expenseDate.isEmpty()
                ? LocalDate.parse(expenseDate) : LocalDate.now());
        dto.setParticipantIds(participantIds);
        dto.setSplitData(splitData.isEmpty() ? null : splitData);

        try {
            Expense expense = expenseService.updateExpense(expenseId, dto, current.getId());
            ra.addFlashAttribute("successMsg", "Expense '" + expense.getTitle() + "' updated!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Error: " + e.getMessage());
        }
        return "redirect:/expenses/group/" + groupId;
    }

    @PostMapping("/{expenseId}/delete")
    public String deleteExpense(@PathVariable Long expenseId,
                                @RequestParam Long groupId,
                                RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        expenseService.deleteExpense(expenseId, current.getId());
        ra.addFlashAttribute("successMsg", "Expense deleted.");
        return "redirect:/expenses/group/" + groupId;
    }

    @PostMapping("/ocr-scan")
    @ResponseBody
    public Map<String, String> ocrScan(@RequestParam MultipartFile receiptFile) {
        String text = expenseService.extractTextFromReceipt(receiptFile);
        String amount = "";
        Matcher m = Pattern.compile("(?i)total[:\\s]+(?:inr|rs\\.?|₹)?\\s*([\\d,]+\\.?\\d{0,2})")
                           .matcher(text);
        if (m.find()) amount = m.group(1).replace(",", "");
        return Map.of("rawText", text, "detectedAmount", amount);
    }
}
