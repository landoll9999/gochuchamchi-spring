package com.gochuchamchi.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserBehaviorLogDto {
    private String eventType;
    private Long userId;
    private String anonymousId;
    private String behaviorSessionId;
    private String requestPath;
    private String resourceType;
    private String resourceId;
    private String metadata;
    private Integer responseStatus;
}
