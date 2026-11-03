package com.tutego.jarielle;

import com.tutego.jarielle.layout.ElkLayoutEngine;
import com.tutego.jarielle.model.*;
import com.tutego.jarielle.parser.MermaidParser;
import com.tutego.jarielle.sequence.PositionedSequenceDiagram;
import com.tutego.jarielle.sequence.SequenceDiagram;
import com.tutego.jarielle.sequence.SequenceLayout;
import com.tutego.jarielle.sequence.SequenceParser;
import com.tutego.jarielle.theme.DiagramColors;

import java.util.Arrays;
import java.util.List;

/**
 * Facade — Java port of {@code renderMermaidSVG} / {@code parseMermaid} from src/index.ts.
 * Provides synchronous parsing + layout + painting helpers for non-UI use.
 */
public final class Jarielle {

    private Jarielle() {}

    /** Parse Mermaid text into a logical graph. */
    public static MermaidGraph parseMermaid(String text) {
        return MermaidParser.parseMermaid(text);
    }

    /** Layout a parsed graph with default options. */
    public static PositionedGraph layout(MermaidGraph graph) {
        return ElkLayoutEngine.layout(graph, RenderOptions.defaults());
    }

    /** Layout a parsed graph with custom options. */
    public static PositionedGraph layout(MermaidGraph graph, RenderOptions options) {
        return ElkLayoutEngine.layout(graph, options);
    }

    /** Parse + layout in one step. */
    public static PositionedGraph parseAndLayout(String mermaidText) {
        return layout(parseMermaid(mermaidText));
    }

    public static PositionedGraph parseAndLayout(String mermaidText, RenderOptions options) {
        return layout(parseMermaid(mermaidText), options);
    }

    /** Build DiagramColors from render options. */
    public static DiagramColors toDiagramColors(RenderOptions opts) {
        return DiagramColors.from(opts);
    }

    /**
     * Parses a sequence diagram text.
     * @param text mermaid source starting with {@code sequenceDiagram}
     * @return parsed logical diagram
     */
    public static SequenceDiagram parseSequence(String text) {
        List<String> lines = Arrays.stream(text.split("\n")).map(String::trim).filter(s->!s.isEmpty() && !s.startsWith("%%")).toList();
        return SequenceParser.parse(lines);
    }
    /**
     * Layouts a sequence diagram.
     * @param sd logical diagram
     * @param opts render options
     * @return positioned diagram ready for painting
     */
    public static PositionedSequenceDiagram layoutSequence(SequenceDiagram sd, RenderOptions opts) {
        return SequenceLayout.layout(sd, opts);
    }
    /** Parses and layouts a sequence diagram with defaults. */
    public static PositionedSequenceDiagram parseAndLayoutSequence(String text) {
        return layoutSequence(parseSequence(text), RenderOptions.defaults());
    }
    /** Returns true if text is a sequence diagram. */
    public static boolean isSequenceDiagram(String text) {
        String first = Arrays.stream(text.trim().split("[\\n;]")).map(String::trim).filter(s->!s.isEmpty()).findFirst().orElse("").toLowerCase();
        return first.startsWith("sequencediagram");
    }

    /**
     * Parses a class diagram text.
     * @param text mermaid source starting with {@code classDiagram}
     * @return parsed diagram
     */
    public static com.tutego.jarielle.clazz.ClassDiagram parseClass(String text) {
        List<String> lines = Arrays.stream(text.split("\n")).map(String::trim).filter(s->!s.isEmpty() && !s.startsWith("%%")).toList();
        return com.tutego.jarielle.clazz.ClassParser.parse(lines);
    }
    /**
     * Layouts a class diagram.
     * @param cd logical diagram
     * @param opts options
     * @return positioned diagram
     */
    public static com.tutego.jarielle.clazz.PositionedClassDiagram layoutClass(com.tutego.jarielle.clazz.ClassDiagram cd, RenderOptions opts) {
        return com.tutego.jarielle.clazz.ClassLayout.layout(cd, opts);
    }
    /**
     * Parses an ER diagram text.
     * @param text mermaid source starting with {@code erDiagram}
     * @return parsed diagram
     */
    public static com.tutego.jarielle.er.ErDiagram parseEr(String text) {
        List<String> lines = Arrays.stream(text.split("\n")).map(String::trim).filter(s->!s.isEmpty() && !s.startsWith("%%")).toList();
        return com.tutego.jarielle.er.ErParser.parse(lines);
    }
    /**
     * Layouts an ER diagram.
     * @param ed logical diagram
     * @return positioned diagram
     */
    public static com.tutego.jarielle.er.PositionedErDiagram layoutEr(com.tutego.jarielle.er.ErDiagram ed) {
        return com.tutego.jarielle.er.ErLayout.layout(ed);
    }
}
