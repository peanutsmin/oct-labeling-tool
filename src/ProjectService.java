import com.google.gson.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ProjectService {

    public static void saveProject(AnnotationStore store, String savePath, String projectName) {
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
                    annObj.addProperty("label", ann.label);
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
            FileWriter fw = new FileWriter(savePath);
            fw.write(gson.toJson(root));
            fw.close();
            System.out.println("프로젝트 저장 완료! " + savePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadProject(AnnotationStore store, String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            JsonObject root = JsonParser.parseReader(br).getAsJsonObject();
            br.close();

            store.clear();
            JsonArray images = root.getAsJsonArray("images");
            for (JsonElement imageEl : images) {
                JsonObject imageObj = imageEl.getAsJsonObject();
                String filePath = imageObj.get("file").getAsString();

                ArrayList<Annotation> anns = new ArrayList<>();
                JsonArray annotations = imageObj.getAsJsonArray("annotations");
                for (JsonElement annEl : annotations) {
                    JsonObject annObj = annEl.getAsJsonObject();
                    String label = annObj.get("label").getAsString();
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
                store.getAll().put(filePath, anns);
            }
            System.out.println("프로젝트 불러오기 완료! 이미지 수: " + store.getAll().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}