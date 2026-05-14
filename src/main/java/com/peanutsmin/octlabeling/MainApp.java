package com.peanutsmin.octlabeling;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Bounds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MainApp extends Application {

    private I18n i18n = new I18n();
    private AnnotationStore store = new AnnotationStore();
    private ImageCanvas canvas = new ImageCanvas();
    private List<File> imageFiles = new ArrayList<>();
    private int currentIndex = 0;
    private Label fileLabel;
    private Label progressLabel;
    private Label overviewLabel;
    private CheckBox reviewedCheckBox;
    private ComboBox<String> labelBox;
    private Button zoomResetBtn;
    private Slider brightnessSlider;
    private Slider contrastSlider;
    private Stage mainStage;
    private BorderPane root;
    private ScrollPane canvasScrollPane;
    private DialogService dialogs;
    private CanvasInteractionController canvasController;
    private double lastPanX;
    private double lastPanY;

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;
        this.dialogs = new DialogService(stage, i18n);
        root = new BorderPane();
        canvasScrollPane = new ScrollPane(canvas);
        canvasScrollPane.setPannable(true);
        canvasScrollPane.setFitToWidth(false);
        canvasScrollPane.setFitToHeight(false);
        root.setCenter(canvasScrollPane);
        labelBox = new ComboBox<>();
        AppStyle.applyRoot(root);
        AppStyle.applyCanvas(canvas);
        buildUI();

        canvasController = new CanvasInteractionController(
                canvas,
                store,
                this::currentImageFile,
                () -> LabelClass.fromDisplay(labelBox.getValue()),
                label -> label.display(i18n),
                this::updateStats
        );
        canvasController.install();

        Scene scene = new Scene(root, 800, 640);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) {
                if (currentIndex < imageFiles.size() - 1) {
                    store.saveCurrent();
                    currentIndex++;
                    loadImage(currentIndex);
                }
            } else if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) {
                if (currentIndex > 0) {
                    store.saveCurrent();
                    currentIndex--;
                    loadImage(currentIndex);
                }
            } else if (e.getCode() == KeyCode.DIGIT1) labelBox.getSelectionModel().select(0);
            else if (e.getCode() == KeyCode.DIGIT2) labelBox.getSelectionModel().select(1);
            else if (e.getCode() == KeyCode.DIGIT3) labelBox.getSelectionModel().select(2);
        });
        scene.setOnScroll(e -> {
            if (!e.isControlDown()) {
                return;
            }
            if (e.getDeltaY() > 0) {
                changeZoom(ImageCanvas.ZOOM_STEP);
            } else if (e.getDeltaY() < 0) {
                changeZoom(-ImageCanvas.ZOOM_STEP);
            }
            e.consume();
        });
        installPanHandlers();

        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(720);
        stage.show();
    }

    private void buildUI() {
        labelBox.getItems().clear();
        labelBox.getItems().addAll(
                i18n.t("정상", "Normal", "Normal"),
                i18n.t("의심", "Suspicious", "Verdächtig"),
                i18n.t("확실히 암", "Confirmed Cancer", "Bestätigter Krebs")
        );
        labelBox.getSelectionModel().select(1);

        Button openBtn = new Button(i18n.t("이미지 선택", "Open Images", "Bilder öffnen"));
        openBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    i18n.t("이미지", "Images", "Bilder"),
                    "*.png", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG", "*.bmp"
            ));
            List<File> files = fc.showOpenMultipleDialog(mainStage);
            if (files != null && !files.isEmpty()) {
                imageFiles = files;
                currentIndex = 0;
                store.clear();
                loadImage(currentIndex);
            }
        });

        Button prevBtn = new Button(i18n.t("< 이전", "< Prev", "< Zurück"));
        prevBtn.setOnAction(e -> {
            if (currentIndex > 0) {
                store.saveCurrent();
                currentIndex--;
                loadImage(currentIndex);
            }
        });

        Button nextBtn = new Button(i18n.t("다음 >", "Next >", "Weiter >"));
        nextBtn.setOnAction(e -> {
            if (currentIndex < imageFiles.size() - 1) {
                store.saveCurrent();
                currentIndex++;
                loadImage(currentIndex);
            }
        });

        Button applyLabelBtn = new Button(i18n.t("라벨 적용", "Apply Label", "Label anwenden"));
        applyLabelBtn.setOnAction(e -> {
            LabelClass label = LabelClass.fromDisplay(labelBox.getValue());
            if (!canvasController.applySelectedLabel(label)) {
                progressLabel.setText(i18n.t(
                        "라벨을 적용할 박스를 먼저 선택하세요.",
                        "Select a box before applying a label.",
                        "Wählen Sie zuerst eine Box aus."
                ));
            }
        });

        reviewedCheckBox = new CheckBox(i18n.t("검수 완료", "Reviewed", "Geprüft"));
        reviewedCheckBox.setSelected(isCurrentImageReviewed());
        reviewedCheckBox.setOnAction(e -> {
            File file = currentImageFile();
            if (file != null) {
                store.setReviewed(file.getAbsolutePath(), reviewedCheckBox.isSelected());
                updateStats();
            }
        });

        Button zoomOutBtn = new Button("-");
        zoomOutBtn.setOnAction(e -> changeZoom(-ImageCanvas.ZOOM_STEP));
        zoomResetBtn = new Button(zoomLabel());
        zoomResetBtn.setOnAction(e -> {
            canvas.resetZoom();
            renderAnnotations();
            updateZoomLabel();
        });
        Button zoomInBtn = new Button("+");
        zoomInBtn.setOnAction(e -> changeZoom(ImageCanvas.ZOOM_STEP));

        brightnessSlider = createAdjustmentSlider(canvas.getBrightness());
        brightnessSlider.valueProperty().addListener((obs, oldValue, newValue) ->
                canvas.setBrightness(newValue.doubleValue()));
        contrastSlider = createAdjustmentSlider(canvas.getContrast());
        contrastSlider.valueProperty().addListener((obs, oldValue, newValue) ->
                canvas.setContrast(newValue.doubleValue()));
        Button resetAdjustmentsBtn = new Button(i18n.t("보정 초기화", "Reset Adjustments", "Anpassungen zurücksetzen"));
        resetAdjustmentsBtn.setOnAction(e -> resetImageAdjustments());

        Button saveBtn = new Button(i18n.t("JSON 내보내기", "JSON Export", "JSON Export"));
        saveBtn.setOnAction(e -> {
            runExportWithValidation(
                    i18n.t("JSON 내보내기", "JSON Export", "JSON Export"),
                    i18n.t("JSON 내보내기 폴더 선택", "Choose JSON export folder", "JSON-Exportordner wählen"),
                    dir -> ExportService.exportLabels(store, dir)
            );
        });
        Button yoloBtn = new Button(i18n.t("YOLO 내보내기", "YOLO Export", "YOLO Export"));
        yoloBtn.setOnAction(e -> {
            runExportWithValidation(
                    i18n.t("YOLO 내보내기", "YOLO Export", "YOLO Export"),
                    i18n.t("YOLO 내보내기 폴더 선택", "Choose YOLO export folder", "YOLO-Exportordner wählen"),
                    dir -> ExportService.exportYolo(store, dir)
            );
        });

        Button cocoBtn = new Button(i18n.t("COCO 내보내기", "COCO Export", "COCO Export"));
        cocoBtn.setOnAction(e -> {
            runExportWithValidation(
                    i18n.t("COCO 내보내기", "COCO Export", "COCO Export"),
                    i18n.t("COCO 내보내기 폴더 선택", "Choose COCO export folder", "COCO-Exportordner wählen"),
                    dir -> ExportService.exportCoco(store, dir)
            );
        });

        Button saveProjectBtn = new Button(i18n.t("프로젝트 저장", "Save Project", "Projekt speichern"));
        saveProjectBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(i18n.t("프로젝트 저장", "Save Project", "Projekt speichern"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            fc.setInitialFileName("project.json");
            File file = fc.showSaveDialog(mainStage);
            if (file != null) {
                dialogs.showProjectResult(
                        i18n.t("프로젝트 저장", "Save Project", "Projekt speichern"),
                        ProjectService.saveProject(store, file.getAbsolutePath(), "oct_project")
                );
            }
        });

        Button loadProjectBtn = new Button(i18n.t("프로젝트 열기", "Open Project", "Projekt öffnen"));
        loadProjectBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(i18n.t("project.json 선택", "Choose project.json", "project.json auswählen"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File file = fc.showOpenDialog(mainStage);
            if (file != null) {
                ProjectService.ProjectResult result = ProjectService.loadProject(store, file.getAbsolutePath());
                dialogs.showProjectResult(
                        i18n.t("프로젝트 열기", "Open Project", "Projekt öffnen"),
                        result
                );
                if (result.isSuccess()) {
                    loadProjectImages();
                }
            }
        });
        fileLabel = new Label(i18n.t("이미지 없음", "No image", "Kein Bild"));
        progressLabel = new Label("");
        overviewLabel = new Label("");
        AppStyle.applyStatusLabel(progressLabel);
        AppStyle.applyStatusLabel(overviewLabel);

        Button btnKo = new Button("한국어");
        Button btnEn = new Button("English");
        Button btnDe = new Button("Deutsch");
        btnKo.setOnAction(e -> {
            i18n.setLang("ko");
            buildUI();
        });
        btnEn.setOnAction(e -> {
            i18n.setLang("en");
            buildUI();
        });
        btnDe.setOnAction(e -> {
            i18n.setLang("de");
            buildUI();
        });

        AppStyle.applyPrimaryButton(openBtn);
        for (Button button : List.of(prevBtn, nextBtn, applyLabelBtn, zoomOutBtn, zoomResetBtn, zoomInBtn,
                resetAdjustmentsBtn, saveBtn, yoloBtn, cocoBtn, saveProjectBtn, loadProjectBtn, btnKo, btnEn, btnDe)) {
            AppStyle.applyButton(button);
        }
        AppStyle.applyComboBox(labelBox);
        AppStyle.applySlider(brightnessSlider);
        AppStyle.applySlider(contrastSlider);

        HBox langBar = new HBox(6, btnKo, btnEn, btnDe);
        AppStyle.applyToolbar(langBar);
        HBox toolbar1 = new HBox(8, openBtn, prevBtn, fileLabel, nextBtn, reviewedCheckBox,
                new Label(i18n.t("라벨:", "Label:", "Label:")), labelBox, applyLabelBtn);
        AppStyle.applyToolbar(toolbar1);

        HBox toolbar2 = new HBox(8,
                new Label(i18n.t("확대:", "Zoom:", "Zoom:")), zoomOutBtn, zoomResetBtn, zoomInBtn,
                saveBtn, yoloBtn, cocoBtn, saveProjectBtn, loadProjectBtn);
        AppStyle.applyToolbar(toolbar2);
        HBox toolbar3 = new HBox(8,
                new Label(i18n.t("밝기:", "Brightness:", "Helligkeit:")), brightnessSlider,
                new Label(i18n.t("대비:", "Contrast:", "Kontrast:")), contrastSlider,
                resetAdjustmentsBtn);
        AppStyle.applyToolbar(toolbar3);
        HBox statusBar = new HBox(20, progressLabel, overviewLabel);
        AppStyle.applyStatusBar(statusBar);
        VBox top = new VBox(langBar, toolbar1, toolbar2, toolbar3, statusBar);
        root.setTop(top);
        if (mainStage != null)
            mainStage.setTitle(i18n.t("OCT 라벨링 툴", "OCT Labeling Tool", "OCT Beschriftungswerkzeug"));
    }

    private File chooseDirectory(String title) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(title);
        return dc.showDialog(mainStage);
    }

    private void runExportWithValidation(
            String exportName,
            String directoryTitle,
            Function<File, ExportService.ExportResult> exportAction
    ) {
        DatasetValidationReport report = DatasetValidationReport.fromStore(store);
        if (!dialogs.confirmValidationReport(exportName, report)) {
            return;
        }

        File dir = chooseDirectory(directoryTitle);
        if (dir != null) {
            dialogs.showExportResult(exportName, exportAction.apply(dir));
        }
    }

    private void loadProjectImages() {
        currentIndex = 0;
        imageFiles = new ArrayList<>(store.getAll().keySet().stream()
                .map(File::new)
                .filter(File::exists)
                .toList());
        if (!imageFiles.isEmpty()) {
            currentIndex = 0;
            loadImage(currentIndex);
            return;
        }

        canvas.clearBoxes();
        canvasController.resetSelection();
        fileLabel.setText(i18n.t("이미지 없음", "No image", "Kein Bild"));
        progressLabel.setText(i18n.t(
                "프로젝트를 열었지만 이미지 파일을 찾을 수 없습니다.",
                "Project opened, but referenced image files were not found.",
                "Projekt geöffnet, aber referenzierte Bilddateien wurden nicht gefunden."
        ));
    }

    private File currentImageFile() {
        if (imageFiles.isEmpty() || currentIndex < 0 || currentIndex >= imageFiles.size()) {
            return null;
        }
        return imageFiles.get(currentIndex);
    }

    private void updateStats() {
        store.saveCurrent();
        int n = 0, s = 0, c = 0;
        for (Annotation ann : store.getCurrent()) {
            switch (ann.label) {
                case NORMAL -> n++;
                case SUSPICIOUS -> s++;
                case CONFIRMED_CANCER -> c++;
            }
        }
        if (!imageFiles.isEmpty()) {
            progressLabel.setText((currentIndex + 1) + " / " + imageFiles.size() +
                    "   " + i18n.t("정상: ", "Normal: ", "Normal: ") + n +
                    "  " + i18n.t("의심: ", "Suspicious: ", "Verdächtig: ") + s +
                    "  " + i18n.t("확실히 암: ", "Cancer: ", "Krebs: ") + c);
        }
        updateOverviewStats();
    }

    private void updateOverviewStats() {
        int totalNormal = 0;
        int totalSuspicious = 0;
        int totalCancer = 0;
        for (ArrayList<Annotation> annotations : store.getAll().values()) {
            for (Annotation ann : annotations) {
                switch (ann.label) {
                    case NORMAL -> totalNormal++;
                    case SUSPICIOUS -> totalSuspicious++;
                    case CONFIRMED_CANCER -> totalCancer++;
                }
            }
        }

        if (overviewLabel != null) {
            overviewLabel.setText(i18n.t("전체: ", "Overall: ", "Gesamt: ") +
                    store.getAll().size() + i18n.t(" 이미지", " images", " Bilder") +
                    " / " + store.reviewedCount() + i18n.t(" 검수", " reviewed", " geprüft") +
                    "   " + i18n.t("정상: ", "Normal: ", "Normal: ") + totalNormal +
                    "  " + i18n.t("의심: ", "Suspicious: ", "Verdächtig: ") + totalSuspicious +
                    "  " + i18n.t("암: ", "Cancer: ", "Krebs: ") + totalCancer);
        }
    }

    private boolean isCurrentImageReviewed() {
        File file = currentImageFile();
        return file != null && store.isReviewed(file.getAbsolutePath());
    }

    private void updateReviewedControl() {
        if (reviewedCheckBox != null) {
            reviewedCheckBox.setSelected(isCurrentImageReviewed());
        }
    }

    private void changeZoom(double delta) {
        canvas.setZoomFactor(canvas.getZoomFactor() + delta);
        renderAnnotations();
        updateZoomLabel();
    }

    private void installPanHandlers() {
        canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!isPanGesture(e)) {
                return;
            }
            lastPanX = e.getSceneX();
            lastPanY = e.getSceneY();
            e.consume();
        });

        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!isPanGesture(e)) {
                return;
            }
            double dx = e.getSceneX() - lastPanX;
            double dy = e.getSceneY() - lastPanY;
            panCanvas(dx, dy);
            lastPanX = e.getSceneX();
            lastPanY = e.getSceneY();
            e.consume();
        });
    }

    private boolean isPanGesture(MouseEvent e) {
        return e.isMiddleButtonDown() || e.isAltDown();
    }

    private void panCanvas(double dx, double dy) {
        Bounds viewport = canvasScrollPane.getViewportBounds();
        Bounds content = canvas.getLayoutBounds();
        double extraWidth = content.getWidth() - viewport.getWidth();
        double extraHeight = content.getHeight() - viewport.getHeight();

        if (extraWidth > 0) {
            canvasScrollPane.setHvalue(clamp(canvasScrollPane.getHvalue() - dx / extraWidth, 0, 1));
        }
        if (extraHeight > 0) {
            canvasScrollPane.setVvalue(clamp(canvasScrollPane.getVvalue() - dy / extraHeight, 0, 1));
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String zoomLabel() {
        return Math.round(canvas.getZoomFactor() * 100) + "%";
    }

    private void updateZoomLabel() {
        if (zoomResetBtn != null) {
            zoomResetBtn.setText(zoomLabel());
        }
    }

    private Slider createAdjustmentSlider(double initialValue) {
        Slider slider = new Slider(ImageCanvas.MIN_ADJUSTMENT, ImageCanvas.MAX_ADJUSTMENT, initialValue);
        slider.setBlockIncrement(0.05);
        slider.setMajorTickUnit(0.5);
        slider.setMinorTickCount(4);
        return slider;
    }

    private void resetImageAdjustments() {
        canvas.resetImageAdjustments();
        if (brightnessSlider != null) {
            brightnessSlider.setValue(canvas.getBrightness());
        }
        if (contrastSlider != null) {
            contrastSlider.setValue(canvas.getContrast());
        }
    }

    private void loadImage(int index) {
        canvas.clearBoxes();
        if (canvasController != null) {
            canvasController.resetSelection();
        }
        File file = imageFiles.get(index);
        Image image = new Image(file.toURI().toString());
        store.loadFor(file.getAbsolutePath(), (int) image.getWidth(), (int) image.getHeight());
        canvas.setImage(image);
        fileLabel.setText(file.getName());
        updateReviewedControl();
        renderAnnotations();
        updateStats();
    }

    private void renderAnnotations() {
        canvas.clearBoxes();
        if (canvasController != null) {
            canvasController.resetSelection();
        }
        for (Annotation ann : store.getCurrent()) {
            Rectangle r = AnnotationGeometry.rectangleFromAnnotation(ann, canvas);
            Color color = ImageCanvas.getLabelColor(ann.label);
            r.setStroke(color);
            r.setFill(Color.TRANSPARENT);
            r.setStrokeWidth(2);
            Text t = new Text(ann.label.display(i18n));
            t.setFill(color);
            canvas.addBox(r, t);
            canvas.updateTextPosition(canvas.getRects().size() - 1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
