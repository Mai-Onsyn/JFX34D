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
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

/**
 * 右侧属性视图（手写模式）：场景集合、对象属性、摄像机位置、材质、旋转平面。
 *
 * <p>【接口调用】（AI Api Interface document v2.md §0.1，直接调用 Kotlin 侧已定义接口）
 * <ul>
 *   <li>摄像机位置 → {@code RendererInterface.getCamera().getPosition()}（§3.1.1 ✅）/ {@code setPosition(pos)}（§3.1.3 ✅）</li>
 *   <li>对象属性 XYZW：选中对象 ——
 *       {@code getTransform().getModelMatrix(name)}（§4.3，后端 TODO，暂不接）、
 *       {@code getGeometry().getTetrahedronInfos(name, tetId)}（§6.2，后端 TODO，暂不接）</li>
 *   <li>旋转平面：模型旋转 → {@code getTransform().rotate(axis, angle)}（§4.2.4，后端 TODO，暂不接）；
 *       摄像机旋转 → {@code getCamera().rotateXY..rotateZW}（§3.2.2，后端 TODO，暂不接）</li>
 *   <li>材质：渲染侧显示模式（线框/实体/半透明），RendererInterface 无对应操作</li>
 * </ul>
 */
public class PropertyPanel extends VBox {

    private final RendererInterface renderer;

    public PropertyPanel(RendererInterface renderer) {
        super(16);
        this.renderer = renderer;
        setPadding(new Insets(20));
        setStyle(UiTheme.GLASS);

        VBox sceneOutliner = new SceneOutliner();

        VBox objBox = new VBox(8);
        // 接口接线：选中对象属性 ← TransformInterface#getModelMatrix / GeometryInterface#getTetrahedronInfos
        objBox.getChildren().addAll(
                new Label("对象属性"),
                new Label("Tesseract 001  MESH · 4D"),
                buildCoordinateRow("X", "0.00"),
                buildCoordinateRow("Y", "0.00"),
                buildCoordinateRow("Z", "0.00"),
                buildCoordinateRow("W", "0.00")
        );

        VBox matBox = new VBox(8);
        ToggleGroup matGroup = new ToggleGroup();
        ToggleButton wire = new ToggleButton("线框");
        ToggleButton solid = new ToggleButton("实体");
        ToggleButton half = new ToggleButton("半透明");
        wire.setToggleGroup(matGroup);
        solid.setToggleGroup(matGroup);
        half.setToggleGroup(matGroup);
        wire.setSelected(true);
        matBox.getChildren().addAll(
                new Label("材质"),
                new HBox(8, wire, solid, half)
        );

        VBox rotBox = new VBox(8);
        // 接口接线：模型旋转 → TransformInterface#rotate(plane, angle)；摄像机旋转 → CameraInterface#rotateXY..rotateZW
        rotBox.getChildren().add(new Label("旋转平面"));
        ToggleGroup rotGroup = new ToggleGroup();
        String[] planes = {"XY", "XZ", "XW", "YZ", "YW", "ZW"};
        FlowPane planePane = new FlowPane(8, 8);
        for (String plane : planes) {
            ToggleButton btn = new ToggleButton(plane);
            btn.setToggleGroup(rotGroup);
            btn.getStyleClass().add("accent");
            planePane.getChildren().add(btn);
        }
        ((ToggleButton) planePane.getChildren().get(1)).setSelected(true);
        rotBox.getChildren().add(planePane);

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

    /** 接口调用：摄像机位置区块 —— 读取 getPosition() / 应用 setPosition(pos)。 */
    private VBox buildCameraBox() {
        VBox box = new VBox(8);
        box.getChildren().add(new Label("摄像机位置"));

        TextField fx = new TextField("0.00");
        TextField fy = new TextField("0.00");
        TextField fz = new TextField("0.00");
        TextField fw = new TextField("0.00");
        TextField[] fields = {fx, fy, fz, fw};
        String[] axes = {"X", "Y", "Z", "W"};
        for (int i = 0; i < 4; i++) {
            fields[i].setPrefWidth(160);
            Label l = new Label(axes[i]);
            l.setPrefWidth(20);
            HBox row = new HBox(8, l, fields[i]);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }

        Button read = new Button("读取");
        Button apply = new Button("应用");
        read.getStyleClass().add("accent");
        apply.getStyleClass().add("accent");

        // GET_CAMERA_POS（§3.1.1，已实现）：回填当前坐标
        read.setOnAction(e -> {
            Vector4f p = renderer.getCamera().getPosition();
            fields[0].setText(String.format("%.2f", p.x));
            fields[1].setText(String.format("%.2f", p.y));
            fields[2].setText(String.format("%.2f", p.z));
            fields[3].setText(String.format("%.2f", p.w));
        });

        // SET_CAMERA_POS（§3.1.3，已实现）：应用输入坐标
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
        return box;
    }

    private HBox buildCoordinateRow(String axis, String value) {
        Label label = new Label(axis);
        label.setPrefWidth(20);
        TextField field = new TextField(value);
        field.setPrefWidth(160);
        HBox row = new HBox(8, label, field);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }
}