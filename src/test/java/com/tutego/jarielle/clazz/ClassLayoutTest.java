package com.tutego.jarielle.clazz;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ClassLayoutTest {

    @Test
    void layoutSimple() {
        List<String> lines = Arrays.stream("classDiagram\nA <|-- B\nA *-- C".split("\n")).map(String::trim).toList();
        var d = ClassParser.parse(lines);
        var pos = ClassLayout.layout(d, null);
        assertThat(pos.classes()).hasSize(3);
        assertThat(pos.width()).isGreaterThan(0);
        for (var c : pos.classes()) assertThat(c.width()).isGreaterThan(0);
        assertThat(pos.relationships()).hasSize(2);
    }
}
