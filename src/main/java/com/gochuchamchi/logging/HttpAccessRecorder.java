package com.gochuchamchi.logging;

import jakarta.servlet.http.HttpServletRequest;

public interface HttpAccessRecorder {

    void record(HttpServletRequest request, int statusCode, long responseTimeMs, Throwable failure);
}
