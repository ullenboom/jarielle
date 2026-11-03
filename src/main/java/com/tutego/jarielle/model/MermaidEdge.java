package com.tutego.jarielle.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Logical edge — port of {@code MermaidEdge} in src/types.ts.
 */
public record MermaidEdge(
        String source,
        String target,
        String label,
        EdgeStyle style,
        boolean hasArrowStart,
        boolean hasArrowEnd
) {
    public MermaidEdge {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(style, "style");
    }

    public Optional<String> labelOpt() {
        return Optional.ofNullable(label);
    }

    public boolean isSelfLoop() {
        return source.equals(target);
    }
}
