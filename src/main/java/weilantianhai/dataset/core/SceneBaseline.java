package weilantianhai.dataset.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.cpu4dkt.Mesh4D;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.operations.FxThreads;
import weilantianhai.dataset.DatasetPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class SceneBaseline {
    /** 沿用引擎自带的后缀（{@code IOInterface.loadModel} 读的就是这个格式）。 */
    private static final String EXT = ".4do";
    private static final String META = "meta.json";
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SceneBaseline() {
    }

    /** 覆盖初始场景目录的系统属性（便于保留多份、或把场景放到别处）。 */
    public static final String DIR_PROPERTY = "jfx34d.baseline.dir";

    /**
     * 初始场景所在目录。缺省 {@code data/scene/baseline}；
     * 可用系统属性 {@code -Djfx34d.baseline.dir=<绝对路径>} 覆盖。
     */
    public static Path dir() {
        String override = System.getProperty(DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim());
        }
        return DatasetPaths.BASELINE_DIR;
    }

    private static Path metaFile() {
        return dir().resolve(META);
    }

    /** 是否已经保存过初始场景。 */
    public static boolean exists() {
        return Files.isRegularFile(metaFile());
    }

    public static void save(GL4DRegion region, RendererInterface renderer) throws Exception {
        FxThreads.run(() -> saveOnFx(region, renderer));
    }

    private static void saveOnFx(GL4DRegion region, RendererInterface renderer) throws IOException {
        Path dir = dir();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                for (Path p : s.toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
        Files.createDirectories(dir);

        // 取场景里的**根模型**（路径第一段），每个根存一个文件
        Set<String> roots = new LinkedHashSet<>();
        for (Mesh4D m : region.getScene4D().getMeshList()) {
            String n = m.getName();
            if (n == null || n.isBlank()) {
                continue;
            }
            int slash = n.indexOf('/');
            roots.add(slash < 0 ? n : n.substring(0, slash));
        }

        List<String> files = new ArrayList<>();
        for (String root : roots) {
            String fileName = sanitize(root) + EXT;
            Path target = dir.resolve(fileName);
            renderer.getIo().saveModel(root, target.toAbsolutePath().toString());
            if (Files.isRegularFile(target)) {
                files.add(fileName);
            }
        }

        // 从落盘文件里回读 "g" 分组名 —— 那些才是复位后**真正会被恢复**的模型
        // （空容器不落盘，所以不能直接用内存里的模型列表当期望值）
        List<String> expected = new ArrayList<>();
        for (String f : files) {
            expected.addAll(groupNames(dir.resolve(f)));
        }

        JSONObject meta = new JSONObject();
        meta.put("saved_at", LocalDateTime.now().format(STAMP));
        meta.put("roots", new JSONArray(new ArrayList<>(roots)));
        meta.put("files", new JSONArray(files));
        meta.put("expected_models", new JSONArray(expected));
        Files.writeString(metaFile(), meta.toJSONString(), StandardCharsets.UTF_8);
    }

    public static void load(RendererInterface renderer) throws IOException {
        for (Path f : files()) {
            renderer.getIo().loadModel("", f.toFile());
        }
    }

    /** 复位后期望看到的模型列表（读 {@code meta.json}，没有就返回空表）。 */
    public static List<String> expectedModels() {
        JSONObject meta = readMeta();
        if (meta == null) {
            return List.of();
        }
        JSONArray arr = meta.getJSONArray("expected_models");
        return arr == null ? List.of() : new ArrayList<>(arr.toJavaList(String.class));
    }

    /** 人类可读的一行描述，用于日志。 */
    public static String describe() {
        if (!exists()) {
            return "未保存";
        }
        JSONObject meta = readMeta();
        if (meta == null) {
            return "已保存（" + metaFile() + "，但读不出来）";
        }
        List<String> models = expectedModels();
        return meta.getString("saved_at") + " 保存，" + models.size() + " 个模型：" + models
                + "  @ " + dir().toAbsolutePath();
    }

    /* ==================== 内部 ==================== */

    private static List<Path> files() throws IOException {
        Path dir = dir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(EXT)).sorted().toList();
        }
    }

    /** 读 {@code .4do} 里的 {@code g <名字>} 行，得到会被恢复的模型路径。 */
    private static List<String> groupNames(Path file) throws IOException {
        List<String> out = new ArrayList<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.startsWith("g ")) {
                String name = line.substring(2).strip();
                if (!name.isEmpty()) {
                    out.add(name);
                }
            }
        }
        return out;
    }

    private static JSONObject readMeta() {
        if (!exists()) {
            return null;
        }
        try {
            return JSONObject.parseObject(Files.readString(metaFile(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[baseline] meta.json 读取失败: " + e.getMessage());
            return null;
        }
    }

    /** 文件名安全化（模型名可能含路径分隔符）。 */
    private static String sanitize(String s) {
        String out = s.strip().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return out.isBlank() ? "model" : out;
    }
}
