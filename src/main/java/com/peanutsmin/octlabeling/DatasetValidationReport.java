package com.peanutsmin.octlabeling;

import java.util.ArrayList;
import java.util.List;

public class DatasetValidationReport {
    public static final int MIN_BOX_SIZE_PX = 5;

    private int totalImages;
    private int reviewedImages;
    private int emptyImages;
    private int totalLabels;
    private int exportableLabels;
    private int invalidBoxes;
    private int clippedBoxes;
    private int tinyBoxes;
    private int normalLabels;
    private int suspiciousLabels;
    private int cancerLabels;

    public static DatasetValidationReport fromStore(AnnotationStore store) {
        store.saveCurrent();
        DatasetValidationReport report = new DatasetValidationReport();
        report.totalImages = store.getAll().size();
        report.reviewedImages = store.reviewedCount();

        for (var entry : store.getAll().entrySet()) {
            ArrayList<Annotation> annotations = entry.getValue();
            if (annotations.isEmpty()) {
                report.emptyImages++;
            }

            for (Annotation ann : annotations) {
                report.totalLabels++;
                report.countLabel(ann.label);

                NormalizedBox box = NormalizedBox.fromAnnotation(ann);
                if (!box.isValid()) {
                    report.invalidBoxes++;
                    continue;
                }

                report.exportableLabels++;
                if (isClipped(ann, box)) {
                    report.clippedBoxes++;
                }
                if (box.pixelW(ann.imageWidth) < MIN_BOX_SIZE_PX || box.pixelH(ann.imageHeight) < MIN_BOX_SIZE_PX) {
                    report.tinyBoxes++;
                }
            }
        }
        return report;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public int getReviewedImages() {
        return reviewedImages;
    }

    public int getUnreviewedImages() {
        return totalImages - reviewedImages;
    }

    public int getEmptyImages() {
        return emptyImages;
    }

    public int getTotalLabels() {
        return totalLabels;
    }

    public int getExportableLabels() {
        return exportableLabels;
    }

    public int getInvalidBoxes() {
        return invalidBoxes;
    }

    public int getClippedBoxes() {
        return clippedBoxes;
    }

    public int getTinyBoxes() {
        return tinyBoxes;
    }

    public int getNormalLabels() {
        return normalLabels;
    }

    public int getSuspiciousLabels() {
        return suspiciousLabels;
    }

    public int getCancerLabels() {
        return cancerLabels;
    }

    public boolean hasWarnings() {
        return getUnreviewedImages() > 0 || emptyImages > 0 || invalidBoxes > 0 || clippedBoxes > 0
                || tinyBoxes > 0 || isLabelDistributionSkewed();
    }

    public boolean isLabelDistributionSkewed() {
        if (totalLabels < 5) {
            return false;
        }
        int max = Math.max(normalLabels, Math.max(suspiciousLabels, cancerLabels));
        return max >= Math.ceil(totalLabels * 0.8);
    }

    public List<String> warningKeys() {
        ArrayList<String> warnings = new ArrayList<>();
        if (getUnreviewedImages() > 0) warnings.add("unreviewed");
        if (emptyImages > 0) warnings.add("empty");
        if (invalidBoxes > 0) warnings.add("invalid");
        if (clippedBoxes > 0) warnings.add("clipped");
        if (tinyBoxes > 0) warnings.add("tiny");
        if (isLabelDistributionSkewed()) warnings.add("skewed");
        return warnings;
    }

    private void countLabel(LabelClass label) {
        switch (label) {
            case NORMAL -> normalLabels++;
            case SUSPICIOUS -> suspiciousLabels++;
            case CONFIRMED_CANCER -> cancerLabels++;
        }
    }

    private static boolean isClipped(Annotation ann, NormalizedBox box) {
        return differs(ann.x, box.x)
                || differs(ann.y, box.y)
                || differs(Math.abs(ann.w), box.w)
                || differs(Math.abs(ann.h), box.h);
    }

    private static boolean differs(double a, double b) {
        return Math.abs(a - b) > 0.000001;
    }
}
