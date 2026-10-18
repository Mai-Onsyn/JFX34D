package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class GoCameraBookmarkOperation extends GuardedOperation {
    private JSONObject data;

    @Override
    public String name() {
        return "GO_CAMERA_BOOKMARK";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        // 参数校验推迟到执行时：寻址需要先拿到机位列表（在 run 中读取）。
        this.data = data == null ? new JSONObject() : data;
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        int index = CameraBookmarkResolver.resolve(data, name());
        try {
            return FxThreads.call(() -> {
                var ui = HCloudbyte.ui.interfaces.UIInterface.getInstance();
                var record = ui.getCameraRecords().get(index);   // 取副本，供回显
                ui.applyCameraRecord(index);
                return "Success\n已开始切换到机位 [" + index + "] " + record.getName()
                        + "（过渡约 1 秒）"
                        + "\npos=" + OperationParams.formatVector4(record.getPosition());
            });
        } catch (Exception e) {
            throw new IRException("GO_CAMERA_BOOKMARK_FAILED",
                    ListCameraBookmarksOperation.uiMessage(e));
        }
    }
}
