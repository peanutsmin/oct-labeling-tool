package com.peanutsmin.octlabeling;

import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnnotationGeometry {

    public static Bounds imageBounds(ImageCanvas canvas) {
        if (canvas.getImageView().getImage() == null) {
            return null;
        }
        return canvas.getImageView().getBoundsInParent();
    }

    public static boolean isInsideImage(ImageCanvas canvas, double x, double y) {
        Bounds bounds = imageBounds(canvas);
        return bounds != null && bounds.contains(x, y);
    }

    public static Rectangle rectangleFromAnnotation(Annotation ann, ImageCanvas canvas) {
        Bounds bounds = imageBounds(canvas);
        if (bounds == null) {
            return new Rectangle();
        }
        return new Rectangle(
                bounds.getMinX() + ann.x * bounds.getWidth(),
                bounds.getMinY() + ann.y * bounds.getHeight(),
                ann.w * bounds.getWidth(),
                ann.h * bounds.getHeight()
        );
    }

    public static Annotation annotationFromRectangle(File imageFile, LabelClass label, Rectangle rect, ImageCanvas canvas) {
        Image image = canvas.getImageView().getImage();
        int imageWidth = (int) image.getWidth();
        int imageHeight = (int) image.getHeight();
        NormalizedBox box = normalizedBox(rect, canvas);
        return new Annotation(
                imageFile.getName(),
                label,
                box.x, box.y, box.w, box.h,
                box.pixelX(imageWidth), box.pixelY(imageHeight), box.pixelW(imageWidth), box.pixelH(imageHeight),
                imageWidth, imageHeight
        );
    }

    public static Polygon polygonFromMask(MaskAnnotation mask, ImageCanvas canvas) {
        Bounds bounds = imageBounds(canvas);
        Polygon polygon = new Polygon();
        if (bounds == null) {
            return polygon;
        }
        for (MaskPoint point : mask.points) {
            polygon.getPoints().add(bounds.getMinX() + point.x * bounds.getWidth());
            polygon.getPoints().add(bounds.getMinY() + point.y * bounds.getHeight());
        }
        styleMaskPolygon(polygon, mask.label);
        return polygon;
    }

    public static MaskAnnotation maskFromCanvasPoints(File imageFile, LabelClass label, List<Double> canvasPoints, ImageCanvas canvas) {
        Image image = canvas.getImageView().getImage();
        int imageWidth = (int) image.getWidth();
        int imageHeight = (int) image.getHeight();
        Bounds bounds = imageBounds(canvas);
        ArrayList<MaskPoint> normalizedPoints = new ArrayList<>();
        for (int i = 0; i + 1 < canvasPoints.size(); i += 2) {
            double x = (canvasPoints.get(i) - bounds.getMinX()) / bounds.getWidth();
            double y = (canvasPoints.get(i + 1) - bounds.getMinY()) / bounds.getHeight();
            normalizedPoints.add(new MaskPoint(clamp01(x), clamp01(y)));
        }
        return new MaskAnnotation(imageFile.getName(), label, normalizedPoints, imageWidth, imageHeight);
    }

    public static void styleMaskPolygon(Polygon polygon, LabelClass label) {
        Color color = ImageCanvas.getLabelColor(label);
        polygon.setStroke(color);
        polygon.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.22));
        polygon.setStrokeWidth(2);
    }

    public static void updateAnnotationFromRectangle(Annotation ann, Rectangle rect, ImageCanvas canvas) {
        NormalizedBox box = normalizedBox(rect, canvas);
        ann.x = box.x;
        ann.y = box.y;
        ann.w = box.w;
        ann.h = box.h;
        ann.xPixel = box.pixelX(ann.imageWidth);
        ann.yPixel = box.pixelY(ann.imageHeight);
        ann.wPixel = box.pixelW(ann.imageWidth);
        ann.hPixel = box.pixelH(ann.imageHeight);
    }

    public static double clampX(ImageCanvas canvas, double x) {
        Bounds bounds = imageBounds(canvas);
        if (bounds == null) return x;
        return clamp(x, bounds.getMinX(), bounds.getMaxX());
    }

    public static double clampY(ImageCanvas canvas, double y) {
        Bounds bounds = imageBounds(canvas);
        if (bounds == null) return y;
        return clamp(y, bounds.getMinY(), bounds.getMaxY());
    }

    private static NormalizedBox normalizedBox(Rectangle rect, ImageCanvas canvas) {
        Bounds bounds = imageBounds(canvas);
        double x = (rect.getX() - bounds.getMinX()) / bounds.getWidth();
        double y = (rect.getY() - bounds.getMinY()) / bounds.getHeight();
        double w = rect.getWidth() / bounds.getWidth();
        double h = rect.getHeight() / bounds.getHeight();
        return NormalizedBox.fromValues(x, y, w, h);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }
}
