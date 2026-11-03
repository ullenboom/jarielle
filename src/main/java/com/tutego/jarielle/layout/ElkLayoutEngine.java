package com.tutego.jarielle.layout;

import com.tutego.jarielle.model.*;
import com.tutego.jarielle.util.ShapeClipper;
import com.tutego.jarielle.util.StyleConstants;
import com.tutego.jarielle.util.TextMetrics;

import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.properties.IProperty;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import java.util.*;

/**
 * Layout engine — Java port of {@code src/layout-engine.ts}.
 * Uses Eclipse Layout Kernel (ELK) via org.eclipse.elk.core.
 */
public final class ElkLayoutEngine {

    private ElkLayoutEngine() {}

    private static final double DEFAULT_PADDING = 40;
    private static final double DEFAULT_NODE_SPACING = 28;
    private static final double DEFAULT_LAYER_SPACING = 48;
    private static final boolean MERGE_EDGES = true;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static PositionedGraph layout(MermaidGraph graph, RenderOptions options) {
        if (graph == null) throw new IllegalArgumentException("graph is null");
        RenderOptions opts = options != null ? options.mergedWithDefaults() : RenderOptions.defaults();
        double padding = opts.effectivePadding();
        double nodeSpacing = opts.effectiveNodeSpacing();
        double layerSpacing = opts.effectiveLayerSpacing();

        // Build ELK graph
        ElkNode elkRoot = buildElkGraph(graph, opts);

        // Ensure ELK meta data providers are registered
        ensureElkProviders();

        // Configure layout options via CoreOptions / LayeredOptions
        boolean hasDirOverride = graph.subgraphs().stream().anyMatch(MermaidSubgraph::hasDirectionOverride);
        configureRoot(elkRoot, graph.direction(), padding, nodeSpacing, layerSpacing, hasDirOverride);

        // Run layout
        var engine = new RecursiveGraphLayoutEngine();
        engine.layout(elkRoot, new BasicProgressMonitor());

        // Convert back
        return elkToPositioned(elkRoot, graph, padding);
    }

    // -------------------------------------------------------------------------
    // ELK meta-data registration
    // -------------------------------------------------------------------------

    private static volatile boolean providersRegistered = false;

    private static void ensureElkProviders() {
        if (providersRegistered) return;
        synchronized (ElkLayoutEngine.class) {
            if (providersRegistered) return;
            try {
                var service = org.eclipse.elk.core.data.LayoutMetaDataService.getInstance();
                // Force ServiceLoader with multiple classloaders to ensure discovery in Surefire forked JVM
                // 1. Use context classloader
                try {
                    var ctxLoader = Thread.currentThread().getContextClassLoader();
                    var loader = java.util.ServiceLoader.load(org.eclipse.elk.core.data.ILayoutMetaDataProvider.class, ctxLoader);
                    for (var p : loader) try { service.registerLayoutMetaDataProviders(p); } catch (Throwable ignore) {}
                } catch (Throwable ignore) {}
                // 2. Use ElkLayoutEngine classloader
                try {
                    var cl = ElkLayoutEngine.class.getClassLoader();
                    var loader2 = java.util.ServiceLoader.load(org.eclipse.elk.core.data.ILayoutMetaDataProvider.class, cl);
                    for (var p : loader2) try { service.registerLayoutMetaDataProviders(p); } catch (Throwable ignore) {}
                } catch (Throwable ignore) {}
                // 3. Use LayoutMetaDataService classloader (ELK's original logic)
                try {
                    var cl3 = org.eclipse.elk.core.data.LayoutMetaDataService.class.getClassLoader();
                    if (cl3 != null) {
                        var loader3 = java.util.ServiceLoader.load(org.eclipse.elk.core.data.ILayoutMetaDataProvider.class, cl3);
                        for (var p : loader3) try { service.registerLayoutMetaDataProviders(p); } catch (Throwable ignore) {}
                    }
                } catch (Throwable ignore) {}

                // Explicit fallback: directly instantiate Layered provider (works even if ServiceLoader failed)
                if (service.getAlgorithmData("org.eclipse.elk.layered") == null) {
                    try {
                        var p = new org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider();
                        service.registerLayoutMetaDataProviders(p);
                    } catch (Throwable ignored) {
                        try {
                            Class<?> c = Class.forName("org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider");
                            Object provider = c.getDeclaredConstructor().newInstance();
                            service.registerLayoutMetaDataProviders((org.eclipse.elk.core.data.ILayoutMetaDataProvider) provider);
                        } catch (Exception ignored2) {}
                    }
                }
                // common options are provided by CoreOptions; no separate provider needed
                if (service.getOptionData("org.eclipse.elk.spacing.nodeNode") == null) {
                    // no-op, CoreOptions already covers it
                }
            } catch (Throwable t) {
                // ignore
            }
            providersRegistered = true;
        }
    }

    // -------------------------------------------------------------------------
    // Node sizing (mirrors TS estimateNodeSize)
    // -------------------------------------------------------------------------

    static double[] estimateNodeSize(String id, String label, NodeShape shape) {
        var metrics = TextMetrics.measureMultilineText(label, StyleConstants.FontSizes.NODE_LABEL, StyleConstants.FontWeights.NODE_LABEL);
        double width = metrics.width() + StyleConstants.NodePadding.HORIZONTAL * 2;
        double height = metrics.height() + StyleConstants.NodePadding.VERTICAL * 2;

        if (shape == NodeShape.DIAMOND) {
            double side = Math.max(width, height) + StyleConstants.NodePadding.DIAMOND_EXTRA;
            width = side;
            height = side;
        }
        if (shape == NodeShape.CIRCLE || shape == NodeShape.DOUBLE_CIRCLE) {
            double diameter = Math.ceil(Math.sqrt(width * width + height * height)) + 8;
            width = shape == NodeShape.DOUBLE_CIRCLE ? diameter + 12 : diameter;
            height = width;
        }
        if (shape == NodeShape.HEXAGON) {
            width += StyleConstants.NodePadding.HORIZONTAL;
        }
        if (shape == NodeShape.TRAPEZOID || shape == NodeShape.TRAPEZOID_ALT) {
            width += StyleConstants.NodePadding.HORIZONTAL;
        }
        if (shape == NodeShape.ASYMMETRIC) {
            width += 12;
        }
        if (shape == NodeShape.CYLINDER) {
            height += 14;
        }
        if (shape == NodeShape.STATE_START || shape == NodeShape.STATE_END) {
            return new double[]{28, 28};
        }
        width = Math.max(width, 60);
        height = Math.max(height, 36);
        return new double[]{width, height};
    }

    // -------------------------------------------------------------------------
    // Build ELK graph
    // -------------------------------------------------------------------------

    private static ElkNode buildElkGraph(MermaidGraph graph, RenderOptions opts) {
        // Collect subgraph node ids
        Set<String> subgraphNodeIds = new HashSet<>();
        Set<String> subgraphIds = new HashSet<>();
        for (var sg : graph.subgraphs()) {
            subgraphIds.add(sg.id());
            collectSubgraphNodeIds(sg, subgraphNodeIds, subgraphIds);
        }

        // Build node-to-subgraph map
        Map<String, String> nodeToSubgraph = buildNodeToSubgraphMap(graph.subgraphs());

        // Classify edges
        Map<String, List<IndexedEdge>> edgesBySubgraph = new HashMap<>();
        edgesBySubgraph.put(null, new ArrayList<>());
        List<IndexedEdge> crossEdges = new ArrayList<>();

        for (int i = 0; i < graph.edges().size(); i++) {
            var e = graph.edges().get(i);
            String srcSg = nodeToSubgraph.get(e.source());
            String tgtSg = nodeToSubgraph.get(e.target());
            if (srcSg != null && srcSg.equals(tgtSg)) {
                edgesBySubgraph.computeIfAbsent(srcSg, k -> new ArrayList<>()).add(new IndexedEdge(i, e));
            } else if (srcSg == null && tgtSg == null) {
                edgesBySubgraph.get(null).add(new IndexedEdge(i, e));
            } else {
                crossEdges.add(new IndexedEdge(i, e));
            }
        }

        boolean hasDirectionOverride = graph.subgraphs().stream().anyMatch(MermaidSubgraph::hasDirectionOverride);

        // Create root
        ElkNode root = ElkGraphUtil.createGraph();
        root.setIdentifier("root");

        // Keep lookup for nodes by id
        Map<String, ElkNode> elkNodeById = new HashMap<>();

        // Add top-level nodes
        for (var entry : graph.nodes().entrySet()) {
            String id = entry.getKey();
            if (subgraphNodeIds.contains(id) || subgraphIds.contains(id)) continue;
            var n = entry.getValue();
            double[] size = estimateNodeSize(id, n.label(), n.shape());
            ElkNode elkNode = ElkGraphUtil.createNode(root);
            elkNode.setIdentifier(id);
            elkNode.setWidth(size[0]);
            elkNode.setHeight(size[1]);
            ElkLabel lbl = ElkGraphUtil.createLabel(elkNode);
            lbl.setText(n.label());
            // store dimensions via ElkNode width/height already
            elkNodeById.put(id, elkNode);
        }

        // Add subgraphs
        for (var sg : graph.subgraphs()) {
            ElkNode sgNode = subgraphToElk(sg, graph, edgesBySubgraph, elkNodeById, root);
            // sgNode already added to root inside helper
        }

        // Add root-level edges
        for (var ie : edgesBySubgraph.get(null)) {
            createElkEdge(root, ie, elkNodeById, graph);
        }
        // Add cross-hierarchy edges (direct)
        for (var ie : crossEdges) {
            createElkEdge(root, ie, elkNodeById, graph);
        }

        return root;
    }

    private static ElkNode subgraphToElk(
            MermaidSubgraph sg,
            MermaidGraph graph,
            Map<String, List<IndexedEdge>> edgesBySubgraph,
            Map<String, ElkNode> elkNodeById,
            ElkNode parent) {

        ElkNode elkNode = ElkGraphUtil.createNode(parent);
        elkNode.setIdentifier(sg.id());
        // Store label
        if (sg.label() != null && !sg.label().isEmpty()) {
            ElkLabel lbl = ElkGraphUtil.createLabel(elkNode);
            lbl.setText(sg.label());
        }

        // Add direct child nodes
        for (String nid : sg.nodeIds()) {
            var n = graph.nodes().get(nid);
            if (n == null) continue;
            double[] size = estimateNodeSize(nid, n.label(), n.shape());
            ElkNode child = ElkGraphUtil.createNode(elkNode);
            child.setIdentifier(nid);
            child.setWidth(size[0]);
            child.setHeight(size[1]);
            ElkLabel lbl = ElkGraphUtil.createLabel(child);
            lbl.setText(n.label());
            elkNodeById.put(nid, child);
        }

        // Recursively add nested subgraphs
        for (var childSg : sg.children()) {
            subgraphToElk(childSg, graph, edgesBySubgraph, elkNodeById, elkNode);
        }

        // Add internal edges
        List<IndexedEdge> internal = edgesBySubgraph.getOrDefault(sg.id(), List.of());
        for (var ie : internal) {
            createElkEdge(elkNode, ie, elkNodeById, graph);
        }

        // Set layout options for this compound node
        configureCompound(elkNode, sg);

        return elkNode;
    }

    private static void createElkEdge(ElkNode container, IndexedEdge ie, Map<String, ElkNode> lookup, MermaidGraph graph) {
        var e = ie.edge();
        ElkNode src = lookup.get(e.source());
        ElkNode tgt = lookup.get(e.target());
        ElkEdge elkEdge = ElkGraphUtil.createEdge(container);
        elkEdge.setIdentifier("e" + ie.index());
        if (src != null) elkEdge.getSources().add(src);
        if (tgt != null) elkEdge.getTargets().add(tgt);
        if (e.label() != null && !e.label().isEmpty()) {
            ElkLabel lbl = ElkGraphUtil.createLabel(elkEdge);
            lbl.setText(e.label());
            var m = TextMetrics.measureMultilineText(e.label(), StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.EDGE_LABEL);
            lbl.setWidth(m.width() + 8);
            lbl.setHeight(m.height() + 6);
            // Port of TS: layoutOptions: { 'elk.edgeLabels.inline': 'true', 'elk.edgeLabels.placement': 'CENTER' }
            // Must be set on the label (and also globally on root). Without this label is placed beside (SIDE) not CENTER.
            setElkOption(lbl, "edgeLabels.inline", "true");
            setElkOption(lbl, "edgeLabels.placement", "CENTER");
            // Also try direct CoreOptions on label
            try { lbl.setProperty(CoreOptions.EDGE_LABELS_INLINE, true); } catch (Throwable ignored) {}
            try { lbl.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, org.eclipse.elk.core.options.EdgeLabelPlacement.CENTER); } catch (Throwable ignored) {}
        }
        try { ElkGraphUtil.updateContainment(elkEdge); } catch (Exception ignored) {}
    }

    private record IndexedEdge(int index, MermaidEdge edge) {}

    private static void collectSubgraphNodeIds(MermaidSubgraph sg, Set<String> out, Set<String> subgraphIds) {
        out.addAll(sg.nodeIds());
        for (var child : sg.children()) {
            subgraphIds.add(child.id());
            collectSubgraphNodeIds(child, out, subgraphIds);
        }
    }

    private static Map<String, String> buildNodeToSubgraphMap(List<MermaidSubgraph> subgraphs) {
        Map<String, String> map = new HashMap<>();
        for (var sg : subgraphs) traverseSg(sg, map);
        return map;
    }

    private static void traverseSg(MermaidSubgraph sg, Map<String, String> map) {
        for (String nid : sg.nodeIds()) map.put(nid, sg.id());
        for (var child : sg.children()) traverseSg(child, map);
    }

    // -------------------------------------------------------------------------
    // ELK configuration helpers
    // -------------------------------------------------------------------------

    // Generic ELK string-option setter (port of JS layoutOptions via color-mix/ elk.* keys)
    @SuppressWarnings("unchecked")
    private static void setElkOption(ElkGraphElement el, String suffix, String value) {
        try {
            var service = LayoutMetaDataService.getInstance();
            String id = "org.eclipse.elk." + suffix;
            var data = service.getOptionData(id);
            if (data == null) data = service.getOptionDataBySuffix(suffix);
            // fallback: try leaf suffix after last dot
            if (data == null) {
                int dot = suffix.lastIndexOf('.');
                if (dot >= 0) data = service.getOptionDataBySuffix(suffix.substring(dot + 1));
            }
            if (data != null) {
                Object parsed = data.parseValue(value);
                el.setProperty((IProperty<Object>) data, parsed);
            }
        } catch (Throwable ignored) {}
    }

    private static void configureRoot(ElkNode root, Direction dir, double padding, double nodeSpacing, double layerSpacing, boolean hasDirectionOverride) {
        // Ensure providers already registered
        ensureElkProviders();
        try { root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered"); } catch (Throwable t) { root.setProperty(CoreOptions.ALGORITHM, "layered"); }
        // Direction via CoreOptions (stable) + string fallback
        try {
            switch (dir) {
                case LR -> root.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.RIGHT);
                case RL -> root.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.LEFT);
                case BT -> root.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.UP);
                case TD, TB -> root.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.DOWN);
            }
        } catch (Throwable ignored) { setElkOption(root, "direction", dir.toElk()); }

        // Core spacings
        try { root.setProperty(CoreOptions.SPACING_NODE_NODE, nodeSpacing); } catch (Throwable ignored) {}
        try { root.setProperty(CoreOptions.SPACING_EDGE_EDGE, 12.0); } catch (Throwable ignored) {}
        try { root.setProperty(CoreOptions.SPACING_EDGE_NODE, 12.0); } catch (Throwable ignored) {}
        try { root.setProperty(CoreOptions.PADDING, new org.eclipse.elk.core.math.ElkPadding(padding, padding, padding, padding)); } catch (Throwable ignored) {}

        // Layered specifics via string API (mirrors layout-engine.ts exactly)
        setElkOption(root, "layered.spacing.nodeNodeBetweenLayers", String.valueOf(layerSpacing));
        setElkOption(root, "layered.spacing.edgeEdgeBetweenLayers", "12");
        setElkOption(root, "layered.spacing.edgeNodeBetweenLayers", "12");
        setElkOption(root, "spacing.nodeNode", String.valueOf(nodeSpacing));
        setElkOption(root, "spacing.edgeEdge", "12");
        setElkOption(root, "spacing.edgeNode", "12");
        setElkOption(root, "edgeRouting", "ORTHOGONAL");
        setElkOption(root, "layered.nodePlacement.bk.fixedAlignment", "BALANCED");
        setElkOption(root, "contentAlignment", "H_CENTER V_CENTER");
        setElkOption(root, "layered.thoroughness", "3");
        setElkOption(root, "layered.highDegreeNodes.treatment", "true");
        setElkOption(root, "layered.highDegreeNodes.threshold", "8");
        setElkOption(root, "layered.compaction.postCompaction.strategy", "LEFT_RIGHT_CONSTRAINT_LOCKING");
        setElkOption(root, "layered.considerModelOrder.strategy", "NODES_AND_EDGES");
        setElkOption(root, "layered.wrapping.strategy", "OFF");
        setElkOption(root, "layered.crossingMinimization.strategy", "LAYER_SWEEP");
        setElkOption(root, "layered.nodePlacement.strategy", "BRANDES_KOEPF");
        String hierarchy = hasDirectionOverride ? "SEPARATE" : "INCLUDE_CHILDREN";
        try { root.setProperty(CoreOptions.HIERARCHY_HANDLING, hasDirectionOverride ? HierarchyHandling.SEPARATE_CHILDREN : HierarchyHandling.INCLUDE_CHILDREN); } catch (Throwable ignored) { setElkOption(root, "hierarchyHandling", hierarchy); }
        setElkOption(root, "hierarchyHandling", hierarchy);
        // Edge labels global defaults — per-label inline will be set in createElkEdge
        setElkOption(root, "edgeLabels.inline", "true");
        setElkOption(root, "edgeLabels.placement", "CENTER");
    }

    private static void configureCompound(ElkNode compound, MermaidSubgraph sg) {
        try { compound.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered"); } catch (Throwable ignored) {}
        try {
            if (sg.hasDirectionOverride()) {
                Direction d = sg.direction().orElse(Direction.TD);
                switch (d) {
                    case LR -> compound.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.RIGHT);
                    case RL -> compound.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.LEFT);
                    case BT -> compound.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.UP);
                    case TD, TB -> compound.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.DOWN);
                }
                setElkOption(compound, "direction", d.toElk());
            }
        } catch (Throwable ignored) {}
        try { compound.setProperty(CoreOptions.PADDING, new org.eclipse.elk.core.math.ElkPadding(44, 16, 16, 16)); } catch (Throwable ignored) { setElkOption(compound, "padding", "[top=44,left=16,bottom=16,right=16]"); }
        try { compound.setProperty(CoreOptions.SPACING_NODE_NODE, 28.0); } catch (Throwable ignored) {}
        try { compound.setProperty(CoreOptions.SPACING_EDGE_EDGE, 12.0); } catch (Throwable ignored) {}
        try { compound.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL); } catch (Throwable ignored) { setElkOption(compound, "edgeRouting", "ORTHOGONAL"); }
        try { compound.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN); } catch (Throwable ignored) {}
        setElkOption(compound, "layered.spacing.nodeNodeBetweenLayers", "48");
        setElkOption(compound, "layered.spacing.edgeEdgeBetweenLayers", "12");
        setElkOption(compound, "layered.spacing.edgeNodeBetweenLayers", "12");
        setElkOption(compound, "layered.nodePlacement.bk.fixedAlignment", "BALANCED");
        setElkOption(compound, "contentAlignment", "H_CENTER V_CENTER");
        setElkOption(compound, "spacing.nodeNode", "28");
    }

    // -------------------------------------------------------------------------
    // ELK result -> PositionedGraph
    // -------------------------------------------------------------------------

    private static PositionedGraph elkToPositioned(ElkNode root, MermaidGraph graph, double padding) {
        // Build subgraph id set
        Set<String> subgraphIds = new HashSet<>();
        for (var sg : graph.subgraphs()) collectAllSubgraphIds(sg, subgraphIds);

        List<PositionedNode> nodes = new ArrayList<>();
        List<PositionedGroup> groups = new ArrayList<>();

        extractNodesAndGroups(root, graph, subgraphIds, nodes, groups, 0, 0);

        List<PositionedEdge> edges = new ArrayList<>();
        // Extract edges recursively (all levels)
        extractEdgesRecursively(root, graph, edges, 0, 0);

        // Align layers (port of alignLayerNodes)
        alignLayerNodes(nodes, edges, graph.direction());

        // Bundle edges if enabled
        if (MERGE_EDGES) {
            bundleEdgePaths(edges, nodes, groups, graph.direction());
        }

        // Shape clipping
        Map<String, PositionedNode> nodeMap = new HashMap<>();
        for (var n : nodes) nodeMap.put(n.id(), n);
        // Need to produce new edges list with clipped points
        List<PositionedEdge> clipped = new ArrayList<>();
        for (var e : edges) {
            List<Point> pts = e.points();
            PositionedNode s = nodeMap.get(e.source());
            PositionedNode t = nodeMap.get(e.target());
            if (s != null) pts = ShapeClipper.clipEdgeToShape(pts, s, true);
            if (t != null) {
                // need updated target node maybe same; but we clip against original
                PositionedNode tn = nodeMap.get(e.target());
                pts = ShapeClipper.clipEdgeToShape(pts, tn, false);
            }
            clipped.add(new PositionedEdge(e.source(), e.target(), e.label(), e.style(), e.hasArrowStart(), e.hasArrowEnd(), pts, e.labelPosition(), e.inlineStyle()));
        }
        edges = clipped;

        // Calculate bounds including edges
        double width = root.getWidth();
        double height = root.getHeight();
        if (width <= 0) width = 800;
        if (height <= 0) height = 600;
        double arrowMargin = StyleConstants.ArrowHead.WIDTH;
        for (var e : edges) {
            for (var p : e.points()) {
                width = Math.max(width, p.x() + arrowMargin + padding);
                height = Math.max(height, p.y() + arrowMargin + padding);
            }
            if (e.labelPosition() != null) {
                width = Math.max(width, e.labelPosition().x() + 60 + padding);
                height = Math.max(height, e.labelPosition().y() + 20 + padding);
            }
        }

        return new PositionedGraph(width, height, nodes, edges, groups);
    }

    private static void extractNodesAndGroups(
            ElkNode elkNode,
            MermaidGraph graph,
            Set<String> subgraphIds,
            List<PositionedNode> nodes,
            List<PositionedGroup> groups,
            double offsetX, double offsetY) {

        if (elkNode.getChildren() == null) return;
        for (ElkNode child : elkNode.getChildren()) {
            double x = child.getX() + offsetX;
            double y = child.getY() + offsetY;
            double w = child.getWidth();
            double h = child.getHeight();

            if (subgraphIds.contains(child.getIdentifier())) {
                List<PositionedGroup> childGroups = new ArrayList<>();
                extractNodesAndGroups(child, graph, subgraphIds, nodes, childGroups, x, y);
                MermaidSubgraph sg = findSubgraph(graph.subgraphs(), child.getIdentifier());
                String label = sg != null ? sg.label() : "";
                groups.add(new PositionedGroup(child.getIdentifier(), label, x, y, w, h, childGroups));
            } else {
                var mn = graph.nodes().get(child.getIdentifier());
                if (mn != null) {
                    Map<String, String> style = resolveNodeStyle(child.getIdentifier(), graph);
                    nodes.add(new PositionedNode(child.getIdentifier(), mn.label(), mn.shape(), x, y, w, h, style));
                }
                if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                    extractNodesAndGroups(child, graph, subgraphIds, nodes, groups, x, y);
                }
            }
        }
    }

    private static MermaidSubgraph findSubgraph(List<MermaidSubgraph> list, String id) {
        for (var sg : list) {
            if (sg.id().equals(id)) return sg;
            var found = findSubgraph(sg.children(), id);
            if (found != null) return found;
        }
        return null;
    }

    private static void collectAllSubgraphIds(MermaidSubgraph sg, Set<String> out) {
        out.add(sg.id());
        for (var child : sg.children()) collectAllSubgraphIds(child, out);
    }

    private static Map<String, String> resolveNodeStyle(String nodeId, MermaidGraph graph) {
        Map<String, String> result = null;
        String cls = graph.classAssignments().get(nodeId);
        if (cls != null) {
            Map<String, String> def = graph.classDefs().get(cls);
            if (def != null) result = new LinkedHashMap<>(def);
        }
        Map<String, String> ns = graph.nodeStyles().get(nodeId);
        if (ns != null) {
            if (result == null) result = new LinkedHashMap<>(ns);
            else result.putAll(ns);
        }
        return result;
    }

    private static Map<String, String> resolveEdgeStyle(int edgeIndex, MermaidGraph graph) {
        Map<String, String> result = null;
        Map<String, String> def = graph.linkStyles().get("default");
        if (def != null) result = new LinkedHashMap<>(def);
        Map<String, String> idx = graph.linkStyles().get(edgeIndex);
        if (idx != null) {
            if (result == null) result = new LinkedHashMap<>(idx);
            else result.putAll(idx);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Edge extraction
    // -------------------------------------------------------------------------

    private static void extractEdgesRecursively(ElkNode node, MermaidGraph graph, List<PositionedEdge> out, double offX, double offY) {
        // Collect edges at this node
        for (ElkEdge e : node.getContainedEdges()) {
            String id = e.getIdentifier(); // "e{index}"
            if (id == null || !id.startsWith("e")) continue;
            String numPart = id.substring(1);
            int idx;
            try { idx = Integer.parseInt(numPart.split("_")[0]); } catch (NumberFormatException ex) { continue; }
            if (idx < 0 || idx >= graph.edges().size()) continue;
            var original = graph.edges().get(idx);

            List<Point> points = new ArrayList<>();
            if (e.getSections().isEmpty()) continue;
            ElkEdgeSection sec = e.getSections().get(0);
            points.add(new Point(sec.getStartX() + offX, sec.getStartY() + offY));
            for (ElkBendPoint bp : sec.getBendPoints()) {
                points.add(new Point(bp.getX() + offX, bp.getY() + offY));
            }
            points.add(new Point(sec.getEndX() + offX, sec.getEndY() + offY));

            Point labelPos = null;
            if (original.label() != null && points.size() >= 2) {
                // ELK label position
                if (!e.getLabels().isEmpty()) {
                    ElkLabel l = e.getLabels().get(0);
                    // ELK labels have x,y relative to edge's container
                    labelPos = new Point(l.getX() + l.getWidth()/2 + offX, l.getY() + l.getHeight()/2 + offY);
                    // If label not placed (0,0 with no size), fallback to midpoint
                    if (labelPos.x() == offX && labelPos.y() == offY) labelPos = calcMidpoint(points);
                } else {
                    labelPos = calcMidpoint(points);
                }
            }

            out.add(new PositionedEdge(original.source(), original.target(), original.label(), original.style(),
                    original.hasArrowStart(), original.hasArrowEnd(), points, labelPos, resolveEdgeStyle(idx, graph)));
        }
        for (ElkNode child : node.getChildren()) {
            extractEdgesRecursively(child, graph, out, offX + child.getX(), offY + child.getY());
        }
    }

    private static Point calcMidpoint(List<Point> pts) {
        if (pts.isEmpty()) return new Point(0,0);
        if (pts.size() == 1) return pts.get(0);
        double total = 0;
        for (int i=1;i<pts.size();i++) total += pts.get(i-1).distance(pts.get(i));
        double half = total/2;
        double acc = 0;
        for (int i=1;i<pts.size();i++) {
            double seg = pts.get(i-1).distance(pts.get(i));
            if (acc + seg >= half) {
                double t = (half - acc)/seg;
                double x = pts.get(i-1).x() + t*(pts.get(i).x()-pts.get(i-1).x());
                double y = pts.get(i-1).y() + t*(pts.get(i).y()-pts.get(i-1).y());
                return new Point(x,y);
            }
            acc+=seg;
        }
        return pts.get(pts.size()-1);
    }

    // -------------------------------------------------------------------------
    // Layer alignment (port of alignLayerNodes)
    // -------------------------------------------------------------------------

    private static void alignLayerNodes(List<PositionedNode> nodes, List<PositionedEdge> edges, Direction dir) {
        if (nodes.isEmpty()) return;
        boolean isHoriz = dir.isHorizontal();
        Set<String> connected = new HashSet<>();
        for (var e : edges) {
            connected.add(e.source()+":"+e.target());
            connected.add(e.target()+":"+e.source());
        }
        double THRESHOLD = DEFAULT_LAYER_SPACING * 0.6;
        List<PositionedNode> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator.comparingDouble(n -> isHoriz ? n.x() : n.y()));

        List<List<PositionedNode>> layers = new ArrayList<>();
        List<PositionedNode> cur = new ArrayList<>();
        cur.add(sorted.get(0));
        for (int i=1;i<sorted.size();i++) {
            double pos = isHoriz ? sorted.get(i).x() : sorted.get(i).y();
            double prev = isHoriz ? sorted.get(i-1).x() : sorted.get(i-1).y();
            double gap = pos - prev;
            String curId = sorted.get(i).id();
            boolean hasEdgeToLayer = cur.stream().anyMatch(n -> connected.contains(n.id()+":"+curId));
            if (gap <= THRESHOLD && !hasEdgeToLayer) cur.add(sorted.get(i));
            else { layers.add(cur); cur = new ArrayList<>(); cur.add(sorted.get(i)); }
        }
        layers.add(cur);

        Map<String, Double> deltas = new HashMap<>();
        for (var layer : layers) {
            if (layer.size() <= 1) continue;
            double min = layer.stream().mapToDouble(n -> isHoriz ? n.x() : n.y()).min().orElse(0);
            double max = layer.stream().mapToDouble(n -> isHoriz ? n.x() : n.y()).max().orElse(0);
            if (max - min <= 1) continue;
            double target = (min+max)/2;
            for (var n : layer) {
                double old = isHoriz ? n.x() : n.y();
                double delta = target - old;
                if (Math.abs(delta) > 0.5) {
                    int idx = nodes.indexOf(n);
                    PositionedNode shifted = isHoriz ? n.withPosition(target, n.y()) : n.withPosition(n.x(), target);
                    nodes.set(idx, shifted);
                    deltas.put(n.id(), delta);
                }
            }
        }
        if (deltas.isEmpty()) return;
        // Adjust edge endpoints
        for (int i=0;i<edges.size();i++) {
            var e = edges.get(i);
            if (e.points().size()<2) continue;
            List<Point> pts = new ArrayList<>(e.points());
            Double sDelta = deltas.get(e.source());
            Double tDelta = deltas.get(e.target());
            if (sDelta != null) {
                Point fst = pts.get(0);
                if (isHoriz) {
                    pts.set(0, new Point(fst.x()+sDelta, fst.y()));
                    if (pts.size()>1 && pts.get(1).x() == fst.x()) {
                        Point sec = pts.get(1);
                        pts.set(1, new Point(sec.x()+sDelta, sec.y()));
                    }
                } else {
                    pts.set(0, new Point(fst.x(), fst.y()+sDelta));
                    if (pts.size()>1 && pts.get(1).y() == fst.y()) {
                        Point sec = pts.get(1);
                        pts.set(1, new Point(sec.x(), sec.y()+sDelta));
                    }
                }
            }
            if (tDelta != null) {
                int last = pts.size()-1;
                Point lst = pts.get(last);
                if (isHoriz) {
                    pts.set(last, new Point(lst.x()+tDelta, lst.y()));
                    if (pts.size()>1) {
                        Point prev = pts.get(last-1);
                        if (prev.x() == lst.x()) pts.set(last-1, new Point(prev.x()+tDelta, prev.y()));
                    }
                } else {
                    pts.set(last, new Point(lst.x(), lst.y()+tDelta));
                    if (pts.size()>1) {
                        Point prev = pts.get(last-1);
                        if (prev.y() == lst.y()) pts.set(last-1, new Point(prev.x(), prev.y()+tDelta));
                    }
                }
            }
            edges.set(i, new PositionedEdge(e.source(), e.target(), e.label(), e.style(), e.hasArrowStart(), e.hasArrowEnd(), pts, e.labelPosition(), e.inlineStyle()));
        }
    }

    // -------------------------------------------------------------------------
    // Edge bundling (port of bundleEdgePaths)
    // -------------------------------------------------------------------------

    private static void bundleEdgePaths(List<PositionedEdge> edges, List<PositionedNode> nodes, List<PositionedGroup> groups, Direction dir) {
        Map<String, PositionedNode> nodeMap = new HashMap<>();
        for (var n : nodes) nodeMap.put(n.id(), n);
        Set<PositionedEdge> processed = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean isLR = dir == Direction.LR;
        boolean isRL = dir == Direction.RL;
        boolean isBT = dir == Direction.BT;
        boolean isHoriz = isLR || isRL;

        Map<String, List<PositionedEdge>> fanOut = new HashMap<>();
        for (var e : edges) {
            if (e.source().equals(e.target())) continue;
            fanOut.computeIfAbsent(e.source(), k->new ArrayList<>()).add(e);
        }
        for (var entry : fanOut.entrySet()) {
            String srcId = entry.getKey();
            List<PositionedEdge> group = entry.getValue();
            if (group.size()<2) continue;
            EdgeStyle st = group.get(0).style();
            if (group.stream().anyMatch(e -> e.label()!=null || e.style()!=st)) continue;
            PositionedNode src = nodeMap.get(srcId);
            if (src==null) continue;
            List<PositionedEdge> forward = group.stream().filter(e -> {
                PositionedNode t = nodeMap.get(e.target());
                if (t==null) return false;
                if (isLR) return t.x() > src.x()+src.width();
                if (isRL) return t.x()+t.width() < src.x();
                if (isBT) return t.y()+t.height() < src.y();
                return t.y() > src.y()+src.height();
            }).toList();
            if (forward.size()<2) continue;

            double srcCX = src.centerX();
            double srcCY = src.centerY();

            if (isHoriz) {
                double exitX = isLR ? src.x()+src.width() : src.x();
                double exitY = srcCY;
                double nearestX = isLR ? forward.stream().mapToDouble(e -> nodeMap.get(e.target()).x()).min().orElse(exitX)
                        : forward.stream().mapToDouble(e -> nodeMap.get(e.target()).x()+nodeMap.get(e.target()).width()).max().orElse(exitX);
                double jX = exitX + (nearestX - exitX)/2;
                jX = adjustJunctionForGroups(jX, srcCX, srcCY, groups, dir);
                for (var e : forward) {
                    PositionedNode tgt = nodeMap.get(e.target());
                    double entryX = isLR ? tgt.x() : tgt.x()+tgt.width();
                    double entryY = tgt.centerY();
                    List<Point> pts = List.of(new Point(exitX, exitY), new Point(jX, exitY), new Point(jX, entryY), new Point(entryX, entryY));
                    int idx = edges.indexOf(e);
                    edges.set(idx, new PositionedEdge(e.source(), e.target(), e.label(), e.style(), e.hasArrowStart(), e.hasArrowEnd(), pts, e.labelPosition(), e.inlineStyle()));
                    processed.add(edges.get(idx));
                }
            } else {
                double exitX = srcCX;
                double exitY = isBT ? src.y() : src.y()+src.height();
                double nearestY = isBT ? forward.stream().mapToDouble(e -> nodeMap.get(e.target()).y()+nodeMap.get(e.target()).height()).max().orElse(exitY)
                        : forward.stream().mapToDouble(e -> nodeMap.get(e.target()).y()).min().orElse(exitY);
                double jY = exitY + (nearestY - exitY)/2;
                jY = adjustJunctionForGroups(jY, srcCX, srcCY, groups, dir);
                for (var e : forward) {
                    PositionedNode tgt = nodeMap.get(e.target());
                    double entryX = tgt.centerX();
                    double entryY = isBT ? tgt.y()+tgt.height() : tgt.y();
                    List<Point> pts = List.of(new Point(exitX, exitY), new Point(exitX, jY), new Point(entryX, jY), new Point(entryX, entryY));
                    int idx = edges.indexOf(e);
                    edges.set(idx, new PositionedEdge(e.source(), e.target(), e.label(), e.style(), e.hasArrowStart(), e.hasArrowEnd(), pts, e.labelPosition(), e.inlineStyle()));
                    processed.add(edges.get(idx));
                }
            }
        }
        // fan-in
        Map<String, List<PositionedEdge>> fanIn = new HashMap<>();
        for (var e : edges) {
            if (processed.contains(e) || e.source().equals(e.target())) continue;
            fanIn.computeIfAbsent(e.target(), k->new ArrayList<>()).add(e);
        }
        for (var entry : fanIn.entrySet()) {
            String tgtId = entry.getKey();
            List<PositionedEdge> group = entry.getValue();
            if (group.size()<2) continue;
            EdgeStyle st = group.get(0).style();
            if (group.stream().anyMatch(e -> e.label()!=null || e.style()!=st)) continue;
            PositionedNode tgt = nodeMap.get(tgtId);
            if (tgt==null) continue;
            List<PositionedEdge> forward = group.stream().filter(e -> {
                PositionedNode s = nodeMap.get(e.source());
                if (s==null) return false;
                if (isLR) return s.x()+s.width() < tgt.x();
                if (isRL) return s.x() > tgt.x()+tgt.width();
                if (isBT) return s.y() > tgt.y()+tgt.height();
                return s.y()+s.height() < tgt.y();
            }).toList();
            if (forward.size()<2) continue;

            double tgtCX = tgt.centerX();
            double tgtCY = tgt.centerY();
            if (isHoriz) {
                double entryX = isLR ? tgt.x() : tgt.x()+tgt.width();
                double entryY = tgtCY;
                double farthestX = isLR ? forward.stream().mapToDouble(e->nodeMap.get(e.source()).x()+nodeMap.get(e.source()).width()).max().orElse(entryX)
                        : forward.stream().mapToDouble(e->nodeMap.get(e.source()).x()).min().orElse(entryX);
                double jX = farthestX + (entryX - farthestX)/2;
                jX = adjustJunctionForGroups(jX, tgtCX, tgtCY, groups, dir);
                for (var e : forward) {
                    PositionedNode src = nodeMap.get(e.source());
                    double exitX = isLR ? src.x()+src.width() : src.x();
                    double exitY = src.centerY();
                    List<Point> pts = List.of(new Point(exitX, exitY), new Point(jX, exitY), new Point(jX, entryY), new Point(entryX, entryY));
                    int idx = edges.indexOf(e);
                    edges.set(idx, new PositionedEdge(e.source(), e.target(), e.label(), e.style(), e.hasArrowStart(), e.hasArrowEnd(), pts, e.labelPosition(), e.inlineStyle()));
                }
            } else {
                double entryX = tgtCX;
                double entryY = isBT ? tgt.y()+tgt.height() : tgt.y();
                double farthestY = isBT ? forward.stream().mapToDouble(e->nodeMap.get(e.source()).y()).min().orElse(entryY)
                        : forward.stream().mapToDouble(e->nodeMap.get(e.source()).y()+nodeMap.get(e.source()).height()).max().orElse(entryY);
                double jY = farthestY + (entryY - farthestY)/2;
                jY = adjustJunctionForGroups(jY, tgtCX, tgtCY, groups, dir);
                for (var e : forward) {
                    PositionedNode src = nodeMap.get(e.source());
                    double exitX = src.centerX();
                    double exitY = isBT ? src.y() : src.y()+src.height();
                    List<Point> pts = List.of(new Point(exitX, exitY), new Point(exitX, jY), new Point(entryX, jY), new Point(entryX, entryY));
                    int idx = edges.indexOf(e);
                    edges.set(idx, new PositionedEdge(e.source(), e.target(), e.label(), e.style(), e.hasArrowStart(), e.hasArrowEnd(), pts, e.labelPosition(), e.inlineStyle()));
                }
            }
        }
    }

    private static double adjustJunctionForGroups(double junctionMain, double refX, double refY, List<PositionedGroup> groups, Direction dir) {
        double GAP = 12;
        boolean isLR = dir == Direction.LR;
        boolean isRL = dir == Direction.RL;
        boolean isBT = dir == Direction.BT;
        boolean isHoriz = isLR || isRL;
        Set<String> refIds = new HashSet<>();
        for (var g : findGroupsContaining(refX, refY, groups)) refIds.add(g.id());
        double probeX = isHoriz ? junctionMain : refX;
        double probeY = isHoriz ? refY : junctionMain;
        for (var g : findGroupsContaining(probeX, probeY, groups)) {
            if (!refIds.contains(g.id())) {
                if (isLR) return g.x() - GAP;
                if (isRL) return g.x()+g.width()+GAP;
                if (isBT) return g.y()+g.height()+GAP;
                return g.y()-GAP;
            }
        }
        return junctionMain;
    }

    private static List<PositionedGroup> findGroupsContaining(double x, double y, List<PositionedGroup> groups) {
        List<PositionedGroup> res = new ArrayList<>();
        for (var g : groups) {
            if (g.contains(x,y)) {
                res.add(g);
                res.addAll(findGroupsContaining(x,y, g.children()));
            }
        }
        return res;
    }
}
