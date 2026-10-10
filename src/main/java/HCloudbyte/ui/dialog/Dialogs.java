package HCloudbyte.ui.dialog;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.function.Consumer;

/**
 * 对话框中转：统一的错误 / 成功提示，以及「多字段表单」弹窗。
 * 所有需要弹窗的地方都走这里，避免各处重复拼 Alert / Dialog。
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** 统一错误弹窗。 */
    public static void error(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "未知错误" : message);
        alert.showAndWait();
    }

    /** 统一成功提示弹窗。 */
    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "" : message);
        alert.showAndWait();
    }

    /**
     * 多字段表单弹窗：点确定时执行 {@code onConfirm}；若它抛异常，则错误内联显示且窗口不关。
     * 校验与业务逻辑全写在 {@code onConfirm} 里，调用方保持简短。
     *
     * @param labels   字段标签
     * @param initials 字段初值（可为 null / 元素可 null）
     */
    public static void form(String title, String header, String[] labels, String[] initials,
                            Consumer<String[]> onConfirm) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        TextField[] fields = new TextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = new TextField(initials != null && initials[i] != null ? initials[i] : "");
            fields[i].setPrefWidth(220);
            grid.addRow(i, new Label(labels[i]), fields[i]);
        }
        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(320);
        error.setStyle("-fx-text-fill: #e06c75;");
        grid.addRow(labels.length, error);
        dialog.getDialogPane().setContent(grid);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            String[] values = new String[fields.length];
            for (int i = 0; i < fields.length; i++) values[i] = fields[i].getText();
            try {
                onConfirm.accept(values);
            } catch (Exception ex) {
                error.setText(ex.getMessage());
                e.consume();   // 失败不关窗
            }
        });
        dialog.showAndWait();
    }
}
