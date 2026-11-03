package com.tutego.jarielle.er;

import java.util.List;

public record PositionedErDiagram(double width, double height, List<PositionedErEntity> entities, List<PositionedErRelationship> relationships) {
    public PositionedErDiagram { entities = List.copyOf(entities); relationships = List.copyOf(relationships); }
    public record PositionedErEntity(String id, String label, List<ErDiagram.ErAttribute> attributes, double x, double y, double width, double height, double headerHeight, double rowHeight) {
        public PositionedErEntity { attributes = List.copyOf(attributes); }
    }
    public record PositionedErRelationship(String entity1, String entity2, String cardinality1, String cardinality2, String label, boolean identifying, List<Point> points) {
        public PositionedErRelationship { points = List.copyOf(points); }
    }
    public record Point(double x, double y) {}
}
