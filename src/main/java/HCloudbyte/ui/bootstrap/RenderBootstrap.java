package HCloudbyte.ui.bootstrap;

import HCloudbyte.ui.viewport.ViewportBox;
import javafx.scene.paint.Color;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;

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

        // 渲染默认值：光照关、线框开、深灰背景（与设置弹窗里的默认勾选一致）
        renderer.getScene().enableLightRendering(false);
        renderer.getScene().enableTriangleLineRendering(true);
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
