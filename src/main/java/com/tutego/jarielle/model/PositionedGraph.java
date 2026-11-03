package com.tutego.jarielle.model;

import java.util.List;

/**
 * Final positioned graph ready for rendering — port of {@code PositionedGraph}.
 */
public record PositionedGraph(
        double width,
        double height,
        List<PositionedNode> nodes,
        List<PositionedEdge> edges,
        List<PositionedGroup> groups
) {
    public PositionedGraph {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        groups = List.copyOf(groups);
    }

    public static PositionedGraph empty() {
        return new PositionedGraph(0, 0, List.of(), List.of(), List.of());
    }
}
