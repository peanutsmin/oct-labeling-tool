# OCT Labeling Tool

**English** | [Deutsch](#deutsch) | [한국어](#한국어)

---

## English

### Background

During a medical imaging lecture on early lung cancer detection, I learned that peripheral lung lesions found via low-dose CT screening are extremely difficult to biopsy using conventional bronchoscopy.

Optical biopsy (OCT-based) is emerging as a radiation-free future solution. While general tools like CVAT and Label Studio exist, no lightweight desktop tool specialized for lung OCT lesion annotation was available.

I built this as an experimental MVP — The tool currently supports JSON export, YOLO-format export, and project save/load. Future plans include COCO export, mask annotation, and DICOM support.

### What I Built
A lightweight desktop tool for annotating lung OCT images, designed to generate labeled datasets for future AI training.

![Screenshot](screenshot.png)

### Features
- Load multiple images (jpg, jpeg, png, bmp)
- Draw bounding boxes with mouse drag
- Label regions: Normal / Suspicious / Confirmed Cancer
- Keyboard shortcuts (←/→ to navigate, 1/2/3 to select label)
- Labels persist when switching between images
- Right-click (two-finger tap) to delete a box
- Multilingual UI: Korean / English / German
- Export to JSON with normalized coordinates
- Summary statistics saved to summary.json
- Export to YOLO format for object detection training
- Project save/load (project.json) for continuous annotation workflow
- Coordinate clamping for safer dataset export

### Tech Stack
- Java 21
- JavaFX 21

## How to Run

**Requirements**
- Java 21
- JavaFX 21 SDK ([Download](https://gluonhq.com/products/javafx/))

### Compile
```bash
javac --module-path /path/to/javafx/lib --add-modules javafx.controls src/*.java
```

### Run
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls -cp src MainApp
```

### macOS example
```bash
javac --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls src/*.java
java --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls -cp src MainApp
```

### Export format example
```json
{
  "file": "image.jpeg",
  "label": "Suspicious",
  "x": 0.2425,
  "y": 0.1897,
  "x_pixel": 72,
  "y_pixel": 31,
  "image_width": 300,
  "image_height": 168
}
```
### Run with Maven
```bash
mvn javafx:run
```
## Workflow

1. Load OCT images
2. Draw bounding boxes with mouse drag
3. Assign labels: Normal / Suspicious / Confirmed Cancer
4. Save project as `project.json` for future sessions
5. Reopen project to continue or correct annotations
6. Export as JSON or YOLO format for AI training

## Limitations

- This tool is an experimental MVP and is not intended for clinical diagnosis.
- It currently supports bounding-box annotation only.
- DICOM support and mask-based annotation are planned for future versions.
- Coordinate accuracy depends on display scaling and may vary across screen resolutions.

---

## Deutsch

### Hintergrund

Während einer Vorlesung über Früherkennung von Lungenkrebs erfuhr ich, dass periphere Lungenläsionen, die im Low-dose-CT-Screening entdeckt werden, mit konventioneller Bronchoskopie extrem schwer zu biopsieren sind.

Optische Biopsie (OCT-basiert) gilt als strahlungsfreie Zukunftslösung. Obwohl allgemeine Tools wie CVAT und Label Studio existieren, fehlte ein leichtgewichtiges Desktop-Tool speziell für Lungen-OCT-Läsionsannotation.

Dieses Tool wurde als experimentelle MVP-Implementierung entwickelt — Das Tool unterstützt aktuell JSON-Export, YOLO-Format-Export und Projekt-Speichern/Laden. Geplante Erweiterungen: COCO-Export, Mask-Annotation und DICOM-Unterstützung.

### Funktionen
- Mehrere Bilder laden (jpg, jpeg, png, bmp)
- Bounding Boxes per Maus-Drag zeichnen
- Regionen beschriften: Normal / Verdächtig / Bestätigter Krebs
- Tastaturkürzel (←/→ navigieren, 1/2/3 Label wählen)
- Mehrsprachige Oberfläche: Koreanisch / Englisch / Deutsch
- JSON-Export mit normierten Koordinaten
- YOLO-Format-Export für Object-Detection-Training
- Projekt speichern/laden (project.json) für kontinuierliche Annotation
- Koordinaten-Clamping für sicheren Datensatz-Export

### Technologie
- Java 21
- JavaFX 21

## Workflow

1. OCT-Bilder laden
2. Bounding Boxes per Maus-Drag zeichnen
3. Label zuweisen: Normal / Verdächtig / Bestätigter Krebs
4. Projekt als `project.json` speichern
5. Projekt erneut öffnen und Annotationen korrigieren
6. Als JSON oder YOLO-Format exportieren

## Einschränkungen

- Dieses Tool ist ein experimentelles MVP und nicht für klinische Diagnosen geeignet.
- Aktuell wird nur Bounding-Box-Annotation unterstützt.
- DICOM-Unterstützung und maskenbasierte Annotation sind für zukünftige Versionen geplant.

---

## 한국어

### 배경

폐암 조기 진단 강의에서 저선량 CT 스크리닝으로 발견된 말초 폐 병변은 기존 기관지내시경으로 생검하기 매우 어렵다는 것을 배웠습니다.

광학 생검(OCT 기반)은 방사선 부담이 없는 미래 진단 기술로 주목받고 있습니다. CVAT, Label Studio 같은 범용 라벨링 툴은 존재하지만, 폐 OCT 병변 라벨링에 특화된 가벼운 데스크톱 툴을 실험적으로 구현했습니다.

AI 학습용 데이터셋 구축의 첫 단계로 이 툴을 직접 제작했습니다. 현재 JSON export, YOLO format export, project 저장/불러오기 기능을 지원합니다. 향후 COCO export, mask annotation, DICOM support 기능을 추가할 예정입니다.

### 기능
- 여러 장 이미지 선택 (jpg, jpeg, png, bmp)
- 마우스 드래그로 박스 그리기
- 정상 / 의심 / 확실히 암 라벨 선택
- 단축키 (←/→ 이미지 이동, 1/2/3 라벨 선택)
- 한국어 / English / Deutsch UI 전환
- 정규화된 좌표로 JSON 저장
- summary.json에 통계 저장
- YOLO format으로 export (AI 객체탐지 학습용)
- project.json 저장/불러오기 (이어서 작업 가능)
- 좌표 clamp로 안전한 데이터셋 export


### 기술 스택
- Java 21
- JavaFX 21

## 사용 흐름

1. OCT 이미지 불러오기
2. 마우스 드래그로 박스 그리기
3. 정상 / 의심 / 확실히 암 라벨 선택
4. `project.json`으로 프로젝트 저장
5. 나중에 프로젝트 열어서 수정
6. JSON 또는 YOLO 형식으로 export

## 한계 및 주의사항

- 이 툴은 실험적 MVP이며 임상 진단 목적으로 사용할 수 없습니다.
- 현재 bounding-box 방식의 annotation만 지원합니다.
- DICOM 지원 및 mask 기반 annotation은 향후 추가 예정입니다.


