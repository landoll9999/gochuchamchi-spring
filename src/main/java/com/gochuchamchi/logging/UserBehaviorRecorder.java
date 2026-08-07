package com.gochuchamchi.logging;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface UserBehaviorRecorder {
    void record(HttpServletRequest request, String eventType,
                String anonymousId, String behaviorSessionId,
                String resourceType, String resourceId,
                Map<String, Object> metadata, int responseStatus);
}
