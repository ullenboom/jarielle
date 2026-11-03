package com.tutego.jarielle.model;

import java.util.*;

/**
 * Parsed logical graph — port of {@code MermaidGraph} in src/types.ts.
 * Insertion order is preserved (LinkedHashMap / ArrayList) to mirror JS Map ordering.
 */
public final class MermaidGraph {

    private Direction direction;
    private final Map<String, MermaidNode> nodes = new LinkedHashMap<>();
    private final List<MermaidEdge> edges = new ArrayList<>();
    private final List<MermaidSubgraph> subgraphs = new ArrayList<>();
    private final Map<String, Map<String, String>> classDefs = new LinkedHashMap<>();
    private final Map<String, String> classAssignments = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> nodeStyles = new LinkedHashMap<>();
    /**
     * Key is either Integer edge index or the string "default".
     * We model it as Map&lt;Object,Map&lt;String,String&gt;&gt; to mirror TS {@code Map<number|'default',...>}
     */
    private final Map<Object, Map<String, String>> linkStyles = new LinkedHashMap<>();

    public MermaidGraph(Direction direction) {
        this.direction = Objects.requireNonNull(direction);
    }

    // --- direction ---

    public Direction direction() { return direction; }
    public void direction(Direction d) { this.direction = Objects.requireNonNull(d); }

    // --- nodes ---

    public Map<String, MermaidNode> nodes() { return nodes; }

    // --- edges ---

    public List<MermaidEdge> edges() { return edges; }

    // --- subgraphs ---

    public List<MermaidSubgraph> subgraphs() { return subgraphs; }

    // --- classDefs ---

    public Map<String, Map<String, String>> classDefs() { return classDefs; }

    // --- classAssignments ---

    public Map<String, String> classAssignments() { return classAssignments; }

    // --- nodeStyles ---

    public Map<String, Map<String, String>> nodeStyles() { return nodeStyles; }

    // --- linkStyles ---

    public Map<Object, Map<String, String>> linkStyles() { return linkStyles; }

    @Override
    public String toString() {
        return "MermaidGraph{direction=%s, nodes=%s, edges=%s, subgraphs=%s}"
                .formatted(direction, nodes.keySet(), edges, subgraphs);
    }
}
