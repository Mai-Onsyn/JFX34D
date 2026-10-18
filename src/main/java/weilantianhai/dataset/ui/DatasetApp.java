package weilantianhai.dataset.ui;

import HCloudbyte.ui.bootstrap.AgentBootstrap;
import HCloudbyte.ui.bootstrap.RenderBootstrap;
import HCloudbyte.ui.interfaces.UIInterface;
import HCloudbyte.ui.viewport.CameraBookmarksView;
import atlantafx.base.theme.PrimerDark;
import com.alibaba.fastjson2.JSONObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import mai_onsyn.renderer.core.GL4DRegion;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.llm.LLMClient;
import weilantianhai.agent.llm.PromptLoader;
import weilantianhai.dataset.DatasetPaths;
import weilantianhai.dataset.core.AgentTaskRunner;
import weilantianhai.dataset.core.CheckEvaluator;
import weilantianhai.dataset.core.SceneBaseline;
import weilantianhai.dataset.core.SceneSnapshot;
import weilantianhai.dataset.core.SceneSnapshotter;
import weilantianhai.dataset.core.TaskRunRecord;
import weilantianhai.dataset.store.DatasetStore;
import weilantianhai.dataset.summary.MetricsAggregator;
import weilantianhai.dataset.tasks.TaskBanks;
import weilantianhai.dataset.tasks.TaskGenerator;
import weilantianhai.dataset.tasks.TaskGoal;
import weilantianhai.dataset.tasks.TaskLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatasetApp extends Application {
    /** 模型下拉框的两个取值（同时也是落盘用的模型标识）。 */
    public static final String MODEL_DEEPSEEK = "deepseek";
    public static final String MODEL_QWEN = "qwen";

    /** 任务来源：题库名（见 {@link TaskBanks}）。 */
    private static String bankName = TaskBanks.BUILTIN;
    /** 数据集根目录；null 表示用默认的 {@link DatasetPaths#DATASET_ROOT}。 */
    private static Path datasetRoot;
    /** 全自动模式：不弹打分框，直接按判据落盘。 */
    private static boolean autoScoreOnly = false;
    /** 起始被测试模型（可由 --model= 覆盖）。 */
    private static String modelKey = MODEL_DEEPSEEK;
    /**
     * 出题模式参数：{@code --generate} 时**只出题不跑循环**，
     * 生成结果写 JSONL 后退出。用 {@code --count/--difficulty/--category} 控制。
     */
    private static boolean generateMode = false;
    private static int genCount = 20;
    private static String genDifficulty = "mixed";
    private static String genCategory = "mixed";
    /** {@code --no-start-page}：跳过开始页直接跑（给自动/批量场景用）。 */
    private static boolean skipStartPage = false;
    /** {@code --keep-scene}：题目之间不清场景。实例属性在 {@link #start} 里才建，所以先记在静态字段。 */
    private static boolean keepSceneCli = false;
    /** {@code --baseline}：强制复位到初始场景（即便尚未保存过，也会照此设置）。 */
    private static boolean baselineCli = false;

    private GL4DRegion region;
    private RendererInterface renderer;
    private SceneSnapshotter snapshotter;
    private DatasetStore store;

    private ScorePanel scorePanel;
    private StartPanel startPanel;
    private Label progressLabel;
    private TextArea logArea;
    private ComboBox<String> modelBox;
    private ComboBox<String> bankBox;

    private final javafx.beans.property.BooleanProperty writeToDatasetProp =
            new javafx.beans.property.SimpleBooleanProperty(true);

    private final javafx.beans.property.BooleanProperty keepSceneProp =
            new javafx.beans.property.SimpleBooleanProperty(false);

    private final javafx.beans.property.BooleanProperty baselineResetProp =
            new javafx.beans.property.SimpleBooleanProperty(SceneBaseline.exists());

    /** 已完成的记录，供「完整 trace」弹窗用。 */
    private final Map<Integer, TaskRunRecord> recordsByTask = new ConcurrentHashMap<>();

    /** 当前被测试模型的客户端（切换模型时整体替换）。 */
    private volatile LLMClient activeClient;

    /** 主循环线程；只在「开始」时创建一次。 */
    private volatile Thread runnerThread;
    /** Agent 是否可用（开始页据此禁用按钮）。 */
    private volatile boolean agentReady = false;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 命令行开关在建 StartPanel 之前落到属性上，这样开始页/顶栏的状态与之一致
        keepSceneProp.set(keepSceneCli);
        if (baselineCli) {
            baselineResetProp.set(true);
        }

        // ---- 引导渲染 ----
        RenderBootstrap.Result render = RenderBootstrap.create();
        this.region = render.region();
        this.renderer = render.renderer();

        // ---- 引导 Agent ----
        AgentBootstrap.Result agent = AgentBootstrap.init(render.renderer());
        // AgentBootstrap 目前固定用 DeepSeek；从接口回读，便于后续切换
        if (agent.ready()) {
            this.activeClient = AgentInterface.getInstance().getClient();
            this.agentReady = true;
        }

        // ---- 出题模式：不建窗口，直接出题后退出 ----
        if (generateMode) {
            stage.close();
            runGenerateOnly();
            return;
        }

        // ---- 机位悬浮窗：游离实例，只当机位存储用（不放进可视树也能工作） ----
        CameraBookmarksView overlay = new CameraBookmarksView(render.renderer());
        UIInterface.initialize(null, overlay);

        this.snapshotter = new SceneSnapshotter(region, renderer,
                () -> baselineResetProp.get() && SceneBaseline.exists());

        // ---- 装配窗口 ----
        StackPane viewportStack = new StackPane(region);
        // 机位悬浮窗放在左上角，方便肉眼确认 AI 是否真的建了机位
        StackPane.setAlignment(overlay, Pos.TOP_LEFT);
        StackPane.setMargin(overlay, new Insets(8));
        viewportStack.getChildren().add(overlay);

        scorePanel = new ScorePanel(this::showTraceDialog);
        StackPane.setAlignment(scorePanel, Pos.TOP_RIGHT);
        StackPane.setMargin(scorePanel, new Insets(8));
        viewportStack.getChildren().add(scorePanel);

        // ---- 开始页：只盖住 4D 视口，不盖顶栏 ----
        //
        // 早先这里是铺满整个窗口的（scene.setRoot(new StackPane(root, startPanel))），
        // 结果把顶栏的「出题…」按钮挡住了 —— 用户想"先出题再开始"根本点不到。
        // 现在它只是 viewportStack 的一层：顶栏与底部日志都保持可见可用，
        // 出题过程也能实时看到日志。
        startPanel = new StartPanel(modelKey, writeToDatasetProp, keepSceneProp,
                this::onStartClicked, this::showGeneratorDialog);
        StackPane.setAlignment(startPanel, Pos.CENTER);
        viewportStack.getChildren().add(startPanel);

        BorderPane root = new BorderPane();
        root.setCenter(viewportStack);
        root.setTop(buildHeader());
        root.setBottom(buildLogPane());

        Scene scene = new Scene(root, 1280, 860);
        scene.setFill(Color.web("#232527"));
        stage.setTitle("JFX34D 数据集生成器");
        stage.setScene(scene);
        stage.show();

        // 题库目录先建好，并把内置题导出成可编辑的模板文件
        try {
            TaskBanks.ensureBuiltinExport();
            refreshBankBox(null);
            startPanel.refreshBanks(null);
        } catch (Exception e) {
            log("[warn] 题库目录初始化失败: " + e.getMessage());
        }

        if (!agent.ready()) {
            log("[fatal] Agent 初始化失败：无法连接 LLM");
            alert("Agent 初始化失败", "无法连接 LLM\n\n"
                    + (agent.error() == null ? "未知错误" : agent.error().getMessage()));
            startPanel.setStartDisabled(true, "Agent 未就绪");
            return;
        }

        log("就绪。请在开始页选好配置后点「开始」。");
        log("题库目录 = " + DatasetPaths.resolve(DatasetPaths.TASKS_DIR));
        log("数据集目录 = " + DatasetPaths.resolve(DatasetPaths.DATASET_ROOT));

        // --no-start-page：给批量/自动场景用，直接开跑
        if (skipStartPage) {
            startPanel.setVisible(false);
            startPanel.setManaged(false);
            onStartClicked(new StartPanel.Config(modelKey, bankName, storePath(),
                    true, keepSceneProp.get()));
        }
    }

    /* ==================== 开始页回调 ==================== */

    /**
     * 点「开始」：应用开始页里的配置，然后启动主循环。
     *
     * <p>只跑一次 —— 用 {@code runnerThread} 做闸门（重复点开始不会起两个循环）。
     */
    private void onStartClicked(StartPanel.Config cfg) {
        if (runnerThread != null && runnerThread.isAlive()) {
            log("[start] 已在运行中，忽略重复的开始请求");
            return;
        }
        if (!agentReady) {
            log("[start] Agent 未就绪，无法开始");
            return;
        }

        modelKey = cfg.modelKey() == null ? MODEL_DEEPSEEK : cfg.modelKey();
        datasetRoot = cfg.datasetRoot();
        writeToDatasetProp.set(cfg.writeToDataset());
        keepSceneProp.set(cfg.keepScene());
        store = new DatasetStore(datasetRoot);

        log("[start] 模型=" + modelKey + "  题库=" + cfg.bankName()
                + "  写入数据集=" + (cfg.writeToDataset() ? "是" : "否（试跑）")
                + "  复位=" + describeResetMode()
                + "  数据集=" + DatasetPaths.resolve(datasetRoot));
        if (cfg.keepScene()) {
            log("[warn] 「保留上一题场景」已开启 —— 题目之间**不复位**，各题起点将不再一致，"
                    + "差异可能来自上一题残留。");
        }

        // 切到选中的模型（可能和启动默认值不同）
        switchModel(modelKey, true);
        setBank(cfg.bankName(), true);

        Thread t = new Thread(this::runLoop, "dataset-runner");
        t.setDaemon(true);
        runnerThread = t;
        t.start();
    }

    private Path storePath() {
        return datasetRoot == null ? DatasetPaths.DATASET_ROOT : datasetRoot;
    }

    /** 当前复位模式的可读描述（写进日志，方便回溯这批数据是在什么起点下跑出来的）。 */
    private String describeResetMode() {
        if (keepSceneProp.get()) {
            return "不复位（保留上一题场景）";
        }
        if (baselineResetProp.get()) {
            return SceneBaseline.exists()
                    ? "初始场景（" + SceneBaseline.expectedModels().size() + " 个模型）"
                    : "空场景（还没有存过初始场景）";
        }
        return "空场景";
    }

    private HBox buildHeader() {
        progressLabel = new Label("准备中…");
        progressLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");
        Label hint = new Label("窗口观察 AI 结果 → 右上角打分 → 提交");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        VBox box = new VBox(2, progressLabel, hint);
        HBox.setHgrow(box, Priority.ALWAYS);

        // ---- 模型选择器：任务之间切换被测试模型 ----
        Label modelLabel = new Label("被测试模型");
        modelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        modelBox = new ComboBox<>();
        modelBox.getItems().addAll(MODEL_DEEPSEEK, MODEL_QWEN);
        modelBox.setValue(modelKey);
        modelBox.setPrefWidth(150);
        modelBox.valueProperty().addListener((obs, oldV, newV) -> switchModel(newV, false));
        VBox modelCol = new VBox(2, modelLabel, modelBox);
        modelCol.setAlignment(Pos.CENTER_LEFT);

        // ---- 题库选择器：任务之间切换题集 ----
        Label bankLabel = new Label("题库");
        bankLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        bankBox = new ComboBox<>();
        bankBox.setPrefWidth(220);
        bankBox.valueProperty().addListener((obs, oldV, newV) -> setBank(newV, false));
        VBox bankCol = new VBox(2, bankLabel, bankBox);
        bankCol.setAlignment(Pos.CENTER_LEFT);

        // ---- 出题按钮 ----
        Button genBtn = new Button("出题…");
        genBtn.setStyle("-fx-font-size: 12px;");
        genBtn.setOnAction(e -> showGeneratorDialog());
        VBox genCol = new VBox(2, new Label(" "), genBtn);
        genCol.setAlignment(Pos.CENTER_LEFT);

        // ---- 写入数据集开关（与开始页那个共用同一个属性） ----
        CheckBox writeBox = new CheckBox("写入数据集");
        writeBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #c7ccd1;");
        writeBox.selectedProperty().bindBidirectional(writeToDatasetProp);
        Label writeLabel = new Label("落盘");
        writeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        VBox writeCol = new VBox(2, writeLabel, writeBox);
        writeCol.setAlignment(Pos.CENTER_LEFT);

        // ---- 题目之间保留场景（默认关；开了下一题接着上一题） ----
        CheckBox keepBox = new CheckBox("保留上一题场景");
        keepBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #c7ccd1;");
        keepBox.selectedProperty().bindBidirectional(keepSceneProp);
        keepBox.setTooltip(new javafx.scene.control.Tooltip(
                "关闭（默认）：每题执行前复位到初始场景（或空场景），保证各题起点一致。\n"
                        + "开启：只有第一题复位，之后接着上一题继续 —— 可以引用上一题建的模型，\n"
                        + "但各题起点不再一致。"));
        Label keepLabel = new Label("场景");
        keepLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        VBox keepCol = new VBox(2, keepLabel, keepBox);
        keepCol.setAlignment(Pos.CENTER_LEFT);

        // ---- 初始场景：一键把当前场景存下来，之后复位就回到它 ----
        Button baselineBtn = new Button("存为初始场景");
        baselineBtn.setStyle("-fx-font-size: 12px;");
        baselineBtn.setTooltip(new javafx.scene.control.Tooltip(
                "把**当前场景**存成「初始场景」。\n"
                        + "存过之后，每题复位就回到这份场景（而不是清空）——\n"
                        + "适合先搭好一个复杂场景，再在上面连着测很多操作。\n"
                        + "位置：" + DatasetPaths.resolve(SceneBaseline.dir())));
        baselineBtn.setOnAction(e -> saveBaseline());
        CheckBox baselineBox = new CheckBox("复位到初始场景");
        baselineBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #c7ccd1;");
        baselineBox.selectedProperty().bindBidirectional(baselineResetProp);
        baselineBox.setTooltip(new javafx.scene.control.Tooltip(
                "开启：每题复位回到「已保存的初始场景」。\n"
                        + "关闭：每题复位成**空场景**（什么都不塞）。"));
        Label baselineLabel = new Label("初始场景");
        baselineLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");
        HBox baselineRow = new HBox(8, baselineBox, baselineBtn);
        baselineRow.setAlignment(Pos.CENTER_LEFT);
        VBox baselineCol = new VBox(2, baselineLabel, baselineRow);
        baselineCol.setAlignment(Pos.CENTER_LEFT);

        HBox bar = new HBox(14, box, modelCol, bankCol, writeCol, keepCol, baselineCol, genCol);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: rgba(38,42,47,0.95);");
        return bar;
    }

    /* ==================== 模型切换 ==================== */

    /** 刷新题库下拉框（保留当前选择，除非被删了）。 */
    private void refreshBankBox(String preferred) {
        if (bankBox == null) return;
        String want = preferred != null ? preferred : bankBox.getValue();
        List<String> banks = TaskBanks.list();
        bankBox.getItems().setAll(banks);
        bankBox.setValue(want != null && banks.contains(want) ? want : TaskBanks.BUILTIN);
        bankName = bankBox.getValue();
    }

    /**
     * 切换题库。**下一题生效**：主循环每道题开始前都重新读一次 {@link #bankName}，
     * 所以这里不用中断正在跑的题。
     *
     * @param quiet 开始页初始化时传 true，不打印"已切换"
     */
    private void setBank(String name, boolean quiet) {
        if (name == null || name.isBlank()) return;
        bankName = name;
        if (bankBox != null && !name.equals(bankBox.getValue())) {
            bankBox.setValue(name);
        }
        int n = TaskBanks.countTasks(name);
        if (!quiet) log("[bank] 已切换到题库「" + name + "」（" + n + " 题，下一题生效）");
        if (progressLabel != null) {
            setProgress("题库=" + name + "（" + n + " 题） ｜ " + modelKey);
        }
    }

    /**
     * 切换被测试模型。在**任务之间**调用才安全：{@code AgentInterfaceImpl} 每轮开头会快照
     * 一次 client，所以正在跑的轮次仍用旧模型，不会半路换签证。
     *
     * @param force 开始页点开始时传 true（即便 key 相同也重建一次，确保 client 就绪）
     */
    private void switchModel(String key, boolean force) {
        if (key == null) return;
        key = key.strip().toLowerCase();
        // 受控词表：只允许这两个，避免把 API 模型名当分组标识写进记录
        if (!MODEL_DEEPSEEK.equals(key) && !MODEL_QWEN.equals(key)) {
            log("[model] 忽略未知模型标识: " + key + "（只认 "
                    + MODEL_DEEPSEEK + " / " + MODEL_QWEN + "）");
            return;
        }
        if (!force && key.equals(modelKey) && activeClient != null) return;   // 同值且已就绪 → 无需重建
        try {
            LLMClient next = createClient(key);
            activeClient = next;
            modelKey = key;
            AgentInterface.getInstance().setClient(next);
            log("[model] 已切换到 " + next.getModel() + " @ " + next.getBaseURL()
                    + "（分组标识 " + key + "）");
            if (progressLabel != null) {
                setProgress("模型=" + key + " (" + next.getModel() + ") ｜ 就绪");
            }
            if (modelBox != null && !key.equals(modelBox.getValue())) {
                modelBox.setValue(key);
            }
        } catch (Exception e) {
            log("[model] 切换失败: " + e.getMessage());
            alert("模型切换失败", "无法创建 " + key + " 客户端：\n" + e.getMessage());
        }
    }

    /** 按 key 造客户端。 */
    private static LLMClient createClient(String key) throws Exception {
        if (MODEL_QWEN.equals(key)) {
            return LLMClient.local();
        }
        return LLMClient.deepSeek();
    }

    /* ==================== 初始场景 ==================== */

    private void saveBaseline() {
        Thread t = new Thread(() -> {
            try {
                SceneBaseline.save(region, renderer);
                List<String> models = SceneBaseline.expectedModels();
                log("[baseline] 已保存初始场景 → " + DatasetPaths.resolve(SceneBaseline.dir()));
                log("[baseline] " + SceneBaseline.describe());
                if (models.isEmpty()) {
                    log("[baseline] ⚠ 当前场景里没有可保存的模型（空场景，或只有不含四面体的空容器）"
                            + "—— 复位到它的效果与空场景相同");
                }
                Platform.runLater(() -> baselineResetProp.set(true));
            } catch (Exception e) {
                log("[baseline] 保存失败: " + e);
                e.printStackTrace();
            }
        }, "baseline-save");
        t.setDaemon(true);
        t.start();
    }

    /* ==================== 出题（LLM 自动出题器） ==================== */

    /** 出题对话框：选数量 / 难度 / 类别，生成后**存成一份新题库**。 */
    private void showGeneratorDialog() {
        if (activeClient == null) {
            alert("无法出题", "被测试模型未就绪，请先选好模型。");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("LLM 自动出题");
        dialog.setHeaderText("用当前模型（" + describeActiveModel() + "）基于真实操作清单出题，"
                + "结果存为新的题库");

        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.getItems().addAll("mixed", "simple", "medium", "complex", "trap");
        diffBox.setValue("mixed");

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("mixed", "camera", "camera_bookmark", "model_tree",
                "transform", "shape", "carve", "composite");
        catBox.setValue("mixed");

        Spinner<Integer> countSpin = new Spinner<>(1, 200, 20);
        countSpin.setEditable(true);

        CheckBox selfFilter = new CheckBox("可完成性自检（多花一轮调用，过滤离谱题）");
        selfFilter.setSelected(true);

        TextField nameField = new TextField("generated");
        nameField.setPrefColumnCount(24);
        Label nameHint = new Label("将保存为 " + DatasetPaths.TASKS_DIR + "/<名字>-<时间戳>.jsonl");
        nameHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("数量"), countSpin);
        grid.addRow(1, new Label("难度"), diffBox);
        grid.addRow(2, new Label("类别"), catBox);
        grid.addRow(3, new Label("题库名"), nameField);
        grid.add(nameHint, 1, 4);
        grid.add(selfFilter, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("开始生成", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);

        var result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != dialog.getDialogPane().getButtonTypes().get(0)) {
            return;
        }

        TaskGenerator.Options opt = new TaskGenerator.Options();
        opt.count = countSpin.getValue();
        opt.difficulty = diffBox.getValue();
        opt.category = catBox.getValue();
        opt.selfFilter = selfFilter.isSelected();

        Path out = TaskBanks.newBankFile(nameField.getText());
        Thread t = new Thread(() -> {
            try {
                log("[gen] 开始出题：count=" + opt.count + " difficulty=" + opt.difficulty
                        + " category=" + opt.category);
                TaskGenerator gen = new TaskGenerator(activeClient, opt);
                List<TaskGoal> tasks = gen.generate(startIdFor(out));
                String saved = TaskBanks.saveAsBank(tasks, out);
                log("[gen] 完成 " + tasks.size() + " 条 → " + DatasetPaths.resolve(out));
                // 新题库立刻出现在两个下拉框里
                Platform.runLater(() -> {
                    refreshBankBox(saved);
                    startPanel.selectBank(saved);
                    alert2("出题完成",
                            "已保存 " + tasks.size() + " 题的题库：\n" + saved
                                    + "\n\n已在题库下拉框里选中，直接点开始（或在跑的过程中切换）即可测试。");
                });
            } catch (Exception e) {
                log("[gen] 出题失败: " + e);
                e.printStackTrace();
                Platform.runLater(() -> alert("出题失败", String.valueOf(e)));
            }
        }, "task-generator");
        t.setDaemon(true);
        t.start();
    }

    /** 纯出题模式（{@code --generate}）：不建窗口、不跑循环，生成完退出。 */
    private void runGenerateOnly() {
        try {
            TaskGenerator.Options opt = new TaskGenerator.Options();
            opt.count = genCount;
            opt.difficulty = genDifficulty;
            opt.category = genCategory;
            opt.selfFilter = true;

            // 默认写进题库目录；显式给了 --tasks= 就按那个路径写
            Path out = explicitTaskFile != null ? explicitTaskFile
                    : TaskBanks.newBankFile("generated");

            System.out.println("[gen] 出题中 count=" + opt.count
                    + " difficulty=" + opt.difficulty + " category=" + opt.category);
            if (explicitTaskFile != null && Files.exists(out)) {
                // 显式指定了已存在的文件：这是用户自己的选择，但要提醒一句会覆盖
                System.out.println("[gen] 注意：--tasks= 指向已存在的文件，将被覆盖 → "
                        + DatasetPaths.resolve(out));
            }
            TaskGenerator gen = new TaskGenerator(activeClient, opt);
            List<TaskGoal> tasks = gen.generate(startIdFor(out));
            TaskLoader.save(tasks, out);
            System.out.println("[gen] 完成 " + tasks.size() + " 条 → " + DatasetPaths.resolve(out));
        } catch (Exception e) {
            System.err.println("[gen] 失败: " + e);
            e.printStackTrace();
        }
        Platform.exit();
        System.exit(0);
    }

    /** {@code --generate} 模式下显式指定的输出文件（{@code --tasks=}）。 */
    private static Path explicitTaskFile;

    private int startIdFor(Path out) {
        if (out == null || !Files.exists(out)) {
            return 1;
        }
        int max = 0;
        try {
            for (TaskGoal g : TaskLoader.load(out)) {
                max = Math.max(max, g.id());
            }
        } catch (Exception ignored) {
        }
        return max + 1;
    }

    /** 当前被测试模型的展示文本。 */
    private String describeActiveModel() {
        if (activeClient == null) return "未初始化";
        return activeClient.getModel() + " @ " + activeClient.getBaseURL();
    }

    private VBox buildLogPane() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        logArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;"
                + "-fx-control-inner-background: rgba(26,28,32,0.98); -fx-text-fill: #9aa0a6;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox box = new VBox(logArea);
        box.setPrefHeight(140);
        return box;
    }

    /* ==================== 主循环（后台线程） ==================== */

    private void runLoop() {
        List<TaskGoal> tasks;
        try {
            tasks = TaskBanks.load(bankName);
        } catch (Exception e) {
            log("[fatal] 任务加载失败: " + e.getMessage());
            return;
        }
        if (tasks.isEmpty()) {
            log("[fatal] 题库「" + bankName + "」里没有任务");
            return;
        }

        String model = currentModelName();
        String apiModel = currentApiModelName();
        String promptHash = hashSystemPrompt();
        log("任务数=" + tasks.size() + "  题库=" + bankName + "  模型=" + model + " (" + apiModel + ")"
                + "  数据集根=" + DatasetPaths.resolve(store.root()));

        AgentInterface agent = AgentInterface.getInstance();
        AgentTaskRunner runner = new AgentTaskRunner(agent, () -> snapshotter.snapshot());

        // 当前 tasks 列表对应的题库名（与 bankName 不同 → 说明用户在跑的过程中换了题库）
        String loadedBank = bankName;
        // 本次运行的记录（试跑模式下没有落盘，汇总只能靠它）
        List<TaskRunRecord> runRecords = new java.util.ArrayList<>();

        int index = 0;
        for (TaskGoal task : tasks) {
            if (scorePanel.isAbortRequested()) {
                log("[abort] 用户终止，停止循环");
                break;
            }

            // ---- 每题开始前重新读题库/模型：页面内切换在下一题生效 ----
            if (!bankName.equals(loadedBank)) {
                try {
                    List<TaskGoal> reloaded = TaskBanks.load(bankName);
                    if (!reloaded.isEmpty()) {
                        tasks = reloaded;
                        loadedBank = bankName;
                        index = 0;   // 新题库重新计数，进度条才有意义
                        log("[bank] 已切换到题库「" + bankName + "」（" + tasks.size() + " 题）");
                    } else {
                        log("[bank] 题库「" + bankName + "」为空，忽略切换");
                        bankName = loadedBank;
                    }
                } catch (Exception e) {
                    log("[bank] 题库「" + bankName + "」加载失败，继续用旧的: " + e.getMessage());
                    bankName = loadedBank;
                }
            }

            index++;
            final int i = index;
            setProgress(String.format("任务 %d/%d ｜ %s ｜ %s ｜ 正在执行…",
                    i, tasks.size(), loadedBank, task.category()));

            // 模型可能在任务之间被切换 —— 每条记录都重新取一次，保证「记录里写的模型」=「实际跑的模型」
            String taskModel = currentModelName();
            String taskApiModel = currentApiModelName();
            TaskRunRecord record = new TaskRunRecord(task, taskModel, promptHash,
                    OffsetDateTime.now() + "__" + taskModel + "__" + task.category() + "__" + task.id());
            record.setApiModel(taskApiModel);

            try {
                if (!keepScene()) {
                    snapshotter.reset();
                }
                // 快照一次即可：它要跨 FX 线程，别白跑两趟
                SceneSnapshot before = new SceneSnapshot(snapshotter.snapshot());
                if (keepScene()) {
                    log("[keep] 本题不复位，接着上一题继续（现有模型：" + before.modelPaths() + "）");
                }
                record.setSnapshotBefore(before);

                // 2. 执行
                agent.setTraceListener(null); // 由 runner 内部接管
                AgentTaskRunner.RunOutcome outcome = runner.run(task, record);

                // 3. 等相机动画落定（机位跳转约 1 秒），再采末态
                Thread.sleep(1500);
                record.setSnapshotAfter(new SceneSnapshot(snapshotter.snapshot()));

                // 4. 自动判据
                CheckEvaluator.Report autoReport = evaluateAuto(task, record);
                record.setAutoCheck(
                        autoReport == null ? null : autoReport.allPassed(),
                        autoReport == null ? null : autoReport.describe());

                // 5. 打分
                if (autoScoreOnly) {
                    if (autoReport == null) {
                        record.setVerdict(TaskRunRecord.Verdict.SKIP, "自动模式下无判据", "auto");
                    } else {
                        record.setVerdict(autoReport.allPassed()
                                        ? TaskRunRecord.Verdict.CORRECT
                                        : TaskRunRecord.Verdict.INCORRECT,
                                autoReport.describe(), "auto");
                    }
                } else {
                    if (outcome.timedOut()) {
                        record.setVerdict(TaskRunRecord.Verdict.TIMEOUT, "任务超时", "system");
                    } else if (record.isAgentError()) {
                        record.setVerdict(TaskRunRecord.Verdict.ERROR,
                                "Agent 错误：" + record.getAgentErrorMessage(), "system");
                    } else {
                        ScorePanel.Score score = scorePanel.awaitScore(task, record, autoReport);
                        record.setVerdict(score.verdict(), score.comment(), "human");
                    }
                }

                // 6. 落盘（受「写入数据集」开关控制）
                recordsByTask.put(task.id(), record);
                runRecords.add(record);
                if (writeToDataset()) {
                    store.append(record);
                    log(String.format("[save] task=%d verdict=%s rounds=%d %.1fs",
                            task.id(), record.getVerdict(), record.roundCount(),
                            record.getElapsedMs() / 1000.0));
                } else {
                    log(String.format("[dry-run] task=%d verdict=%s rounds=%d %.1fs（开关关闭，未落盘）",
                            task.id(), record.getVerdict(), record.roundCount(),
                            record.getElapsedMs() / 1000.0));
                }

            } catch (Throwable t) {
                log("[error] task=" + task.id() + " -> " + t);
                t.printStackTrace();
                try {
                    record.setVerdict(TaskRunRecord.Verdict.ERROR, String.valueOf(t), "system");
                    // 第 6 步可能已经 add 过了（比如 append 抛 IOException 才走到这里），
                    // 别重复计入，否则试跑汇总的样本数会虚高
                    if (!runRecords.contains(record)) {
                        runRecords.add(record);
                    }
                    if (writeToDataset()) {
                        store.append(record);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 7. 汇总
        if (writeToDataset()) {
            try {
                List<JSONObject> all = store.loadAllRaw();
                MetricsAggregator.Report report = MetricsAggregator.aggregate(all);
                String md = MetricsAggregator.toMarkdown(report);
                String day = java.time.LocalDate.now().toString();
                store.writeSummary("compare__" + day + ".md", md);
                store.writeSummary(day + "__" + model + ".json",
                        MetricsAggregator.toJson(report).toJSONString(
                                com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat));
                log("=== 汇总 ===\n" + MetricsAggregator.toConsole(report));
                log("[done] 汇总已写入 " + DatasetPaths.resolve(store.summaryDir()));
            } catch (Exception e) {
                log("[error] 汇总失败: " + e.getMessage());
            }
        } else {
            // 试跑模式：磁盘上没有新数据，汇总只能拿本次运行的内存记录来算，且不写文件
            try {
                List<JSONObject> all = new java.util.ArrayList<>();
                for (TaskRunRecord r : runRecords) {
                    all.add(r.toJson());
                }
                MetricsAggregator.Report report = MetricsAggregator.aggregate(all);
                log("=== 本次试跑汇总（未落盘） ===\n" + MetricsAggregator.toConsole(report));
            } catch (Exception e) {
                log("[error] 试跑汇总失败: " + e.getMessage());
            }
        }

        setProgress("全部完成（题库 " + loadedBank + "）");
        if (writeToDataset()) {
            log("[done] 数据集写入 " + DatasetPaths.resolve(store.root()));
        } else {
            log("[done] 试跑结束 —— 「写入数据集」关闭，本次没有改动 " + DatasetPaths.resolve(store.root()));
        }
        Platform.runLater(this::showSummaryDialog);
    }

    /** 「写入数据集」开关的当前值。 */
    private boolean writeToDataset() {
        return writeToDatasetProp.get();
    }

    /** 「题目之间保留场景」开关的当前值。 */
    private boolean keepScene() {
        return keepSceneProp.get();
    }

    private CheckEvaluator.Report evaluateAuto(TaskGoal task, TaskRunRecord record) {
        if (!task.hasCheck()) return null;
        var snap = record.getSnapshotAfter();
        if (snap == null) return null;
        CheckEvaluator.Context ctx = new CheckEvaluator.Context(
                snap, record.finalAnswer(), record.allReceipts());
        return CheckEvaluator.evaluate(task.assertions(), ctx);
    }

    private void showTraceDialog(int taskId) {
        TaskRunRecord r = recordsByTask.get(taskId);
        if (r == null) return;
        TextArea ta = new TextArea(r.toJson().toJSONString(
                com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat));
        ta.setEditable(false);
        ta.setPrefRowCount(30);
        ta.setPrefColumnCount(100);
        ta.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        ScrollPane sp = new ScrollPane(ta);
        sp.setFitToWidth(true);
        sp.setPrefSize(980, 640);

        Stage dialog = new Stage();
        dialog.setTitle("任务 " + taskId + " 完整轨迹");
        dialog.setScene(new Scene(new StackPane(sp), 1000, 700));
        dialog.show();
    }

    private void showSummaryDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (writeToDataset()) {
            alert.setTitle("数据集生成完成");
            alert.setHeaderText("汇总已写入 " + DatasetPaths.resolve(store.summaryDir()));
            alert.setContentText("查看 compare__<date>.md 获取模型量化评估总表。");
        } else {
            alert.setTitle("试跑结束");
            alert.setHeaderText("「写入数据集」处于关闭状态，本次没有落盘");
            alert.setContentText("结果只在上方日志里（含本次汇总）。"
                    + "想真正写入，请打开「写入数据集」开关后重跑。");
        }
        alert.show();
    }

    private void setProgress(String text) {
        Platform.runLater(() -> progressLabel.setText(text));
    }

    private void log(String text) {
        System.out.println("[dataset] " + text);
        Platform.runLater(() -> {
            if (logArea != null) logArea.appendText(text + System.lineSeparator());
        });
    }

    private void alert(String title, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(title);
            a.setContentText(content);
            a.show();
        });
    }

    /** 非错误的提示弹窗（出题完成等）。 */
    private void alert2(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(content);
        a.show();
    }

    private String currentModelName() {
        return modelKey;
    }

    /** 具体 API 模型名（写进 {@code env.api_model}，仅用于追溯，不参与分组）。 */
    private String currentApiModelName() {
        LLMClient c = activeClient;
        if (c != null) return c.getModel();
        return "unknown";
    }

    private static String hashSystemPrompt() {
        try {
            String p = PromptLoader.loadSystemPrompt();
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(p.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("sha1:");
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable t) {
            return "sha1:unknown";
        }
    }

    public static void main(String[] args) {
        for (String a : args) {
            if (a.startsWith("--tasks=")) {
                // 直接给文件路径：当作题库文件用
                explicitTaskFile = Path.of(a.substring("--tasks=".length()));
                bankName = explicitTaskFile.getFileName().toString();
            } else if (a.startsWith("--bank=")) {
                bankName = a.substring("--bank=".length()).strip();
            } else if (a.startsWith("--data=")) {
                datasetRoot = Path.of(a.substring("--data=".length()));
            } else if (a.equals("--auto")) autoScoreOnly = true;
            else if (a.equals("--no-start-page")) skipStartPage = true;
            else if (a.equals("--keep-scene")) keepSceneCli = true;
            else if (a.equals("--baseline")) baselineCli = true;
            else if (a.startsWith("--model=")) {
                modelKey = a.substring("--model=".length()).strip().toLowerCase();
            } else if (a.equals("--generate")) generateMode = true;
            else if (a.startsWith("--count=")) genCount = Integer.parseInt(a.substring("--count=".length()).strip());
            else if (a.startsWith("--difficulty=")) genDifficulty = a.substring("--difficulty=".length()).strip();
            else if (a.startsWith("--category=")) genCategory = a.substring("--category=".length()).strip();
        }
        // 传具体类而不是 args：避免 JavaFX 再去推断主类
        launch(DatasetApp.class, args);
    }
}
