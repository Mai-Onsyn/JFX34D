package HCloudbyte.ui;

import HCloudbyte.ui.bootstrap.AgentBootstrap;
import HCloudbyte.ui.bootstrap.RenderBootstrap;
import HCloudbyte.ui.layout.RootLayout;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * 重构版主应用入口。
 *
 * <p>在原来的 {@code HCloudbyte.ui.Main} 里把启动行改成：
 * <pre>{@code Application.launch(RefactoredApp.class);}</pre>
 * 即可切到本套 UI（原 {@code MainApp} 保持不动，随时可切回）。
 *
 * <p>职责很薄，只做四步：
 * <ol>
 *   <li>设置深色主题（PrimerDark）；</li>
 *   <li>{@link RenderBootstrap} 建渲染视口、{@link AgentBootstrap} 初始化 Agent；</li>
 *   <li>{@link RootLayout} 装配整个界面；</li>
 *   <li>开窗，Agent 未就绪时禁用输入并弹窗提示。</li>
 * </ol>
 */
public class RefactoredApp extends Application {

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 1. 渲染视口 + 渲染接口（绑定同一摄像机）
        RenderBootstrap.Result render = RenderBootstrap.create();
        // 2. Agent（自然语言 → IR → 渲染）
        AgentBootstrap.Result agent = AgentBootstrap.init(render.renderer());
        // 3. 界面装配
        RootLayout layout = new RootLayout(render.region(), render.renderer(), render.viewportBox());

        Scene scene = new Scene(layout, 1280, 800);
        scene.setFill(Color.web("#232527"));   // 深色窗口底色（Blender 背景灰蓝）
        // 右键菜单（ContextMenu）是 Popup，不继承节点级样式表，必须挂到 Scene 级
        java.net.URL overlayCss = getClass().getResource("/css/viewport-overlay.css");
        if (overlayCss != null) scene.getStylesheets().add(overlayCss.toExternalForm());

        stage.setTitle("四维智构");
        stage.setScene(scene);
        stage.show();

        // 4. Agent 初始化失败：禁用语音输入 + 弹窗提示（UI 已就绪后再处理，不阻塞启动）
        if (!agent.ready()) {
            layout.getChatPanel().setInputEnabled(false);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Agent 初始化失败");
            alert.setHeaderText("语音建模不可用，输入框与发送按钮已禁用");
            alert.setContentText("原因：" + (agent.error() != null ? agent.error().getMessage() : "未知错误")
                    + "\n\n请检查 LLM 服务配置（deepSeek 连接）后重启应用。");
            alert.show();
        }
    }
}
