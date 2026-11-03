package com.tutego.jarielle.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Positioned edge after layout — port of {@code PositionedEdge}.
 * Points are in absolute coordinates.
 */
public record PositionedEdge(
        String source,
        String target,
        String label,
        EdgeStyle style,
        boolean hasArrowStart,
        boolean hasArrowEnd,
        List<Point> points,
        Point labelPosition,
        Map<String, String> inlineStyle
) {
    public PositionedEdge {
        points = List.copyOf(points);
        if (inlineStyle != null) inlineStyle = Map.copyOf(inlineStyle);
    }

    public Optional<String> labelOpt() { return Optional.ofNullable(label); }
    public Optional<Point> labelPositionOpt() { return Optional.ofNullable(labelPosition); }
    public Optional<Map<String, String>> inlineStyleOpt() { return Optional.ofNullable(inlineStyle); }

    public PositionedEdge withPoints(List<Point> newPoints) {
        return new PositionedEdge(source, target, label, style, hasArrowStart, hasArrowEnd, newPoints, labelPosition, inlineStyle);
    }

    public PositionedEdge withLabelPosition(Point p) {
        return new PositionedEdge(source, target, label, style, hasArrowStart, hasArrowEnd, points, p, inlineStyle);
    }
}
