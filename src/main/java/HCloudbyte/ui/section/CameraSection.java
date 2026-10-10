package HCloudbyte.ui.section;

import HCloudbyte.ui.theme.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

/**
 * 「摄像机位置」区块：读取 / 应用相机 4D 坐标，相对移动（前后左右上下 + w 轴 ana），
 * 以及相机绕 6 平面旋转。全部走 {@code RendererInterface.getCamera()}（CameraInterface）。
 */
public final class CameraSection extends VBox {

    public CameraSection(RendererInterface renderer) {
        super(6);
        getChildren().add(new Label("摄像机位置"));

        TextField[] fields = new TextField[4];
        String[] axes = {"X", "Y", "Z", "W"};
        for (int i = 0; i < 4; i++) {
            fields[i] = new TextField("0.00");
            fields[i].setPrefWidth(64);
            fields[i].setStyle("-fx-padding: 2 6;");
            Label label = new Label(axes[i]);
            label.setPrefWidth(48);
            HBox row = new HBox(8, label, fields[i]);
            row.setAlignment(Pos.CENTER_LEFT);
            getChildren().add(row);
        }

        Button read = Buttons.accent("读取", 2, 12);
        Button apply = Buttons.accent("应用", 2, 12);

        // 读取 getPosition
        read.setOnAction(e -> {
            Vector4f p = renderer.getCamera().getPosition();
            fields[0].setText(String.format("%.2f", p.x));
            fields[1].setText(String.format("%.2f", p.y));
            fields[2].setText(String.format("%.2f", p.z));
            fields[3].setText(String.format("%.2f", p.w));
        });
        // 应用 setPosition
        apply.setOnAction(e -> {
            try {
                renderer.getCamera().setPosition(new Vector4f(
                        Float.parseFloat(fields[0].getText().trim()),
                        Float.parseFloat(fields[1].getText().trim()),
                        Float.parseFloat(fields[2].getText().trim()),
                        Float.parseFloat(fields[3].getText().trim())));
            } catch (NumberFormatException ignored) {
                // 输入非法时忽略
            }
        });

        HBox buttons = new HBox(8, read, apply);
        buttons.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(buttons);

        getChildren().add(buildMoveGrid(renderer));
        getChildren().add(buildAnaBox(renderer));

        // 相机旋转：与模型旋转同格式（6 平面 + 角度应用）
        getChildren().add(new Separator());
        getChildren().add(Theme.hintLabel("相机旋转"));
        PlaneSelector plane = new PlaneSelector();
        getChildren().add(plane);

        TextField angle = new TextField("15");
        angle.setPrefWidth(56);
        angle.setStyle("-fx-padding: 2 6;");
        Button rotate = Buttons.accent("应用", 2, 10);
        rotate.setOnAction(e -> {
            try {
                float a = Float.parseFloat(angle.getText().trim());
                switch (plane.getSelected()) {
                    case "XY" -> renderer.getCamera().rotateXY(a);
                    case "XZ" -> renderer.getCamera().rotateXZ(a);
                    case "XW" -> renderer.getCamera().rotateXW(a);
                    case "YZ" -> renderer.getCamera().rotateYZ(a);
                    case "YW" -> renderer.getCamera().rotateYW(a);
                    case "ZW" -> renderer.getCamera().rotateZW(a);
                }
            } catch (NumberFormatException ignored) {
                // 角度非法时忽略
            }
        });
        HBox rotateFoot = new HBox(8, angle, rotate);
        rotateFoot.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(rotateFoot);
    }

    /** 左/右/上/下/前/后 六个相对移动按钮（相对相机坐标系，步长 0.5）。 */
    private static GridPane buildMoveGrid(RendererInterface renderer) {
        String[] names = {"左", "右", "上", "下", "前", "后"};
        Runnable[] actions = {
                () -> renderer.getCamera().moveRight(-0.5f),
                () -> renderer.getCamera().moveRight(0.5f),
                () -> renderer.getCamera().moveUp(0.5f),
                () -> renderer.getCamera().moveUp(-0.5f),
                () -> renderer.getCamera().moveForward(0.5f),
                () -> renderer.getCamera().moveForward(-0.5f)
        };
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            Button b = Buttons.accent(names[i], 2, 10);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> actions[idx].run());
            grid.add(b, i % 2, i / 2);
        }
        return grid;
    }

    /** w 轴（ana）方向移动。 */
    private static HBox buildAnaBox(RendererInterface renderer) {
        Button minus = Buttons.accent("Z-", 2, 10);
        minus.setOnAction(e -> renderer.getCamera().moveAna(-0.5f));
        Button plus = Buttons.accent("Z+", 2, 10);
        plus.setOnAction(e -> renderer.getCamera().moveAna(0.5f));
        return new HBox(8, minus, plus);
    }
}
