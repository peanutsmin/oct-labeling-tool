package com.peanutsmin.octlabeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AnnotationStore {
    private HashMap<String, ArrayList<Annotation>> store = new HashMap<>();
    private HashMap<String, ArrayList<MaskAnnotation>> masks = new HashMap<>();
    private HashMap<String, ImageMetadata> images = new HashMap<>();
    private HashSet<String> reviewedImages = new HashSet<>();
    private ArrayList<Annotation> current = new ArrayList<>();
    private ArrayList<MaskAnnotation> currentMasks = new ArrayList<>();
    private String currentPath = "";

    public void setCurrentPath(String path) {
        this.currentPath = path;
    }

    public ArrayList<Annotation> getCurrent() {
        return current;
    }

    public ArrayList<MaskAnnotation> getCurrentMasks() {
        return currentMasks;
    }

    public void addAnnotation(Annotation ann) {
        current.add(ann);
    }

    public void addMask(MaskAnnotation mask) {
        currentMasks.add(mask);
    }

    public void removeAnnotation(int index) {
        current.remove(index);
    }

    public void removeMask(int index) {
        currentMasks.remove(index);
    }

    public void saveCurrent() {
        if (!currentPath.isEmpty()) {
            registerImage(currentPath, inferCurrentWidth(), inferCurrentHeight());
            store.put(currentPath, new ArrayList<>(current));
            masks.put(currentPath, new ArrayList<>(currentMasks));
        }
    }

    public void loadFor(String path) {
        loadFor(path, 0, 0);
    }

    public void loadFor(String path, int imageWidth, int imageHeight) {
        current.clear();
        currentMasks.clear();
        currentPath = path;
        registerImage(path, imageWidth, imageHeight);
        if (store.containsKey(path)) {
            current.addAll(store.get(path));
        }
        if (masks.containsKey(path)) {
            currentMasks.addAll(masks.get(path));
        }
    }

    public void registerImage(String path, int imageWidth, int imageHeight) {
        if (path == null || path.isBlank()) {
            return;
        }
        ImageMetadata existing = images.get(path);
        if (existing == null || existing.width() == 0 || existing.height() == 0) {
            images.put(path, new ImageMetadata(path, imageWidth, imageHeight));
        }
        store.putIfAbsent(path, new ArrayList<>());
        masks.putIfAbsent(path, new ArrayList<>());
    }

    public void clear() {
        store.clear();
        masks.clear();
        images.clear();
        reviewedImages.clear();
        current.clear();
        currentMasks.clear();
        currentPath = "";
    }

    public HashMap<String, ArrayList<Annotation>> getAll() {
        return store;
    }

    public HashMap<String, ArrayList<MaskAnnotation>> getAllMasks() {
        return masks;
    }

    public Map<String, ImageMetadata> getImages() {
        return images;
    }

    public ImageMetadata getImageMetadata(String path) {
        ImageMetadata metadata = images.get(path);
        if (metadata != null) {
            return metadata;
        }
        ArrayList<Annotation> annotations = store.get(path);
        if (annotations != null && !annotations.isEmpty()) {
            Annotation ann = annotations.get(0);
            metadata = new ImageMetadata(path, ann.imageWidth, ann.imageHeight);
            images.put(path, metadata);
            return metadata;
        }
        metadata = new ImageMetadata(path, 0, 0);
        images.put(path, metadata);
        return metadata;
    }

    public void setReviewed(String path, boolean reviewed) {
        if (path == null || path.isBlank()) {
            return;
        }
        if (reviewed) {
            reviewedImages.add(path);
            registerImage(path, 0, 0);
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

    public int totalMaskCount() {
        return masks.values().stream().mapToInt(List::size).sum();
    }

    private int inferCurrentWidth() {
        if (!current.isEmpty()) {
            return current.get(0).imageWidth;
        }
        return currentMasks.isEmpty() ? 0 : currentMasks.get(0).imageWidth;
    }

    private int inferCurrentHeight() {
        if (!current.isEmpty()) {
            return current.get(0).imageHeight;
        }
        return currentMasks.isEmpty() ? 0 : currentMasks.get(0).imageHeight;
    }
}
