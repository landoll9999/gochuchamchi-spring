package com.gochuchamchi.config;

import com.gochuchamchi.logging.HttpAccessRecorder;
import com.gochuchamchi.logging.LogRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class HttpAccessLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_RESPONSE_HEADER = "X-Request-Id";

    private final HttpAccessRecorder accessRecorder;

    public HttpAccessLoggingFilter(HttpAccessRecorder accessRecorder) {
        this.accessRecorder = accessRecorder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.equals("/favicon.ico") || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(LogRequestContext.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_RESPONSE_HEADER, requestId);

        long startedAt = System.nanoTime();
        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            int statusCode = response.getStatus();
            if (failure != null && statusCode < 500) {
                statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            long responseTimeMs = (System.nanoTime() - startedAt) / 1_000_000L;
            accessRecorder.record(request, statusCode, responseTimeMs, failure);
        }
    }
}
