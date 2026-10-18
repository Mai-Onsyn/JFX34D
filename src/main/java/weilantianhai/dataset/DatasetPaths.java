package weilantianhai.dataset;

import java.nio.file.Path;

public final class DatasetPaths {
    /** 项目里所有数据的总目录。 */
    public static final Path DATA_ROOT = Path.of("data");

    /** 数据集产物根目录。 */
    public static final Path DATASET_ROOT = DATA_ROOT.resolve("dataset");

    /** 题库目录（存放 {@code *.jsonl}）。 */
    public static final Path TASKS_DIR = DATA_ROOT.resolve("tasks");

    /** 场景资源目录（初始场景等）。 */
    public static final Path SCENE_ROOT = DATA_ROOT.resolve("scene");

    /** **初始场景**（复位时回到的那份场景）。 */
    public static final Path BASELINE_DIR = SCENE_ROOT.resolve("baseline");

    /** 题库清单里代表「内置 14 题」的哨兵值（不是文件路径）。 */
    public static final String BUILTIN_BANK = "(内置 14 题)";

    private DatasetPaths() {
    }

    /** 输出绝对路径，便于日志里显示（相对路径在 IDE 里看不出来到底写哪了）。 */
    public static Path resolve(Path p) {
        return p.toAbsolutePath().normalize();
    }

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
