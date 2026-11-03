package com.tutego.jarielle.ui;

import com.tutego.jarielle.theme.Themes;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.*;

class MermaidPanelTest {

    @Test
    void panelRendersWithoutException() {
        var panel = new MermaidPanel();
        panel.setMermaidText("graph TD\n A[Hello] --> B{World}\n B -->|Yes| C[Done]");
        assertThat(panel.getPositionedGraph()).isNotNull();
        assertThat(panel.getPreferredSize().width).isGreaterThan(0);
        // Paint to offscreen image
        Dimension d = panel.getPreferredSize();
        panel.setSize(d);
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertThatCode(() -> panel.paint(g2)).doesNotThrowAnyException();
        g2.dispose();
        // Image should not be empty (has at least one non-transparent pixel)
        boolean hasContent = false;
        for (int x = 0; x < img.getWidth() && !hasContent; x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if ((img.getRGB(x, y) >>> 24) != 0) { hasContent = true; break; }
            }
        }
        assertThat(hasContent).isTrue();
    }

    @Test
    void panelHandlesErrorGracefully() {
        var panel = new MermaidPanel();
        panel.setMermaidText("not a valid header");
        assertThat(panel.getPositionedGraph()).isNull();
        Dimension d = panel.getPreferredSize();
        panel.setSize(d);
        BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertThatCode(() -> panel.paint(g2)).doesNotThrowAnyException();
        g2.dispose();
    }

    @Test
    void panelThemeSwitch() {
        var panel = new MermaidPanel();
        panel.setMermaidText("graph LR\n A --> B --> C");
        panel.setDiagramColors(Themes.get("dracula"));
        assertThat(panel.getDiagramColors().bg()).isEqualTo("#282a36");
        Dimension d = panel.getPreferredSize();
        panel.setSize(d);
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertThatCode(() -> panel.paint(g2)).doesNotThrowAnyException();
        g2.dispose();
    }

    @Test
    void panelWithAllShapes() {
        String txt = """
                graph TD
                    A[Rect] --> B(Rounded)
                    B --> C{Diamond}
                    C --> D([Stadium])
                    D --> E((Circle))
                    E --> F[[Sub]]
                    F --> G{{Hex}}
                    G --> H[(Cyl)]
                    H --> I>Flag]
                    I --> J[/Trap\\]
                    J --> K[\\Trap2/]
                    K --> L(((Double)))
                """;
        var panel = new MermaidPanel();
        panel.setMermaidText(txt);
        assertThat(panel.getPositionedGraph().nodes()).hasSize(12);
        Dimension d = panel.getPreferredSize();
        panel.setSize(d);
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertThatCode(() -> panel.paint(g2)).doesNotThrowAnyException();
        g2.dispose();
    }
}
