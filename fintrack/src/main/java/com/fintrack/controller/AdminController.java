package com.fintrack.controller;

import com.fintrack.service.UserService;
import com.fintrack.service.impl.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * AdminController — Owner: Saanvi Kakkar
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuditService auditService;
    private final UserService userService;

    public AdminController(AuditService auditService, UserService userService) {
        this.auditService = auditService;
        this.userService  = userService;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("recentLogs",    auditService.getRecentLogs(0, 20));
        model.addAttribute("actionsToday",
                auditService.countSince(LocalDateTime.now().withHour(0).withMinute(0)));
        model.addAttribute("allUsers",  userService.findAllActive());
        model.addAttribute("totalUsers", userService.findAllActive().size());
        return "admin/dashboard";
    }

    @GetMapping("/audit")
    public String auditLogs(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("logs", auditService.getRecentLogs(page, 50));
        model.addAttribute("currentPage", page);
        return "admin/audit";
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.deactivateUser(id);
        ra.addFlashAttribute("successMsg", "User deactivated.");
        return "redirect:/admin";
    }
}
