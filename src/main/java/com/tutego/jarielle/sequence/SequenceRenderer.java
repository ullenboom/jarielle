package com.tutego.jarielle.sequence;

import com.tutego.jarielle.theme.ColorUtils;
import com.tutego.jarielle.theme.DiagramColors;
import com.tutego.jarielle.util.J2dText;
import com.tutego.jarielle.util.StyleConstants;
import com.tutego.jarielle.util.TextMetrics;

import java.awt.*;
import java.awt.geom.*;

/**
 * Java2D renderer for sequence diagrams — port of {@code src/sequence/renderer.ts}.
 */
public final class SequenceRenderer {

    private SequenceRenderer() {}

    public static void render(Graphics2D g2, PositionedSequenceDiagram diagram, DiagramColors colors, String fontName) {
        var pal = ColorUtils.derive(colors);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (colors != null) {
            // background already painted by MermaidPanel; but ensure diagram bg
        }
        // 1. blocks
        for (var b : diagram.blocks()) drawBlock(g2, b, pal, fontName);
        // 2. lifelines
        for (var l : diagram.lifelines()) drawLifeline(g2, l, pal);
        // 3. activations
        for (var a : diagram.activations()) drawActivation(g2, a, pal);
        // 4. messages
        for (var m : diagram.messages()) drawMessage(g2, m, pal, fontName);
        // 5. notes
        for (var n : diagram.notes()) drawNote(g2, n, pal, fontName);
        // 6. actors on top
        for (var a : diagram.actors()) drawActor(g2, a, pal, fontName);
    }

    private static void drawBlock(Graphics2D g2, PositionedSequenceDiagram.PositionedBlock block, ColorUtils.DerivedPalette pal, String fontName) {
        g2.setColor(pal.nodeStroke());
        g2.setStroke(new BasicStroke((float)StyleConstants.StrokeWidths.OUTER_BOX));
        g2.draw(new Rectangle2D.Double(block.x(), block.y(), block.width(), block.height()));
        // tab
        String labelText = block.type() + (block.label()!=null && !block.label().isEmpty() ? " ["+block.label()+"]" : "");
        String firstLine = labelText.split("\n")[0];
        double tabWidth = TextMetrics.measureTextWidth(firstLine, StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.GROUP_HEADER) + 16;
        double tabHeight = 18;
        g2.setColor(pal.groupHeader());
        g2.fill(new Rectangle2D.Double(block.x(), block.y(), tabWidth, tabHeight));
        g2.setColor(pal.nodeStroke());
        g2.draw(new Rectangle2D.Double(block.x(), block.y(), tabWidth, tabHeight));
        g2.setColor(pal.textSec());
        Font f = new Font(fontName, Font.BOLD, StyleConstants.FontSizes.EDGE_LABEL);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        double tx = block.x() + 6;
        double ty = block.y() + tabHeight/2 + fm.getAscent()/2 - 2;
        // support multiline? just draw first line
        for (String line : labelText.split("\n")) {
            g2.drawString(line.replaceAll("<[^>]*>",""), (float)tx, (float)ty);
            ty += fm.getHeight();
            if (ty > block.y()+tabHeight) break;
        }
        // dividers
        g2.setColor(pal.line());
        g2.setStroke(new BasicStroke(0.75f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f,4f}, 0f));
        for (var d : block.dividers()) {
            g2.draw(new Line2D.Double(block.x(), d.y(), block.x()+block.width(), d.y()));
            if (d.label()!=null && !d.label().isEmpty()) {
                g2.setColor(pal.textMuted());
                Font df = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
                g2.setFont(df);
                g2.drawString("["+stripTags(d.label())+"]", (float)(block.x()+8), (float)(d.y()+14));
                g2.setColor(pal.line());
            }
        }
    }

    private static void drawLifeline(Graphics2D g2, PositionedSequenceDiagram.Lifeline l, ColorUtils.DerivedPalette pal) {
        g2.setColor(pal.line());
        g2.setStroke(new BasicStroke(0.75f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f,4f}, 0f));
        g2.draw(new Line2D.Double(l.x(), l.topY(), l.x(), l.bottomY()));
    }

    private static void drawActivation(Graphics2D g2, PositionedSequenceDiagram.Activation a, ColorUtils.DerivedPalette pal) {
        g2.setColor(pal.nodeFill());
        g2.fill(new Rectangle2D.Double(a.x(), a.topY(), a.width(), a.bottomY()-a.topY()));
        g2.setColor(pal.nodeStroke());
        g2.setStroke(new BasicStroke((float)StyleConstants.StrokeWidths.INNER_BOX));
        g2.draw(new Rectangle2D.Double(a.x(), a.topY(), a.width(), a.bottomY()-a.topY()));
    }

    private static void drawMessage(Graphics2D g2, PositionedSequenceDiagram.PositionedMessage msg, ColorUtils.DerivedPalette pal, String fontName) {
        Color lineCol = pal.line();
        Color arrowCol = pal.arrow();
        g2.setColor(lineCol);
        Stroke s = "dashed".equals(msg.lineStyle()) ? new BasicStroke((float)StyleConstants.StrokeWidths.CONNECTOR, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f,4f},0f) : new BasicStroke((float)StyleConstants.StrokeWidths.CONNECTOR);
        g2.setStroke(s);
        boolean filled = "filled".equals(msg.arrowHead());
        if (msg.isSelf()) {
            double loopW = 30, loopH = 20, pad=8;
            Path2D p = new Path2D.Double();
            p.moveTo(msg.x1(), msg.y());
            p.lineTo(msg.x1()+loopW, msg.y());
            p.lineTo(msg.x1()+loopW, msg.y()+loopH);
            p.lineTo(msg.x2(), msg.y()+loopH);
            g2.draw(p);
            drawArrowHead(g2, new Point2D.Double(msg.x1()+loopW, msg.y()+loopH), new Point2D.Double(msg.x2(), msg.y()+loopH), filled, arrowCol, lineCol);
            // label
            g2.setColor(pal.textMuted());
            Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String txt = stripTags(msg.label());
            String[] lines = txt.split("\n");
            double lx = msg.x1()+loopW+pad;
            double ly = msg.y()+loopH/2 + fm.getAscent()/2 -2;
            for (String line : lines) {
                g2.drawString(line, (float)lx, (float)ly);
                ly += fm.getHeight();
            }
        } else {
            g2.draw(new Line2D.Double(msg.x1(), msg.y(), msg.x2(), msg.y()));
            drawArrowHead(g2, new Point2D.Double(msg.x1(), msg.y()), new Point2D.Double(msg.x2(), msg.y()), filled, arrowCol, lineCol);
            double midX = (msg.x1()+msg.x2())/2;
            double baseY = msg.y() - 10;
            Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
            J2dText.drawCentered(g2, stripTags(msg.label()), midX, baseY, f, pal.textMuted());
        }
    }

    private static void drawArrowHead(Graphics2D g2, Point2D from, Point2D to, boolean filled, Color arrowCol, Color lineCol) {
        double dx = to.getX()-from.getX(), dy = to.getY()-from.getY();
        double len = Math.hypot(dx,dy);
        if (len<1e-6) return;
        double ux=dx/len, uy=dy/len;
        double px=-uy, py=ux;
        double w=StyleConstants.ArrowHead.WIDTH, h=StyleConstants.ArrowHead.HEIGHT;
        Point2D tip = to;
        Point2D base = new Point2D.Double(tip.getX()-ux*w, tip.getY()-uy*w);
        Point2D left = new Point2D.Double(base.getX()+px*h/2, base.getY()+py*h/2);
        Point2D right = new Point2D.Double(base.getX()-px*h/2, base.getY()-py*h/2);
        if (filled) {
            Path2D tri = new Path2D.Double();
            tri.moveTo(tip.getX(), tip.getY());
            tri.lineTo(left.getX(), left.getY());
            tri.lineTo(right.getX(), right.getY());
            tri.closePath();
            g2.setColor(arrowCol);
            g2.fill(tri);
            g2.setColor(arrowCol);
            g2.setStroke(new BasicStroke(0.75f));
            g2.draw(tri);
        } else {
            Path2D tri = new Path2D.Double();
            tri.moveTo(left.getX(), left.getY());
            tri.lineTo(tip.getX(), tip.getY());
            tri.lineTo(right.getX(), right.getY());
            g2.setColor(arrowCol);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(tri);
        }
    }

    private static void drawNote(Graphics2D g2, PositionedSequenceDiagram.PositionedNote note, ColorUtils.DerivedPalette pal, String fontName) {
        double fold=6, x=note.x(), y=note.y(), w=note.width(), h=note.height();
        Path2D body = new Path2D.Double();
        body.moveTo(x, y);
        body.lineTo(x+w-fold, y);
        body.lineTo(x+w, y+fold);
        body.lineTo(x+w, y+h);
        body.lineTo(x, y+h);
        body.closePath();
        g2.setColor(Color.WHITE);
        // Use bg? original uses var(--bg)
        g2.fill(body);
        g2.setColor(pal.nodeStroke());
        g2.setStroke(new BasicStroke((float)StyleConstants.StrokeWidths.INNER_BOX));
        g2.draw(body);
        // fold triangle
        Path2D foldTri = new Path2D.Double();
        foldTri.moveTo(x+w-fold, y);
        foldTri.lineTo(x+w, y+fold);
        foldTri.lineTo(x+w-fold, y+fold);
        foldTri.closePath();
        g2.setColor(pal.innerStroke());
        g2.fill(foldTri);
        g2.setColor(pal.nodeStroke());
        g2.draw(foldTri);
        // text centered
        g2.setColor(pal.textMuted());
        Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        String txt = stripTags(note.text());
        String[] lines = txt.split("\n");
        double totalH = lines.length * fm.getHeight();
        double yy = y + h/2 - totalH/2 + fm.getAscent();
        for (String line : lines) {
            int sw = fm.stringWidth(line);
            g2.drawString(line, (float)(x + w/2 - sw/2), (float)yy);
            yy += fm.getHeight();
        }
    }

    private static void drawActor(Graphics2D g2, PositionedSequenceDiagram.PositionedActor actor, ColorUtils.DerivedPalette pal, String fontName) {
        double x=actor.x(), y=actor.y(), w=actor.width(), h=actor.height();
        if (actor.type()==ActorType.ACTOR) {
            // stick figure icon
            double s = (h/24)*0.9;
            double tx = x -12*s, ty = y + (h -24*s)/2;
            double sw = StyleConstants.StrokeWidths.OUTER_BOX / s;
            g2.setColor(pal.line());
            g2.setStroke(new BasicStroke((float)sw));
            AffineTransform old = g2.getTransform();
            g2.translate(tx, ty);
            g2.scale(s, s);
            // outer circle
            g2.draw(new Ellipse2D.Double(3,3,18,18));
            // head
            g2.draw(new Ellipse2D.Double(9,7,6,6));
            // shoulders arc
            Path2D shoulders = new Path2D.Double();
            shoulders.moveTo(5.6,18.3);
            // approximate arc via cubic?
            shoulders.curveTo(7,17,9.4,16,12,16);
            shoulders.curveTo(14.6,16,16.9,17,18.3,18.3);
            g2.draw(shoulders);
            g2.setTransform(old);
            // label below
            g2.setColor(pal.fg());
            Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.NODE_LABEL);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String[] lines = stripTags(actor.label()).split("\n");
            double ly = y + h + 14 + fm.getAscent()/2;
            for (String line : lines) {
                int sw2 = fm.stringWidth(line);
                g2.drawString(line, (float)(x - sw2/2), (float)ly);
                ly += fm.getHeight();
            }
        } else {
            g2.setColor(pal.nodeFill());
            g2.fill(new RoundRectangle2D.Double(x - w/2, y, w, h, 8,8));
            g2.setColor(pal.nodeStroke());
            g2.setStroke(new BasicStroke((float)StyleConstants.StrokeWidths.OUTER_BOX));
            g2.draw(new RoundRectangle2D.Double(x - w/2, y, w, h, 8,8));
            g2.setColor(pal.fg());
            Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.NODE_LABEL);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String[] lines = stripTags(actor.label()).split("\n");
            double totalH = lines.length * fm.getHeight();
            double yy = y + h/2 - totalH/2 + fm.getAscent();
            for (String line : lines) {
                int sw2 = fm.stringWidth(line);
                g2.drawString(line, (float)(x - sw2/2), (float)yy);
                yy += fm.getHeight();
            }
        }
    }

    private static String stripTags(String s){ return s.replaceAll("(?i)</?(?:b|strong|i|em|u|s|del)\\s*>", ""); }
}
