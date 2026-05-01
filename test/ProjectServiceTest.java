import com.google.gson.JsonParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;

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
        File projectFile = new File(temp.getRoot(), "project.json");

        ProjectService.ProjectResult saveResult = ProjectService.saveProject(
                store,
                projectFile.getAbsolutePath(),
                "test_project"
        );

        assertTrue(saveResult.isSuccess());
        assertEquals(1, saveResult.getImageCount());
        assertEquals(1, saveResult.getLabelCount());

        try (FileReader reader = new FileReader(projectFile)) {
            String label = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("images")
                    .get(0)
                    .getAsJsonObject()
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
        assertEquals(1, loadResult.getLabelCount());
        assertEquals(LabelClass.CONFIRMED_CANCER,
                loadedStore.getAll().get("/images/sample_001.png").get(0).label);
    }
}
