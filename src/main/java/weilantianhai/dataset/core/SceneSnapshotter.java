package weilantianhai.dataset.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.cpu4dkt.Camera4D;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import org.joml.Vector4f;

import java.util.List;
import weilantianhai.agent.ir.operations.FxThreads;

/**
 * 场景快照采集 + 任务间复位。所有操作都在 FX 线程上做。
 *
 * <h3>为什么要复位</h3>
 *
 * <p>每个任务的起始状态必须完全一致，否则数据集里的差异来自"上一题的残留"而非"模型能力"。
 * 软复位清空 4D 模型树 + 机位 + 相机姿态，并放回一个固定的初始模型。
 */
public final class SceneSnapshotter {

    private final GL4DRegion region;
    private final RendererInterface renderer;
    /** 复位时放回的初始模型（保证每题起点一致）。 */
    private final String seedModelName;

    public SceneSnapshotter(GL4DRegion region, RendererInterface renderer, String seedModelName) {
        this.region = region;
        this.renderer = renderer;
        this.seedModelName = seedModelName;
    }

    /* ==================== 采集 ==================== */

    /** 采集当前世界状态。可在任意线程调用（内部切 FX 线程）。 */
    public JSONObject snapshot() throws Exception {
        return FxThreads.call(this::snapshotOnFx);
    }

    private JSONObject snapshotOnFx() {
        JSONObject o = new JSONObject();

        // 模型树：全量路径
        JSONArray models = new JSONArray();
        List<Mesh4D> meshes = region.getScene4D().getMeshList();
        for (Mesh4D m : meshes) {
            models.add(m.getName());
        }
        o.put("models", models);

        // 模型信息（顶点/四面体数）——只对非 GROUP 的实体模型取
        JSONObject info = new JSONObject();
        for (Mesh4D m : meshes) {
            JSONObject mi = new JSONObject();
            mi.put("tetrahedrons", m.getTetrahedrons().size());
            mi.put("vertices", m.getTetrahedrons().size() * 4);
            mi.put("kind", m.getKind().name());
            mi.put("visible", m.getVisible());
            info.put(m.getName(), mi);
        }
        o.put("model_info", info);

        // 相机：从底层 Camera4D 直接读（不走 AnimatedValue 缓存，取"屏幕真值"）
        Camera4D cam = region.getScene4D().getCamera();
        o.put("camera_pos", vec(cam.getPos()));
        JSONObject view = new JSONObject();
        view.put("vx", vec(cam.getVx()));
        view.put("vy", vec(cam.getVy()));
        view.put("vz", vec(cam.getVz()));
        view.put("vw", vec(cam.getVw()));
        o.put("camera_view", view);

        // 机位
        JSONArray bookmarks = new JSONArray();
        try {
            var ui = HCloudbyte.ui.interfaces.UIInterface.getInstance();
            List<HCloudbyte.ui.viewport.CameraRecord> records = ui.getCameraRecords();
            for (HCloudbyte.ui.viewport.CameraRecord r : records) {
                JSONObject b = new JSONObject();
                b.put("name", r.getName());
                b.put("pos", vec(r.getPosition()));
                bookmarks.add(b);
            }
        } catch (Throwable t) {
            o.put("bookmarks_error", t.getMessage());
        }
        o.put("bookmarks", bookmarks);

        return o;
    }

    /* ==================== 复位 ==================== */

    /**
     * 软复位：清空 4D 模型树与机位，相机回到基线，放回初始模型。
     *
     * <p>为什么清的是 {@code scene4D.meshList} 而不是调 {@code DELETE_MODEL}：
     * 前者是一次性 list 操作，不受"路径是否存在于中间层"的影响；后者可能因为残留的
     * 中间层路径而删不干净。
     *
     * <h3>复位后自检</h3>
     *
     * <p>{@code reset()} 之后立刻采一次快照，与上方常量描述的**基线**比对。
     * 若不一致就抛异常 —— 因为"复位没复位干净"会让后续所有题的结论都不可信，
     * 而且这种错误**静默**（界面上看不出来），是最危险的一类。
     *
     * @throws IllegalStateException 复位后状态与基线不符
     */
    public void reset() throws Exception {
        FxThreads.run(this::resetOnFx);
        verifyBaseline();
    }

    /**
     * 校验当前状态确实是基线状态。用于 {@link #reset()} 之后，也可单独调用。
     *
     * @throws IllegalStateException 不符时，异常消息里带上实际值
     */
    public void verifyBaseline() throws Exception {
        JSONObject snap = snapshot();
        List<String> models = snap.getJSONArray("models").toJavaList(String.class);
        if (!models.equals(List.of(seedModelName))) {
            throw new IllegalStateException(
                    "复位后模型树不是基线：期望 [" + seedModelName + "]，实际 " + models
                            + " —— 上一题的模型没清干净，本题结论不可信");
        }
        List<?> bookmarks = snap.getJSONArray("bookmarks");
        if (bookmarks != null && !bookmarks.isEmpty()) {
            throw new IllegalStateException(
                    "复位后机位不为空（" + bookmarks.size() + " 个）—— 上一题的机位没清干净");
        }
        Vector4f pos = SceneSnapshot.readVec(snap.getJSONArray("camera_pos"));
        if (pos == null || !vecEq(pos, BASELINE_POS, 1e-3f)) {
            throw new IllegalStateException("复位后相机位置不是基线 ("
                    + BASELINE_POS.x + ", " + BASELINE_POS.y + ", "
                    + BASELINE_POS.z + ", " + BASELINE_POS.w + ")，实际 " + pos);
        }
    }

    /** 复位基线：相机位于 z 轴负方向 10 处，朝向单位姿态。 */
    private static final Vector4f BASELINE_POS = new Vector4f(0f, 0f, -10f, 0f);

    private static boolean vecEq(Vector4f a, Vector4f b, float eps) {
        return Math.abs(a.x - b.x) <= eps && Math.abs(a.y - b.y) <= eps
                && Math.abs(a.z - b.z) <= eps && Math.abs(a.w - b.w) <= eps;
    }

    private void resetOnFx() {
        // 1. 清 4D 模型树
        region.getScene4D().getMeshList().clear();

        // 2. 清机位
        try {
            var ui = HCloudbyte.ui.interfaces.UIInterface.getInstance();
            while (!ui.getCameraRecords().isEmpty()) {
                ui.removeCameraRecord(0);
            }
        } catch (Throwable t) {
            System.err.println("[dataset] 清机位失败: " + t.getMessage());
        }

        // 3. 相机回到基线 —— 直接写底层 Camera4D，绕过 AnimatedValue 动画
        Camera4D cam = region.getScene4D().getCamera();
        cam.getPos().set(BASELINE_POS.x, BASELINE_POS.y, BASELINE_POS.z, BASELINE_POS.w);
        cam.getVx().set(1f, 0f, 0f, 0f);
        cam.getVy().set(0f, 1f, 0f, 0f);
        cam.getVz().set(0f, 0f, 1f, 0f);
        cam.getVw().set(0f, 0f, 0f, 1f);

        // 4. 放回初始模型
        renderer.getShape().createTesseract("", seedModelName, new Vector4f(0, 0, 0, 0), 1);
    }

    private static JSONArray vec(Vector4f v) {
        JSONArray a = new JSONArray();
        a.add(v.x);
        a.add(v.y);
        a.add(v.z);
        a.add(v.w);
        return a;
    }
}
