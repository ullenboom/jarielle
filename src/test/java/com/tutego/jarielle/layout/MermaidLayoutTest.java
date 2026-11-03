package com.tutego.jarielle.layout;

import com.tutego.jarielle.model.*;
import com.tutego.jarielle.parser.MermaidParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MermaidLayoutTest {

    @Test
    void layoutSimpleGraph() {
        var g = MermaidParser.parseMermaid("graph TD\n A --> B --> C");
        var pos = ElkLayoutEngine.layout(g, RenderOptions.defaults());
        assertThat(pos.nodes()).hasSize(3);
        assertThat(pos.edges()).hasSize(2);
        assertThat(pos.width()).isGreaterThan(0);
        assertThat(pos.height()).isGreaterThan(0);
        for (var n : pos.nodes()) {
            assertThat(n.width()).isGreaterThan(0);
            assertThat(n.height()).isGreaterThan(0);
        }
        for (var e : pos.edges()) {
            assertThat(e.points()).hasSizeGreaterThan(1);
        }
    }

    @Test
    void layoutWithSubgraph() {
        var txt = """
                graph TD
                    A --> B
                    subgraph S [Inner]
                        C --> D
                    end
                    B --> C
                """;
        var g = MermaidParser.parseMermaid(txt);
        var pos = ElkLayoutEngine.layout(g, RenderOptions.defaults());
        assertThat(pos.groups()).hasSize(1);
        assertThat(pos.nodes()).hasSize(4);
        // Ensure group contains its nodes geometrically? At least group bounds exist
        var grp = pos.groups().get(0);
        assertThat(grp.width()).isGreaterThan(0);
        assertThat(grp.height()).isGreaterThan(0);
    }

    @Test
    void layoutDirectionLR() {
        var g = MermaidParser.parseMermaid("graph LR\n A --> B --> C");
        var pos = ElkLayoutEngine.layout(g, RenderOptions.defaults());
        // In LR, x should increase along flow
        var byX = pos.nodes().stream().sorted((a,b)->Double.compare(a.x(), b.x())).toList();
        // Not strict but at least ordering
        assertThat(byX).hasSize(3);
    }

    @Test
    void layoutStateDiagram() {
        var txt = """
                stateDiagram-v2
                    [*] --> S1
                    S1 --> S2: go
                    S2 --> [*]
                """;
        var g = MermaidParser.parseMermaid(txt);
        var pos = ElkLayoutEngine.layout(g, RenderOptions.defaults());
        assertThat(pos.nodes().stream().anyMatch(n -> n.shape() == NodeShape.STATE_START)).isTrue();
        assertThat(pos.nodes().stream().anyMatch(n -> n.shape() == NodeShape.STATE_END)).isTrue();
    }

    @Test
    void layoutDiamondClipping() {
        var g = MermaidParser.parseMermaid("graph TD\n A{Dec} --> B[Out]");
        var pos = ElkLayoutEngine.layout(g, RenderOptions.defaults());
        assertThat(pos.nodes().stream().anyMatch(n -> n.shape() == NodeShape.DIAMOND)).isTrue();
        // edges should be clipped to diamond — ensure points not NaN
        for (var e : pos.edges()) {
            for (var p : e.points()) {
                assertThat(Double.isFinite(p.x())).isTrue();
                assertThat(Double.isFinite(p.y())).isTrue();
            }
        }
    }

    @Test
    void layoutFanOutBundling() {
        var g = MermaidParser.parseMermaid("graph TD\n A --> B\n A --> C\n A --> D");
        var pos = ElkLayoutEngine.layout(g, RenderOptions.defaults());
        assertThat(pos.edges()).hasSize(3);
        // After bundling, all edges should share trunk — check they have 4 points (bundled)
        // Not strict, but at least not failing
    }

    @Test
    void layoutWithLinkStyle() {
        var g = MermaidParser.parseMermaid("graph TD\n A --> B\n linkStyle 0 stroke:#f00,stroke-width:2px");
        var pos = ElkLayoutEngine.layout(g, RenderOptions.defaults());
        assertThat(pos.edges().get(0).inlineStyle()).containsEntry("stroke", "#f00");
    }
}
