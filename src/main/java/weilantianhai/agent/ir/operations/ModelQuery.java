package weilantianhai.agent.ir.operations;

import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 操作类共用的模型查询小工具。
 *
 * <p>文档里不少返回文案要带上"现在有几个模型""这个模型有多少四面体"，
 * 但渲染接口只提供 {@code listModel()}（根路径）与 {@code getModel(path)}，
 * 相关拼装收敛在这里。
 */
final class ModelQuery {

    private ModelQuery() {}

    /** 当前场景的**根模型**数量（{@code LIST_MODEL} 的长度） */
    static int rootCount(RendererInterface renderer) {
        return renderer.getModel().listModel().size();
    }

    /**
     * 取某个路径下模型自身的四面体数量；路径不存在或不是真实模型时返回 -1。
     *
     * <p>注意 {@code ModelInterface.getModel} 走的是精确匹配，纯分组路径会抛异常，
     * 因此这里吞掉异常返回 -1，由调用方决定怎么显示。
     */
    static int tetrahedronCount(RendererInterface renderer, String path) {
        try {
            return renderer.getModel().getModel(path).getTetrahedrons().size();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** 把四面体数量格式化成可显示文本，取不到时给 "unknown" */
    static String tetrahedronText(RendererInterface renderer, String path) {
        int n = tetrahedronCount(renderer, path);
        return n < 0 ? "unknown" : String.valueOf(n);
    }

    /**
     * 重命名后的完整路径：把 {@code path} 的最后一段换成 {@code newName}。
     * {@code newName} 是叶名，即使里面带 {@code /} 也不会新建层级（与渲染实现一致）。
     */
    static String renamedPath(String path, String newName) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? newName : path.substring(0, slash + 1) + newName;
    }
}
