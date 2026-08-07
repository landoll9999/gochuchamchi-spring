package com.gochuchamchi.config;

import com.gochuchamchi.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuditLogService auditLogService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        if (authentication != null) {
            auditLogService.successForUsername("LOGOUT", authentication.getName(),
                    "USER", null, null);
        }
        SimpleUrlAuthenticationSuccessHandler redirect = new SimpleUrlAuthenticationSuccessHandler("/");
        redirect.onAuthenticationSuccess(request, response, authentication);
    }
}
