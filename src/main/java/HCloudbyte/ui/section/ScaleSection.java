package HCloudbyte.ui.section;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;

import java.util.function.Supplier;

/**
 * 「缩放」区块：沿 x / y / z / w 四个轴增量缩放（TransformInterface.scale）。
 * 初值 1.00（等比），应用时取输入框当前值。
 */
public final class ScaleSection extends VBox {

    public ScaleSection(RendererInterface renderer, Supplier<String> selectedModel) {
        super(6);

        String[] axes = {"X", "Y", "Z", "W"};
        TextField[] fields = new TextField[4];
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < 4; i++) {
            Label label = new Label(axes[i]);
            label.setPrefWidth(18);
            fields[i] = new TextField("1.00");
            fields[i].setPrefWidth(56);
            fields[i].setStyle("-fx-padding: 2 6;");
            grid.add(new HBox(4, label, fields[i]), i % 2, i / 2);
        }
        getChildren().add(grid);

        var apply = Buttons.accent("应用", 2, 10);
        apply.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) return;
            try {
                renderer.getTransform().scale(name,
                        Float.parseFloat(fields[0].getText().trim()),
                        Float.parseFloat(fields[1].getText().trim()),
                        Float.parseFloat(fields[2].getText().trim()),
                        Float.parseFloat(fields[3].getText().trim()));
            } catch (NumberFormatException ignored) {
                // 输入非法时忽略
            }
        });
        getChildren().add(apply);
    }
}
