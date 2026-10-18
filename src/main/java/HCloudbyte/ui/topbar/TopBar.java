package HCloudbyte.ui.topbar;

import HCloudbyte.ui.dialog.Dialogs;
import HCloudbyte.ui.dialog.SettingsCache;
import HCloudbyte.ui.dialog.SettingsDialog;
import HCloudbyte.ui.theme.SegmentedControl;
import HCloudbyte.ui.theme.Theme;
import HCloudbyte.ui.viewport.ViewportBox;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.cpu4dkt.Camera4DUtilsKt;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

import java.util.function.IntConsumer;

public class TopBar extends StackPane {
    private final SettingsCache cache = new SettingsCache();

    /**
     * @param renderer          渲染接口（设置弹窗调 SceneInterface）
     * @param region            渲染视口（3D/4D 模式切换走 GL4DRegion 内置 I 键逻辑）
     * @param viewportBox       视口区域指示方块（设置弹窗里可开关 / 跟视口大小）
     * @param onInputModeChange 输入模式切换回调（0 = 手写模式，1 = 语音模式）
     */
    public TopBar(RendererInterface renderer, GL4DRegion region, ViewportBox viewportBox,
                  IntConsumer onInputModeChange) {
        Label title = new Label("四维智构");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_TITLE + ";");

        Label file = new Label("建模工具");
        file.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + ";");

        Separator sep = new Separator(Orientation.VERTICAL);

        // 投影切换：走 SceneInterface.setPerspectiveProjection，只影响 4D 投影矩阵（透视 / 正交）
        SegmentedControl projection = new SegmentedControl(
                new String[]{"透视投影", "正交投影"},
                0,
                i -> renderer.getScene().setPerspectiveProjection(i == 0));

        Separator sep2 = new Separator(Orientation.VERTICAL);

        HBox leftBox = new HBox(12, title, file, sep, projection, sep2, buildLookAt(renderer));
        leftBox.setAlignment(Pos.CENTER_LEFT);

        // 3D/4D 输入模式：调 SceneInterface.setEnable4DInput（互斥）。
        // 键盘 I 键（GL4DRegion 内置）切换时，经 setOnEnable4DInputChanged 回调同步 UI。
        SegmentedControl dimSwitch = new SegmentedControl(
                new String[]{"3D", "4D"},
                region.getEnableInput() ? 0 : 1,
                i -> renderer.getScene().setEnable4DInput(i == 1));
        renderer.getScene().setOnEnable4DInputChanged(fourD -> {
            dimSwitch.select(fourD ? 1 : 0);
            return kotlin.Unit.INSTANCE;
        });

        SegmentedControl inputMode = new SegmentedControl(
                new String[]{"手动模式", "Agent模式"}, 0, onInputModeChange);

        Button renderSettings = Theme.buildLiquidButton("设置");
        renderSettings.setOnAction(e -> SettingsDialog.show(renderer, cache, viewportBox));

        HBox rightBox = new HBox(12, dimSwitch, inputMode, renderSettings);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(leftBox, spacer, rightBox);
        content.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(content);
        setPadding(new Insets(12, 20, 12, 20));
        setStyle(Theme.GLASS);
    }

    /* ==================== 看向某点 ==================== */

    private static HBox buildLookAt(RendererInterface renderer) {
        TextField point = new TextField();
        point.setPromptText("看向 x, y, z, w");
        point.setPrefWidth(132);
        point.setStyle("-fx-padding: 4 8;");

        Button look = new Button("看向");
        look.setCursor(Cursor.HAND);
        look.setStyle("-fx-padding: 4 12; -fx-background-radius: 6; -fx-text-fill: #ffffff;"
                + "-fx-background-color: rgba(255,138,26,0.85);");
        look.setOnMouseEntered(e -> look.setStyle("-fx-padding: 4 12; -fx-background-radius: 6; -fx-text-fill: #ffffff;"
                + "-fx-background-color: rgba(255,138,26,1.0);"));
        look.setOnMouseExited(e -> look.setStyle("-fx-padding: 4 12; -fx-background-radius: 6; -fx-text-fill: #ffffff;"
                + "-fx-background-color: rgba(255,138,26,0.85);"));

        Runnable action = () -> {
            float[] p = parsePoint(point.getText());
            if (p == null) {
                Dialogs.error("坐标格式错误", "请输入 4 个数字，例如：0, 0, 0, 0");
                return;
            }
            Vector4f eye = renderer.getCamera().getPosition();
            Vector4f target = new Vector4f(p[0], p[1], p[2], p[3]);
            if (new Vector4f(target).sub(eye).length() < 1e-6f) {
                Dialogs.error("无法看向该点", "目标点与摄像机位置重合，方向无法确定。");
                return;
            }
            renderer.getCamera().setView(Camera4DUtilsKt.lookAt4D(eye, target));   // 平滑动画转到该视线
        };

        look.setOnAction(e -> action.run());
        point.setOnAction(e -> action.run());       // 回车也能触发

        HBox box = new HBox(6, point, look);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /**
     * 解析坐标文本为 4 个 float。分隔符随意：空白、各种逗号分号（, ，、; ；）、括号都可以，
     * 例如 {@code "1, 2, 3, 4"} / {@code "1 2 3 4"} / {@code "(1，2、3；4)"} 都行。格式不对返回 null。
     */
    private static float[] parsePoint(String text) {
        if (text == null) return null;
        // 所有分隔符统一成单个空格，再去掉首尾，剩下的就是纯数字段
        String normalized = text.trim().replaceAll("[\\s,，、;；()（）\\[\\]{}]+", " ").trim();
        if (normalized.isEmpty()) return null;
        String[] parts = normalized.split(" ");
        if (parts.length != 4) return null;
        float[] out = new float[4];
        try {
            for (int i = 0; i < 4; i++) out[i] = Float.parseFloat(parts[i]);
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }
}
