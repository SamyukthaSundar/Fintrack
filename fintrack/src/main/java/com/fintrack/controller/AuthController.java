package com.fintrack.controller;

import com.fintrack.dto.UserRegistrationDto;
import com.fintrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController — Owner: Saanvi Kakkar
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error  != null) model.addAttribute("errorMsg",   "Invalid username or password.");
        if (logout != null) model.addAttribute("successMsg", "You have been logged out.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") UserRegistrationDto dto,
                           BindingResult result,
                           RedirectAttributes ra) {
        if (result.hasErrors()) return "auth/register";
        if (userService.existsByUsername(dto.getUsername())) {
            result.rejectValue("username", "duplicate", "Username already taken.");
            return "auth/register";
        }
        if (userService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "duplicate", "Email already registered.");
            return "auth/register";
        }
        userService.register(dto);
        ra.addFlashAttribute("successMsg", "Registration successful! Please log in.");
        return "redirect:/auth/login";
    }
}
