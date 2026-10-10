package HCloudbyte.ui.section;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

/**
 * 4D 六平面选择器：一排 {@code XY / XZ / XW / YZ / YW / ZW} 的互斥按钮（两行三列）。
 * 模型旋转（{@link RotateSection}）与相机旋转（{@link CameraSection}）都用它，避免重复代码。
 */
final class PlaneSelector extends GridPane {

    private static final String[] PLANES = {"XY", "XZ", "XW", "YZ", "YW", "ZW"};

    private String selected = PLANES[0];

    PlaneSelector() {
        setHgap(8);
        setVgap(8);
        ToggleGroup group = new ToggleGroup();
        for (int i = 0; i < PLANES.length; i++) {
            ToggleButton btn = new ToggleButton(PLANES[i]);
            btn.setToggleGroup(group);
            btn.getStyleClass().add("accent");
            btn.setStyle("-fx-padding: 2 10;");
            btn.setOnAction(e -> selected = btn.getText());
            add(btn, i % 3, i / 3);
            if (i == 0) btn.setSelected(true);
        }
    }

    /** 当前选中的平面名（默认 XY）。 */
    String getSelected() {
        return selected;
    }
}
