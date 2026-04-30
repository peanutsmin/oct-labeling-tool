import javafx.scene.layout.Pane;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class ImageCanvas extends Pane {

    private ImageView imageView;
    private ArrayList<Rectangle> rects = new ArrayList<>();
    private ArrayList<Text> texts = new ArrayList<>();

    public ImageCanvas() {
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        getChildren().add(imageView);
        setFocusTraversable(true);
    }

    public ImageView getImageView() {
        return imageView;
    }

    public ArrayList<Rectangle> getRects() {
        return rects;
    }

    public ArrayList<Text> getTexts() {
        return texts;
    }

    public void addBox(Rectangle r, Text t) {
        rects.add(r);
        texts.add(t);
        getChildren().addAll(r, t);
    }

    public void removeBox(int index) {
        getChildren().remove(rects.get(index));
        getChildren().remove(texts.get(index));
        rects.remove(index);
        texts.remove(index);
    }

    public void clearBoxes() {
        getChildren().removeAll(rects);
        getChildren().removeAll(texts);
        rects.clear();
        texts.clear();
    }

    public static Color getLabelColor(String label) {
        if (label.equals("정상") || label.equals("Normal")) return Color.GREEN;
        if (label.equals("의심") || label.equals("Suspicious") || label.equals("Verdächtig")) return Color.ORANGE;
        return Color.RED;
    }
}