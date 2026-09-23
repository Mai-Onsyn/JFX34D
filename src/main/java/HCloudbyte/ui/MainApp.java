package HCloudbyte.ui;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.cpu4dkt.generator.HypercubeKt;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.ogl3d.data.ColorARGB;
import org.joml.Vector4f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 主应用：组装背景、顶栏、视口、右侧面板、状态栏，并负责手写/语音模式切换。
 * 各面板拆分为独立类，此处只做装配与接线。
 *
 * <p>【接口调用】（AI Api Interface document v2.md §0.1，直接调用 Kotlin 侧已定义接口）
 * <ul>
 *   <li>视口：创建 {@link GL4DRegion}（渲染 + 内置 4D 键盘），加一个超立方体演示模型，开线框渲染</li>
 *   <li>绑定：{@code RendererInterface.init(region.getScene4D().getCamera())} —— 等价 start4DTest2 里
 *       RendererInterfaceInitializer.initialize(region) 的用法，把 RendererInterface 绑定到视口的
 *       scene4D.camera4D，UI 的 getCamera() 与视口键盘共用同一摄像机</li>
 *   <li>创建各面板时传入：{@code ViewportPanel(RendererInterface, GL4DRegion)}、
 *       {@code PropertyPanel(RendererInterface)}、{@code StatusBar(RendererInterface)}</li>
 *   <li>手写/语音模式切换本身是 UI 状态（isVisible），不调用接口</li>
 * </ul>
 */
public class MainApp extends Application {

    private PropertyPanel propertyView;
    private ChatPanel chatView;
    private mai_onsyn.renderer.interfaces.RendererInterface renderer;   // Kotlin 侧已定义接口，直接调用
    // AgentService agentService;          // v2 文档 §2，后端 TODO，暂不接入

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // 初始化渲染视口（等价 Kotlin MainApp.kt 的 start4DTest2 用法）：
        // GL4DRegion 渲染 4D 模型 + 内置键盘；RendererInterface.init(region.scene4D.camera4D)
        // 把 RendererInterface 绑定到该视口的摄像机（与 RendererInterfaceInitializer.initialize(region) 完全等价）
        GL4DRegion region = new GL4DRegion();
        // 超立方体演示模型（8 胞调色板与 Hypercube.kt 默认一致，48 个四面体）
        region.getScene4D().getMeshList().add(new Mesh4D(buildHypercubeTets()));
        region.setOutlineRendering(true);
        RendererInterface.Companion.init(region.getScene4D().getCamera());
        renderer = RendererInterface.Companion.getINSTANCE();

        StackPane rootStack = new StackPane();
        Pane background = buildGlassBackground();
        background.setMouseTransparent(true);

        BorderPane layoutRoot = buildLayout(region);

        rootStack.getChildren().addAll(background, layoutRoot);

        Scene scene = new Scene(rootStack, 1400, 900);
        stage.setTitle("JFX 34D");
        stage.setScene(scene);
        stage.show();
    }

    /* ==================== 主布局 ==================== */

    private BorderPane buildLayout(GL4DRegion region) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        TopBar top = new TopBar(index -> switchRightPanel(index == 1));
        // 接口调用：TopBar 增加保存/打开后 → IOInterface#saveModel / #loadModel（后端 TODO，暂不接）
        ViewportPanel center = new ViewportPanel(renderer, region);
        // 接口调用：视口 = GL4DRegion（渲染 + 内置键盘），RendererInterface 已绑定其摄像机（§3.1 ✅）
        StackPane right = buildRightPanel();
        StatusBar bottom = new StatusBar(renderer);
        // 接口调用：状态栏 CAM 坐标 → CameraInterface#getPosition（§3.1.1 ✅）

        root.setTop(top);
        root.setCenter(center);
        root.setRight(right);
        root.setBottom(bottom);

        BorderPane.setMargin(top, new Insets(0, 0, 14, 0));
        BorderPane.setMargin(right, new Insets(0, 0, 0, 14));
        BorderPane.setMargin(bottom, new Insets(14, 0, 0, 0));
        return root;
    }

    /* ==================== 右侧面板切换 ==================== */

    private StackPane buildRightPanel() {
        propertyView = new PropertyPanel(renderer);
        // 接口调用：PropertyPanel 摄像机位置 → camera().getPosition()/setPosition()（§3.1 ✅）
        chatView = new ChatPanel(new SceneOutliner());
        // 接口调用：ChatPanel → agentService.send()（后端 TODO，暂不接）
        // 注意：SceneOutliner 若同时挂到 PropertyPanel 与 ChatPanel，须各持一份或显式切换，
        //       否则 JavaFX 会把节点从旧父容器中夺走（此前诊断过的双父 bug）。

        chatView.setVisible(false);

        // 右侧内容超高时滚动 + 压平 minHeight，防止把顶栏/状态栏挤出窗口（上传版布局溢出 bug 的修复）
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(propertyView);
        scroll.setFitToWidth(true);
        scroll.setMinHeight(0);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        StackPane stack = new StackPane(scroll, chatView);
        stack.setPrefWidth(280);
        stack.setMinWidth(280);
        stack.setMaxWidth(280);
        stack.setMinHeight(0);   // 关键：压平 minHeight，避免 BorderPane 被撑出窗口
        return stack;
    }

    /** 0 = 手写模式，1 = 语音模式。 */
    private void switchRightPanel(boolean voiceMode) {
        propertyView.setVisible(!voiceMode);
        chatView.setVisible(voiceMode);
    }

    /* ==================== 背景 ==================== */

    private Pane buildGlassBackground() {
        Pane bg = new Pane();
        bg.setStyle("-fx-background-color: linear-gradient(to bottom right, #eef2f7, #f7f0f5);");

        Circle c1 = new Circle(260, 180, 400, Color.web("#4a90e2", 0.55));
        Circle c2 = new Circle(1150, 160, 5000, Color.web("#9b59b6", 0.45));
        Circle c3 = new Circle(180, 760, 250, Color.web("#1abc9c", 0.42));
        Circle c4 = new Circle(1250, 780, 290, Color.web("#ff6b9d", 0.45));

        bg.getChildren().addAll(c1, c2, c3, c4);
        bg.setEffect(new GaussianBlur(60));
        bg.setCache(true);
        bg.setCacheHint(CacheHint.QUALITY);
        return bg;
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 构造超立方体的四面体列表（8 胞调色板与 Hypercube.kt 默认值一致，共 48 个四面体）。
     * ColorARGB 是 {@code @JvmInline value class}：Java 源码无法直接构造
     * （构造器 private，companion invoke 被 Kotlin 改为带 '-' 的 mangled 名），
     * 故反射调用其装箱工厂 box-impl(int hex) 构造 8 胞颜色，再调普通 3 参 constructHypercubeWithCellColors。
     */
    private static List<mai_onsyn.renderer.cpu4dkt.Tetrahedron> buildHypercubeTets() {
        try {
            Method box = ColorARGB.class.getMethod("box-impl", int.class);
            List<ColorARGB> colors = new ArrayList<>();
            colors.add((ColorARGB) box.invoke(null, 0xFFFF6666)); // +X (1.0, 0.4, 0.4)
            colors.add((ColorARGB) box.invoke(null, 0xFF990066)); // -X (0.6, 0.0, 0.4)
            colors.add((ColorARGB) box.invoke(null, 0xFF66FF66)); // +Y (0.4, 1.0, 0.4)
            colors.add((ColorARGB) box.invoke(null, 0xFF009966)); // -Y (0.0, 0.6, 0.4)
            colors.add((ColorARGB) box.invoke(null, 0xFF6666FF)); // +Z (0.4, 0.4, 1.0)
            colors.add((ColorARGB) box.invoke(null, 0xFF006699)); // -Z (0.0, 0.4, 0.6)
            colors.add((ColorARGB) box.invoke(null, 0xFFFFFF66)); // +W (1.0, 1.0, 0.4)
            colors.add((ColorARGB) box.invoke(null, 0xFFFF8000)); // -W (1.0, 0.5, 0.0)
            return HypercubeKt.constructHypercubeWithCellColors(new Vector4f(), 1f, colors);
        } catch (Exception e) {
            throw new RuntimeException("constructHypercubeWithCellColors 调用失败", e);
        }
    }
}