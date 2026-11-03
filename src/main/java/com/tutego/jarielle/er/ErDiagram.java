package com.tutego.jarielle.er;

import java.util.ArrayList;
import java.util.List;

public final class ErDiagram {
    private final List<ErEntity> entities = new ArrayList<>();
    private final List<ErRelationship> relationships = new ArrayList<>();

    public List<ErEntity> entities() { return entities; }
    public List<ErRelationship> relationships() { return relationships; }

    public record ErEntity(String id, String label, List<ErAttribute> attributes) {
        public ErEntity(String id, String label) { this(id, label, new ArrayList<>()); }
    }
    public record ErAttribute(String type, String name, List<String> keys, String comment) {}
    public record ErRelationship(String entity1, String entity2, String cardinality1, String cardinality2, String label, boolean identifying) {}
}
