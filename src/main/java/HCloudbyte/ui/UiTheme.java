package HCloudbyte.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.function.IntConsumer;

/**
 * 全局 UI 主题（Blender / IDE 深色风格）：深灰面板 + 橙色强调。
 */
public final class UiTheme {

    private UiTheme() {}

    /** 深色玻璃拟态面板（Blender 面板灰）。 */
    public static final String GLASS =
            "-fx-background-color: rgba(48,52,57,0.92);" +
                    "-fx-background-radius: 20;" +
                    "-fx-border-color: rgba(110,116,124,0.6);" +
                    "-fx-border-radius: 20;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 28, 0.15, 0, 10);";

    /** 分段控件容器底（轻微弧度）。 */
    public static final String SEGMENT_BG =
            "-fx-background-color: rgba(38,42,47,0.95);" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: rgba(110,116,124,0.7);" +
                    "-fx-border-radius: 6;" +
                    "-fx-border-width: 1;";

    /** 液态按钮默认态（轻微弧度）。 */
    public static final String LIQUID_BTN =
            "-fx-background-color: linear-gradient(to bottom, rgba(72,77,83,0.95), rgba(50,54,59,0.95));" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: rgba(110,116,124,0.85);" +
                    "-fx-border-radius: 6;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 8 16;" +
                    "-fx-text-fill: #d6d9dc;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.12, 0, 2);";

    /** 液态按钮悬停态（轻微弧度）。 */
    public static final String LIQUID_BTN_HOVER =
            "-fx-background-color: linear-gradient(to bottom, rgba(96,102,110,0.95), rgba(66,71,77,0.95));" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: rgba(140,146,154,0.95);" +
                    "-fx-border-radius: 6;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 8 16;" +
                    "-fx-text-fill: #f2f3f5;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 14, 0.15, 0, 4);";

    /** 液态按钮：带默认/悬停样式切换。 */
    public static Button buildLiquidButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(LIQUID_BTN);
        btn.setCursor(Cursor.HAND);
        btn.setOnMouseEntered(e -> btn.setStyle(LIQUID_BTN_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(LIQUID_BTN));
        return btn;
    }

    /** 分段选择控件（无回调）。 */
    public static HBox buildSegmentedControl(String[] options, int defaultIndex) {
        return buildSegmentedControl(options, defaultIndex, null);
    }

    /** 分段选择控件，选中时回调 [onChange]（参数为选中下标）。 */
    public static HBox buildSegmentedControl(String[] options, int defaultIndex,
                                             IntConsumer onChange) {
        ToggleGroup group = new ToggleGroup();
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setStyle(SEGMENT_BG);
        box.setPadding(new Insets(3));

        String normalStyle =
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 18;" +
                        "-fx-text-fill: #9aa0a6;";

        String selectedStyle =
                "-fx-background-color: rgba(255,138,26,0.92);" +   // Blender 橙
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 18;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0.1, 0, 2);";

        ToggleButton[] buttons = new ToggleButton[options.length];
        for (int i = 0; i < options.length; i++) {
            ToggleButton btn = new ToggleButton(options[i]);
            btn.setToggleGroup(group);
            btn.setStyle(normalStyle);
            final int idx = i;
            btn.setOnAction(e -> {
                if (onChange != null) onChange.accept(idx);
            });
            buttons[i] = btn;
            box.getChildren().add(btn);
        }

        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (oldT instanceof ToggleButton tb) tb.setStyle(normalStyle);
            if (newT instanceof ToggleButton tb) tb.setStyle(selectedStyle);
        });

        buttons[defaultIndex].setSelected(true);
        buttons[defaultIndex].setStyle(selectedStyle);
        return box;
    }
}
