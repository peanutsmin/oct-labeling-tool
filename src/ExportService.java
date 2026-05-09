import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            Summary summary = new Summary(store.getAll().size());

            for (ArrayList<Annotation> list : store.getAll().values()) {
                for (Annotation ann : list) {
                    NormalizedBox box = NormalizedBox.fromAnnotation(ann);
                    if (!box.isValid()) continue;
                    labels.add(new LabelExport(ann, box));
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
                summary.total_labels = labels.size();
                GSON.toJson(new SummaryExport(summary), sw);
            }

            return ExportResult.success(outputDir, labels.size());
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
                        NormalizedBox box = NormalizedBox.fromAnnotation(ann);
                        if (!box.isValid()) continue;
                        fw.write(String.format(Locale.US, "%d %.6f %.6f %.6f %.6f%n",
                                classId, box.centerX(), box.centerY(), box.w, box.h));
                    }
                }
            }
            return ExportResult.success(dir, countExportableLabels(store));
        } catch (Exception e) {
            return ExportResult.failure(dir, e);
        }
    }

    public static ExportResult exportCoco(AnnotationStore store) {
        return exportCoco(store, new File("."));
    }

    public static ExportResult exportCoco(AnnotationStore store, File outputDir) {
        store.saveCurrent();
        File outputFile = new File(outputDir, "coco_annotations.json");

        try {
            ensureDirectory(outputDir);

            CocoExport coco = new CocoExport();
            for (LabelClass label : LabelClass.values()) {
                coco.categories.add(new CocoCategory(label.classId(), label.exportValue()));
            }

            int imageId = 1;
            int annotationId = 1;
            List<Map.Entry<String, ArrayList<Annotation>>> entries = new ArrayList<>(store.getAll().entrySet());
            entries.sort(Comparator.comparing(Map.Entry::getKey));

            for (Map.Entry<String, ArrayList<Annotation>> entry : entries) {
                ArrayList<Annotation> annotations = entry.getValue();
                int imageWidth = 0;
                int imageHeight = 0;
                if (!annotations.isEmpty()) {
                    imageWidth = annotations.get(0).imageWidth;
                    imageHeight = annotations.get(0).imageHeight;
                }

                String imageName = new File(entry.getKey()).getName();
                coco.images.add(new CocoImage(imageId, imageName, imageWidth, imageHeight));

                for (Annotation ann : annotations) {
                    NormalizedBox box = NormalizedBox.fromAnnotation(ann);
                    if (!box.isValid()) continue;
                    coco.annotations.add(new CocoAnnotation(annotationId, imageId, ann, box));
                    annotationId++;
                }
                imageId++;
            }

            try (FileWriter fw = new FileWriter(outputFile)) {
                GSON.toJson(coco, fw);
            }

            return ExportResult.success(outputFile, coco.annotations.size());
        } catch (Exception e) {
            return ExportResult.failure(outputFile, e);
        }
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

    private static int countExportableLabels(AnnotationStore store) {
        int count = 0;
        for (ArrayList<Annotation> list : store.getAll().values()) {
            for (Annotation ann : list) {
                if (NormalizedBox.fromAnnotation(ann).isValid()) {
                    count++;
                }
            }
        }
        return count;
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

        LabelExport(Annotation ann, NormalizedBox box) {
            file = ann.file;
            label = ann.label.exportValue();
            x = box.x;
            y = box.y;
            w = box.w;
            h = box.h;
            x_pixel = box.pixelX(ann.imageWidth);
            y_pixel = box.pixelY(ann.imageHeight);
            w_pixel = box.pixelW(ann.imageWidth);
            h_pixel = box.pixelH(ann.imageHeight);
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

        Summary(int totalImages) {
            total_images = totalImages;
        }
    }

    private static class CocoExport {
        ArrayList<CocoImage> images = new ArrayList<>();
        ArrayList<CocoAnnotation> annotations = new ArrayList<>();
        ArrayList<CocoCategory> categories = new ArrayList<>();
    }

    private static class CocoImage {
        int id;
        String file_name;
        int width;
        int height;

        CocoImage(int id, String fileName, int width, int height) {
            this.id = id;
            this.file_name = fileName;
            this.width = width;
            this.height = height;
        }
    }

    private static class CocoAnnotation {
        int id;
        int image_id;
        int category_id;
        double[] bbox;
        int area;
        int iscrowd = 0;

        CocoAnnotation(int id, int imageId, Annotation ann, NormalizedBox box) {
            int x = box.pixelX(ann.imageWidth);
            int y = box.pixelY(ann.imageHeight);
            int width = box.pixelW(ann.imageWidth);
            int height = box.pixelH(ann.imageHeight);

            this.id = id;
            this.image_id = imageId;
            this.category_id = ann.label.classId();
            this.bbox = new double[]{x, y, width, height};
            this.area = width * height;
        }
    }

    private static class CocoCategory {
        int id;
        String name;
        String supercategory = "lung_oct";

        CocoCategory(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
