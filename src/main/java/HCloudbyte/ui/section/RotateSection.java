package HCloudbyte.ui.section;

import HCloudbyte.ui.theme.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import mai_onsyn.renderer.cpu4dkt.CameraOrientation;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Direction;

import java.util.function.Supplier;

/**
 * 「旋转」区块：模型绕 6 个平面旋转（TransformInterface.rotate），
 * 下方实时只读显示相机 6 平面姿态角（每秒钟刷新一次）。
 */
public final class RotateSection extends VBox {

    public RotateSection(RendererInterface renderer, Supplier<String> selectedModel) {
        super(6);

        getChildren().add(Theme.hintLabel("模型旋转"));
        PlaneSelector plane = new PlaneSelector();
        getChildren().add(plane);

        TextField angle = new TextField("15");
        angle.setPrefWidth(56);
        angle.setStyle("-fx-padding: 2 6;");
        var apply = Buttons.accent("应用", 2, 10);
        apply.setOnAction(e -> {
            String name = selectedModel.get();
            if (name == null) return;
            try {
                float a = Float.parseFloat(angle.getText().trim());
                renderer.getTransform().rotate(name, Direction.Plane.valueOf(plane.getSelected()), a);
            } catch (NumberFormatException ignored) {
                // 角度非法时忽略
            }
        });
        HBox foot = new HBox(8, angle, apply);
        foot.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(foot);

        // 相机姿态角（只读）：相机在黑色视口里用键盘旋转，这里每秒回显 6 个平面角
        getChildren().add(new Separator());
        getChildren().add(Theme.hintLabel("相机旋转角度（键盘视口）"));
        getChildren().add(buildCameraAngleGrid(renderer));
    }

    /** 6 个只读角度标签 + 每秒刷新。 */
    private static GridPane buildCameraAngleGrid(RendererInterface renderer) {
        String[] planes = {"XY", "XZ", "YZ", "XW", "YW", "ZW"};
        Label[] values = new Label[6];

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < 6; i++) {
            Label name = new Label(planes[i]);
            name.setPrefWidth(30);
            values[i] = new Label("0.0°");
            values[i].setStyle("-fx-font-size: 11px; -fx-text-fill: #c7ccd1;");
            grid.add(new HBox(4, name, values[i]), i % 3, i / 3);
        }

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            try {
                CameraOrientation o = renderer.getScene().get4DCameraOrientation();
                values[0].setText(String.format("%.1f°", o.getXy()));
                values[1].setText(String.format("%.1f°", o.getXz()));
                values[2].setText(String.format("%.1f°", o.getYz()));
                values[3].setText(String.format("%.1f°", o.getXw()));
                values[4].setText(String.format("%.1f°", o.getYw()));
                values[5].setText(String.format("%.1f°", o.getZw()));
            } catch (Exception ignored) {
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        return grid;
    }
}
