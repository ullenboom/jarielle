package com.tutego.jarielle.ui;

import com.tutego.jarielle.layout.ElkLayoutEngine;
import com.tutego.jarielle.model.MermaidGraph;
import com.tutego.jarielle.model.PositionedGraph;
import com.tutego.jarielle.model.RenderOptions;
import com.tutego.jarielle.parser.MermaidParser;
import com.tutego.jarielle.clazz.ClassDiagram;
import com.tutego.jarielle.clazz.ClassParser;
import com.tutego.jarielle.clazz.ClassLayout;
import com.tutego.jarielle.clazz.ClassRenderer;
import com.tutego.jarielle.clazz.PositionedClassDiagram;
import com.tutego.jarielle.er.ErDiagram;
import com.tutego.jarielle.er.ErParser;
import com.tutego.jarielle.er.ErLayout;
import com.tutego.jarielle.er.ErRenderer;
import com.tutego.jarielle.er.PositionedErDiagram;
import com.tutego.jarielle.sequence.PositionedSequenceDiagram;
import com.tutego.jarielle.sequence.SequenceDiagram;
import com.tutego.jarielle.sequence.SequenceLayout;
import com.tutego.jarielle.sequence.SequenceParser;
import com.tutego.jarielle.sequence.SequenceRenderer;
import com.tutego.jarielle.theme.DiagramColors;
import com.tutego.jarielle.theme.Themes;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Java2D component that visualises a Mermaid graph.
 * Port of the SVG-based rendering to Java2D — uses {@link GraphRenderer} and ELK layout.
 *
 * <p>Usage:</p>
 * <pre>
 *   var panel = new MermaidPanel();
 *   panel.setMermaidText("graph TD\n A-->B");
 *   frame.add(new JScrollPane(panel));
 * </pre>
 */
public class MermaidPanel extends JComponent {

    public enum DiagramKind { FLOWCHART, SEQUENCE, CLASS, ER }

    private MermaidGraph graph;
    private PositionedGraph positioned;
    private SequenceDiagram sequence;
    private PositionedSequenceDiagram positionedSeq;
    private ClassDiagram classDiagram;
    private PositionedClassDiagram positionedClass;
    private ErDiagram erDiagram;
    private PositionedErDiagram positionedEr;
    private DiagramKind kind = DiagramKind.FLOWCHART;
    private DiagramColors colors = DiagramColors.of("#FFFFFF", "#27272A");
    private RenderOptions options = RenderOptions.defaults();
    private String fontName = "Inter";
    private String mermaidText;
    private String errorMessage;

    public MermaidPanel() {
        setOpaque(true);
        setDoubleBuffered(true);
        // Default colors from THEMES "zinc-light"
        var zinc = Themes.get("zinc-light");
        if (zinc != null) colors = zinc;
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    public MermaidGraph getGraph() { return graph; }

    public PositionedGraph getPositionedGraph() { return positioned; }

    public SequenceDiagram getSequence() { return sequence; }

    public PositionedSequenceDiagram getPositionedSequence() { return positionedSeq; }

    public ClassDiagram getClassDiagram() { return classDiagram; }

    public PositionedClassDiagram getPositionedClass() { return positionedClass; }

    public ErDiagram getErDiagram() { return erDiagram; }

    public PositionedErDiagram getPositionedEr() { return positionedEr; }

    public DiagramKind getKind() { return kind; }

    public DiagramColors getDiagramColors() { return colors; }

    public void setDiagramColors(DiagramColors colors) {
        this.colors = Objects.requireNonNull(colors);
        repaint();
    }

    public RenderOptions getRenderOptions() { return options; }

    public void setRenderOptions(RenderOptions options) {
        this.options = options != null ? options : RenderOptions.defaults();
        if (mermaidText != null) reLayout();
        repaint();
    }

    public String getFontName() { return fontName; }

    public void setFontName(String fontName) {
        this.fontName = fontName != null ? fontName : "Inter";
        repaint();
    }

    // -------------------------------------------------------------------------
    // Graph setters
    // -------------------------------------------------------------------------

    private static DiagramKind detectKind(String text) {
        String first = Arrays.stream(text.trim().split("[\\n;]")).map(String::trim).filter(s->!s.isEmpty()).findFirst().orElse("").toLowerCase();
        if (first.startsWith("sequencediagram")) return DiagramKind.SEQUENCE;
        if (first.startsWith("xychart")) return DiagramKind.FLOWCHART; // not yet
        if (first.startsWith("classdiagram")) return DiagramKind.CLASS;
        if (first.startsWith("erdiagram")) return DiagramKind.ER;
        return DiagramKind.FLOWCHART;
    }

    public void setMermaidText(String text) {
        this.mermaidText = Objects.requireNonNull(text);
        this.errorMessage = null;
        this.kind = detectKind(text);
        try {
            List<String> lines = Arrays.stream(text.split("\n")).map(String::trim).filter(s->!s.isEmpty() && !s.startsWith("%%")).toList();
            if (kind == DiagramKind.SEQUENCE) {
                if (lines.isEmpty() || !lines.get(0).toLowerCase().startsWith("sequencediagram")) throw new IllegalArgumentException("Invalid sequenceDiagram header");
                SequenceDiagram sd = SequenceParser.parse(lines);
                setSequence(sd);
            } else if (kind == DiagramKind.CLASS) {
                if (lines.isEmpty() || !lines.get(0).toLowerCase().startsWith("classdiagram")) throw new IllegalArgumentException("Invalid classDiagram header");
                ClassDiagram cd = ClassParser.parse(lines);
                setClassDiagram(cd);
            } else if (kind == DiagramKind.ER) {
                if (lines.isEmpty() || !lines.get(0).toLowerCase().startsWith("erdiagram")) throw new IllegalArgumentException("Invalid erDiagram header");
                ErDiagram ed = ErParser.parse(lines);
                setErDiagram(ed);
            } else {
                MermaidGraph g = MermaidParser.parseMermaid(text);
                setGraph(g);
            }
        } catch (Exception ex) {
            this.graph = null; this.sequence = null; this.classDiagram = null; this.erDiagram = null;
            this.positioned = null; this.positionedSeq = null; this.positionedClass = null; this.positionedEr = null;
            this.errorMessage = ex.getMessage();
            revalidate(); repaint();
        }
    }

    public void setGraph(MermaidGraph graph) {
        this.graph = Objects.requireNonNull(graph);
        this.sequence = null; this.positionedSeq = null; this.classDiagram = null; this.positionedClass = null; this.erDiagram = null; this.positionedEr = null;
        this.kind = DiagramKind.FLOWCHART;
        this.mermaidText = null;
        this.errorMessage = null;
        reLayout();
    }

    public void setSequence(SequenceDiagram seq) {
        this.sequence = Objects.requireNonNull(seq);
        this.graph = null; this.positioned = null; this.classDiagram = null; this.positionedClass = null; this.erDiagram = null; this.positionedEr = null;
        this.kind = DiagramKind.SEQUENCE;
        this.mermaidText = null;
        this.errorMessage = null;
        reLayout();
    }

    public void setClassDiagram(ClassDiagram cd) {
        this.classDiagram = Objects.requireNonNull(cd);
        this.graph = null; this.positioned = null; this.sequence = null; this.positionedSeq = null; this.erDiagram = null; this.positionedEr = null;
        this.kind = DiagramKind.CLASS;
        this.mermaidText = null;
        this.errorMessage = null;
        reLayout();
    }

    public void setErDiagram(ErDiagram ed) {
        this.erDiagram = Objects.requireNonNull(ed);
        this.graph = null; this.positioned = null; this.sequence = null; this.positionedSeq = null; this.classDiagram = null; this.positionedClass = null;
        this.kind = DiagramKind.ER;
        this.mermaidText = null;
        this.errorMessage = null;
        reLayout();
    }

    private void reLayout() {
        if (kind == DiagramKind.SEQUENCE) {
            if (sequence == null) { positionedSeq = null; return; }
            try { positionedSeq = SequenceLayout.layout(sequence, buildEffectiveOptions()); errorMessage = null; } catch (Exception ex) { positionedSeq = null; errorMessage = ex.getMessage(); ex.printStackTrace(); }
        } else if (kind == DiagramKind.CLASS) {
            if (classDiagram == null) { positionedClass = null; return; }
            try { positionedClass = ClassLayout.layout(classDiagram, buildEffectiveOptions()); errorMessage = null; } catch (Exception ex) { positionedClass = null; errorMessage = ex.getMessage(); ex.printStackTrace(); }
        } else if (kind == DiagramKind.ER) {
            if (erDiagram == null) { positionedEr = null; return; }
            try { positionedEr = ErLayout.layout(erDiagram); errorMessage = null; } catch (Exception ex) { positionedEr = null; errorMessage = ex.getMessage(); ex.printStackTrace(); }
        } else {
            if (graph == null) { positioned = null; return; }
            try { positioned = ElkLayoutEngine.layout(graph, buildEffectiveOptions()); errorMessage = null; } catch (Exception ex) { positioned = null; errorMessage = ex.getMessage(); ex.printStackTrace(); }
        }
        revalidate(); repaint();
    }

    private RenderOptions buildEffectiveOptions() {
        // Merge colors into options for layout (spacing etc still from options)
        // DiagramColors bg/fg are not layout-affecting, just painting
        return options;
    }

    // -------------------------------------------------------------------------
    // JComponent overrides
    // -------------------------------------------------------------------------

    @Override
    public Dimension getPreferredSize() {
        if (kind == DiagramKind.SEQUENCE && positionedSeq != null) {
            int w = (int) Math.ceil(positionedSeq.width()); int h = (int) Math.ceil(positionedSeq.height()); Insets in = getInsets(); return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }
        if (kind == DiagramKind.CLASS && positionedClass != null) {
            int w = (int) Math.ceil(positionedClass.width()); int h = (int) Math.ceil(positionedClass.height()); Insets in = getInsets(); return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }
        if (kind == DiagramKind.ER && positionedEr != null) {
            int w = (int) Math.ceil(positionedEr.width()); int h = (int) Math.ceil(positionedEr.height()); Insets in = getInsets(); return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }
        if (positioned != null) {
            int w = (int) Math.ceil(positioned.width()); int h = (int) Math.ceil(positioned.height()); Insets in = getInsets(); return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }
        return new Dimension(400, 300);
    }

    @Override
    public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Background of component (outside graph)
            if (isOpaque()) {
                var pal = com.tutego.jarielle.theme.ColorUtils.derive(colors);
                g2.setColor(pal.bg());
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (errorMessage != null) {
                g2.setColor(Color.RED);
                g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                drawMultilineError(g2, errorMessage);
                return;
            }

            if (kind == DiagramKind.SEQUENCE) {
                if (positionedSeq == null) {
                    g2.setColor(Color.GRAY);
                    g2.setFont(new Font(fontName, Font.PLAIN, 13));
                    String msg = "No diagram";
                    FontMetrics fm = g2.getFontMetrics();
                    int sw = fm.stringWidth(msg);
                    g2.drawString(msg, (getWidth() - sw) / 2, getHeight() / 2);
                    return;
                }
                Insets in = getInsets();
                g2.translate(in.left, in.top);
                double extraX = Math.max(0, getWidth() - in.left - in.right - positionedSeq.width()) / 2.0;
                double extraY = Math.max(0, getHeight() - in.top - in.bottom - positionedSeq.height()) / 2.0;
                g2.translate(extraX, extraY);
                if (!options.transparent()) {
                    var pal = com.tutego.jarielle.theme.ColorUtils.derive(colors);
                    g2.setColor(pal.bg());
                    g2.fillRect(0,0,(int)positionedSeq.width(),(int)positionedSeq.height());
                }
                SequenceRenderer.render(g2, positionedSeq, colors, fontName);
                return;
            }
            if (kind == DiagramKind.CLASS) {
                if (positionedClass == null) {
                    g2.setColor(Color.GRAY);
                    g2.setFont(new Font(fontName, Font.PLAIN, 13));
                    String msg = "No diagram";
                    FontMetrics fm = g2.getFontMetrics();
                    int sw = fm.stringWidth(msg);
                    g2.drawString(msg, (getWidth() - sw) / 2, getHeight() / 2);
                    return;
                }
                Insets in = getInsets();
                g2.translate(in.left, in.top);
                double extraX = Math.max(0, getWidth() - in.left - in.right - positionedClass.width()) / 2.0;
                double extraY = Math.max(0, getHeight() - in.top - in.bottom - positionedClass.height()) / 2.0;
                g2.translate(extraX, extraY);
                if (!options.transparent()) {
                    var pal = com.tutego.jarielle.theme.ColorUtils.derive(colors);
                    g2.setColor(pal.bg());
                    g2.fillRect(0,0,(int)positionedClass.width(),(int)positionedClass.height());
                }
                ClassRenderer.render(g2, positionedClass, colors, fontName);
                return;
            }
            if (kind == DiagramKind.ER) {
                if (positionedEr == null) {
                    g2.setColor(Color.GRAY);
                    g2.setFont(new Font(fontName, Font.PLAIN, 13));
                    String msg = "No diagram";
                    FontMetrics fm = g2.getFontMetrics();
                    int sw = fm.stringWidth(msg);
                    g2.drawString(msg, (getWidth() - sw) / 2, getHeight() / 2);
                    return;
                }
                Insets in = getInsets();
                g2.translate(in.left, in.top);
                double extraX = Math.max(0, getWidth() - in.left - in.right - positionedEr.width()) / 2.0;
                double extraY = Math.max(0, getHeight() - in.top - in.bottom - positionedEr.height()) / 2.0;
                g2.translate(extraX, extraY);
                if (!options.transparent()) {
                    var pal = com.tutego.jarielle.theme.ColorUtils.derive(colors);
                    g2.setColor(pal.bg());
                    g2.fillRect(0,0,(int)positionedEr.width(),(int)positionedEr.height());
                }
                ErRenderer.render(g2, positionedEr, colors, fontName);
                return;
            }

            if (positioned == null) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font(fontName, Font.PLAIN, 13));
                String msg = "No diagram";
                FontMetrics fm = g2.getFontMetrics();
                int sw = fm.stringWidth(msg);
                g2.drawString(msg, (getWidth() - sw) / 2, getHeight() / 2);
                return;
            }

            // Translate to account for insets
            Insets in = getInsets();
            g2.translate(in.left, in.top);

            // Optionally center if component larger than graph
            double extraX = Math.max(0, getWidth() - in.left - in.right - positioned.width()) / 2.0;
            double extraY = Math.max(0, getHeight() - in.top - in.bottom - positioned.height()) / 2.0;
            g2.translate(extraX, extraY);

            // Clip to graph bounds for crispness
            GraphRenderer.render(g2, positioned, colors, fontName, options.transparent());

        } finally {
            g2.dispose();
        }
    }

    private void drawMultilineError(Graphics2D g2, String msg) {
        int y = 20;
        for (String line : msg.split("\n")) {
            g2.drawString(line, 10, y);
            y += 16;
        }
    }

    // -------------------------------------------------------------------------
    // Convenience factories
    // -------------------------------------------------------------------------

    public static MermaidPanel ofText(String mermaidText) {
        var p = new MermaidPanel();
        p.setMermaidText(mermaidText);
        return p;
    }

    public static MermaidPanel ofGraph(MermaidGraph graph) {
        var p = new MermaidPanel();
        p.setGraph(graph);
        return p;
    }
}
