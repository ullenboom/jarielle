package com.tutego.jarielle.parser;

import com.tutego.jarielle.model.*;
import com.tutego.jarielle.util.MultilineUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mermaid parser — port of {@code src/parser.ts} (flowcharts + state diagrams).
 * <p>
 * Uses line-by-line regex approach; fully synchronous, preserves JS semantics.
 */
public final class MermaidParser {

    private MermaidParser() {}

    // -------------------------------------------------------------------------
    // Public entry
    // -------------------------------------------------------------------------

    public static MermaidGraph parseMermaid(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Empty mermaid diagram");

        // Split, trim, filter empty / comments
        List<String> lines = Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("%%"))
                .toList();

        if (lines.isEmpty()) throw new IllegalArgumentException("Empty mermaid diagram");

        String header = lines.get(0);
        if (Pattern.compile("^stateDiagram(-v2)?\\s*$", Pattern.CASE_INSENSITIVE).matcher(header).matches()) {
            return parseStateDiagram(lines);
        }
        return parseFlowchart(lines);
    }

    // -------------------------------------------------------------------------
    // Flowchart
    // -------------------------------------------------------------------------

    private static MermaidGraph parseFlowchart(List<String> lines) {
        Matcher hm = Pattern.compile("^(?:graph|flowchart)\\s+(TD|TB|LR|BT|RL)\\s*$", Pattern.CASE_INSENSITIVE).matcher(lines.get(0));
        if (!hm.matches()) {
            throw new IllegalArgumentException(
                    "Invalid mermaid header: \"" + lines.get(0) + "\". Expected \"graph TD\", \"flowchart LR\", \"stateDiagram-v2\", etc.");
        }
        Direction direction = Direction.parse(hm.group(1));
        var graph = new MermaidGraph(direction);

        Deque<MermaidSubgraph> stack = new ArrayDeque<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            // classDef
            Matcher mClassDef = Pattern.compile("^classDef\\s+(\\w+)\\s+(.+)$").matcher(line);
            if (mClassDef.matches()) {
                String name = mClassDef.group(1);
                String propsStr = mClassDef.group(2);
                graph.classDefs().put(name, parseStyleProps(propsStr));
                continue;
            }

            // class assignment: class A,B className
            Matcher mClassAssign = Pattern.compile("^class\\s+([\\w,\\-]+)\\s+(\\w+)$").matcher(line);
            if (mClassAssign.matches()) {
                String[] ids = mClassAssign.group(1).split(",");
                String className = mClassAssign.group(2);
                for (String id : ids) graph.classAssignments().put(id.trim(), className);
                continue;
            }

            // style
            Matcher mStyle = Pattern.compile("^style\\s+([\\w,\\-]+)\\s+(.+)$").matcher(line);
            if (mStyle.matches()) {
                String[] ids = mStyle.group(1).split(",");
                Map<String, String> props = parseStyleProps(mStyle.group(2));
                for (String id : ids) {
                    String key = id.trim();
                    graph.nodeStyles().merge(key, new LinkedHashMap<>(props), (oldV, newV) -> {
                        Map<String, String> merged = new LinkedHashMap<>(oldV);
                        merged.putAll(newV);
                        return merged;
                    });
                    // also need to ensure entry exists even if merge not triggered as above
                    graph.nodeStyles().computeIfAbsent(key, k -> new LinkedHashMap<>()).putAll(props);
                    // Above merge already handled; simplify: just putAll
                }
                // Fix: the above merge logic duplicated; do canonical:
                // Re-apply correctly: for each id, merge props
                // Already done via computeIfAbsent — but we did double. It's ok; values are same.
                continue;
            }

            // linkStyle
            Matcher mLinkStyle = Pattern.compile("^linkStyle\\s+(default|[\\d,\\s]+)\\s+(.+)$").matcher(line);
            if (mLinkStyle.matches()) {
                String target = mLinkStyle.group(1).trim();
                Map<String, String> props = parseStyleProps(mLinkStyle.group(2));
                if ("default".equals(target)) {
                    graph.linkStyles().merge("default", new LinkedHashMap<>(props), (a, b) -> {
                        Map<String, String> mm = new LinkedHashMap<>(a); mm.putAll(b); return mm;
                    });
                    graph.linkStyles().computeIfAbsent("default", k -> new LinkedHashMap<>()).putAll(props);
                } else {
                    for (String part : target.split(",")) {
                        String t = part.trim();
                        if (t.isEmpty()) continue;
                        try {
                            int idx = Integer.parseInt(t);
                            graph.linkStyles().merge(idx, new LinkedHashMap<>(props), (a, b) -> {
                                Map<String, String> mm = new LinkedHashMap<>(a); mm.putAll(b); return mm;
                            });
                            graph.linkStyles().computeIfAbsent(idx, k -> new LinkedHashMap<>()).putAll(props);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                continue;
            }

            // direction override inside subgraph
            Matcher mDir = Pattern.compile("^direction\\s+(TD|TB|LR|BT|RL)\\s*$", Pattern.CASE_INSENSITIVE).matcher(line);
            if (mDir.matches() && !stack.isEmpty()) {
                stack.peek().direction(Direction.parse(mDir.group(1)));
                continue;
            }

            // subgraph start
            Matcher mSub = Pattern.compile("^subgraph\\s+(.+)$").matcher(line);
            if (mSub.matches()) {
                String rest = mSub.group(1).trim();
                Matcher bracket = Pattern.compile("^([\\w\\-]+)\\s*\\[(.+)\\]$").matcher(rest);
                String id, label;
                if (bracket.matches()) {
                    id = bracket.group(1);
                    label = MultilineUtils.normalizeBrTags(bracket.group(2));
                } else {
                    label = MultilineUtils.normalizeBrTags(rest);
                    id = rest.replaceAll("\\s+", "_").replaceAll("[^\\w]", "");
                    if (id.isEmpty()) id = "sg_" + (graph.subgraphs().size() + stack.size());
                }
                stack.push(new MermaidSubgraph(id, label));
                continue;
            }

            if ("end".equals(line)) {
                if (!stack.isEmpty()) {
                    MermaidSubgraph completed = stack.pop();
                    if (!stack.isEmpty()) stack.peek().children().add(completed);
                    else graph.subgraphs().add(completed);
                }
                continue;
            }

            // Edge / node definitions
            parseEdgeLine(line, graph, stack);
            // Need to reflect stack changes back? parseEdgeLine only reads via list.
            // But nodeIds tracking must affect the actual stack objects — which are same references.
        }
        return graph;
    }

    // -------------------------------------------------------------------------
    // State diagram
    // -------------------------------------------------------------------------

    private static MermaidGraph parseStateDiagram(List<String> lines) {
        var graph = new MermaidGraph(Direction.TD);
        Deque<MermaidSubgraph> compositeStack = new ArrayDeque<>();
        Set<String> compositeIds = new HashSet<>();
        int[] startCount = {0};
        int[] endCount = {0};

        Pattern dirPat = Pattern.compile("^direction\\s+(TD|TB|LR|BT|RL)\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern linkStylePat = Pattern.compile("^linkStyle\\s+(default|[\\d,\\s]+)\\s+(.+)$");
        Pattern compositeOpen = Pattern.compile("^state\\s+(?:\"([^\"]+)\"\\s+as\\s+)?([\\w\\p{L}]+)\\s*\\{$", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern stateAlias = Pattern.compile("^state\\s+\"([^\"]+)\"\\s+as\\s+([\\w\\p{L}]+)\\s*$", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern transition = Pattern.compile("^(\\[\\*\\]|[\\w\\p{L}\\-]+)\\s*(-->)\\s*(\\[\\*\\]|[\\w\\p{L}\\-]+)(?:\\s*:\\s*(.+))?$", Pattern.UNICODE_CHARACTER_CLASS);
        Pattern stateDesc = Pattern.compile("^([\\w\\p{L}\\-]+)\\s*:\\s*(.+)$", Pattern.UNICODE_CHARACTER_CLASS);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            Matcher mDir = dirPat.matcher(line);
            if (mDir.matches()) {
                Direction d = Direction.parse(mDir.group(1));
                if (!compositeStack.isEmpty()) compositeStack.peek().direction(d);
                else graph.direction(d);
                continue;
            }

            Matcher mLs = linkStylePat.matcher(line);
            if (mLs.matches()) {
                String target = mLs.group(1).trim();
                Map<String, String> props = parseStyleProps(mLs.group(2));
                if ("default".equals(target)) {
                    graph.linkStyles().computeIfAbsent("default", k -> new LinkedHashMap<>()).putAll(props);
                } else {
                    for (String p : target.split(",")) {
                        String t = p.trim();
                        if (t.isEmpty()) continue;
                        try {
                            int idx = Integer.parseInt(t);
                            graph.linkStyles().computeIfAbsent(idx, k -> new LinkedHashMap<>()).putAll(props);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                continue;
            }

            Matcher mComp = compositeOpen.matcher(line);
            if (mComp.matches()) {
                String label = mComp.group(1) != null ? mComp.group(1) : mComp.group(2);
                String id = mComp.group(2);
                MermaidSubgraph sg = new MermaidSubgraph(id, label);
                compositeStack.push(sg);
                compositeIds.add(id);
                graph.nodes().remove(id);
                continue;
            }

            if ("}".equals(line)) {
                if (!compositeStack.isEmpty()) {
                    MermaidSubgraph completed = compositeStack.pop();
                    if (!compositeStack.isEmpty()) compositeStack.peek().children().add(completed);
                    else graph.subgraphs().add(completed);
                }
                continue;
            }

            Matcher mAlias = stateAlias.matcher(line);
            if (mAlias.matches()) {
                String label = MultilineUtils.normalizeBrTags(mAlias.group(1));
                String id = mAlias.group(2);
                registerStateNode(graph, compositeStack, new MermaidNode(id, label, NodeShape.ROUNDED));
                continue;
            }

            Matcher mTrans = transition.matcher(line);
            if (mTrans.matches()) {
                String sId = mTrans.group(1);
                String tId = mTrans.group(3);
                String rawLabel = mTrans.group(4);
                String edgeLabel = rawLabel != null ? MultilineUtils.normalizeBrTags(rawLabel.trim()) : null;
                if (edgeLabel != null && edgeLabel.isEmpty()) edgeLabel = null;

                if ("[*]".equals(sId)) {
                    startCount[0]++;
                    sId = "_start" + (startCount[0] > 1 ? startCount[0] : "");
                    registerStateNode(graph, compositeStack, new MermaidNode(sId, "", NodeShape.STATE_START));
                } else if (!compositeIds.contains(sId)) {
                    ensureStateNode(graph, compositeStack, sId);
                }

                if ("[*]".equals(tId)) {
                    endCount[0]++;
                    tId = "_end" + (endCount[0] > 1 ? endCount[0] : "");
                    registerStateNode(graph, compositeStack, new MermaidNode(tId, "", NodeShape.STATE_END));
                } else if (!compositeIds.contains(tId)) {
                    ensureStateNode(graph, compositeStack, tId);
                }

                graph.edges().add(new MermaidEdge(sId, tId, edgeLabel, EdgeStyle.SOLID, false, true));
                continue;
            }

            Matcher mDesc = stateDesc.matcher(line);
            if (mDesc.matches()) {
                String id = mDesc.group(1);
                String label = MultilineUtils.normalizeBrTags(mDesc.group(2).trim());
                registerStateNode(graph, compositeStack, new MermaidNode(id, label, NodeShape.ROUNDED));
            }
        }
        return graph;
    }

    private static void registerStateNode(MermaidGraph graph, Deque<MermaidSubgraph> stack, MermaidNode node) {
        boolean isNew = !graph.nodes().containsKey(node.id());
        if (isNew) graph.nodes().put(node.id(), node);
        if (!stack.isEmpty()) {
            MermaidSubgraph cur = stack.peek();
            if (!cur.nodeIds().contains(node.id())) cur.nodeIds().add(node.id());
        }
    }

    private static void ensureStateNode(MermaidGraph graph, Deque<MermaidSubgraph> stack, String id) {
        if (!graph.nodes().containsKey(id)) {
            registerStateNode(graph, stack, new MermaidNode(id, id, NodeShape.ROUNDED));
        } else if (!stack.isEmpty()) {
            MermaidSubgraph cur = stack.peek();
            if (!cur.nodeIds().contains(id)) cur.nodeIds().add(id);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    static Map<String, String> parseStyleProps(String propsStr) {
        String cleaned = propsStr.replaceAll(";\\s*$", "");
        Map<String, String> props = new LinkedHashMap<>();
        for (String pair : cleaned.split(",")) {
            int colon = pair.indexOf(':');
            if (colon > 0) {
                String k = pair.substring(0, colon).trim();
                String v = pair.substring(colon + 1).trim();
                if (!k.isEmpty() && !v.isEmpty()) props.put(k, v);
            }
        }
        return props;
    }

    // -------------------------------------------------------------------------
    // Flowchart edge line parser
    // -------------------------------------------------------------------------

    private static final Pattern ARROW_REGEX = Pattern.compile("^(<)?(-->|-.->|==>|---|-\\.\\-|===)(?:\\|([^|]*)\\|)?");
    private static final Pattern TEXT_ARROW_REGEX = Pattern.compile("^(<)?(--|-\\.|==)\\s+(.+?)\\s+(-->|---|\\.->|-\\.\\-|==>|===)");

    // Node patterns ordered most specific first
    private record NodePattern(Pattern regex, NodeShape shape) {}

    private static final List<NodePattern> NODE_PATTERNS = List.of(
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\(\\(\\((.+?)\\)\\)\\)"), NodeShape.DOUBLE_CIRCLE),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\(\\[(.+?)\\]\\)"), NodeShape.STADIUM),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\(\\((.+?)\\)\\)"), NodeShape.CIRCLE),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\[\\[(.+?)\\]\\]"), NodeShape.SUBROUTINE),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\[\\((.+?)\\)\\]"), NodeShape.CYLINDER),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\[/(.+?)\\\\\\]"), NodeShape.TRAPEZOID),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\[\\\\(.+?)/\\]"), NodeShape.TRAPEZOID_ALT),
            new NodePattern(Pattern.compile("^([\\w\\-]+)>(.+?)\\]"), NodeShape.ASYMMETRIC),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\{\\{(.+?)\\}\\}"), NodeShape.HEXAGON),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\[(.+?)\\]"), NodeShape.RECTANGLE),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\((.+?)\\)"), NodeShape.ROUNDED),
            new NodePattern(Pattern.compile("^([\\w\\-]+)\\{(.+?)\\}"), NodeShape.DIAMOND)
    );

    private static final Pattern BARE_NODE_REGEX = Pattern.compile("^([\\w\\-]+)");
    private static final Pattern CLASS_SHORTHAND_REGEX = Pattern.compile("^:::([\\w][\\w\\-]*)");

    private static void parseEdgeLine(String line, MermaidGraph graph, Deque<MermaidSubgraph> stack) {

        String remaining = line.trim();
        ConsumedNodeGroup firstGroup = consumeNodeGroup(remaining, graph, stack);
        if (firstGroup == null || firstGroup.ids().isEmpty()) return;
        remaining = firstGroup.remaining().trim();
        List<String> prevIds = firstGroup.ids();

        while (!remaining.isEmpty()) {
            boolean hasArrowStart;
            EdgeStyle style;
            boolean hasArrowEnd;
            String edgeLabel = null;

            Matcher arrowM = ARROW_REGEX.matcher(remaining);
            if (arrowM.find() && arrowM.start() == 0) {
                hasArrowStart = arrowM.group(1) != null;
                String op = arrowM.group(2);
                String rawLabel = arrowM.group(3);
                if (rawLabel != null) {
                    String t = rawLabel.trim();
                    if (!t.isEmpty()) edgeLabel = MultilineUtils.normalizeBrTags(t);
                }
                remaining = remaining.substring(arrowM.end()).trim();
                style = EdgeStyle.fromArrowOp(op);
                hasArrowEnd = op.endsWith(">");
            } else {
                Matcher textM = TEXT_ARROW_REGEX.matcher(remaining);
                if (!(textM.find() && textM.start() == 0)) break;
                hasArrowStart = textM.group(1) != null;
                String rawLabel = textM.group(3).trim();
                if (!rawLabel.isEmpty()) edgeLabel = MultilineUtils.normalizeBrTags(rawLabel);
                String openOp = textM.group(2);
                String closeOp = textM.group(4);
                remaining = remaining.substring(textM.end()).trim();
                style = EdgeStyle.fromTextArrowOps(openOp, closeOp);
                hasArrowEnd = closeOp.endsWith(">");
            }

            ConsumedNodeGroup nextGroup = consumeNodeGroup(remaining, graph, stack);
            if (nextGroup == null || nextGroup.ids().isEmpty()) break;
            remaining = nextGroup.remaining().trim();

            for (String s : prevIds) {
                for (String t : nextGroup.ids()) {
                    graph.edges().add(new MermaidEdge(s, t, edgeLabel, style, hasArrowStart, hasArrowEnd));
                }
            }
            prevIds = nextGroup.ids();
        }
    }

    private record ConsumedNodeGroup(List<String> ids, String remaining) {}
    private record ConsumedNode(String id, String remaining) {}

    private static ConsumedNodeGroup consumeNodeGroup(String text, MermaidGraph graph, Deque<MermaidSubgraph> stack) {
        ConsumedNode first = consumeNode(text, graph, stack);
        if (first == null) return null;
        List<String> ids = new ArrayList<>();
        ids.add(first.id());
        String remaining = first.remaining().trim();
        while (remaining.startsWith("&")) {
            remaining = remaining.substring(1).trim();
            ConsumedNode next = consumeNode(remaining, graph, stack);
            if (next == null) break;
            ids.add(next.id());
            remaining = next.remaining().trim();
        }
        return new ConsumedNodeGroup(List.copyOf(ids), remaining);
    }

    private static ConsumedNode consumeNode(String text, MermaidGraph graph, Deque<MermaidSubgraph> stack) {
        String id = null;
        String remaining = text;

        for (NodePattern np : NODE_PATTERNS) {
            Matcher m = np.regex().matcher(text);
            if (m.find() && m.start() == 0) {
                id = m.group(1);
                String label = MultilineUtils.normalizeBrTags(m.group(2));
                registerNode(graph, stack, new MermaidNode(id, label, np.shape()));
                remaining = text.substring(m.end());
                break;
            }
        }

        if (id == null) {
            Matcher bm = BARE_NODE_REGEX.matcher(text);
            if (bm.find() && bm.start() == 0) {
                id = bm.group(1);
                if (!graph.nodes().containsKey(id)) {
                    registerNode(graph, stack, new MermaidNode(id, id, NodeShape.RECTANGLE));
                }
                remaining = text.substring(bm.end());
            }
        }

        if (id == null) return null;

        Matcher cm = CLASS_SHORTHAND_REGEX.matcher(remaining);
        if (cm.find() && cm.start() == 0) {
            graph.classAssignments().put(id, cm.group(1));
            remaining = remaining.substring(cm.end());
        }
        return new ConsumedNode(id, remaining);
    }

    private static void registerNode(MermaidGraph graph, Deque<MermaidSubgraph> stack, MermaidNode node) {
        boolean isNew = !graph.nodes().containsKey(node.id());
        if (isNew) graph.nodes().put(node.id(), node);
        trackInSubgraph(stack, node.id());
    }

    private static void trackInSubgraph(Deque<MermaidSubgraph> stack, String nodeId) {
        if (!stack.isEmpty()) {
            MermaidSubgraph cur = stack.peek();
            if (!cur.nodeIds().contains(nodeId)) cur.nodeIds().add(nodeId);
        }
    }
}
