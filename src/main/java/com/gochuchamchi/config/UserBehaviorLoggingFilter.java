package com.gochuchamchi.config;

import com.gochuchamchi.logging.UserBehaviorRecorder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserBehaviorLoggingFilter extends OncePerRequestFilter {

    static final String ANONYMOUS_COOKIE = "gc_anonymous_id";
    static final String SESSION_COOKIE = "gc_behavior_session_id";

    private static final Duration ANONYMOUS_MAX_AGE = Duration.ofDays(365);
    private static final Duration SESSION_MAX_AGE = Duration.ofMinutes(30);
    private static final Pattern PRODUCT_DETAIL = Pattern.compile("^/shop/(\\d+)$");
    private static final Pattern NOTICE_DETAIL = Pattern.compile("^/notice/(\\d+)$");

    private final UserBehaviorRecorder behaviorLogService;

    public UserBehaviorLoggingFilter(UserBehaviorRecorder behaviorLogService) {
        this.behaviorLogService = behaviorLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = applicationPath(request);
        return !(path.equals("/") || path.equals("/shop") || path.equals("/notice")
                || PRODUCT_DETAIL.matcher(path).matches() || NOTICE_DETAIL.matcher(path).matches());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String anonymousId = validUuidCookie(request, ANONYMOUS_COOKIE);
        if (anonymousId == null) {
            anonymousId = UUID.randomUUID().toString();
            addCookie(response, request, ANONYMOUS_COOKIE, anonymousId, ANONYMOUS_MAX_AGE);
        }

        String behaviorSessionId = validUuidCookie(request, SESSION_COOKIE);
        if (behaviorSessionId == null) {
            behaviorSessionId = UUID.randomUUID().toString();
        }
        // 활동이 있을 때마다 30분 만료 시간을 갱신한다. 인증 세션과는 무관한 분석용 식별자다.
        addCookie(response, request, SESSION_COOKIE, behaviorSessionId, SESSION_MAX_AGE);

        filterChain.doFilter(request, response);

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            BehaviorEvent event = classify(request);
            if (event != null) {
                behaviorLogService.record(request, event.eventType(), anonymousId, behaviorSessionId,
                        event.resourceType(), event.resourceId(), event.metadata(), response.getStatus());
            }
        }
    }

    private BehaviorEvent classify(HttpServletRequest request) {
        String path = applicationPath(request);
        if (path.equals("/")) {
            return new BehaviorEvent("HOME_VIEW", null, null, Map.of());
        }
        if (path.equals("/shop")) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sort", allowed(request.getParameter("sort"),
                    "newest", "views_desc", "views_asc", "price_asc", "price_desc", "name", "brand", "popular"));
            metadata.put("category", limited(request.getParameter("category"), 50));
            metadata.put("page", positiveInt(request.getParameter("page"), 1));
            return new BehaviorEvent("PRODUCT_LIST_VIEW", "PRODUCT_LIST", null, metadata);
        }
        if (path.equals("/notice")) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("period", allowed(request.getParameter("period"), "week", "month", "year", "all"));
            metadata.put("type", allowed(request.getParameter("type"), "title", "content", "all"));
            metadata.put("page", positiveInt(request.getParameter("page"), 1));
            // 검색어 원문은 개인정보가 포함될 수 있어 저장하지 않고 사용 여부만 남긴다.
            metadata.put("searchUsed", request.getParameter("keyword") != null
                    && !request.getParameter("keyword").isBlank());
            return new BehaviorEvent("NOTICE_LIST_VIEW", "NOTICE_LIST", null, metadata);
        }

        Matcher product = PRODUCT_DETAIL.matcher(path);
        if (product.matches()) {
            return new BehaviorEvent("PRODUCT_DETAIL_VIEW", "PRODUCT", product.group(1), Map.of());
        }
        Matcher notice = NOTICE_DETAIL.matcher(path);
        if (notice.matches()) {
            return new BehaviorEvent("NOTICE_DETAIL_VIEW", "NOTICE", notice.group(1), Map.of());
        }
        return null;
    }

    private String applicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
    }

    private String validUuidCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                try {
                    return UUID.fromString(cookie.getValue()).toString();
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private void addCookie(HttpServletResponse response, HttpServletRequest request,
                           String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String allowed(String value, String... allowed) {
        if (value != null) {
            for (String candidate : allowed) {
                if (candidate.equals(value)) {
                    return value;
                }
            }
        }
        return allowed[0];
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private int positiveInt(String value, int defaultValue) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private record BehaviorEvent(String eventType, String resourceType, String resourceId,
                                 Map<String, Object> metadata) {
    }
}
