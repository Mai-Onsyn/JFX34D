package HCloudbyte.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

import java.util.List;

/**
 * 右侧属性视图（手写模式）：场景集合、变换、摄像机位置、材质、旋转平面（紧凑 Blender 风格）。
 *
 * <p>【接口调用】（AI Api Interface document.md，直接调用 Kotlin 侧已定义接口）
 * <ul>
 *   <li>场景集合 → {@code ModelInterface#listModel()}（§4.1.1 LIST_MODEL ✅）</li>
 *   <li>变换（选中模型位置）→ {@code TransformInterface#getModelMatrix(name)}（§4.1.4 GET_MODEL_MATRIX ✅，
 *       行主序 5×5 矩阵最后一列为平移分量：matrix[4]/[9]/[14]/[19]）</li>
 *   <li>摄像机位置 → {@code CameraInterface#getPosition()/setPosition()}（§3.1.1/§3.1.3 ✅）</li>
 *   <li>摄像机移动 → {@code CameraInterface#moveRight/moveUp/moveForward}（§3.1.2 MOVE_CAMERA_POS ✅）</li>
 *   <li>旋转平面 → {@code CameraInterface#rotateXY..rotateZW}（§3.2.2 ROTATE_CAMERA_VIEW ✅，入参为弧度）</li>
 *   <li>材质 → {@code SceneInterface#enableTriangleLineRendering}（线框/实体；接口暂无半透明模式）</li>
 * </ul>
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
        selectedModel = models.isEmpty() ? null : models.get(0);

        VBox sceneOutliner = new SceneOutliner(renderer, name -> selectedModel = name);

        VBox objBox = buildTransformBox();

        VBox matBox = buildMaterialBox();

        VBox rotBox = buildRotateBox();

        VBox camBox = buildCameraBox();

        getChildren().addAll(
                sceneOutliner,
                new Separator(),
                objBox,
                new Separator(),
                camBox,
                new Separator(),
                matBox,
                new Separator(),
                rotBox
        );
    }

    /** 变换区块：标题行（变换 + 读取按钮）+ 位置 X/Y/Z/W，读取选中模型的平移分量。 */
    private VBox buildTransformBox() {
        VBox box = new VBox(3);

        Label title = new Label("变换");
        Button read = new Button("读取");
        read.getStyleClass().add("accent");
        read.setStyle("-fx-padding: 2 12;");
        // 接口调用：GET_MODEL_MATRIX（§4.1.4 ✅）：行主序 5×5 矩阵最后一列为平移
        read.setOnAction(e -> {
            if (selectedModel == null) return;
            Matrix5f m = renderer.getTransform().getModelMatrix(selectedModel);
            objFields[0].setText(String.format("%.2f", m.get(4)));
            objFields[1].setText(String.format("%.2f", m.get(9)));
            objFields[2].setText(String.format("%.2f", m.get(14)));
            objFields[3].setText(String.format("%.2f", m.get(19)));
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox titleRow = new HBox(6, title, spacer, read);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        box.getChildren().add(titleRow);
        box.getChildren().add(buildCoordinateRow("位置X", "0.00", 0));
        box.getChildren().add(buildCoordinateRow("Y", "0.00", 1));
        box.getChildren().add(buildCoordinateRow("Z", "0.00", 2));
        box.getChildren().add(buildCoordinateRow("W", "0.00", 3));
        return box;
    }

    private final TextField[] objFields = new TextField[4];

    /** 材质区块：线框/实体/半透明 —— 线框渲染开关。 */
    private VBox buildMaterialBox() {
        VBox box = new VBox(5);
        ToggleGroup matGroup = new ToggleGroup();
        ToggleButton wire = new ToggleButton("线框");
        ToggleButton solid = new ToggleButton("实体");
        ToggleButton half = new ToggleButton("半透明");
        wire.setToggleGroup(matGroup);
        solid.setToggleGroup(matGroup);
        half.setToggleGroup(matGroup);
        wire.setSelected(true);
        for (ToggleButton b : new ToggleButton[]{wire, solid, half}) {
            b.setStyle("-fx-padding: 2 10;");
        }
        // 接口调用：线框渲染开关（SceneInterface.enableTriangleLineRendering ✅）
        // 注意：接口暂无半透明模式，半透明暂按实体渲染
        wire.setOnAction(e -> renderer.getScene().enableTriangleLineRendering(true));
        solid.setOnAction(e -> renderer.getScene().enableTriangleLineRendering(false));
        half.setOnAction(e -> renderer.getScene().enableTriangleLineRendering(false));

        box.getChildren().addAll(
                new Label("材质"),
                new HBox(6, wire, solid, half)
        );
        return box;
    }

    /** 旋转平面区块：点击按钮 → 摄像机视角旋转（弧度）。 */
    private VBox buildRotateBox() {
        VBox box = new VBox(5);
        box.getChildren().add(new Label("旋转平面"));
        ToggleGroup rotGroup = new ToggleGroup();
        String[] planes = {"XY", "XZ", "XW", "YZ", "YW", "ZW"};
        FlowPane planePane = new FlowPane(6, 6);
        for (String plane : planes) {
            ToggleButton btn = new ToggleButton(plane);
            btn.setToggleGroup(rotGroup);
            btn.getStyleClass().add("accent");
            btn.setStyle("-fx-padding: 2 10;");
            // 接口调用：ROTATE_CAMERA_VIEW（§3.2.2 ✅）：摄像机视角绕该平面旋转 ≈15°（0.26 rad）
            btn.setOnAction(e -> {
                float a = 0.26f;
                switch (plane) {
                    case "XY" -> renderer.getCamera().rotateXY(a);
                    case "XZ" -> renderer.getCamera().rotateXZ(a);
                    case "XW" -> renderer.getCamera().rotateXW(a);
                    case "YZ" -> renderer.getCamera().rotateYZ(a);
                    case "YW" -> renderer.getCamera().rotateYW(a);
                    case "ZW" -> renderer.getCamera().rotateZW(a);
                    default -> { }
                }
            });
            planePane.getChildren().add(btn);
        }
        ((ToggleButton) planePane.getChildren().get(1)).setSelected(true);
        box.getChildren().add(planePane);
        return box;
    }

    /** 接口调用：摄像机位置区块 —— 读取 getPos() / 应用 setPos(pos) / 移动 moveRight..moveForward。 */
    private VBox buildCameraBox() {
        VBox box = new VBox(3);
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
            HBox row = new HBox(4, l, fields[i]);
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

        HBox buttons = new HBox(6, read, apply);
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
        HBox moveBox = new HBox(4);
        for (int i = 0; i < dirs.length; i++) {
            final int idx = i;
            Button b = new Button(dirs[i]);
            b.setStyle("-fx-padding: 2 10;");
            b.setOnAction(e -> acts[idx].run());
            moveBox.getChildren().add(b);
        }
        box.getChildren().add(moveBox);
        return box;
    }

    /** Blender 变换面板式坐标行：标签宽、值窄、行紧凑。 */
    private HBox buildCoordinateRow(String axis, String value, int fieldIdx) {
        Label label = new Label(axis);
        label.setPrefWidth(48);
        TextField field = new TextField(value);
        field.setPrefWidth(64);
        field.setStyle("-fx-padding: 2 6;");
        objFields[fieldIdx] = field;
        HBox row = new HBox(4, label, field);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }
}
