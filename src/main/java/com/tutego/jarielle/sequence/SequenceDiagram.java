package com.tutego.jarielle.sequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed sequence diagram — Java port of {@code SequenceDiagram} in src/sequence/types.ts.
 */
public final class SequenceDiagram {
    private final List<Actor> actors = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private final List<Block> blocks = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();

    public List<Actor> actors() { return actors; }
    public List<Message> messages() { return messages; }
    public List<Block> blocks() { return blocks; }
    public List<Note> notes() { return notes; }

    public record Actor(String id, String label, ActorType type) {}
    public record Message(String from, String to, String label, String lineStyle, String arrowHead, boolean activate, boolean deactivate) {
        public Message(String from, String to, String label, String lineStyle, String arrowHead) {
            this(from, to, label, lineStyle, arrowHead, false, false);
        }
    }
    public record Block(String type, String label, int startIndex, int endIndex, List<Divider> dividers) {
        public Block(String type, String label, int startIndex, int endIndex, List<Divider> dividers) {
            this.type = type; this.label = label; this.startIndex = startIndex; this.endIndex = endIndex;
            this.dividers = List.copyOf(dividers);
        }
    }
    public record Divider(int index, String label) {}
    public record Note(List<String> actorIds, String text, String position, int afterIndex) {
        public Note(List<String> actorIds, String text, String position, int afterIndex) {
            this.actorIds = List.copyOf(actorIds); this.text = text; this.position = position; this.afterIndex = afterIndex;
        }
    }
}
