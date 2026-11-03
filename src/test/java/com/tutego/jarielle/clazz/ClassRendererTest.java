package com.tutego.jarielle.clazz;

import com.tutego.jarielle.ui.MermaidPanel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.*;

class ClassRendererTest {

    @Test
    void renderClassViaPanel() {
        var p = new MermaidPanel();
        String txt = """
                classDiagram
                    class Animal {
                        +String name
                        +eat() void
                    }
                    Animal <|-- Dog
                    Animal <|-- Cat
                """;
        p.setMermaidText(txt);
        assertThat(p.getKind()).isEqualTo(MermaidPanel.DiagramKind.CLASS);
        assertThat(p.getPositionedClass()).isNotNull();
        Dimension pref = p.getPreferredSize();
        p.setSize(pref);
        BufferedImage img = new BufferedImage(pref.width, pref.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertThatCode(() -> p.paint(g2)).doesNotThrowAnyException();
        g2.dispose();
    }
}
