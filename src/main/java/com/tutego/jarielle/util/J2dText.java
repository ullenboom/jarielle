package com.tutego.jarielle.util;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Unified text rendering utility for Java2D.
 * <p>
 * Separation of concerns: {@link TextMetrics} is used exclusively for ELK sizing
 * (before layout) where no {@link Graphics2D} is available. For painting,
 * only real {@link FontMetrics}/{@link java.awt.font.TextLayout} is used so that
 * kerning/ligatures and background pills are perfectly centered.
 * </p>
 * <p>
 * Reduces duplication in {@code GraphRenderer}, {@code ClassRenderer},
 * {@code SequenceRenderer} and {@code ErRenderer}.
 * </p>
 */
public final class J2dText {

    private J2dText() {}

    /**
     * Measures a text block with real {@link FontMetrics}.
     * @return [maxWidth, totalHeight, lineHeight]
     */
    public static double[] measure(Graphics2D g2, String text, Font font) {
        if (text == null || text.isEmpty()) return new double[]{0,0,0};
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        String[] lines = stripTags(text).split("\n", -1);
        int maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, fm.stringWidth(l));
        double totalH = lines.length * fm.getHeight();
        return new double[]{maxW, totalH, fm.getHeight()};
    }

    /**
     * Draws multiline text centered at {@code (cx,cy)}.
     * Vertically centered: first baseline = cy - totalH/2 + ascent.
     */
    public static void drawCentered(Graphics2D g2, String text, double cx, double cy, Font font, Color color) {
        if (text == null || text.isEmpty()) return;
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        String[] lines = stripTags(text).split("\n", -1);
        double totalH = lines.length * fm.getHeight();
        double y = cy - totalH/2 + fm.getAscent();
        for (String line : lines) {
            int sw = fm.stringWidth(line);
            g2.drawString(line, (float)(cx - sw/2.0), (float)y);
            y += fm.getHeight();
        }
    }

    /**
     * Draws text centered with a background pill (e.g. edge labels).
     * @param padX horizontal padding, padY vertical padding, arc corner radius
     */
    public static void drawCenteredWithBackground(Graphics2D g2, String text, double cx, double cy, Font font, Color textColor, Color bg, Color border, double padX, double padY, double arc) {
        if (text == null || text.isEmpty()) return;
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        String[] lines = stripTags(text).split("\n", -1);
        int maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, fm.stringWidth(l));
        double totalH = lines.length * fm.getHeight();
        double bgW = maxW + padX*2;
        double bgH = totalH + padY*2;
        double bx = cx - bgW/2, by = cy - bgH/2;
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Double(bx, by, bgW, bgH, arc, arc));
        if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(bx, by, bgW, bgH, arc, arc));
        }
        drawCentered(g2, text, cx, cy, font, textColor);
    }

    /**
     * Draws left-aligned text at {@code (x, y)} (baseline y).
     */
    public static void drawLeft(Graphics2D g2, String text, double x, double y, Font font, Color color) {
        if (text == null || text.isEmpty()) return;
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(stripTags(text), (float)x, (float)y);
    }

    /**
     * Draws text at an arbitrary position with an anchor.
     */
    public static void drawAnchored(Graphics2D g2, String text, double cx, double cy, Font font, Color color, Anchor anchor) {
        if (text == null || text.isEmpty()) return;
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        String[] lines = stripTags(text).split("\n", -1);
        double totalH = lines.length * fm.getHeight();
        double startY = switch (anchor) {
            case CENTER -> cy - totalH/2 + fm.getAscent();
            case TOP -> cy + fm.getAscent();
            case LEFT, RIGHT -> cy - totalH/2 + fm.getAscent();
        };
        double y = startY;
        for (String line : lines) {
            int sw = fm.stringWidth(line);
            double x = switch (anchor) {
                case CENTER, TOP -> cx - sw/2.0;
                case LEFT -> cx;
                case RIGHT -> cx - sw;
            };
            g2.drawString(line, (float)x, (float)y);
            y += fm.getHeight();
        }
    }

    public enum Anchor { CENTER, LEFT, RIGHT, TOP }

    /**
     * Computes the background rectangle for centered text — measurement only.
     */
    public static Rectangle2D backgroundBounds(Graphics2D g2, String text, double cx, double cy, Font font, double padX, double padY) {
        if (text == null) return new Rectangle2D.Double(cx,cy,0,0);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        String[] lines = stripTags(text).split("\n", -1);
        int maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, fm.stringWidth(l));
        double totalH = lines.length * fm.getHeight();
        return new Rectangle2D.Double(cx - (maxW+padX*2)/2, cy - (totalH+padY*2)/2, maxW+padX*2, totalH+padY*2);
    }

    static String stripTags(String s) {
        return s == null ? "" : s.replaceAll("(?i)</?(?:b|strong|i|em|u|s|del)\\s*>", "");
    }
}
