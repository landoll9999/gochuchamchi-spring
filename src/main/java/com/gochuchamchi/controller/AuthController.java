package com.gochuchamchi.controller;

import com.gochuchamchi.config.LoginFailureHandler;
import com.gochuchamchi.dto.RegisterForm;
import com.gochuchamchi.service.UserService;
import com.gochuchamchi.service.AuditLogService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("web")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        // 정지된 계정이 로그인을 시도하면 LoginFailureHandler 가 안내 문구를 세션에 담아둔다.
        // 한 번 보여준 뒤에는 지워서 새로고침 때 다시 뜨지 않게 한다.
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
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm()); // 템플릿 ${form}과 일치
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("form", form);
            return "auth/register";
        }

        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("success", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            auditLogService.failure("USER_REGISTERED", null, form.getUsername(),
                    "USER", null, "VALIDATION_FAILED", null);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            return "auth/register";
        }
    }
}
