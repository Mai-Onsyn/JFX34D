package HCloudbyte.ui.section;

import javafx.scene.control.Button;

/**
 * 属性面板各区块共用的小控件工厂。
 * 目前只有「小号强调按钮」：橙色 accent 样式 + 自定义内边距，避免每个区块各写一遍。
 */
final class Buttons {

    private Buttons() {
    }

    /** 小号强调按钮（accent 样式）。 */
    static Button accent(String text, int vPad, int hPad) {
        Button b = new Button(text);
        b.getStyleClass().add("accent");
        b.setStyle("-fx-padding: " + vPad + " " + hPad + ";");
        return b;
    }
}
