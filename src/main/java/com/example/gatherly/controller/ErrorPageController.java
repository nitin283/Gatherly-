package com.example.gatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Maps the security access-denied path to the friendly 403 page. */
@Controller
public class ErrorPageController {

    @GetMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }
}
