import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OctViewer extends Application {

    // 언어 설정
    private String lang = "en";

    private String t(String ko, String en, String de) {
        if (lang.equals("ko")) return ko;
        if (lang.equals("de")) return de;
        return en;
    }

    private double startX, startY;
    private Rectangle currentRect;
    private String currentFile = "";
    private ArrayList<Rectangle> rects = new ArrayList<>();
    private ArrayList<Text> texts = new ArrayList<>();
    private ArrayList<String> currentLabels = new ArrayList<>();
    private HashMap<String, ArrayList<String>> savedLabels = new HashMap<>();
    private List<File> imageFiles = new ArrayList<>();
    private int currentIndex = 0;
    private Label fileLabel;
    private Label progressLabel;
    private ComboBox<String> labelBox;
    private Button openBtn, prevBtn, nextBtn, saveBtn;
    private Label labelTitle;
    private HBox toolbar;
    private Scene scene;
    private Stage mainStage;
    private Pane canvas;
    private ImageView imageView;

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;
        BorderPane root = new BorderPane();
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        canvas = new Pane(imageView);
        canvas.setFocusTraversable(true);

        labelBox = new ComboBox<>();
        updateLanguage(root);

        canvas.setOnMousePressed(e -> {
            canvas.requestFocus();
            if (e.isSecondaryButtonDown()) {
                Rectangle target = null;
                double minDist = Double.MAX_VALUE;
                for (Rectangle r : rects) {
                    double cx = r.getX() + r.getWidth() / 2;
                    double cy = r.getY() + r.getHeight() / 2;
                    double dist = Math.hypot(e.getX() - cx, e.getY() - cy);
                    if (dist < minDist) { minDist = dist; target = r; }
                }
                if (target != null) {
                    int idx = rects.indexOf(target);
                    canvas.getChildren().remove(target);
                    canvas.getChildren().remove(texts.get(idx));
                    rects.remove(idx);
                    texts.remove(idx);
                    currentLabels.remove(idx);
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
            double dispW = imageView.getBoundsInParent().getWidth();
            double dispH = imageView.getBoundsInParent().getHeight();
            double nx = currentRect.getX() / dispW;
            double ny = currentRect.getY() / dispH;
            double nw = currentRect.getWidth() / dispW;
            double nh = currentRect.getHeight() / dispH;
            String entry = String.format(
                "{\"file\":\"%s\",\"label\":\"%s\",\"x\":%.4f,\"y\":%.4f,\"w\":%.4f,\"h\":%.4f}",
                currentFile, label, nx, ny, nw, nh
            );
            currentLabels.add(entry);
            rects.add(currentRect);
            Color color = getLabelColor(label);
            currentRect.setStroke(color);
            Text txt = new Text(currentRect.getX() + 2, currentRect.getY() - 4, label);
            txt.setFill(color);
            texts.add(txt);
            canvas.getChildren().add(txt);
            currentRect = null;
            updateStats();
        });

        root.setCenter(canvas);

        scene = new Scene(root, 800, 640);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) {
                if (currentIndex < imageFiles.size() - 1) {
                    saveCurrentLabels(); currentIndex++; loadImage(currentIndex);
                }
            } else if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) {
                if (currentIndex > 0) {
                    saveCurrentLabels(); currentIndex--; loadImage(currentIndex);
                }
            } else if (e.getCode() == KeyCode.DIGIT1) {
                labelBox.getSelectionModel().select(0);
            } else if (e.getCode() == KeyCode.DIGIT2) {
                labelBox.getSelectionModel().select(1);
            } else if (e.getCode() == KeyCode.DIGIT3) {
                labelBox.getSelectionModel().select(2);
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    private Color getLabelColor(String label) {
        if (label.equals("정상") || label.equals("Normal") || label.equals("Normal (DE)")) return Color.GREEN;
        if (label.equals("의심") || label.equals("Suspicious") || label.equals("Verdächtig")) return Color.ORANGE;
        return Color.RED;
    }

    private void updateLanguage(BorderPane root) {
        labelBox.getItems().clear();
        labelBox.getItems().addAll(
            t("정상", "Normal", "Normal"),
            t("의심", "Suspicious", "Verdächtig"),
            t("확실히 암", "Confirmed Cancer", "Bestätigter Krebs")
        );
        labelBox.getSelectionModel().select(1);

        openBtn = new Button(t("이미지 선택", "Open Images", "Bilder öffnen"));
        openBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(t("이미지 선택", "Select Images", "Bilder auswählen"));
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(t("이미지", "Images", "Bilder"), "*.png", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG", "*.bmp")
            );
            List<File> files = fc.showOpenMultipleDialog(mainStage);
            if (files != null && !files.isEmpty()) {
                imageFiles = files;
                currentIndex = 0;
                savedLabels.clear();
                loadImage(currentIndex);
            }
        });

        prevBtn = new Button(t("< 이전", "< Prev", "< Zurück"));
        prevBtn.setOnAction(e -> {
            if (currentIndex > 0) { saveCurrentLabels(); currentIndex--; loadImage(currentIndex); }
        });

        nextBtn = new Button(t("다음 >", "Next >", "Weiter >"));
        nextBtn.setOnAction(e -> {
            if (currentIndex < imageFiles.size() - 1) { saveCurrentLabels(); currentIndex++; loadImage(currentIndex); }
        });

        saveBtn = new Button(t("저장", "Save", "Speichern"));
        saveBtn.setOnAction(e -> {
            saveCurrentLabels();
            try {
                FileWriter fw = new FileWriter("labels.json");
                fw.write("[\n");
                boolean first = true;
                for (ArrayList<String> list : savedLabels.values()) {
                    for (String lbl : list) {
                        if (!first) fw.write(",\n");
                        fw.write("  " + lbl);
                        first = false;
                    }
                }
                fw.write("\n]");
                fw.close();

                int total = savedLabels.values().stream().mapToInt(List::size).sum();
                int n = 0, s = 0, c = 0;
                for (ArrayList<String> list : savedLabels.values()) {
                    for (String lbl : list) {
                        if (lbl.contains("정상") || lbl.contains("Normal")) n++;
                        else if (lbl.contains("의심") || lbl.contains("Suspicious") || lbl.contains("Verdächtig")) s++;
                        else c++;
                    }
                }
                FileWriter sw = new FileWriter("summary.json");
                sw.write("{\n  \"summary\": {\n");
                sw.write("    \"total_images\": " + savedLabels.size() + ",\n");
                sw.write("    \"total_labels\": " + total + ",\n");
                sw.write("    \"normal\": " + n + ",\n");
                sw.write("    \"suspicious\": " + s + ",\n");
                sw.write("    \"confirmed_cancer\": " + c + "\n");
                sw.write("  }\n}");
                sw.close();
                System.out.println(t("저장 완료! 총 ", "Saved! Total ", "Gespeichert! Gesamt ") + total);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        fileLabel = new Label(t("이미지 없음", "No image", "Kein Bild"));
        progressLabel = new Label("");
        progressLabel.setStyle("-fx-text-fill: steelblue; -fx-font-weight: bold;");

        // 언어 버튼
        Button btnKo = new Button("한국어");
        Button btnEn = new Button("English");
        Button btnDe = new Button("Deutsch");

        btnKo.setOnAction(e -> { lang = "ko"; updateLanguage(root); });
        btnEn.setOnAction(e -> { lang = "en"; updateLanguage(root); });
        btnDe.setOnAction(e -> { lang = "de"; updateLanguage(root); });

        labelTitle = new Label(t("라벨:", "Label:", "Label:"));

        HBox langBar = new HBox(6, btnKo, btnEn, btnDe);
        langBar.setStyle("-fx-padding: 4 8;");

        toolbar = new HBox(8, openBtn, prevBtn, fileLabel, nextBtn,
                           labelTitle, labelBox, saveBtn);
        toolbar.setStyle("-fx-padding: 8;");

        HBox statusBar = new HBox(20, progressLabel);
        statusBar.setStyle("-fx-padding: 2 8;");

        VBox top = new VBox(langBar, toolbar, statusBar);
        root.setTop(top);

        if (mainStage != null) {
            mainStage.setTitle(t("OCT 라벨링 툴", "OCT Labeling Tool", "OCT Beschriftungswerkzeug"));
        }
    }

    private void updateStats() {
        int n = 0, s = 0, c = 0;
        for (String entry : currentLabels) {
            if (entry.contains("정상") || entry.contains("Normal")) n++;
            else if (entry.contains("의심") || entry.contains("Suspicious") || entry.contains("Verdächtig")) s++;
            else c++;
        }
        String stats = t(
            "정상: " + n + "  의심: " + s + "  확실히 암: " + c,
            "Normal: " + n + "  Suspicious: " + s + "  Cancer: " + c,
            "Normal: " + n + "  Verdächtig: " + s + "  Krebs: " + c
        );
        if (!imageFiles.isEmpty()) {
            progressLabel.setText((currentIndex + 1) + " / " + imageFiles.size() + "   " + stats);
        }
    }

    private void saveCurrentLabels() {
        if (!currentFile.isEmpty()) {
            savedLabels.put(currentFile, new ArrayList<>(currentLabels));
        }
    }

    private void loadImage(int index) {
        canvas.getChildren().removeAll(rects);
        canvas.getChildren().removeAll(texts);
        rects.clear(); texts.clear(); currentLabels.clear();
        File file = imageFiles.get(index);
        currentFile = file.getName();
        imageView.setImage(new Image(file.toURI().toString()));
        fileLabel.setText(currentFile);
        if (savedLabels.containsKey(currentFile)) {
            for (String entry : savedLabels.get(currentFile)) {
                String[] parts = entry.replace("{","").replace("}","").replace("\"","").split(",");
                double x = 0, y = 0, w = 0, h = 0; String lbl = "Suspicious";
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length < 2) continue;
                    String key = kv[0].trim(), val = kv[1].trim();
                    if (key.equals("label")) lbl = val;
                    else if (key.equals("x")) x = Double.parseDouble(val);
                    else if (key.equals("y")) y = Double.parseDouble(val);
                    else if (key.equals("w")) w = Double.parseDouble(val);
                    else if (key.equals("h")) h = Double.parseDouble(val);
                }
                double dispW = imageView.getFitWidth();
                double dispH = dispW / imageView.getImage().getWidth() * imageView.getImage().getHeight();
                Rectangle r = new Rectangle(x * dispW, y * dispH, w * dispW, h * dispH);
                Color color = getLabelColor(lbl);
                r.setStroke(color); r.setFill(Color.TRANSPARENT); r.setStrokeWidth(2);
                Text t = new Text(r.getX() + 2, r.getY() - 4, lbl);
                t.setFill(color);
                rects.add(r); texts.add(t);
                canvas.getChildren().addAll(r, t);
                currentLabels.add(entry);
            }
        }
        updateStats();
    }

    public static void main(String[] args) { launch(args); }
}