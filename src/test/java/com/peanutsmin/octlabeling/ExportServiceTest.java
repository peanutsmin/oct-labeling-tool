package com.peanutsmin.octlabeling;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExportServiceTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void exportsJsonAndSummaryToSelectedDirectory() throws Exception {
        AnnotationStore store = sampleStore();
        File outputDir = temp.newFolder("json-export");

        ExportService.ExportResult result = ExportService.exportLabels(store, outputDir);

        assertTrue(result.isSuccess());
        assertEquals(outputDir.getAbsolutePath(), result.getOutputPath().getAbsolutePath());
        assertEquals(1, result.getTotalLabels());

        File labelsFile = new File(outputDir, "labels.json");
        File summaryFile = new File(outputDir, "summary.json");
        assertTrue(labelsFile.isFile());
        assertTrue(summaryFile.isFile());

        try (FileReader reader = new FileReader(labelsFile)) {
            JsonArray labels = JsonParser.parseReader(reader).getAsJsonArray();
            JsonObject label = labels.get(0).getAsJsonObject();
            assertEquals("sample_001.png", label.get("file").getAsString());
            assertEquals("suspicious", label.get("label").getAsString());
        }

        try (FileReader reader = new FileReader(summaryFile)) {
            JsonObject summary = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("summary");
            assertEquals(1, summary.get("total_images").getAsInt());
            assertEquals(1, summary.get("total_labels").getAsInt());
            assertEquals(1, summary.get("suspicious").getAsInt());
        }
    }

    @Test
    public void exportsYoloFilesUnderSelectedDirectory() throws Exception {
        AnnotationStore store = sampleStore();
        File outputDir = temp.newFolder("yolo-export");

        Locale originalLocale = Locale.getDefault();
        ExportService.ExportResult result;
        try {
            Locale.setDefault(Locale.GERMANY);
            result = ExportService.exportYolo(store, outputDir);
        } finally {
            Locale.setDefault(originalLocale);
        }

        assertTrue(result.isSuccess());
        assertEquals(outputDir.getAbsolutePath(), result.getOutputPath().getAbsolutePath());
        assertEquals(1, result.getTotalLabels());

        File yoloFile = new File(outputDir, "labels_yolo/sample_001.txt");
        assertTrue(yoloFile.isFile());
        assertEquals("1 0.300000 0.400000 0.200000 0.200000", Files.readString(yoloFile.toPath()).trim());
        assertTrue(new File(outputDir, "classes.txt").isFile());
        assertTrue(new File(outputDir, "data.yaml").isFile());
        assertTrue(Files.readString(new File(outputDir, "classes.txt").toPath()).contains("confirmed_cancer"));
        assertTrue(Files.readString(new File(outputDir, "data.yaml").toPath()).contains("train: images"));
    }

    @Test
    public void exportsCocoImageDimensionsForEmptyReviewedImages() throws Exception {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/empty.png", 640, 480);
        store.setReviewed("/images/empty.png", true);
        File outputDir = temp.newFolder("empty-coco-export");

        ExportService.ExportResult result = ExportService.exportCoco(store, outputDir);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getTotalLabels());
        try (FileReader reader = new FileReader(new File(outputDir, "coco_annotations.json"))) {
            JsonObject image = JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonArray("images").get(0).getAsJsonObject();
            assertEquals("empty.png", image.get("file_name").getAsString());
            assertEquals(640, image.get("width").getAsInt());
            assertEquals(480, image.get("height").getAsInt());
        }
    }

    @Test
    public void exportsCocoFileUnderSelectedDirectory() throws Exception {
        AnnotationStore store = sampleStore();
        File outputDir = temp.newFolder("coco-export");

        ExportService.ExportResult result = ExportService.exportCoco(store, outputDir);

        assertTrue(result.isSuccess());
        assertEquals(new File(outputDir, "coco_annotations.json").getAbsolutePath(), result.getOutputPath().getAbsolutePath());
        assertEquals(1, result.getTotalLabels());

        try (FileReader reader = new FileReader(new File(outputDir, "coco_annotations.json"))) {
            JsonObject coco = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject image = coco.getAsJsonArray("images").get(0).getAsJsonObject();
            JsonObject annotation = coco.getAsJsonArray("annotations").get(0).getAsJsonObject();
            JsonObject category = coco.getAsJsonArray("categories").get(1).getAsJsonObject();

            assertEquals("sample_001.png", image.get("file_name").getAsString());
            assertEquals(1000, image.get("width").getAsInt());
            assertEquals(1000, image.get("height").getAsInt());
            assertEquals(1, annotation.get("image_id").getAsInt());
            assertEquals(1, annotation.get("category_id").getAsInt());
            assertEquals(200.0, annotation.getAsJsonArray("bbox").get(0).getAsDouble(), 0.000001);
            assertEquals(300.0, annotation.getAsJsonArray("bbox").get(1).getAsDouble(), 0.000001);
            assertEquals(200.0, annotation.getAsJsonArray("bbox").get(2).getAsDouble(), 0.000001);
            assertEquals(200.0, annotation.getAsJsonArray("bbox").get(3).getAsDouble(), 0.000001);
            assertEquals(40000, annotation.get("area").getAsInt());
            assertEquals("suspicious", category.get("name").getAsString());
        }
    }

    @Test
    public void exportsMaskJsonAndPngFiles() throws Exception {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/masked.png", 10, 10);
        store.addMask(new MaskAnnotation(
                "masked.png",
                LabelClass.CONFIRMED_CANCER,
                List.of(
                        new MaskPoint(0.1, 0.1),
                        new MaskPoint(0.8, 0.1),
                        new MaskPoint(0.8, 0.8),
                        new MaskPoint(0.1, 0.8)
                ),
                10,
                10
        ));
        File outputDir = temp.newFolder("mask-export");

        ExportService.ExportResult result = ExportService.exportMasks(store, outputDir);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalLabels());

        try (FileReader reader = new FileReader(new File(outputDir, "masks.json"))) {
            JsonObject mask = JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject();
            assertEquals("masked.png", mask.get("file").getAsString());
            assertEquals("confirmed_cancer", mask.get("label").getAsString());
            assertEquals(4, mask.getAsJsonArray("points").size());
            assertEquals(10, mask.get("image_width").getAsInt());
        }

        File png = new File(outputDir, "masks_png/masked_mask.png");
        assertTrue(png.isFile());
        assertTrue(ImageIO.read(png).getRaster().getSample(5, 5, 0) > 0);
    }

    @Test
    public void clampsJsonAndYoloExportsConsistently() throws Exception {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/out_of_bounds.png");
        store.addAnnotation(new Annotation(
                "out_of_bounds.png",
                LabelClass.NORMAL,
                -0.1, 0.9, 0.3, 0.3,
                -100, 900, 300, 300,
                1000, 1000
        ));
        File outputDir = temp.newFolder("clamped-export");

        ExportService.ExportResult jsonResult = ExportService.exportLabels(store, outputDir);
        ExportService.ExportResult yoloResult = ExportService.exportYolo(store, outputDir);
        ExportService.ExportResult cocoResult = ExportService.exportCoco(store, outputDir);

        assertTrue(jsonResult.isSuccess());
        assertTrue(yoloResult.isSuccess());
        assertTrue(cocoResult.isSuccess());
        assertEquals(1, jsonResult.getTotalLabels());
        assertEquals(1, yoloResult.getTotalLabels());
        assertEquals(1, cocoResult.getTotalLabels());

        try (FileReader reader = new FileReader(new File(outputDir, "labels.json"))) {
            JsonObject label = JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject();
            assertEquals(0.0, label.get("x").getAsDouble(), 0.000001);
            assertEquals(0.9, label.get("y").getAsDouble(), 0.000001);
            assertEquals(0.2, label.get("w").getAsDouble(), 0.000001);
            assertEquals(0.1, label.get("h").getAsDouble(), 0.000001);
            assertEquals(0, label.get("x_pixel").getAsInt());
            assertEquals(900, label.get("y_pixel").getAsInt());
            assertEquals(200, label.get("w_pixel").getAsInt());
            assertEquals(100, label.get("h_pixel").getAsInt());
        }

        File yoloFile = new File(outputDir, "labels_yolo/out_of_bounds.txt");
        assertEquals("0 0.100000 0.950000 0.200000 0.100000", Files.readString(yoloFile.toPath()).trim());

        try (FileReader reader = new FileReader(new File(outputDir, "coco_annotations.json"))) {
            JsonObject annotation = JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonArray("annotations").get(0).getAsJsonObject();
            assertEquals(0.0, annotation.getAsJsonArray("bbox").get(0).getAsDouble(), 0.000001);
            assertEquals(900.0, annotation.getAsJsonArray("bbox").get(1).getAsDouble(), 0.000001);
            assertEquals(200.0, annotation.getAsJsonArray("bbox").get(2).getAsDouble(), 0.000001);
            assertEquals(100.0, annotation.getAsJsonArray("bbox").get(3).getAsDouble(), 0.000001);
            assertEquals(20000, annotation.get("area").getAsInt());
        }
    }

    private AnnotationStore sampleStore() {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/sample_001.png", 1000, 1000);
        store.addAnnotation(new Annotation(
                "sample_001.png",
                LabelClass.SUSPICIOUS,
                0.2, 0.3, 0.2, 0.2,
                200, 300, 200, 200,
                1000, 1000
        ));
        return store;
    }
}
