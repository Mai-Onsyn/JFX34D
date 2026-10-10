package HCloudbyte.ui.section;

import HCloudbyte.ui.theme.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

import java.util.function.Supplier;

/**
 * 「变换」区块：读取 / 写回绝对平移（矩阵最后一列），以及相对移动（±X/±Y/±Z/±W，步长 0.5）。
 *
 * <p>接口：{@code TransformInterface.getModelMatrix / setModelMatrix / move}。
 */
public final class PositionSection extends VBox {

    private final RendererInterface renderer;
    private final Supplier<String> selectedModel;
    private final TextField[] fields = new TextField[4];

    public PositionSection(RendererInterface renderer, Supplier<String> selectedModel) {
        super(6);
        this.renderer = renderer;
        this.selectedModel = selectedModel;

        Label title = new Label("平移");
        Button read = Buttons.accent("读取", 2, 12);
        Button apply = Buttons.accent("应用", 2, 12);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(8, title, spacer, read, apply);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(titleRow);

        getChildren().add(coordinateRow("位置X", 0));
        getChildren().add(coordinateRow("Y", 1));
        getChildren().add(coordinateRow("Z", 2));
        getChildren().add(coordinateRow("W", 3));

        // 模型相对移动（TransformInterface.move）
        getChildren().add(Theme.hintLabel("相对移动"));
        getChildren().add(buildMoveGrid());

        // 读取：getModelMatrix 行主序 5×5 最后一列为平移
        read.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) return;
            Matrix5f m = renderer.getTransform().getModelMatrix(name);
            fields[0].setText(String.format("%.2f", m.get(4)));
            fields[1].setText(String.format("%.2f", m.get(9)));
            fields[2].setText(String.format("%.2f", m.get(14)));
            fields[3].setText(String.format("%.2f", m.get(19)));
        });

        // 应用：复制当前矩阵，改写平移列后写回（绝对位置）
        apply.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) return;
            try {
                float[] data = renderer.getTransform().getModelMatrix(name).getData().clone();
                data[4] = Float.parseFloat(fields[0].getText().trim());
                data[9] = Float.parseFloat(fields[1].getText().trim());
                data[14] = Float.parseFloat(fields[2].getText().trim());
                data[19] = Float.parseFloat(fields[3].getText().trim());
                renderer.getTransform().setModelMatrix(name, new Matrix5f(data));
            } catch (NumberFormatException ignored) {
                // 输入非法时忽略，保持原矩阵
            }
        });
    }

    /** ±X/±Y/±Z/±W 八个相对移动按钮（两行四列，步长 0.5）。 */
    private GridPane buildMoveGrid() {
        String[] names = {"X-", "X+", "Y-", "Y+", "Z-", "Z+", "W-", "W+"};
        int[] axes = {0, 0, 1, 1, 2, 2, 3, 3};
        float[] steps = {-0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f};

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        for (int i = 0; i < names.length; i++) {
            final int axis = axes[i];
            final float step = steps[i];
            Button b = Buttons.accent(names[i], 2, 8);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> {
                String name = selectedModel.get();
                if (name == null) return;
                float[] offset = {0, 0, 0, 0};
                offset[axis] = step;
                renderer.getTransform().move(name, new Vector4f(offset[0], offset[1], offset[2], offset[3]));
            });
            grid.add(b, i % 4, i / 4);
        }
        return grid;
    }

    /** 一行：标签 + 输入框，并把输入框登记到 fields[fieldIdx]。 */
    private HBox coordinateRow(String axis, int fieldIdx) {
        Label label = new Label(axis);
        label.setPrefWidth(48);
        TextField field = new TextField("0.00");
        field.setPrefWidth(64);
        field.setStyle("-fx-padding: 2 6;");
        fields[fieldIdx] = field;
        HBox row = new HBox(8, label, field);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
