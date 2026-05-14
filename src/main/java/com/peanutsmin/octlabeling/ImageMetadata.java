package com.peanutsmin.octlabeling;

import java.io.File;

public class ImageMetadata {
    private final String path;
    private final String fileName;
    private final int width;
    private final int height;

    public ImageMetadata(String path, int width, int height) {
        this.path = path;
        this.fileName = new File(path).getName();
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public String path() {
        return path;
    }

    public String fileName() {
        return fileName;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
