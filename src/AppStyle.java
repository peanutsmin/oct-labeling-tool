import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class AppStyle {
    public static void applyRoot(BorderPane root) {
        root.setStyle("-fx-background-color: #f4f6fb;");
    }

    public static void applyCanvas(ImageCanvas canvas) {
        canvas.setStyle(
                "-fx-background-color: #111827;" +
                        "-fx-border-color: #d7dde8;" +
                        "-fx-border-width: 1 0 1 0;"
        );
    }

    public static void applyToolbar(HBox toolbar) {
        toolbar.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-padding: 8 10;" +
                        "-fx-spacing: 8;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 0 0 1 0;"
        );
    }

    public static void applyStatusBar(HBox statusBar) {
        statusBar.setStyle(
                "-fx-background-color: #f8fafc;" +
                        "-fx-padding: 6 10;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 0 0 1 0;"
        );
    }

    public static void applyButton(Button button) {
        button.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 10;" +
                        "-fx-text-fill: #1f2937;"
        );
    }

    public static void applyPrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: #2563eb;" +
                        "-fx-border-color: #1d4ed8;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 10;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );
    }

    public static void applyComboBox(ComboBox<?> comboBox) {
        comboBox.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;"
        );
    }

    public static void applyStatusLabel(Label label) {
        label.setStyle("-fx-text-fill: #334155; -fx-font-weight: bold;");
    }
}
