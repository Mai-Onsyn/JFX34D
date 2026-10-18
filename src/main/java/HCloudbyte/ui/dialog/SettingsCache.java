package HCloudbyte.ui.dialog;

import javafx.scene.paint.Color;

public final class SettingsCache {
    /** 窗口背景色。 */
    public Color backgroundColor = Color.color(0.25, 0.25, 0.25);
    /** 4D 视口大小（边长）。 */
    public double displaySize = 8.0;
    /** 3D / 4D FPS 上限。 */
    public double fps3d = 60.0;
    public double fps4d = 60.0;
    /** 3D / 4D 相机 FOV（度）。 */
    public double fov3d = 70.0;
    public double fov4d = 80.0;
    /** 相机速度倍率：[3D移动, 3D旋转, 4D移动, 4D旋转]。 */
    public final double[] speed = {1.0, 1.0, 1.0, 1.0};
    /** 是否启用光照渲染（默认关闭）。 */
    public boolean light = false;
    /** 是否启用线框渲染（默认开启）。 */
    public boolean wire = true;
    /** 环境光颜色。 */
    public Color ambient = Color.color(0.3, 0.3, 0.3);
}
