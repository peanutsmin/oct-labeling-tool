import com.google.gson.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class ProjectService {

    public static ProjectResult saveProject(AnnotationStore store, String savePath, String projectName) {
        store.saveCurrent();
        try {
            JsonObject root = new JsonObject();
            root.addProperty("project_name", projectName);
            root.addProperty("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            JsonArray images = new JsonArray();
            for (java.util.Map.Entry<String, ArrayList<Annotation>> entry : store.getAll().entrySet()) {
                JsonObject imageObj = new JsonObject();
                imageObj.addProperty("file", entry.getKey());

                JsonArray annotations = new JsonArray();
                for (Annotation ann : entry.getValue()) {
                    JsonObject annObj = new JsonObject();
                    annObj.addProperty("label", ann.label.exportValue());
                    annObj.addProperty("x", ann.x);
                    annObj.addProperty("y", ann.y);
                    annObj.addProperty("w", ann.w);
                    annObj.addProperty("h", ann.h);
                    annObj.addProperty("x_pixel", ann.xPixel);
                    annObj.addProperty("y_pixel", ann.yPixel);
                    annObj.addProperty("w_pixel", ann.wPixel);
                    annObj.addProperty("h_pixel", ann.hPixel);
                    annObj.addProperty("image_width", ann.imageWidth);
                    annObj.addProperty("image_height", ann.imageHeight);
                    annotations.add(annObj);
                }
                imageObj.add("annotations", annotations);
                images.add(imageObj);
            }
            root.add("images", images);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter fw = new FileWriter(savePath)) {
                fw.write(gson.toJson(root));
            }
            return ProjectResult.success(new File(savePath), store.getAll().size(), store.totalCount());
        } catch (Exception e) {
            return ProjectResult.failure(new File(savePath), e);
        }
    }

    public static ProjectResult loadProject(AnnotationStore store, String path) {
        try {
            JsonObject root;
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                root = JsonParser.parseReader(br).getAsJsonObject();
            }

            HashMap<String, ArrayList<Annotation>> loaded = new HashMap<>();
            JsonArray images = root.getAsJsonArray("images");
            int labelCount = 0;
            for (JsonElement imageEl : images) {
                JsonObject imageObj = imageEl.getAsJsonObject();
                String filePath = imageObj.get("file").getAsString();

                ArrayList<Annotation> anns = new ArrayList<>();
                JsonArray annotations = imageObj.getAsJsonArray("annotations");
                for (JsonElement annEl : annotations) {
                    JsonObject annObj = annEl.getAsJsonObject();
                    LabelClass label = LabelClass.fromStoredValue(annObj.get("label").getAsString());
                    double x = annObj.get("x").getAsDouble();
                    double y = annObj.get("y").getAsDouble();
                    double w = annObj.get("w").getAsDouble();
                    double h = annObj.get("h").getAsDouble();
                    int xp = annObj.get("x_pixel").getAsInt();
                    int yp = annObj.get("y_pixel").getAsInt();
                    int wp = annObj.get("w_pixel").getAsInt();
                    int hp = annObj.get("h_pixel").getAsInt();
                    int iw = annObj.get("image_width").getAsInt();
                    int ih = annObj.get("image_height").getAsInt();
                    anns.add(new Annotation(new File(filePath).getName(), label, x, y, w, h, xp, yp, wp, hp, iw, ih));
                }
                labelCount += anns.size();
                loaded.put(filePath, anns);
            }

            store.clear();
            store.getAll().putAll(loaded);
            return ProjectResult.success(new File(path), loaded.size(), labelCount);
        } catch (Exception e) {
            return ProjectResult.failure(new File(path), e);
        }
    }

    public static class ProjectResult {
        private final boolean success;
        private final File projectFile;
        private final int imageCount;
        private final int labelCount;
        private final String errorMessage;

        private ProjectResult(boolean success, File projectFile, int imageCount, int labelCount, String errorMessage) {
            this.success = success;
            this.projectFile = projectFile;
            this.imageCount = imageCount;
            this.labelCount = labelCount;
            this.errorMessage = errorMessage;
        }

        public static ProjectResult success(File projectFile, int imageCount, int labelCount) {
            return new ProjectResult(true, projectFile, imageCount, labelCount, null);
        }

        public static ProjectResult failure(File projectFile, Exception error) {
            String message = error.getMessage();
            if (message == null || message.isBlank()) {
                message = error.getClass().getSimpleName();
            }
            return new ProjectResult(false, projectFile, 0, 0, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public File getProjectFile() {
            return projectFile;
        }

        public int getImageCount() {
            return imageCount;
        }

        public int getLabelCount() {
            return labelCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
