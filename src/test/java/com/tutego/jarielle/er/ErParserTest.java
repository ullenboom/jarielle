package com.tutego.jarielle.er;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ErParserTest {

    private ErDiagram parse(String txt) {
        List<String> lines = Arrays.stream(txt.split("\n")).map(String::trim).filter(s->!s.isEmpty()).toList();
        return ErParser.parse(lines);
    }

    @Test
    void parseBasicRelationship() {
        var d = parse("erDiagram\nCUSTOMER ||--o{ ORDER : places");
        assertThat(d.relationships()).hasSize(1);
        assertThat(d.relationships().get(0).entity1()).isEqualTo("CUSTOMER");
        assertThat(d.relationships().get(0).cardinality1()).isEqualTo("one");
        assertThat(d.relationships().get(0).cardinality2()).isEqualTo("zero-many");
        assertThat(d.relationships().get(0).identifying()).isTrue();
    }

    @Test
    void parseEntityWithAttributes() {
        var d = parse("erDiagram\nCUSTOMER {\nstring name PK\nint age\n}\nCUSTOMER ||--o{ ORDER : places");
        assertThat(d.entities()).extracting(ErDiagram.ErEntity::id).contains("CUSTOMER");
        var cust = d.entities().stream().filter(e->e.id().equals("CUSTOMER")).findFirst().orElseThrow();
        assertThat(cust.attributes()).hasSize(2);
        assertThat(cust.attributes().get(0).keys()).contains("PK");
    }

    @Test
    void parseNonIdentifying() {
        var d = parse("erDiagram\nA |o..o{ B : test");
        assertThat(d.relationships().get(0).identifying()).isFalse();
    }
}
