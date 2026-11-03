package com.tutego.jarielle.clazz;

import com.tutego.jarielle.layout.ElkLayoutEngine;
import com.tutego.jarielle.model.RenderOptions;
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
 * Class layout — port of {@code src/class/layout.ts}.
 */
public final class ClassLayout {

    private ClassLayout() {}

    private static class CLS {
        static final double padding = 40;
        static final double boxPadX = 8;
        static final double headerBaseHeight = 32;
        static final double annotationHeight = 16;
        static final double memberRowHeight = 20;
        static final double sectionPadY = 8;
        static final double emptySectionHeight = 8;
        static final double minWidth = 120;
        static final int memberFontSize = 11;
        static final int memberFontWeight = 400;
        static final double nodeSpacing = 40;
        static final double layerSpacing = 60;
    }

    public static PositionedClassDiagram layout(ClassDiagram diagram, RenderOptions options) {
        if (diagram.classes().isEmpty()) return new PositionedClassDiagram(0,0,List.of(),List.of());

        // sizes
        Map<String, double[]> sizeMap = new HashMap<>(); // width, height, headerHeight, attrHeight, methodHeight
        Map<String, double[]> detailMap = new HashMap<>();
        for (var cls : diagram.classes()) {
            double headerH = cls.annotation() != null ? CLS.headerBaseHeight + CLS.annotationHeight : CLS.headerBaseHeight;
            double attrH = cls.attributes().isEmpty() ? CLS.emptySectionHeight : cls.attributes().size()*CLS.memberRowHeight + CLS.sectionPadY;
            double methH = cls.methods().isEmpty() ? CLS.emptySectionHeight : cls.methods().size()*CLS.memberRowHeight + CLS.sectionPadY;
            double headerW = TextMetrics.measureTextWidth(cls.label(), StyleConstants.FontSizes.NODE_LABEL, StyleConstants.FontWeights.NODE_LABEL);
            double maxAttrW = maxMemberWidth(cls.attributes());
            double maxMethW = maxMemberWidth(cls.methods());
            double width = Math.max(CLS.minWidth, Math.max(headerW + CLS.boxPadX*2, Math.max(maxAttrW + CLS.boxPadX*2, maxMethW + CLS.boxPadX*2)));
            double height = headerH + attrH + methH;
            sizeMap.put(cls.id(), new double[]{width, height});
            detailMap.put(cls.id(), new double[]{headerH, attrH, methH});
        }

        // ELK graph
        ElkNode root = ElkGraphUtil.createGraph();
        root.setIdentifier("root");
        try { root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered"); } catch (Throwable ignored) {}
        root.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.DOWN);
        root.setProperty(CoreOptions.SPACING_NODE_NODE, CLS.nodeSpacing);
        try {
            var lo = Class.forName("org.eclipse.elk.alg.layered.options.LayeredOptions");
            var prop = (org.eclipse.elk.graph.properties.IProperty<Double>) lo.getField("SPACING_NODE_NODE_BETWEEN_LAYERS").get(null);
            root.setProperty(prop, CLS.layerSpacing);
        } catch (Throwable ignored) {}
        root.setProperty(CoreOptions.PADDING, new org.eclipse.elk.core.math.ElkPadding(CLS.padding, CLS.padding, CLS.padding, CLS.padding));
        root.setProperty(CoreOptions.EDGE_ROUTING, org.eclipse.elk.core.options.EdgeRouting.ORTHOGONAL);

        for (var cls : diagram.classes()) {
            double[] wh = sizeMap.get(cls.id());
            ElkNode n = ElkGraphUtil.createNode(root);
            n.setIdentifier(cls.id());
            n.setWidth(wh[0]); n.setHeight(wh[1]);
        }
        for (int i=0;i<diagram.relationships().size();i++) {
            var rel = diagram.relationships().get(i);
            ElkNode from = findNode(root, rel.from());
            ElkNode to = findNode(root, rel.to());
            ElkEdge e = ElkGraphUtil.createEdge(root);
            e.setIdentifier("e"+i);
            if (from!=null) e.getSources().add(from);
            if (to!=null) e.getTargets().add(to);
            if (rel.label()!=null) {
                var m = TextMetrics.measureMultilineText(rel.label(), StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.EDGE_LABEL);
                ElkLabel lbl = ElkGraphUtil.createLabel(e);
                lbl.setText(rel.label());
                lbl.setWidth(m.width()+8); lbl.setHeight(m.height()+6);
            }
        }
        // ensure providers
        try { Class.forName("org.eclipse.elk.core.data.LayoutMetaDataService").getMethod("getInstance").invoke(null); } catch (Throwable ignored) {}
        var engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new BasicProgressMonitor());

        // extract
        Map<String, ClassDiagram.ClassNode> lookup = new HashMap<>();
        for (var c: diagram.classes()) lookup.put(c.id(), c);
        List<PositionedClassDiagram.PositionedClassNode> positioned = new ArrayList<>();
        for (var child : root.getChildren()) {
            var cls = lookup.get(child.getIdentifier());
            if (cls==null) continue;
            double[] det = detailMap.get(cls.id());
            positioned.add(new PositionedClassDiagram.PositionedClassNode(cls.id(), cls.label(), cls.annotation(), cls.attributes(), cls.methods(), child.getX(), child.getY(), child.getWidth(), child.getHeight(), det[0], det[1], det[2]));
        }
        List<PositionedClassDiagram.PositionedClassRelationship> rels = new ArrayList<>();
        for (int i=0;i<root.getContainedEdges().size();i++) {
            var e = root.getContainedEdges().get(i);
            var rel = diagram.relationships().get(Integer.parseInt(e.getIdentifier().substring(1)));
            List<PositionedClassDiagram.Point> pts = new ArrayList<>();
            if (!e.getSections().isEmpty()) {
                var sec = e.getSections().get(0);
                pts.add(new PositionedClassDiagram.Point(sec.getStartX(), sec.getStartY()));
                for (var bp: sec.getBendPoints()) pts.add(new PositionedClassDiagram.Point(bp.getX(), bp.getY()));
                pts.add(new PositionedClassDiagram.Point(sec.getEndX(), sec.getEndY()));
            }
            PositionedClassDiagram.Point labelPos = null;
            if (!e.getLabels().isEmpty()) {
                var lbl = e.getLabels().get(0);
                labelPos = new PositionedClassDiagram.Point(lbl.getX()+lbl.getWidth()/2, lbl.getY()+lbl.getHeight()/2);
            }
            rels.add(new PositionedClassDiagram.PositionedClassRelationship(rel.from(), rel.to(), rel.type(), rel.markerAt(), rel.label(), rel.fromCardinality(), rel.toCardinality(), pts, labelPos));
        }
        return new PositionedClassDiagram(root.getWidth()>0?root.getWidth():600, root.getHeight()>0?root.getHeight():400, positioned, rels);
    }

    private static ElkNode findNode(ElkNode root, String id) {
        for (var c: root.getChildren()) if (id.equals(c.getIdentifier())) return c;
        return null;
    }

    private static double maxMemberWidth(List<ClassDiagram.ClassMember> members) {
        if (members.isEmpty()) return 0;
        double max=0;
        for (var m: members) {
            String txt = memberToString(m);
            double w = txt.length()* CLS.memberFontSize *0.6; // mono estimate
            if (w>max) max=w;
        }
        return max;
    }
    private static String memberToString(ClassDiagram.ClassMember m) {
        String vis = m.visibility()!=null && !m.visibility().isEmpty() ? m.visibility()+" " : "";
        String name = m.isMethod() ? m.name()+"("+(m.params()!=null?m.params():"")+")" : m.name();
        String type = m.type()!=null ? ": "+m.type() : "";
        return vis+name+type;
    }
}
