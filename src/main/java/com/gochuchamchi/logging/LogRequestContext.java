package com.gochuchamchi.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class LogRequestContext {

    public static final String REQUEST_ID_ATTRIBUTE = LogRequestContext.class.getName() + ".requestId";

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
        return clientAddress(request);
    }

    public static String clientAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] addresses = forwardedFor.split(",");
            // CloudFront appends the viewer IP and ALB appends the CloudFront edge IP.
            // The second value from the right ignores viewer-supplied spoofed prefixes.
            int index = addresses.length >= 2 ? addresses.length - 2 : 0;
            return sanitize(addresses[index], 45);
        }
        return sanitize(request.getRemoteAddr(), 45);
    }

    public static String requestId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        return value == null ? null : sanitize(value.toString(), 64);
    }

    public static String cloudFrontRequestId(HttpServletRequest request) {
        return request == null ? null : sanitize(request.getHeader("X-Amz-Cf-Id"), 128);
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
