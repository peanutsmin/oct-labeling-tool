import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class ExportService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ExportResult exportLabels(AnnotationStore store) {
        return exportLabels(store, new File("."));
    }

    public static ExportResult exportLabels(AnnotationStore store, File outputDir) {
        store.saveCurrent();
        try {
            ensureDirectory(outputDir);
            ArrayList<LabelExport> labels = new ArrayList<>();
            Summary summary = new Summary(store.getAll().size(), store.totalCount());

            for (ArrayList<Annotation> list : store.getAll().values()) {
                for (Annotation ann : list) {
                    labels.add(new LabelExport(ann));
                    switch (ann.label) {
                        case NORMAL -> summary.normal++;
                        case SUSPICIOUS -> summary.suspicious++;
                        case CONFIRMED_CANCER -> summary.confirmed_cancer++;
                    }
                }
            }

            try (FileWriter fw = new FileWriter(new File(outputDir, "labels.json"))) {
                GSON.toJson(labels, fw);
            }

            try (FileWriter sw = new FileWriter(new File(outputDir, "summary.json"))) {
                GSON.toJson(new SummaryExport(summary), sw);
            }

            return ExportResult.success(outputDir, store.totalCount());
        } catch (Exception e) {
            return ExportResult.failure(outputDir, e);
        }
    }

    public static ExportResult exportYolo(AnnotationStore store) {
        return exportYolo(store, new File("."));
    }

    public static ExportResult exportYolo(AnnotationStore store, File outputDir) {
        store.saveCurrent();
        File dir = new File(outputDir, "labels_yolo");

        try {
            ensureDirectory(outputDir);
            ensureDirectory(dir);

            for (java.util.Map.Entry<String, ArrayList<Annotation>> entry : store.getAll().entrySet()) {
                String imagePath = entry.getKey();
                String imageName = new File(imagePath).getName();
                String baseName = imageName.replaceAll("\\.[^.]+$", "");

                try (FileWriter fw = new FileWriter(new File(dir, baseName + ".txt"))) {
                    for (Annotation ann : entry.getValue()) {
                        int classId = ann.label.classId();
                        double x = clamp(ann.x);
                        double y = clamp(ann.y);
                        double w = clamp(ann.w);
                        double h = clamp(ann.h);
                        if (w <= 0 || h <= 0) continue;
                        if (x + w > 1.0) w = 1.0 - x;
                        if (y + h > 1.0) h = 1.0 - y;
                        double xCenter = x + w / 2.0;
                        double yCenter = y + h / 2.0;
                        fw.write(String.format(Locale.US, "%d %.6f %.6f %.6f %.6f%n",
                                classId, xCenter, yCenter, w, h));
                    }
                }
            }
            return ExportResult.success(dir, store.totalCount());
        } catch (Exception e) {
            return ExportResult.failure(dir, e);
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void ensureDirectory(File dir) throws IOException {
        if (dir == null) {
            throw new IOException("Output directory is not selected.");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create output directory: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new IOException("Output path is not a directory: " + dir.getAbsolutePath());
        }
    }

    public static class ExportResult {
        private final boolean success;
        private final File outputPath;
        private final int totalLabels;
        private final String errorMessage;

        private ExportResult(boolean success, File outputPath, int totalLabels, String errorMessage) {
            this.success = success;
            this.outputPath = outputPath;
            this.totalLabels = totalLabels;
            this.errorMessage = errorMessage;
        }

        public static ExportResult success(File outputPath, int totalLabels) {
            return new ExportResult(true, outputPath, totalLabels, null);
        }

        public static ExportResult failure(File outputPath, Exception error) {
            String message = error.getMessage();
            if (message == null || message.isBlank()) {
                message = error.getClass().getSimpleName();
            }
            return new ExportResult(false, outputPath, 0, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public File getOutputPath() {
            return outputPath;
        }

        public int getTotalLabels() {
            return totalLabels;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private static class LabelExport {
        String file;
        String label;
        double x, y, w, h;
        int x_pixel, y_pixel, w_pixel, h_pixel;
        int image_width, image_height;

        LabelExport(Annotation ann) {
            file = ann.file;
            label = ann.label.exportValue();
            x = ann.x;
            y = ann.y;
            w = ann.w;
            h = ann.h;
            x_pixel = ann.xPixel;
            y_pixel = ann.yPixel;
            w_pixel = ann.wPixel;
            h_pixel = ann.hPixel;
            image_width = ann.imageWidth;
            image_height = ann.imageHeight;
        }
    }

    private static class SummaryExport {
        Summary summary;

        SummaryExport(Summary summary) {
            this.summary = summary;
        }
    }

    private static class Summary {
        int total_images;
        int total_labels;
        int normal;
        int suspicious;
        int confirmed_cancer;

        Summary(int totalImages, int totalLabels) {
            total_images = totalImages;
            total_labels = totalLabels;
        }
    }
}
