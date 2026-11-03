package com.tutego.jarielle.model;

import java.util.List;

/**
 * Positioned subgraph group after layout — port of {@code PositionedGroup}.
 */
public record PositionedGroup(
        String id,
        String label,
        double x,
        double y,
        double width,
        double height,
        List<PositionedGroup> children
) {
    public PositionedGroup {
        children = List.copyOf(children);
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
