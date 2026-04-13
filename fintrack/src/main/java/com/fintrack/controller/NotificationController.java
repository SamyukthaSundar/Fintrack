package com.fintrack.controller;

import com.fintrack.model.User;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * NotificationController — Owner: Saanvi Kakkar / Samyuktha S
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationController(NotificationRepository notificationRepository,
                                   UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService            = userService;
    }

    @GetMapping
    public String notifications(Model model) {
        User current = userService.getCurrentUser();
        model.addAttribute("notifications",
                notificationRepository.findByUserIdOrderByCreatedAtDesc(current.getId()));
        model.addAttribute("user", current);
        return "dashboard/notifications";
    }

    @PostMapping("/mark-all-read")
    public String markAllRead() {
        User current = userService.getCurrentUser();
        notificationRepository.markAllReadByUserId(current.getId());
        return "redirect:/notifications";
    }

    @GetMapping("/api/unread-count")
    @ResponseBody
    public long unreadCount() {
        User current = userService.getCurrentUser();
        return notificationRepository.countByUserIdAndIsReadFalse(current.getId());
    }
}
