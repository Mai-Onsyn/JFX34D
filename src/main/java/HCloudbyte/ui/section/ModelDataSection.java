package HCloudbyte.ui.section;

import HCloudbyte.ui.property.PropertyPanel;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 「数据」区：显示选中模型的类型 / 四面体数 / 顶点数 / 可见性 / params JSON，
 * 并提供刷新与复制 JSON。选中模型变化时由 {@link PropertyPanel} 调用 {@link #refresh}。
 */
public final class ModelDataSection extends VBox {

    private final RendererInterface renderer;

    private final Label kindLabel = new Label("-");
    private final Label tetsLabel = new Label("-");
    private final Label vertexLabel = new Label("-");
    private final Label visibleLabel = new Label("-");
    private final Label paramsLabel = new Label("（未选中）");

    public ModelDataSection(RendererInterface renderer) {
        super(6);
        this.renderer = renderer;

        Button refresh = new Button("刷新");
        refresh.setOnAction(e -> refresh(currentName));

        Button copyJson = new Button("复制JSON");
        copyJson.setOnAction(e -> copyParams());

        getChildren().add(new HBox(8, new Label("数据"), refresh, copyJson));
        getChildren().add(row("类型", kindLabel));
        getChildren().add(row("四面体", tetsLabel));
        getChildren().add(row("顶点", vertexLabel));
        getChildren().add(row("可见", visibleLabel));

        paramsLabel.setWrapText(true);
        paramsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        getChildren().add(paramsLabel);
    }

    /** 当前显示的模型名（供「刷新」按钮使用）。 */
    private String currentName;

    /** 选中模型变化时刷新数据区：直接拿 Mesh4D 实例读字段。 */
    public void refresh(String name) {
        currentName = name;
        if (name == null) {
            setAll("-", "-", "-", "-", "（未选中）");
            return;
        }
        Mesh4D mesh;
        try {
            mesh = renderer.getModel().getModel(name);
        } catch (Exception ex) {
            // 中间/结构节点没有实体模型
            setAll("结构节点", "-", "-", "-", "（该路径不是实体模型）");
            return;
        }
        int tets = mesh.getTetrahedrons().size();
        setAll(mesh.getKind().name(),
                String.valueOf(tets),
                String.valueOf(tets * 4),
                mesh.getVisible() ? "是" : "否（已隐藏）",
                mesh.getParams() == null ? "params: （无）" : mesh.getParams().toJSONString());
    }

    private void copyParams() {
        String text = paramsLabel.getText();
        if (text == null || text.startsWith("（未选中）") || text.startsWith("读取失败")
                || text.startsWith("params: （无）")) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void setAll(String kind, String tets, String vertex, String visible, String params) {
        kindLabel.setText(kind);
        tetsLabel.setText(tets);
        vertexLabel.setText(vertex);
        visibleLabel.setText(visible);
        paramsLabel.setText(params);
    }

    private static HBox row(String label, Label value) {
        Label l = new Label(label);
        l.setPrefWidth(48);
        HBox h = new HBox(8, l, value);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
