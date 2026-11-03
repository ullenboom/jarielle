package com.tutego.jarielle.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Text metrics — port of src/text-metrics.ts.
 * Character-class bucket width estimation, independent of AWT FontMetrics,
 * mirroring the TS implementation used for ELK node sizing.
 */
public final class TextMetrics {

    private TextMetrics() {}

    private static final Set<String> NARROW_CHARS = Set.of(
            "i","l","t","f","j","I","1","!","|",".",",",":",";","'");

    private static final Set<String> WIDE_CHARS = Set.of(
            "W","M","w","m","@","%");

    private static final Set<String> VERY_WIDE_CHARS = Set.of("W","M");

    private static final Set<String> SEMI_NARROW_PUNCT = Set.of(
            "(",")","[","]","{","}","/","\\","-","\"","`");

    public static final double LINE_HEIGHT_RATIO = 1.3;

    public record MultilineMetrics(double width, double height, List<String> lines, double lineHeight) {}

    private static boolean isCombiningMark(int cp) {
        return (cp >= 0x0300 && cp <= 0x036F)
            || (cp >= 0x1AB0 && cp <= 0x1AFF)
            || (cp >= 0x1DC0 && cp <= 0x1DFF)
            || (cp >= 0x20D0 && cp <= 0x20FF)
            || (cp >= 0xFE20 && cp <= 0xFE2F);
    }

    private static boolean isFullwidth(int cp) {
        return (cp >= 0x1100 && cp <= 0x115F)
            || (cp >= 0x2E80 && cp <= 0x2EFF)
            || (cp >= 0x2F00 && cp <= 0x2FDF)
            || (cp >= 0x3000 && cp <= 0x303F)
            || (cp >= 0x3040 && cp <= 0x309F)
            || (cp >= 0x30A0 && cp <= 0x30FF)
            || (cp >= 0x3100 && cp <= 0x312F)
            || (cp >= 0x3130 && cp <= 0x318F)
            || (cp >= 0x3190 && cp <= 0x31FF)
            || (cp >= 0x3200 && cp <= 0x33FF)
            || (cp >= 0x3400 && cp <= 0x4DBF)
            || (cp >= 0x4E00 && cp <= 0x9FFF)
            || (cp >= 0xAC00 && cp <= 0xD7AF)
            || (cp >= 0xF900 && cp <= 0xFAFF)
            || (cp >= 0xFF00 && cp <= 0xFF60)
            || (cp >= 0xFFE0 && cp <= 0xFFE6)
            || cp >= 0x20000;
    }

    private static boolean isEmoji(String ch) {
        // Simplified: cover common emoji ranges
        int cp = ch.codePointAt(0);
        return (cp >= 0x1F300 && cp <= 0x1FAFF)
            || (cp >= 0x2600 && cp <= 0x27BF)
            || (cp >= 0xFE00 && cp <= 0xFE0F)
            || (cp >= 0x1F900 && cp <= 0x1F9FF);
    }

    public static double getCharWidth(String ch) {
        int cp = ch.codePointAt(0);
        if (isCombiningMark(cp)) return 0;
        if (isFullwidth(cp) || isEmoji(ch)) return 2.0;
        if (" ".equals(ch)) return 0.3;
        if (VERY_WIDE_CHARS.contains(ch)) return 1.5;
        if (WIDE_CHARS.contains(ch)) return 1.2;
        if (NARROW_CHARS.contains(ch)) return 0.4;
        if (SEMI_NARROW_PUNCT.contains(ch)) return 0.5;
        if ("r".equals(ch)) return 0.8;
        if (cp >= 65 && cp <= 90) return 1.2;
        if (cp >= 48 && cp <= 57) return 1.0;
        return 1.0;
    }

    public static double measureTextWidth(String text, int fontSize, int fontWeight) {
        double baseRatio = fontWeight >= 600 ? 0.60 : fontWeight >= 500 ? 0.57 : 0.54;
        double total = 0;
        // iterate codepoints
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            total += getCharWidth(ch);
            i += Character.charCount(cp);
        }
        double minPadding = fontSize * 0.15;
        return total * fontSize * baseRatio + minPadding;
    }

    public static MultilineMetrics measureMultilineText(String text, int fontSize, int fontWeight) {
        String[] parts = text.split("\n", -1);
        List<String> lines = List.of(parts);
        double lineHeight = fontSize * LINE_HEIGHT_RATIO;
        double maxWidth = 0;
        for (String line : lines) {
            // strip formatting tags for measurement (same as TS)
            String plain = line.replaceAll("(?i)</?(?:b|strong|i|em|u|s|del)\\s*>", "");
            double w = measureTextWidth(plain, fontSize, fontWeight);
            if (w > maxWidth) maxWidth = w;
        }
        return new MultilineMetrics(maxWidth, lines.size() * lineHeight, new ArrayList<>(lines), lineHeight);
    }

    /** Estimate same as styles.ts estimateTextWidth. */
    public static double estimateTextWidth(String text, int fontSize, int fontWeight) {
        return measureTextWidth(text, fontSize, fontWeight);
    }
}
