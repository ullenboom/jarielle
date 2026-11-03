package com.tutego.jarielle.ui;

import com.tutego.jarielle.model.EdgeStyle;
import com.tutego.jarielle.model.NodeShape;
import com.tutego.jarielle.model.Point;
import com.tutego.jarielle.model.PositionedEdge;
import com.tutego.jarielle.model.PositionedGraph;
import com.tutego.jarielle.model.PositionedGroup;
import com.tutego.jarielle.model.PositionedNode;
import com.tutego.jarielle.theme.ColorUtils;
import com.tutego.jarielle.theme.DiagramColors;
import com.tutego.jarielle.util.J2dText;
import com.tutego.jarielle.util.StyleConstants;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.*;
import java.util.List;

/**
 * Java2D renderer — port of {@code src/renderer.ts}.
 * Renders a {@link PositionedGraph} onto a {@link Graphics2D}.
 * <p>
 * Text rendering uses exclusively {@link J2dText} (real {@code FontMetrics});
 * ELK sizing uses {@code TextMetrics}. This ensures background pills are
 * perfectly centered (fix for ER label "is in").
 * </p>
 */
public final class GraphRenderer {

    private GraphRenderer() {}

    /**
     * Renders the graph.
     * @param g2 target graphics
     * @param graph positioned graph
     * @param colors diagram colors
     * @param fontName font family
     * @param transparent if true, no background is filled
     */
    public static void render(Graphics2D g2, PositionedGraph graph, DiagramColors colors, String fontName, boolean transparent) {
        var palette = ColorUtils.derive(colors);
        var hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHints(hints);

        if (!transparent) {
            g2.setColor(palette.bg());
            g2.fill(new Rectangle2D.Double(0, 0, graph.width(), graph.height()));
        }

        for (var group : graph.groups()) drawGroup(g2, group, palette, fontName);
        for (var edge : graph.edges()) drawEdge(g2, edge, palette);
        for (var edge : graph.edges()) if (edge.label() != null) drawEdgeLabel(g2, edge, palette, fontName);
        for (var node : graph.nodes()) drawNode(g2, node, palette, fontName);
    }

    private static void drawGroup(Graphics2D g2, PositionedGroup group, ColorUtils.DerivedPalette pal, String fontName) {
        double headerH = StyleConstants.FontSizes.GROUP_HEADER + 16;
        g2.setColor(pal.groupFill());
        g2.fill(new Rectangle2D.Double(group.x(), group.y(), group.width(), group.height()));
        g2.setColor(pal.nodeStroke());
        g2.setStroke(new BasicStroke((float) StyleConstants.StrokeWidths.OUTER_BOX));
        g2.draw(new Rectangle2D.Double(group.x(), group.y(), group.width(), group.height()));
        g2.setColor(pal.groupHeader());
        g2.fill(new Rectangle2D.Double(group.x(), group.y(), group.width(), headerH));
        g2.setColor(pal.nodeStroke());
        g2.draw(new Rectangle2D.Double(group.x(), group.y(), group.width(), headerH));
        if (group.label() != null && !group.label().isEmpty()) {
            Font font = new Font(fontName, Font.BOLD, StyleConstants.FontSizes.GROUP_HEADER);
            // Linksbündig, vertikal zentriert im Header — nutzt J2dText für Konsistenz
            String[] lines = group.label().split("\n");
            // Für linksbündig: x = group.x+12, y = header-Mitte
            double cx = group.x() + 12;
            double cy = group.y() + headerH/2;
            // Zeichne zeilenweise linksbündig, vertikal zentriert
            g2.setFont(font);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            double totalH = lines.length * fm.getHeight();
            double y = cy - totalH/2 + fm.getAscent();
            g2.setColor(pal.textSec());
            for (String line : lines) {
                g2.drawString(stripTags(line), (float)cx, (float)y);
                y += fm.getHeight();
            }
        }
        for (var child : group.children()) drawGroup(g2, child, pal, fontName);
    }

    private static void drawEdge(Graphics2D g2, PositionedEdge edge, ColorUtils.DerivedPalette pal) {
        if (edge.points().size() < 2) return;
        Color strokeCol = pal.line();
        float strokeW = (float) StyleConstants.StrokeWidths.CONNECTOR;
        if (edge.style() == EdgeStyle.THICK) strokeW *= 2;
        if (edge.inlineStyle() != null) {
            String s = edge.inlineStyle().get("stroke");
            if (s != null) { Color c = ColorUtils.parseHex(s); if (c != null) strokeCol = c; }
            String sw = edge.inlineStyle().get("stroke-width");
            if (sw != null) try { strokeW = Float.parseFloat(sw.replace("px","").trim()); } catch (NumberFormatException ignored) {}
        }
        g2.setColor(strokeCol);
        Stroke stroke = edge.style() == EdgeStyle.DOTTED ? new BasicStroke(strokeW, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f,4f}, 0f) : new BasicStroke(strokeW, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(stroke);
        Path2D path = new Path2D.Double();
        Point first = edge.points().get(0);
        path.moveTo(first.x(), first.y());
        for (int i=1;i<edge.points().size();i++) { Point p=edge.points().get(i); path.lineTo(p.x(), p.y()); }
        g2.draw(path);
        if (edge.hasArrowEnd() || edge.hasArrowStart()) {
            g2.setStroke(new BasicStroke(0.75f));
            Color arrowCol = edge.inlineStyle()!=null && edge.inlineStyle().containsKey("stroke") ? strokeCol : pal.arrow();
            g2.setColor(arrowCol);
            if (edge.hasArrowEnd()) drawArrowHead(g2, edge.points(), true);
            if (edge.hasArrowStart()) drawArrowHead(g2, edge.points(), false);
        }
    }

    private static void drawArrowHead(Graphics2D g2, List<Point> pts, boolean atEnd) {
        if (pts.size()<2) return;
        Point tip = atEnd ? pts.get(pts.size()-1) : pts.get(0);
        Point prev = atEnd ? pts.get(pts.size()-2) : pts.get(1);
        double dx=tip.x()-prev.x(), dy=tip.y()-prev.y(), len=Math.hypot(dx,dy);
        if (len<1e-6) return;
        double ux=dx/len, uy=dy/len, px=-uy, py=ux;
        double w=StyleConstants.ArrowHead.WIDTH, h=StyleConstants.ArrowHead.HEIGHT;
        Point base=new Point(tip.x()-ux*w, tip.y()-uy*w);
        Point left=new Point(base.x()+px*(h/2), base.y()+py*(h/2));
        Point right=new Point(base.x()-px*(h/2), base.y()-py*(h/2));
        Path2D tri=new Path2D.Double();
        tri.moveTo(tip.x(), tip.y());
        tri.lineTo(left.x(), left.y());
        tri.lineTo(right.x(), right.y());
        tri.closePath();
        g2.fill(tri); g2.draw(tri);
    }

    private static void drawEdgeLabel(Graphics2D g2, PositionedEdge edge, ColorUtils.DerivedPalette pal, String fontName) {
        String label=edge.label();
        if (label==null||label.isEmpty()) return;
        Point mid=edge.labelPosition()!=null?edge.labelPosition():calcMidpoint(edge.points());
        int fontSize=StyleConstants.FontSizes.EDGE_LABEL;
        Font font=new Font(fontName, Font.PLAIN, fontSize);
        // Einheitlich über J2dText: Hintergrund + zentrierter Text
        J2dText.drawCenteredWithBackground(g2, label, mid.x(), mid.y(), font, pal.textSec(), pal.bg(), pal.innerStroke(), 4, 3, 4);
    }

    private static Point calcMidpoint(List<Point> pts) {
        if (pts.isEmpty()) return new Point(0,0);
        if (pts.size()==1) return pts.get(0);
        double total=0; for(int i=1;i<pts.size();i++) total+=pts.get(i-1).distance(pts.get(i));
        double half=total/2, acc=0;
        for(int i=1;i<pts.size();i++){ double seg=pts.get(i-1).distance(pts.get(i)); if(acc+seg>=half){ double t=(half-acc)/seg; return new Point(pts.get(i-1).x()+t*(pts.get(i).x()-pts.get(i-1).x()), pts.get(i-1).y()+t*(pts.get(i).y()-pts.get(i-1).y())); } acc+=seg; }
        return pts.get(pts.size()-1);
    }

    private static void drawNode(Graphics2D g2, PositionedNode node, ColorUtils.DerivedPalette pal, String fontName) {
        Color fill=pal.nodeFill(), stroke=pal.nodeStroke();
        float sw=(float)StyleConstants.StrokeWidths.INNER_BOX;
        if (node.inlineStyle()!=null){
            String f=node.inlineStyle().get("fill"); if(f!=null){ Color c=ColorUtils.parseHex(f); if(c!=null) fill=c; }
            String s=node.inlineStyle().get("stroke"); if(s!=null){ Color c=ColorUtils.parseHex(s); if(c!=null) stroke=c; }
            String w=node.inlineStyle().get("stroke-width"); if(w!=null) try{ sw=Float.parseFloat(w.replace("px","").trim()); }catch(NumberFormatException ignored){}
        }
        Shape shape=createNodeShape(node);
        if (node.shape()==NodeShape.SUBROUTINE){
            g2.setColor(fill); g2.fill(new Rectangle2D.Double(node.x(), node.y(), node.width(), node.height()));
            g2.setColor(stroke); g2.setStroke(new BasicStroke(sw)); g2.draw(new Rectangle2D.Double(node.x(), node.y(), node.width(), node.height()));
            double inset=8; g2.draw(new Line2D.Double(node.x()+inset, node.y(), node.x()+inset, node.y()+node.height())); g2.draw(new Line2D.Double(node.x()+node.width()-inset, node.y(), node.x()+node.width()-inset, node.y()+node.height()));
        } else if (node.shape()==NodeShape.DOUBLE_CIRCLE){
            double cx=node.x()+node.width()/2, cy=node.y()+node.height()/2, outerR=Math.min(node.width(),node.height())/2, innerR=outerR-5;
            Ellipse2D outer=new Ellipse2D.Double(cx-outerR, cy-outerR, outerR*2, outerR*2), inner=new Ellipse2D.Double(cx-innerR, cy-innerR, innerR*2, innerR*2);
            g2.setColor(fill); g2.fill(outer); g2.setColor(stroke); g2.setStroke(new BasicStroke(sw)); g2.draw(outer); g2.draw(inner);
        } else if (node.shape()==NodeShape.CYLINDER){
            double ry=7, cx=node.x()+node.width()/2, bodyTop=node.y()+ry, bodyH=node.height()-2*ry;
            g2.setColor(fill); g2.fill(new Rectangle2D.Double(node.x(), bodyTop, node.width(), bodyH));
            g2.setColor(stroke); g2.setStroke(new BasicStroke(sw)); g2.draw(new Line2D.Double(node.x(), bodyTop, node.x(), bodyTop+bodyH)); g2.draw(new Line2D.Double(node.x()+node.width(), bodyTop, node.x()+node.width(), bodyTop+bodyH));
            Ellipse2D top=new Ellipse2D.Double(node.x(), bodyTop-ry, node.width(), ry*2), bottom=new Ellipse2D.Double(node.x(), node.y()+node.height()-ry*2, node.width(), ry*2);
            g2.setColor(fill); g2.fill(top); g2.fill(bottom); g2.setColor(stroke); g2.draw(top); g2.draw(bottom);
        } else if (node.shape()==NodeShape.STATE_START){
            double cx=node.x()+node.width()/2, cy=node.y()+node.height()/2, r=Math.min(node.width(),node.height())/2-2;
            Ellipse2D c=new Ellipse2D.Double(cx-r, cy-r, r*2, r*2); g2.setColor(pal.fg()); g2.fill(c);
        } else if (node.shape()==NodeShape.STATE_END){
            double cx=node.x()+node.width()/2, cy=node.y()+node.height()/2, outerR=Math.min(node.width(),node.height())/2-2, innerR=outerR-4;
            Ellipse2D outer=new Ellipse2D.Double(cx-outerR, cy-outerR, outerR*2, outerR*2), inner=new Ellipse2D.Double(cx-innerR, cy-innerR, innerR*2, innerR*2);
            g2.setColor(pal.fg()); g2.setStroke(new BasicStroke((float)(StyleConstants.StrokeWidths.INNER_BOX*2))); g2.draw(outer); g2.fill(inner);
        } else {
            if (shape!=null){ g2.setColor(fill); g2.fill(shape); g2.setColor(stroke); g2.setStroke(new BasicStroke(sw)); g2.draw(shape); }
        }
        if ((node.shape()==NodeShape.STATE_START||node.shape()==NodeShape.STATE_END) && (node.label()==null||node.label().isEmpty())) return;
        Color textColor=pal.fg();
        if (node.inlineStyle()!=null && node.inlineStyle().containsKey("color")){ Color c=ColorUtils.parseHex(node.inlineStyle().get("color")); if(c!=null) textColor=c; }
        int fontSize=StyleConstants.FontSizes.NODE_LABEL, weight=StyleConstants.FontWeights.NODE_LABEL;
        Font font=new Font(fontName, weight>=600?Font.BOLD:Font.PLAIN, fontSize);
        double cx=node.x()+node.width()/2, cy=node.y()+node.height()/2;
        J2dText.drawCentered(g2, node.label(), cx, cy, font, textColor);
    }

    private static Shape createNodeShape(PositionedNode node) {
        double x=node.x(), y=node.y(), w=node.width(), h=node.height();
        return switch(node.shape()){
            case RECTANGLE -> new Rectangle2D.Double(x,y,w,h);
            case ROUNDED -> new RoundRectangle2D.Double(x,y,w,h,12,12);
            case STADIUM -> new RoundRectangle2D.Double(x,y,w,h,h,h);
            case CIRCLE -> { double cx=x+w/2, cy=y+h/2, r=Math.min(w,h)/2; yield new Ellipse2D.Double(cx-r, cy-r, r*2, r*2); }
            case DIAMOND -> { double cx=x+w/2, cy=y+h/2; Path2D p=new Path2D.Double(); p.moveTo(cx,y); p.lineTo(x+w,cy); p.lineTo(cx,y+h); p.lineTo(x,cy); p.closePath(); yield p; }
            case HEXAGON -> { double inset=h/4; Path2D p=new Path2D.Double(); p.moveTo(x+inset,y); p.lineTo(x+w-inset,y); p.lineTo(x+w,y+h/2); p.lineTo(x+w-inset,y+h); p.lineTo(x+inset,y+h); p.lineTo(x,y+h/2); p.closePath(); yield p; }
            case ASYMMETRIC -> { double indent=12; Path2D p=new Path2D.Double(); p.moveTo(x+indent,y); p.lineTo(x+w,y); p.lineTo(x+w,y+h); p.lineTo(x+indent,y+h); p.lineTo(x,y+h/2); p.closePath(); yield p; }
            case TRAPEZOID -> { double inset=w*0.15; Path2D p=new Path2D.Double(); p.moveTo(x+inset,y); p.lineTo(x+w-inset,y); p.lineTo(x+w,y+h); p.lineTo(x,y+h); p.closePath(); yield p; }
            case TRAPEZOID_ALT -> { double inset=w*0.15; Path2D p=new Path2D.Double(); p.moveTo(x,y); p.lineTo(x+w,y); p.lineTo(x+w-inset,y+h); p.lineTo(x+inset,y+h); p.closePath(); yield p; }
            default -> new Rectangle2D.Double(x,y,w,h);
        };
    }

    private static String stripTags(String s){ return s.replaceAll("(?i)</?(?:b|strong|i|em|u|s|del)\\s*>", ""); }
}
