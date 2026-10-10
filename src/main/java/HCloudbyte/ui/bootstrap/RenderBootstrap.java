package HCloudbyte.ui.bootstrap;

import HCloudbyte.ui.viewport.ViewportBox;
import javafx.scene.paint.Color;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

/**
 * 渲染侧引导：创建 4D 渲染视口与渲染接口，并铺好初始场景（默认模型 + 相机位置 + 视口指示方块）。
 *
 * <p>等价于原 {@code start4DTest2} 的用法：{@code RendererInterface.init(region)} 把
 * {@code RendererInterface} 绑定到 GL4DRegion 的摄像机，UI 的 getCamera() 与视口键盘共用同一摄像机。
 */
public final class RenderBootstrap {

    private RenderBootstrap() {
    }

    /** 引导结果：视口 + 渲染接口 + 视口指示方块，供 UI 各处共用。 */
    public record Result(GL4DRegion region, RendererInterface renderer, ViewportBox viewportBox) {
    }

    /** 创建视口与渲染接口，并初始化默认场景。 */
    public static Result create() {
        GL4DRegion region = new GL4DRegion();
        region.setOutlineRendering(true);            // 线框描边
        RendererInterface.Companion.init(region);
        RendererInterface renderer = RendererInterface.Companion.getINSTANCE();

        // 渲染默认值：光照开、线框关、深灰背景
        renderer.getScene().enableLightRendering(true);
        renderer.getScene().enableTriangleLineRendering(false);
        renderer.getScene().setBackgroundColor(Color.color(0.25, 0.25, 0.25));

        // 视口区域指示方块：半透明正方体，边长跟「视口大小」走，默认开启
        ViewportBox viewportBox = new ViewportBox(region);
        viewportBox.setVisible(true);

        // 演示模型：一个超立方体 + 相机后移，让它落在视野里
        renderer.getShape().createTesseract("", "Tesseract", new Vector4f(0, 0, 0, 0), 1);
        renderer.getCamera().moveForward(-10);

        return new Result(region, renderer, viewportBox);
    }
}

