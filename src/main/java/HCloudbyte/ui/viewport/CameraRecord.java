package HCloudbyte.ui.viewport;

import mai_onsyn.renderer.cpu4dkt.CameraOrientation;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;

/**
 * 一个摄像机机位（相机书签）。
 *
 * <p>包含四样东西：
 * <ul>
 *   <li>{@code name}    —— 机位名称（可重命名）；</li>
 *   <li>{@code position}—— 摄像机位置（4D）；</li>
 *   <li>{@code view}    —— 摄像机视线的四条坐标轴；</li>
 *   <li>{@code orientation} —— 记录时的 6 平面姿态角（来自 {@code SceneInterface.get4DCameraOrientation()}，
 *       只用于展示；可能为 null）。</li>
 * </ul>
 *
 * <p>位置与视线在构造 / 读取时都做深拷贝，外部拿到的永远是副本，改不坏内部数据。
 */
public final class CameraRecord {

    private String name;
    private final Vector4f position;
    private final Coordinate4D view;
    private final CameraOrientation orientation;

    public CameraRecord(String name, Vector4f position, Coordinate4D view, CameraOrientation orientation) {
        this.name = name;
        this.position = new Vector4f(position);
        this.view = copyView(view);
        this.orientation = orientation;
    }

    public String getName() {
        return name;
    }

    /** 重命名（右键菜单 / UIInterface 调用）。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 摄像机位置（副本）。 */
    public Vector4f getPosition() {
        return new Vector4f(position);
    }

    /** 摄像机视线四条轴（副本）。 */
    public Coordinate4D getView() {
        return copyView(view);
    }

    /** 记录时的 6 平面姿态角；未知时为 null。 */
    public CameraOrientation getOrientation() {
        return orientation;
    }

    /** 深拷贝一份（用于对外返回）。 */
    public CameraRecord copy() {
        return new CameraRecord(name, position, view, orientation);
    }

    private static Coordinate4D copyView(Coordinate4D v) {
        return new Coordinate4D(
                new Vector4f(v.getVx()), new Vector4f(v.getVy()),
                new Vector4f(v.getVz()), new Vector4f(v.getVw()));
    }
}
