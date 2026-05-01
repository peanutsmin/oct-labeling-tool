public enum LabelClass {
    NORMAL(0, "normal", "정상", "Normal", "Normal"),
    SUSPICIOUS(1, "suspicious", "의심", "Suspicious", "Verdächtig"),
    CONFIRMED_CANCER(2, "confirmed_cancer", "확실히 암", "Confirmed Cancer", "Bestätigter Krebs");

    private final int classId;
    private final String exportValue;
    private final String ko;
    private final String en;
    private final String de;

    LabelClass(int classId, String exportValue, String ko, String en, String de) {
        this.classId = classId;
        this.exportValue = exportValue;
        this.ko = ko;
        this.en = en;
        this.de = de;
    }

    public int classId() {
        return classId;
    }

    public String exportValue() {
        return exportValue;
    }

    public String display(I18n i18n) {
        return i18n.t(ko, en, de);
    }

    public static LabelClass fromDisplay(String value) {
        for (LabelClass label : values()) {
            if (label.ko.equals(value) || label.en.equals(value) || label.de.equals(value)) {
                return label;
            }
        }
        return SUSPICIOUS;
    }

    public static LabelClass fromStoredValue(String value) {
        for (LabelClass label : values()) {
            if (label.name().equals(value) || label.exportValue.equals(value)
                    || label.ko.equals(value) || label.en.equals(value) || label.de.equals(value)) {
                return label;
            }
        }
        return SUSPICIOUS;
    }
}
