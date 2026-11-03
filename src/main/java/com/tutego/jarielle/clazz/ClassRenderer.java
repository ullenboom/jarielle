package com.tutego.jarielle.clazz;

import com.tutego.jarielle.theme.ColorUtils;
import com.tutego.jarielle.theme.DiagramColors;
import com.tutego.jarielle.util.J2dText;
import com.tutego.jarielle.util.StyleConstants;

import java.awt.*;
import java.awt.geom.*;

/**
 * Java2D renderer for class diagrams — port of {@code src/class/renderer.ts}.
 */
public final class ClassRenderer {

    private ClassRenderer(){}

    public static void render(Graphics2D g2, PositionedClassDiagram diagram, DiagramColors colors, String fontName) {
        var pal = ColorUtils.derive(colors);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // relationships behind
        for (var rel: diagram.relationships()) drawRelationship(g2, rel, pal);
        // boxes
        for (var cls: diagram.classes()) drawClassBox(g2, cls, pal, fontName);
        // labels
        for (var rel: diagram.relationships()) drawRelationshipLabels(g2, rel, pal, fontName);
    }

    private static void drawClassBox(Graphics2D g2, PositionedClassDiagram.PositionedClassNode cls, ColorUtils.DerivedPalette pal, String fontName) {
        double x=cls.x(), y=cls.y(), w=cls.width(), h=cls.height(), headerH=cls.headerHeight(), attrH=cls.attrHeight();
        g2.setColor(pal.nodeFill());
        g2.fill(new Rectangle2D.Double(x,y,w,h));
        g2.setColor(pal.nodeStroke());
        g2.setStroke(new BasicStroke((float)StyleConstants.StrokeWidths.OUTER_BOX));
        g2.draw(new Rectangle2D.Double(x,y,w,h));
        // header bg
        g2.setColor(pal.groupHeader());
        g2.fill(new Rectangle2D.Double(x,y,w,headerH));
        g2.setColor(pal.nodeStroke());
        g2.draw(new Rectangle2D.Double(x,y,w,headerH));
        // annotation
        double nameY = y + headerH/2;
        if (cls.annotation()!=null) {
            g2.setColor(pal.textMuted());
            Font af = new Font(fontName, Font.ITALIC, 10);
            g2.setFont(af);
            FontMetrics fm = g2.getFontMetrics();
            String txt = "<<" + cls.annotation() + ">>";
            int sw = fm.stringWidth(txt);
            g2.drawString(txt, (float)(x + w/2 - sw/2), (float)(y + 12 + fm.getAscent()/2));
            nameY = y + headerH/2 + 6;
        }
        // name
        g2.setColor(pal.fg());
        Font nf = new Font(fontName, Font.BOLD, StyleConstants.FontSizes.NODE_LABEL);
        g2.setFont(nf);
        FontMetrics fm = g2.getFontMetrics();
        String label = cls.label();
        int sw = fm.stringWidth(label);
        // support multiline? single line for now
        g2.drawString(label, (float)(x + w/2 - sw/2), (float)(nameY + fm.getAscent()/2 - 2));

        // divider header-attr
        double attrTop = y + headerH;
        g2.setColor(pal.nodeStroke());
        g2.setStroke(new BasicStroke((float)StyleConstants.StrokeWidths.INNER_BOX));
        g2.draw(new Line2D.Double(x, attrTop, x+w, attrTop));
        // attributes
        double rowH = 20;
        for (int i=0;i<cls.attributes().size();i++) {
            var m = cls.attributes().get(i);
            double my = attrTop + 4 + i*rowH + rowH/2;
            drawMember(g2, m, x+8, my, pal, fontName);
        }
        // divider attr-methods
        double methTop = attrTop + attrH;
        g2.draw(new Line2D.Double(x, methTop, x+w, methTop));
        for (int i=0;i<cls.methods().size();i++) {
            var m = cls.methods().get(i);
            double my = methTop + 4 + i*rowH + rowH/2;
            drawMember(g2, m, x+8, my, pal, fontName);
        }
    }

    private static void drawMember(Graphics2D g2, ClassDiagram.ClassMember m, double x, double y, ColorUtils.DerivedPalette pal, String fontName) {
        Font font = new Font(Font.MONOSPACED, (m.isAbstract()? Font.ITALIC:Font.PLAIN), 11);
        if (m.isStatic()) font = font.deriveFont(Font.PLAIN | Font.ITALIC); // approximate underline via italic? We'll underline via attribute
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        float currX = (float)x;
        if (m.visibility()!=null && !m.visibility().isEmpty()) {
            g2.setColor(pal.textFaint());
            String vis = m.visibility()+" ";
            g2.drawString(vis, currX, (float)(y + fm.getAscent()/2 -2));
            currX += fm.stringWidth(vis);
        }
        g2.setColor(pal.textSec());
        String name = m.isMethod() ? m.name()+"("+(m.params()!=null?m.params():"")+")" : m.name();
        g2.drawString(name, currX, (float)(y + fm.getAscent()/2 -2));
        if (m.isStatic()) {
            int sw = fm.stringWidth(name);
            g2.drawLine((int)currX, (int)(y+4), (int)(currX+sw), (int)(y+4));
        }
        currX += fm.stringWidth(name);
        if (m.type()!=null) {
            g2.setColor(pal.textFaint());
            g2.drawString(": ", currX, (float)(y + fm.getAscent()/2 -2));
            currX += fm.stringWidth(": ");
            g2.setColor(pal.textMuted());
            g2.drawString(m.type(), currX, (float)(y + fm.getAscent()/2 -2));
        }
    }

    private static void drawRelationship(Graphics2D g2, PositionedClassDiagram.PositionedClassRelationship rel, ColorUtils.DerivedPalette pal) {
        if (rel.points().size()<2) return;
        boolean isDashed = "dependency".equals(rel.type()) || "realization".equals(rel.type());
        Stroke s = isDashed ? new BasicStroke((float)StyleConstants.StrokeWidths.CONNECTOR, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f,4f},0f) : new BasicStroke((float)StyleConstants.StrokeWidths.CONNECTOR);
        g2.setColor(pal.line());
        g2.setStroke(s);
        Path2D p = new Path2D.Double();
        p.moveTo(rel.points().get(0).x(), rel.points().get(0).y());
        for (int i=1;i<rel.points().size();i++) p.lineTo(rel.points().get(i).x(), rel.points().get(i).y());
        g2.draw(p);
        // markers
        String type = rel.type();
        String markerAt = rel.markerAt();
        if ("inheritance".equals(type) || "realization".equals(type)) {
            drawHollowTriangle(g2, rel, pal);
        } else if ("composition".equals(type)) {
            drawFilledDiamond(g2, rel, pal);
        } else if ("aggregation".equals(type)) {
            drawHollowDiamond(g2, rel, pal);
        } else if ("association".equals(type) || "dependency".equals(type)) {
            drawOpenArrow(g2, rel, pal);
        }
    }

    private static void drawHollowTriangle(Graphics2D g2, PositionedClassDiagram.PositionedClassRelationship rel, ColorUtils.DerivedPalette pal) {
        var pts = rel.points();
        boolean atFrom = "from".equals(rel.markerAt());
        var tip = atFrom ? pts.get(0) : pts.get(pts.size()-1);
        var next = atFrom ? pts.get(1) : pts.get(pts.size()-2);
        double dx = tip.x()-next.x(), dy=tip.y()-next.y();
        double len=Math.hypot(dx,dy); if(len<1e-6) return;
        double ux=dx/len, uy=dy/len, px=-uy, py=ux;
        double w=12, h=10; // as in svg marker
        // triangle points: tip, base left/right back 12 along edge
        double baseX = tip.x() - ux*12, baseY = tip.y() - uy*12;
        Point2D p1 = new Point2D.Double(tip.x(), tip.y());
        Point2D p2 = new Point2D.Double(baseX + px*5, baseY + py*5);
        Point2D p3 = new Point2D.Double(baseX - px*5, baseY - py*5);
        Path2D tri = new Path2D.Double();
        tri.moveTo(p1.getX(), p1.getY());
        tri.lineTo(p2.getX(), p2.getY());
        tri.lineTo(p3.getX(), p3.getY());
        tri.closePath();
        g2.setColor(Color.WHITE);
        g2.fill(tri);
        g2.setColor(pal.arrow());
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(tri);
    }
    private static void drawFilledDiamond(Graphics2D g2, PositionedClassDiagram.PositionedClassRelationship rel, ColorUtils.DerivedPalette pal) {
        var pts = rel.points();
        boolean atFrom = "from".equals(rel.markerAt());
        var tip = atFrom ? pts.get(0) : pts.get(pts.size()-1);
        var next = atFrom ? pts.get(1) : pts.get(pts.size()-2);
        double dx = tip.x()-next.x(), dy=tip.y()-next.y();
        double len=Math.hypot(dx,dy); if(len<1e-6) return;
        double ux=dx/len, uy=dy/len, px=-uy, py=ux;
        double backX = tip.x() - ux*12, backY = tip.y() - uy*12;
        double midX = tip.x() - ux*6, midY = tip.y() - uy*6;
        Path2D d = new Path2D.Double();
        d.moveTo(tip.x(), tip.y());
        d.lineTo(midX + px*5, midY + py*5);
        d.lineTo(backX, backY);
        d.lineTo(midX - px*5, midY - py*5);
        d.closePath();
        g2.setColor(pal.arrow());
        g2.fill(d);
        g2.setColor(pal.arrow());
        g2.draw(d);
    }
    private static void drawHollowDiamond(Graphics2D g2, PositionedClassDiagram.PositionedClassRelationship rel, ColorUtils.DerivedPalette pal) {
        var pts = rel.points();
        boolean atFrom = "from".equals(rel.markerAt());
        var tip = atFrom ? pts.get(0) : pts.get(pts.size()-1);
        var next = atFrom ? pts.get(1) : pts.get(pts.size()-2);
        double dx = tip.x()-next.x(), dy=tip.y()-next.y();
        double len=Math.hypot(dx,dy); if(len<1e-6) return;
        double ux=dx/len, uy=dy/len, px=-uy, py=ux;
        double backX = tip.x() - ux*12, backY = tip.y() - uy*12;
        double midX = tip.x() - ux*6, midY = tip.y() - uy*6;
        Path2D d = new Path2D.Double();
        d.moveTo(tip.x(), tip.y());
        d.lineTo(midX + px*5, midY + py*5);
        d.lineTo(backX, backY);
        d.lineTo(midX - px*5, midY - py*5);
        d.closePath();
        g2.setColor(Color.WHITE);
        g2.fill(d);
        g2.setColor(pal.arrow());
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(d);
    }
    private static void drawOpenArrow(Graphics2D g2, PositionedClassDiagram.PositionedClassRelationship rel, ColorUtils.DerivedPalette pal) {
        var pts = rel.points();
        boolean atFrom = "from".equals(rel.markerAt());
        var tip = atFrom ? pts.get(0) : pts.get(pts.size()-1);
        var next = atFrom ? pts.get(1) : pts.get(pts.size()-2);
        double dx = tip.x()-next.x(), dy=tip.y()-next.y();
        double len=Math.hypot(dx,dy); if(len<1e-6) return;
        double ux=dx/len, uy=dy/len, px=-uy, py=ux;
        double baseX = tip.x() - ux*8, baseY = tip.y() - uy*8;
        g2.setColor(pal.arrow());
        g2.setStroke(new BasicStroke(1.5f));
        Path2D p = new Path2D.Double();
        p.moveTo(baseX + px*3, baseY + py*3);
        p.lineTo(tip.x(), tip.y());
        p.lineTo(baseX - px*3, baseY - py*3);
        g2.draw(p);
    }

    private static void drawRelationshipLabels(Graphics2D g2, PositionedClassDiagram.PositionedClassRelationship rel, ColorUtils.DerivedPalette pal, String fontName) {
        if (rel.label()!=null) {
            var pos = rel.labelPosition()!=null ? rel.labelPosition() : midpoint(rel.points());
            Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
            J2dText.drawCentered(g2, rel.label(), pos.x(), pos.y()-8, f, pal.textMuted());
        }
        if (rel.fromCardinality()!=null) {
            var p = rel.points().get(0);
            var nxt = rel.points().get(1);
            double[] off = cardinalityOffset(p, nxt);
            Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
            g2.setFont(f);
            g2.setColor(pal.textMuted());
            g2.drawString(rel.fromCardinality(), (float)(p.x()+off[0]), (float)(p.y()+off[1]));
        }
        if (rel.toCardinality()!=null) {
            var p = rel.points().get(rel.points().size()-1);
            var prev = rel.points().get(rel.points().size()-2);
            double[] off = cardinalityOffset(p, prev);
            Font f = new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
            g2.setFont(f);
            g2.setColor(pal.textMuted());
            g2.drawString(rel.toCardinality(), (float)(p.x()+off[0]), (float)(p.y()+off[1]));
        }
    }
    private static PositionedClassDiagram.Point midpoint(java.util.List<PositionedClassDiagram.Point> pts){ if(pts.isEmpty()) return new PositionedClassDiagram.Point(0,0); return pts.get(pts.size()/2); }
    private static double[] cardinalityOffset(PositionedClassDiagram.Point from, PositionedClassDiagram.Point to){
        double dx=to.x()-from.x(), dy=to.y()-from.y();
        if (Math.abs(dx)>Math.abs(dy)) return new double[]{dx>0?14:-14, -10};
        return new double[]{-14, dy>0?14:-14};
    }
}
