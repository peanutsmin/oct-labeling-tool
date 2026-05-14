package com.peanutsmin.octlabeling;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/** Builds the top toolbar area for MainApp. Extracted from MainApp.buildUI(). */
public class ToolbarBuilder {

    public interface Callbacks {
        void onImagesLoaded(List<File> files);
        void onPrev();
        void onNext();
        void onReviewedChanged(boolean reviewed);
        void onApplyLabel();
        void onZoomIn();
        void onZoomOut();
        void onZoomReset();
        void onBrightnessChanged(double value);
        void onContrastChanged(double value);
        void onResetAdjustments();
        void onLanguageChanged(String lang);
        void onSaveProject();
        void onLoadProject();
        void onExportJson();
        void onExportYolo();
        void onExportCoco();
        void onValidate();
    }

    private final I18n i18n;
    private final Stage stage;
    private final Callbacks cb;
    private final ImageCanvas canvas;

    public final ComboBox<String> labelBox;
    public final CheckBox reviewedCheckBox;
    public final Label fileLabel;
    public final Label progressLabel;
    public final Label overviewLabel;
    public final ToggleButton boxModeBtn;
    public final ToggleButton maskModeBtn;
    public final Button zoomResetBtn;
    public final Slider brightnessSlider;
    public final Slider contrastSlider;

    public ToolbarBuilder(I18n i18n, Stage stage, ImageCanvas canvas, Callbacks cb) {
        this.i18n = i18n;
        this.stage = stage;
        this.canvas = canvas;
        this.cb = cb;

        labelBox = new ComboBox<>();
        labelBox.getItems().addAll(
            i18n.t("\uC815\uC0C1", "Normal", "Normal"),
            i18n.t("\uC758\uC2EC", "Suspicious", "Verd\u00E4chtig"),
            i18n.t("\uD655\uC2E4\uD788 \uC554", "Confirmed Cancer", "Best\u00E4tigter Krebs")
        );
        labelBox.getSelectionModel().selectFirst();

        fileLabel = new Label();
        progressLabel = new Label();
        overviewLabel = new Label();

        reviewedCheckBox = new CheckBox(
            i18n.t("\uAC80\uC218 \uC644\uB8CC", "Reviewed", "Gepr\u00FCft"));
        reviewedCheckBox.setOnAction(e -> cb.onReviewedChanged(reviewedCheckBox.isSelected()));

        boxModeBtn = new ToggleButton(i18n.t("Box", "Box", "Box"));
        maskModeBtn = new ToggleButton(i18n.t("Mask", "Mask", "Maske"));
        ToggleGroup modeGroup = new ToggleGroup();
        boxModeBtn.setToggleGroup(modeGroup);
        maskModeBtn.setToggleGroup(modeGroup);
        boxModeBtn.setSelected(true);

        zoomResetBtn = new Button("100%");
        zoomResetBtn.setOnAction(e -> cb.onZoomReset());

        brightnessSlider = createAdjustmentSlider(canvas.getBrightness());
        brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            cb.onBrightnessChanged(newVal.doubleValue()));

        contrastSlider = createAdjustmentSlider(canvas.getContrast());
        contrastSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            cb.onContrastChanged(newVal.doubleValue()));

        AppStyle.applyComboBox(labelBox);
        AppStyle.applySlider(brightnessSlider);
        AppStyle.applySlider(contrastSlider);
    }

    public VBox build() {
        HBox langBar = buildLangBar();
        HBox toolbar1 = buildToolbar1();
        HBox toolbar2 = buildToolbar2();
        HBox toolbar3 = buildToolbar3();
        HBox statusBar = buildStatusBar();
        VBox top = new VBox(langBar, toolbar1, toolbar2, toolbar3, statusBar);
        AppStyle.applyTop(top);
        return top;
    }

    private HBox buildLangBar() {
        Button btnKo = new Button("\uD55C\uAD6D\uC5B4");
        Button btnEn = new Button("English");
        Button btnDe = new Button("Deutsch");
        btnKo.setOnAction(e -> cb.onLanguageChanged("ko"));
        btnEn.setOnAction(e -> cb.onLanguageChanged("en"));
        btnDe.setOnAction(e -> cb.onLanguageChanged("de"));
        HBox bar = new HBox(6, btnKo, btnEn, btnDe);
        AppStyle.applyToolbar(bar);
        return bar;
    }

    private HBox buildToolbar1() {
        Button openBtn = new Button(
            i18n.t("\uC774\uBBF8\uC9C0 \uC5F4\uAE30", "Open Images", "Bilder \u00F6ffnen"));
        openBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                i18n.t("\uC774\uBBF8\uC9C0 \uD30C\uC77C", "Image files", "Bilddateien"),
                "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.dcm", "*.dicom", "*.ima"));
            List<File> files = fc.showOpenMultipleDialog(stage);
            if (files != null) cb.onImagesLoaded(files);
        });
        Button prevBtn = new Button(i18n.t("< \uC774\uC804", "< Prev", "< Zur\u00FCck"));
        prevBtn.setOnAction(e -> cb.onPrev());
        Button nextBtn = new Button(i18n.t("\uB2E4\uC74C >", "Next >", "Weiter >"));
        nextBtn.setOnAction(e -> cb.onNext());
        Button applyBtn = new Button(
            i18n.t("\uB77C\uBCA8 \uC801\uC6A9", "Apply Label", "Label anwenden"));
        applyBtn.setOnAction(e -> cb.onApplyLabel());
        HBox bar = new HBox(8, openBtn, prevBtn, fileLabel, nextBtn,
            reviewedCheckBox, applyBtn, labelBox);
        AppStyle.applyToolbar(bar);
        return bar;
    }

    private HBox buildToolbar2() {
        Button saveBtn = new Button(i18n.t("\uC800\uC7A5", "Save", "Speichern"));
        saveBtn.setOnAction(e -> cb.onSaveProject());
        Button loadBtn = new Button(i18n.t("\uBD88\uB7EC\uC624\uAE30", "Load", "Laden"));
        loadBtn.setOnAction(e -> cb.onLoadProject());
        Button validateBtn = new Button(i18n.t("\uAC80\uC99D", "Validate", "Validieren"));
        validateBtn.setOnAction(e -> cb.onValidate());
        Button exportJsonBtn = new Button("JSON Export");
        exportJsonBtn.setOnAction(e -> cb.onExportJson());
        Button exportYoloBtn = new Button("YOLO Export");
        exportYoloBtn.setOnAction(e -> cb.onExportYolo());
        Button exportCocoBtn = new Button("COCO Export");
        exportCocoBtn.setOnAction(e -> cb.onExportCoco());
        HBox bar = new HBox(8, saveBtn, loadBtn, validateBtn,
            exportJsonBtn, exportYoloBtn, exportCocoBtn);
        AppStyle.applyToolbar(bar);
        return bar;
    }

    private HBox buildToolbar3() {
        Button zoomOutBtn = new Button("-");
        zoomOutBtn.setOnAction(e -> cb.onZoomOut());
        Button zoomInBtn = new Button("+");
        zoomInBtn.setOnAction(e -> cb.onZoomIn());
        Button resetBtn = new Button(
            i18n.t("\uCD08\uAE30\uD654", "Reset", "Zur\u00FCcksetzen"));
        resetBtn.setOnAction(e -> cb.onResetAdjustments());
        Label brightnessLbl = new Label(
            i18n.t("\uBC1D\uAE30", "Brightness", "Helligkeit") + ":");
        Label contrastLbl = new Label(
            i18n.t("\uB300\uBE44", "Contrast", "Kontrast") + ":");
        HBox bar = new HBox(8, boxModeBtn, maskModeBtn,
            zoomOutBtn, zoomResetBtn, zoomInBtn,
            brightnessLbl, brightnessSlider,
            contrastLbl, contrastSlider, resetBtn);
        AppStyle.applyToolbar(bar);
        return bar;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(20, progressLabel, overviewLabel);
        AppStyle.applyStatusBar(bar);
        return bar;
    }

    private Slider createAdjustmentSlider(double initialValue) {
        Slider s = new Slider(-1.0, 1.0, initialValue);
        s.setPrefWidth(80);
        return s;
    }
}
