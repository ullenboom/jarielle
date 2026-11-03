package com.tutego.jarielle.theme;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

/**
 * Diagram color configuration — port of {@code DiagramColors} in src/theme.ts.
 * Two required vars (bg/fg) + five optional enrichment vars.
 */
public record DiagramColors(
        String bg,
        String fg,
        String line,
        String accent,
        String muted,
        String surface,
        String border
) {
    public DiagramColors {
        Objects.requireNonNull(bg, "bg");
        Objects.requireNonNull(fg, "fg");
    }

    public static DiagramColors of(String bg, String fg) {
        return new DiagramColors(bg, fg, null, null, null, null, null);
    }

    public static DiagramColors of(String bg, String fg, String line, String accent, String muted, String surface, String border) {
        return new DiagramColors(bg, fg, line, accent, muted, surface, border);
    }

    public Optional<String> lineOpt()    { return Optional.ofNullable(line); }
    public Optional<String> accentOpt()  { return Optional.ofNullable(accent); }
    public Optional<String> mutedOpt()   { return Optional.ofNullable(muted); }
    public Optional<String> surfaceOpt() { return Optional.ofNullable(surface); }
    public Optional<String> borderOpt()  { return Optional.ofNullable(border); }

    /** Parse this theme's colors to AWT Colors (hex #RRGGBB or CSS var() not supported — falls back to fg/bg). */
    public Color bgColor() { return ColorUtils.parseHex(bg); }
    public Color fgColor() { return ColorUtils.parseHex(fg); }

    /** Create from RenderOptions. */
    public static DiagramColors from(com.tutego.jarielle.model.RenderOptions opts) {
        if (opts == null) return DiagramColors.of("#FFFFFF", "#27272A");
        return new DiagramColors(
                opts.effectiveBg(), opts.effectiveFg(),
                opts.line(), opts.accent(), opts.muted(), opts.surface(), opts.border()
        );
    }
}
