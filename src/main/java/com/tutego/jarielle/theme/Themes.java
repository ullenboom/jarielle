package com.tutego.jarielle.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 15 curated themes — port of {@code THEMES} in src/theme.ts.
 */
public final class Themes {

    private Themes() {}

    public static final Map<String, DiagramColors> THEMES;
    public static final DiagramColors DEFAULT_BG_FG = DiagramColors.of("#FFFFFF", "#27272A");

    static {
        Map<String, DiagramColors> m = new LinkedHashMap<>();
        m.put("zinc-light",       DiagramColors.of("#FFFFFF", "#27272A"));
        m.put("zinc-dark",        DiagramColors.of("#18181B", "#FAFAFA"));
        m.put("tokyo-night",      DiagramColors.of("#1a1b26", "#a9b1d6", "#3d59a1", "#7aa2f7", "#565f89", null, null));
        m.put("tokyo-night-storm",DiagramColors.of("#24283b", "#a9b1d6", "#3d59a1", "#7aa2f7", "#565f89", null, null));
        m.put("tokyo-night-light",DiagramColors.of("#d5d6db", "#343b58", "#34548a", "#34548a", "#9699a3", null, null));
        m.put("catppuccin-mocha", DiagramColors.of("#1e1e2e", "#cdd6f4", "#585b70", "#cba6f7", "#6c7086", null, null));
        m.put("catppuccin-latte", DiagramColors.of("#eff1f5", "#4c4f69", "#9ca0b0", "#8839ef", "#9ca0b0", null, null));
        m.put("nord",             DiagramColors.of("#2e3440", "#d8dee9", "#4c566a", "#88c0d0", "#616e88", null, null));
        m.put("nord-light",       DiagramColors.of("#eceff4", "#2e3440", "#aab1c0", "#5e81ac", "#7b88a1", null, null));
        m.put("dracula",          DiagramColors.of("#282a36", "#f8f8f2", "#6272a4", "#bd93f9", "#6272a4", null, null));
        m.put("github-light",     DiagramColors.of("#ffffff", "#1f2328", "#d1d9e0", "#0969da", "#59636e", null, null));
        m.put("github-dark",      DiagramColors.of("#0d1117", "#e6edf3", "#3d444d", "#4493f8", "#9198a1", null, null));
        m.put("solarized-light",  DiagramColors.of("#fdf6e3", "#657b83", "#93a1a1", "#268bd2", "#93a1a1", null, null));
        m.put("solarized-dark",   DiagramColors.of("#002b36", "#839496", "#586e75", "#268bd2", "#586e75", null, null));
        m.put("one-dark",         DiagramColors.of("#282c34", "#abb2bf", "#4b5263", "#c678dd", "#5c6370", null, null));
        THEMES = Collections.unmodifiableMap(m);
    }

    public static DiagramColors get(String name) {
        return THEMES.get(name);
    }

    public static Map<String, DiagramColors> all() {
        return THEMES;
    }
}
