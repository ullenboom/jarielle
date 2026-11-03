package com.tutego.jarielle.sequence;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SequenceParserTest {

    private SequenceDiagram parse(String text) {
        List<String> lines = Arrays.stream(text.split("\n")).map(String::trim).filter(s->!s.isEmpty() && !s.startsWith("%%")).toList();
        return SequenceParser.parse(lines);
    }

    @Test
    void parseBasic() {
        var sd = parse("sequenceDiagram\nAlice->>Bob: Hello\nBob-->>Alice: Hi");
        assertThat(sd.actors()).extracting(SequenceDiagram.Actor::id).containsExactly("Alice","Bob");
        assertThat(sd.messages()).hasSize(2);
        assertThat(sd.messages().get(0).from()).isEqualTo("Alice");
        assertThat(sd.messages().get(0).lineStyle()).isEqualTo("solid");
        assertThat(sd.messages().get(0).arrowHead()).isEqualTo("filled");
        assertThat(sd.messages().get(1).lineStyle()).isEqualTo("dashed");
    }

    @Test
    void parseParticipantAndActor() {
        var sd = parse("sequenceDiagram\nparticipant A as Alice\nactor B as Bob\nA->>B: hi");
        assertThat(sd.actors()).hasSize(2);
        assertThat(sd.actors().get(0).id()).isEqualTo("A");
        assertThat(sd.actors().get(0).label()).isEqualTo("Alice");
        assertThat(sd.actors().get(1).type()).isEqualTo(ActorType.ACTOR);
        assertThat(sd.actors().get(1).id()).isEqualTo("B");
        assertThat(sd.actors().get(1).label()).isEqualTo("Bob");
    }

    @Test
    void parseActivation() {
        var sd = parse("sequenceDiagram\nA->>+B: Hello\nB-->>-A: Hi");
        assertThat(sd.messages().get(0).activate()).isTrue();
        assertThat(sd.messages().get(1).deactivate()).isTrue();
    }

    @Test
    void parseBlocks() {
        var sd = parse("sequenceDiagram\nA->>B: m1\nloop LoopLabel\nA->>B: m2\nend\nA->>B: m3");
        assertThat(sd.blocks()).hasSize(1);
        assertThat(sd.blocks().get(0).type()).isEqualTo("loop");
        assertThat(sd.blocks().get(0).label()).isEqualTo("LoopLabel");
        assertThat(sd.blocks().get(0).startIndex()).isEqualTo(1);
        assertThat(sd.blocks().get(0).endIndex()).isEqualTo(1);
    }

    @Test
    void parseAltWithElse() {
        var sd = parse("sequenceDiagram\nA->>B: m1\nalt AltLabel\nA->>B: m2\nelse ElseLabel\nA->>B: m3\nend");
        assertThat(sd.blocks()).hasSize(1);
        assertThat(sd.blocks().get(0).type()).isEqualTo("alt");
        assertThat(sd.blocks().get(0).dividers()).hasSize(1);
        assertThat(sd.blocks().get(0).dividers().get(0).label()).isEqualTo("ElseLabel");
    }

    @Test
    void parseNotes() {
        var sd = parse("sequenceDiagram\nA->>B: hi\nNote left of A: Left note\nNote right of B: Right\nNote over A,B: Over");
        assertThat(sd.notes()).hasSize(3);
        assertThat(sd.notes().get(0).position()).isEqualTo("left");
        assertThat(sd.notes().get(1).position()).isEqualTo("right");
        assertThat(sd.notes().get(2).position()).isEqualTo("over");
        assertThat(sd.notes().get(2).actorIds()).containsExactly("A","B");
    }

    @Test
    void autoCreateActors() {
        var sd = parse("sequenceDiagram\nAlice->>Bob: Hello");
        assertThat(sd.actors()).extracting(SequenceDiagram.Actor::id).containsExactly("Alice","Bob");
    }
}
