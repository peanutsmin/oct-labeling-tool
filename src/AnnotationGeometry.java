import javafx.geometry.Bounds;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;

import java.io.File;

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
}
