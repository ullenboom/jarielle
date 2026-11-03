package com.tutego.jarielle.model;

import java.util.Map;
import java.util.Optional;

/**
 * Positioned node after ELK layout — port of {@code PositionedNode}.
 */
public record PositionedNode(
        String id,
        String label,
        NodeShape shape,
        double x,
        double y,
        double width,
        double height,
        Map<String, String> inlineStyle
) {
    public PositionedNode {
        if (inlineStyle != null) inlineStyle = Map.copyOf(inlineStyle);
    }

    public Optional<Map<String, String>> inlineStyleOpt() {
        return Optional.ofNullable(inlineStyle);
    }

    public double centerX() { return x + width / 2.0; }
    public double centerY() { return y + height / 2.0; }

    public PositionedNode withPosition(double nx, double ny) {
        return new PositionedNode(id, label, shape, nx, ny, width, height, inlineStyle);
    }
}
