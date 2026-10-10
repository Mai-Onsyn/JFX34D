package HCloudbyte.ui.dialog;

import javafx.scene.paint.Color;

/**
 * 场景设置的「UI 侧缓存」。
 *
 * <p>渲染接口的 FOV / 速度 / FPS / 视口大小等只有 set 没有 get，弹窗每次打开无法从渲染器读回当前值，
 * 所以把「上次应用/拖动的值」缓存下来，重开弹窗时用它回显，避免每次重置成默认值。
 *
 * <p>这是一个纯数据持有对象：由 {@link HCloudbyte.ui.topbar.TopBar} 创建并长期持有，
 * 每次打开 {@link SettingsDialog} 时传进去读写。
 */
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
    /** 是否启用光照渲染。 */
    public boolean light = true;
    /** 是否启用线框渲染。 */
    public boolean wire = false;
    /** 环境光颜色。 */
    public Color ambient = Color.color(0.3, 0.3, 0.3);
}
