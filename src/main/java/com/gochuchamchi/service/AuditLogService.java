package com.gochuchamchi.service;

import com.gochuchamchi.dto.AuditLogDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.logging.LogRequestContext;
import com.gochuchamchi.mapper.AuditLogMapper;
import com.gochuchamchi.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    public void success(String eventType, Long actorUserId, String actorUsername,
                        String targetType, String targetId, String details) {
        record(eventType, "SUCCESS", actorUserId, actorUsername,
                targetType, targetId, null, details);
    }

    public void successForUsername(String eventType, String actorUsername,
                                   String targetType, String targetId, String details) {
        UserDto actor = resolveActor(eventType, actorUsername);
        success(eventType, actor == null ? null : actor.getId(), actorUsername,
                targetType, targetId, details);
    }

    public void failure(String eventType, Long actorUserId, String actorUsername,
                        String targetType, String targetId, String reasonCode, String details) {
        record(eventType, "FAILURE", actorUserId, actorUsername,
                targetType, targetId, reasonCode, details);
    }

    public void failureForUsername(String eventType, String actorUsername,
                                   String targetType, String targetId,
                                   String reasonCode, String details) {
        UserDto actor = resolveActor(eventType, actorUsername);
        failure(eventType, actor == null ? null : actor.getId(), actorUsername,
                targetType, targetId, reasonCode, details);
    }

    private UserDto resolveActor(String eventType, String actorUsername) {
        if (actorUsername == null) {
            return null;
        }
        try {
            return userMapper.findByUsername(actorUsername);
        } catch (RuntimeException e) {
            log.warn("AUDIT_ACTOR_RESOLVE_FAILED event={} actorUsername={}",
                    LogRequestContext.sanitize(eventType, 64),
                    LogRequestContext.sanitize(actorUsername, 50), e);
            return null;
        }
    }

    private void record(String eventType, String outcome, Long actorUserId, String actorUsername,
                        String targetType, String targetId, String reasonCode, String details) {
        HttpServletRequest request = LogRequestContext.currentRequest();
        AuditLogDto entry = AuditLogDto.builder()
                .eventType(LogRequestContext.sanitize(eventType, 64))
                .outcome(outcome)
                .actorUserId(actorUserId)
                .actorUsername(LogRequestContext.sanitize(actorUsername, 50))
                .targetType(LogRequestContext.sanitize(targetType, 32))
                .targetId(LogRequestContext.sanitize(targetId, 100))
                .requestMethod(LogRequestContext.method(request))
                .requestPath(LogRequestContext.path(request))
                .ipAddress(LogRequestContext.remoteAddress(request))
                .userAgent(LogRequestContext.userAgent(request))
                .reasonCode(LogRequestContext.sanitize(reasonCode, 64))
                .details(LogRequestContext.sanitize(details, 1000))
                .build();

        // 중앙 로그 수집기가 DB와 별개로 감사 사건을 가져갈 수 있도록 항상 구조화된 한 줄을 남긴다.
        log.info("AUDIT event={} outcome={} actorUserId={} actorUsername={} targetType={} targetId={} method={} path={} ip={} reason={} details={}",
                entry.getEventType(), entry.getOutcome(), entry.getActorUserId(), entry.getActorUsername(),
                entry.getTargetType(), entry.getTargetId(), entry.getRequestMethod(), entry.getRequestPath(),
                entry.getIpAddress(), entry.getReasonCode(), entry.getDetails());

        try {
            auditLogMapper.insert(entry);
        } catch (RuntimeException e) {
            // 로깅 저장소 장애가 사용자 요청의 성공/실패를 바꾸지 않도록 표준 로그를 폴백으로 사용한다.
            log.error("AUDIT_PERSIST_FAILED event={} outcome={} targetType={} targetId={}",
                    entry.getEventType(), entry.getOutcome(), entry.getTargetType(), entry.getTargetId(), e);
        }
    }
}
