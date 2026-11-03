package com.tutego.jarielle.clazz;

import com.tutego.jarielle.util.MultilineUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class diagram parser — port of {@code src/class/parser.ts}.
 */
public final class ClassParser {

    private ClassParser() {}

    public static ClassDiagram parse(List<String> lines) {
        var diagram = new ClassDiagram();
        Map<String, ClassDiagram.ClassNode> classMap = new LinkedHashMap<>();
        ClassDiagram.ClassNamespace currentNamespace = null;
        ClassDiagram.ClassNode currentClass = null;
        int braceDepth = 0;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            if (currentClass != null && braceDepth > 0) {
                if ("}".equals(line)) {
                    braceDepth--;
                    if (braceDepth == 0) currentClass = null;
                    continue;
                }
                Matcher am = Pattern.compile("^<<(\\w+)>>$").matcher(line);
                if (am.matches()) {
                    // annotation mutable: need to recreate node with annotation
                    // Since record is immutable, we workaround by storing in map and updating via mutable wrapper
                    // For simplicity, maintain mutable annotation via map entry replacement
                    var old = classMap.get(currentClass.id());
                    var updated = new ClassDiagram.ClassNode(old.id(), old.label(), am.group(1), old.attributes(), old.methods());
                    classMap.put(old.id(), updated);
                    currentClass = updated;
                    continue;
                }
                var member = parseMember(line);
                if (member != null) {
                    if (member.isMethod()) currentClass.methods().add(member.member());
                    else currentClass.attributes().add(member.member());
                }
                continue;
            }

            Matcher nsM = Pattern.compile("^namespace\\s+(\\S+)\\s*\\{$").matcher(line);
            if (nsM.matches()) {
                currentNamespace = new ClassDiagram.ClassNamespace(nsM.group(1), new ArrayList<>());
                continue;
            }
            if ("}".equals(line) && currentNamespace != null) {
                diagram.namespaces().add(currentNamespace);
                currentNamespace = null;
                continue;
            }

            Matcher blockM = Pattern.compile("^class\\s+(\\S+?)(?:\\s*~(\\w+)~)?\\s*\\{$").matcher(line);
            if (blockM.matches()) {
                String id = blockM.group(1);
                String generic = blockM.group(2);
                var cls = ensureClass(classMap, id);
                if (generic != null) {
                    var old = classMap.get(id);
                    var updated = new ClassDiagram.ClassNode(old.id(), id + "<" + generic + ">", old.annotation(), old.attributes(), old.methods());
                    classMap.put(id, updated);
                    cls = updated;
                }
                currentClass = cls;
                braceDepth = 1;
                if (currentNamespace != null) currentNamespace.classIds().add(id);
                continue;
            }

            Matcher classOnlyM = Pattern.compile("^class\\s+(\\S+?)(?:\\s*~(\\w+)~)?\\s*$").matcher(line);
            if (classOnlyM.matches()) {
                String id = classOnlyM.group(1);
                String generic = classOnlyM.group(2);
                var cls = ensureClass(classMap, id);
                if (generic != null) {
                    var old = classMap.get(id);
                    var updated = new ClassDiagram.ClassNode(old.id(), id + "<" + generic + ">", old.annotation(), old.attributes(), old.methods());
                    classMap.put(id, updated);
                }
                if (currentNamespace != null) currentNamespace.classIds().add(id);
                continue;
            }

            Matcher inlineAnnotM = Pattern.compile("^class\\s+(\\S+?)\\s*\\{\\s*<<(\\w+)>>\\s*\\}$").matcher(line);
            if (inlineAnnotM.matches()) {
                var cls = ensureClass(classMap, inlineAnnotM.group(1));
                var old = classMap.get(cls.id());
                var updated = new ClassDiagram.ClassNode(old.id(), old.label(), inlineAnnotM.group(2), old.attributes(), old.methods());
                classMap.put(cls.id(), updated);
                continue;
            }

            Matcher inlineAttrM = Pattern.compile("^(\\S+?)\\s*:\\s*(.+)$").matcher(line);
            if (inlineAttrM.matches()) {
                String rest = inlineAttrM.group(2);
                if (!rest.matches(".*<\\|--.*|.*--.*|.*\\*--.*|.*o--.*|.*-->.*|.*\\.\\.>.*|.*\\.\\.\\|>.*")) {
                    var cls = ensureClass(classMap, inlineAttrM.group(1));
                    var member = parseMember(rest);
                    if (member != null) {
                        if (member.isMethod()) cls.methods().add(member.member());
                        else cls.attributes().add(member.member());
                    }
                    continue;
                }
            }

            var rel = parseRelationship(line);
            if (rel != null) {
                ensureClass(classMap, rel.from());
                ensureClass(classMap, rel.to());
                diagram.relationships().add(rel);
            }
        }
        diagram.classes().addAll(classMap.values());
        return diagram;
    }

    private static ClassDiagram.ClassNode ensureClass(Map<String, ClassDiagram.ClassNode> map, String id) {
        return map.computeIfAbsent(id, k -> new ClassDiagram.ClassNode(k, k, null, new ArrayList<>(), new ArrayList<>()));
    }

    private record ParsedMember(ClassDiagram.ClassMember member, boolean isMethod) {}

    private static ParsedMember parseMember(String line) {
        String trimmed = line.trim().replaceAll(";$", "");
        if (trimmed.isEmpty()) return null;
        String visibility = "";
        String rest = trimmed;
        if (rest.matches("^[+\\-#~].*")) { visibility = rest.substring(0,1); rest = rest.substring(1).trim(); }
        Matcher methodM = Pattern.compile("^(.+?)\\(([^)]*)\\)(?:\\s*(.+))?$").matcher(rest);
        if (methodM.matches()) {
            String name = methodM.group(1).trim();
            String params = methodM.group(2) != null ? methodM.group(2).trim() : null;
            if (params != null && params.isEmpty()) params = null;
            String type = methodM.group(3) != null ? methodM.group(3).trim() : null;
            boolean isStatic = name.endsWith("$") || rest.contains("$");
            boolean isAbstract = name.endsWith("*") || rest.contains("*");
            name = name.replaceAll("[$*]$", "");
            var m = new ClassDiagram.ClassMember(visibility, name, type, isStatic, isAbstract, true, params);
            return new ParsedMember(m, true);
        }
        String[] parts = rest.split("\\s+");
        String name, type = null;
        if (parts.length >= 2) { type = parts[0]; name = String.join(" ", Arrays.copyOfRange(parts,1,parts.length)); }
        else { name = parts[0]; }
        boolean isStatic = name.endsWith("$");
        boolean isAbstract = name.endsWith("*");
        name = name.replaceAll("[$*]$", "");
        var m = new ClassDiagram.ClassMember(visibility, name, type, isStatic, isAbstract, false, null);
        return new ParsedMember(m, false);
    }

    private static ClassDiagram.ClassRelationship parseRelationship(String line) {
        Matcher m = Pattern.compile("^(\\S+?)\\s+(?:\"([^\"]*?)\"\\s+)?(<\\|--|<\\|\\.\\.|\\*--|o--|-->|--\\*|--o|--\\|>|\\.\\.>|\\.\\.\\|>|<--|<\\.\\.?|--)\\s+(?:\"([^\"]*?)\"\\s+)?(\\S+?)(?:\\s*:\\s*(.+))?$").matcher(line);
        if (!m.matches()) return null;
        String from = m.group(1);
        String fromCard = m.group(2) != null ? MultilineUtils.normalizeBrTags(m.group(2)) : null;
        String arrow = m.group(3).trim();
        String toCard = m.group(4) != null ? MultilineUtils.normalizeBrTags(m.group(4)) : null;
        String to = m.group(5);
        String rawLabel = m.group(6) != null ? m.group(6).trim() : null;
        String label = rawLabel != null ? MultilineUtils.normalizeBrTags(rawLabel) : null;
        var parsed = parseArrow(arrow);
        if (parsed == null) return null;
        return new ClassDiagram.ClassRelationship(from, to, parsed.type, parsed.markerAt, label, fromCard, toCard);
    }

    private record ArrowInfo(String type, String markerAt) {}
    private static ArrowInfo parseArrow(String arrow) {
        String a = arrow.trim();
        return switch (a) {
            case "<|--" -> new ArrowInfo("inheritance","from");
            case "--|>" -> new ArrowInfo("inheritance","to");
            case "<|.." -> new ArrowInfo("realization","from");
            case "..|>" -> new ArrowInfo("realization","to");
            case "*--" -> new ArrowInfo("composition","from");
            case "--*" -> new ArrowInfo("composition","to");
            case "o--" -> new ArrowInfo("aggregation","from");
            case "--o" -> new ArrowInfo("aggregation","to");
            case "-->" -> new ArrowInfo("association","to");
            case "<--" -> new ArrowInfo("association","from");
            case "..>" -> new ArrowInfo("dependency","to");
            case "<.." -> new ArrowInfo("dependency","from");
            case "--" -> new ArrowInfo("association","to");
            default -> null;
        };
    }
}
