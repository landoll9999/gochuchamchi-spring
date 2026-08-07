package com.gochuchamchi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gochuchamchi.dto.UserBehaviorLogDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.logging.LogRequestContext;
import com.gochuchamchi.logging.UserBehaviorRecorder;
import com.gochuchamchi.mapper.UserBehaviorLogMapper;
import com.gochuchamchi.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserBehaviorLogService implements UserBehaviorRecorder {

    private static final Logger log = LoggerFactory.getLogger(UserBehaviorLogService.class);

    private final UserBehaviorLogMapper behaviorLogMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void record(HttpServletRequest request, String eventType,
                       String anonymousId, String behaviorSessionId,
                       String resourceType, String resourceId,
                       Map<String, Object> metadata, int responseStatus) {
        try {
            recordInternal(request, eventType, anonymousId, behaviorSessionId,
                    resourceType, resourceId, metadata, responseStatus);
        } catch (RuntimeException e) {
            // 분석 로깅 장애로 페이지 응답이 실패해서는 안 된다.
            log.error("BEHAVIOR_RECORD_FAILED event={} path={}",
                    LogRequestContext.sanitize(eventType, 64), LogRequestContext.path(request), e);
        }
    }

    private void recordInternal(HttpServletRequest request, String eventType,
                                String anonymousId, String behaviorSessionId,
                                String resourceType, String resourceId,
                                Map<String, Object> metadata, int responseStatus) {
        Long userId = currentUserId();
        UserBehaviorLogDto entry = UserBehaviorLogDto.builder()
                .eventType(LogRequestContext.sanitize(eventType, 64))
                .userId(userId)
                .anonymousId(LogRequestContext.sanitize(anonymousId, 36))
                .behaviorSessionId(LogRequestContext.sanitize(behaviorSessionId, 36))
                .requestPath(LogRequestContext.path(request))
                .resourceType(LogRequestContext.sanitize(resourceType, 32))
                .resourceId(LogRequestContext.sanitize(resourceId, 100))
                .metadata(toJson(metadata))
                .responseStatus(responseStatus)
                .build();

        log.info("BEHAVIOR event={} userId={} anonymousId={} behaviorSessionId={} path={} resourceType={} resourceId={} status={} metadata={}",
                entry.getEventType(), entry.getUserId(), entry.getAnonymousId(), entry.getBehaviorSessionId(),
                entry.getRequestPath(), entry.getResourceType(), entry.getResourceId(),
                entry.getResponseStatus(), entry.getMetadata());

        try {
            behaviorLogMapper.insert(entry);
        } catch (RuntimeException e) {
            log.error("BEHAVIOR_PERSIST_FAILED event={} path={} resourceType={} resourceId={}",
                    entry.getEventType(), entry.getRequestPath(), entry.getResourceType(), entry.getResourceId(), e);
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        UserDto user = userMapper.findByUsername(authentication.getName());
        return user == null ? null : user.getId();
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return LogRequestContext.sanitize(objectMapper.writeValueAsString(metadata), 1000);
        } catch (JsonProcessingException e) {
            log.warn("BEHAVIOR_METADATA_SERIALIZE_FAILED", e);
            return null;
        }
    }
}
