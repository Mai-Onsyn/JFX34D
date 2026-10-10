package HCloudbyte.ui.theme;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.function.IntConsumer;

/**
 * 分段选择控件（一排互斥的 ToggleButton），橙色高亮当前项。
 *
 * <p>两种用法：
 * <ul>
 *   <li>只要「选中回调」：{@code new SegmentedControl(options, defaultIndex, onChange)}；</li>
 *   <li>还需要「程序化选中」（例如渲染侧键盘 I 键切回同步）：调用 {@link #select(int)}，
 *       它只切选中态与样式、<b>不触发 onChange</b>，避免回调循环。</li>
 * </ul>
 */
public final class SegmentedControl extends HBox {

    private static final String NORMAL_STYLE =
            "-fx-background-color: transparent;"
                    + "-fx-background-radius: 6;"
                    + "-fx-padding: 6 18;"
                    + "-fx-text-fill: " + Theme.TEXT_DIM + ";";

    private static final String SELECTED_STYLE =
            "-fx-background-color: rgba(255,138,26,0.92);"   // Blender 橙
                    + "-fx-background-radius: 6;"
                    + "-fx-padding: 6 18;"
                    + "-fx-text-fill: #ffffff;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0.1, 0, 2);";

    private final ToggleButton[] buttons;

    /**
     * @param options      选项文案
     * @param defaultIndex 默认选中下标
     * @param onChange     选中回调（可为 null，参数为选中下标）
     */
    public SegmentedControl(String[] options, int defaultIndex, IntConsumer onChange) {
        super();
        setAlignment(Pos.CENTER);
        setStyle(Theme.SEGMENT_BG);
        setPadding(new Insets(3));

        ToggleGroup group = new ToggleGroup();
        buttons = new ToggleButton[options.length];
        for (int i = 0; i < options.length; i++) {
            ToggleButton btn = new ToggleButton(options[i]);
            btn.setToggleGroup(group);
            btn.setStyle(NORMAL_STYLE);
            final int idx = i;
            btn.setOnAction(e -> {
                if (onChange != null) onChange.accept(idx);
            });
            buttons[i] = btn;
            getChildren().add(btn);
        }

        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (oldT instanceof ToggleButton tb) tb.setStyle(NORMAL_STYLE);
            if (newT instanceof ToggleButton tb) tb.setStyle(SELECTED_STYLE);
        });

        buttons[defaultIndex].setSelected(true);
        buttons[defaultIndex].setStyle(SELECTED_STYLE);
    }

    /** 便捷构造：无回调。 */
    public SegmentedControl(String[] options, int defaultIndex) {
        this(options, defaultIndex, null);
    }

    /** 当前选中下标（无选中时返回 0）。 */
    public int getSelectedIndex() {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isSelected()) return i;
        }
        return 0;
    }

    /** 程序化选中（只切选中态与样式，不触发 onChange）。 */
    public void select(int index) {
        if (index >= 0 && index < buttons.length) buttons[index].setSelected(true);
    }
}
