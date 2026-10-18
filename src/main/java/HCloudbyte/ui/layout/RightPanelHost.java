package HCloudbyte.ui.layout;

import HCloudbyte.ui.chat.ChatPanel;
import HCloudbyte.ui.property.PropertyPanel;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;

public class RightPanelHost extends StackPane {
    private final PropertyPanel propertyPanel;
    private final ChatPanel chatPanel;

    public RightPanelHost(PropertyPanel propertyPanel, ChatPanel chatPanel) {
        this.propertyPanel = propertyPanel;
        this.chatPanel = chatPanel;

        ScrollPane scroll = new ScrollPane(propertyPanel);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);   // 内容矮于视口时也撑满高度，与左侧视口等高
        scroll.setMinHeight(0);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        chatPanel.setVisible(false);

        getChildren().addAll(scroll, chatPanel);

        setPrefWidth(280);
        setMinWidth(280);
        setMaxWidth(280);
        setMinHeight(0);   // 关键：压平 minHeight，避免 BorderPane 被撑出窗口
    }

    /**
     * 切换右侧显示：手写模式（属性面板）或语音模式（聊天面板）。
     *
     * @param voiceMode true = 语音模式，false = 手写模式
     */
    public void setVoiceMode(boolean voiceMode) {
        propertyPanel.setVisible(!voiceMode);
        chatPanel.setVisible(voiceMode);
    }
}
