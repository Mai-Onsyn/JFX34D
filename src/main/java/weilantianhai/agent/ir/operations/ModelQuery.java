package weilantianhai.agent.ir.operations;

import mai_onsyn.renderer.interfaces.RendererInterface;

final class ModelQuery {
    private ModelQuery() {}

    static int modelCount(RendererInterface renderer) {
        return renderer.getModel().listAllModels().size();
    }

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
