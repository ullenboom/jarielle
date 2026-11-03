package com.tutego.jarielle.clazz;

import java.util.List;

/**
 * Positioned class diagram — port of {@code PositionedClassDiagram}.
 */
public record PositionedClassDiagram(double width, double height, List<PositionedClassNode> classes, List<PositionedClassRelationship> relationships) {
    public PositionedClassDiagram { classes = List.copyOf(classes); relationships = List.copyOf(relationships); }
    public record PositionedClassNode(String id, String label, String annotation, List<ClassDiagram.ClassMember> attributes, List<ClassDiagram.ClassMember> methods, double x, double y, double width, double height, double headerHeight, double attrHeight, double methodHeight) {
        public PositionedClassNode { attributes = List.copyOf(attributes); methods = List.copyOf(methods); }
    }
    public record PositionedClassRelationship(String from, String to, String type, String markerAt, String label, String fromCardinality, String toCardinality, List<Point> points, Point labelPosition) {
        public PositionedClassRelationship { points = List.copyOf(points); }
    }
    public record Point(double x, double y) {}
}
