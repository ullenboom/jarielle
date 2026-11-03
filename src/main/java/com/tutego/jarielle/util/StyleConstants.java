package com.tutego.jarielle.util;

/**
 * Style constants — port of src/styles.ts.
 */
public final class StyleConstants {

    private StyleConstants() {}

    public static final class FontSizes {
        public static final int NODE_LABEL   = 13;
        public static final int EDGE_LABEL   = 11;
        public static final int GROUP_HEADER = 12;
        private FontSizes() {}
    }

    public static final class FontWeights {
        public static final int NODE_LABEL   = 500;
        public static final int EDGE_LABEL   = 400;
        public static final int GROUP_HEADER = 600;
        private FontWeights() {}
    }

    public static final class NodePadding {
        public static final int HORIZONTAL    = 20;
        public static final int VERTICAL      = 10;
        public static final int DIAMOND_EXTRA = 24;
        private NodePadding() {}
    }

    public static final class StrokeWidths {
        public static final double OUTER_BOX = 1.0;
        public static final double INNER_BOX = 0.75;
        public static final double CONNECTOR = 1.0;
        private StrokeWidths() {}
    }

    public static final double TEXT_BASELINE_SHIFT = 0.35;
    public static final int GROUP_HEADER_CONTENT_PAD = 12;

    public static final class ArrowHead {
        public static final double WIDTH  = 8;
        public static final double HEIGHT = 5;
        private ArrowHead() {}
    }
}
