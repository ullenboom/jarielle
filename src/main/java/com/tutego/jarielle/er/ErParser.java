package com.tutego.jarielle.er;

import com.tutego.jarielle.util.MultilineUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ER parser — port of {@code src/er/parser.ts}.
 */
public final class ErParser {

    private ErParser(){}

    public static ErDiagram parse(List<String> lines) {
        var diagram = new ErDiagram();
        Map<String, ErDiagram.ErEntity> entityMap = new LinkedHashMap<>();
        ErDiagram.ErEntity current = null;

        for (int i=1;i<lines.size();i++) {
            String line = lines.get(i);
            if (current != null) {
                if ("}".equals(line)) { current = null; continue; }
                var attr = parseAttribute(line);
                if (attr != null) current.attributes().add(attr);
                continue;
            }
            Matcher em = Pattern.compile("^(\\S+)\\s*\\{$").matcher(line);
            if (em.matches()) {
                String id = em.group(1);
                current = ensureEntity(entityMap, id);
                continue;
            }
            var rel = parseRelationship(line);
            if (rel != null) {
                ensureEntity(entityMap, rel.entity1());
                ensureEntity(entityMap, rel.entity2());
                diagram.relationships().add(rel);
            }
        }
        diagram.entities().addAll(entityMap.values());
        return diagram;
    }

    private static ErDiagram.ErEntity ensureEntity(Map<String, ErDiagram.ErEntity> map, String id) {
        return map.computeIfAbsent(id, k -> new ErDiagram.ErEntity(k,k, new ArrayList<>()));
    }

    private static ErDiagram.ErAttribute parseAttribute(String line) {
        Matcher m = Pattern.compile("^(\\S+)\\s+(\\S+)(?:\\s+(.+))?$").matcher(line);
        if (!m.matches()) return null;
        String type = m.group(1);
        String name = m.group(2);
        String rest = m.group(3) != null ? m.group(3).trim() : "";
        List<String> keys = new ArrayList<>();
        String comment = null;
        Matcher cm = Pattern.compile("\"([^\"]*)\"").matcher(rest);
        if (cm.find()) comment = MultilineUtils.normalizeBrTags(cm.group(1));
        String withoutComment = rest.replaceAll("\"[^\"]*\"", "").trim();
        for (String part : withoutComment.split("\\s+")) {
            String up = part.toUpperCase();
            if ("PK".equals(up) || "FK".equals(up) || "UK".equals(up)) keys.add(up);
        }
        return new ErDiagram.ErAttribute(type, name, keys, comment);
    }

    private static ErDiagram.ErRelationship parseRelationship(String line) {
        Matcher m = Pattern.compile("^(\\S+)\\s+([|o}{]+(?:--|\\.\\.)[|o}{]+)\\s+(\\S+)\\s*:\\s*(.+)$").matcher(line);
        if (!m.matches()) return null;
        String e1 = m.group(1);
        String cardStr = m.group(2);
        String e2 = m.group(3);
        String rawLabel = m.group(4).trim().replaceAll("^[\"']|[\"']$","");
        String label = MultilineUtils.normalizeBrTags(rawLabel);
        Matcher lm = Pattern.compile("^([|o}{]+)(--|\\.\\.?)([|o}{]+)$").matcher(cardStr);
        if (!lm.matches()) return null;
        String left = lm.group(1), lineStyle = lm.group(2), right = lm.group(3);
        String c1 = parseCardinality(left);
        String c2 = parseCardinality(right);
        if (c1==null || c2==null) return null;
        boolean identifying = "--".equals(lineStyle);
        return new ErDiagram.ErRelationship(e1,e2,c1,c2,label,identifying);
    }

    private static String parseCardinality(String str) {
        String sorted = str.chars().sorted().collect(StringBuilder::new, (sb,c)->sb.append((char)c), StringBuilder::append).toString();
        // normalize: sort already does, but need to handle {o vs o{ same
        if ("||".equals(sorted)) return "one";
        if ("o|".equals(sorted)) return "zero-one";
        if ("|}".equals(sorted) || "{|".equals(sorted)) return "many";
        if ("{o".equals(sorted) || "o{".equals(sorted)) return "zero-many";
        return null;
    }
}
