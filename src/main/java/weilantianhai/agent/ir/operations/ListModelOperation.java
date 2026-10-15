package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

import java.util.List;

/**
 * LIST_MODEL：列出场景中**所有模型**的完整路径（文档 §4.1.1）。无参数。
 *
 * <pre>
 * # LIST_MODEL(id=6)
 * 0. "Tower"
 * 1. "Tower/Base"
 * 2. "Gallery"
 * </pre>
 *
 * <h3>为什么用 {@code listAllModels()} 而不是 {@code listModel()}</h3>
 *
 * <p>渲染侧两个接口都在：
 * <ul>
 *   <li>{@code listModel()} —— 只返回**根路径**（第一段去重），子模型完全看不到；</li>
 *   <li>{@code listAllModels()} —— 返回**每个模型的完整路径**。</li>
 * </ul>
 *
 * <p>这里用后者：AI 需要的是"场景里到底有哪些东西、能拿哪个路径去操作"，
 * 只给根路径的话它还得再发一次 {@code GET_MODEL_INFO} 才知道下面挂了什么。
 *
 * <p>附带的好处：这个列表的长度与 {@code SceneSnapshotter} 采的模型数
 * （即数据集判据里的 {@code model.count}）**口径一致** —— 都是"场景里有多少个模型"，
 * 而不是"有多少个根"。用 {@code listModel()} 时回执里的计数会和判据对不上。
 *
 * <p>要只看某个模型下面的部件，用 {@code GET_MODEL_INFO}。
 */
public class ListModelOperation extends GuardedOperation {

    @Override
    public String name() {
        return "LIST_MODEL";
    }

    @Override
    protected void parse(JSONObject data) {
        // 无参数操作
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        List<String> paths = renderer.getModel().listAllModels();
        if (paths.isEmpty()) {
            return "No models in scene";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i).append(". \"").append(paths.get(i)).append('"');
        }
        return sb.toString();
    }
}
