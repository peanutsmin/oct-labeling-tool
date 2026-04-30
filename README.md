# OCT Labeling Tool

**English** | [Deutsch](#deutsch) | [한국어](#한국어)

---

## English

### Background

During a medical imaging lecture on early lung cancer detection, I learned that peripheral lung lesions found via low-dose CT screening are extremely difficult to biopsy using conventional bronchoscopy.

Optical biopsy (OCT-based) is emerging as a radiation-free future solution. While general tools like CVAT and Label Studio exist, no lightweight desktop tool specialized for lung OCT lesion annotation was available.

I built this as an experimental MVP — with plans to add YOLO/COCO export, mask annotation, DICOM support, and project reload.

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

### Tech Stack
- Java 21
- JavaFX 21

## How to Run

**Requirements**
- Java 21
- JavaFX 21 SDK ([Download](https://gluonhq.com/products/javafx/))

### Compile
```bash
javac --module-path /path/to/javafx/lib --add-modules javafx.controls OctViewer.java
```

### Run
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls OctViewer
```

### macOS example
```bash
javac --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls OctViewer.java
java --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls OctViewer
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
---

## Deutsch

### Hintergrund

Während einer Vorlesung über Früherkennung von Lungenkrebs erfuhr ich, dass periphere Lungenläsionen, die im Low-dose-CT-Screening entdeckt werden, mit konventioneller Bronchoskopie extrem schwer zu biopsieren sind.

Optische Biopsie (OCT-basiert) gilt als strahlungsfreie Zukunftslösung. Obwohl allgemeine Tools wie CVAT und Label Studio existieren, fehlte ein leichtgewichtiges Desktop-Tool speziell für Lungen-OCT-Läsionsannotation.

Dieses Tool wurde als experimentelle MVP-Implementierung entwickelt — mit geplanten Erweiterungen: YOLO/COCO-Export, Mask-Annotation, DICOM-Unterstützung und Projekt-Reload.

### Funktionen
- Mehrere Bilder laden (jpg, jpeg, png, bmp)
- Bounding Boxes per Maus-Drag zeichnen
- Regionen beschriften: Normal / Verdächtig / Bestätigter Krebs
- Tastaturkürzel (←/→ navigieren, 1/2/3 Label wählen)
- Mehrsprachige Oberfläche: Koreanisch / Englisch / Deutsch
- JSON-Export mit normierten Koordinaten

### Technologie
- Java 21
- JavaFX 21

---

## 한국어

### 배경
### 배경

폐암 조기 진단 강의에서 저선량 CT 스크리닝으로 발견된 말초 폐 병변은 기존 기관지내시경으로 생검하기 극히 어렵다는 것을 배웠습니다.

광학 생검(OCT 기반)은 방사선 부담이 없는 미래 진단 기술로 주목받고 있습니다. CVAT, Label Studio 같은 범용 라벨링 툴은 존재하지만, 폐 OCT 병변 라벨링에 특화된 가벼운 데스크톱 툴을 실험적으로 구현했습니다.

AI 학습용 데이터셋 구축의 첫 단계로 이 툴을 직접 제작했습니다. 현재는 MVP 단계이며, 향후 YOLO/COCO export, mask annotation, DICOM support, project reload 기능을 추가할 예정입니다.

### 기능
- 여러 장 이미지 선택 (jpg, jpeg, png, bmp)
- 마우스 드래그로 박스 그리기
- 정상 / 의심 / 확실히 암 라벨 선택
- 단축키 (←/→ 이미지 이동, 1/2/3 라벨 선택)
- 한국어 / English / Deutsch UI 전환
- 정규화된 좌표로 JSON 저장
- summary.json에 통계 저장

### 기술 스택
- Java 21
- JavaFX 21
