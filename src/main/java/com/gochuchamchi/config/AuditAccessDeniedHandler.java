package com.gochuchamchi.config;

import com.gochuchamchi.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditLogService auditLogService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        auditLogService.failureForUsername("ACCESS_DENIED", username,
                null, null, "INSUFFICIENT_PRIVILEGES", null);

        AccessDeniedHandlerImpl delegate = new AccessDeniedHandlerImpl();
        delegate.setErrorPage("/error/403");
        delegate.handle(request, response, accessDeniedException);
    }
}
