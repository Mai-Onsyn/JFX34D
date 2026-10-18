package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class RenameCameraBookmarkOperation extends GuardedOperation {
    private JSONObject data;
    private String newName;

    @Override
    public String name() {
        return "RENAME_CAMERA_BOOKMARK";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        JSONObject d = data == null ? new JSONObject() : data;
        // 新名称必须给（名字为空的重命名没有意义）
        this.newName = OperationParams.getRequiredString(d, "new_name", name());
        this.data = d;
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        int index = CameraBookmarkResolver.resolve(data, name());
        try {
            return FxThreads.call(() -> {
                var ui = HCloudbyte.ui.interfaces.UIInterface.getInstance();
                String oldName = ui.getCameraRecords().get(index).getName();
                ui.renameCameraRecord(index, newName);
                return "Success\n机位 [" + index + "] " + oldName + " → " + newName;
            });
        } catch (Exception e) {
            throw new IRException("RENAME_CAMERA_BOOKMARK_FAILED",
                    ListCameraBookmarksOperation.uiMessage(e));
        }
    }
}
