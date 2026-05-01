import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LabelClassTest {

    @Test
    public void mapsDisplayLabelsAcrossLanguages() {
        assertEquals(LabelClass.NORMAL, LabelClass.fromDisplay("정상"));
        assertEquals(LabelClass.SUSPICIOUS, LabelClass.fromDisplay("Suspicious"));
        assertEquals(LabelClass.CONFIRMED_CANCER, LabelClass.fromDisplay("Bestätigter Krebs"));
    }

    @Test
    public void mapsStoredLabelsFromCanonicalAndLegacyValues() {
        assertEquals(LabelClass.NORMAL, LabelClass.fromStoredValue("normal"));
        assertEquals(LabelClass.SUSPICIOUS, LabelClass.fromStoredValue("Verdächtig"));
        assertEquals(LabelClass.CONFIRMED_CANCER, LabelClass.fromStoredValue("CONFIRMED_CANCER"));
    }

    @Test
    public void exposesStableExportValuesAndClassIds() {
        assertEquals("normal", LabelClass.NORMAL.exportValue());
        assertEquals(1, LabelClass.SUSPICIOUS.classId());
        assertEquals("confirmed_cancer", LabelClass.CONFIRMED_CANCER.exportValue());
    }
}
