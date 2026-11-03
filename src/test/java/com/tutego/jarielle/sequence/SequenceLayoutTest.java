package com.tutego.jarielle.sequence;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SequenceLayoutTest {

    private PositionedSequenceDiagram layout(String text) {
        List<String> lines = Arrays.stream(text.split("\n")).map(String::trim).filter(s->!s.isEmpty()).toList();
        var sd = SequenceParser.parse(lines);
        return SequenceLayout.layout(sd, null);
    }

    @Test
    void layoutBasic() {
        var pos = layout("sequenceDiagram\nAlice->>Bob: Hello\nBob-->>Alice: Hi");
        assertThat(pos.actors()).hasSize(2);
        assertThat(pos.messages()).hasSize(2);
        assertThat(pos.width()).isGreaterThan(200);
        assertThat(pos.height()).isGreaterThan(100);
        assertThat(pos.lifelines()).hasSize(2);
    }

    @Test
    void layoutSelfMessage() {
        var pos = layout("sequenceDiagram\nAlice->>Alice: Self\nAlice->>Bob: Next");
        assertThat(pos.messages().get(0).isSelf()).isTrue();
        assertThat(pos.messages().get(1).isSelf()).isFalse();
    }

    @Test
    void layoutActivations() {
        var pos = layout("sequenceDiagram\nA->>+B: Hello\nB-->>-A: Hi");
        assertThat(pos.activations()).hasSizeGreaterThan(0);
        assertThat(pos.activations().get(0).actorId()).isEqualTo("B");
    }

    @Test
    void layoutBlocks() {
        var pos = layout("sequenceDiagram\nA->>B: m1\nloop Loop\nA->>B: m2\nend");
        assertThat(pos.blocks()).hasSize(1);
        assertThat(pos.blocks().get(0).width()).isGreaterThan(0);
        assertThat(pos.blocks().get(0).height()).isGreaterThan(0);
    }

    @Test
    void layoutNotes() {
        var pos = layout("sequenceDiagram\nA->>B: hi\nNote left of A: Left\nNote over A,B: Over");
        assertThat(pos.notes()).hasSize(2);
        for (var n : pos.notes()) {
            assertThat(n.width()).isGreaterThan(0);
            assertThat(n.height()).isGreaterThan(0);
        }
    }
}
