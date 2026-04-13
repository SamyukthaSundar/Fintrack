package com.fintrack.controller;

import com.fintrack.model.Group;
import com.fintrack.service.GroupService;
import com.fintrack.service.UserService;
import com.fintrack.service.impl.AnalyticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.Map;

/**
 * AnalyticsController — Owner: Sanika Gupta
 */
@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final GroupService groupService;
    private final UserService userService;

    public AnalyticsController(AnalyticsService analyticsService,
                                GroupService groupService,
                                UserService userService) {
        this.analyticsService = analyticsService;
        this.groupService     = groupService;
        this.userService      = userService;
    }

    @GetMapping("/group/{groupId}")
    public String analyticsPage(@PathVariable Long groupId, Model model) {
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        int currentYear = Year.now().getValue();
        model.addAttribute("group", group);
        model.addAttribute("currentUser", userService.getCurrentUser());
        model.addAttribute("monthlyData", analyticsService.getMonthlyTotals(groupId, currentYear));
        model.addAttribute("categoryData", analyticsService.getCategoryBreakdown(groupId));
        model.addAttribute("settlementStats", analyticsService.getSettlementStats(groupId));
        model.addAttribute("year", currentYear);
        return "analytics/dashboard";
    }

    @GetMapping("/api/group/{groupId}/monthly")
    @ResponseBody
    public Map<String, Object> monthlyChart(@PathVariable Long groupId,
                                             @RequestParam(defaultValue = "0") int year) {
        int y = (year == 0) ? Year.now().getValue() : year;
        return Map.of("data", analyticsService.getMonthlyTotals(groupId, y), "year", y);
    }

    @GetMapping("/api/group/{groupId}/categories")
    @ResponseBody
    public Map<String, Object> categoryChart(@PathVariable Long groupId) {
        return Map.of("data", analyticsService.getCategoryBreakdown(groupId));
    }

    @GetMapping("/group/{groupId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long groupId) {
        Group group = groupService.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        try {
            byte[] pdf = analyticsService.generateGroupStatement(groupId, group.getName());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"fintrack-statement-" + groupId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
