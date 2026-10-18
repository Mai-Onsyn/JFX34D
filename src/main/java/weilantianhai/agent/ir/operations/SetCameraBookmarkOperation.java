package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import HCloudbyte.ui.viewport.CameraRecord;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

public class SetCameraBookmarkOperation extends GuardedOperation {
    private String name;
    private Vector4f pos;
    /** 已解析好的朝向；三种来源之一。null 表示用单位基。 */
    private Coordinate4D view;

    @Override
    public String name() {
        return "SET_CAMERA_BOOKMARK";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredString(data, "name", name());
        this.pos = OperationParams.getRequiredVector4(data, "pos", "POS");

        JSONObject viewObj = data == null ? null : data.getJSONObject("view");
        Vector4f lookAt = OperationParams.getOptionalVector4(data, "look_at", "LOOK_AT");

        if (viewObj != null && lookAt != null) {
            throw new IRException("SET_CAMERA_BOOKMARK_AMBIGUOUS",
                    "view 与 look_at 不能同时给出，请二选一（推荐 look_at）");
        }

        if (lookAt != null) {
            // 【首选】位置 + 看向的点 → lookAt 自动算朝向
            if (pos.equals(lookAt, 1e-6f)) {
                throw new IRException("SET_CAMERA_BOOKMARK_DEGENERATE",
                        "pos 与 look_at 不能相同（视线方向为零向量，朝向无法确定）");
            }
            this.view = CameraRecord.lookAt(name, new Vector4f(pos), new Vector4f(lookAt)).getView();
            return;
        }

        if (viewObj == null) {
            // 都不给 → 单位基（正对）
            this.view = new Coordinate4D(
                    new Vector4f(1, 0, 0, 0), new Vector4f(0, 1, 0, 0),
                    new Vector4f(0, 0, 1, 0), new Vector4f(0, 0, 0, 1));
            return;
        }

        // 显式四基：校验单位 + 两两正交（规则同 SET_CAMERA_VIEW）
        Vector4f vx = OperationParams.getRequiredVector4(viewObj, "vx", "VX");
        Vector4f vy = OperationParams.getRequiredVector4(viewObj, "vy", "VY");
        Vector4f vz = OperationParams.getRequiredVector4(viewObj, "vz", "VZ");
        Vector4f vw = OperationParams.getRequiredVector4(viewObj, "vw", "VW");

        OperationParams.requireUnit(vx, "vx", "VECTOR");
        OperationParams.requireUnit(vy, "vy", "VECTOR");
        OperationParams.requireUnit(vz, "vz", "VECTOR");
        OperationParams.requireUnit(vw, "vw", "VECTOR");

        OperationParams.requireOrthogonal(vx, vy, "vx", "vy", "VECTOR");
        OperationParams.requireOrthogonal(vx, vz, "vx", "vz", "VECTOR");
        OperationParams.requireOrthogonal(vx, vw, "vx", "vw", "VECTOR");
        OperationParams.requireOrthogonal(vy, vz, "vy", "vz", "VECTOR");
        OperationParams.requireOrthogonal(vy, vw, "vy", "vw", "VECTOR");
        OperationParams.requireOrthogonal(vz, vw, "vz", "vw", "VECTOR");

        this.view = new Coordinate4D(vx, vy, vz, vw);
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        try {
            return FxThreads.call(() -> {
                var ui = HCloudbyte.ui.interfaces.UIInterface.getInstance();

                // 重名校验：机位名应当唯一，否则后续按 name 寻址会歧义
                for (CameraRecord r : ui.getCameraRecords()) {
                    if (name.equals(r.getName())) {
                        throw new IRException("SET_CAMERA_BOOKMARK_DUPLICATE",
                                "已存在名为 \"" + name + "\" 的机位，请换一个名字或先删除旧机位");
                    }
                }

                CameraRecord record = new CameraRecord(name, new Vector4f(pos), view, null);
                ui.addCameraRecord(record);

                int index = ui.getCameraRecords().size() - 1;
                return "Success\n已创建机位 [" + index + "] " + name
                        + "\npos=" + OperationParams.formatVector4(pos)
                        + "\nview:\n" + CameraViewFormatter.format(record.getView());
            });
        } catch (IRException e) {
            throw e;   // 重名等业务错误原样上抛
        } catch (Exception e) {
            throw new IRException("SET_CAMERA_BOOKMARK_FAILED",
                    ListCameraBookmarksOperation.uiMessage(e));
        }
    }
}
