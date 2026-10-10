package HCloudbyte.ui.dialog;

import HCloudbyte.ui.scenetree.SceneModelOperations;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

/**
 * 「添加形状」弹窗：在所选节点的「下一级」或「同级(父级)」下创建一个参数化形状。
 * 形状类型 → renderer.getShape().createXxx(...)。
 */
public final class ShapeCreateDialog {

    private ShapeCreateDialog() {
    }

    /**
     * @param renderer  渲染接口
     * @param path      当前选中节点路径（空串表示场景根）
     * @param onChanged 创建成功后回调（通常用于重建场景树）
     */
    public static void show(RendererInterface renderer, String path, Runnable onChanged) {
        boolean atRoot = path == null || path.isEmpty();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("添加形状");
        dialog.setHeaderText("创建形状（" + (atRoot ? "场景根" : path) + "）");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        RadioButton toSibling = new RadioButton("同级（与所选并列）");
        RadioButton toChild = new RadioButton("下一级（挂到所选下）");
        ToggleGroup targetGroup = new ToggleGroup();
        toSibling.setToggleGroup(targetGroup);
        toChild.setToggleGroup(targetGroup);
        toSibling.setSelected(true);

        Label targetHint = new Label();
        targetHint.setStyle("-fx-text-fill: #8ab4f8; -fx-font-size: 12px;");
        targetHint.setWrapText(true);
        Runnable updateHint = () -> {
            String target = toChild.isSelected() ? path : SceneModelOperations.parentOf(path);
            targetHint.setText("将创建到：" + (target.isEmpty() ? "场景根" : target));
        };
        updateHint.run();
        toSibling.selectedProperty().addListener((o, a, b) -> updateHint.run());
        toChild.selectedProperty().addListener((o, a, b) -> updateHint.run());

        ComboBox<String> shapeBox = new ComboBox<>();
        shapeBox.getItems().addAll("正四面体", "5Cell", "16Cell", "超立方体", "超球");
        shapeBox.getSelectionModel().select(0);

        TextField nameField = new TextField();
        nameField.setPromptText("留空自动命名");

        TextField cx = new TextField("0.00"), cy = new TextField("0.00"),
                cz = new TextField("0.00"), cw = new TextField("0.00");
        HBox centerRow = new HBox(4,
                new Label("X"), compact(cx), new Label("Y"), compact(cy),
                new Label("Z"), compact(cz), new Label("W"), compact(cw));

        Label paramLabel = new Label("半径");
        TextField param = new TextField("1.00");
        Label densityLabel = new Label("密度");
        TextField density = new TextField("3.00");
        HBox paramRow = new HBox(4, paramLabel, compact(param), densityLabel, compact(density));

        // 只有「超球」才需要密度参数
        shapeBox.valueProperty().addListener((o, oldV, newV) -> {
            paramLabel.setText(paramLabelFor(newV));
            boolean ball = "超球".equals(newV);
            density.setVisible(ball);
            density.setManaged(ball);
            densityLabel.setVisible(ball);
            densityLabel.setManaged(ball);
        });
        density.setVisible(false);
        density.setManaged(false);
        densityLabel.setVisible(false);
        densityLabel.setManaged(false);

        Label error = new Label();
        error.setWrapText(true);
        error.setMaxWidth(300);
        error.setStyle("-fx-text-fill: #e06c75;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        int row = 0;
        if (!atRoot) {
            grid.addRow(row++, new Label("位置"), new HBox(8, toSibling, toChild));   // 根下无需选位置
        }
        grid.addRow(row++, new Label("目标"), targetHint);
        grid.addRow(row++, new Label("形状"), shapeBox);
        grid.addRow(row++, new Label("名称"), nameField);
        grid.addRow(row++, new Label("中心"), centerRow);
        grid.addRow(row++, new Label("参数"), paramRow);
        grid.addRow(row, error);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, e -> {
            String target = toChild.isSelected() ? path : SceneModelOperations.parentOf(path);
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = autoName(shapeBox.getValue());
                Vector4f center = new Vector4f(parseF(cx.getText(), 0f), parseF(cy.getText(), 0f),
                        parseF(cz.getText(), 0f), parseF(cw.getText(), 0f));
                float p = parseF(param.getText(), 1f);
                switch (shapeBox.getValue()) {
                    case "正四面体" -> renderer.getShape().createTetrahedron(target, name, center, p);
                    case "5Cell" -> renderer.getShape().create5Cell(target, name, center, p);
                    case "16Cell" -> renderer.getShape().create16Cell(target, name, center, p);
                    case "超立方体" -> renderer.getShape().createTesseract(target, name, center, p);
                    case "超球" -> renderer.getShape().createBall4(target, name, center, p,
                            parseF(density.getText(), 0.5f));
                    default -> throw new IllegalArgumentException("未知形状：" + shapeBox.getValue());
                }
            } catch (Exception ex) {
                error.setText("添加失败：" + ex.getMessage());
                e.consume();   // 失败不关窗
                return;
            }
            onChanged.run();
        });

        dialog.showAndWait();
    }

    private static TextField compact(TextField field) {
        field.setPrefWidth(46);
        field.setStyle("-fx-padding: 2 6;");
        return field;
    }

    private static String paramLabelFor(String shape) {
        return switch (shape) {
            case "5Cell" -> "大小";
            case "超立方体" -> "边长";
            default -> "半径";
        };
    }

    private static float parseF(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String autoName(String shape) {
        String prefix = switch (shape) {
            case "正四面体" -> "Tet";
            case "5Cell" -> "FiveCell";
            case "16Cell" -> "SixteenCell";
            case "超立方体" -> "Tesseract";
            default -> "Ball";
        };
        return prefix + "_" + (System.currentTimeMillis() % 100000);
    }
}
