package HCloudbyte.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.ogl3d.data.Light;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * 顶部栏：标题、文件名、投影切换、输入模式切换、Agent 状态、渲染设置。
 *
 * <p>【接口接线点】（AI Api Interface document v2.md §0.1）
 * <ul>
 *   <li>保存 / 打开（将来加按钮）→ {@code IOInterface#saveModel(name, fileName)} / {@code #loadModel(name, file)}（§7）</li>
 *   <li>投影切换（透视/正交）：渲染侧投影模式，RendererInterface 无对应操作，走渲染设置回调</li>
 *   <li>Agent 状态：AgentService 上线/下线时更新（§2 对话流程）</li>
 *   <li>渲染设置：渲染参数（FPS、顶点数等），对接 StatusBar / GL3DRegion</li>
 * </ul>
 */
public class TopBar extends StackPane {

    /**
     * @param renderer 渲染接口（设置弹窗调 SceneInterface）
     * @param region 渲染视口（3D/4D 模式切换走 GL4DRegion 内置 I 键逻辑）
     * @param onInputModeChange 输入模式切换回调（0 = 手写模式，1 = 语音模式）
     */
    public TopBar(RendererInterface renderer, GL4DRegion region, IntConsumer onInputModeChange) {
        Label title = new Label("JFX 34D");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");

        Label file = new Label("scene.4do");
        file.setStyle("-fx-text-fill: #8a8f94;");

        Separator sep = new Separator(Orientation.VERTICAL);

        HBox projectionSwitch = UiTheme.buildSegmentedControl(
                new String[]{"透视投影", "正交投影"}, 0);
        // 接口接线：投影模式切换 → 渲染设置回调（RendererInterface 无对应操作）

        HBox leftBox = new HBox(12, title, file, sep, projectionSwitch);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        // 3D/4D 输入模式切换：直接调 SceneInterface.setEnable4DInput（互斥：4D=true / 3D=false）。
        // 键盘 I 键（渲染侧 GL4DRegion 内置）切换时，经 setOnEnable4DInputChanged 回调回同步 UI。
        UiTheme.Segmented dimSwitch = new UiTheme.Segmented(
                new String[]{"3D", "4D"},
                region.getEnableInput() ? 0 : 1,
                i -> renderer.getScene().setEnable4DInput(i == 1));
        renderer.getScene().setOnEnable4DInputChanged(fourD -> {
            dimSwitch.select(fourD ? 1 : 0);
            return kotlin.Unit.INSTANCE;
        });

        HBox inputModeSwitch = UiTheme.buildSegmentedControl(
                new String[]{"手写模式", "语音模式"}, 0, onInputModeChange);

        Label agentStatus = new Label("● Agent 在线");
        agentStatus.setStyle("-fx-text-fill: #7fd08a;");
        // 接口接线：Agent 状态 → AgentService 生命周期（可用 send() 后置为在线）

        Button renderSettings = UiTheme.buildLiquidButton("设置");
        // 接口调用：点击 → 场景设置弹窗（光照/背景色/视口大小/光源/FPS上限，SceneInterface ✅）
        renderSettings.setOnAction(e -> showSettingsDialog(renderer));

        HBox rightBox = new HBox(12, dimSwitch.box, inputModeSwitch, agentStatus, renderSettings);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(leftBox, spacer, rightBox);
        content.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(content);
        setPadding(new Insets(12, 20, 12, 20));
        setStyle(UiTheme.GLASS);
    }

    /** 场景设置弹窗：光照渲染 / 背景色 / 4D 视口大小 / 光源列表与删除 / FPS 上限（SceneInterface ✅）。 */
    private void showSettingsDialog(RendererInterface renderer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("设置");
        dialog.setHeaderText("场景设置");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        // 光照渲染开关
        CheckBox light = new CheckBox("启用光照渲染");
        light.setOnAction(e -> renderer.getScene().enableLightRendering(light.isSelected()));
        grid.addRow(0, light);

        // 背景色
        ColorPicker bg = new ColorPicker(Color.color(0.25, 0.25, 0.25));
        bg.setPrefWidth(140);
        Button bgBtn = new Button("应用背景色");
        bgBtn.setOnAction(e -> renderer.getScene().setBackgroundColor(bg.getValue()));
        grid.addRow(1, new Label("背景色"), new HBox(6, bg, bgBtn));

        // 4D 视口大小（边长）
        TextField display = new TextField("1.0");
        display.setPrefWidth(60);
        Button dsBtn = new Button("应用视口大小");
        dsBtn.setOnAction(e -> {
            try {
                renderer.getScene().setDisplaySize(Float.parseFloat(display.getText().trim()));
            } catch (NumberFormatException ignored) { }
        });
        grid.addRow(2, new Label("视口大小"), new HBox(6, display, dsBtn));

        // 光源：刷新 / 删除
        Label lights = new Label();
        lights.setWrapText(true);
        lights.setPrefWidth(280);
        lights.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        Button refreshL = new Button("刷新光源");
        refreshL.setOnAction(e -> lights.setText(lightNames(renderer.getScene()  .listLights())));
        Button addBtn = new Button("添加光源");
        addBtn.setOnAction(e -> showAddLightDialog(renderer, lights));
        TextField delLight = new TextField();
        delLight.setPromptText("光源名（逗号分隔）");
        delLight.setPrefWidth(130);
        Button delBtn = new Button("删除");
        delBtn.setOnAction(e -> {
            String t = delLight.getText().trim();
            if (t.isEmpty()) return;
            renderer.getScene().removeLights(List.of(t.split("[,，]")));
            lights.setText(lightNames(renderer.getScene().listLights()));
        });
        grid.addRow(3, new Label("光源"), refreshL);
        grid.addRow(4, new HBox(6, addBtn, delLight, delBtn));
        grid.addRow(5, lights);

        // FPS 上限
        TextField fps3d = new TextField("60");
        fps3d.setPrefWidth(60);
        TextField fps4d = new TextField("60");
        fps4d.setPrefWidth(60);
        Button fpsBtn = new Button("应用FPS上限");
        fpsBtn.setOnAction(e -> {
            try {
                renderer.getScene().set3DMaxFPS(Float.parseFloat(fps3d.getText().trim()));
                renderer.getScene().set4DMaxFPS(Float.parseFloat(fps4d.getText().trim()));
            } catch (NumberFormatException ignored) { }
        });
        grid.addRow(6, new Label("FPS上限(3D/4D)"), new HBox(6, fps3d, fps4d, fpsBtn));

        // 相机 FOV（3D/4D，0~90°）：拖动即时应用（SceneInterface.set3D/4DCameraFov ✅）。
        // 说明：接口只有 set 无 get，弹窗打开时无法回显当前值，初始为默认（3D 60° / 4D 70°）。
        Slider fov3d = new Slider(10, 90, 60);
        Slider fov4d = new Slider(10, 90, 70);
        fov3d.setPrefWidth(160);
        fov4d.setPrefWidth(160);
        Label fov3dVal = new Label("60°");
        Label fov4dVal = new Label("70°");
        fov3dVal.setPrefWidth(36);
        fov4dVal.setPrefWidth(36);
        fov3d.valueProperty().addListener((o, a, b) -> {
            fov3dVal.setText(String.format("%.0f°", b.doubleValue()));
            renderer.getScene().set3DCameraFov((float) b.doubleValue());
        });
        fov4d.valueProperty().addListener((o, a, b) -> {
            fov4dVal.setText(String.format("%.0f°", b.doubleValue()));
            renderer.getScene().set4DCameraFov((float) b.doubleValue());
        });
        grid.addRow(7, new Label("FOV(3D)"), new HBox(6, fov3d, fov3dVal));
        grid.addRow(8, new Label("FOV(4D)"), new HBox(6, fov4d, fov4dVal));

        // 线框渲染开关（SceneInterface.enableTriangleLineRendering ✅），默认启用
        CheckBox wire = new CheckBox("启用线框渲染");
        wire.setSelected(true);
        wire.setOnAction(e -> renderer.getScene().enableTriangleLineRendering(wire.isSelected()));
        grid.addRow(9, wire);

        // 相机速度倍率（3D/4D 移动+旋转，4 个）：拖动即时应用（SceneInterface ✅，默认 1.0）
        Slider[] speedSliders = new Slider[4];
        Label[] speedVals = new Label[4];
        String[] speedNames = {"3D移动", "3D旋转", "4D移动", "4D旋转"};
        Runnable[] speedApps = {
                () -> renderer.getScene().set3DCameraMoveSpeed((float) speedSliders[0].getValue()),
                () -> renderer.getScene().set3DCameraRotateSpeed((float) speedSliders[1].getValue()),
                () -> renderer.getScene().set4DCameraMoveSpeed((float) speedSliders[2].getValue()),
                () -> renderer.getScene().set4DCameraRotateSpeed((float) speedSliders[3].getValue())
        };
        for (int i = 0; i < 4; i++) {
            speedSliders[i] = new Slider(0.1, 5.0, 1.0);
            speedSliders[i].setPrefWidth(160);
            speedVals[i] = new Label("1.0x");
            speedVals[i].setPrefWidth(40);
            final int idx = i;
            speedSliders[i].valueProperty().addListener((o, a, b) -> {
                speedVals[idx].setText(String.format("%.1fx", b.doubleValue()));
                speedApps[idx].run();
            });
            grid.addRow(10 + i, new Label("速度(" + speedNames[i] + ")"), new HBox(6, speedSliders[i], speedVals[i]));
        }

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    /** 添加光源弹窗：名字/位置/颜色/强度 → Light（Java 可 new，setColor 走 Float 重载避开 ColorARGB）。 */
    private void showAddLightDialog(RendererInterface renderer, Label lights) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("添加光源");
        dialog.setHeaderText("添加点光源（位置为世界坐标）");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField name = new TextField("PointLight");
        TextField x = new TextField("0");
        x.setPrefWidth(56);
        TextField y = new TextField("50");
        y.setPrefWidth(56);
        TextField z = new TextField("0");
        z.setPrefWidth(56);
        ColorPicker color = new ColorPicker(Color.WHITE);
        TextField intensity = new TextField("0.6");
        intensity.setPrefWidth(56);
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new Insets(12));
        g.addRow(0, new Label("名字"), name);
        g.addRow(1, new Label("位置"), new HBox(6, x, y, z));
        g.addRow(2, new Label("颜色"), color);
        g.addRow(3, new Label("强度"), intensity);
        dialog.getDialogPane().setContent(g);
        dialog.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;
            try {
                Light l = new Light();
                l.setName(name.getText().trim().isEmpty() ? "PointLight" : name.getText().trim());
                l.moveTo(
                        Float.parseFloat(x.getText().trim()),
                        Float.parseFloat(y.getText().trim()),
                        Float.parseFloat(z.getText().trim()));
                javafx.scene.paint.Color c = color.getValue();
                l.setColor((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(), 1f);
                l.setIntensity(Float.parseFloat(intensity.getText().trim()));
                renderer.getScene().addLights(l);  lights.setText(lightNames(renderer.getScene().listLights()));
            } catch (NumberFormatException ignored) { }
        });
    }

    /** listLights() 返回 List<Light>，转成名字逗号分隔文本（Label.setText 需要 String）。 */
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