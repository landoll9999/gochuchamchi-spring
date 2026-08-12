package com.gochuchamchi.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LogRequestContextTest {

    @Test
    void removesControlCharactersAndLimitsLength() {
        assertThat(LogRequestContext.sanitize("  first\r\nsecond\tthird  ", 18))
                .isEqualTo("first  second thir");
    }

    @Test
    void selectsViewerAddressFromCloudFrontAndAlbForwardedChain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "spoofed, 203.0.113.10, 54.240.1.1");

        assertThat(LogRequestContext.clientAddress(request)).isEqualTo("203.0.113.10");
    }
}
