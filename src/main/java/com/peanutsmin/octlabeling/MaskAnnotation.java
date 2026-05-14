package com.peanutsmin.octlabeling;

import java.util.ArrayList;
import java.util.List;

public class MaskAnnotation {
    public String file;
    public LabelClass label;
    public ArrayList<MaskPoint> points;
    public int imageWidth;
    public int imageHeight;

    public MaskAnnotation(String file, LabelClass label, List<MaskPoint> points, int imageWidth, int imageHeight) {
        this.file = file;
        this.label = label;
        this.points = new ArrayList<>(points);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public boolean isValid() {
        return points.size() >= 3 && imageWidth > 0 && imageHeight > 0;
    }
}
