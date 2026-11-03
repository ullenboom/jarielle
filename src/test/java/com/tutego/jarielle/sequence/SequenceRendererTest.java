package com.tutego.jarielle.sequence;

import com.tutego.jarielle.theme.Themes;
import com.tutego.jarielle.ui.MermaidPanel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.*;

class SequenceRendererTest {

    @Test
    void renderSequenceViaPanel() {
        var panel = new MermaidPanel();
        String txt = """
                sequenceDiagram
                    participant Alice
                    participant Bob
                    Alice->>Bob: Hello
                    Bob-->>Alice: Hi
                    Alice->>Bob: How are you?
                """;
        panel.setMermaidText(txt);
        assertThat(panel.getKind()).isEqualTo(MermaidPanel.DiagramKind.SEQUENCE);
        assertThat(panel.getPositionedSequence()).isNotNull();
        Dimension pref = panel.getPreferredSize();
        assertThat(pref.width).isGreaterThan(200);
        panel.setSize(pref);
        BufferedImage img = new BufferedImage(pref.width, pref.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertThatCode(() -> panel.paint(g2)).doesNotThrowAnyException();
        g2.dispose();
    }

    @Test
    void renderWithBlocksAndNotes() {
        var panel = new MermaidPanel();
        String txt = """
                sequenceDiagram
                    Alice->>Bob: m1
                    loop Loop
                        Alice->>Bob: m2
                    end
                    Note over Alice,Bob: Note text
                """;
        panel.setDiagramColors(Themes.get("dracula"));
        panel.setMermaidText(txt);
        assertThat(panel.getPositionedSequence().blocks()).hasSize(1);
        Dimension pref = panel.getPreferredSize();
        panel.setSize(pref);
        BufferedImage img = new BufferedImage(pref.width, pref.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertThatCode(() -> panel.paint(g2)).doesNotThrowAnyException();
        g2.dispose();
    }
}
