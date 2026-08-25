package com.gochuchamchi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("web")
public class HomeController {
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
