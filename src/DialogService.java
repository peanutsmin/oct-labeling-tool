import javafx.scene.control.Alert;
import javafx.stage.Stage;

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

    private Alert createResultAlert(String title, boolean success) {
        Alert.AlertType type = success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        return alert;
    }
}
