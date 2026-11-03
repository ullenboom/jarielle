package com.tutego.jarielle.er;

import com.tutego.jarielle.theme.ColorUtils;
import com.tutego.jarielle.theme.DiagramColors;
import com.tutego.jarielle.util.J2dText;
import com.tutego.jarielle.util.StyleConstants;
import com.tutego.jarielle.util.TextMetrics;

import java.awt.*;
import java.awt.geom.*;

/**
 * ER renderer — port of {@code src/er/renderer.ts}.
 */
public final class ErRenderer {

    private ErRenderer(){}

    public static void render(Graphics2D g2, PositionedErDiagram diagram, DiagramColors colors, String fontName) {
        var pal = ColorUtils.derive(colors);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (var rel: diagram.relationships()) drawRelationshipLine(g2, rel, pal);
        for (var e: diagram.entities()) drawEntityBox(g2, e, pal, fontName);
        for (var rel: diagram.relationships()) drawCardinality(g2, rel, pal);
        for (var rel: diagram.relationships()) drawLabel(g2, rel, pal, fontName);
    }

    private static void drawEntityBox(Graphics2D g2, PositionedErDiagram.PositionedErEntity e, ColorUtils.DerivedPalette pal, String fontName) {
        double x=e.x(), y=e.y(), w=e.width(), h=e.height(), headerH=e.headerHeight(), rowH=e.rowHeight();
        g2.setColor(pal.nodeFill());
        g2.fill(new Rectangle2D.Double(x,y,w,h));
        g2.setColor(pal.nodeStroke());
        g2.setStroke(new BasicStroke((float)StyleConstants.StrokeWidths.OUTER_BOX));
        g2.draw(new Rectangle2D.Double(x,y,w,h));
        g2.setColor(pal.groupHeader());
        g2.fill(new Rectangle2D.Double(x,y,w,headerH));
        g2.setColor(pal.nodeStroke());
        g2.draw(new Rectangle2D.Double(x,y,w,headerH));
        // name
        g2.setColor(pal.fg());
        Font nf = new Font(fontName, Font.BOLD, StyleConstants.FontSizes.NODE_LABEL);
        g2.setFont(nf);
        FontMetrics fm = g2.getFontMetrics();
        String label=e.label();
        int sw=fm.stringWidth(label);
        g2.drawString(label, (float)(x+w/2-sw/2), (float)(y+headerH/2+fm.getAscent()/2-2));
        double attrTop = y+headerH;
        g2.draw(new Line2D.Double(x, attrTop, x+w, attrTop));
        if (e.attributes().isEmpty()) {
            g2.setColor(pal.textFaint());
            Font f = new Font(fontName, Font.ITALIC, 11);
            g2.setFont(f);
            fm=g2.getFontMetrics();
            String txt="(no attributes)";
            sw=fm.stringWidth(txt);
            g2.drawString(txt, (float)(x+w/2-sw/2), (float)(attrTop+rowH/2+fm.getAscent()/2-2));
        } else {
            for (int i=0;i<e.attributes().size();i++) {
                var attr=e.attributes().get(i);
                double rowY = attrTop + i*rowH + rowH/2;
                // keys badge
                String keys = String.join(",", attr.keys());
                double keyW=0;
                if (!keys.isEmpty()) {
                    keyW = TextMetrics.measureTextWidth(keys, 9, 600)+8;
                    g2.setColor(pal.groupHeader()); // key badge uses groupHeader? original uses key-badge (fg 10%)
                    // use pal.innerStroke blended? approximate with groupHeader
                    g2.fill(new RoundRectangle2D.Double(x+6, rowY-7, keyW, 14,4,4));
                    g2.setColor(pal.textSec());
                    Font kf = new Font(fontName, Font.BOLD, 9);
                    g2.setFont(kf);
                    fm=g2.getFontMetrics();
                    int ksw=fm.stringWidth(keys);
                    g2.drawString(keys, (float)(x+6+keyW/2-ksw/2), (float)(rowY+fm.getAscent()/2-2));
                }
                // type
                double typeX = x+8 + (keyW>0? keyW+6:0);
                g2.setColor(pal.textMuted());
                Font mf = new Font(Font.MONOSPACED, Font.PLAIN, 11);
                g2.setFont(mf);
                fm=g2.getFontMetrics();
                g2.drawString(attr.type(), (float)typeX, (float)(rowY+fm.getAscent()/2-2));
                // name right aligned
                g2.setColor(pal.textSec());
                String name = attr.name();
                int nsw=fm.stringWidth(name);
                g2.drawString(name, (float)(x+w-8-nsw), (float)(rowY+fm.getAscent()/2-2));
            }
        }
    }

    private static void drawRelationshipLine(Graphics2D g2, PositionedErDiagram.PositionedErRelationship rel, ColorUtils.DerivedPalette pal) {
        if (rel.points().size()<2) return;
        g2.setColor(pal.line());
        Stroke s = rel.identifying() ? new BasicStroke((float)StyleConstants.StrokeWidths.CONNECTOR) : new BasicStroke((float)StyleConstants.StrokeWidths.CONNECTOR, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,10f,new float[]{6f,4f},0f);
        g2.setStroke(s);
        Path2D p = new Path2D.Double();
        p.moveTo(rel.points().get(0).x(), rel.points().get(0).y());
        for (int i=1;i<rel.points().size();i++) p.lineTo(rel.points().get(i).x(), rel.points().get(i).y());
        g2.draw(p);
    }

    private static void drawLabel(Graphics2D g2, PositionedErDiagram.PositionedErRelationship rel, ColorUtils.DerivedPalette pal, String fontName) {
        if (rel.label()==null || rel.points().size()<2) return;
        var pts = rel.points();
        double total=0;
        for(int i=1;i<pts.size();i++) total+= Math.hypot(pts.get(i).x()-pts.get(i-1).x(), pts.get(i).y()-pts.get(i-1).y());
        double half=total/2, walked=0;
        double mx=pts.get(0).x(), my=pts.get(0).y();
        for(int i=1;i<pts.size();i++){
            double seg=Math.hypot(pts.get(i).x()-pts.get(i-1).x(), pts.get(i).y()-pts.get(i-1).y());
            if(walked+seg>=half){
                double t=(half-walked)/seg;
                mx=pts.get(i-1).x()+(pts.get(i).x()-pts.get(i-1).x())*t;
                my=pts.get(i-1).y()+(pts.get(i).y()-pts.get(i-1).y())*t;
                break;
            }
            walked+=seg;
        }
        Font f=new Font(fontName, Font.PLAIN, StyleConstants.FontSizes.EDGE_LABEL);
        J2dText.drawCenteredWithBackground(g2, rel.label(), mx, my, f, pal.textMuted(), Color.WHITE, pal.innerStroke(), 4, 3, 4);
    }

    private static void drawCardinality(Graphics2D g2, PositionedErDiagram.PositionedErRelationship rel, ColorUtils.DerivedPalette pal) {
        if (rel.points().size()<2) return;
        drawCrowsFoot(g2, rel.points().get(0), rel.points().get(1), rel.cardinality1(), pal);
        drawCrowsFoot(g2, rel.points().get(rel.points().size()-1), rel.points().get(rel.points().size()-2), rel.cardinality2(), pal);
    }

    private static void drawCrowsFoot(Graphics2D g2, PositionedErDiagram.Point point, PositionedErDiagram.Point toward, String cardinality, ColorUtils.DerivedPalette pal) {
        double dx=point.x()-toward.x(), dy=point.y()-toward.y();
        double len=Math.hypot(dx,dy); if(len==0) return;
        double ux=dx/len, uy=dy/len, px=-uy, py=ux;
        double tipX=point.x()-ux*4, tipY=point.y()-uy*4;
        double backX=point.x()-ux*16, backY=point.y()-uy*16;
        boolean hasOne = "one".equals(cardinality) || "zero-one".equals(cardinality);
        boolean hasCrow = "many".equals(cardinality) || "zero-many".equals(cardinality);
        boolean hasCircle = "zero-one".equals(cardinality) || "zero-many".equals(cardinality);
        g2.setColor(pal.line());
        g2.setStroke(new BasicStroke(1.25f));
        if (hasOne) {
            double halfW=6;
            g2.draw(new Line2D.Double(tipX+px*halfW, tipY+py*halfW, tipX-px*halfW, tipY-py*halfW));
            double line2X=tipX-ux*4, line2Y=tipY-uy*4;
            g2.draw(new Line2D.Double(line2X+px*halfW, line2Y+py*halfW, line2X-px*halfW, line2Y-py*halfW));
        }
        if (hasCrow) {
            double fanW=7;
            g2.draw(new Line2D.Double(tipX+px*fanW, tipY+py*fanW, backX, backY));
            g2.draw(new Line2D.Double(tipX, tipY, backX, backY));
            g2.draw(new Line2D.Double(tipX-px*fanW, tipY-py*fanW, backX, backY));
        }
        if (hasCircle) {
            double off = hasCrow?20:12;
            double cx=point.x()-ux*off, cy=point.y()-uy*off;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(cx-4, cy-4,8,8));
            g2.setColor(pal.line());
            g2.draw(new Ellipse2D.Double(cx-4, cy-4,8,8));
        }
    }
}
