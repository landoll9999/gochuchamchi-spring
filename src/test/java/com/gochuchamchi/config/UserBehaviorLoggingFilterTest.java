package com.gochuchamchi.config;

import com.gochuchamchi.logging.UserBehaviorRecorder;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserBehaviorLoggingFilterTest {

    private final UserBehaviorRecorder behaviorLogService = mock(UserBehaviorRecorder.class);
    private final UserBehaviorLoggingFilter filter = new UserBehaviorLoggingFilter(behaviorLogService);

    @Test
    void logsAnonymousProductDetailAndSetsIdentifiers() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shop/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verify(behaviorLogService).record(eq(request), eq("PRODUCT_DETAIL_VIEW"),
                argThat(this::isUuid), argThat(this::isUuid),
                eq("PRODUCT"), eq("42"), eq(Map.of()), eq(200));
        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(value -> value.startsWith(UserBehaviorLoggingFilter.ANONYMOUS_COOKIE + "="))
                .anyMatch(value -> value.startsWith(UserBehaviorLoggingFilter.SESSION_COOKIE + "="));
    }

    @Test
    void keepsAnonymousIdentifierAndDoesNotStoreRawSearchKeyword() throws Exception {
        String anonymousId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/notice");
        request.setCookies(
                new Cookie(UserBehaviorLoggingFilter.ANONYMOUS_COOKIE, anonymousId),
                new Cookie(UserBehaviorLoggingFilter.SESSION_COOKIE, sessionId));
        request.addParameter("keyword", "personal@example.com");
        request.addParameter("period", "month");
        request.addParameter("type", "content");
        request.addParameter("page", "2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ArgumentCaptor<Map<String, Object>> metadata = ArgumentCaptor.forClass(Map.class);

        filter.doFilter(request, response, new MockFilterChain());

        verify(behaviorLogService).record(eq(request), eq("NOTICE_LIST_VIEW"),
                eq(anonymousId), eq(sessionId), eq("NOTICE_LIST"), isNull(), metadata.capture(), eq(200));
        assertThat(metadata.getValue())
                .containsEntry("searchUsed", true)
                .containsEntry("period", "month")
                .containsEntry("type", "content")
                .containsEntry("page", 2)
                .doesNotContainValue("personal@example.com");
    }

    @Test
    void ignoresPostRequestsAndUntrackedPages() throws Exception {
        filter.doFilter(new MockHttpServletRequest("POST", "/shop/42"),
                new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(new MockHttpServletRequest("GET", "/admin/users"),
                new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(behaviorLogService);
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
