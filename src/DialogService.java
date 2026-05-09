import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

public class DialogService {
    private final Stage owner;
    private final I18n i18n;

    public DialogService(Stage owner, I18n i18n) {
        this.owner = owner;
        this.i18n = i18n;
    }

    public void showExportResult(String exportName, ExportService.ExportResult result) {
        Alert alert = createResultAlert(exportName, result.isSuccess());
        if (result.isSuccess()) {
            alert.setHeaderText(i18n.t("내보내기 완료", "Export complete", "Export abgeschlossen"));
            alert.setContentText(
                    i18n.t("저장 위치: ", "Saved to: ", "Gespeichert unter: ")
                            + result.getOutputPath().getAbsolutePath()
                            + "\n"
                            + i18n.t("라벨 수: ", "Labels: ", "Labels: ")
                            + result.getTotalLabels()
            );
        } else {
            alert.setHeaderText(i18n.t("내보내기 실패", "Export failed", "Export fehlgeschlagen"));
            alert.setContentText(result.getErrorMessage());
        }
        alert.showAndWait();
    }

    public void showProjectResult(String actionName, ProjectService.ProjectResult result) {
        Alert alert = createResultAlert(actionName, result.isSuccess());
        if (result.isSuccess()) {
            alert.setHeaderText(i18n.t("프로젝트 작업 완료", "Project action complete", "Projektaktion abgeschlossen"));
            alert.setContentText(
                    i18n.t("파일: ", "File: ", "Datei: ")
                            + result.getProjectFile().getAbsolutePath()
                            + "\n"
                            + i18n.t("이미지 수: ", "Images: ", "Bilder: ")
                            + result.getImageCount()
                            + "\n"
                            + i18n.t("라벨 수: ", "Labels: ", "Labels: ")
                            + result.getLabelCount()
            );
        } else {
            alert.setHeaderText(i18n.t("프로젝트 작업 실패", "Project action failed", "Projektaktion fehlgeschlagen"));
            alert.setContentText(result.getErrorMessage());
        }
        alert.showAndWait();
    }

    public boolean confirmValidationReport(String exportName, DatasetValidationReport report) {
        Alert alert = new Alert(report.hasWarnings() ? Alert.AlertType.CONFIRMATION : Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle(i18n.t("내보내기 전 검증", "Pre-export validation", "Validierung vor dem Export"));
        alert.setHeaderText(report.hasWarnings()
                ? i18n.t("확인할 항목이 있습니다.", "Some items need review.", "Einige Punkte sollten geprüft werden.")
                : i18n.t("검증을 통과했습니다.", "Validation passed.", "Validierung bestanden."));
        alert.setContentText(formatValidationReport(exportName, report));
        alert.setResizable(true);

        if (!report.hasWarnings()) {
            alert.showAndWait();
            return true;
        }

        ButtonType continueButton = new ButtonType(i18n.t("계속 내보내기", "Continue export", "Export fortsetzen"));
        ButtonType cancelButton = new ButtonType(i18n.t("취소", "Cancel", "Abbrechen"));
        alert.getButtonTypes().setAll(continueButton, cancelButton);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == continueButton;
    }

    private Alert createResultAlert(String title, boolean success) {
        Alert.AlertType type = success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        return alert;
    }

    private String formatValidationReport(String exportName, DatasetValidationReport report) {
        StringBuilder text = new StringBuilder();
        text.append(exportName).append("\n\n");
        text.append(i18n.t("이미지: ", "Images: ", "Bilder: "))
                .append(report.getTotalImages())
                .append("  ")
                .append(i18n.t("검수 완료: ", "Reviewed: ", "Geprüft: "))
                .append(report.getReviewedImages())
                .append("  ")
                .append(i18n.t("미검수: ", "Unreviewed: ", "Ungeprüft: "))
                .append(report.getUnreviewedImages())
                .append("\n");
        text.append(i18n.t("라벨: ", "Labels: ", "Labels: "))
                .append(report.getTotalLabels())
                .append("  ")
                .append(i18n.t("내보내기 가능: ", "Exportable: ", "Exportierbar: "))
                .append(report.getExportableLabels())
                .append("\n");
        text.append(i18n.t("정상: ", "Normal: ", "Normal: "))
                .append(report.getNormalLabels())
                .append("  ")
                .append(i18n.t("의심: ", "Suspicious: ", "Verdächtig: "))
                .append(report.getSuspiciousLabels())
                .append("  ")
                .append(i18n.t("암: ", "Cancer: ", "Krebs: "))
                .append(report.getCancerLabels())
                .append("\n\n");

        if (!report.hasWarnings()) {
            text.append(i18n.t(
                    "경고 없이 내보낼 수 있습니다.",
                    "No warnings found. Export can continue.",
                    "Keine Warnungen gefunden. Der Export kann fortgesetzt werden."
            ));
            return text.toString();
        }

        text.append(i18n.t("경고:", "Warnings:", "Warnungen:")).append("\n");
        if (report.getUnreviewedImages() > 0) {
            text.append("- ").append(i18n.t("미검수 이미지: ", "Unreviewed images: ", "Ungeprüfte Bilder: "))
                    .append(report.getUnreviewedImages()).append("\n");
        }
        if (report.getEmptyImages() > 0) {
            text.append("- ").append(i18n.t("annotation 없는 이미지: ", "Images without annotations: ", "Bilder ohne Annotationen: "))
                    .append(report.getEmptyImages()).append("\n");
        }
        if (report.getInvalidBoxes() > 0) {
            text.append("- ").append(i18n.t("내보낼 수 없는 박스: ", "Non-exportable boxes: ", "Nicht exportierbare Boxen: "))
                    .append(report.getInvalidBoxes()).append("\n");
        }
        if (report.getClippedBoxes() > 0) {
            text.append("- ").append(i18n.t("이미지 경계에 맞춰 잘리는 박스: ", "Boxes clipped to image bounds: ", "Auf Bildgrenzen zugeschnittene Boxen: "))
                    .append(report.getClippedBoxes()).append("\n");
        }
        if (report.getTinyBoxes() > 0) {
            text.append("- ").append(i18n.t("5x5 픽셀보다 작은 박스: ", "Boxes smaller than 5x5 pixels: ", "Boxen kleiner als 5x5 Pixel: "))
                    .append(report.getTinyBoxes()).append("\n");
        }
        if (report.isLabelDistributionSkewed()) {
            text.append("- ").append(i18n.t(
                    "라벨 분포가 한 클래스에 크게 치우쳐 있습니다.",
                    "Label distribution is heavily skewed toward one class.",
                    "Die Label-Verteilung ist stark auf eine Klasse konzentriert."
            )).append("\n");
        }
        return text.toString();
    }
}
