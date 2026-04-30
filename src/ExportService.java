import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ExportService {

    public static void exportLabels(AnnotationStore store) {
        store.saveCurrent();
        try {
            FileWriter fw = new FileWriter("labels.json");
            fw.write("[\n");
            boolean first = true;
            for (ArrayList<Annotation> list : store.getAll().values()) {
                for (Annotation ann : list) {
                    if (!first) fw.write(",\n");
                    fw.write("  " + ann.toJson());
                    first = false;
                }
            }
            fw.write("\n]");
            fw.close();

            int n = 0, s = 0, c = 0;
            for (ArrayList<Annotation> list : store.getAll().values()) {
                for (Annotation ann : list) {
                    if (ann.label.equals("정상") || ann.label.equals("Normal")) n++;
                    else if (ann.label.equals("의심") || ann.label.equals("Suspicious") || ann.label.equals("Verdächtig")) s++;
                    else c++;
                }
            }

            FileWriter sw = new FileWriter("summary.json");
            sw.write("{\n  \"summary\": {\n");
            sw.write("    \"total_images\": " + store.getAll().size() + ",\n");
            sw.write("    \"total_labels\": " + store.totalCount() + ",\n");
            sw.write("    \"normal\": " + n + ",\n");
            sw.write("    \"suspicious\": " + s + ",\n");
            sw.write("    \"confirmed_cancer\": " + c + "\n");
            sw.write("  }\n}");
            sw.close();
            System.out.println("Saved! Total " + store.totalCount() + " labels");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void exportYolo(AnnotationStore store) {
        store.saveCurrent();
        File dir = new File("labels_yolo");
        dir.mkdirs();

        for (java.util.Map.Entry<String, ArrayList<Annotation>> entry : store.getAll().entrySet()) {
            String imagePath = entry.getKey();
            String imageName = new File(imagePath).getName();
            String baseName = imageName.replaceAll("\\.[^.]+$", "");

            try {
                FileWriter fw = new FileWriter("labels_yolo/" + baseName + ".txt");
                for (Annotation ann : entry.getValue()) {
                    int classId = labelToClassId(ann.label);
                    if (classId < 0) continue;
                    double x = clamp(ann.x);
                    double y = clamp(ann.y);
                    double w = clamp(ann.w);
                    double h = clamp(ann.h);
                    if (w <= 0 || h <= 0) continue;
                    if (x + w > 1.0) w = 1.0 - x;
                    if (y + h > 1.0) h = 1.0 - y;
                    double xCenter = x + w / 2.0;
                    double yCenter = y + h / 2.0;
                    fw.write(String.format("%d %.6f %.6f %.6f %.6f%n",
                            classId, xCenter, yCenter, w, h));
                }
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("YOLO export 완료! labels_yolo/ 폴더 확인");
    }

    private static int labelToClassId(String label) {
        return switch (label) {
            case "정상", "Normal" -> 0;
            case "의심", "Suspicious", "Verdächtig" -> 1;
            case "확실히 암", "Confirmed Cancer", "Bestätigter Krebs" -> 2;
            default -> -1;
        };
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}