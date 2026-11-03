package com.tutego.jarielle.er;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ErLayoutTest {

    @Test
    void layoutSimple() {
        var lines = Arrays.stream("erDiagram\nCUSTOMER ||--o{ ORDER : places\nORDER ||--|{ ITEM : contains".split("\n")).map(String::trim).toList();
        var d = ErParser.parse(lines);
        var pos = ErLayout.layout(d);
        assertThat(pos.entities()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(pos.width()).isGreaterThan(0);
        assertThat(pos.relationships()).hasSize(2);
    }
}
