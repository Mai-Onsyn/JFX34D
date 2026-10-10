package HCloudbyte.ui.layout;

import HCloudbyte.ui.chat.ChatPanel;
import HCloudbyte.ui.interfaces.UIInterface;
import HCloudbyte.ui.property.PropertyPanel;
import HCloudbyte.ui.scenetree.SceneTree;
import HCloudbyte.ui.statusbar.StatusBar;
import HCloudbyte.ui.topbar.TopBar;
import HCloudbyte.ui.viewport.ViewportBox;
import HCloudbyte.ui.viewport.ViewportPanel;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 主布局装配：把顶栏 / 中央视口 / 右侧面板 / 底部状态栏拼成一个 {@link BorderPane}。
 *
 * <p>本类只做「创建各面板 → 摆位 → 接线」，不含任何业务逻辑。
 * 其中右侧面板用 {@link RightPanelHost} 承载手写/语音两种模式；顶栏的模式切换回调直接切它。
 */
public class RootLayout extends BorderPane {

    private final ChatPanel chatPanel;

    public RootLayout(GL4DRegion region, RendererInterface renderer, ViewportBox viewportBox) {
        setPadding(new Insets(16));

        PropertyPanel propertyPanel = new PropertyPanel(renderer);
        chatPanel = new ChatPanel(new SceneTree(renderer));
        RightPanelHost rightPanel = new RightPanelHost(propertyPanel, chatPanel);

        ViewportPanel viewport = new ViewportPanel(region, renderer);
        TopBar topBar = new TopBar(renderer, region, viewportBox, index -> rightPanel.setVoiceMode(index == 1));
        StatusBar statusBar = new StatusBar(region, renderer);

        // 接口初始化：把 UIInterface 绑定到场景集合树（选中路径）+ 视口悬浮窗（相机机位）
        UIInterface.initialize(propertyPanel.getSceneTree(), viewport.getOverlay());

        setTop(topBar);
        setCenter(viewport);
        setRight(rightPanel);
        setBottom(statusBar);

        BorderPane.setMargin(topBar, new Insets(0, 0, 14, 0));
        BorderPane.setMargin(rightPanel, new Insets(0, 0, 0, 14));
        BorderPane.setMargin(statusBar, new Insets(14, 0, 0, 0));
    }

    /** 聊天面板引用：Agent 初始化失败时由 App 用于禁用输入。 */
    public ChatPanel getChatPanel() {
        return chatPanel;
    }
}
