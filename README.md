# OCT Labeling Tool

**English** | [Deutsch](#deutsch) | [한국어](#한국어)

---

## English

### Background
During a medical imaging lecture on early lung cancer detection, I learned that peripheral lung lesions found via low-dose CT screening are extremely difficult to biopsy using conventional bronchoscopy. Optical biopsy (OCT-based) is emerging as a radiation-free future solution — but **no dedicated labeling tool exists for lung OCT images**. I built this tool as a first step toward creating labeled datasets for AI model training.

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

**Compile**
javac --module-path /path/to/javafx/lib --add-modules javafx.controls OctViewer.java

**Run**
java --module-path /path/to/javafx/lib --add-modules javafx.controls OctViewer

**macOS example**
javac --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls OctViewer.java
java --module-path ~/javafx-sdk/javafx-sdk-21.0.2/lib --add-modules javafx.controls OctViewer

---

## Deutsch

### Hintergrund
Während einer Vorlesung über Früherkennung von Lungenkrebs erfuhr ich, dass periphere Lungenläsionen, die im Low-dose-CT-Screening entdeckt werden, mit konventioneller Bronchoskopie extrem schwer zu biopsieren sind. Optische Biopsie (OCT-basiert) gilt als strahlungsfreie Zukunftslösung — aber **es existierte kein dediziertes Annotierungswerkzeug für Lungen-OCT-Bilder**. Als ersten Schritt zur Erstellung annotierter Datensätze für das KI-Training habe ich dieses Tool selbst entwickelt.

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
폐암 조기 진단 강의에서 저선량 CT 스크리닝으로 발견된 말초 폐 병변은 기존 기관지내시경으로 생검하기 극히 어렵다는 것을 배웠습니다. 광학 생검(OCT 기반)이 방사선 없는 미래 솔루션으로 주목받고 있지만, **폐 OCT 영상 전용 라벨링 툴은 존재하지 않았습니다. AI 학습용 데이터셋 구축의 첫 단계로 이 툴을 직접 제작했습니다.**

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
