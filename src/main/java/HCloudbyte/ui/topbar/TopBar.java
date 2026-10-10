package HCloudbyte.ui.topbar;

import HCloudbyte.ui.dialog.SettingsCache;
import HCloudbyte.ui.dialog.SettingsDialog;
import HCloudbyte.ui.theme.SegmentedControl;
import HCloudbyte.ui.theme.Theme;
import HCloudbyte.ui.viewport.ViewportBox;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;

import java.util.function.IntConsumer;

/**
 * 顶部栏：标题 / 文件名 / 投影切换 / 3D-4D 输入模式 / 手写-语音模式 / Agent 状态 / 渲染设置。
 *
 * <p>只负责把控件摆好并接线；「场景设置」弹窗内容全在
 * {@link SettingsDialog}，缓存值放在长期持有的 {@link SettingsCache} 里。
 */
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
        Label title = new Label("JFX 34D");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT_TITLE + ";");

        Label file = new Label("scene.4do");
        file.setStyle("-fx-text-fill: " + Theme.TEXT_MUTED + ";");

        Separator sep = new Separator(Orientation.VERTICAL);

        // 投影切换：RendererInterface 无对应操作，先只做 UI（渲染设置回调预留）
        SegmentedControl projection = new SegmentedControl(new String[]{"透视投影", "正交投影"}, 0);

        HBox leftBox = new HBox(12, title, file, sep, projection);
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
                new String[]{"手写模式", "语音模式"}, 0, onInputModeChange);

        Label agentStatus = new Label("● Agent 在线");
        agentStatus.setStyle("-fx-text-fill: " + Theme.SUCCESS + ";");

        Button renderSettings = Theme.buildLiquidButton("设置");
        renderSettings.setOnAction(e -> SettingsDialog.show(renderer, cache, viewportBox));

        HBox rightBox = new HBox(12, dimSwitch, inputMode, agentStatus, renderSettings);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(leftBox, spacer, rightBox);
        content.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(content);
        setPadding(new Insets(12, 20, 12, 20));
        setStyle(Theme.GLASS);
    }
}
