package HCloudbyte.ui.section;

import HCloudbyte.ui.theme.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Direction;

import java.util.function.Supplier;

/**
 * 「裁剪」区块：沿「源轴 → 目标轴」压缩 / 拉伸（TransformInterface.clip），量可正可负（负 = 反向）。
 */
public final class ClipSection extends VBox {

    public ClipSection(RendererInterface renderer, Supplier<String> selectedModel) {
        super(6);
        getChildren().add(new Label("裁剪"));

        ComboBox<String> srcBox = axisBox(0);
        ComboBox<String> destBox = axisBox(1);

        TextField amount = new TextField("0.5");
        amount.setPrefWidth(56);
        amount.setStyle("-fx-padding: 2 6;");

        var apply = Buttons.accent("应用", 2, 10);
        apply.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) return;
            try {
                renderer.getTransform().clip(name,
                        Direction.Axis.valueOf(srcBox.getValue()),
                        Direction.Axis.valueOf(destBox.getValue()),
                        Float.parseFloat(amount.getText().trim()));
            } catch (NumberFormatException ignored) {
            }
        });

        HBox row1 = new HBox(8, new Label("源轴"), srcBox, new Label("目标轴"), destBox);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, new Label("量"), amount, apply);
        row2.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(row1, row2);
        getChildren().add(Theme.hintLabel("沿源轴压缩/拉伸到目标轴，量可为负（反向）"));
    }

    private static ComboBox<String> axisBox(int defaultIndex) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll("X", "Y", "Z", "W");
        box.getSelectionModel().select(defaultIndex);
        return box;
    }
}
