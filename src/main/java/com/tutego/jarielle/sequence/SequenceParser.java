package com.tutego.jarielle.sequence;

import com.tutego.jarielle.util.MultilineUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sequence diagram parser — Java port of {@code src/sequence/parser.ts}.
 */
public final class SequenceParser {

    private SequenceParser() {}

    public static SequenceDiagram parse(List<String> lines) {
        var diagram = new SequenceDiagram();
        Set<String> actorIds = new LinkedHashSet<>();
        Deque<BlockStack> blockStack = new ArrayDeque<>();

        Pattern actorPat = Pattern.compile("^(participant|actor)\\s+(\\S+?)(?:\\s+as\\s+(.+))?$");
        Pattern notePat = Pattern.compile("^Note\\s+(left of|right of|over)\\s+([^:]+):\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        Pattern blockPat = Pattern.compile("^(loop|alt|opt|par|critical|break|rect)\\s*(.*)$");
        Pattern dividerPat = Pattern.compile("^(else|and)\\s*(.*)$");
        // Message patterns — two stages as in TS
        Pattern msgPat1 = Pattern.compile("^(\\S+?)\\s*(--?>?>|--?[)x]|--?>>|--?>)\\s*([+-]?)(\\S+?)\\s*:\\s*(.+)$");
        Pattern msgPat2 = Pattern.compile("^(\\S+?)\\s*(->>|-->>|-\\)|--\\)|-x|--x|->|--> )\\s*([+-]?)(\\S+?)\\s*:\\s*(.+)$");

        // For robust matching, use the first complex pattern; fallback to simple.
        // The TS uses slightly different regex; we simplify to one that covers both.
        Pattern messagePat = Pattern.compile("^(\\S+)\\s*(--?>>|-->>|->>|->|-->|-\\))\\s*([+-]?)(\\S+)\\s*:\\s*(.+)$");
        // More permissive fallback: cover --, >>, ), x etc.
        Pattern simplePat = Pattern.compile("^(\\S+)\\s*(-->>|->>|--\\)|-\\).*?|--x|-x|->|-->|--)\\s*([+-]?)(\\S+)\\s*:\\s*(.+)$");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            Matcher mActor = actorPat.matcher(line);
            if (mActor.matches()) {
                String typeStr = mActor.group(1);
                String id = mActor.group(2);
                String rawLabel = mActor.group(3) != null ? mActor.group(3).trim() : id;
                String label = MultilineUtils.normalizeBrTags(rawLabel);
                ActorType type = "actor".equals(typeStr) ? ActorType.ACTOR : ActorType.PARTICIPANT;
                if (!actorIds.contains(id)) {
                    actorIds.add(id);
                    diagram.actors().add(new SequenceDiagram.Actor(id, label, type));
                }
                continue;
            }

            Matcher mNote = notePat.matcher(line);
            if (mNote.matches()) {
                String posStr = mNote.group(1).toLowerCase();
                String actorsStr = mNote.group(2).trim();
                String text = MultilineUtils.normalizeBrTags(mNote.group(3).trim());
                List<String> noteActorIds = Arrays.stream(actorsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
                for (String aid : noteActorIds) ensureActor(diagram, actorIds, aid);
                String pos = "over";
                if ("left of".equals(posStr)) pos = "left";
                else if ("right of".equals(posStr)) pos = "right";
                diagram.notes().add(new SequenceDiagram.Note(noteActorIds, text, pos, diagram.messages().size() - 1));
                continue;
            }

            Matcher mBlock = blockPat.matcher(line);
            if (mBlock.matches()) {
                String blockType = mBlock.group(1);
                String rawLabel = mBlock.group(2) != null ? mBlock.group(2).trim() : "";
                String label = MultilineUtils.normalizeBrTags(rawLabel);
                blockStack.push(new BlockStack(blockType, label, diagram.messages().size(), new ArrayList<>()));
                continue;
            }

            Matcher mDivider = dividerPat.matcher(line);
            if (mDivider.matches() && !blockStack.isEmpty()) {
                String raw = mDivider.group(2) != null ? mDivider.group(2).trim() : "";
                String label = MultilineUtils.normalizeBrTags(raw);
                blockStack.peek().dividers.add(new SequenceDiagram.Divider(diagram.messages().size(), label));
                continue;
            }

            if ("end".equals(line) && !blockStack.isEmpty()) {
                BlockStack completed = blockStack.pop();
                int endIdx = Math.max(diagram.messages().size() - 1, completed.startIndex);
                diagram.blocks().add(new SequenceDiagram.Block(completed.type, completed.label, completed.startIndex, endIdx, completed.dividers));
                continue;
            }

            // Message — try primary pattern first (covers arrow variations)
            // Use TS first pattern: (\S+?)\s*(--?>?>|--?[)x]|--?>>|--?>)\s*([+-]?)(\S+?)\s*:\s*(.+)
            // We'll implement custom parsing for robustness: split at colon.
            int colon = line.indexOf(':');
            if (colon > 0) {
                String left = line.substring(0, colon).trim();
                String label = MultilineUtils.normalizeBrTags(line.substring(colon + 1).trim());
                // left should be like "A->>B" or "A-->>+B"
                // Find arrow in left
                // Arrow operators: ->>, -->>, -), --), -x, --x, ->, -->, --, etc with optional +/- before target
                // We'll search for arrow tokens
                Matcher arrowM = Pattern.compile("(.*?)\\s*(->>|-->>|-\\).*?|--\\)|--x|-x|->|-->|-->|--)\\s*([+-]?)(\\S+)").matcher(left);
                // Simpler: iterate over known arrows longest first
                String[] arrows = {"-->>", "->>", "--)", "-)", "--x", "-x", "-->", "->", "--", "->>"};
                // Sort by length descending
                Arrays.sort(arrows, (a,b)->Integer.compare(b.length(), a.length()));
                boolean matched = false;
                for (String arr : arrows) {
                    int idx = left.indexOf(arr);
                    if (idx >= 0) {
                        String from = left.substring(0, idx).trim();
                        String rest = left.substring(idx + arr.length()).trim();
                        String activation = "";
                        String to = rest;
                        if (rest.startsWith("+") || rest.startsWith("-")) {
                            activation = rest.substring(0,1);
                            to = rest.substring(1).trim();
                        }
                        if (from.isEmpty() || to.isEmpty()) continue;
                        // Validate from/to not containing arrow chars
                        if (from.matches(".*[\\->)x]+.*") || to.matches(".*[\\->)x]+.*")) {
                            // Actually from may contain only id, so if it contains arrow-like chars it's invalid; skip
                        }
                        ensureActor(diagram, actorIds, from);
                        ensureActor(diagram, actorIds, to);
                        String lineStyle = arr.startsWith("--") ? "dashed" : "solid";
                        String arrowHead = (arr.contains(">>") || arr.contains("x")) ? "filled" : "open";
                        boolean activate = "+".equals(activation);
                        boolean deactivate = "-".equals(activation);
                        diagram.messages().add(new SequenceDiagram.Message(from, to, label, lineStyle, arrowHead, activate, deactivate));
                        matched = true;
                        break;
                    }
                }
                if (matched) continue;
                // fallback to regex match (TS style)
                Matcher m1 = msgPat1.matcher(line);
                if (m1.matches()) {
                    String from = m1.group(1);
                    String arr = m1.group(2);
                    String act = m1.group(3);
                    String to = m1.group(4);
                    ensureActor(diagram, actorIds, from);
                    ensureActor(diagram, actorIds, to);
                    String lineStyle = arr.startsWith("--") ? "dashed" : "solid";
                    String arrowHead = (arr.contains(">>") || arr.contains("x")) ? "filled" : "open";
                    diagram.messages().add(new SequenceDiagram.Message(from, to, label, lineStyle, arrowHead, "+".equals(act), "-".equals(act)));
                    continue;
                }
            }
        }
        return diagram;
    }

    private static void ensureActor(SequenceDiagram diagram, Set<String> ids, String id) {
        if (!ids.contains(id)) {
            ids.add(id);
            diagram.actors().add(new SequenceDiagram.Actor(id, id, ActorType.PARTICIPANT));
        }
    }

    private record BlockStack(String type, String label, int startIndex, List<SequenceDiagram.Divider> dividers) {}
}
