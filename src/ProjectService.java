import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ProjectService {

    public static void saveProject(AnnotationStore store, String savePath, String projectName) {
        store.saveCurrent();
        try {
            FileWriter fw = new FileWriter("savePath");
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

            store.clear();
            String content = sb.toString();

            // file 경로 추출
            java.util.regex.Pattern filePattern = java.util.regex.Pattern.compile("\"file\":\\s*\"([^\"]+)\"");
            java.util.regex.Pattern labelPattern = java.util.regex.Pattern.compile("\"label\":\\s*\"([^\"]+)\"");
            java.util.regex.Pattern xPattern = java.util.regex.Pattern.compile("\"x\":\\s*([0-9.]+)");
            java.util.regex.Pattern yPattern = java.util.regex.Pattern.compile("\"y\":\\s*([0-9.]+)");
            java.util.regex.Pattern wPattern = java.util.regex.Pattern.compile("\"w\":\\s*([0-9.]+)");
            java.util.regex.Pattern hPattern = java.util.regex.Pattern.compile("\"h\":\\s*([0-9.]+)");
            java.util.regex.Pattern xpPattern = java.util.regex.Pattern.compile("\"x_pixel\":\\s*([0-9]+)");
            java.util.regex.Pattern ypPattern = java.util.regex.Pattern.compile("\"y_pixel\":\\s*([0-9]+)");
            java.util.regex.Pattern wpPattern = java.util.regex.Pattern.compile("\"w_pixel\":\\s*([0-9]+)");
            java.util.regex.Pattern hpPattern = java.util.regex.Pattern.compile("\"h_pixel\":\\s*([0-9]+)");
            java.util.regex.Pattern iwPattern = java.util.regex.Pattern.compile("\"image_width\":\\s*([0-9]+)");
            java.util.regex.Pattern ihPattern = java.util.regex.Pattern.compile("\"image_height\":\\s*([0-9]+)");

            // 이미지 블록별로 파싱
            String[] imageBlocks = content.split("\\{\\s*\\n\\s*\"file\":");
            for (int i = 1; i < imageBlocks.length; i++) {
                String block = "\"file\":" + imageBlocks[i];
                java.util.regex.Matcher fm = filePattern.matcher(block);
                if (!fm.find()) continue;
                String filePath = fm.group(1);

                ArrayList<Annotation> anns = new ArrayList<>();
                // annotation 블록 파싱
                String[] annParts = block.split("\\{\"file\":");
                for (int j = 1; j < annParts.length; j++) {
                    String ap = "{\"file\":" + annParts[j].split("\\}")[0] + "}";
                    try {
                        String label = extract(labelPattern, ap);
                        double x = Double.parseDouble(extract(xPattern, ap));
                        double y = Double.parseDouble(extract(yPattern, ap));
                        double w = Double.parseDouble(extract(wPattern, ap));
                        double h = Double.parseDouble(extract(hPattern, ap));
                        int xp = Integer.parseInt(extract(xpPattern, ap));
                        int yp = Integer.parseInt(extract(ypPattern, ap));
                        int wp = Integer.parseInt(extract(wpPattern, ap));
                        int hp = Integer.parseInt(extract(hpPattern, ap));
                        int iw = Integer.parseInt(extract(iwPattern, ap));
                        int ih = Integer.parseInt(extract(ihPattern, ap));
                        anns.add(new Annotation(new File(filePath).getName(), label, x, y, w, h, xp, yp, wp, hp, iw, ih));
                    } catch (Exception ignored) {}
                }
                store.getAll().put(filePath, anns);
            }
            System.out.println("프로젝트 불러오기 완료! 이미지 수: " + store.getAll().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extract(java.util.regex.Pattern p, String text) {
        java.util.regex.Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : "";
    }
}