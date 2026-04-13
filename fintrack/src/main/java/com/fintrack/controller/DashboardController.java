package com.fintrack.controller;

import com.fintrack.model.User;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.service.GroupService;
import com.fintrack.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * DashboardController — Owner: Saanvi Kakkar
 */
@Controller
public class DashboardController {

    private final UserService userService;
    private final GroupService groupService;
    private final NotificationRepository notificationRepository;

    public DashboardController(UserService userService,
                                GroupService groupService,
                                NotificationRepository notificationRepository) {
        this.userService             = userService;
        this.groupService            = groupService;
        this.notificationRepository  = notificationRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User current = userService.getCurrentUser();
        model.addAttribute("user", current);
        model.addAttribute("groups", groupService.findGroupsForUser(current.getId()));
        model.addAttribute("unreadCount",
                notificationRepository.countByUserIdAndIsReadFalse(current.getId()));
        return "dashboard/home";
    }
}
