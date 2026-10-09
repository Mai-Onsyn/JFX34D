package HCloudbyte.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import mai_onsyn.renderer.cpu4dkt.CameraOrientation;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Coordinate4D;
import mai_onsyn.renderer.utils.Direction;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 右侧属性视图（手写模式）：场景集合、变换、摄像机位置、四面体详情（紧凑 Blender 风格）。
 */
public class PropertyPanel extends VBox {

    private final RendererInterface renderer;
    /** 当前选中的模型名（SceneOutliner 点击联动）。 */
    private String selectedModel;

    public PropertyPanel(RendererInterface renderer) {
        super(10);
        this.renderer = renderer;
        setPadding(new Insets(16));
        setStyle(UiTheme.GLASS);

        // 默认选中第一个模型（后续由 SceneOutliner 点击切换）
        List<String> models = renderer.getModel().listModel();
        selectedModel = models.isEmpty() ? null : models.getFirst();

        VBox sceneOutliner = new SceneOutliner(renderer, name -> {
            selectedModel = name;
            updateData(name);
        });

        // 折叠区块：等高、紧挨、颜色统一（VBox 间距 0，块间无分隔线）
        TitledPane posPane = fold("变换", buildPositionBox());
        TitledPane rotPane = fold("旋转", buildRotatePart());
        TitledPane scalePane = fold("缩放", buildScalePart());
        TitledPane clipPane = fold("裁剪", buildClipPart());
        TitledPane camPane = fold("摄像机位置", buildCameraBox());
        TitledPane tetPane = fold("四面体详情", buildTetBox());
        VBox foldBox = new VBox(0, posPane, rotPane, scalePane, clipPane, camPane, tetPane);

        getChildren().addAll(
                sceneOutliner,
                new Separator(),
                foldBox,
                new Separator(),
                buildDataBox()
        );

        updateData(selectedModel);
    }

    /** 可折叠区块：背景与面板底色统一（透明透出面板背景），悬停微亮、展开保持透明。 */
    private TitledPane fold(String title, Node content) {
        TitledPane tp = new TitledPane(title, content);
        tp.setAnimated(false);
        tp.setExpanded(false);

        // 🎨 1. 整块默认透明，直接透出面板背景（和背景 100% 同色）；hover 微亮提示；展开用橙色边框提示
        String baseStyle = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 6; -fx-padding: 0; -fx-text-fill: #e8e8e8;";
        String hoverStyle = "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 6; -fx-padding: 0; -fx-text-fill: #e8e8e8;";
        String expandedStyle = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 6; -fx-padding: 0; -fx-text-fill: #e8e8e8;";

        tp.setStyle(baseStyle);

        // 🎨 2. 标题栏 / 内容区 / 箭头统一透明，让面板背景透出（与背景 100% 同色）
        styleFoldParts(tp);
        // 挂到场景后再补一次（未入场景时 applyCss 后 lookup 可能失败 → 内容区残留默认底色）
        Platform.runLater(() -> styleFoldParts(tp));

        // 🎨 4. 鼠标悬停和展开的事件监听（在 TitledPane 级别处理）
        tp.setOnMouseEntered(e -> {
            if (!tp.isExpanded()) tp.setStyle(hoverStyle);
        });
        tp.setOnMouseExited(e -> {
            if (!tp.isExpanded()) tp.setStyle(baseStyle);
        });
        tp.expandedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                tp.setStyle(expandedStyle);
            } else {
                tp.setStyle(baseStyle);
            }
        });

        return tp;
    }
    /** 折叠块内部样式：标题栏/内容区/箭头全部透明（内容区与面板背景同色）。 */
    private void styleFoldParts(TitledPane tp) {
        tp.applyCss();
        tp.layout();
        Node bar = tp.lookup(".title");
        if (bar != null) bar.setStyle("-fx-background-color: transparent; -fx-padding: 7 10 7 10;");
        Node body = tp.lookup(".content");
        if (body != null) body.setStyle("-fx-background-color: transparent; -fx-padding: 4 0 0 0; -fx-border-color: transparent; -fx-border-width: 0;");
        Node arrow = tp.lookup(".arrow");
        if (arrow != null) arrow.setStyle("-fx-background-color: #c7ccd1;");
    }

    /** 变换-平移区块：读取/写回绝对平移（getModelMatrix 最后一列为平移列）。 */
    private VBox buildPositionBox() {
        VBox box = new VBox(6);
        Label title = new Label("平移");
        Button read = new Button("读取");
        Button apply = new Button("应用");
        read.getStyleClass().add("accent");
        apply.getStyleClass().add("accent");
        read.setStyle("-fx-padding: 2 12;");
        apply.setStyle("-fx-padding: 2 12;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox titleRow = new HBox(8, title, spacer, read, apply);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().add(titleRow);
        box.getChildren().add(buildCoordinateRow("位置X", "0.00", 0));
        box.getChildren().add(buildCoordinateRow("Y", "0.00", 1));
        box.getChildren().add(buildCoordinateRow("Z", "0.00", 2));
        box.getChildren().add(buildCoordinateRow("W", "0.00", 3));

        // 模型相对移动（TransformInterface.move ✅）：±X/Y/Z/W，步长 0.5
        Label mvTitle = new Label("相对移动");
        mvTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        box.getChildren().add(mvTitle);
        String[] mvNames = {"X-", "X+", "Y-", "Y+", "Z-", "Z+", "W-", "W+"};
        int[] mvAxis = {0, 0, 1, 1, 2, 2, 3, 3};
        float[] mvDirs = {-0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f};
        GridPane mvGrid = new GridPane();
        mvGrid.setHgap(6);
        mvGrid.setVgap(6);
        for (int i = 0; i < 8; i++) {
            final int axis = mvAxis[i];
            final float d = mvDirs[i];
            Button b = new Button(mvNames[i]);
            b.getStyleClass().add("accent");
            b.setStyle("-fx-padding: 2 8;");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> {
                if (selectedModel == null) return;
                float[] off = {0, 0, 0, 0};
                off[axis] = d;
                renderer.getTransform().move(selectedModel, new Vector4f(off[0], off[1], off[2], off[3]));
            });
            mvGrid.add(b, i % 4, i / 4);
        }
        box.getChildren().add(mvGrid);

        // 接口调用：GET_MODEL_MATRIX（§4.1.4 ✅）：行主序 5×5 矩阵最后一列为平移
        read.setOnAction(e -> {
            if (selectedModel == null) return;
            Matrix5f m = renderer.getTransform().getModelMatrix(selectedModel);
            objFields[0].setText(String.format("%.2f", m.get(4)));
            objFields[1].setText(String.format("%.2f", m.get(9)));
            objFields[2].setText(String.format("%.2f", m.get(14)));
            objFields[3].setText(String.format("%.2f", m.get(19)));
        });

        // 接口调用：SET_MODEL_MATRIX（§4.1.4 ✅）：复制当前矩阵，改写平移列后写回（绝对位置）
        apply.setOnAction(e -> {
            if (selectedModel == null) return;
            try {
                float[] d = renderer.getTransform().getModelMatrix(selectedModel).getData().clone();
                d[4] = Float.parseFloat(objFields[0].getText().trim());
                d[9] = Float.parseFloat(objFields[1].getText().trim());
                d[14] = Float.parseFloat(objFields[2].getText().trim());
                d[19] = Float.parseFloat(objFields[3].getText().trim());
                renderer.getTransform().setModelMatrix(selectedModel, new Matrix5f(d));
            } catch (NumberFormatException ignored) {
                // 输入非法时忽略，保持原矩阵
            }
        });
        return box;
    }

    /** 变换-旋转区块：模型绕 6 平面旋转（两行三列 + 底部角度应用）；下方实时显示相机旋转角度（键盘视口操作，只读）。 */
    private VBox buildRotatePart() {
        VBox box = new VBox(6);

        Label modelTitle = new Label("模型旋转");
        modelTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");

        ToggleGroup rotGroup = new ToggleGroup();
        String[] planes = {"XY", "XZ", "XW", "YZ", "YW", "ZW"};
        String[] planeSel = {planes[0]};
        GridPane planePane = new GridPane();
        planePane.setHgap(8);
        planePane.setVgap(8);
        for (int i = 0; i < planes.length; i++) {
            ToggleButton btn = new ToggleButton(planes[i]);
            btn.setToggleGroup(rotGroup);
            btn.getStyleClass().add("accent");
            btn.setStyle("-fx-padding: 2 10;");
            btn.setOnAction(e -> planeSel[0] = btn.getText());
            planePane.add(btn, i % 3, i / 3);
        }
        ((ToggleButton) planePane.getChildren().get(0)).setSelected(true);
        box.getChildren().add(planePane);
        TextField rotAngle = new TextField("15");
        rotAngle.setPrefWidth(56);
        rotAngle.setStyle("-fx-padding: 2 6;");
        Button rotBtn = new Button("应用");
        rotBtn.getStyleClass().add("accent");
        rotBtn.setStyle("-fx-padding: 2 10;");
        rotBtn.setOnAction(e -> {
            if (selectedModel == null) return;
            try {
                float a = Float.parseFloat(rotAngle.getText().trim());
                renderer.getTransform().rotate(selectedModel, Direction.Plane.valueOf(planeSel[0]), a);
            } catch (NumberFormatException ignored) {
                // 角度非法时忽略
            }
        });
        HBox rotFoot = new HBox(8, rotAngle, rotBtn);
        rotFoot.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().add(rotFoot);

        // 相机旋转（CameraInterface.rotateXY..rotateZW ✅）：按精确角度转视角（方向与模型旋转相反）
        Label camRotTitle = new Label("相机旋转");
        camRotTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        box.getChildren().add(camRotTitle);
        ComboBox<String> camPlaneBox = new ComboBox<>();
        camPlaneBox.getItems().addAll("XY", "XZ", "XW", "YZ", "YW", "ZW");
        camPlaneBox.getSelectionModel().select(0);
        TextField camRotAngle = new TextField("15");
        camRotAngle.setPrefWidth(56);
        camRotAngle.setStyle("-fx-padding: 2 6;");
        Button camRotBtn = new Button("应用");
        camRotBtn.getStyleClass().add("accent");
        camRotBtn.setStyle("-fx-padding: 2 10;");
        camRotBtn.setOnAction(e -> {
            try {
                float a = Float.parseFloat(camRotAngle.getText().trim());
                switch (camPlaneBox.getValue()) {
                    case "XY" -> renderer.getCamera().rotateXY(a);
                    case "XZ" -> renderer.getCamera().rotateXZ(a);
                    case "XW" -> renderer.getCamera().rotateXW(a);
                    case "YZ" -> renderer.getCamera().rotateYZ(a);
                    case "YW" -> renderer.getCamera().rotateYW(a);
                    case "ZW" -> renderer.getCamera().rotateZW(a);
                }
            } catch (NumberFormatException ignored) {
                // 角度非法时忽略
            }
        });
        HBox camRotFoot = new HBox(8, camPlaneBox, camRotAngle, camRotBtn);
        camRotFoot.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().add(camRotFoot);

        // 相机旋转角度（只读显示；相机在黑色视口里用键盘旋转，这里每秒回显 6 平面姿态）
        box.getChildren().add(new Separator());
        Label camTitle = new Label("相机旋转角度（键盘视口）");
        camTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        box.getChildren().add(camTitle);

        String[] camPlanes = {"XY", "XZ", "YZ", "XW", "YW", "ZW"};
        Label[] camAngles = new Label[6];
        GridPane camGrid = new GridPane();
        camGrid.setHgap(8);
        camGrid.setVgap(8);
        for (int i = 0; i < 6; i++) {
            Label l = new Label(camPlanes[i]);
            l.setPrefWidth(30);
            camAngles[i] = new Label("0.0°");
            camAngles[i].setStyle("-fx-font-size: 11px; -fx-text-fill: #c7ccd1;");
            HBox cell = new HBox(4, l, camAngles[i]);
            camGrid.add(cell, i % 3, i / 3);
        }
        box.getChildren().add(camGrid);

        // 每秒刷新相机 6 平面姿态角度（SceneInterface.get4DCameraOrientation ✅）
        Timeline camTl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            try {
                CameraOrientation o = renderer.getScene().get4DCameraOrientation();
                camAngles[0].setText(String.format("%.1f°", o.getXy()));
                camAngles[1].setText(String.format("%.1f°", o.getXz()));
                camAngles[2].setText(String.format("%.1f°", o.getYz()));
                camAngles[3].setText(String.format("%.1f°", o.getXw()));
                camAngles[4].setText(String.format("%.1f°", o.getYw()));
                camAngles[5].setText(String.format("%.1f°", o.getZw()));
            } catch (Exception ignored) { }
        }));
        camTl.setCycleCount(Animation.INDEFINITE);
        camTl.play();

        return box;
    }

    /** 变换-缩放区块：x/y/z/w 增量缩放（两行两列 + 应用）。 */
    private VBox buildScalePart() {
        VBox box = new VBox(6);
        TextField[] sc = new TextField[4];
        String[] axes = {"X", "Y", "Z", "W"};
        GridPane scGrid = new GridPane();
        scGrid.setHgap(8);
        scGrid.setVgap(8);
        for (int i = 0; i < 4; i++) {
            Label l = new Label(axes[i]);
            l.setPrefWidth(18);
            sc[i] = new TextField("1.00");
            sc[i].setPrefWidth(56);
            sc[i].setStyle("-fx-padding: 2 6;");
            HBox cell = new HBox(4, l, sc[i]);
            scGrid.add(cell, i % 2, i / 2);
        }
        box.getChildren().add(scGrid);
        Button scBtn = new Button("应用");
        scBtn.getStyleClass().add("accent");
        scBtn.setStyle("-fx-padding: 2 10;");
        scBtn.setOnAction(e -> {
            if (selectedModel == null) return;
            try {
                renderer.getTransform().scale(selectedModel,
                        Float.parseFloat(sc[0].getText().trim()),
                        Float.parseFloat(sc[1].getText().trim()),
                        Float.parseFloat(sc[2].getText().trim()),
                        Float.parseFloat(sc[3].getText().trim()));
            } catch (NumberFormatException ignored) {
                // 输入非法时忽略
            }
        });
        box.getChildren().add(scBtn);
        return box;
    }


    private final TextField[] objFields = new TextField[4];

    /** 接口调用：摄像机位置区块 —— 读取 getPos() / 应用 setPos(pos) / 移动 moveRight..moveForward。 */
    private VBox buildCameraBox() {
        VBox box = new VBox(6);
        box.getChildren().add(new Label("摄像机位置"));

        TextField fx = new TextField("0.00");
        TextField fy = new TextField("0.00");
        TextField fz = new TextField("0.00");
        TextField fw = new TextField("0.00");
        TextField[] fields = {fx, fy, fz, fw};
        String[] axes = {"X", "Y", "Z", "W"};
        for (int i = 0; i < 4; i++) {
            fields[i].setPrefWidth(64);
            fields[i].setStyle("-fx-padding: 2 6;");
            Label l = new Label(axes[i]);
            l.setPrefWidth(48);
            HBox row = new HBox(8, l, fields[i]);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }

        Button read = new Button("读取");
        Button apply = new Button("应用");
        read.getStyleClass().add("accent");
        apply.getStyleClass().add("accent");
        read.setStyle("-fx-padding: 2 12;");
        apply.setStyle("-fx-padding: 2 12;");

        // GET_CAMERA_POS（§3.1.1 ✅）：回填当前坐标
        read.setOnAction(e -> {
            Vector4f p = renderer.getCamera().getPosition();
            fields[0].setText(String.format("%.2f", p.x));
            fields[1].setText(String.format("%.2f", p.y));
            fields[2].setText(String.format("%.2f", p.z));
            fields[3].setText(String.format("%.2f", p.w));
        });

        // SET_CAMERA_POS（§3.1.3 ✅）：应用输入坐标
        apply.setOnAction(e -> {
            try {
                float x = Float.parseFloat(fx.getText().trim());
                float y = Float.parseFloat(fy.getText().trim());
                float z = Float.parseFloat(fz.getText().trim());
                float w = Float.parseFloat(fw.getText().trim());
                renderer.getCamera().setPosition(new Vector4f(x, y, z, w));
            } catch (NumberFormatException ex) {
                // 输入非法时忽略，保持原坐标
            }
        });

        HBox buttons = new HBox(8, read, apply);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().add(buttons);

        // MOVE_CAMERA_POS（§3.1.2 ✅）：左/右/上/下/前/后，步长 0.5（相对摄像机坐标系）
        String[] dirs = {"左", "右", "上", "下", "前", "后"};
        Runnable[] acts = {
                () -> renderer.getCamera().moveRight(-0.5f),
                () -> renderer.getCamera().moveRight(0.5f),
                () -> renderer.getCamera().moveUp(0.5f),
                () -> renderer.getCamera().moveUp(-0.5f),
                () -> renderer.getCamera().moveForward(0.5f),
                () -> renderer.getCamera().moveForward(-0.5f)
        };
        GridPane moveGrid = new GridPane();
        moveGrid.setHgap(8);
        moveGrid.setVgap(6);
        for (int i = 0; i < dirs.length; i++) {
            final int idx = i;
            Button b = new Button(dirs[i]);
            b.getStyleClass().add("accent");
            b.setStyle("-fx-padding: 2 10;");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> acts[idx].run());
            moveGrid.add(b, i % 2, i / 2);
        }
        box.getChildren().add(moveGrid);

        // MOVE_CAMERA_POS：Z 轴（ana），步长 0.5（相对摄像机坐标系）
        Button za = new Button("Z-");
        za.setStyle("-fx-padding: 2 10;");
        za.setOnAction(e -> renderer.getCamera().moveAna(-0.5f));
        Button zp = new Button("Z+");
        zp.setStyle("-fx-padding: 2 10;");
        zp.setOnAction(e -> renderer.getCamera().moveAna(0.5f));
        HBox anaBox = new HBox(8, za, zp);
        box.getChildren().add(anaBox);

        // 机位预设（CameraInterface.getView/setView ✅）：3 个槽位，存当前姿态 / 一键切回
        box.getChildren().add(new Separator());
        Label presetTitle = new Label("机位预设");
        presetTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        box.getChildren().add(presetTitle);
        ComboBox<String> presetBox = new ComboBox<>();
        presetBox.getItems().addAll("预设 1", "预设 2", "预设 3");
        presetBox.getSelectionModel().select(0);
        Coordinate4D[] presets = new Coordinate4D[3];
        Label presetState = new Label("未保存");
        presetState.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        Button savePreset = new Button("存当前机位");
        savePreset.getStyleClass().add("accent");
        savePreset.setStyle("-fx-padding: 2 10;");
        savePreset.setOnAction(e -> {
            try {
                presets[presetBox.getSelectionModel().getSelectedIndex()] =
                        renderer.getCamera().getView();
                presetState.setText("已存入 " + presetBox.getValue());
            } catch (Exception ex) {
                presetState.setText("保存失败：" + ex.getMessage());
            }
        });
        Button loadPreset = new Button("切到预设");
        loadPreset.getStyleClass().add("accent");
        loadPreset.setStyle("-fx-padding: 2 10;");
        loadPreset.setOnAction(e -> {
            Coordinate4D p = presets[presetBox.getSelectionModel().getSelectedIndex()];
            if (p == null) {
                presetState.setText(presetBox.getValue() + " 为空，先保存");
                return;
            }
            renderer.getCamera().setView(p);
            presetState.setText("已切换至 " + presetBox.getValue());
        });
        box.getChildren().add(presetBox);
        HBox presetBtns = new HBox(8, savePreset, loadPreset);
        presetBtns.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().addAll(presetBtns, presetState);
        return box;
    }

    /** 变换-裁剪区块：沿轴压缩/拉伸（TransformInterface.clip ✅：源轴→目标轴，量可正可负）。 */
    private VBox buildClipPart() {
        VBox box = new VBox(6);
        box.getChildren().add(new Label("裁剪"));
        ComboBox<String> srcBox = new ComboBox<>();
        srcBox.getItems().addAll("X", "Y", "Z", "W");
        srcBox.getSelectionModel().select(0);
        ComboBox<String> destBox = new ComboBox<>();
        destBox.getItems().addAll("X", "Y", "Z", "W");
        destBox.getSelectionModel().select(1);
        TextField amount = new TextField("0.5");
        amount.setPrefWidth(56);
        amount.setStyle("-fx-padding: 2 6;");
        Button apply = new Button("应用");
        apply.getStyleClass().add("accent");
        apply.setStyle("-fx-padding: 2 10;");
        apply.setOnAction(e -> {
            if (selectedModel == null) return;
            try {
                renderer.getTransform().clip(selectedModel,
                        Direction.Axis.valueOf(srcBox.getValue()),
                        Direction.Axis.valueOf(destBox.getValue()),
                        Float.parseFloat(amount.getText().trim()));
            } catch (NumberFormatException ignored) { }
        });
        HBox row1 = new HBox(8, new Label("源轴"), srcBox, new Label("目标轴"), destBox);
        row1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, new Label("量"), amount, apply);
        row2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().addAll(row1, row2);
        Label hint = new Label("沿源轴压缩/拉伸到目标轴，量可为负（反向）");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        box.getChildren().add(hint);
        return box;
    }

    /* ==================== 四面体详情 ==================== */

    /** 四面体详情区：输索引 → getTetrahedronInfos 显示 4 顶点坐标（GeometryInterface ✅）。 */
    private VBox buildTetBox() {
        VBox box = new VBox(6);
        box.getChildren().add(new Label("四面体详情"));
        TextField idx = new TextField("0");
        idx.setPrefWidth(48);
        idx.setStyle("-fx-padding: 2 6;");
        Label info = new Label("（选中模型后查询）");
        info.setWrapText(true);
        info.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        Button query = new Button("查询");
        query.getStyleClass().add("accent");
        query.setStyle("-fx-padding: 2 10;");
        query.setOnAction(e -> {
            if (selectedModel == null) {
                info.setText("请先选中模型");
                return;
            }
            try {
                int i = Integer.parseInt(idx.getText().trim());
                info.setText(renderer.getGeometry().getTetrahedronInfos(selectedModel, i));
            } catch (NumberFormatException ex) {
                info.setText("索引需为整数");
            } catch (Exception ex) {
                info.setText("查询失败：" + ex.getMessage());
            }
        });
        box.getChildren().addAll(new HBox(8, new Label("索引"), idx, query), info);

        // 删除该四面体（GeometryInterface.removeTetrahedron ✅，删除后索引顺移）
        Button delBtn = new Button("删除该四面体");
        delBtn.getStyleClass().add("accent");
        delBtn.setStyle("-fx-padding: 2 10;");
        delBtn.setOnAction(e -> {
            if (selectedModel == null) {
                info.setText("请先选中模型");
                return;
            }
            try {
                int i = Integer.parseInt(idx.getText().trim());
                renderer.getGeometry().removeTetrahedron(selectedModel, i);
                info.setText("已删除四面体 #" + i + "（索引已顺移）");
                updateData(selectedModel);
            } catch (NumberFormatException ex) {
                info.setText("索引需为整数");
            } catch (Exception ex) {
                info.setText("删除失败：" + ex.getMessage());
            }
        });
        box.getChildren().add(delBtn);

        // 局部变形（GeometryInterface.transformTetrahedrons ✅）：只变换指定索引的四面体
        box.getChildren().add(new Separator());
        Label lvTitle = new Label("局部变形（只转选中胞）");
        lvTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        box.getChildren().add(lvTitle);
        TextField lvIdx = new TextField();
        lvIdx.setPromptText("索引(逗号分隔，空=全部)");
        lvIdx.setPrefWidth(150);
        lvIdx.setStyle("-fx-padding: 2 6;");
        ComboBox<String> lvPlane = new ComboBox<>();
        lvPlane.getItems().addAll("XY", "XZ", "XW", "YZ", "YW", "ZW");
        lvPlane.getSelectionModel().select(0);
        TextField lvAngle = new TextField("15");
        lvAngle.setPrefWidth(56);
        lvAngle.setStyle("-fx-padding: 2 6;");
        Button lvBtn = new Button("应用");
        lvBtn.getStyleClass().add("accent");
        lvBtn.setStyle("-fx-padding: 2 10;");
        lvBtn.setOnAction(e -> {
            if (selectedModel == null) {
                info.setText("请先选中模型");
                return;
            }
            try {
                List<Integer> indices = parseIndices(lvIdx.getText());
                float a = Float.parseFloat(lvAngle.getText().trim());
                renderer.getGeometry().transformTetrahedrons(selectedModel, indices,
                        planeRotMatrix(lvPlane.getValue(), a));
                info.setText("已变换 " + (indices.isEmpty() ? "全部" : indices.size() + " 个") + " 四面体");
                updateData(selectedModel);
            } catch (NumberFormatException ex) {
                info.setText("角度需为数字");
            } catch (Exception ex) {
                info.setText("变换失败：" + ex.getMessage());
            }
        });
        box.getChildren().add(lvIdx);
        HBox lvRow1 = new HBox(8, new Label("平面"), lvPlane, new Label("角度"), lvAngle);
        lvRow1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().add(lvRow1);
        lvBtn.setStyle("-fx-padding: 2 16;");
        HBox lvRow2 = new HBox(lvBtn);
        lvRow2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().add(lvRow2);
        return box;
    }

    /* ==================== 数据区（getModel → kind / 四面体数 / params JSON） ==================== */

    private final Label kindLabel = new Label("-");
    private final Label tetsLabel = new Label("-");
    private final Label vertexLabel = new Label("-");
    private final Label paramsLabel = new Label("（未选中）");
    private final Label visibleLabel = new Label("-");

    private VBox buildDataBox() {
        VBox box = new VBox(6);
        Button refresh = new Button("刷新");
        refresh.setOnAction(e -> updateData(selectedModel));
        Button copyJson = new Button("复制JSON");
        copyJson.setOnAction(e -> {
            String s = paramsLabel.getText();
            if (s != null && !s.startsWith("（未选中）") && !s.startsWith("读取失败") && !s.startsWith("params: （无）")) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(s);
                Clipboard.getSystemClipboard().setContent(cc);
            }
        });
        box.getChildren().add(new HBox(8, new Label("数据"), refresh, copyJson));
        box.getChildren().add(row("类型", kindLabel));
        box.getChildren().add(row("四面体", tetsLabel));
        box.getChildren().add(row("顶点", vertexLabel));
        box.getChildren().add(row("可见", visibleLabel));
        paramsLabel.setWrapText(true);
        paramsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        box.getChildren().add(paramsLabel);
        return box;
    }

    /** 选中模型变化时刷新数据区：直接拿 Mesh4D 实例读字段。 */
    private void updateData(String name) {
        if (name == null) {
            kindLabel.setText("-");
            tetsLabel.setText("-");
            vertexLabel.setText("-");
            visibleLabel.setText("-");
            paramsLabel.setText("（未选中）");
            return;
        }
        Mesh4D m;
        try {
            m = renderer.getModel().getModel(name);
        } catch (Exception ex) {
            // 中间/结构节点没有实体模型（SceneOutliner 的选中回调可能给出这类路径）
            kindLabel.setText("结构节点");
            tetsLabel.setText("-");
            vertexLabel.setText("-");
            visibleLabel.setText("-");
            paramsLabel.setText("（该路径不是实体模型）");
            return;
        }
        int tets = m.getTetrahedrons().size();
        kindLabel.setText(m.getKind().name());
        tetsLabel.setText(String.valueOf(tets));
        vertexLabel.setText(String.valueOf(tets * 4));
        visibleLabel.setText(m.getVisible() ? "是" : "否（已隐藏）");
        paramsLabel.setText(m.getParams() == null ? "params: （无）" : m.getParams().toJSONString());
    }

    private static HBox row(String label, Label value) {
        Label l = new Label(label);
        l.setPrefWidth(48);
        HBox h = new HBox(8, l, value);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return h;
    }

    /** Blender 变换面板式坐标行：标签宽、值窄、行紧凑。 */
    private HBox buildCoordinateRow(String axis, String value, int fieldIdx) {
        Label label = new Label(axis);
        label.setPrefWidth(48);
        TextField field = new TextField(value);
        field.setPrefWidth(64);
        field.setStyle("-fx-padding: 2 6;");
        objFields[fieldIdx] = field;
        HBox row = new HBox(8, label, field);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    /** 解析逗号/空格分隔的四面体索引列表；空输入 → 空列表（接口语义 = 全部）。 */
    private static List<Integer> parseIndices(String text) {
        List<Integer> out = new ArrayList<>();
        if (text == null) return out;
        for (String s : text.split("[,，\\s]+")) {
            if (s.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    /** 构造绕某平面旋转 θ 的 5×5 行主序矩阵（用于 transformTetrahedrons / 局部变形）。 */
    private static Matrix5f planeRotMatrix(String plane, float deg) {
        float rad = (float) Math.toRadians(deg);
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        float[] d = {1,0,0,0,0, 0,1,0,0,0, 0,0,1,0,0, 0,0,0,1,0, 0,0,0,0,1};
        switch (plane) {
            case "XY": d[0]=c; d[1]=-s; d[5]=s; d[6]=c; break;
            case "XZ": d[0]=c; d[2]=-s; d[10]=s; d[12]=c; break;
            case "XW": d[0]=c; d[3]=-s; d[15]=s; d[18]=c; break;
            case "YZ": d[6]=c; d[7]=-s; d[11]=s; d[12]=c; break;
            case "YW": d[6]=c; d[8]=-s; d[16]=s; d[18]=c; break;
            case "ZW": d[12]=c; d[13]=-s; d[17]=s; d[18]=c; break;
        }
        return new Matrix5f(d);
    }
}