package com.tutego.jarielle.er;

import com.tutego.jarielle.util.StyleConstants;
import com.tutego.jarielle.util.TextMetrics;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import java.util.*;

/**
 * ER layout — port of {@code src/er/layout.ts}.
 */
public final class ErLayout {

    private ErLayout(){}

    private static class ER {
        static final double padding=40, boxPadX=14, headerHeight=34, rowHeight=22, minWidth=140;
        static final int attrFontSize=11, attrFontWeight=400;
        static final double nodeSpacing=70, layerSpacing=90;
    }

    public static PositionedErDiagram layout(ErDiagram diagram) {
        if (diagram.entities().isEmpty()) return new PositionedErDiagram(0,0,List.of(),List.of());
        Map<String, double[]> sizeMap = new HashMap<>();
        for (var e: diagram.entities()) {
            double headerW = TextMetrics.measureTextWidth(e.label(), StyleConstants.FontSizes.NODE_LABEL, StyleConstants.FontWeights.NODE_LABEL);
            double maxAttrW=0;
            for (var attr: e.attributes()) {
                String txt = attr.type()+"  "+attr.name()+(attr.keys().isEmpty()?"":"  "+String.join(",",attr.keys()));
                double w = txt.length()*ER.attrFontSize*0.6;
                if (w>maxAttrW) maxAttrW=w;
            }
            double w = Math.max(ER.minWidth, Math.max(headerW + ER.boxPadX*2, maxAttrW + ER.boxPadX*2));
            double h = ER.headerHeight + Math.max(e.attributes().size(),1)*ER.rowHeight;
            sizeMap.put(e.id(), new double[]{w,h});
        }
        ElkNode root = ElkGraphUtil.createGraph();
        root.setIdentifier("root");
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.RIGHT);
        root.setProperty(CoreOptions.SPACING_NODE_NODE, ER.nodeSpacing);
        try {
            var lo = Class.forName("org.eclipse.elk.alg.layered.options.LayeredOptions");
            var prop = (org.eclipse.elk.graph.properties.IProperty<Double>) lo.getField("SPACING_NODE_NODE_BETWEEN_LAYERS").get(null);
            root.setProperty(prop, ER.layerSpacing);
        } catch (Throwable ignored){}
        root.setProperty(CoreOptions.PADDING, new org.eclipse.elk.core.math.ElkPadding(ER.padding, ER.padding, ER.padding, ER.padding));
        root.setProperty(CoreOptions.EDGE_ROUTING, org.eclipse.elk.core.options.EdgeRouting.ORTHOGONAL);

        for (var e: diagram.entities()) {
            double[] wh = sizeMap.get(e.id());
            ElkNode n = ElkGraphUtil.createNode(root);
            n.setIdentifier(e.id());
            n.setWidth(wh[0]); n.setHeight(wh[1]);
        }
        for (int i=0;i<diagram.relationships().size();i++) {
            var rel = diagram.relationships().get(i);
            ElkNode from = find(root, rel.entity1()), to = find(root, rel.entity2());
            ElkEdge edge = ElkGraphUtil.createEdge(root);
            edge.setIdentifier("e"+i);
            if (from!=null) edge.getSources().add(from);
            if (to!=null) edge.getTargets().add(to);
            if (rel.label()!=null) {
                var m = TextMetrics.measureMultilineText(rel.label(), StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.EDGE_LABEL);
                ElkLabel lbl = ElkGraphUtil.createLabel(edge);
                lbl.setText(rel.label());
                lbl.setWidth(m.width()+8); lbl.setHeight(m.height()+6);
            }
        }
        var engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new BasicProgressMonitor());
        Map<String, ErDiagram.ErEntity> lookup = new HashMap<>();
        for (var e: diagram.entities()) lookup.put(e.id(), e);
        List<PositionedErDiagram.PositionedErEntity> positioned = new ArrayList<>();
        for (var child: root.getChildren()) {
            var e = lookup.get(child.getIdentifier());
            if (e==null) continue;
            double[] wh = sizeMap.get(e.id());
            positioned.add(new PositionedErDiagram.PositionedErEntity(e.id(), e.label(), e.attributes(), child.getX(), child.getY(), child.getWidth(), child.getHeight(), ER.headerHeight, ER.rowHeight));
        }
        List<PositionedErDiagram.PositionedErRelationship> rels = new ArrayList<>();
        for (int i=0;i<root.getContainedEdges().size();i++) {
            var e = root.getContainedEdges().get(i);
            var rel = diagram.relationships().get(Integer.parseInt(e.getIdentifier().substring(1)));
            List<PositionedErDiagram.Point> pts = new ArrayList<>();
            if (!e.getSections().isEmpty()) {
                var sec = e.getSections().get(0);
                pts.add(new PositionedErDiagram.Point(sec.getStartX(), sec.getStartY()));
                for (var bp: sec.getBendPoints()) pts.add(new PositionedErDiagram.Point(bp.getX(), bp.getY()));
                pts.add(new PositionedErDiagram.Point(sec.getEndX(), sec.getEndY()));
            }
            rels.add(new PositionedErDiagram.PositionedErRelationship(rel.entity1(), rel.entity2(), rel.cardinality1(), rel.cardinality2(), rel.label(), rel.identifying(), pts));
        }
        return new PositionedErDiagram(root.getWidth()>0?root.getWidth():600, root.getHeight()>0?root.getHeight():400, positioned, rels);
    }

    private static ElkNode find(ElkNode root, String id) {
        for (var c: root.getChildren()) if (id.equals(c.getIdentifier())) return c;
        return null;
    }
}
