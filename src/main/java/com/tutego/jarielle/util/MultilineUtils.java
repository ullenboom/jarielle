package com.tutego.jarielle.util;

import java.util.regex.Pattern;

/**
 * Port of src/multiline-utils.ts — normalization helpers.
 */
public final class MultilineUtils {

    private MultilineUtils() {}

    // Matches <br>, <br/>, <br /> and literal \n
    private static final Pattern BR_TAG = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);

    public static String normalizeBrTags(String label) {
        if (label == null) return "";
        String s = label;
        // Strip surrounding double quotes
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        s = BR_TAG.matcher(s).replaceAll("\n");
        s = s.replace("\\n", "\n");
        // Strip unsupported tags
        s = s.replaceAll("(?i)</?(?:sub|sup|small|mark)\\s*>", "");
        // Markdown → HTML
        s = s.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        s = s.replaceAll("(?<!\\*)\\*([^\\s*](?:[^*]*[^\\s*])?)\\*(?!\\*)", "<i>$1</i>");
        s = s.replaceAll("~~(.+?)~~", "<s>$1</s>");
        return s;
    }

    public static String stripFormattingTags(String text) {
        return text.replaceAll("(?i)</?(?:b|strong|i|em|u|s|del)\\s*>", "");
    }

    public static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
