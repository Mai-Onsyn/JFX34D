package HCloudbyte.ui.property;

import HCloudbyte.ui.section.CameraSection;
import HCloudbyte.ui.section.ClipSection;
import HCloudbyte.ui.section.ModelDataSection;
import HCloudbyte.ui.section.PositionSection;
import HCloudbyte.ui.section.RotateSection;
import HCloudbyte.ui.section.ScaleSection;
import HCloudbyte.ui.section.TetrahedronSection;
import HCloudbyte.ui.scenetree.SceneTree;
import HCloudbyte.ui.theme.Theme;
import HCloudbyte.ui.widget.CollapsibleSection;
import javafx.geometry.Insets;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import mai_onsyn.renderer.interfaces.RendererInterface;

import java.util.List;
import java.util.function.Supplier;

/**
 * 右侧属性视图（手写模式）：场景集合 + 变换/旋转/缩放/裁剪/摄像机/四面体 六个折叠区块 + 数据区。
 *
 * <p>本类只做「装配与接线」，各区块（{@code section} 包）与场景树（{@code scenetree} 包）各自独立实现。
 * 选中模型由本类持有（{@code selectedModel}），以 {@link Supplier} 形式传给需要它的区块。
 */
public class PropertyPanel extends VBox {

    private final RendererInterface renderer;
    private final SceneTree sceneTree;
    private final ModelDataSection dataSection;

    /** 当前选中的模型名（SceneTree 点击联动）。 */
    private String selectedModel;

    public PropertyPanel(RendererInterface renderer) {
        super(10);
        this.renderer = renderer;
        setPadding(new Insets(16));
        setStyle(Theme.GLASS);

        // 默认选中第一个模型（后续由 SceneTree 点击切换）
        List<String> models = renderer.getModel().listModel();
        selectedModel = models.isEmpty() ? null : models.get(0);

        // 数据区必须先于场景树创建：场景树构造时会触发一次选中回调 → 调用 dataSection.refresh
        dataSection = new ModelDataSection(renderer);

        Supplier<String> selection = () -> selectedModel;
        Runnable onDataChanged = () -> dataSection.refresh(selectedModel);

        sceneTree = new SceneTree(renderer, name -> {
            selectedModel = name;
            dataSection.refresh(name);
        });

        VBox foldBox = new VBox(0,
                new CollapsibleSection("变换", new PositionSection(renderer, selection)),
                new CollapsibleSection("旋转", new RotateSection(renderer, selection)),
                new CollapsibleSection("缩放", new ScaleSection(renderer, selection)),
                new CollapsibleSection("裁剪", new ClipSection(renderer, selection)),
                new CollapsibleSection("摄像机位置", new CameraSection(renderer)),
                new CollapsibleSection("四面体详情", new TetrahedronSection(renderer, selection, onDataChanged)));

        getChildren().addAll(
                sceneTree,
                new Separator(),
                foldBox,
                new Separator(),
                dataSection);

        dataSection.refresh(selectedModel);
    }

    /** 供 UIInterface 初始化读取选中路径的场景集合树引用。 */
    public SceneTree getSceneTree() {
        return sceneTree;
    }
}
