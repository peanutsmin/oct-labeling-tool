package com.peanutsmin.octlabeling;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MainApp extends Application {

    private final I18n i18n = new I18n();
    private final AnnotationStore store = new AnnotationStore();
    private final ImageCanvas canvas = new ImageCanvas();
    private List<File> imageFiles = new ArrayList<>();
    private int currentIndex = 0;
    private Stage mainStage;
    private BorderPane root;
    private ScrollPane canvasScrollPane;
    private DialogService dialogs;
    private CanvasInteractionController canvasController;
    private ToolbarBuilder toolbarBuilder;
    private double lastPanX;
    private double lastPanY;

    public void start(Stage stage) {
        this.mainStage = stage;
        this.dialogs = new DialogService(stage, i18n);
        root = new BorderPane();
        canvasScrollPane = new ScrollPane(canvas);
        canvasScrollPane.setPannable(true);
        canvasScrollPane.setFitToWidth(false);
        canvasScrollPane.setFitToHeight(false);
        root.setCenter(canvasScrollPane);
        AppStyle.applyRoot(root);
        AppStyle.applyCanvas(canvas);
        buildUI();

        canvasController = new CanvasInteractionController(
                canvas,
                store,
                this::currentImageFile,
                () -> LabelClass.fromDisplay(toolbarBuilder.labelBox.getValue()),
                this::selectedAnnotationMode,
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
            } else if (e.getCode() == KeyCode.DIGIT1) toolbarBuilder.labelBox.getSelectionModel().select(0);
            else if (e.getCode() == KeyCode.DIGIT2) toolbarBuilder.labelBox.getSelectionModel().select(1);
            else if (e.getCode() == KeyCode.DIGIT3) toolbarBuilder.labelBox.getSelectionModel().select(2);
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
        toolbarBuilder = new ToolbarBuilder(i18n, mainStage, canvas, new ToolbarBuilder.Callbacks() {
            @Override public void onImagesLoaded(List<File> files) {
                imageFiles = new ArrayList<>(files);
                currentIndex = 0;
                store.clear();
                loadImage(0);
            }
            @Override public void onPrev() {
                if (currentIndex > 0) { store.saveCurrent(); currentIndex--; loadImage(currentIndex); }
            }
            @Override public void onNext() {
                if (currentIndex < imageFiles.size() - 1) { store.saveCurrent(); currentIndex++; loadImage(currentIndex); }
            }
            @Override public void onReviewedChanged(boolean reviewed) {
                File f = currentImageFile();
                if (f != null) { store.setReviewed(f, reviewed); updateStats(); }
            }
            @Override public void onApplyLabel() {
                LabelClass label = LabelClass.fromDisplay(toolbarBuilder.labelBox.getValue());
                if (!canvasController.applySelectedLabel(label)) {
                    toolbarBuilder.progressLabel.setText(i18n.t("라벨을 선택하세요", "Select a box first", "Wählen Sie zuerst eine Box"));
                } else { updateStats(); }
            }
            @Override public void onZoomIn()  { changeZoom( ImageCanvas.ZOOM_STEP); }
            @Override public void onZoomOut() { changeZoom(-ImageCanvas.ZOOM_STEP); }
            @Override public void onZoomReset() { canvas.setZoom(1.0); updateZoomLabel(); }
            @Override public void onBrightnessChanged(double v) { canvas.setBrightness(v); }
            @Override public void onContrastChanged(double v)   { canvas.setContrast(v); }
            @Override public void onResetAdjustments() { resetImageAdjustments(); }
            @Override public void onLanguageChanged(String lang) {
                i18n.setLanguage(lang); store.saveCurrent();
                mainStage.close();
                try { new MainApp().start(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
            }
            @Override public void onSaveProject() {
                File dir = chooseDirectory(i18n.t("프로젝트 저장 폴더 선택", "Choose project folder", "Projektordner wählen"));
                if (dir != null) { store.saveCurrent(); ProjectService.saveProject(store, imageFiles, dir, dialogs, i18n); }
            }
            @Override public void onLoadProject() { loadProjectImages(); }
            @Override public void onExportJson() {
                runExportWithValidation(
                    i18n.t("JSON 내보내기", "JSON Export", "JSON Export"),
                    i18n.t("JSON 내보낼 폴더 선택", "Choose JSON export folder", "JSON-Exportordner wählen"),
                    dir -> ExportService.exportLabels(store, dir));
            }
            @Override public void onExportYolo() {
                runExportWithValidation(
                    i18n.t("YOLO 내보내기", "YOLO Export", "YOLO Export"),
                    i18n.t("YOLO 내보낼 폴더 선택", "Choose YOLO export folder", "YOLO-Exportordner wählen"),
                    dir -> ExportService.exportYolo(store, dir));
            }
            @Override public void onExportCoco() {
                runExportWithValidation(
                    i18n.t("COCO 내보내기", "COCO Export", "COCO Export"),
                    i18n.t("COCO 내보낼 폴더 선택", "Choose COCO export folder", "COCO-Exportordner wählen"),
                    dir -> ExportService.exportCoco(store, dir));
            }
            @Override public void onValidate() {
                store.saveCurrent();
                DatasetValidationReport report = DatasetValidationReport.generate(store);
                dialogs.showValidationReport(report);
            }
        });
        root.setTop(toolbarBuilder.build());
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
        toolbarBuilder.fileLabel.setText(i18n.t("ì´ë¯¸ì§ ìì", "No image", "Kein Bild"));
        toolbarBuilder.progressLabel.setText(i18n.t(
                "íë¡ì í¸ë¥¼ ì´ìì§ë§ ì´ë¯¸ì§ íì¼ì ì°¾ì ì ììµëë¤.",
                "Project opened, but referenced image files were not found.",
                "Projekt geÃ¶ffnet, aber referenzierte Bilddateien wurden nicht gefunden."
        ));
    }

    private File currentImageFile() {
        if (imageFiles.isEmpty() || currentIndex < 0 || currentIndex >= imageFiles.size()) {
            return null;
        }
        return imageFiles.get(currentIndex);
    }

    private AnnotationMode selectedAnnotationMode() {
        return toolbarBuilder.maskModeBtn != null && toolbarBuilder.maskModeBtn.isSelected() ? AnnotationMode.MASK : AnnotationMode.BOX;
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
        int maskCount = store.getCurrentMasks().size();
        if (!imageFiles.isEmpty()) {
            toolbarBuilder.progressLabel.setText((currentIndex + 1) + " / " + imageFiles.size() +
                    "   " + i18n.t("ì ì: ", "Normal: ", "Normal: ") + n +
                    "  " + i18n.t("ìì¬: ", "Suspicious: ", "VerdÃ¤chtig: ") + s +
                    "  " + i18n.t("íì¤í ì: ", "Cancer: ", "Krebs: ") + c +
                    "  " + i18n.t("ë§ì¤í¬: ", "Masks: ", "Masken: ") + maskCount);
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

        if (toolbarBuilder.overviewLabel != null) {
            toolbarBuilder.overviewLabel.setText(i18n.t("ì ì²´: ", "Overall: ", "Gesamt: ") +
                    store.getAll().size() + i18n.t(" ì´ë¯¸ì§", " images", " Bilder") +
                    " / " + store.reviewedCount() + i18n.t(" ê²ì", " reviewed", " geprÃ¼ft") +
                    " / " + store.totalMaskCount() + i18n.t(" ë§ì¤í¬", " masks", " Masken") +
                    "   " + i18n.t("ì ì: ", "Normal: ", "Normal: ") + totalNormal +
                    "  " + i18n.t("ìì¬: ", "Suspicious: ", "VerdÃ¤chtig: ") + totalSuspicious +
                    "  " + i18n.t("ì: ", "Cancer: ", "Krebs: ") + totalCancer);
        }
        for (MaskAnnotation mask : store.getCurrentMasks()) {
            Polygon polygon = AnnotationGeometry.polygonFromMask(mask, canvas);
            Text text = new Text(mask.label.display(i18n));
            text.setFill(ImageCanvas.getLabelColor(mask.label));
            canvas.addMask(polygon, text);
            canvas.updateMaskTextPosition(canvas.getMasks().size() - 1);
        }
    }

    private boolean isCurrentImageReviewed() {
        File file = currentImageFile();
        return file != null && store.isReviewed(file.getAbsolutePath());
    }

    private void updateReviewedControl() {
        if (toolbarBuilder.reviewedCheckBox != null) {
            toolbarBuilder.reviewedCheckBox.setSelected(isCurrentImageReviewed());
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
        if (toolbarBuilder.zoomResetBtn != null) {
            toolbarBuilder.zoomResetBtn.setText(zoomLabel());
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
        if (toolbarBuilder.brightnessSlider != null) {
            toolbarBuilder.brightnessSlider.setValue(canvas.getBrightness());
        }
        if (toolbarBuilder.contrastSlider != null) {
            toolbarBuilder.contrastSlider.setValue(canvas.getContrast());
        }
    }

    private void loadImage(int index) {
        canvas.clearBoxes();
        if (canvasController != null) {
            canvasController.resetSelection();
        }
        File file = imageFiles.get(index);
        Image image = loadDisplayImage(file);
        store.loadFor(file.getAbsolutePath(), (int) image.getWidth(), (int) image.getHeight());
        canvas.setImage(image);
        toolbarBuilder.fileLabel.setText(file.getName());
        updateReviewedControl();
        renderAnnotations();
        updateStats();
    }

    private Image loadDisplayImage(File file) {
        try {
            if (DicomImageLoader.isDicom(file)) {
                return DicomImageLoader.load(file);
            }
            return new Image(file.toURI().toString());
        } catch (Exception e) {
            toolbarBuilder.progressLabel.setText(i18n.t(
                    "ì´ë¯¸ì§ë¥¼ ì´ ì ììµëë¤: ",
                    "Could not open image: ",
                    "Bild konnte nicht geÃ¶ffnet werden: "
            ) + e.getMessage());
            return new Image(file.toURI().toString());
        }
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
