package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * SAVE_CAMERA_BOOKMARK：把**当前相机状态**存成一个机位。
 *
 * <p>参数：{@code name}（可选，缺省自动命名 "机位N"）。
 *
 * <p>等价于 UI 上的「记录」按钮，但可以自定义名称。
 * 返回值里带上了新机位的下标，方便后续直接 {@code GO_CAMERA_BOOKMARK}。
 */
public class SaveCameraBookmarkOperation extends GuardedOperation {

    private String name;

    @Override
    public String name() {
        return "SAVE_CAMERA_BOOKMARK";
    }

    @Override
    protected void parse(JSONObject data) {
        this.name = OperationParams.getOptionalString(data, "name", null);
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        try {
            return FxThreads.call(() -> {
                var ui = HCloudbyte.ui.interfaces.UIInterface.getInstance();

                // 重名校验：机位名应当唯一，否则后续按 name 寻址会歧义。
                // 只在调用方显式给了 name 时校验；自动命名（name==null）由 UI 生成 "机位N"，不会撞名。
                if (name != null && !name.isBlank()) {
                    for (var r : ui.getCameraRecords()) {
                        if (name.equals(r.getName())) {
                            throw new IRException("SAVE_CAMERA_BOOKMARK_DUPLICATE",
                                    "已存在名为 \"" + name + "\" 的机位，请换一个名字或先删除旧机位");
                        }
                    }
                }

                int index = ui.captureCameraRecord(name);
                // 读回刚存的机位，回显位置（此时 UI 已更新，getCameraRecords 必含它）
                var record = ui.getCameraRecords().get(index);
                return "Success\n已保存机位 [" + index + "] " + record.getName()
                        + "\npos=" + OperationParams.formatVector4(record.getPosition());
            });
        } catch (IRException e) {
            throw e;   // 重名等业务错误原样上抛
        } catch (Exception e) {
            throw new IRException("SAVE_CAMERA_BOOKMARK_FAILED",
                    ListCameraBookmarksOperation.uiMessage(e));
        }
    }
}
