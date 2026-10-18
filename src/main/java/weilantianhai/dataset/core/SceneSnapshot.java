package weilantianhai.dataset.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class SceneSnapshot {
    /** 容差：位置/向量比较的默认阈值。 */
    public static final float EPS = 1e-3f;

    private final JSONObject json;

    public SceneSnapshot(JSONObject json) {
        this.json = json == null ? new JSONObject() : json;
    }

    public JSONObject toJson() {
        return json;
    }

    /* ==================== 读取视图 ==================== */

    /** 模型树的全部路径。 */
    public List<String> modelPaths() {
        JSONArray arr = json.getJSONArray("models");
        return arr == null ? List.of() : arr.toJavaList(String.class);
    }

    public Vector4f cameraPos() {
        return readVec(json.getJSONArray("camera_pos"));
    }

    /** 机位名字列表。 */
    public List<String> bookmarkNames() {
        JSONArray arr = json.getJSONArray("bookmarks");
        if (arr == null) return List.of();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            names.add(arr.getJSONObject(i).getString("name"));
        }
        return names;
    }

    /** 按名字取机位的位置；不存在返回 null。 */
    public Vector4f bookmarkPos(String name) {
        JSONArray arr = json.getJSONArray("bookmarks");
        if (arr == null) return null;
        for (int i = 0; i < arr.size(); i++) {
            JSONObject b = arr.getJSONObject(i);
            if (name.equals(b.getString("name"))) {
                return readVec(b.getJSONArray("pos"));
            }
        }
        return null;
    }

    /** 模型信息：{@code model_info: { "path": { "vertices": n, "tetrahedrons": n } }}。 */
    public JSONObject modelInfo(String path) {
        JSONObject info = json.getJSONObject("model_info");
        return info == null ? null : info.getJSONObject(path);
    }

    /* ==================== 工具 ==================== */

    public static boolean vecEq(Vector4f a, Vector4f b, float eps) {
        if (a == null || b == null) return false;
        return Math.abs(a.x - b.x) <= eps
                && Math.abs(a.y - b.y) <= eps
                && Math.abs(a.z - b.z) <= eps
                && Math.abs(a.w - b.w) <= eps;
    }

    public static Vector4f readVec(JSONArray arr) {
        if (arr == null || arr.size() < 4) return null;
        return new Vector4f(
                arr.getFloatValue(0), arr.getFloatValue(1),
                arr.getFloatValue(2), arr.getFloatValue(3));
    }
}
