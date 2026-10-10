package HCloudbyte.ui.section;

import HCloudbyte.ui.theme.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 「四面体详情」区块：按索引查询 / 删除单个四面体，以及对指定索引做局部变形（只转选中胞）。
 *
 * <p>数据变化后通过 {@code onDataChanged} 通知外部（通常是刷新下方的数据区）。
 */
public final class TetrahedronSection extends VBox {

    private final RendererInterface renderer;
    private final Supplier<String> selectedModel;
    private final Runnable onDataChanged;
    private final Label info = new Label("（选中模型后查询）");

    public TetrahedronSection(RendererInterface renderer, Supplier<String> selectedModel, Runnable onDataChanged) {
        super(6);
        this.renderer = renderer;
        this.selectedModel = selectedModel;
        this.onDataChanged = onDataChanged;

        getChildren().add(new Label("四面体详情"));

        TextField index = new TextField("0");
        index.setPrefWidth(48);
        index.setStyle("-fx-padding: 2 6;");

        info.setWrapText(true);
        info.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");

        Button query = Buttons.accent("查询", 2, 10);
        query.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) {
                info.setText("请先选中模型");
                return;
            }
            try {
                int i = Integer.parseInt(index.getText().trim());
                info.setText(renderer.getGeometry().getTetrahedronInfos(name, i));
            } catch (NumberFormatException ex) {
                info.setText("索引需为整数");
            } catch (Exception ex) {
                info.setText("查询失败：" + ex.getMessage());
            }
        });
        getChildren().addAll(new HBox(8, new Label("索引"), index, query), info);

        // 删除该四面体（删除后索引顺移）
        Button delete = Buttons.accent("删除该四面体", 2, 10);
        delete.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) {
                info.setText("请先选中模型");
                return;
            }
            try {
                int i = Integer.parseInt(index.getText().trim());
                renderer.getGeometry().removeTetrahedron(name, i);
                info.setText("已删除四面体 #" + i + "（索引已顺移）");
                onDataChanged.run();
            } catch (NumberFormatException ex) {
                info.setText("索引需为整数");
            } catch (Exception ex) {
                info.setText("删除失败：" + ex.getMessage());
            }
        });
        getChildren().add(delete);

        // 局部变形：只变换指定索引的四面体
        getChildren().add(new Separator());
        getChildren().add(Theme.hintLabel("局部变形（只转选中胞）"));
        getChildren().add(buildLocalDeformPart());
    }

    /** 局部变形输入区：索引(可空=全部) + 平面 + 角度 + 应用。 */
    private VBox buildLocalDeformPart() {
        VBox box = new VBox(6);

        TextField indices = new TextField();
        indices.setPromptText("索引(逗号分隔，空=全部)");
        indices.setPrefWidth(150);
        indices.setStyle("-fx-padding: 2 6;");

        ComboBox<String> plane = new ComboBox<>();
        plane.getItems().addAll("XY", "XZ", "XW", "YZ", "YW", "ZW");
        plane.getSelectionModel().select(0);

        TextField angle = new TextField("15");
        angle.setPrefWidth(56);
        angle.setStyle("-fx-padding: 2 6;");

        Button apply = Buttons.accent("应用", 2, 16);
        apply.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) {
                info.setText("请先选中模型");
                return;
            }
            try {
                List<Integer> ids = parseIndices(indices.getText());
                float a = Float.parseFloat(angle.getText().trim());
                renderer.getGeometry().transformTetrahedrons(name, ids, planeRotMatrix(plane.getValue(), a));
                info.setText("已变换 " + (ids.isEmpty() ? "全部" : ids.size() + " 个") + " 四面体");
                onDataChanged.run();
            } catch (NumberFormatException ex) {
                info.setText("角度需为数字");
            } catch (Exception ex) {
                info.setText("变换失败：" + ex.getMessage());
            }
        });

        box.getChildren().add(indices);
        HBox row = new HBox(8, new Label("平面"), plane, new Label("角度"), angle);
        row.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(row);
        HBox applyRow = new HBox(apply);
        applyRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(applyRow);
        return box;
    }

    /** 解析逗号/空格分隔的索引；空输入 → 空列表（接口语义 = 全部）。 */
    private static List<Integer> parseIndices(String text) {
        List<Integer> out = new ArrayList<>();
        if (text == null) return out;
        for (String s : text.split("[,，\\s]+")) {
            if (s.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    /** 构造绕某平面旋转 θ 的 5×5 行主序矩阵（用于 transformTetrahedrons）。 */
    private static Matrix5f planeRotMatrix(String plane, float deg) {
        float rad = (float) Math.toRadians(deg);
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        float[] d = {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1};
        switch (plane) {
            case "XY" -> { d[0] = c; d[1] = -s; d[5] = s; d[6] = c; }
            case "XZ" -> { d[0] = c; d[2] = -s; d[10] = s; d[12] = c; }
            case "XW" -> { d[0] = c; d[3] = -s; d[15] = s; d[18] = c; }
            case "YZ" -> { d[6] = c; d[7] = -s; d[11] = s; d[12] = c; }
            case "YW" -> { d[6] = c; d[8] = -s; d[16] = s; d[18] = c; }
            case "ZW" -> { d[12] = c; d[13] = -s; d[17] = s; d[18] = c; }
        }
        return new Matrix5f(d);
    }
}
