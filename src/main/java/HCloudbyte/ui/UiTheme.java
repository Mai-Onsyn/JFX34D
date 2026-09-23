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
 * 全局 UI 主题：玻璃拟态样式常量 + 通用控件工厂。
 */
public final class UiTheme {

    private UiTheme() {}

    /** 玻璃拟态面板。 */
    public static final String GLASS =
            "-fx-background-color: rgba(255,255,255,0.42);" +
                    "-fx-background-radius: 20;" +
                    "-fx-border-color: rgba(255,255,255,0.75);" +
                    "-fx-border-radius: 20;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(31,41,55,0.18), 28, 0.15, 0, 10);";

    /** 分段控件容器底。 */
    public static final String SEGMENT_BG =
            "-fx-background-color: rgba(255,255,255,0.45);" +
                    "-fx-background-radius: 20;" +
                    "-fx-border-color: rgba(255,255,255,0.9);" +
                    "-fx-border-radius: 20;" +
                    "-fx-border-width: 1;";

    /** 液态按钮默认态。 */
    public static final String LIQUID_BTN =
            "-fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.92), rgba(255,255,255,0.58));" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(255,255,255,0.95);" +
                    "-fx-border-radius: 18;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 8 16;" +
                    "-fx-text-fill: #333;" +
                    "-fx-effect: dropshadow(gaussian, rgba(31,41,55,0.14), 10, 0.12, 0, 2);";

    /** 液态按钮悬停态。 */
    public static final String LIQUID_BTN_HOVER =
            "-fx-background-color: linear-gradient(to bottom, rgba(255,255,255,1.0), rgba(255,255,255,0.75));" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(255,255,255,1.0);" +
                    "-fx-border-radius: 18;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 8 16;" +
                    "-fx-text-fill: #111;" +
                    "-fx-effect: dropshadow(gaussian, rgba(31,41,55,0.22), 14, 0.15, 0, 4);";

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
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 6 18;" +
                        "-fx-text-fill: #555;";

        String selectedStyle =
                "-fx-background-color: rgba(255,255,255,0.95);" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 6 18;" +
                        "-fx-text-fill: #111;" +
                        "-fx-effect: dropshadow(gaussian, rgba(31,41,55,0.18), 8, 0.1, 0, 2);";

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