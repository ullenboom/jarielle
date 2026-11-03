package com.tutego.jarielle.model;

/**
 * Mermaid node shapes — 1:1 port of {@code NodeShape} in src/types.ts.
 */
public enum NodeShape {
    RECTANGLE("rectangle"),
    ROUNDED("rounded"),
    DIAMOND("diamond"),
    STADIUM("stadium"),
    CIRCLE("circle"),
    SUBROUTINE("subroutine"),
    DOUBLE_CIRCLE("doublecircle"),
    HEXAGON("hexagon"),
    CYLINDER("cylinder"),
    ASYMMETRIC("asymmetric"),
    TRAPEZOID("trapezoid"),
    TRAPEZOID_ALT("trapezoid-alt"),
    STATE_START("state-start"),
    STATE_END("state-end");

    private final String mermaidName;

    NodeShape(String mermaidName) {
        this.mermaidName = mermaidName;
    }

    public String mermaidName() {
        return mermaidName;
    }

    public static NodeShape fromMermaidName(String name) {
        for (var s : values()) {
            if (s.mermaidName.equals(name)) return s;
        }
        throw new IllegalArgumentException("Unknown NodeShape: " + name);
    }

    public boolean isRectangular() {
        return this == RECTANGLE || this == ROUNDED || this == STADIUM;
    }
}
