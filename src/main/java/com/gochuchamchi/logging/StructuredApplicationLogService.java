package com.gochuchamchi.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gochuchamchi.dto.AuditLogDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StructuredApplicationLogService implements HttpAccessRecorder {

    private static final Logger securityJson = LoggerFactory.getLogger("APPLICATION_SECURITY_JSON");
    private static final Logger log = LoggerFactory.getLogger(StructuredApplicationLogService.class);

    private final ObjectMapper objectMapper;

    @Override
    public void record(HttpServletRequest request, int statusCode, long responseTimeMs, Throwable failure) {
        Map<String, Object> event = baseEvent(request, "HTTP_ACCESS", "HTTP_REQUEST");
        event.put("severity", httpSeverity(statusCode));
        event.put("statusCode", statusCode);
        event.put("responseTimeMs", responseTimeMs);
        event.put("outcome", statusCode < 400 ? "SUCCESS" : "FAILURE");
        if (failure != null) {
            event.put("exceptionType", LogRequestContext.sanitize(failure.getClass().getSimpleName(), 100));
        }
        write(event);
    }

    public void recordSecurityEvent(AuditLogDto entry) {
        HttpServletRequest request = LogRequestContext.currentRequest();
        Map<String, Object> event = baseEvent(request, "SECURITY_EVENT", entry.getEventType());
        event.put("severity", securitySeverity(entry));
        event.put("outcome", entry.getOutcome());
        putIfPresent(event, "actorUserId", entry.getActorUserId());
        putIfPresent(event, "actorUsername", entry.getActorUsername());
        putIfPresent(event, "targetType", entry.getTargetType());
        putIfPresent(event, "targetId", entry.getTargetId());
        putIfPresent(event, "reasonCode", entry.getReasonCode());
        putIfPresent(event, "details", entry.getDetails());
        write(event);
    }

    private Map<String, Object> baseEvent(HttpServletRequest request, String category, String eventType) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("eventCategory", category);
        event.put("eventType", eventType);
        putIfPresent(event, "requestId", LogRequestContext.requestId(request));
        putIfPresent(event, "cloudFrontRequestId", LogRequestContext.cloudFrontRequestId(request));
        putIfPresent(event, "clientIp", LogRequestContext.clientAddress(request));
        putIfPresent(event, "method", LogRequestContext.method(request));
        putIfPresent(event, "uri", LogRequestContext.path(request));
        putIfPresent(event, "userAgent", LogRequestContext.userAgent(request));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            putIfPresent(event, "principal", LogRequestContext.sanitize(authentication.getName(), 100));
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(value -> LogRequestContext.sanitize(value, 64))
                    .toList();
            if (!roles.isEmpty()) {
                event.put("roles", roles);
            }
        }
        return event;
    }

    private String httpSeverity(int statusCode) {
        if (statusCode >= 500) {
            return "HIGH";
        }
        if (statusCode >= 400) {
            return "MEDIUM";
        }
        return "INFO";
    }

    private String securitySeverity(AuditLogDto entry) {
        String type = entry.getEventType() == null ? "" : entry.getEventType();
        if (type.contains("ROLE_CHANGED") || type.contains("SUPERADMIN")) {
            return "CRITICAL";
        }
        if (type.equals("ACCESS_DENIED") || type.equals("ACCESS_BLOCKED")
                || type.equals("PASSWORD_CHANGED") || type.contains("SUSPENDED")) {
            return "HIGH";
        }
        if (type.equals("LOGIN") && "FAILURE".equals(entry.getOutcome())) {
            return "MEDIUM";
        }
        return "INFO";
    }

    private void putIfPresent(Map<String, Object> event, String key, Object value) {
        if (value != null && (!(value instanceof String text) || !text.isBlank())) {
            event.put(key, value);
        }
    }

    private void write(Map<String, Object> event) {
        try {
            securityJson.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("STRUCTURED_SECURITY_LOG_SERIALIZE_FAILED category={} type={}",
                    event.get("eventCategory"), event.get("eventType"), e);
        }
    }
}
