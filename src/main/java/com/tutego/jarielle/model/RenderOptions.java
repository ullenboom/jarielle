package com.tutego.jarielle.model;

/**
 * Render / layout options — port of {@code RenderOptions} in src/types.ts.
 * All fields optional; defaults are provided via {@link #defaults()}.
 *
 * Java 25 record with compact constructor and builder.
 */
public record RenderOptions(
        String bg,
        String fg,
        String line,
        String accent,
        String muted,
        String surface,
        String border,
        String font,
        boolean transparent,
        double padding,
        double nodeSpacing,
        double layerSpacing,
        Double componentSpacing,
        Double thoroughness,
        boolean interactive
) {

    public static final RenderOptions DEFAULTS = new RenderOptions(
            "#FFFFFF", "#27272A",
            null, null, null, null, null,
            "Inter", false,
            40, 28, 48, null, 3.0, false
    );

    public static RenderOptions defaults() {
        return DEFAULTS;
    }

    /** Builder for ergonomic construction. */
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String bg;
        private String fg;
        private String line;
        private String accent;
        private String muted;
        private String surface;
        private String border;
        private String font = "Inter";
        private boolean transparent;
        private double padding = 40;
        private double nodeSpacing = 28;
        private double layerSpacing = 48;
        private Double componentSpacing;
        private Double thoroughness = 3.0;
        private boolean interactive;

        public Builder bg(String v) { this.bg = v; return this; }
        public Builder fg(String v) { this.fg = v; return this; }
        public Builder line(String v) { this.line = v; return this; }
        public Builder accent(String v) { this.accent = v; return this; }
        public Builder muted(String v) { this.muted = v; return this; }
        public Builder surface(String v) { this.surface = v; return this; }
        public Builder border(String v) { this.border = v; return this; }
        public Builder font(String v) { this.font = v; return this; }
        public Builder transparent(boolean v) { this.transparent = v; return this; }
        public Builder padding(double v) { this.padding = v; return this; }
        public Builder nodeSpacing(double v) { this.nodeSpacing = v; return this; }
        public Builder layerSpacing(double v) { this.layerSpacing = v; return this; }
        public Builder componentSpacing(Double v) { this.componentSpacing = v; return this; }
        public Builder thoroughness(Double v) { this.thoroughness = v; return this; }
        public Builder interactive(boolean v) { this.interactive = v; return this; }

        public RenderOptions build() {
            return new RenderOptions(bg, fg, line, accent, muted, surface, border,
                    font, transparent, padding, nodeSpacing, layerSpacing, componentSpacing, thoroughness, interactive);
        }
    }

    /** Merge with defaults — nulls fall back to defaults. */
    public RenderOptions mergedWithDefaults() {
        var d = DEFAULTS;
        return new RenderOptions(
                bg != null ? bg : d.bg,
                fg != null ? fg : d.fg,
                line, accent, muted, surface, border,
                font != null ? font : d.font,
                transparent,
                padding != 0 ? padding : d.padding,
                nodeSpacing != 0 ? nodeSpacing : d.nodeSpacing,
                layerSpacing != 0 ? layerSpacing : d.layerSpacing,
                componentSpacing, thoroughness, interactive
        );
    }

    // Convenience getters with defaults
    public String effectiveBg() { return bg != null ? bg : DEFAULTS.bg; }
    public String effectiveFg() { return fg != null ? fg : DEFAULTS.fg; }
    public String effectiveFont() { return font != null ? font : DEFAULTS.font; }
    public double effectivePadding() { return padding != 0 ? padding : DEFAULTS.padding; }
    public double effectiveNodeSpacing() { return nodeSpacing != 0 ? nodeSpacing : DEFAULTS.nodeSpacing; }
    public double effectiveLayerSpacing() { return layerSpacing != 0 ? layerSpacing : DEFAULTS.layerSpacing; }
}
