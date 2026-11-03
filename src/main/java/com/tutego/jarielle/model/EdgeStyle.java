package com.tutego.jarielle.model;

public enum EdgeStyle {
    SOLID,
    DOTTED,
    THICK;

    public static EdgeStyle fromArrowOp(String op) {
        return switch (op) {
            case "-.->", "-.-" -> DOTTED;
            case "==>", "===" -> THICK;
            default -> SOLID;
        };
    }

    public static EdgeStyle fromTextArrowOps(String open, String close) {
        if ("-.".equals(open) || ".->".equals(close) || "-.-".equals(close)) return DOTTED;
        if ("==".equals(open) || "==>".equals(close) || "===".equals(close)) return THICK;
        return SOLID;
    }
}
