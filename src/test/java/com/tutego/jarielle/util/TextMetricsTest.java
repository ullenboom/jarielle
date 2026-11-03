package com.tutego.jarielle.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TextMetricsTest {

    @Test
    void charWidths() {
        assertThat(TextMetrics.getCharWidth("i")).isEqualTo(0.4);
        assertThat(TextMetrics.getCharWidth("W")).isEqualTo(1.5);
        assertThat(TextMetrics.getCharWidth(" ")).isEqualTo(0.3);
        assertThat(TextMetrics.getCharWidth("A")).isEqualTo(1.2);
    }

    @Test
    void multiline() {
        var m = TextMetrics.measureMultilineText("Hello\nWorld", 13, 500);
        assertThat(m.lines()).hasSize(2);
        assertThat(m.width()).isGreaterThan(0);
        assertThat(m.height()).isEqualTo(2 * 13 * TextMetrics.LINE_HEIGHT_RATIO);
    }

    @Test
    void estimateWidthScalesWithSize() {
        double w1 = TextMetrics.measureTextWidth("Hello", 13, 500);
        double w2 = TextMetrics.measureTextWidth("Hello", 26, 500);
        assertThat(w2).isGreaterThan(w1);
    }
}
