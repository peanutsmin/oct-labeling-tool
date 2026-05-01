import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
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
        assertEquals(new File(outputDir, "labels_yolo").getAbsolutePath(), result.getOutputPath().getAbsolutePath());
        assertEquals(1, result.getTotalLabels());

        File yoloFile = new File(outputDir, "labels_yolo/sample_001.txt");
        assertTrue(yoloFile.isFile());
        assertEquals("1 0.300000 0.400000 0.200000 0.200000", Files.readString(yoloFile.toPath()).trim());
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

        assertTrue(jsonResult.isSuccess());
        assertTrue(yoloResult.isSuccess());
        assertEquals(1, jsonResult.getTotalLabels());
        assertEquals(1, yoloResult.getTotalLabels());

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
    }

    private AnnotationStore sampleStore() {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/sample_001.png");
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
