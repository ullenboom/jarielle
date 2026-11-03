package com.tutego.jarielle.theme;

import java.awt.Color;

/**
 * Color utilities — ports color-mix() derivations from src/theme.ts MIX weights.
 */
public final class ColorUtils {

    private ColorUtils() {}

    public static final class Mix {
        public static final int TEXT_SEC     = 60;
        public static final int TEXT_MUTED   = 40;
        public static final int TEXT_FAINT   = 25;
        public static final int LINE         = 50;
        public static final int ARROW        = 85;
        public static final int NODE_FILL    = 3;
        public static final int NODE_STROKE  = 20;
        public static final int GROUP_HEADER = 5;
        public static final int INNER_STROKE = 12;
        public static final int KEY_BADGE    = 10;
        private Mix() {}
    }

    /** Parse hex like "#1a1b26", "#fff", "var(--bg)" fallback to null. */
    public static Color parseHex(String hex) {
        if (hex == null) return null;
        String v = hex.trim();
        if (v.startsWith("var(")) return null; // CSS variable — caller decides
        if (v.startsWith("#")) v = v.substring(1);
        if (v.length() == 3) v = "" + v.charAt(0) + v.charAt(0) + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2);
        if (v.length() != 6) return Color.BLACK;
        try {
            int rgb = Integer.parseInt(v, 16);
            return new Color(rgb);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    /** Parse hex safely with fallback. */
    public static Color parseHexOr(String hex, Color fallback) {
        Color c = parseHex(hex);
        return c != null ? c : fallback;
    }

    /** Mix fg into bg at percent (0..100) — ports CSS color-mix(in srgb, fg pct%, bg). */
    public static Color mix(Color fg, Color bg, int percent) {
        double p = percent / 100.0;
        int r = (int) Math.round(bg.getRed()   * (1 - p) + fg.getRed()   * p);
        int g = (int) Math.round(bg.getGreen() * (1 - p) + fg.getGreen() * p);
        int b = (int) Math.round(bg.getBlue()  * (1 - p) + fg.getBlue()  * p);
        int a = (int) Math.round(bg.getAlpha() * (1 - p) + fg.getAlpha() * p);
        return new Color(clamp(r), clamp(g), clamp(b), clamp(a));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /** Derived palette from DiagramColors — mirrors CSS variable derivations in theme.ts. */
    public record DerivedPalette(
            Color bg,
            Color fg,
            Color text,
            Color textSec,
            Color textMuted,
            Color textFaint,
            Color line,
            Color arrow,
            Color nodeFill,
            Color nodeStroke,
            Color groupFill,
            Color groupHeader,
            Color innerStroke
    ) {}

    public static DerivedPalette derive(DiagramColors dc) {
        Color bg = parseHexOr(dc.bg(), Color.WHITE);
        Color fg = parseHexOr(dc.fg(), Color.BLACK);

        Color line       = dc.line()   != null ? parseHexOr(dc.line(),   mix(fg, bg, Mix.LINE))       : mix(fg, bg, Mix.LINE);
        Color arrow      = dc.accent() != null ? parseHexOr(dc.accent(), mix(fg, bg, Mix.ARROW))      : mix(fg, bg, Mix.ARROW);
        Color textMutedC = dc.muted() != null ? parseHexOr(dc.muted(), mix(fg, bg, Mix.TEXT_MUTED))   : mix(fg, bg, Mix.TEXT_MUTED);
        Color nodeFill   = dc.surface()!= null ? parseHexOr(dc.surface(),mix(fg, bg, Mix.NODE_FILL))  : mix(fg, bg, Mix.NODE_FILL);
        Color nodeStroke = dc.border() != null ? parseHexOr(dc.border(), mix(fg, bg, Mix.NODE_STROKE)): mix(fg, bg, Mix.NODE_STROKE);
        // textSec falls back to muted then derivation
        Color textSec = dc.muted() != null ? parseHexOr(dc.muted(), mix(fg, bg, Mix.TEXT_SEC)) : mix(fg, bg, Mix.TEXT_SEC);

        return new DerivedPalette(
                bg, fg,
                fg,
                textSec,
                textMutedC,
                mix(fg, bg, Mix.TEXT_FAINT),
                line, arrow, nodeFill, nodeStroke,
                bg,
                mix(fg, bg, Mix.GROUP_HEADER),
                mix(fg, bg, Mix.INNER_STROKE)
        );
    }
}
