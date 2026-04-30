public class Annotation {
    public String file;
    public String label;
    public double x, y, w, h;
    public int xPixel, yPixel, wPixel, hPixel;
    public int imageWidth, imageHeight;

    public Annotation(String file, String label,
                      double x, double y, double w, double h,
                      int xPixel, int yPixel, int wPixel, int hPixel,
                      int imageWidth, int imageHeight) {
        this.file = file;
        this.label = label;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.xPixel = xPixel;
        this.yPixel = yPixel;
        this.wPixel = wPixel;
        this.hPixel = hPixel;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public String toJson() {
        return String.format(
                "{\"file\":\"%s\",\"label\":\"%s\"," +
                        "\"x\":%.4f,\"y\":%.4f,\"w\":%.4f,\"h\":%.4f," +
                        "\"x_pixel\":%d,\"y_pixel\":%d,\"w_pixel\":%d,\"h_pixel\":%d," +
                        "\"image_width\":%d,\"image_height\":%d}",
                file, label, x, y, w, h,
                xPixel, yPixel, wPixel, hPixel,
                imageWidth, imageHeight
        );
    }
}