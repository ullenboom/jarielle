# Jarielle — beautiful-mermaid Java port

**Java 21 port of [lukilabs/beautiful-mermaid](https://github.com/lukilabs/beautiful-mermaid) (TypeScript).** Original library renders Mermaid diagrams as beautiful SVGs with ELK.js. This port renders the same diagrams with **Java2D** (`Graphics2D`) inside a Swing `JComponent`, using **Eclipse Layout Kernel (ELK)** for layout.

<img width="1357" height="887" alt="image" src="https://github.com/user-attachments/assets/c5ca9356-05b8-4100-8157-58da736f8829" />
<img width="1357" height="887" alt="image" src="https://github.com/user-attachments/assets/5277beb8-6583-49f6-a22e-fa6393ccae26" />
<img width="1357" height="887" alt="image" src="https://github.com/user-attachments/assets/ad39037a-f668-4f6c-8baa-f988a89c48f3" />
<img width="1357" height="627" alt="image" src="https://github.com/user-attachments/assets/30af529d-0647-40c4-b2ef-e6c71e497d46" />

Original work by [Craft Docs](https://craft.do) (MIT). This Java port keeps the **MIT** license.

## Features

- **6 diagram types (4 fully ported, 2 planned):**
  - Flowcharts / Graphs `graph TD/LR` including state diagrams `stateDiagram-v2` — **fully ported**
  - Sequence diagrams `sequenceDiagram` — **fully ported**
  - Class diagrams `classDiagram` — **fully ported**
  - ER diagrams `erDiagram` — **fully ported**
  - XY Charts `xychart-beta` — *not yet ported*
  - ASCII output — *intentionally omitted (Java2D only)*
- **All 14 node shapes:** `rectangle`, `rounded`, `diamond`, `stadium`, `circle`, `subroutine`, `doublecircle`, `hexagon`, `cylinder`, `asymmetric`, `trapezoid`, `trapezoid-alt`, `state-start`, `state-end`
- **3 edge styles:** `solid`, `dotted`, `thick` + `linkStyle`/`classDef`/`style` overrides
- **Subgraphs** (nested) with `direction` override
- **15 built-in themes** (`zinc-light`, `tokyo-night`, `dracula`, …) + 2-color mono mode via `DiagramColors`
- **ELK layered layout** via `org.eclipse.elk:org.eclipse.elk.core:0.12.0` (+ `xbase.lib` for `LayeredMetaDataProvider`)
- **Unified Java2D text** — `TextMetrics` only for ELK sizing, `J2dText` (`FontMetrics`/`TextLayout`) for rendering (fixes ER label centering)
- **Swing component** `MermaidPanel extends JComponent` — embeddable, scrollable, theme-switchable
- **Java 21** — records, pattern matching, `var`, `List.copyOf` (no preview features, fully compatible with LTS)

## Requirements

- JDK 21+ (tested with Temurin 21.0.10 and 26.0.1, `release 21`)
- Maven 3.9+ (`./mvnw` wrapper included)

## Installation

```xml
<dependency>
  <groupId>com.tutego</groupId>
  <artifactId>jarielle</artifactId>
  <version>1.0.0</version>
</dependency>
```

Build locally:

```bash
./mvnw verify        # compile + 59 tests + jar + sources
./mvnw test
```

Dependencies (from Central, no manual install):

- `org.eclipse.elk:org.eclipse.elk.graph|core|alg.layered|alg.common:0.12.0`
- `org.eclipse.xtext:org.eclipse.xtext.xbase.lib:2.36.0`
- `org.eclipse.xtend:org.eclipse.xtend.lib:2.36.0`
- Test: `org.junit.jupiter:junit-jupiter:6.0.1`, `org.assertj:assertj-core:3.27.4`

## Quick Start

### 1. Swing panel (recommended)

```java
import com.tutego.jarielle.ui.MermaidPanel;
import com.tutego.jarielle.theme.Themes;
import javax.swing.*;

var panel = new MermaidPanel();
panel.setMermaidText("""
    graph TD
        A[Christmas] -->|Get money| B(Go shopping)
        B --> C{Let me think}
        C -->|One| D[Laptop]
        C -->|Two| E[iPhone]
        C -->|Three| F[Car]
    """);
panel.setDiagramColors(Themes.get("dracula")); // live theme switch

JFrame f = new JFrame();
f.add(new JScrollPane(panel));
f.setSize(1100,800);
f.setVisible(true);
```

### 2. Headless rendering to PNG

```java
import com.tutego.jarielle.ui.MermaidPanel;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.*;

MermaidPanel p = new MermaidPanel();
p.setMermaidText("graph LR; A-->B-->C");
Dimension d = p.getPreferredSize();
p.setSize(d);
BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
Graphics2D g2 = img.createGraphics();
p.paint(g2); g2.dispose();
ImageIO.write(img, "png", new File("out.png"));
```

### 3. Low-level API

```java
import com.tutego.jarielle.Jarielle;
import com.tutego.jarielle.model.*;
import com.tutego.jarielle.sequence.*;
import com.tutego.jarielle.clazz.*;
import com.tutego.jarielle.er.*;

// Flowchart
MermaidGraph g = Jarielle.parseMermaid("graph TD\nA-->B");
PositionedGraph pg = Jarielle.layout(g);

// Sequence
SequenceDiagram sd = Jarielle.parseSequence("sequenceDiagram\nAlice->>Bob: hello");
PositionedSequenceDiagram psd = Jarielle.layoutSequence(sd, RenderOptions.defaults());

// Class
var cd = Jarielle.parseClass("classDiagram\nAnimal <|-- Dog");
var pcd = Jarielle.layoutClass(cd, RenderOptions.defaults());

// ER
var ed = Jarielle.parseEr("erDiagram\nCUSTOMER ||--o{ ORDER : places");
var ped = Jarielle.layoutEr(ed);
```

## Demo

```bash
./mvnw compile exec:java -Dexec.mainClass=com.tutego.jarielle.Demo
# or
java -cp target/classes:... com.tutego.jarielle.Demo
```

`src/main/java/com/tutego/jarielle/Demo.java` shows a `JFrame` with:
- Combo for 9 samples (5 flowchart/state + 2 sequence + 1 class + 1 ER)
- Theme combo (15 themes)
- Editable `JTextArea` with live `Render` button

## Theming

Two-color foundation (like original):

```java
panel.setDiagramColors(new DiagramColors("#FFFFFF","#27272A")); // mono
panel.setDiagramColors(new DiagramColors("#1a1b26","#a9b1d6","#3d59a1","#7aa2f7","#565f89",null,null));
```

All colors become `ColorUtils.DerivedPalette` via `ColorUtils.mix()` (port of `color-mix(in srgb, --fg 3..85%, --bg)` in `src/theme.ts`).

## Supported / Not Supported

| Type | Status | Notes |
|------|--------|-------|
| Flowchart `graph` | ✅ Full | `TD/TB/LR/BT/RL`, all shapes, `subgraph` + `direction`, `classDef`/`style`/`linkStyle` |
| State `stateDiagram-v2` | ✅ Full | `[*]` start/end, composite states |
| Sequence `sequenceDiagram` | ✅ Full | `participant`/`actor`, `->>`/`-->>`/`-)`/`--x`, `+`/`-` activation, `loop`/`alt`/`opt`/`par` + `else`/`and`, `Note left/right/over` |
| Class `classDiagram` | ✅ Full | `+`/`-`/`#`/`~` visibility, `<<interface>>`, `*--`/`o--`/`-->`, generics `~T~`, cardinalities |
| ER `erDiagram` | ✅ Full | `||`/`|o`/`}o`/`{o`, `PK`/`FK`/`UK`, crow's foot, `--`/`..` identifying |
| XY Chart `xychart-beta` | ❌ | Planned |
| ASCII | ❌ | Intentionally omitted (Java2D only) |
| `interactive` tooltips | ❌ | Planned |

## API Javadoc

All public APIs are documented in English:

- `com.tutego.jarielle.Jarielle` — facade (`parseMermaid`, `layout`, `parseSequence`, etc.)
- `com.tutego.jarielle.ui.MermaidPanel` — `JComponent`, `setMermaidText`, `setDiagramColors`, `setRenderOptions`
- `com.tutego.jarielle.parser.MermaidParser`, `sequence.SequenceParser`, `clazz.ClassParser`, `er.ErParser`
- `layout.ElkLayoutEngine`, `sequence.SequenceLayout`, `clazz.ClassLayout`, `er.ErLayout`
- `theme.DiagramColors`, `theme.Themes`, `theme.ColorUtils`
- `model.*`, `sequence.*`, `clazz.*`, `er.*` — immutable records

Generate:

```bash
./mvnw javadoc:javadoc
```

## License

MIT — same as original. See [`LICENSE`](LICENSE). Original copyright (c) 2026 Craft Docs, Java port (c) 2026 Jarielle contributors.

## Attribution

Based on [beautiful-mermaid](https://github.com/lukilabs/beautiful-mermaid) by Alexander Grooff (`mermaid-ascii`) and Craft. ELK layout via Eclipse ELK.

## Release 1.0

- `pom.xml:10` `1.0.0` (was `1.0.0-SNAPSHOT`)
- Tag: `v1.0.0`
- Artifacts: `jarielle-1.0.0.jar` + `sources.jar` via `maven-source-plugin`
- Requires JDK 21+, verify with `./mvnw verify`
