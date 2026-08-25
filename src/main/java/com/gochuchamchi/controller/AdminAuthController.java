package com.gochuchamchi.controller;

import com.gochuchamchi.config.LoginFailureHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("admin")
public class AdminAuthController {

    @GetMapping("/auth/login")
    public String loginPage(HttpSession session, Model model) {
        Object suspended = session.getAttribute(LoginFailureHandler.SUSPENDED_MESSAGE);
        if (suspended != null) {
            model.addAttribute("suspendedMessage", suspended);
            session.removeAttribute(LoginFailureHandler.SUSPENDED_MESSAGE);
        }
        Object throttled = session.getAttribute(LoginFailureHandler.THROTTLED_MESSAGE);
        if (throttled != null) {
            model.addAttribute("throttledMessage", throttled);
            session.removeAttribute(LoginFailureHandler.THROTTLED_MESSAGE);
        }
        return "admin/login";
    }
}
