package com.example.gatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the public landing page. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "Gatherly");
        model.addAttribute("tagline", "Discover, create and book events, all in one place.");
        return "index";
    }
}
