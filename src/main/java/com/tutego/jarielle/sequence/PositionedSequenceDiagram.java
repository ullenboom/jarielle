package com.tutego.jarielle.sequence;

import java.util.List;

/**
 * Positioned sequence diagram — Java port of {@code PositionedSequenceDiagram}.
 */
public record PositionedSequenceDiagram(
        double width,
        double height,
        List<PositionedActor> actors,
        List<Lifeline> lifelines,
        List<PositionedMessage> messages,
        List<Activation> activations,
        List<PositionedBlock> blocks,
        List<PositionedNote> notes
) {
    public PositionedSequenceDiagram { actors = List.copyOf(actors); lifelines = List.copyOf(lifelines); messages = List.copyOf(messages); activations = List.copyOf(activations); blocks = List.copyOf(blocks); notes = List.copyOf(notes); }

    public record PositionedActor(String id, String label, ActorType type, double x, double y, double width, double height) {}
    public record Lifeline(String actorId, double x, double topY, double bottomY) {}
    public record PositionedMessage(String from, String to, String label, String lineStyle, String arrowHead, double x1, double x2, double y, boolean isSelf) {}
    public record Activation(String actorId, double x, double topY, double bottomY, double width) {}
    public record PositionedBlock(String type, String label, double x, double y, double width, double height, List<DividerPos> dividers) {
        public PositionedBlock { dividers = List.copyOf(dividers); }
        public record DividerPos(double y, String label) {}
    }
    public record PositionedNote(String text, double x, double y, double width, double height, List<String> actors, String position) {
        public PositionedNote { actors = actors != null ? List.copyOf(actors) : List.of(); }
    }
}
