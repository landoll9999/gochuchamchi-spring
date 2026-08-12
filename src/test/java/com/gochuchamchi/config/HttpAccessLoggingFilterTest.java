package com.gochuchamchi.config;

import com.gochuchamchi.logging.HttpAccessRecorder;
import com.gochuchamchi.logging.LogRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HttpAccessLoggingFilterTest {

    private final HttpAccessRecorder recorder = mock(HttpAccessRecorder.class);
    private final HttpAccessLoggingFilter filter = new HttpAccessLoggingFilter(recorder);

    @Test
    void recordsStatusAndAddsCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(403));

        assertThat(request.getAttribute(LogRequestContext.REQUEST_ID_ATTRIBUTE)).isNotNull();
        assertThat(response.getHeader("X-Request-Id"))
                .isEqualTo(request.getAttribute(LogRequestContext.REQUEST_ID_ATTRIBUTE));
        verify(recorder).record(eq(request), eq(403), longThat(value -> value >= 0), isNull());
    }

    @Test
    void recordsUnhandledExceptionAs500AndRethrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new RuntimeException("test failure");

        assertThrows(RuntimeException.class,
                () -> filter.doFilter(request, response, (req, res) -> { throw failure; }));

        verify(recorder).record(eq(request), eq(500), longThat(value -> value >= 0), same(failure));
    }

    @Test
    void ignoresStaticAssets() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/css/common.css");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(recorder);
    }
}
