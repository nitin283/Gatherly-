package com.example.gatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.gatherly.dto.RegisterRequest;
import com.example.gatherly.model.Role;
import com.example.gatherly.service.BusinessRuleException;
import com.example.gatherly.service.UserService;

import jakarta.validation.Valid;

/** Handles registration pages and supplies the custom login page. */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                            BindingResult bindingResult,
                            Model model) {
        if (bindingResult.hasErrors()) {
            return "register"; // show the form again with field error messages
        }

        try {
            Role role = Role.valueOf(request.getRole());
            userService.registerAttendeeOrOrganizer(
                    request.getFullName(), request.getEmail(), request.getPassword(), role);
        } catch (BusinessRuleException | IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }

        return "redirect:/login?registered";
    }
}
