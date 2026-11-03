package com.tutego.jarielle.parser;

import com.tutego.jarielle.model.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MermaidParserTest {

    @Test
    void parseSimpleFlowchart() {
        var g = MermaidParser.parseMermaid("graph TD\n A[Start] --> B{Decision}\n B -->|Yes| C[Process]\n B -->|No| D[End]\n C --> D");
        assertThat(g.direction()).isEqualTo(Direction.TD);
        assertThat(g.nodes()).containsKeys("A", "B", "C", "D");
        assertThat(g.nodes().get("B").shape()).isEqualTo(NodeShape.DIAMOND);
        assertThat(g.edges()).hasSize(4);
        var e = g.edges().get(1);
        assertThat(e.label()).isEqualTo("Yes");
        assertThat(e.style()).isEqualTo(EdgeStyle.SOLID);
    }

    @Test
    void parseLRWithMultipleShapes() {
        var txt = """
                graph LR
                    A((Circle)) --> B[[Subroutine]]
                    B --> C{{Hexagon}}
                    C --> D[(Cylinder)]
                    D --> E>Flag]
                    E --> F[/Trapezoid\\]
                    F --> G[\\TrapAlt/]
                """;
        var g = MermaidParser.parseMermaid(txt);
        assertThat(g.direction()).isEqualTo(Direction.LR);
        assertThat(g.nodes().get("A").shape()).isEqualTo(NodeShape.CIRCLE);
        assertThat(g.nodes().get("B").shape()).isEqualTo(NodeShape.SUBROUTINE);
        assertThat(g.nodes().get("C").shape()).isEqualTo(NodeShape.HEXAGON);
        assertThat(g.nodes().get("D").shape()).isEqualTo(NodeShape.CYLINDER);
        assertThat(g.nodes().get("E").shape()).isEqualTo(NodeShape.ASYMMETRIC);
        assertThat(g.nodes().get("F").shape()).isEqualTo(NodeShape.TRAPEZOID);
        assertThat(g.nodes().get("G").shape()).isEqualTo(NodeShape.TRAPEZOID_ALT);
    }

    @Test
    void parseChainedAndParallelEdges() {
        var g = MermaidParser.parseMermaid("graph TD\n A --> B --> C\n X & Y --> Z");
        assertThat(g.edges()).anySatisfy(e -> assertThat(e.source()).isEqualTo("A"));
        // A->B, B->C, X->Z, Y->Z = 4 edges
        assertThat(g.edges()).hasSize(4);
        assertThat(g.edges().stream().filter(e -> e.target().equals("Z")).count()).isEqualTo(2);
    }

    @Test
    void parseDottedAndThickEdges() {
        var g = MermaidParser.parseMermaid("graph TD\n A -.-> B\n B ==> C\n C --- D");
        assertThat(g.edges().get(0).style()).isEqualTo(EdgeStyle.DOTTED);
        assertThat(g.edges().get(0).hasArrowEnd()).isTrue();
        assertThat(g.edges().get(1).style()).isEqualTo(EdgeStyle.THICK);
        assertThat(g.edges().get(2).style()).isEqualTo(EdgeStyle.SOLID);
        assertThat(g.edges().get(2).hasArrowEnd()).isFalse();
    }

    @Test
    void parseBidirectionalEdge() {
        var g = MermaidParser.parseMermaid("graph TD\n A <--> B");
        assertThat(g.edges().get(0).hasArrowStart()).isTrue();
        assertThat(g.edges().get(0).hasArrowEnd()).isTrue();
    }

    @Test
    void parseSubgraphs() {
        var txt = """
                graph TD
                    subgraph One [First]
                        A --> B
                    end
                    subgraph Two
                        C --> D
                    end
                    B --> C
                """;
        var g = MermaidParser.parseMermaid(txt);
        assertThat(g.subgraphs()).hasSize(2);
        assertThat(g.subgraphs().get(0).label()).isEqualTo("First");
        assertThat(g.subgraphs().get(0).nodeIds()).containsExactlyInAnyOrder("A", "B");
        // cross edge
        assertThat(g.edges()).anySatisfy(e -> assertThat(e.source()).isEqualTo("B"));
    }

    @Test
    void parseSubgraphWithDirectionOverride() {
        var txt = """
                graph TD
                    A --> B
                    subgraph S [Inner]
                        direction LR
                        C --> D
                    end
                """;
        var g = MermaidParser.parseMermaid(txt);
        assertThat(g.subgraphs().get(0).direction()).contains(Direction.LR);
    }

    @Test
    void parseClassDefAndStyle() {
        var txt = """
                graph TD
                    A[One] --> B[Two]
                    classDef myClass fill:#f00,stroke:#333
                    class A myClass
                    style B fill:#00f,stroke:#000
                    linkStyle 0 stroke:#ff0
                    linkStyle default stroke:#888
                """;
        var g = MermaidParser.parseMermaid(txt);
        assertThat(g.classDefs()).containsKey("myClass");
        assertThat(g.classAssignments()).containsEntry("A", "myClass");
        assertThat(g.nodeStyles()).containsKey("B");
        assertThat(g.linkStyles()).containsKey(0);
        assertThat(g.linkStyles()).containsKey("default");
    }

    @Test
    void parseStateDiagram() {
        var txt = """
                stateDiagram-v2
                    [*] --> Idle
                    Idle --> Processing: start
                    Processing --> Complete: done
                    Complete --> [*]
                """;
        var g = MermaidParser.parseMermaid(txt);
        assertThat(g.direction()).isEqualTo(Direction.TD);
        assertThat(g.nodes()).containsKey("Idle");
        // start pseudo
        assertThat(g.nodes().values().stream().anyMatch(n -> n.shape() == NodeShape.STATE_START)).isTrue();
        assertThat(g.nodes().values().stream().anyMatch(n -> n.shape() == NodeShape.STATE_END)).isTrue();
        assertThat(g.edges()).hasSize(4);
    }

    @Test
    void parseDoubleCircleAndStadium() {
        var g = MermaidParser.parseMermaid("graph TD\n A(((Double))) --> B([Stadium])");
        assertThat(g.nodes().get("A").shape()).isEqualTo(NodeShape.DOUBLE_CIRCLE);
        assertThat(g.nodes().get("B").shape()).isEqualTo(NodeShape.STADIUM);
    }

    @Test
    void parseTextEmbeddedLabel() {
        var g = MermaidParser.parseMermaid("graph TD\n A -- Hello --> B");
        assertThat(g.edges().get(0).label()).isEqualTo("Hello");
    }

    @Test
    void parseWithCommentsAndEmptyLines() {
        var g = MermaidParser.parseMermaid("%% comment\ngraph TD\n\n A --> B\n%% another\n B --> C");
        assertThat(g.edges()).hasSize(2);
    }

    @Test
    void throwsOnEmpty() {
        assertThatThrownBy(() -> MermaidParser.parseMermaid("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MermaidParser.parseMermaid("%% only comment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsOnInvalidHeader() {
        assertThatThrownBy(() -> MermaidParser.parseMermaid("graph XX\n A --> B")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseClassShorthand() {
        var g = MermaidParser.parseMermaid("graph TD\n A[Hi]:::myClass --> B");
        assertThat(g.classAssignments()).containsEntry("A", "myClass");
    }
}
