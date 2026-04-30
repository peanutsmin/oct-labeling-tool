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
    private Label statsLabel;
    private Label progressLabel;
    private ImageView imageView;
    private Pane canvas;
    private ComboBox<String> labelBox;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        canvas = new Pane(imageView);

        labelBox = new ComboBox<>();
        labelBox.getItems().addAll("정상", "의심", "확실히 암");
        labelBox.setValue("의심");

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
            if (currentRect.getWidth() < 5 || currentRect.getHeight() <5) {
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
            Color color = label.equals("정상") ? Color.GREEN :
                          label.equals("의심") ? Color.ORANGE : Color.RED;
            currentRect.setStroke(color);
            Text t = new Text(currentRect.getX() + 2, currentRect.getY() - 4, label);
            t.setFill(color);
            texts.add(t);
            canvas.getChildren().add(t);
            currentRect = null;
            updateStats();
        });

        root.setCenter(canvas);

        Button openBtn = new Button("이미지 선택");
        openBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("이미지 선택 (여러 장 가능)");
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("이미지", "*.png", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG", "*.bmp")
            );
            List<File> files = fc.showOpenMultipleDialog(stage);
            if (files != null && !files.isEmpty()) {
                imageFiles = files;
                currentIndex = 0;
                savedLabels.clear();
                loadImage(currentIndex);
            }
        });

        Button prevBtn = new Button("< 이전");
        prevBtn.setOnAction(e -> {
            if (currentIndex > 0) {
                saveCurrentLabels();
                currentIndex--;
                loadImage(currentIndex);
            }
        });

        Button nextBtn = new Button("다음 >");
        nextBtn.setOnAction(e -> {
            if (currentIndex < imageFiles.size() - 1) {
                saveCurrentLabels();
                currentIndex++;
                loadImage(currentIndex);
            }
        });

        fileLabel = new Label("이미지 없음");
        progressLabel = new Label("");
        progressLabel.setStyle("-fx-text-fill: steelblue; -fx-font-weight: bold;");

        statsLabel = new Label("");
        statsLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11;");

        Button saveBtn = new Button("저장");
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
                int totalNormal = 0, totalSuspect = 0, totalCancer = 0;
                for (ArrayList<String> list : savedLabels.values()) {
                    for (String lbl : list) {
                        if (lbl.contains("정상")) totalNormal++;
                        else if (lbl.contains("의심")) totalSuspect++;
                        else if (lbl.contains("확실히 암")) totalCancer++;
                    }
                }

                FileWriter sw = new FileWriter("summary.json");
                sw.write("{\n");
                sw.write("  \"summary\": {\n");
                sw.write("    \"total_images\": " + savedLabels.size() + ",\n");
                sw.write("    \"total_labels\": " + total + ",\n");
                sw.write("    \"정상\": " + totalNormal + ",\n");
                sw.write("    \"의심\": " + totalSuspect + ",\n");
                sw.write("    \"확실히_암\": " + totalCancer + "\n");
                sw.write("  }\n}");
                sw.close();
                System.out.println("저장 완료! 총 " + total + "개 라벨");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox toolbar = new HBox(10, openBtn, prevBtn, fileLabel, nextBtn,
                                new Label("라벨:"), labelBox, saveBtn);
        toolbar.setStyle("-fx-padding: 8;");

        HBox statusBar = new HBox(20, progressLabel, statsLabel);
        statusBar.setStyle("-fx-padding: 4 8;");

        VBox top = new VBox(toolbar, statusBar);
        root.setTop(top);

        Scene scene = new Scene(root, 800, 620);

        // 단축키
        canvas.setFocusTraversable(true);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) {
                if (currentIndex < imageFiles.size() - 1) {
                    saveCurrentLabels();
                    currentIndex++;
                    loadImage(currentIndex);
                }
            } else if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) {
                if (currentIndex > 0) {
                    saveCurrentLabels();
                    currentIndex--;
                    loadImage(currentIndex);
                }
            } else if (e.getCode() == KeyCode.DIGIT1) {
                labelBox.setValue("정상");
            } else if (e.getCode() == KeyCode.DIGIT2) {
                labelBox.setValue("의심");
            } else if (e.getCode() == KeyCode.DIGIT3) {
                labelBox.setValue("확실히 암");
            }
        });

        stage.setTitle("OCT Labeling Tool");
        stage.setScene(scene);
        stage.show();
    }

    private void updateStats() {
        int normal = 0, suspect = 0, cancer = 0;
        for (String entry : currentLabels) {
            if (entry.contains("정상")) normal++;
            else if (entry.contains("의심")) suspect++;
            else if (entry.contains("확실히 암")) cancer++;
        }
        statsLabel.setText("현재 이미지 — 정상: " + normal + "  의심: " + suspect + "  확실히 암: " + cancer);

        if (!imageFiles.isEmpty()) {
            int done = savedLabels.size();
            progressLabel.setText("진행: " + (currentIndex + 1) + " / " + imageFiles.size() + "장");
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
        rects.clear();
        texts.clear();
        currentLabels.clear();
        File file = imageFiles.get(index);
        currentFile = file.getName();
        imageView.setImage(new Image(file.toURI().toString()));
        fileLabel.setText(currentFile);

        if (savedLabels.containsKey(currentFile)) {
            for (String entry : savedLabels.get(currentFile)) {
                String[] parts = entry.replace("{","").replace("}","").replace("\"","").split(",");
                double x = 0, y = 0, w = 0, h = 0;
                String lbl = "의심";
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length < 2) continue;
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if (key.equals("label")) lbl = val;
                    if (key.equals("x")) x = Double.parseDouble(val);
                    if (key.equals("y")) y = Double.parseDouble(val);
                    if (key.equals("w")) w = Double.parseDouble(val);
                    if (key.equals("h")) h = Double.parseDouble(val);
                }
                double dispW = imageView.getFitWidth();
                double dispH = dispW / imageView.getImage().getWidth() * imageView.getImage().getHeight();
                Rectangle r = new Rectangle(x * dispW, y * dispH, w * dispW, h * dispH);
                Color color = lbl.equals("정상") ? Color.GREEN :
                              lbl.equals("의심") ? Color.ORANGE : Color.RED;
                r.setStroke(color);
                r.setFill(Color.TRANSPARENT);
                r.setStrokeWidth(2);
                Text t = new Text(r.getX() + 2, r.getY() - 4, lbl);
                t.setFill(color);
                rects.add(r);
                texts.add(t);
                canvas.getChildren().addAll(r, t);
                currentLabels.add(entry);
            }
        }
        updateStats();
    }

    public static void main(String[] args) {
        launch(args);
    }
}