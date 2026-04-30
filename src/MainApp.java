import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
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
                Rectangle target = null;
                double minDist = Double.MAX_VALUE;
                for (Rectangle r : canvas.getRects()) {
                    double cx = r.getX() + r.getWidth() / 2;
                    double cy = r.getY() + r.getHeight() / 2;
                    double dist = Math.hypot(e.getX() - cx, e.getY() - cy);
                    if (dist < minDist) {
                        minDist = dist;
                        target = r;
                    }
                }
                if (target != null) {
                    int idx = canvas.getRects().indexOf(target);
                    canvas.removeBox(idx);
                    store.removeAnnotation(idx);
                    updateStats();
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
            String label = labelBox.getValue();
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
            Text txt = new Text(currentRect.getX() + 2, currentRect.getY() - 4, label);
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

        Button saveBtn = new Button(i18n.t("저장", "Save", "Speichern"));
        saveBtn.setOnAction(e -> ExportService.exportLabels(store));

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
        HBox toolbar = new HBox(8, openBtn, prevBtn, fileLabel, nextBtn,
                new Label(i18n.t("라벨:", "Label:", "Label:")), labelBox, saveBtn);
        toolbar.setStyle("-fx-padding: 8;");
        HBox statusBar = new HBox(20, progressLabel);
        statusBar.setStyle("-fx-padding: 2 8;");
        VBox top = new VBox(langBar, toolbar, statusBar);
        root.setTop(top);
        if (mainStage != null)
            mainStage.setTitle(i18n.t("OCT 라벨링 툴", "OCT Labeling Tool", "OCT Beschriftungswerkzeug"));
    }

    private void updateStats() {
        int n = 0, s = 0, c = 0;
        for (Annotation ann : store.getCurrent()) {
            if (ann.label.equals("정상") || ann.label.equals("Normal")) n++;
            else if (ann.label.equals("의심") || ann.label.equals("Suspicious") || ann.label.equals("Verdächtig")) s++;
            else c++;
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
            Text t = new Text(r.getX() + 2, r.getY() - 4, ann.label);
            t.setFill(color);
            canvas.addBox(r, t);
        }
        updateStats();
    }

    public static void main(String[] args) {
        launch(args);
    }
}