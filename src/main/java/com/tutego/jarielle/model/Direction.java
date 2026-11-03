package com.tutego.jarielle.model;

/**
 * Mermaid flowchart direction, ported from {@code src/types.ts:Direction}.
 */
public enum Direction {
    TD,
    TB,
    LR,
    BT,
    RL;

    /**
     * Parses a Mermaid direction token (case-insensitive).
     * {@code TB} is normalized to {@code TD} as in the TS implementation.
     */
    public static Direction parse(String raw) {
        return switch (raw.toUpperCase()) {
            case "TD", "TB" -> TD;
            case "LR"      -> LR;
            case "RL"      -> RL;
            case "BT"      -> BT;
            default        -> throw new IllegalArgumentException("Unknown direction: " + raw);
        };
    }

    /** ELK direction string, mirroring {@code directionToElk} in layout-engine.ts. */
    public String toElk() {
        return switch (this) {
            case LR -> "RIGHT";
            case RL -> "LEFT";
            case BT -> "UP";
            case TD, TB -> "DOWN";
        };
    }

    public boolean isHorizontal() {
        return this == LR || this == RL;
    }
}
