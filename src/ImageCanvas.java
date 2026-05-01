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

    public int findTopmostBox(double x, double y) {
        for (int idx = rects.size() - 1; idx >= 0; idx--) {
            if (rects.get(idx).contains(x, y)) {
                return idx;
            }
        }
        return -1;
    }

    public void updateTextPosition(int index) {
        Rectangle rect = rects.get(index);
        Text text = texts.get(index);
        text.setX(rect.getX() + 4);
        text.setY(Math.max(14, rect.getY() - 4));
    }

    public void selectBox(int index) {
        for (int i = 0; i < rects.size(); i++) {
            Rectangle rect = rects.get(i);
            rect.setStrokeWidth(i == index ? 3 : 2);
            rect.getStrokeDashArray().clear();
            if (i == index) {
                rect.getStrokeDashArray().addAll(8.0, 4.0);
            }
        }
    }

    public void clearSelection() {
        for (Rectangle rect : rects) {
            rect.setStrokeWidth(2);
            rect.getStrokeDashArray().clear();
        }
    }

    public static Color getLabelColor(LabelClass label) {
        return switch (label) {
            case NORMAL -> Color.GREEN;
            case SUSPICIOUS -> Color.ORANGE;
            case CONFIRMED_CANCER -> Color.RED;
        };
    }
}
