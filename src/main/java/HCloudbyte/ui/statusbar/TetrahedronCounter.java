package HCloudbyte.ui.statusbar;

import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * 场景四面体总数统计。
 *
 * <p>{@code getModelInfos} 返回 markdown 文本：实体 mesh 报 {@code "Tetrahedrons: N"}，
 * 分组路径报 {@code "Total tetrahedrons: N"}。逐个根路径取信息并解析，优先汇总值。
 * 解析有开销，调用方（状态栏）做了低频刷新（约 2 秒一次）。
 */
final class TetrahedronCounter {

    private TetrahedronCounter() {
    }

    /** 统计全场景四面体总数；个别模型解析失败不影响整体（返回已有累加值）。 */
    static int count(RendererInterface renderer) {
        int total = 0;
        try {
            for (String root : renderer.getModel().listModel()) {
                String info = renderer.getGeometry().getModelInfos(root);
                int n = parseCount(info, "Total tetrahedrons: ");   // 分组路径的汇总
                if (n == 0) n = parseCount(info, "Tetrahedrons: "); // 实体模型的单模型数
                total += n;
            }
        } catch (Exception ignored) {
            // 个别模型信息解析失败不影响整体显示
        }
        return total;
    }

    /** 从 markdown 信息串里解析 "key N" 的数字部分，找不到返回 0。 */
    private static int parseCount(String text, String key) {
        int i = text.indexOf(key);
        if (i < 0) return 0;
        int j = i + key.length();
        int k = j;
        while (k < text.length() && Character.isDigit(text.charAt(k))) k++;
        return k > j ? Integer.parseInt(text.substring(j, k)) : 0;
    }
}
