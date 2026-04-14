package com.fintrack.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fintrack.dto.GroupCreateDto;
import com.fintrack.model.Group;
import com.fintrack.model.GroupMember;
import com.fintrack.model.User;
import com.fintrack.service.GroupService;
import com.fintrack.service.UserService;
import com.fintrack.service.impl.DebtSimplificationService;

import jakarta.validation.Valid;

/**
 * GroupController — Owner: Saanvi Kakkar
 */
@Controller
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final DebtSimplificationService debtEngine;

    public GroupController(GroupService groupService,
                            UserService userService,
                            DebtSimplificationService debtEngine) {
        this.groupService = groupService;
        this.userService  = userService;
        this.debtEngine   = debtEngine;
    }

    @GetMapping
    public String listGroups(Model model) {
        User current = userService.getCurrentUser();
        model.addAttribute("groups", groupService.findGroupsForUser(current.getId()));
        model.addAttribute("user", current);
        return "group/list";
    }

    @GetMapping("/new")
    public String newGroupForm(Model model) {
        model.addAttribute("groupForm", new GroupCreateDto());
        return "group/new";
    }

    @PostMapping("/new")
    public String createGroup(@Valid @ModelAttribute("groupForm") GroupCreateDto dto,
                              BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return "group/new";
        User current = userService.getCurrentUser();
        Group group = groupService.createGroup(dto.getName(), dto.getDescription(),
                dto.getCurrency(), current.getId());
        ra.addFlashAttribute("successMsg", "Group '" + group.getName() + "' created!");
        return "redirect:/groups/" + group.getId();
    }

    @GetMapping("/{id}")
    public String groupDetail(@PathVariable Long id, Model model) {
        User current = userService.getCurrentUser();
        Group group = groupService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        if (!groupService.isMember(id, current.getId())) return "redirect:/groups?error=access";

        List<GroupMember> members       = groupService.getMembers(id);
        List<DebtSimplificationService.SimplifiedDebt> debts = debtEngine.computeSimplifiedDebts(id);
        Map<Long, BigDecimal> netBalances = debtEngine.computeNetBalances(id);

        model.addAttribute("group", group);
        model.addAttribute("members", members);
        model.addAttribute("debts", debts);
        model.addAttribute("netBalances", netBalances);
        model.addAttribute("currentUser", current);
        model.addAttribute("allUsers", userService.findAllActive());
        return "group/detail";
    }

    @PostMapping("/{id}/add-member")
    public String addMember(@PathVariable Long id, @RequestParam Long userId, RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        try {
            groupService.addMember(id, userId, current.getId());
            ra.addFlashAttribute("successMsg", "Member added successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/groups/" + id;
    }

    @PostMapping("/{id}/remove-member")
    public String removeMember(@PathVariable Long id, @RequestParam Long userId, RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        groupService.removeMember(id, userId, current.getId());
        ra.addFlashAttribute("successMsg", "Member removed.");
        return "redirect:/groups/" + id;
    }
    @PostMapping("/{id}/delete")
    public String deleteGroup(@PathVariable Long id, RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        try {
            groupService.deleteGroup(id, current.getId());
            ra.addFlashAttribute("successMsg", "Group deleted successfully.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/groups/" + id;
        }
        return "redirect:/groups";
    }
}
