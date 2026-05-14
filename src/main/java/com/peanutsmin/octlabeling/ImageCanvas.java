package com.peanutsmin.octlabeling;

import javafx.scene.layout.Pane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class ImageCanvas extends Pane {

    private ImageView imageView;
    private ArrayList<Rectangle> rects = new ArrayList<>();
    private ArrayList<Text> texts = new ArrayList<>();
    private ArrayList<Polygon> masks = new ArrayList<>();
    private ArrayList<Text> maskTexts = new ArrayList<>();
    private double zoomFactor = 1.0;
    private double brightness = 0.0;
    private double contrast = 0.0;
    private ColorAdjust colorAdjust = new ColorAdjust();

    private static final double BASE_FIT_WIDTH = 800.0;
    public static final double MIN_ZOOM = 0.25;
    public static final double MAX_ZOOM = 4.0;
    public static final double ZOOM_STEP = 0.25;
    public static final double MIN_ADJUSTMENT = -1.0;
    public static final double MAX_ADJUSTMENT = 1.0;

    public ImageCanvas() {
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(BASE_FIT_WIDTH);
        imageView.setEffect(colorAdjust);
        getChildren().add(imageView);
        setFocusTraversable(true);
        updateCanvasSize();
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

    public ArrayList<Polygon> getMasks() {
        return masks;
    }

    public ArrayList<Text> getMaskTexts() {
        return maskTexts;
    }

    public void setImage(Image image) {
        imageView.setImage(image);
        applyZoom();
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void zoomIn() {
        setZoomFactor(zoomFactor + ZOOM_STEP);
    }

    public void zoomOut() {
        setZoomFactor(zoomFactor - ZOOM_STEP);
    }

    public void resetZoom() {
        setZoomFactor(1.0);
    }

    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = clamp(zoomFactor, MIN_ZOOM, MAX_ZOOM);
        applyZoom();
    }

    public double getBrightness() {
        return brightness;
    }

    public double getContrast() {
        return contrast;
    }

    public void setBrightness(double brightness) {
        this.brightness = clamp(brightness, MIN_ADJUSTMENT, MAX_ADJUSTMENT);
        applyImageAdjustments();
    }

    public void setContrast(double contrast) {
        this.contrast = clamp(contrast, MIN_ADJUSTMENT, MAX_ADJUSTMENT);
        applyImageAdjustments();
    }

    public void resetImageAdjustments() {
        brightness = 0.0;
        contrast = 0.0;
        applyImageAdjustments();
    }

    public void addBox(Rectangle r, Text t) {
        rects.add(r);
        texts.add(t);
        getChildren().addAll(r, t);
    }

    public void addMask(Polygon polygon, Text text) {
        masks.add(polygon);
        maskTexts.add(text);
        getChildren().addAll(polygon, text);
    }

    public void removeBox(int index) {
        getChildren().remove(rects.get(index));
        getChildren().remove(texts.get(index));
        rects.remove(index);
        texts.remove(index);
    }

    public void removeMask(int index) {
        getChildren().remove(masks.get(index));
        getChildren().remove(maskTexts.get(index));
        masks.remove(index);
        maskTexts.remove(index);
    }

    public void clearBoxes() {
        getChildren().removeAll(rects);
        getChildren().removeAll(texts);
        getChildren().removeAll(masks);
        getChildren().removeAll(maskTexts);
        rects.clear();
        texts.clear();
        masks.clear();
        maskTexts.clear();
    }

    public int findTopmostBox(double x, double y) {
        for (int idx = rects.size() - 1; idx >= 0; idx--) {
            if (rects.get(idx).contains(x, y)) {
                return idx;
            }
        }
        return -1;
    }

    public int findTopmostMask(double x, double y) {
        for (int idx = masks.size() - 1; idx >= 0; idx--) {
            if (masks.get(idx).contains(x, y)) {
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

    public void updateMaskTextPosition(int index) {
        Polygon polygon = masks.get(index);
        Text text = maskTexts.get(index);
        var bounds = polygon.getBoundsInParent();
        text.setX(bounds.getMinX() + 4);
        text.setY(Math.max(14, bounds.getMinY() - 4));
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
        clearMaskSelection();
    }

    public void selectMask(int index) {
        clearSelection();
        for (int i = 0; i < masks.size(); i++) {
            Polygon polygon = masks.get(i);
            polygon.setStrokeWidth(i == index ? 3 : 2);
            polygon.getStrokeDashArray().clear();
            if (i == index) {
                polygon.getStrokeDashArray().addAll(8.0, 4.0);
            }
        }
    }

    public void clearSelection() {
        for (Rectangle rect : rects) {
            rect.setStrokeWidth(2);
            rect.getStrokeDashArray().clear();
        }
        clearMaskSelection();
    }

    private void clearMaskSelection() {
        for (Polygon polygon : masks) {
            polygon.setStrokeWidth(2);
            polygon.getStrokeDashArray().clear();
        }
    }

    private void applyZoom() {
        imageView.setFitWidth(BASE_FIT_WIDTH * zoomFactor);
        updateCanvasSize();
    }

    private void applyImageAdjustments() {
        colorAdjust.setBrightness(brightness);
        colorAdjust.setContrast(contrast);
    }

    private void updateCanvasSize() {
        double width = imageView.getFitWidth();
        double height = width;
        Image image = imageView.getImage();
        if (image != null && image.getWidth() > 0) {
            height = image.getHeight() * (width / image.getWidth());
        }
        setMinSize(width, height);
        setPrefSize(width, height);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static Color getLabelColor(LabelClass label) {
        return switch (label) {
            case NORMAL -> Color.GREEN;
            case SUSPICIOUS -> Color.ORANGE;
            case CONFIRMED_CANCER -> Color.RED;
        };
    }
}
