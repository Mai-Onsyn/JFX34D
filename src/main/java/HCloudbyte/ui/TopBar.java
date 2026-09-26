package HCloudbyte.ui;

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

import java.util.function.IntConsumer;

/**
 * 顶部栏：标题、文件名、投影切换、输入模式切换、Agent 状态、渲染设置。
 *
 * <p>【接口接线点】（AI Api Interface document v2.md §0.1）
 * <ul>
 *   <li>保存 / 打开（将来加按钮）→ {@code IOInterface#saveModel(name, fileName)} / {@code #loadModel(name, file)}（§7）</li>
 *   <li>投影切换（透视/正交）：渲染侧投影模式，RendererInterface 无对应操作，走渲染设置回调</li>
 *   <li>Agent 状态：AgentService 上线/下线时更新（§2 对话流程）</li>
 *   <li>渲染设置：渲染参数（FPS、顶点数等），对接 StatusBar / GL3DRegion</li>
 * </ul>
 */
public class TopBar extends StackPane {

    /**
     * @param onInputModeChange 输入模式切换回调（0 = 手写模式，1 = 语音模式）
     */
    public TopBar(IntConsumer onInputModeChange) {
        Label title = new Label("JFX 34D");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");

        Label file = new Label("scene.4do");
        file.setStyle("-fx-text-fill: #8a8f94;");

        Separator sep = new Separator(Orientation.VERTICAL);

        HBox projectionSwitch = UiTheme.buildSegmentedControl(
                new String[]{"透视投影", "正交投影"}, 0);
        // 接口接线：投影模式切换 → 渲染设置回调（RendererInterface 无对应操作）

        HBox leftBox = new HBox(12, title, file, sep, projectionSwitch);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        HBox inputModeSwitch = UiTheme.buildSegmentedControl(
                new String[]{"手写模式", "语音模式"}, 0, onInputModeChange);

        Label agentStatus = new Label("● Agent 在线");
        agentStatus.setStyle("-fx-text-fill: #7fd08a;");
        // 接口接线：Agent 状态 → AgentService 生命周期（可用 send() 后置为在线）

        Button renderSettings = UiTheme.buildLiquidButton("渲染设置");
        // 接口接线：点击 → 渲染设置面板（FPS/顶点数等，对接 GL3DRegion 与 StatusBar）

        HBox rightBox = new HBox(12, inputModeSwitch, agentStatus, renderSettings);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(leftBox, spacer, rightBox);
        content.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(content);
        setPadding(new Insets(12, 20, 12, 20));
        setStyle(UiTheme.GLASS);
    }
}