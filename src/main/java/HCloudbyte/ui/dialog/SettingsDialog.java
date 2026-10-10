package HCloudbyte.ui.dialog;

import HCloudbyte.ui.viewport.ViewportBox;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import mai_onsyn.renderer.cpu4dkt.CameraOrientation;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.interfaces.SceneInterface;
import mai_onsyn.renderer.ogl3d.data.Light;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 场景设置弹窗：光照 / 背景色 / 视口大小 / 视口区域方块 / 光源列表与增删 / FPS 上限 / FOV / 线框 /
 * 相机速度 / 环境光，以及只读的 4D 相机姿态角与 1% Low FPS。
 *
 * <p>所有可写控件的初始值都取自 {@link SettingsCache}（上次应用/拖动的值），不再每次重置成默认。
 */
public final class SettingsDialog {

    private SettingsDialog() {
    }

    /** 打开「场景设置」弹窗（阻塞）。 */
    public static void show(RendererInterface renderer, SettingsCache cache, ViewportBox viewportBox) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("设置");
        dialog.setHeaderText("场景设置");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        int row = 0;
        row = addLightSwitch(grid, row, renderer, cache);
        row = addBackgroundColor(grid, row, renderer, cache);
        row = addDisplaySize(grid, row, renderer, cache, viewportBox);
        row = addViewportBoxSwitch(grid, row, viewportBox);
        row = addLightSourceRows(grid, row, renderer);
        row = addFpsSection(grid, row, renderer, cache);
        row = addFovSection(grid, row, renderer, cache);
        row = addWireSwitch(grid, row, renderer, cache);
        row = addSpeedSliders(grid, row, renderer, cache);
        row = addAmbientColor(grid, row, renderer, cache);
        row = addOrientationReadout(grid, row, renderer);
        addLowFpsReadout(grid, row, renderer);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    /* ==================== 各设置行 ==================== */

    private static int addLightSwitch(GridPane grid, int row, RendererInterface renderer, SettingsCache cache) {
        CheckBox light = new CheckBox("启用光照渲染");
        light.setSelected(cache.light);
        light.setOnAction(e -> {
            cache.light = light.isSelected();
            renderer.getScene().enableLightRendering(light.isSelected());
        });
        grid.addRow(row, light);
        return row + 1;
    }

    private static int addBackgroundColor(GridPane grid, int row, RendererInterface renderer, SettingsCache cache) {
        ColorPicker bg = new ColorPicker(cache.backgroundColor);
        bg.setPrefWidth(140);
        Button apply = new Button("应用背景色");
        apply.setMinWidth(Region.USE_PREF_SIZE);
        apply.setOnAction(e -> {
            cache.backgroundColor = bg.getValue();
            renderer.getScene().setBackgroundColor(bg.getValue());
        });
        grid.addRow(row, new Label("背景色"), new HBox(6, bg, apply));
        return row + 1;
    }

    private static int addDisplaySize(GridPane grid, int row, RendererInterface renderer, SettingsCache cache,
                                      ViewportBox viewportBox) {
        TextField display = new TextField(String.valueOf(cache.displaySize));
        display.setPrefWidth(60);
        Button apply = new Button("应用视口大小");
        apply.setMinWidth(Region.USE_PREF_SIZE);
        apply.setOnAction(e -> {
            try {
                cache.displaySize = Double.parseDouble(display.getText().trim());
                renderer.getScene().setDisplaySize((float) cache.displaySize);
                // 指示方块跟着视口一起变，否则方块就不再代表真实视口范围
                viewportBox.setSize((float) cache.displaySize);
            } catch (NumberFormatException ignored) {
            }
        });
        grid.addRow(row, new Label("视口大小"), new HBox(6, display, apply));
        return row + 1;
    }

    /** 视口区域指示方块：半透明正方体开关，控制它是否显示在视口里。 */
    private static int addViewportBoxSwitch(GridPane grid, int row, ViewportBox viewportBox) {
        CheckBox showBox = new CheckBox("显示视口区域（半透明方框）");
        showBox.setSelected(viewportBox.isVisible());
        showBox.setOnAction(e -> viewportBox.setVisible(showBox.isSelected()));
        grid.addRow(row, showBox);
        return row + 1;
    }

    /** 光源：添加 / 刷新 / 清单 / 删除 + 当前列表。 */
    private static int addLightSourceRows(GridPane grid, int row, RendererInterface renderer) {
        Label lights = new Label();
        lights.setWrapText(true);
        lights.setPrefWidth(280);
        lights.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");

        Button refresh = new Button("刷新光源");
        refresh.setMinWidth(Region.USE_PREF_SIZE);
        refresh.setOnAction(e -> lights.setText(lightNames(renderer.getScene().listLights())));

        Button list = new Button("清单");
        list.setMinWidth(Region.USE_PREF_SIZE);
        list.setOnAction(e -> lights.setText(renderer.getScene().listLight()));

        Button add = new Button("添加光源");
        add.setMinWidth(Region.USE_PREF_SIZE);
        add.setOnAction(e -> {
            AddLightDialog.show(renderer);
            lights.setText(lightNames(renderer.getScene().listLights()));
        });

        TextField delField = new TextField();
        delField.setPromptText("光源名（逗号分隔）");
        delField.setPrefWidth(130);
        Button del = new Button("删除");
        del.setMinWidth(Region.USE_PREF_SIZE);
        del.setOnAction(e -> {
            String text = delField.getText().trim();
            if (text.isEmpty()) return;
            renderer.getScene().removeLights(List.of(text.split("[,，]")));
            lights.setText(lightNames(renderer.getScene().listLights()));
        });

        grid.addRow(row, new Label("光源"), new HBox(6, add, refresh, list));
        grid.addRow(row + 1, new HBox(6, delField, del));
        grid.addRow(row + 2, lights);
        return row + 3;
    }

    private static int addFpsSection(GridPane grid, int row, RendererInterface renderer, SettingsCache cache) {
        TextField fps3d = new TextField();
        fps3d.setPrefWidth(60);
        fps3d.setPromptText("如 120");
        TextField fps4d = new TextField();
        fps4d.setPrefWidth(60);
        fps4d.setPromptText("如 120");
        Button apply = new Button("应用FPS上限");
        apply.setMinWidth(Region.USE_PREF_SIZE);
        apply.setOnAction(e -> {
            try {
                cache.fps3d = Double.parseDouble(fps3d.getText().trim());
                cache.fps4d = Double.parseDouble(fps4d.getText().trim());
                renderer.getScene().set3DMaxFPS((float) cache.fps3d);
                renderer.getScene().set4DMaxFPS((float) cache.fps4d);
            } catch (NumberFormatException ignored) {
            }
        });
        grid.addRow(row, new Label("FPS上限(3D/4D)"), new HBox(6, fps3d, fps4d, apply));
        return row + 1;
    }

    /** 相机 FOV（3D/4D, 10~90°）：拖动即时应用 + 更新缓存。 */
    private static int addFovSection(GridPane grid, int row, RendererInterface renderer, SettingsCache cache) {
        Slider fov3d = new Slider(10, 90, cache.fov3d);
        Slider fov4d = new Slider(10, 90, cache.fov4d);
        fov3d.setPrefWidth(160);
        fov4d.setPrefWidth(160);
        Label fov3dVal = new Label(String.format("%.0f°", cache.fov3d));
        Label fov4dVal = new Label(String.format("%.0f°", cache.fov4d));
        fov3dVal.setPrefWidth(36);
        fov4dVal.setPrefWidth(36);
        fov3d.valueProperty().addListener((o, a, b) -> {
            cache.fov3d = b.doubleValue();
            fov3dVal.setText(String.format("%.0f°", b.doubleValue()));
            renderer.getScene().set3DCameraFov((float) b.doubleValue());
        });
        fov4d.valueProperty().addListener((o, a, b) -> {
            cache.fov4d = b.doubleValue();
            fov4dVal.setText(String.format("%.0f°", b.doubleValue()));
            renderer.getScene().set4DCameraFov((float) b.doubleValue());
        });
        grid.addRow(row, new Label("FOV(3D)"), new HBox(6, fov3d, fov3dVal));
        grid.addRow(row + 1, new Label("FOV(4D)"), new HBox(6, fov4d, fov4dVal));
        return row + 2;
    }

    private static int addWireSwitch(GridPane grid, int row, RendererInterface renderer, SettingsCache cache) {
        CheckBox wire = new CheckBox("启用线框渲染");
        wire.setSelected(cache.wire);
        wire.setOnAction(e -> {
            cache.wire = wire.isSelected();
            renderer.getScene().enableTriangleLineRendering(wire.isSelected());
        });
        grid.addRow(row, wire);
        return row + 1;
    }

    /** 相机速度倍率：3D/4D 的移动+旋转，共 4 个滑条。 */
    private static int addSpeedSliders(GridPane grid, int row, RendererInterface renderer, SettingsCache cache) {
        Slider[] sliders = new Slider[4];
        Label[] values = new Label[4];
        String[] names = {"3D移动", "3D旋转", "4D移动", "4D旋转"};
        Runnable[] apply = {
                () -> renderer.getScene().set3DCameraMoveSpeed((float) sliders[0].getValue()),
                () -> renderer.getScene().set3DCameraRotateSpeed((float) sliders[1].getValue()),
                () -> renderer.getScene().set4DCameraMoveSpeed((float) sliders[2].getValue()),
                () -> renderer.getScene().set4DCameraRotateSpeed((float) sliders[3].getValue())
        };
        for (int i = 0; i < 4; i++) {
            sliders[i] = new Slider(0.1, 5.0, cache.speed[i]);
            sliders[i].setPrefWidth(160);
            values[i] = new Label(String.format("%.1fx", cache.speed[i]));
            values[i].setPrefWidth(40);
            final int idx = i;
            sliders[i].valueProperty().addListener((o, a, b) -> {
                cache.speed[idx] = b.doubleValue();
                values[idx].setText(String.format("%.1fx", b.doubleValue()));
                apply[idx].run();
            });
            grid.addRow(row + i, new Label("速度(" + names[i] + ")"), new HBox(6, sliders[i], values[i]));
        }
        return row + 4;
    }

    /**
     * 环境光：整个场景一份，参数是 ColorARGB。
     * ColorARGB 是 {@code @JvmInline value class}，Kotlin 编译后方法名被 mangle 成
     * {@code setAmbientLight-SywUG-4(int)}，Java 源码写不出来，只能反射调用。
     */
    private static int addAmbientColor(GridPane grid, int row, RendererInterface renderer, SettingsCache cache) {
        ColorPicker ambient = new ColorPicker(cache.ambient);
        ambient.setPrefWidth(140);
        Button apply = new Button("应用环境光");
        apply.setMinWidth(Region.USE_PREF_SIZE);
        apply.setOnAction(e -> {
            try {
                cache.ambient = ambient.getValue();
                javafx.scene.paint.Color c = ambient.getValue();
                int hex = (0xFF << 24) | ((int) (c.getRed() * 255 + 0.5) << 16)
                        | ((int) (c.getGreen() * 255 + 0.5) << 8) | (int) (c.getBlue() * 255 + 0.5);
                Method setAmbient = SceneInterface.class.getMethod("setAmbientLight-SywUG-4", int.class);
                setAmbient.invoke(renderer.getScene(), hex);
            } catch (Exception ex) {
                throw new RuntimeException("setAmbientLight 调用失败", ex);
            }
        });
        grid.addRow(row, new Label("环境光"), new HBox(6, ambient, apply));
        return row + 1;
    }

    /** 4D 相机姿态角（只读，6 个平面相位角）。 */
    private static int addOrientationReadout(GridPane grid, int row, RendererInterface renderer) {
        Label orient = new Label();
        orient.setWrapText(true);
        orient.setPrefWidth(340);
        orient.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        Runnable refresh = () -> {
            CameraOrientation o = renderer.getScene().get4DCameraOrientation();
            orient.setText(String.format("XY %.1f° | XZ %.1f° | YZ %.1f° | XW %.1f° | YW %.1f° | ZW %.1f°",
                    o.getXy(), o.getXz(), o.getYz(), o.getXw(), o.getYw(), o.getZw()));
        };
        Button btn = new Button("刷新姿态");
        btn.setMinWidth(Region.USE_PREF_SIZE);
        btn.setOnAction(e -> refresh.run());
        refresh.run();
        grid.addRow(row, new Label("4D姿态角"), new HBox(6, btn));
        grid.addRow(row + 1, orient);
        return row + 2;
    }

    /** 1% Low FPS（只读，卡顿指标）。 */
    private static void addLowFpsReadout(GridPane grid, int row, RendererInterface renderer) {
        Label lowFps = new Label();
        lowFps.setPrefWidth(240);
        lowFps.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        Runnable refresh = () -> lowFps.setText(String.format("3D 1%%Low %.1f  |  4D 1%%Low %.1f",
                renderer.getScene().get3D1PercentLowFPS(), renderer.getScene().get4D1PercentLowFPS()));
        Button btn = new Button("刷新1%Low");
        btn.setMinWidth(Region.USE_PREF_SIZE);
        btn.setOnAction(e -> refresh.run());
        refresh.run();
        grid.addRow(row, new Label("1%Low FPS"), new HBox(6, btn));
        grid.addRow(row + 1, lowFps);
    }

    /** listLights() 返回 List&lt;Light&gt;，转成名字顿号分隔文本。 */
    private static String lightNames(List<Light> list) {
        if (list == null || list.isEmpty()) return "（无光源）";
        StringBuilder sb = new StringBuilder();
        for (Light l : list) {
            if (sb.length() > 0) sb.append("、");
            sb.append(l.getName());
        }
        return sb.toString();
    }
}
