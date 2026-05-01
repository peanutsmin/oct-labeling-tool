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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {

    private I18n i18n = new I18n();
    private AnnotationStore store = new AnnotationStore();
    private ImageCanvas canvas = new ImageCanvas();
    private List<File> imageFiles = new ArrayList<>();
    private int currentIndex = 0;
    private Label fileLabel;
    private Label progressLabel;
    private ComboBox<String> labelBox;
    private Stage mainStage;
    private BorderPane root;
    private DialogService dialogs;
    private CanvasInteractionController canvasController;

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;
        this.dialogs = new DialogService(stage, i18n);
        root = new BorderPane();
        root.setCenter(canvas);
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

        Button saveBtn = new Button(i18n.t("JSON 내보내기", "JSON Export", "JSON Export"));
        saveBtn.setOnAction(e -> {
            File dir = chooseDirectory(i18n.t("JSON 내보내기 폴더 선택", "Choose JSON export folder", "JSON-Exportordner wählen"));
            if (dir != null) {
                dialogs.showExportResult(
                        i18n.t("JSON 내보내기", "JSON Export", "JSON Export"),
                        ExportService.exportLabels(store, dir)
                );
            }
        });
        Button yoloBtn = new Button(i18n.t("YOLO 내보내기", "YOLO Export", "YOLO Export"));
        yoloBtn.setOnAction(e -> {
            File dir = chooseDirectory(i18n.t("YOLO 내보내기 폴더 선택", "Choose YOLO export folder", "YOLO-Exportordner wählen"));
            if (dir != null) {
                dialogs.showExportResult(
                        i18n.t("YOLO 내보내기", "YOLO Export", "YOLO Export"),
                        ExportService.exportYolo(store, dir)
                );
            }
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
        AppStyle.applyStatusLabel(progressLabel);

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
        for (Button button : List.of(prevBtn, nextBtn, saveBtn, yoloBtn, saveProjectBtn, loadProjectBtn, btnKo, btnEn, btnDe)) {
            AppStyle.applyButton(button);
        }
        AppStyle.applyComboBox(labelBox);

        HBox langBar = new HBox(6, btnKo, btnEn, btnDe);
        AppStyle.applyToolbar(langBar);
        HBox toolbar1 = new HBox(8, openBtn, prevBtn, fileLabel, nextBtn,
                new Label(i18n.t("라벨:", "Label:", "Label:")), labelBox);
        AppStyle.applyToolbar(toolbar1);

        HBox toolbar2 = new HBox(8, saveBtn, yoloBtn, saveProjectBtn, loadProjectBtn);
        AppStyle.applyToolbar(toolbar2);
        HBox statusBar = new HBox(20, progressLabel);
        AppStyle.applyStatusBar(statusBar);
        VBox top = new VBox(langBar, toolbar1, toolbar2, statusBar);
        root.setTop(top);
        if (mainStage != null)
            mainStage.setTitle(i18n.t("OCT 라벨링 툴", "OCT Labeling Tool", "OCT Beschriftungswerkzeug"));
    }

    private File chooseDirectory(String title) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(title);
        return dc.showDialog(mainStage);
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
    }

    private void loadImage(int index) {
        canvas.clearBoxes();
        if (canvasController != null) {
            canvasController.resetSelection();
        }
        File file = imageFiles.get(index);
        store.loadFor(file.getAbsolutePath());
        canvas.getImageView().setImage(new Image(file.toURI().toString()));
        fileLabel.setText(file.getName());
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
        updateStats();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
