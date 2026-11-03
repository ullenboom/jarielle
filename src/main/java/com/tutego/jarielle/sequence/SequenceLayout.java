package com.tutego.jarielle.sequence;

import com.tutego.jarielle.model.RenderOptions;
import com.tutego.jarielle.util.StyleConstants;
import com.tutego.jarielle.util.TextMetrics;

import java.util.*;

/**
 * Sequence layout — Java port of {@code src/sequence/layout.ts}.
 */
public final class SequenceLayout {

    private SequenceLayout() {}

    private static class SEQ {
        static final double padding = 30;
        static final double actorGap = 140;
        static final double actorHeight = 40;
        static final double actorPadX = 16;
        static final double headerGap = 20;
        static final double messageRowHeight = 40;
        static final double selfMessageHeight = 30;
        static final double activationWidth = 10;
        static final double blockPadX = 10;
        static final double blockPadTop = 40;
        static final double blockPadBottom = 8;
        static final double blockHeaderExtra = 28;
        static final double dividerExtra = 24;
        static final double noteWidth = 60;
        static final double notePadX = 12;
        static final double notePadY = 6;
        static final double noteGap = 10;
    }

    public static PositionedSequenceDiagram layout(SequenceDiagram diagram, RenderOptions options) {
        if (diagram.actors().isEmpty()) {
            return new PositionedSequenceDiagram(0,0,List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
        }
        // 1. actor widths
        List<Double> actorWidths = new ArrayList<>();
        for (var a : diagram.actors()) {
            double w = TextMetrics.measureTextWidth(a.label(), StyleConstants.FontSizes.NODE_LABEL, StyleConstants.FontWeights.NODE_LABEL) + SEQ.actorPadX*2;
            actorWidths.add(Math.max(w, 80));
        }
        List<Double> actorCenterX = new ArrayList<>();
        double currentX = SEQ.padding + actorWidths.get(0)/2;
        for (int i=0;i<diagram.actors().size();i++) {
            if (i>0) {
                double minGap = Math.max(SEQ.actorGap, (actorWidths.get(i-1)+actorWidths.get(i))/2 + 40);
                currentX += minGap;
            }
            actorCenterX.add(currentX);
        }
        Map<String,Integer> actorIndex = new HashMap<>();
        for (int i=0;i<diagram.actors().size();i++) actorIndex.put(diagram.actors().get(i).id(), i);

        // 2. positioned actors
        double actorY = SEQ.padding;
        List<PositionedSequenceDiagram.PositionedActor> actors = new ArrayList<>();
        for (int i=0;i<diagram.actors().size();i++) {
            var a = diagram.actors().get(i);
            actors.add(new PositionedSequenceDiagram.PositionedActor(a.id(), a.label(), a.type(), actorCenterX.get(i), actorY, actorWidths.get(i), SEQ.actorHeight));
        }

        // 3. extra space before for blocks
        Map<Integer,Double> extraBefore = new HashMap<>();
        for (var b : diagram.blocks()) {
            extraBefore.merge(b.startIndex(), SEQ.blockHeaderExtra, Math::max);
            for (var d : b.dividers()) extraBefore.merge(d.index(), SEQ.dividerExtra, Math::max);
        }
        Map<Integer,List<SequenceDiagram.Note>> notesByAfter = new HashMap<>();
        for (var n : diagram.notes()) {
            notesByAfter.computeIfAbsent(n.afterIndex(), k->new ArrayList<>()).add(n);
        }
        List<PositionedSequenceDiagram.PositionedNote> positionedNotes = new ArrayList<>();
        List<PositionedSequenceDiagram.PositionedMessage> messages = new ArrayList<>();
        Map<String,Deque<ActivationEntry>> stacks = new HashMap<>();
        List<PositionedSequenceDiagram.Activation> activations = new ArrayList<>();
        double nestingOffset = 4;

        double messageY = actorY + SEQ.actorHeight + SEQ.headerGap;
        for (int msgIdx=0; msgIdx<diagram.messages().size(); msgIdx++) {
            var msg = diagram.messages().get(msgIdx);
            int fromIdx = actorIndex.getOrDefault(msg.from(), 0);
            int toIdx = actorIndex.getOrDefault(msg.to(), 0);
            boolean isSelf = msg.from().equals(msg.to());
            double extra = extraBefore.getOrDefault(msgIdx, 0.0);
            if (extra>0) messageY += extra;
            double x1 = actorCenterX.get(fromIdx);
            double x2 = actorCenterX.get(toIdx);
            messages.add(new PositionedSequenceDiagram.PositionedMessage(msg.from(), msg.to(), msg.label(), msg.lineStyle(), msg.arrowHead(), x1, x2, messageY, isSelf));

            if (msg.activate()) {
                stacks.computeIfAbsent(msg.to(), k->new ArrayDeque<>());
                var stack = stacks.get(msg.to());
                int depth = stack.size();
                stack.push(new ActivationEntry(messageY, depth));
            }
            if (msg.deactivate()) {
                var stack = stacks.get(msg.from());
                if (stack!=null && !stack.isEmpty()) {
                    var entry = stack.pop();
                    int idx = actorIndex.getOrDefault(msg.from(), 0);
                    double xOff = entry.depth * nestingOffset;
                    activations.add(new PositionedSequenceDiagram.Activation(msg.from(), actorCenterX.get(idx)-SEQ.activationWidth/2 + xOff, entry.startY, messageY, SEQ.activationWidth));
                }
            }
            messageY += isSelf ? SEQ.selfMessageHeight + SEQ.messageRowHeight : SEQ.messageRowHeight;

            var notesForMsg = notesByAfter.get(msgIdx);
            if (notesForMsg!=null && !notesForMsg.isEmpty()) {
                double selfExtra = isSelf ? SEQ.selfMessageHeight : 0;
                double noteY = messages.get(msgIdx).y() + selfExtra + 8;
                for (var note : notesForMsg) {
                    double noteW = Math.max(SEQ.noteWidth, TextMetrics.measureTextWidth(note.text(), StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.EDGE_LABEL) + SEQ.notePadX*2);
                    double noteH = StyleConstants.FontSizes.EDGE_LABEL + SEQ.notePadY*2;
                    int firstIdx = actorIndex.getOrDefault(note.actorIds().isEmpty()? "" : note.actorIds().get(0), 0);
                    double noteX;
                    if ("left".equals(note.position())) {
                        noteX = actorCenterX.get(firstIdx) - actorWidths.get(firstIdx)/2 - noteW - SEQ.noteGap;
                    } else if ("right".equals(note.position())) {
                        noteX = actorCenterX.get(firstIdx) + actorWidths.get(firstIdx)/2 + SEQ.noteGap;
                    } else {
                        if (note.actorIds().size()>1) {
                            int lastIdx = actorIndex.getOrDefault(note.actorIds().get(note.actorIds().size()-1), firstIdx);
                            noteX = (actorCenterX.get(firstIdx)+actorCenterX.get(lastIdx))/2 - noteW/2;
                        } else {
                            noteX = actorCenterX.get(firstIdx) - noteW/2;
                        }
                    }
                    positionedNotes.add(new PositionedSequenceDiagram.PositionedNote(note.text(), noteX, noteY, noteW, noteH, note.actorIds(), note.position()));
                    noteY += noteH + 4;
                }
                messageY = Math.max(messageY, noteY + SEQ.messageRowHeight/2);
            }
        }
        // close unclosed activations
        for (var e : stacks.entrySet()) {
            String actorId = e.getKey();
            var stack = e.getValue();
            for (var entry : stack) {
                int idx = actorIndex.getOrDefault(actorId, 0);
                double xOff = entry.depth * nestingOffset;
                activations.add(new PositionedSequenceDiagram.Activation(actorId, actorCenterX.get(idx)-SEQ.activationWidth/2 + xOff, entry.startY, messageY - SEQ.messageRowHeight/2, SEQ.activationWidth));
            }
        }

        // 4. blocks
        List<PositionedSequenceDiagram.PositionedBlock> blocks = new ArrayList<>();
        for (var block : diagram.blocks()) {
            var startMsg = block.startIndex() < messages.size() ? messages.get(block.startIndex()) : null;
            var endMsg = block.endIndex() < messages.size() ? messages.get(block.endIndex()) : null;
            double blockTop = (startMsg!=null? startMsg.y() : messageY) - SEQ.blockPadTop;
            double blockBottom = (endMsg!=null? endMsg.y() : messageY) + SEQ.blockPadBottom + 12;
            Set<Integer> involved = new HashSet<>();
            for (int mi=block.startIndex(); mi<=block.endIndex() && mi < diagram.messages().size(); mi++) {
                var m = diagram.messages().get(mi);
                involved.add(actorIndex.getOrDefault(m.from(),0));
                involved.add(actorIndex.getOrDefault(m.to(),0));
            }
            if (involved.isEmpty()) for (int ai=0; ai<diagram.actors().size(); ai++) involved.add(ai);
            int minIdx = involved.stream().mapToInt(Integer::intValue).min().orElse(0);
            int maxIdx = involved.stream().mapToInt(Integer::intValue).max().orElse(0);
            double blockLeft = actorCenterX.get(minIdx) - actorWidths.get(minIdx)/2 - SEQ.blockPadX;
            double blockRight = actorCenterX.get(maxIdx) + actorWidths.get(maxIdx)/2 + SEQ.blockPadX;
            List<PositionedSequenceDiagram.PositionedBlock.DividerPos> dividers = new ArrayList<>();
            for (var d : block.dividers()) {
                var msg = d.index() < messages.size() ? messages.get(d.index()) : null;
                double msgY = msg!=null? msg.y() : messageY;
                double offset = 28;
                if (d.label()!=null && !d.label().isEmpty() && msg!=null && msg.label()!=null) {
                    String divText = "["+d.label()+"]";
                    double divW = TextMetrics.measureTextWidth(divText, StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.EDGE_LABEL);
                    double divLeft = blockLeft + 8;
                    double divRight = divLeft + divW;
                    double msgW = TextMetrics.measureTextWidth(msg.label(), StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.EDGE_LABEL);
                    double msgLeft = msg.isSelf() ? msg.x1()+36 : (msg.x1()+msg.x2())/2 - msgW/2;
                    double msgRight = msgLeft + msgW;
                    if (divRight > msgLeft && divLeft < msgRight) offset = 36;
                }
                dividers.add(new PositionedSequenceDiagram.PositionedBlock.DividerPos(msgY - offset, d.label()));
            }
            blocks.add(new PositionedSequenceDiagram.PositionedBlock(block.type(), block.label(), blockLeft, blockTop, blockRight-blockLeft, blockBottom-blockTop, dividers));
        }

        // 5. bounding box & shift
        double diagramBottom = messageY + SEQ.padding;
        double globalMinX = SEQ.padding;
        double globalMaxX = 0;
        for (var a: actors) { globalMinX = Math.min(globalMinX, a.x()-a.width()/2); globalMaxX = Math.max(globalMaxX, a.x()+a.width()/2); }
        for (var b: blocks) { globalMinX = Math.min(globalMinX, b.x()); globalMaxX = Math.max(globalMaxX, b.x()+b.width()); }
        for (var n: positionedNotes) { globalMinX = Math.min(globalMinX, n.x()); globalMaxX = Math.max(globalMaxX, n.x()+n.width()); }
        for (var m: messages) if (m.isSelf() && m.label()!=null) {
            double loopW=30; double labelLeft = m.x1()+loopW+8; double labelW = TextMetrics.measureTextWidth(m.label(), StyleConstants.FontSizes.EDGE_LABEL, StyleConstants.FontWeights.EDGE_LABEL);
            globalMaxX = Math.max(globalMaxX, labelLeft + labelW + 8);
        }
        double shiftX = globalMinX < SEQ.padding ? SEQ.padding - globalMinX : 0;
        if (shiftX>0) {
            List<PositionedSequenceDiagram.PositionedActor> newActors = new ArrayList<>();
            for (var a: actors) newActors.add(new PositionedSequenceDiagram.PositionedActor(a.id(), a.label(), a.type(), a.x()+shiftX, a.y(), a.width(), a.height()));
            actors = newActors;
            List<PositionedSequenceDiagram.PositionedMessage> newMsgs = new ArrayList<>();
            for (var m: messages) newMsgs.add(new PositionedSequenceDiagram.PositionedMessage(m.from(), m.to(), m.label(), m.lineStyle(), m.arrowHead(), m.x1()+shiftX, m.x2()+shiftX, m.y(), m.isSelf()));
            messages = newMsgs;
            List<PositionedSequenceDiagram.Activation> newActs = new ArrayList<>();
            for (var ac: activations) newActs.add(new PositionedSequenceDiagram.Activation(ac.actorId(), ac.x()+shiftX, ac.topY(), ac.bottomY(), ac.width()));
            activations = newActs;
            List<PositionedSequenceDiagram.PositionedBlock> newBlocks = new ArrayList<>();
            for (var b: blocks) {
                List<PositionedSequenceDiagram.PositionedBlock.DividerPos> nd = b.dividers().stream().map(d-> new PositionedSequenceDiagram.PositionedBlock.DividerPos(d.y(), d.label())).toList();
                newBlocks.add(new PositionedSequenceDiagram.PositionedBlock(b.type(), b.label(), b.x()+shiftX, b.y(), b.width(), b.height(), nd));
            }
            blocks = newBlocks;
            List<PositionedSequenceDiagram.PositionedNote> newNotes = new ArrayList<>();
            for (var n: positionedNotes) newNotes.add(new PositionedSequenceDiagram.PositionedNote(n.text(), n.x()+shiftX, n.y(), n.width(), n.height(), n.actors(), n.position()));
            positionedNotes = newNotes;
            for (int i=0;i<actorCenterX.size();i++) actorCenterX.set(i, actorCenterX.get(i)+shiftX);
        }
        List<PositionedSequenceDiagram.Lifeline> lifelines = new ArrayList<>();
        for (int i=0;i<diagram.actors().size();i++) {
            var a = diagram.actors().get(i);
            lifelines.add(new PositionedSequenceDiagram.Lifeline(a.id(), actorCenterX.get(i), actorY + SEQ.actorHeight, diagramBottom - SEQ.padding));
        }
        double diagramWidth = globalMaxX + shiftX + SEQ.padding;
        double diagramHeight = diagramBottom;
        return new PositionedSequenceDiagram(Math.max(diagramWidth,200), Math.max(diagramHeight,100), actors, lifelines, messages, activations, blocks, positionedNotes);
    }

    private record ActivationEntry(double startY, int depth) {}
}
