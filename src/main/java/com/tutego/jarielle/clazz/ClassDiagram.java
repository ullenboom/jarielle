package com.tutego.jarielle.clazz;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed class diagram — port of {@code ClassDiagram} in src/class/types.ts.
 */
public final class ClassDiagram {
    private final List<ClassNode> classes = new ArrayList<>();
    private final List<ClassRelationship> relationships = new ArrayList<>();
    private final List<ClassNamespace> namespaces = new ArrayList<>();

    public List<ClassNode> classes() { return classes; }
    public List<ClassRelationship> relationships() { return relationships; }
    public List<ClassNamespace> namespaces() { return namespaces; }

    public record ClassNode(String id, String label, String annotation, List<ClassMember> attributes, List<ClassMember> methods) {
        public ClassNode(String id, String label) { this(id, label, null, new ArrayList<>(), new ArrayList<>()); }
        public ClassNode(String id, String label, String annotation, List<ClassMember> attributes, List<ClassMember> methods) {
            this.id = id; this.label = label; this.annotation = annotation; this.attributes = attributes; this.methods = methods;
        }
    }
    public record ClassMember(String visibility, String name, String type, boolean isStatic, boolean isAbstract, boolean isMethod, String params) {}
    public record ClassRelationship(String from, String to, String type, String markerAt, String label, String fromCardinality, String toCardinality) {}
    public record ClassNamespace(String name, List<String> classIds) {}
}
