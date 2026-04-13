package com.fintrack.controller;

import com.fintrack.model.*;
import com.fintrack.model.Settlement.PaymentMethod;
import com.fintrack.service.GroupService;
import com.fintrack.service.SettlementService;
import com.fintrack.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * SettlementController — Owner: Sanika Gupta
 */
@Controller
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final GroupService groupService;
    private final UserService userService;

    public SettlementController(SettlementService settlementService,
                                 GroupService groupService,
                                 UserService userService) {
        this.settlementService = settlementService;
        this.groupService      = groupService;
        this.userService       = userService;
    }

    @GetMapping("/group/{groupId}")
    public String listSettlements(@PathVariable Long groupId, Model model) {
        User current = userService.getCurrentUser();
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        List<Settlement> settlements         = settlementService.findByGroupId(groupId);
        List<Settlement> pendingVerification = settlementService.findPendingForPayee(current.getId());

        model.addAttribute("group", group);
        model.addAttribute("settlements", settlements);
        model.addAttribute("pendingVerification", pendingVerification);
        model.addAttribute("members", groupService.getMembers(groupId));
        model.addAttribute("currentUser", current);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "settlement/list";
    }

    @PostMapping("/initiate")
    public String initiate(@RequestParam Long groupId,
                           @RequestParam Long payeeId,
                           @RequestParam BigDecimal amount,
                           RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        try {
            settlementService.initiate(groupId, current.getId(), payeeId, amount, current.getId());
            ra.addFlashAttribute("successMsg", "Settlement initiated!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/settlements/group/" + groupId;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id,
                         @RequestParam String paymentRef,
                         @RequestParam String paymentMethod,
                         @RequestParam(required = false) String notes,
                         @RequestParam Long groupId,
                         RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        try {
            settlementService.submit(id, paymentRef, PaymentMethod.valueOf(paymentMethod),
                    notes, current.getId());
            ra.addFlashAttribute("successMsg", "Payment proof submitted. Awaiting verification.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/settlements/group/" + groupId;
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable Long id,
                         @RequestParam Long groupId,
                         RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        try {
            settlementService.verify(id, current.getId());
            ra.addFlashAttribute("successMsg", "Payment verified! Ledger updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/settlements/group/" + groupId;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason,
                         @RequestParam Long groupId,
                         RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        try {
            settlementService.reject(id, reason, current.getId());
            ra.addFlashAttribute("errorMsg", "Payment rejected. Payer notified.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/settlements/group/" + groupId;
    }
}
