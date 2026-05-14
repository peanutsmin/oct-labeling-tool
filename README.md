# OCT Labeling Tool

**English** | [Deutsch](#deutsch) | [íêµ­ì´](#íêµ­ì´)

---

## English

### Background

During a medical imaging lecture on early lung cancer detection, I learned that peripheral lung lesions found via low-dose CT screening are very difficult to biopsy using conventional bronchoscopy.

Optical biopsy based on OCT is emerging as a radiation-free future diagnostic approach. However, lung OCT images may still suffer from limited penetration depth, speckle noise, motion artifacts, and interpretation challenges. This project focuses on a first practical step toward AI-assisted analysis: building a structured annotation workflow for imperfect medical imaging data.

While general annotation tools like CVAT and Label Studio exist, I wanted to explore a lightweight desktop workflow specifically tailored to lung OCT lesion annotation.

I built this tool as an experimental MVP to explore an AI dataset annotation workflow for lung OCT images. It currently supports common image files, uncompressed grayscale DICOM preview loading, bounding-box annotation, freehand polygon mask annotation, JSON export, YOLO-format export, COCO-format export, mask JSON/PNG export, project save/load, coordinate validation, zoomable image review, brightness/contrast preview controls, image-level review tracking, and pre-export dataset validation.

### What I Built

A lightweight desktop tool for annotating lung OCT images, designed to generate labeled datasets for future AI training.

![Screenshot](screenshot.png)

### Features

- Load multiple images (jpg, jpeg, png, bmp, dcm, dicom, ima)
- Preview uncompressed 8-bit and 16-bit grayscale DICOM images
- Draw bounding boxes with mouse drag
- Move and resize existing bounding boxes
- Draw freehand polygon masks in mask mode
- Change labels on selected bounding boxes or masks
- Zoom in/out with toolbar controls or Ctrl + mouse wheel
- Pan around zoomed images with scrollbars, trackpad scrolling, Alt + drag, or middle-button drag
- Preview brightness and contrast adjustments without modifying source images
- Label regions: Normal / Suspicious / Confirmed Cancer
- Keyboard shortcuts: â/â to navigate, 1/2/3 to select label
- Labels persist when switching between images
- Mark each image as reviewed and track overall dataset progress and label totals
- Review a validation report before export
- Right-click or two-finger tap to delete a box
- Multilingual UI: Korean / English / German
- Export to JSON with normalized coordinates
- Export to YOLO format for object detection training
- Export to COCO format for object detection datasets
- Export segmentation masks as `masks.json` and grayscale PNG masks
- Save summary statistics to `summary.json`
- Save YOLO helper files: `classes.txt`, `data.yaml`, and an `images/` copy folder when source files are available
- Choose export folders for JSON, YOLO, and COCO outputs
- Project save/load using `project.json`, including reviewed image status
- Project files preserve image width and height even for reviewed images with no bounding boxes
- Coordinate clamping for safer dataset export
- Automatic discard of bounding boxes smaller than 5Ã5 pixels

### Tech Stack

- Java 21
- JavaFX 21
- Maven
- Gson
- dcm4che
- JUnit


### Download

Pre-built releases are available on the [Releases page](https://github.com/peanutsmin/oct-labeling-tool/releases).

Download the latest `oct-labeling-tool-*.jar` and run it with:

```bash
java -jar oct-labeling-tool-*.jar
```

> **Requirements:** Java 21 and JavaFX 21 SDK must be installed. See [How to Run](#how-to-run) for details.

### How to Run

#### Requirements

- Java 21
- JavaFX 21 SDK
- Maven

#### Run with Maven

```bash
mvn javafx:run
```

#### Build a runnable package

```bash
mvn package
java -jar target/oct-labeling-tool-1.0-SNAPSHOT.jar
```

The package step creates the app jar and copies runtime dependencies into `target/lib`.

#### Manual Compile

```bash
javac --module-path /path/to/javafx/lib --add-modules javafx.controls \
  -cp /path/to/gson.jar \
  src/main/java/com/peanutsmin/octlabeling/*.java
```

#### macOS Manual Example

```bash
javac --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls \
  -cp $HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar \
  src/main/java/com/peanutsmin/octlabeling/*.java
java --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls \
  -cp src/main/java:$HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar \
  com.peanutsmin.octlabeling.MainApp
```

### Workflow

1. Load OCT images
2. Draw bounding boxes with mouse drag
3. Assign labels: Normal / Suspicious / Confirmed Cancer
4. Mark reviewed images and monitor overall dataset progress
5. Save the project as `project.json` for future sessions
6. Reopen the project to continue or correct annotations
7. Review the validation report before export
8. Export annotations as JSON, YOLO, or COCO format for AI training

### Export Format Examples

#### JSON Export

```json
[
  {
    "file": "image.jpeg",
    "label": "suspicious",
    "x": 0.2425,
    "y": 0.1897,
    "w": 0.18,
    "h": 0.12,
    "x_pixel": 72,
    "y_pixel": 31,
    "image_width": 300,
    "image_height": 168
  }
]
```

#### YOLO Export

YOLO export writes a dataset-style folder with:

```txt
labels_yolo/
images/
classes.txt
data.yaml
```

```txt
1 0.332500 0.249700 0.180000 0.120000
```

YOLO format:

```txt
class_id x_center y_center width height
```

Class mapping:

```txt
0 = Normal
1 = Suspicious
2 = Confirmed Cancer
```

#### COCO Export

COCO export writes `coco_annotations.json` with `images`, `annotations`, and `categories` arrays:

```json
{
  "images": [
    { "id": 1, "file_name": "image.jpeg", "width": 300, "height": 168 }
  ],
  "annotations": [
    {
      "id": 1,
      "image_id": 1,
      "category_id": 1,
      "bbox": [72, 31, 54, 20],
      "area": 1080,
      "iscrowd": 0
    }
  ],
  "categories": [
    { "id": 0, "name": "normal", "supercategory": "lung_oct" },
    { "id": 1, "name": "suspicious", "supercategory": "lung_oct" },
    { "id": 2, "name": "confirmed_cancer", "supercategory": "lung_oct" }
  ]
}
```

#### Label Schema

Exported JSON uses stable machine-readable labels:

```txt
normal = Normal
suspicious = Suspicious
confirmed_cancer = Confirmed Cancer
```

These values are independent from the selected UI language.

### Limitations

- This tool is an experimental MVP and is not intended for clinical diagnosis.
- It currently supports bounding boxes and freehand polygon masks.
- DICOM support is limited to uncompressed single-channel grayscale images.
- Mask export writes one grayscale class-index mask PNG per image; overlapping masks use the later mask value.
- Zoom changes only the display scale; exported coordinates remain normalized to the original image size and should still be validated before research use.
- Brightness and contrast controls are display-only previews; exported image files and annotation coordinates are not modified.

### Validation

- Project files are saved and loaded using Gson with user-visible success/failure alerts.
- DICOM loading is covered by a generated uncompressed grayscale DICOM fixture test.
- Mask project save/load and JSON/PNG export behavior are covered by JUnit tests.
- Reviewed image status is saved and restored with project files.
- Pre-export validation reports unreviewed images, empty images, invalid boxes, clipped boxes, tiny boxes, exportable label count, and skewed label distribution.
- YOLO export uses normalized center coordinates: `x_center`, `y_center`, `width`, `height`.
- COCO export uses pixel bounding boxes: `x`, `y`, `width`, `height`, and keeps image dimensions for reviewed empty images.
- JSON, YOLO, and COCO exports share the same coordinate clamping logic before writing files.
- Export behavior is covered by JUnit tests, including out-of-bounds box clipping, COCO output, empty-image metadata, and locale-safe YOLO decimals.
- Bounding boxes smaller than 5Ã5 pixels are automatically discarded.

---

## Deutsch

### Hintergrund

WÃ¤hrend einer Vorlesung Ã¼ber FrÃ¼herkennung von Lungenkrebs erfuhr ich, dass periphere LungenlÃ¤sionen, die im Low-dose-CT-Screening entdeckt werden, mit konventioneller Bronchoskopie sehr schwer zu biopsieren sind.

Optische Biopsie auf OCT-Basis gilt als strahlungsfreier diagnostischer Ansatz der Zukunft. Allerdings kÃ¶nnen Lungen-OCT-Bilder durch begrenzte Eindringtiefe, Speckle-Rauschen, Bewegungsartefakte und schwierige Interpretierbarkeit beeintrÃ¤chtigt sein. Dieses Projekt konzentriert sich deshalb auf einen ersten praktischen Schritt in Richtung AI-gestÃ¼tzter Analyse: einen strukturierten Annotation-Workflow fÃ¼r unvollkommene medizinische Bilddaten. Obwohl allgemeine Annotationstools wie CVAT und Label Studio existieren, wollte ich einen leichtgewichtigen Desktop-Workflow speziell fÃ¼r die Annotation von Lungen-OCT-LÃ¤sionen experimentell umsetzen.

Dieses Tool wurde als experimentelles MVP entwickelt, um einen Workflow zur Erstellung von AI-TrainingsdatensÃ¤tzen fÃ¼r Lungen-OCT-Bilder zu untersuchen. Es unterstÃ¼tzt aktuell gÃ¤ngige Bilddateien, Vorschau unkomprimierter Graustufen-DICOM-Dateien, Bounding-Box-Annotation, Freihand-Polygonmasken, JSON-Export, YOLO-Format-Export, COCO-Format-Export, Masken-Export als JSON/PNG, Projekt-Speichern/Laden, Koordinatenvalidierung, zoombare BildprÃ¼fung, Helligkeits- und Kontrastvorschau, Review-Tracking pro Bild sowie Datensatzvalidierung vor dem Export.

### Was ich gebaut habe

Ein leichtgewichtiges Desktop-Tool zur Annotation von Lungen-OCT-Bildern, entwickelt zur Erstellung gelabelter DatensÃ¤tze fÃ¼r zukÃ¼nftiges AI-Training.

![Screenshot](screenshot.png)

### Funktionen

- Mehrere Bilder laden (jpg, jpeg, png, bmp, dcm, dicom, ima)
- Unkomprimierte 8-Bit- und 16-Bit-Graustufen-DICOM-Bilder als Vorschau laden
- Bounding Boxes per Maus-Drag zeichnen
- Vorhandene Bounding Boxes verschieben und skalieren
- Freihand-Polygonmasken im Maskenmodus zeichnen
- Labels ausgewÃ¤hlter Bounding Boxes oder Masken Ã¤ndern
- Per Toolbar oder Ctrl + Mausrad hinein- und herauszoomen
- In gezoomten Bildern per Scrollbar, Trackpad, Alt + Drag oder mittlerer Maustaste navigieren
- Helligkeit und Kontrast als Vorschau anpassen, ohne Quelldateien zu verÃ¤ndern
- Regionen beschriften: Normal / VerdÃ¤chtig / BestÃ¤tigter Krebs
- TastaturkÃ¼rzel: â/â navigieren, 1/2/3 Label wÃ¤hlen
- Labels bleiben beim Wechsel zwischen Bildern erhalten
- Bilder als geprÃ¼ft markieren und Gesamtfortschritt sowie Label-Gesamtzahlen verfolgen
- Vor dem Export einen Validierungsbericht prÃ¼fen
- Rechtsklick oder Zwei-Finger-Tap zum LÃ¶schen einer Box
- Mehrsprachige OberflÃ¤che: Koreanisch / Englisch / Deutsch
- JSON-Export mit normalisierten Koordinaten
- YOLO-Format-Export fÃ¼r Object-Detection-Training
- COCO-Format-Export fÃ¼r Object-Detection-DatensÃ¤tze
- Segmentierungsmasken als `masks.json` und Graustufen-PNGs exportieren
- Summary-Statistiken werden in `summary.json` gespeichert
- YOLO-Hilfsdateien: `classes.txt`, `data.yaml` und ein `images/`-Kopierordner, wenn Quelldateien vorhanden sind
- Exportordner fÃ¼r JSON-, YOLO- und COCO-Ausgaben auswÃ¤hlbar
- Projekt speichern/laden mit `project.json`, inklusive geprÃ¼ftem Bildstatus
- Projektdateien behalten Bildbreite und BildhÃ¶he auch fÃ¼r geprÃ¼fte Bilder ohne Bounding Boxes
- Koordinaten-Clamping fÃ¼r sicheren Datensatz-Export
- Automatisches Verwerfen von Bounding Boxes kleiner als 5Ã5 Pixel

### Technologie

- Java 21
- JavaFX 21
- Maven
- Gson
- dcm4che
- JUnit

### AusfÃ¼hren

#### Voraussetzungen

- Java 21
- JavaFX 21 SDK
- Maven

#### Mit Maven ausfÃ¼hren

```bash
mvn javafx:run
```

#### AusfÃ¼hrbares Paket bauen

```bash
mvn package
java -jar target/oct-labeling-tool-1.0-SNAPSHOT.jar
```

Der Paket-Build erstellt das App-Jar und kopiert Runtime-AbhÃ¤ngigkeiten nach `target/lib`.

#### Manuell kompilieren

```bash
javac --module-path /path/to/javafx/lib --add-modules javafx.controls \
  -cp /path/to/gson.jar \
  src/main/java/com/peanutsmin/octlabeling/*.java
```

### Workflow

1. OCT-Bilder laden
2. Bounding Boxes per Maus-Drag zeichnen
3. Label zuweisen: Normal / VerdÃ¤chtig / BestÃ¤tigter Krebs
4. GeprÃ¼fte Bilder markieren und Gesamtfortschritt kontrollieren
5. Projekt als `project.json` speichern
6. Projekt erneut Ã¶ffnen und Annotationen fortsetzen oder korrigieren
7. Validierungsbericht vor dem Export prÃ¼fen
8. Annotationen als JSON, YOLO- oder COCO-Format fÃ¼r AI-Training exportieren

### Export-Beispiele

#### JSON-Export

```json
[
  {
    "file": "image.jpeg",
    "label": "suspicious",
    "x": 0.2425,
    "y": 0.1897,
    "w": 0.18,
    "h": 0.12,
    "x_pixel": 72,
    "y_pixel": 31,
    "image_width": 300,
    "image_height": 168
  }
]
```

#### YOLO-Export

YOLO-Export schreibt einen dataset-artigen Ordner:

```txt
labels_yolo/
images/
classes.txt
data.yaml
```

```txt
1 0.332500 0.249700 0.180000 0.120000
```

YOLO-Format:

```txt
class_id x_center y_center width height
```

Klassen-Zuordnung:

```txt
0 = Normal
1 = VerdÃ¤chtig
2 = BestÃ¤tigter Krebs
```

#### COCO-Export

Der COCO-Export schreibt `coco_annotations.json` mit `images`, `annotations` und `categories`:

```json
{
  "images": [
    { "id": 1, "file_name": "image.jpeg", "width": 300, "height": 168 }
  ],
  "annotations": [
    {
      "id": 1,
      "image_id": 1,
      "category_id": 1,
      "bbox": [72, 31, 54, 20],
      "area": 1080,
      "iscrowd": 0
    }
  ],
  "categories": [
    { "id": 0, "name": "normal", "supercategory": "lung_oct" },
    { "id": 1, "name": "suspicious", "supercategory": "lung_oct" },
    { "id": 2, "name": "confirmed_cancer", "supercategory": "lung_oct" }
  ]
}
```

#### Label-Schema

Der JSON-Export verwendet stabile maschinenlesbare Labels:

```txt
normal = Normal
suspicious = VerdÃ¤chtig
confirmed_cancer = BestÃ¤tigter Krebs
```

Diese Werte sind unabhÃ¤ngig von der gewÃ¤hlten UI-Sprache.

### EinschrÃ¤nkungen

- Dieses Tool ist ein experimentelles MVP und nicht fÃ¼r klinische Diagnosen geeignet.
- Aktuell werden Bounding Boxes und Freihand-Polygonmasken unterstÃ¼tzt.
- DICOM-UnterstÃ¼tzung ist auf unkomprimierte einkanalige Graustufenbilder beschrÃ¤nkt.
- Maskenexport schreibt pro Bild eine Graustufen-PNG mit Klassenindizes; Ã¼berlappende Masken verwenden den spÃ¤teren Maskenwert.
- Zoom Ã¤ndert nur die AnzeigegrÃ¶Ãe; exportierte Koordinaten bleiben auf die OriginalbildgrÃ¶Ãe normalisiert und sollten vor Forschungseinsatz weiterhin validiert werden.
- Helligkeits- und Kontraststeuerung sind reine Anzeigevorschauen; exportierte Bilddateien und Annotation-Koordinaten werden nicht verÃ¤ndert.

### Validierung

- Projektdateien werden mit Gson gespeichert und geladen, mit sichtbaren Erfolgs- und Fehlermeldungen.
- DICOM-Laden wird durch einen generierten unkomprimierten Graustufen-DICOM-Test abgedeckt.
- Speichern/Laden von Maskenprojekten sowie JSON/PNG-Maskenexport werden durch JUnit-Tests abgedeckt.
- Der geprÃ¼fte Bildstatus wird mit Projektdateien gespeichert und wiederhergestellt.
- Die Validierung vor dem Export meldet ungeprÃ¼fte Bilder, Bilder ohne Annotationen, ungÃ¼ltige Boxen, zugeschnittene Boxen, sehr kleine Boxen, exportierbare Label-Anzahl und unausgewogene Label-Verteilung.
- YOLO-Export verwendet normalisierte Mittelpunkt-Koordinaten: `x_center`, `y_center`, `width`, `height`.
- COCO-Export verwendet Pixel-Bounding-Boxes und behÃ¤lt Bilddimensionen fÃ¼r geprÃ¼fte leere Bilder: `x`, `y`, `width`, `height`.
- JSON-, YOLO- und COCO-Export verwenden dieselbe Koordinatenbegrenzung vor dem Schreiben.
- Das Exportverhalten wird durch JUnit-Tests geprÃ¼ft, inklusive Clipping auÃerhalb des Bildbereichs, COCO-Ausgabe, Metadaten fÃ¼r leere Bilder und locale-sicherer YOLO-Dezimalzahlen.
- Bounding Boxes kleiner als 5Ã5 Pixel werden automatisch verworfen.

---

## íêµ­ì´

### ë°°ê²½

íì ì¡°ê¸° ì§ë¨ ê°ììì ì ì ë CT ì¤í¬ë¦¬ëì¼ë¡ ë°ê²¬ë ë§ì´ í ë³ë³ì ê¸°ì¡´ ê¸°ê´ì§ë´ìê²½ì¼ë¡ ìê²íê¸° ë§¤ì° ì´ë µë¤ë ê²ì ë°°ì ìµëë¤.

OCT ê¸°ë° ê´í ìê²ì ë°©ì¬ì  ë¶ë´ì´ ìë ë¯¸ë ì§ë¨ ì ê·¼ë²ì¼ë¡ ì£¼ëª©ë°ê³  ììµëë¤. CVAT, Label Studio ê°ì ë²ì© ë¼ë²¨ë§ í´ì ì¡´ì¬íì§ë§, í OCT ë³ë³ ë¼ë²¨ë§ì ë§ì¶ ê°ë²¼ì´ ë°ì¤í¬í± ìí¬íë¡ì°ë¥¼ ì¤íì ì¼ë¡ êµ¬íí´ë³´ê³  ì¶ììµëë¤. ë¤ë§ í OCT ììì ì¹¨í¬ ê¹ì´ ì í, speckle noise, motion artifact, í´ì ê¸°ì¤ì ì´ë ¤ì ê°ì ë¬¸ì ê° ìì´ í­ì ì ëªíê³  ì¼ê´ë ë°ì´í°ë¡ ì»ì´ì§ì§ë ììµëë¤. ì´ íë¡ì í¸ë ì´ë¬í íê³ ììì AI ë³´ì¡° ë¶ìì ìí ì²« ë¨ê³ì¸ êµ¬ì¡°íë annotation workflowë¥¼ ì¤ííë ë° ì´ì ì ëììµëë¤.

AI íìµì© ë°ì´í°ì êµ¬ì¶ ê³¼ì ì ì§ì  ì¤íí´ë³´ê¸° ìí´ ì´ í´ì ì ìíìµëë¤. íì¬ ì¼ë° ì´ë¯¸ì§ íì¼, ë¹ìì¶ grayscale DICOM ë¯¸ë¦¬ë³´ê¸°, bounding-box annotation, freehand polygon mask annotation, JSON export, YOLO format export, COCO format export, mask JSON/PNG export, íë¡ì í¸ ì ì¥/ë¶ë¬ì¤ê¸°, ì¢í ê²ì¦, íë ê°ë¥í ì´ë¯¸ì§ ê²í , ë°ê¸°/ëë¹ ë¯¸ë¦¬ë³´ê¸°, ì´ë¯¸ì§ë³ ê²ì ìí ì¶ì , ì ì²´ ë¼ë²¨ íµê³ íì¸, export ì  ë°ì´í°ì ê²ì¦ ê¸°ë¥ì ì§ìí©ëë¤.

### ì ìí ê²

í OCT ì´ë¯¸ì§ë¥¼ ë¼ë²¨ë§íê³ , í¥í AI íìµì© ë°ì´í°ìì ë§ë¤ê¸° ìí ê°ë²¼ì´ ë°ì¤í¬í± annotation toolìëë¤.

![Screenshot](screenshot.png)

### ê¸°ë¥

- ì¬ë¬ ì¥ ì´ë¯¸ì§ ì í (jpg, jpeg, png, bmp, dcm, dicom, ima)
- ë¹ìì¶ 8-bit/16-bit grayscale DICOM ì´ë¯¸ì§ ë¯¸ë¦¬ë³´ê¸°
- ë§ì°ì¤ ëëê·¸ë¡ bounding box ê·¸ë¦¬ê¸°
- ê¸°ì¡´ bounding box ì´ë ë° ë¦¬ì¬ì´ì¦
- mask modeìì freehand polygon mask ê·¸ë¦¬ê¸°
- ì íí bounding box ëë mask ë¼ë²¨ ë³ê²½
- í´ë° ë²í¼ ëë Ctrl + ë§ì°ì¤ í ë¡ íë/ì¶ì
- íëë ì´ë¯¸ì§ë ì¤í¬ë¡¤ë°, í¸ëí¨ë ì¤í¬ë¡¤, Alt + ëëê·¸, ì¤ê° ë²í¼ ëëê·¸ë¡ ì´ë
- ìë³¸ ì´ë¯¸ì§ë¥¼ ìì íì§ ìë ë°ê¸°/ëë¹ ë¯¸ë¦¬ë³´ê¸° ì¡°ì 
- ì ì / ìì¬ / íì¤í ì ë¼ë²¨ ì í
- ë¨ì¶í¤: â/â ì´ë¯¸ì§ ì´ë, 1/2/3 ë¼ë²¨ ì í
- ì´ë¯¸ì§ ì í ì ë¼ë²¨ ì ì§
- ì´ë¯¸ì§ë³ ê²ì ìë£ íì ë° ì ì²´ ë°ì´í°ì ì§íë¥ /ë¼ë²¨ íµê³ íì¸
- export ì  validation report íì¸
- ì°í´ë¦­ ëë ë ìê°ë½ í­ì¼ë¡ ë°ì¤ ì­ì 
- íêµ­ì´ / English / Deutsch UI ì í
- ì ê·íë ì¢íë¡ JSON export
- AI ê°ì²´íì§ íìµì© YOLO format export
- ê°ì²´íì§ ë°ì´í°ìì© COCO format export
- segmentation maskë¥¼ `masks.json`ê³¼ grayscale PNG maskë¡ export
- `summary.json`ì íµê³ ì ì¥
- YOLO ë³´ì¡° íì¼ ì ì¥: `classes.txt`, `data.yaml`, ìë³¸ ì´ë¯¸ì§ê° ì¡´ì¬í  ê²½ì° `images/` ë³µì¬ í´ë
- JSON/YOLO/COCO export í´ë ì í
- ê²ì ìë£ ìíë¥¼ í¬í¨í `project.json` ê¸°ë° íë¡ì í¸ ì ì¥/ë¶ë¬ì¤ê¸°
- bounding boxê° ìë ê²ì ìë£ ì´ë¯¸ì§ë íë¡ì í¸ì COCO exportìì ì´ë¯¸ì§ ëë¹/ëì´ ì ì§
- ìì í ë°ì´í°ì exportë¥¼ ìí ì¢í clamp
- 5Ã5 í½ìë³´ë¤ ìì bounding box ìë ì ì¸

### ê¸°ì  ì¤í

- Java 21
- JavaFX 21
- Maven
- Gson
- dcm4che
- JUnit

### ì¤í ë°©ë²

#### ìêµ¬ì¬í­

- Java 21
- JavaFX 21 SDK
- Maven

#### Mavenì¼ë¡ ì¤í

```bash
mvn javafx:run
```

#### ì¤í ê°ë¥í í¨í¤ì§ ë¹ë

```bash
mvn package
java -jar target/oct-labeling-tool-1.0-SNAPSHOT.jar
```

`mvn package`ë ì± jarì ì¤íì íìí runtime dependencyë¥¼ `target/lib`ì í¨ê» ë³µì¬í©ëë¤.

#### ìë ì»´íì¼

```bash
javac --module-path /path/to/javafx/lib --add-modules javafx.controls \
  -cp /path/to/gson.jar \
  src/main/java/com/peanutsmin/octlabeling/*.java
```

#### macOS ìë ì¤í ìì

```bash
javac --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls \
  -cp $HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar \
  src/main/java/com/peanutsmin/octlabeling/*.java
java --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls \
  -cp src/main/java:$HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar \
  com.peanutsmin.octlabeling.MainApp
```

### ì¬ì© íë¦

1. OCT ì´ë¯¸ì§ ë¶ë¬ì¤ê¸°
2. ë§ì°ì¤ ëëê·¸ë¡ bounding box ê·¸ë¦¬ê¸°
3. ì ì / ìì¬ / íì¤í ì ë¼ë²¨ ì í
4. ê²ì ìë£ ì´ë¯¸ì§ë¥¼ íìíê³  ì ì²´ ë°ì´í°ì ì§íë¥  íì¸
5. ëì¤ì ì´ì´ì ììí  ì ìëë¡ `project.json`ì¼ë¡ íë¡ì í¸ ì ì¥
6. íë¡ì í¸ë¥¼ ë¤ì ì´ì´ annotationì ì´ì´ì ìì 
7. export ì  validation report íì¸
8. AI íìµì©ì¼ë¡ JSON, YOLO ëë COCO íì export

### Export ìì

#### JSON Export

```json
[
  {
    "file": "image.jpeg",
    "label": "suspicious",
    "x": 0.2425,
    "y": 0.1897,
    "w": 0.18,
    "h": 0.12,
    "x_pixel": 72,
    "y_pixel": 31,
    "image_width": 300,
    "image_height": 168
  }
]
```

#### YOLO Export

YOLO exportë ë¤ìê³¼ ê°ì dataset-style í´ëë¥¼ ìì±í©ëë¤:

```txt
labels_yolo/
images/
classes.txt
data.yaml
```

```txt
1 0.332500 0.249700 0.180000 0.120000
```

YOLO íì:

```txt
class_id x_center y_center width height
```

í´ëì¤ ë§¤í:

```txt
0 = ì ì
1 = ìì¬
2 = íì¤í ì
```

#### COCO Export

COCO exportë `images`, `annotations`, `categories` ë°°ì´ì í¬í¨í `coco_annotations.json`ì ìì±í©ëë¤:

```json
{
  "images": [
    { "id": 1, "file_name": "image.jpeg", "width": 300, "height": 168 }
  ],
  "annotations": [
    {
      "id": 1,
      "image_id": 1,
      "category_id": 1,
      "bbox": [72, 31, 54, 20],
      "area": 1080,
      "iscrowd": 0
    }
  ],
  "categories": [
    { "id": 0, "name": "normal", "supercategory": "lung_oct" },
    { "id": 1, "name": "suspicious", "supercategory": "lung_oct" },
    { "id": 2, "name": "confirmed_cancer", "supercategory": "lung_oct" }
  ]
}
```

#### ë¼ë²¨ ì¤í¤ë§

JSON exportë UI ì¸ì´ì ë¬´ê´í ê³ ì  ë¼ë²¨ ê°ì ì¬ì©í©ëë¤:

```txt
normal = ì ì
suspicious = ìì¬
confirmed_cancer = íì¤í ì
```

### íê³ ë° ì£¼ìì¬í­

- ì´ í´ì ì¤íì  MVPì´ë©° ìì ì§ë¨ ëª©ì ì¼ë¡ ì¬ì©í  ì ììµëë¤.
- íì¬ bounding boxì freehand polygon mask annotationì ì§ìí©ëë¤.
- DICOM ì§ìì ë¹ìì¶ single-channel grayscale ì´ë¯¸ì§ë¡ ì íë©ëë¤.
- mask exportë ì´ë¯¸ì§ë³ grayscale class-index PNGë¥¼ ìì±íë©°, ê²¹ì¹ë maskë ëì¤ì ê·¸ë¦° mask ê°ì´ ì°ì í©ëë¤.
- Zoomì íë©´ íì ë°°ì¨ë§ ë°ê¾¸ë©°, export ì¢íë ìë³¸ ì´ë¯¸ì§ í¬ê¸° ê¸°ì¤ì¼ë¡ ì ê·íë©ëë¤. ì°êµ¬ì©ì¼ë¡ ì¬ì©íê¸° ì ìë ì¬ì í ê²ì¦ì´ íìí©ëë¤.
- ë°ê¸°/ëë¹ ì¡°ì ì íë©´ íìì© ë¯¸ë¦¬ë³´ê¸°ì´ë©°, export ì´ë¯¸ì§ íì¼ê³¼ annotation ì¢íë ë³ê²½íì§ ììµëë¤.

### ê²ì¦

- íë¡ì í¸ íì¼ì Gsonì¼ë¡ ì ì¥íê³  ë¶ë¬ì¤ë©°, ì±ê³µ/ì¤í¨ë¥¼ UI Alertë¡ íìí©ëë¤.
- DICOM ë¡ë©ì íì¤í¸ìì ìì±í ë¹ìì¶ grayscale DICOM fixtureë¡ ê²ì¦í©ëë¤.
- mask project ì ì¥/ë¶ë¬ì¤ê¸°ì JSON/PNG mask exportë JUnit íì¤í¸ë¡ ê²ì¦í©ëë¤.
- ì´ë¯¸ì§ë³ ê²ì ìë£ ìíë project íì¼ì ì ì¥ëê³  ë¤ì ë¶ë¬ì¬ ì ììµëë¤.
- ìíë°ìì ì ì²´ ì´ë¯¸ì§ ì, ê²ì ìë£ ì´ë¯¸ì§ ì, ë¼ë²¨ë³ ì ì²´ ê°ìë¥¼ íì¸í  ì ììµëë¤.
- export ì  ê²ì¦ì ë¯¸ê²ì ì´ë¯¸ì§, annotation ìë ì´ë¯¸ì§, ì í¨íì§ ìì ë°ì¤, ì´ë¯¸ì§ ê²½ê³ìì ìë¦¬ë ë°ì¤, 5x5 í½ìë³´ë¤ ìì ë°ì¤, export ê°ë¥í ë¼ë²¨ ì, íìª½ì¼ë¡ ì¹ì°ì¹ ë¼ë²¨ ë¶í¬ë¥¼ ìë ¤ì¤ëë¤.
- YOLO exportë ì ê·íë ì¤ì¬ ì¢íë¥¼ ì¬ì©í©ëë¤: `x_center`, `y_center`, `width`, `height`.
- COCO exportë í½ì bounding boxë¥¼ ì¬ì©íë©°, bounding boxê° ìë ê²ì ì´ë¯¸ì§ì ì´ë¯¸ì§ í¬ê¸°ë ì ì§í©ëë¤: `x`, `y`, `width`, `height`.
- JSON, YOLO, COCO exportë ê°ì ì¢í clamp ë¡ì§ì ê³µì í©ëë¤.
- out-of-bounds box clipping, COCO ì¶ë ¥, ë¹ ì´ë¯¸ì§ ë©íë°ì´í°, locale-safe YOLO ììì  ì¶ë ¥ì JUnit íì¤í¸ë¡ ê²ì¦í©ëë¤.
- 5Ã5 í½ìë³´ë¤ ìì bounding boxë ìëì¼ë¡ ì ì¸ë©ëë¤.
