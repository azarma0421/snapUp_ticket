package com.example.snapUp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ButtonController {
    
    @GetMapping("/")
    public String home() {
        return "buttons";
    }
    
} 