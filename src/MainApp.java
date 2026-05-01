import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
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
    private Rectangle currentRect;
    private double startX, startY;
    private Label fileLabel;
    private Label progressLabel;
    private ComboBox<String> labelBox;
    private Stage mainStage;
    private BorderPane root;

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;
        root = new BorderPane();
        root.setCenter(canvas);
        labelBox = new ComboBox<>();
        buildUI();

        canvas.setOnMousePressed(e -> {
            canvas.requestFocus();
            if (e.isSecondaryButtonDown()) {
                for (int idx = canvas.getRects().size() - 1; idx >= 0; idx--) {
                    Rectangle target = canvas.getRects().get(idx);
                    if (!target.contains(e.getX(), e.getY())) continue;
                    canvas.removeBox(idx);
                    store.removeAnnotation(idx);
                    updateStats();
                    break;
                }
                return;
            }
            startX = e.getX();
            startY = e.getY();
            currentRect = new Rectangle();
            currentRect.setStroke(Color.RED);
            currentRect.setFill(Color.TRANSPARENT);
            currentRect.setStrokeWidth(2);
            canvas.getChildren().add(currentRect);
        });

        canvas.setOnMouseDragged(e -> {
            if (currentRect == null) return;
            double w = e.getX() - startX;
            double h = e.getY() - startY;
            currentRect.setX(w < 0 ? e.getX() : startX);
            currentRect.setY(h < 0 ? e.getY() : startY);
            currentRect.setWidth(Math.abs(w));
            currentRect.setHeight(Math.abs(h));
        });

        canvas.setOnMouseReleased(e -> {
            if (e.isSecondaryButtonDown() || currentRect == null) return;
            if (currentRect.getWidth() < 5 || currentRect.getHeight() < 5) {
                canvas.getChildren().remove(currentRect);
                currentRect = null;
                return;
            }
            LabelClass label = LabelClass.fromDisplay(labelBox.getValue());
            double dispW = canvas.getImageView().getBoundsInLocal().getWidth();
            double dispH = canvas.getImageView().getBoundsInLocal().getHeight();
            double offsetX = canvas.getImageView().getBoundsInParent().getMinX();
            double offsetY = canvas.getImageView().getBoundsInParent().getMinY();
            double adjX = currentRect.getX() - offsetX;
            double adjY = currentRect.getY() - offsetY;
            double nx = adjX / dispW;
            double ny = adjY / dispH;
            double nw = currentRect.getWidth() / dispW;
            double nh = currentRect.getHeight() / dispH;
            int imgW = (int) canvas.getImageView().getImage().getWidth();
            int imgH = (int) canvas.getImageView().getImage().getHeight();
            int px = (int) (nx * imgW);
            int py = (int) (ny * imgH);
            int pw = (int) (nw * imgW);
            int ph = (int) (nh * imgH);

            Annotation ann = new Annotation(
                    imageFiles.get(currentIndex).getName(), label,
                    nx, ny, nw, nh, px, py, pw, ph, imgW, imgH
            );
            store.addAnnotation(ann);
            Color color = ImageCanvas.getLabelColor(label);
            currentRect.setStroke(color);
            Text txt = new Text(currentRect.getX() + 2, currentRect.getY() - 4, label.display(i18n));
            txt.setFill(color);
            canvas.addBox(currentRect, txt);
            currentRect = null;
            updateStats();
        });

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
                showExportResult(
                        i18n.t("JSON 내보내기", "JSON Export", "JSON Export"),
                        ExportService.exportLabels(store, dir)
                );
            }
        });
        Button yoloBtn = new Button(i18n.t("YOLO 내보내기", "YOLO Export", "YOLO Export"));
        yoloBtn.setOnAction(e -> {
            File dir = chooseDirectory(i18n.t("YOLO 내보내기 폴더 선택", "Choose YOLO export folder", "YOLO-Exportordner wählen"));
            if (dir != null) {
                showExportResult(
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
                ProjectService.saveProject(store, file.getAbsolutePath(), "oct_project");
            }
        });

        Button loadProjectBtn = new Button(i18n.t("프로젝트 열기", "Open Project", "Projekt öffnen"));
        loadProjectBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("project.json 선택");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File file = fc.showOpenDialog(mainStage);
            if (file != null) {
                ProjectService.loadProject(store, file.getAbsolutePath());
                imageFiles = new ArrayList<>(store.getAll().keySet().stream()
                        .map(File::new)
                        .filter(File::exists)
                        .toList());
                if (!imageFiles.isEmpty()) {
                    currentIndex = 0;
                    loadImage(currentIndex);
                }
            }
        });
        fileLabel = new Label(i18n.t("이미지 없음", "No image", "Kein Bild"));
        progressLabel = new Label("");
        progressLabel.setStyle("-fx-text-fill: steelblue; -fx-font-weight: bold;");

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

        HBox langBar = new HBox(6, btnKo, btnEn, btnDe);
        langBar.setStyle("-fx-padding: 4 8;");
        HBox toolbar1 = new HBox(8, openBtn, prevBtn, fileLabel, nextBtn,
                new Label(i18n.t("라벨:", "Label:", "Label:")), labelBox);
        toolbar1.setStyle("-fx-padding: 8 8 0 8;");

        HBox toolbar2 = new HBox(8, saveBtn, yoloBtn, saveProjectBtn, loadProjectBtn);
        toolbar2.setStyle("-fx-padding: 0 8 8 8;");
        HBox statusBar = new HBox(20, progressLabel);
        statusBar.setStyle("-fx-padding: 2 8;");
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

    private void showExportResult(String exportName, ExportService.ExportResult result) {
        Alert.AlertType type = result.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert alert = new Alert(type);
        alert.initOwner(mainStage);
        alert.setTitle(exportName);
        if (result.isSuccess()) {
            alert.setHeaderText(i18n.t("내보내기 완료", "Export complete", "Export abgeschlossen"));
            alert.setContentText(
                    i18n.t("저장 위치: ", "Saved to: ", "Gespeichert unter: ")
                            + result.getOutputPath().getAbsolutePath()
                            + "\n"
                            + i18n.t("라벨 수: ", "Labels: ", "Labels: ")
                            + result.getTotalLabels()
            );
        } else {
            alert.setHeaderText(i18n.t("내보내기 실패", "Export failed", "Export fehlgeschlagen"));
            alert.setContentText(result.getErrorMessage());
        }
        alert.showAndWait();
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
        File file = imageFiles.get(index);
        store.loadFor(file.getAbsolutePath());
        canvas.getImageView().setImage(new Image(file.toURI().toString()));
        fileLabel.setText(file.getName());
        for (Annotation ann : store.getCurrent()) {
            double dispW = canvas.getImageView().getFitWidth();
            double dispH = dispW / ann.imageWidth * ann.imageHeight;
            Rectangle r = new Rectangle(ann.x * dispW, ann.y * dispH, ann.w * dispW, ann.h * dispH);
            Color color = ImageCanvas.getLabelColor(ann.label);
            r.setStroke(color);
            r.setFill(Color.TRANSPARENT);
            r.setStrokeWidth(2);
            Text t = new Text(r.getX() + 2, r.getY() - 4, ann.label.display(i18n));
            t.setFill(color);
            canvas.addBox(r, t);
        }
        updateStats();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
