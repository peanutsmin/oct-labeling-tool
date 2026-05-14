package com.peanutsmin.octlabeling;

import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProjectServiceTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void savesAndLoadsProjectWithCanonicalLabels() throws Exception {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/sample_001.png");
        store.addAnnotation(new Annotation(
                "sample_001.png",
                LabelClass.CONFIRMED_CANCER,
                0.1, 0.2, 0.3, 0.4,
                100, 200, 300, 400,
                1000, 1000
        ));
        store.addMask(new MaskAnnotation(
                "sample_001.png",
                LabelClass.SUSPICIOUS,
                List.of(
                        new MaskPoint(0.1, 0.1),
                        new MaskPoint(0.2, 0.1),
                        new MaskPoint(0.2, 0.2)
                ),
                1000,
                1000
        ));
        store.setReviewed("/images/sample_001.png", true);
        File projectFile = new File(temp.getRoot(), "project.json");

        ProjectService.ProjectResult saveResult = ProjectService.saveProject(
                store,
                projectFile.getAbsolutePath(),
                "test_project"
        );

        assertTrue(saveResult.isSuccess());
        assertEquals(1, saveResult.getImageCount());
        assertEquals(2, saveResult.getLabelCount());

        try (FileReader reader = new FileReader(projectFile)) {
            var image = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("images")
                    .get(0)
                    .getAsJsonObject();
            assertTrue(image.get("reviewed").getAsBoolean());
            assertEquals("sample_001.png", image.get("file_name").getAsString());
            assertEquals(1000, image.get("image_width").getAsInt());
            assertEquals(1000, image.get("image_height").getAsInt());
            assertEquals(1, image.getAsJsonArray("masks").size());

            String label = image
                    .getAsJsonArray("annotations")
                    .get(0)
                    .getAsJsonObject()
                    .get("label")
                    .getAsString();
            assertEquals("confirmed_cancer", label);
        }

        AnnotationStore loadedStore = new AnnotationStore();
        ProjectService.ProjectResult loadResult = ProjectService.loadProject(
                loadedStore,
                projectFile.getAbsolutePath()
        );

        assertTrue(loadResult.isSuccess());
        assertEquals(1, loadResult.getImageCount());
        assertEquals(2, loadResult.getLabelCount());
        assertEquals(LabelClass.CONFIRMED_CANCER,
                loadedStore.getAll().get("/images/sample_001.png").get(0).label);
        assertTrue(loadedStore.isReviewed("/images/sample_001.png"));
        assertEquals(1000, loadedStore.getImageMetadata("/images/sample_001.png").width());
        assertEquals(1000, loadedStore.getImageMetadata("/images/sample_001.png").height());
        assertEquals(1, loadedStore.getAllMasks().get("/images/sample_001.png").size());
        assertEquals(LabelClass.SUSPICIOUS,
                loadedStore.getAllMasks().get("/images/sample_001.png").get(0).label);
    }
}
