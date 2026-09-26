package HCloudbyte.ui;

import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;

import java.util.List;
import java.util.function.Consumer;

/**
 * 场景集合树（大纲视图），被属性面板与聊天面板共用。
 * 树行由 ModelInterface#listModel() 动态构建（§4.1.1 LIST_MODEL）。
 *
 * <p>【接口调用】（AI Api Interface document.md §4）
 * <ul>
 *   <li>树加载 → {@code ModelInterface#listModel()}（§4.1.1 LIST_MODEL ✅）</li>
 *   <li>增删复制合并（将来加按钮）→
 *       {@code ModelInterface#createModel(name)}（§4.1.2 CREATE_MODEL）、
 *       {@code #removeModel(name)}（§4.1.3 DELETE_MODEL）、
 *       {@code #copyModel(src, dst)}、{@code #mergeModel(s1, s2, dst)}</li>
 *   <li>选中行：回调 {@link Consumer}{@code <String>} 通知上层（PropertyPanel 变换区联动）</li>
 * </ul>
 */
public class SceneOutliner extends VBox {

    private HBox selectedRow;
    private final Consumer<String> onSelect;

    public SceneOutliner(RendererInterface renderer) {
        this(renderer, null);
    }

    /**
     * @param renderer 渲染接口（取 model 列表构建树）
     * @param onSelect 选中模型名回调（可为 null）
     */
    public SceneOutliner(RendererInterface renderer, Consumer<String> onSelect) {
        super(6);
        this.onSelect = onSelect;

        Label title = new Label("场景集合");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");

        VBox tree = new VBox(2);
        // 接口调用：树行 ← ModelInterface#listModel()（§4.1.1 LIST_MODEL ✅）
        tree.getChildren().add(buildOutlinerRow("📁", "Scene", 0, false, false, null));
        List<String> models = renderer.getModel().listModel();
        for (int i = 0; i < models.size(); i++) {
            String name = models.get(i);
            tree.getChildren().add(buildOutlinerRow("◇", name, 1, i == 0, true, name));
        }

        getChildren().addAll(title, tree);
    }

    private HBox buildOutlinerRow(String icon, String name, int indent,
                                  boolean selected, boolean expandable, String modelName) {
        Label arrow = new Label(expandable ? "▾" : " ");
        arrow.setStyle("-fx-font-size: 9px; -fx-text-fill: #8a8f94;");
        arrow.setMinWidth(10);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 12px;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (selected ? "#ffa116" : "#b6bcc3") + ";");

        HBox row = new HBox(6, arrow, iconLabel, nameLabel);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(4, 10, 4, 10 + indent * 14));

        if (modelName != null) {
            // 点击模型行：切换选中样式并回调上层（PropertyPanel 变换区联动）
            row.setOnMouseClicked(e -> {
                if (selectedRow != null) applyRowStyle(selectedRow, false);
                selectedRow = row;
                applyRowStyle(row, true);
                if (onSelect != null) onSelect.accept(modelName);
            });
        }

        applyRowStyle(row, selected);
        if (selected) selectedRow = row;
        return row;
    }

    private void applyRowStyle(HBox row, boolean selected) {
        Label nameLabel = (Label) row.getChildren().get(2);
        if (selected) {
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffa116;");
            row.setStyle(
                    "-fx-background-color: linear-gradient(to right, rgba(255,138,26,0.22), rgba(255,138,26,0.06));" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: rgba(255,138,26,0.45);" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-width: 1;"
            );
        } else {
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b6bcc3;");
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
            row.setCursor(Cursor.HAND);
            String hover =
                    "-fx-background-color: rgba(100,107,115,0.45);" +
                            "-fx-background-radius: 8;";
            row.setOnMouseEntered(e -> row.setStyle(hover));
            row.setOnMouseExited(e -> row.setStyle(
                    "-fx-background-color: transparent; -fx-background-radius: 8;"));
        }
    }
}
