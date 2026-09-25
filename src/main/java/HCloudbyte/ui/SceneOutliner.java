package HCloudbyte.ui;

import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 场景集合树（大纲视图），被属性面板与聊天面板共用。
 *
 * <p>【接口接线点】（AI Api Interface document v2.md §4）
 * <ul>
 *   <li>树加载 → {@code ModelInterface#listModel()}（§4.1.1 LIST_MODEL，按返回列表动态构建行）</li>
 *   <li>增删复制合并工具（将来加按钮）→
 *       {@code ModelInterface#createModel(name)}（§4.1.2 CREATE_MODEL）、
 *       {@code #removeModel(name)}（§4.1.3 DELETE_MODEL）、
 *       {@code #copyModel(src, dst)}（§4.1.4 COPY_MODEL）、
 *       {@code #mergeModel(s1, s2, dst)}（§4.1.5 MERGE_MODEL）</li>
 *   <li>选中行：UI 状态，可回调上层联动 PropertyPanel（对象属性）</li>
 * </ul>
 */
public class SceneOutliner extends VBox {

    public SceneOutliner() {
        super(6);

        Label title = new Label("场景集合");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox tree = new VBox(2);
        // 接口接线：树行 ← ModelInterface#listModel() 动态构建（当前为写死的示例场景）
        tree.getChildren().add(buildOutlinerRow("📁", "Scene", 0, false, false));
        tree.getChildren().add(buildOutlinerRow("📷", "Camera", 1, false, false));
        tree.getChildren().add(buildOutlinerRow("◇", "Tesseract 001", 1, true, true));
        tree.getChildren().add(buildOutlinerRow("◇", "Tesseract 002", 1, false, false));
        tree.getChildren().add(buildOutlinerRow("💡", "Light", 1, false, false));

        getChildren().addAll(title, tree);
    }

    private HBox buildOutlinerRow(String icon, String name, int indent,
                                  boolean selected, boolean expandable) {
        Label arrow = new Label(expandable ? "▾" : " ");
        arrow.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");
        arrow.setMinWidth(10);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 12px;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (selected ? "#1d4ed8" : "#444") + ";");

        HBox row = new HBox(6, arrow, iconLabel, nameLabel);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(5, 10, 5, 10 + indent * 14));

        if (selected) {
            row.setStyle(
                    "-fx-background-color: linear-gradient(to right, rgba(74,144,226,0.25), rgba(74,144,226,0.08));" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: rgba(74,144,226,0.35);" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-width: 1;"
            );
        } else {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
            row.setCursor(Cursor.HAND);
            String hover =
                    "-fx-background-color: rgba(255,255,255,0.65);" +
                            "-fx-background-radius: 8;";
            row.setOnMouseEntered(e -> row.setStyle(hover));
            row.setOnMouseExited(e -> row.setStyle(
                    "-fx-background-color: transparent; -fx-background-radius: 8;"));
        }
        return row;
    }
}