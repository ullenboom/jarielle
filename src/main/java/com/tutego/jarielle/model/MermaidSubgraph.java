package com.tutego.jarielle.model;

import java.util.*;

/**
 * Logical subgraph / composite state — port of {@code MermaidSubgraph}.
 * Mutable during parsing, then treated as immutable.
 */
public final class MermaidSubgraph {

    private final String id;
    private String label;
    private final List<String> nodeIds = new ArrayList<>();
    private final List<MermaidSubgraph> children = new ArrayList<>();
    private Direction direction; // nullable = inherit

    public MermaidSubgraph(String id, String label) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
    }

    public String id() { return id; }
    public String label() { return label; }
    public void label(String label) { this.label = Objects.requireNonNull(label); }

    public List<String> nodeIds() { return nodeIds; }
    public List<MermaidSubgraph> children() { return children; }

    public Optional<Direction> direction() { return Optional.ofNullable(direction); }
    public void direction(Direction d) { this.direction = d; }
    public boolean hasDirectionOverride() { return direction != null; }

    @Override
    public String toString() {
        return "MermaidSubgraph{id='%s', label='%s', nodes=%s, children=%s, dir=%s}"
                .formatted(id, label, nodeIds, children, direction);
    }
}
