package com.peanutsmin.octlabeling;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class DicomImageLoader {
    private static final int DEFAULT_WINDOW_WIDTH = 4096;

    public static boolean isDicom(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".dcm") || name.endsWith(".dicom") || name.endsWith(".ima");
    }

    public static Image load(File file) throws IOException {
        try (DicomInputStream input = new DicomInputStream(file)) {
            Attributes dataset = input.readDataset(-1, -1);
            return toImage(dataset);
        }
    }

    private static Image toImage(Attributes dataset) throws IOException {
        int rows = dataset.getInt(Tag.Rows, 0);
        int columns = dataset.getInt(Tag.Columns, 0);
        int samplesPerPixel = dataset.getInt(Tag.SamplesPerPixel, 1);
        int bitsAllocated = dataset.getInt(Tag.BitsAllocated, 0);
        String photometric = dataset.getString(Tag.PhotometricInterpretation, "MONOCHROME2");
        byte[] pixelData = dataset.getBytes(Tag.PixelData);

        if (rows <= 0 || columns <= 0) {
            throw new IOException("DICOM image dimensions are missing.");
        }
        if (samplesPerPixel != 1) {
            throw new IOException("Only single-channel grayscale DICOM images are supported.");
        }
        if (bitsAllocated != 8 && bitsAllocated != 16) {
            throw new IOException("Only 8-bit and 16-bit DICOM images are supported.");
        }
        if (pixelData == null || pixelData.length == 0) {
            throw new IOException("DICOM pixel data is missing or compressed.");
        }

        WritableImage image = new WritableImage(columns, rows);
        int expectedLength = rows * columns * (bitsAllocated / 8);
        if (pixelData.length < expectedLength) {
            throw new IOException("DICOM pixel data is shorter than expected.");
        }

        double slope = dataset.getDouble(Tag.RescaleSlope, 1.0);
        double intercept = dataset.getDouble(Tag.RescaleIntercept, 0.0);
        Window window = resolveWindow(dataset, bitsAllocated);
        boolean invert = "MONOCHROME1".equalsIgnoreCase(photometric);
        boolean signed = dataset.getInt(Tag.PixelRepresentation, 0) == 1;

        int offset = 0;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int storedValue;
                if (bitsAllocated == 8) {
                    storedValue = signed ? pixelData[offset] : Byte.toUnsignedInt(pixelData[offset]);
                    offset++;
                } else {
                    int low = Byte.toUnsignedInt(pixelData[offset]);
                    int high = Byte.toUnsignedInt(pixelData[offset + 1]);
                    storedValue = (high << 8) | low;
                    if (signed && storedValue > 32767) {
                        storedValue -= 65536;
                    }
                    offset += 2;
                }

                double value = storedValue * slope + intercept;
                double gray = normalize(value, window.center, window.width);
                if (invert) {
                    gray = 1.0 - gray;
                }
                image.getPixelWriter().setColor(x, y, Color.gray(gray));
            }
        }

        return image;
    }

    private static Window resolveWindow(Attributes dataset, int bitsAllocated) {
        double center = firstDouble(dataset, Tag.WindowCenter, Double.NaN);
        double width = firstDouble(dataset, Tag.WindowWidth, Double.NaN);
        if (!Double.isNaN(center) && !Double.isNaN(width) && width > 0) {
            return new Window(center, width);
        }

        int fallbackWidth = bitsAllocated == 16 ? DEFAULT_WINDOW_WIDTH : 256;
        return new Window(fallbackWidth / 2.0, fallbackWidth);
    }

    private static double firstDouble(Attributes dataset, int tag, double fallback) {
        double[] values = dataset.getDoubles(tag);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return fallback;
    }

    private static double normalize(double value, double center, double width) {
        double min = center - width / 2.0;
        return clamp((value - min) / width);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record Window(double center, double width) {
    }
}
