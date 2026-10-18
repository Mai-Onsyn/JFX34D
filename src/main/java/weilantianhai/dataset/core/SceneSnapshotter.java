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

public final class SceneSnapshotter {
    private final GL4DRegion region;
    private final RendererInterface renderer;
    /** 复位时是否回到"初始场景"（false = 清成空场景）。运行时实时读取，开关立即生效。 */
    private final java.util.function.BooleanSupplier useBaseline;

    public SceneSnapshotter(GL4DRegion region, RendererInterface renderer,
                            java.util.function.BooleanSupplier useBaseline) {
        this.region = region;
        this.renderer = renderer;
        this.useBaseline = useBaseline;
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

    public void reset() throws Exception {
        FxThreads.run(this::resetOnFx);
        verifyBaseline();
    }

    /**
     * 校验当前状态确实是复位后的期望状态。用于 {@link #reset()} 之后，也可单独调用。
     *
     * @throws IllegalStateException 不符时，异常消息里带上实际值
     */
    public void verifyBaseline() throws Exception {
        JSONObject snap = snapshot();
        List<String> models = snap.getJSONArray("models").toJavaList(String.class);
        List<String> expected = expectedModels();
        if (!new java.util.TreeSet<>(models).equals(new java.util.TreeSet<>(expected))) {
            throw new IllegalStateException(
                    "复位后模型树与期望不符：期望 " + expected + "，实际 " + models
                            + " —— 上一题的模型没清干净（或初始场景没加载成功），本题结论不可信");
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

    /** 复位后**期望**看到哪些模型（空场景 → 空表；初始场景 → meta 里记的那些）。 */
    public List<String> expectedModels() {
        return useBaseline.getAsBoolean() && SceneBaseline.exists()
                ? SceneBaseline.expectedModels()
                : List.of();
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

        // 4. 按模式决定要不要把「初始场景」放回来。
        //    不塞任何 seed 模型 —— 清空就是清空。
        if (useBaseline.getAsBoolean() && SceneBaseline.exists()) {
            try {
                SceneBaseline.load(renderer);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "加载初始场景失败（" + SceneBaseline.dir() + "）：" + e.getMessage(), e);
            }
        }
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
