import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AnnotationStore {
    private HashMap<String, ArrayList<Annotation>> store = new HashMap<>();
    private HashSet<String> reviewedImages = new HashSet<>();
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
        reviewedImages.clear();
        current.clear();
        currentPath = "";
    }

    public HashMap<String, ArrayList<Annotation>> getAll() {
        return store;
    }

    public void setReviewed(String path, boolean reviewed) {
        if (path == null || path.isBlank()) {
            return;
        }
        if (reviewed) {
            reviewedImages.add(path);
            store.putIfAbsent(path, new ArrayList<>());
        } else {
            reviewedImages.remove(path);
        }
    }

    public boolean isReviewed(String path) {
        return reviewedImages.contains(path);
    }

    public int reviewedCount() {
        int count = 0;
        for (String path : store.keySet()) {
            if (reviewedImages.contains(path)) {
                count++;
            }
        }
        return count;
    }

    public int totalCount() {
        return store.values().stream().mapToInt(List::size).sum();
    }
}
