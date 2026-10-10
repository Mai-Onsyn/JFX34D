package HCloudbyte.ui.theme;

import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * 全局 UI 主题（Blender / IDE 深色风格）：深灰面板 + 橙色强调。
 *
 * <p>这里只放两类东西：
 * <ul>
 *   <li><b>样式字符串常量</b>（面板底、分段控件底、按钮态、文本配色）——想改颜色/圆角，只改这里；</li>
 *   <li><b>通用控件工厂</b>（液态按钮、标题/弱化/提示文本）——避免每个面板各写一遍 {@code setStyle}。</li>
 * </ul>
 * 不放任何业务逻辑。
 */
public final class Theme {

    private Theme() {
    }

    /* ==================== 面板 / 控件样式 ==================== */

    /** 深色玻璃拟态面板（Blender 面板灰）。 */
    public static final String GLASS =
            "-fx-background-color: rgba(48,52,57,0.92);"
                    + "-fx-background-radius: 20;"
                    + "-fx-border-color: rgba(110,116,124,0.6);"
                    + "-fx-border-radius: 20;"
                    + "-fx-border-width: 1;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 28, 0.15, 0, 10);";

    /** 分段控件容器底（轻微弧度）。 */
    public static final String SEGMENT_BG =
            "-fx-background-color: rgba(38,42,47,0.95);"
                    + "-fx-background-radius: 6;"
                    + "-fx-border-color: rgba(110,116,124,0.7);"
                    + "-fx-border-radius: 6;"
                    + "-fx-border-width: 1;";

    /** 液态按钮默认态（轻微弧度）。 */
    public static final String LIQUID_BTN =
            "-fx-background-color: linear-gradient(to bottom, rgba(72,77,83,0.95), rgba(50,54,59,0.95));"
                    + "-fx-background-radius: 6;"
                    + "-fx-border-color: rgba(110,116,124,0.85);"
                    + "-fx-border-radius: 6;"
                    + "-fx-border-width: 1;"
                    + "-fx-padding: 8 16;"
                    + "-fx-text-fill: #d6d9dc;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.12, 0, 2);";

    /** 液态按钮悬停态（轻微弧度）。 */
    public static final String LIQUID_BTN_HOVER =
            "-fx-background-color: linear-gradient(to bottom, rgba(96,102,110,0.95), rgba(66,71,77,0.95));"
                    + "-fx-background-radius: 6;"
                    + "-fx-border-color: rgba(140,146,154,0.95);"
                    + "-fx-border-radius: 6;"
                    + "-fx-border-width: 1;"
                    + "-fx-padding: 8 16;"
                    + "-fx-text-fill: #f2f3f5;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 14, 0.15, 0, 4);";

    /* ==================== 文本配色 ==================== */

    public static final String TEXT_TITLE = "#e8eaed";   // 标题（亮）
    public static final String TEXT_BODY = "#c7ccd1";    // 正文（次亮）
    public static final String TEXT_MUTED = "#8a8f94";   // 弱化说明
    public static final String TEXT_DIM = "#9aa0a6";     // 更弱（状态栏等）
    public static final String TEXT_FAINT = "#6a7076";   // 最弱（分隔前缀）

    public static final String ACCENT = "#ff8a1a";       // Blender 橙（主强调）
    public static final String SUCCESS = "#7fd08a";      // 成功 / 在线
    public static final String WARNING = "#ffb36b";      // 警告 / 4D
    public static final String ERROR = "#e06c75";        // 错误
    public static final String INFO = "#8ab4f8";         // 信息 / 蓝

    /* ==================== 通用控件工厂 ==================== */

    /** 液态按钮：带默认/悬停样式切换。 */
    public static Button buildLiquidButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(LIQUID_BTN);
        btn.setCursor(Cursor.HAND);
        btn.setOnMouseEntered(e -> btn.setStyle(LIQUID_BTN_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(LIQUID_BTN));
        return btn;
    }

    /** 面板标题文本（13px 粗体亮色）。 */
    public static Label titleLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_TITLE + ";");
        return label;
    }

    /** 弱化文本（灰色）。 */
    public static Label mutedLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        return label;
    }

    /** 区块内小提示（11px 灰）。 */
    public static Label hintLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");
        return label;
    }

    /** 行首灰色小前缀（状态栏分隔用）。 */
    public static Label prefixLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + TEXT_FAINT + ";");
        return label;
    }
}
