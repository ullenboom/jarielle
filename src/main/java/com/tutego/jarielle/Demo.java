package com.tutego.jarielle;

import com.tutego.jarielle.theme.Themes;
import com.tutego.jarielle.ui.MermaidPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Demo JFrame — shows a few sample Mermaid diagrams in MermaidPanel (Java2D).
 * Run with: {@code mvn exec:java -Dexec.mainClass=com.tutego.jarielle.Demo}
 * or via IDE.
 */
public final class Demo {

    private Demo() {}

    private static final String[] SAMPLES = {
            """
            graph TD
                A[Start] --> B{Decision}
                B -->|Yes| C[Process]
                B -->|No| D[End]
                C --> D
            """,
            """
            graph LR
                A --> B --> C --> D
                A & B --> E
                C & D --> E
            """,
            """
            graph TD
                subgraph Frontend
                    A[Web] --> B[API]
                end
                subgraph Backend
                    B --> C[(DB)]
                    B --> D[Cache]
                end
                C --> E[Done]
            """,
            """
            stateDiagram-v2
                [*] --> Idle
                Idle --> Processing: start
                Processing --> Complete: done
                Complete --> [*]
            """,
            """
            graph TD
                A[Christmas] -->|Get money| B(Go shopping)
                B --> C{Let me think}
                C -->|One| D[Laptop]
                C -->|Two| E[iPhone]
                C -->|Three| F[Car]
            """,
            """
            sequenceDiagram
                participant Alice
                participant Bob
                Alice->>Bob: Hello Bob!
                Bob-->>Alice: Hi Alice!
                Alice->>Bob: How are you?
                Bob-->>Alice: Great, thanks!
                Note over Alice,Bob: Conversation
                loop Check
                    Alice->>Bob: Ping
                    Bob-->>Alice: Pong
                end
            """,
            """
            sequenceDiagram
                actor User as User
                participant Frontend
                participant Backend
                participant DB as Database
                User->>+Frontend: Request
                Frontend->>+Backend: Query
                Backend->>+DB: SELECT
                DB-->>-Backend: Result
                Backend-->>-Frontend: Data
                Frontend-->>-User: Response
                Note right of DB: Cached
            """,
            """
            classDiagram
                class Animal {
                    <<abstract>>
                    +String name
                    +int age
                    +eat() void
                }
                class Dog {
                    +String breed
                    +bark() void
                }
                class Cat {
                    +String color
                    +meow() void
                }
                Animal <|-- Dog
                Animal <|-- Cat
                Dog *-- Tail
                Cat o-- Whisker
                class Tail { +String length }
                class Whisker { +String type }
            """,
            """
            erDiagram
                CUSTOMER {
                    string name PK
                    string email UK
                    int age
                }
                ORDER {
                    int id PK
                    string status
                }
                PRODUCT {
                    string sku PK
                    float price
                }
                CUSTOMER ||--o{ ORDER : places
                ORDER ||--|{ ORDER_ITEM : contains
                PRODUCT ||--o{ ORDER_ITEM : "is in"
            """
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Demo::createAndShow);
    }

    private static void createAndShow() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        JFrame frame = new JFrame("jarielle — beautiful-mermaid Java2D Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Theme selector
        JComboBox<String> themeBox = new JComboBox<>(Themes.all().keySet().toArray(new String[0]));
        themeBox.insertItemAt("zinc-light", 0);
        themeBox.setSelectedIndex(0);

        // Sample selector
        JComboBox<String> sampleBox = new JComboBox<>();
        for (int i = 0; i < SAMPLES.length; i++) sampleBox.addItem("Sample " + (i + 1));
        sampleBox.setSelectedIndex(0);

        // Panel
        MermaidPanel panel = MermaidPanel.ofText(SAMPLES[0]);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(panel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // Listeners
        sampleBox.addActionListener(e -> {
            int idx = sampleBox.getSelectedIndex();
            if (idx >= 0 && idx < SAMPLES.length) panel.setMermaidText(SAMPLES[idx]);
        });
        themeBox.addActionListener(e -> {
            String key = (String) themeBox.getSelectedItem();
            var colors = Themes.get(key);
            if (colors == null && "zinc-light".equals(key)) colors = Themes.DEFAULT_BG_FG;
            if (colors != null) panel.setDiagramColors(colors);
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.add(new JLabel("Sample:"));
        top.add(sampleBox);
        top.add(new JLabel("Theme:"));
        top.add(themeBox);

        frame.add(top, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);

        // Text area for editing
        JTextArea editor = new JTextArea(SAMPLES[0], 6, 40);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JButton renderBtn = new JButton("Render");
        renderBtn.addActionListener(e -> panel.setMermaidText(editor.getText()));
        sampleBox.addActionListener(e -> editor.setText(SAMPLES[sampleBox.getSelectedIndex()]));

        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        bottom.add(new JScrollPane(editor), BorderLayout.CENTER);
        bottom.add(renderBtn, BorderLayout.EAST);
        frame.add(bottom, BorderLayout.SOUTH);

        frame.setSize(1100, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
