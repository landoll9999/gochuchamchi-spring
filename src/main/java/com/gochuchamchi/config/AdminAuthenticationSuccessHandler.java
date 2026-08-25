package com.gochuchamchi.config;

import com.gochuchamchi.service.AuditLogService;
import com.gochuchamchi.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("admin")
@RequiredArgsConstructor
public class AdminAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        loginAttemptService.reset(authentication.getName());
        auditLogService.successForUsername("ADMIN_LOGIN", authentication.getName(),
                "USER", null, null);
        setDefaultTargetUrl("/admin");
        setAlwaysUseDefaultTargetUrl(true);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
