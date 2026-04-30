import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnnotationStore {
    private HashMap<String, ArrayList<Annotation>> store = new HashMap<>();
    private ArrayList<Annotation> current = new ArrayList<>();
    private String currentPath = "";

    public void setCurrentPath(String path) {
        this.currentPath = path;
    }

    public ArrayList<Annotation> getCurrent() {
        return current;
    }

    public void addAnnotation(Annotation ann) {
        current.add(ann);
    }

    public void removeAnnotation(int index) {
        current.remove(index);
    }

    public void saveCurrent() {
        if (!currentPath.isEmpty()) {
            store.put(currentPath, new ArrayList<>(current));
        }
    }

    public void loadFor(String path) {
        current.clear();
        currentPath = path;
        if (store.containsKey(path)) {
            current.addAll(store.get(path));
        }
    }

    public void clear() {
        store.clear();
        current.clear();
        currentPath = "";
    }

    public HashMap<String, ArrayList<Annotation>> getAll() {
        return store;
    }

    public int totalCount() {
        return store.values().stream().mapToInt(List::size).sum();
    }
}