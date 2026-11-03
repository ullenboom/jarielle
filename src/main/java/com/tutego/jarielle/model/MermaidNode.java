package com.tutego.jarielle.model;

import java.util.Objects;

/**
 * Logical node — port of {@code MermaidNode} in src/types.ts.
 */
public record MermaidNode(String id, String label, NodeShape shape) {

    public MermaidNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(shape, "shape");
        if (id.isBlank()) throw new IllegalArgumentException("id must not be blank");
    }
}
