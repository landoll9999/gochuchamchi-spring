package com.gochuchamchi.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class LogRequestContext {

    private LogRequestContext() {
    }

    public static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    public static String method(HttpServletRequest request) {
        return request == null ? null : sanitize(request.getMethod(), 10);
    }

    public static String path(HttpServletRequest request) {
        return request == null ? null : sanitize(request.getRequestURI(), 255);
    }

    public static String remoteAddress(HttpServletRequest request) {
        return request == null ? null : sanitize(request.getRemoteAddr(), 45);
    }

    public static String userAgent(HttpServletRequest request) {
        return request == null ? null : sanitize(request.getHeader("User-Agent"), 500);
    }

    public static String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
