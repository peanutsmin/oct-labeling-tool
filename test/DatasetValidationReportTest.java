import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DatasetValidationReportTest {
    @Test
    public void reportsDatasetQualityWarningsBeforeExport() {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/empty.png");
        store.saveCurrent();

        store.loadFor("/images/problematic.png");
        store.addAnnotation(new Annotation(
                "problematic.png",
                LabelClass.SUSPICIOUS,
                -0.1, 0.1, 0.2, 0.2,
                -10, 10, 20, 20,
                100, 100
        ));
        store.addAnnotation(new Annotation(
                "problematic.png",
                LabelClass.NORMAL,
                2.0, 2.0, 0.2, 0.2,
                200, 200, 20, 20,
                100, 100
        ));
        store.addAnnotation(new Annotation(
                "problematic.png",
                LabelClass.CONFIRMED_CANCER,
                0.5, 0.5, 0.02, 0.02,
                50, 50, 2, 2,
                100, 100
        ));
        store.setReviewed("/images/problematic.png", true);

        DatasetValidationReport report = DatasetValidationReport.fromStore(store);

        assertEquals(2, report.getTotalImages());
        assertEquals(1, report.getReviewedImages());
        assertEquals(1, report.getUnreviewedImages());
        assertEquals(1, report.getEmptyImages());
        assertEquals(3, report.getTotalLabels());
        assertEquals(2, report.getExportableLabels());
        assertEquals(1, report.getInvalidBoxes());
        assertEquals(1, report.getClippedBoxes());
        assertEquals(1, report.getTinyBoxes());
        assertTrue(report.hasWarnings());
        assertTrue(report.warningKeys().contains("unreviewed"));
        assertTrue(report.warningKeys().contains("empty"));
        assertTrue(report.warningKeys().contains("invalid"));
        assertTrue(report.warningKeys().contains("clipped"));
        assertTrue(report.warningKeys().contains("tiny"));
    }

    @Test
    public void reportsSkewedLabelDistributionForLargeImbalance() {
        AnnotationStore store = new AnnotationStore();
        store.loadFor("/images/skewed.png");
        for (int i = 0; i < 5; i++) {
            store.addAnnotation(new Annotation(
                    "skewed.png",
                    LabelClass.SUSPICIOUS,
                    0.1, 0.1, 0.2, 0.2,
                    10, 10, 20, 20,
                    100, 100
            ));
        }
        store.setReviewed("/images/skewed.png", true);

        DatasetValidationReport report = DatasetValidationReport.fromStore(store);

        assertEquals(5, report.getSuspiciousLabels());
        assertTrue(report.isLabelDistributionSkewed());
        assertTrue(report.warningKeys().contains("skewed"));
    }
}
