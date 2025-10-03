package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ResetPageController {
    @GetMapping("/reset")
    public String resetPage() {
        // Forward to reset.html in static folder
        return "forward:/reset.html";
    }
}