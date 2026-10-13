package weilantianhai.dataset;

import java.nio.file.Path;

/**
 * 数据集模块的**路径总表** —— 所有目录都从这里取，别在别处写死字符串。
 *
 * <h3>为什么不用 {@code .workbuddy/}</h3>
 *
 * <p>数据集是**要长期保留、要进版本库、要拿出来对比**的实验产物，
 * 而 {@code .workbuddy/} 是工具自己的临时/会话目录（在 {@code .gitignore} 里被忽略），
 * 放那里既会被当成缓存清理，也拿不出去给别人看。所以单独开一个项目级目录：
 *
 * <pre>
 * &lt;项目根&gt;/data/dataset/                 ← 数据集产物（要提交、要归档）
 * ├── raw/&lt;date&gt;/&lt;model&gt;__&lt;category&gt;.jsonl
 * ├── summary/compare__&lt;date&gt;.md
 * └── index.jsonl
 *
 * &lt;项目根&gt;/data/tasks/                   ← 题库（可多个，启动时在开始页里选）
 * ├── builtin.jsonl                       内置 14 题（安装时从 resources 导出）
 * └── generated-&lt;ts&gt;.jsonl                LLM 出题器产出
 * </pre>
 *
 * <p>两者都放在 {@code data/} 之下：一个是"实验数据"，一个是"实验输入"，
 * 一个目录就能整体打包/对比/删除。
 *
 * <p><b>注意</b>：这里给的是**相对路径**（相对于进程工作目录，IDE 里就是项目根）。
 * 需要绝对路径时调 {@link #resolve(Path)}。
 */
public final class DatasetPaths {

    /** 项目里所有数据的总目录。 */
    public static final Path DATA_ROOT = Path.of("data");

    /** 数据集产物根目录。 */
    public static final Path DATASET_ROOT = DATA_ROOT.resolve("dataset");

    /** 题库目录（存放 {@code *.jsonl}）。 */
    public static final Path TASKS_DIR = DATA_ROOT.resolve("tasks");

    /** 题库清单里代表「内置 14 题」的哨兵值（不是文件路径）。 */
    public static final String BUILTIN_BANK = "(内置 14 题)";

    private DatasetPaths() {
    }

    /** 输出绝对路径，便于日志里显示（相对路径在 IDE 里看不出来到底写哪了）。 */
    public static Path resolve(Path p) {
        return p.toAbsolutePath().normalize();
    }

    /**
     * 把相对路径锚定到「项目根」—— 而不是当前工作目录。
     *
     * <p>IDE 的 Run 配置有时会把工作目录设成模块目录甚至别处，
     * 那样相对路径 {@code data/dataset} 就会落到意料之外的地方。这里向上找
     * {@code pom.xml} 作为项目根标记；找不到就退回当前工作目录。
     *
     * @param relative 期望相对于项目根的路径（如 {@code data/dataset}）
     */
    public static Path anchorToProjectRoot(Path relative) {
        Path root = findProjectRoot();
        return root == null ? relative : root.resolve(relative);
    }

    /** 从当前工作目录向上找 {@code pom.xml}；找不到返回 null。 */
    private static Path findProjectRoot() {
        Path dir = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (java.nio.file.Files.exists(dir.resolve("pom.xml"))) return dir;
            dir = dir.getParent();
        }
        return null;
    }
}
