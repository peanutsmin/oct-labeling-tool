public class NormalizedBox {
    public final double x;
    public final double y;
    public final double w;
    public final double h;

    private NormalizedBox(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static NormalizedBox fromAnnotation(Annotation ann) {
        return fromValues(ann.x, ann.y, ann.w, ann.h);
    }

    public static NormalizedBox fromValues(double x, double y, double w, double h) {
        double left = Math.min(x, x + w);
        double right = Math.max(x, x + w);
        double top = Math.min(y, y + h);
        double bottom = Math.max(y, y + h);

        double clippedLeft = clamp(left);
        double clippedRight = clamp(right);
        double clippedTop = clamp(top);
        double clippedBottom = clamp(bottom);

        return new NormalizedBox(
                clippedLeft,
                clippedTop,
                clippedRight - clippedLeft,
                clippedBottom - clippedTop
        );
    }

    public boolean isValid() {
        return w > 0.0 && h > 0.0;
    }

    public double centerX() {
        return x + w / 2.0;
    }

    public double centerY() {
        return y + h / 2.0;
    }

    public int pixelX(int imageWidth) {
        return toPixel(x, imageWidth);
    }

    public int pixelY(int imageHeight) {
        return toPixel(y, imageHeight);
    }

    public int pixelW(int imageWidth) {
        return toPixel(w, imageWidth);
    }

    public int pixelH(int imageHeight) {
        return toPixel(h, imageHeight);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int toPixel(double value, int size) {
        return (int) Math.round(value * size);
    }
}
