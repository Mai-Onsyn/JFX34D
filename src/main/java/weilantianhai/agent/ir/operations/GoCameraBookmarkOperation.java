package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GO_CAMERA_BOOKMARK：跳转到指定机位（等价于 UI 上左键点击机位行）。
 *
 * <p>参数：{@code name} 或 {@code index}（二选一，name 优先）。
 *
 * <p><b>注意</b>：底层 {@code setPosition} / {@code setView} 会触发**约 1 秒的平滑过渡动画**，
 * 所以返回的 {@code pos/view} 是动画**开始时**的值，不是终值。跳转后不要立即
 * {@code GET_CAMERA_POS} 验证——拿到的是中间帧。
 */
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
