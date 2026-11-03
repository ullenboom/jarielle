package com.tutego.jarielle.theme;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.*;

class ThemeTest {

    @Test
    void derivePaletteFromMono() {
        var dc = DiagramColors.of("#FFFFFF", "#27272A");
        var pal = ColorUtils.derive(dc);
        assertThat(pal.bg()).isEqualTo(Color.WHITE);
        assertThat(pal.fg()).isNotNull();
        assertThat(pal.nodeFill()).isNotEqualTo(pal.bg());
        assertThat(pal.line()).isNotNull();
    }

    @Test
    void deriveWithEnrichmentOverrides() {
        var dc = DiagramColors.of("#1a1b26", "#a9b1d6", "#3d59a1", "#7aa2f7", "#565f89", null, null);
        var pal = ColorUtils.derive(dc);
        assertThat(pal.line()).isEqualTo(ColorUtils.parseHex("#3d59a1"));
        assertThat(pal.arrow()).isEqualTo(ColorUtils.parseHex("#7aa2f7"));
    }

    @Test
    void themesContain15() {
        assertThat(Themes.all()).hasSize(15);
        assertThat(Themes.get("dracula")).isNotNull();
        assertThat(Themes.get("tokyo-night").bg()).isEqualTo("#1a1b26");
    }

    @Test
    void colorMix() {
        Color bg = Color.WHITE;
        Color fg = Color.BLACK;
        Color mixed = ColorUtils.mix(fg, bg, 50);
        assertThat(mixed.getRed()).isBetween(120, 135);
        assertThat(mixed.getGreen()).isBetween(120, 135);
        assertThat(mixed.getBlue()).isBetween(120, 135);
    }

    @Test
    void parseHex() {
        assertThat(ColorUtils.parseHex("#fff")).isEqualTo(Color.WHITE);
        assertThat(ColorUtils.parseHex("#000000")).isEqualTo(Color.BLACK);
        assertThat(ColorUtils.parseHex("var(--bg)")).isNull();
    }
}
