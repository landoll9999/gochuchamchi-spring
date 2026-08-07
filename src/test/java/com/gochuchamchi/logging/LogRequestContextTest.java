package com.gochuchamchi.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogRequestContextTest {

    @Test
    void removesControlCharactersAndLimitsLength() {
        assertThat(LogRequestContext.sanitize("  first\r\nsecond\tthird  ", 18))
                .isEqualTo("first  second thir");
    }
}
