package com.gochuchamchi.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogDto {
    private String eventType;
    private String outcome;
    private Long actorUserId;
    private String actorUsername;
    private String targetType;
    private String targetId;
    private String requestMethod;
    private String requestPath;
    private String ipAddress;
    private String userAgent;
    private String reasonCode;
    private String details;
}
