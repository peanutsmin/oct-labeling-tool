import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
}