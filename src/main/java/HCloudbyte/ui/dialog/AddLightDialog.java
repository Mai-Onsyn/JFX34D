package HCloudbyte.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.ogl3d.data.Light;

/**
 * 「添加光源」弹窗：名字 / 位置(世界坐标) / 颜色 / 强度 → 构造 {@link Light} 并加入场景。
 * Light 在 Java 侧可直接 new；颜色走 {@code setColor(float,float,float,float)} 重载，避开 ColorARGB。
 */
public final class AddLightDialog {

    private AddLightDialog() {
    }

    /** 打开弹窗；点确定且输入合法时向场景添加一个点光源。 */
    public static void show(RendererInterface renderer) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("添加光源");
        dialog.setHeaderText("添加点光源（位置为世界坐标）");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField name = new TextField("PointLight");
        TextField x = new TextField("0");
        x.setPrefWidth(56);
        TextField y = new TextField("50");
        y.setPrefWidth(56);
        TextField z = new TextField("0");
        z.setPrefWidth(56);
        ColorPicker color = new ColorPicker(Color.WHITE);
        TextField intensity = new TextField("0.6");
        intensity.setPrefWidth(56);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("名字"), name);
        grid.addRow(1, new Label("位置"), new HBox(6, x, y, z));
        grid.addRow(2, new Label("颜色"), color);
        grid.addRow(3, new Label("强度"), intensity);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            try {
                Light light = new Light();
                String lightName = name.getText().trim();
                light.setName(lightName.isEmpty() ? "PointLight" : lightName);
                light.moveTo(
                        Float.parseFloat(x.getText().trim()),
                        Float.parseFloat(y.getText().trim()),
                        Float.parseFloat(z.getText().trim()));
                Color c = color.getValue();
                light.setColor((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(), 1f);
                light.setIntensity(Float.parseFloat(intensity.getText().trim()));
                renderer.getScene().addLights(light);
            } catch (NumberFormatException ignored) {
                // 输入非法时忽略，不加光源
            }
        });
    }
}
