import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ProjectService {

    public static void saveProject(AnnotationStore store, String projectName) {
        store.saveCurrent();
        try {
            FileWriter fw = new FileWriter("project.json");
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            fw.write("{\n");
            fw.write("  \"project_name\": \"" + projectName + "\",\n");
            fw.write("  \"created_at\": \"" + now + "\",\n");
            fw.write("  \"images\": [\n");

            boolean firstImage = true;
            for (java.util.Map.Entry<String, ArrayList<Annotation>> entry : store.getAll().entrySet()) {
                if (!firstImage) fw.write(",\n");
                fw.write("    {\n");
                fw.write("      \"file\": \"" + entry.getKey() + "\",\n");
                fw.write("      \"annotations\": [\n");

                boolean firstAnn = true;
                for (Annotation ann : entry.getValue()) {
                    if (!firstAnn) fw.write(",\n");
                    fw.write("        " + ann.toJson());
                    firstAnn = false;
                }
                fw.write("\n      ]\n");
                fw.write("    }");
                firstImage = false;
            }

            fw.write("\n  ]\n}");
            fw.close();
            System.out.println("프로젝트 저장 완료! project.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadProject(AnnotationStore store, String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();

            String content = sb.toString();
            store.clear();

            String[] imageBlocks = content.split("\\{\\s*\"file\":");
            for (int i = 1; i < imageBlocks.length; i++) {
                String block = imageBlocks[i];
                String filePath = block.split("\"")[1];

                String[] annParts = block.split("\\{\"file\":\"" + filePath.replace("/", "\\/") + "\"");
                String annSection = block;

                String[] annBlocks = annSection.split("\\{\"file\":\"[^\"]+\",\"label\":");
                ArrayList<Annotation> anns = new ArrayList<>();

                for (int j = 1; j < annBlocks.length; j++) {
                    try {
                        String annBlock = "{\"file\":\"x\",\"label\":" + annBlocks[j];
                        annBlock = annBlock.split("\\}")[0] + "}";
                        String[] parts = annBlock.replace("{", "").replace("}", "").replace("\"", "").split(",");
                        String label = "";
                        double x = 0, y = 0, w = 0, h = 0;
                        int xp = 0, yp = 0, wp = 0, hp = 0, iw = 1, ih = 1;
                        for (String part : parts) {
                            String[] kv = part.split(":");
                            if (kv.length < 2) continue;
                            String k = kv[0].trim(), v = kv[1].trim();
                            switch (k) {
                                case "label" -> label = v;
                                case "x" -> x = Double.parseDouble(v);
                                case "y" -> y = Double.parseDouble(v);
                                case "w" -> w = Double.parseDouble(v);
                                case "h" -> h = Double.parseDouble(v);
                                case "x_pixel" -> xp = Integer.parseInt(v);
                                case "y_pixel" -> yp = Integer.parseInt(v);
                                case "w_pixel" -> wp = Integer.parseInt(v);
                                case "h_pixel" -> hp = Integer.parseInt(v);
                                case "image_width" -> iw = Integer.parseInt(v);
                                case "image_height" -> ih = Integer.parseInt(v);
                            }
                        }
                        if (!label.isEmpty()) {
                            anns.add(new Annotation(new File(filePath).getName(), label, x, y, w, h, xp, yp, wp, hp, iw, ih));
                        }
                    } catch (Exception ignored) {}
                }

                store.getAll().put(filePath, anns);
            }
            System.out.println("프로젝트 불러오기 완료!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}