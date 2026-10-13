package weilantianhai.dataset.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import weilantianhai.dataset.DatasetPaths;
import weilantianhai.dataset.tasks.TaskBanks;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * 开始页：程序起来后**先停在这里**，让用户选好模型与题库，点「开始」才真正跑题。
 *
 * <h3>为什么要先停一下</h3>
 *
 * <p>原版一进窗口就开始跑循环，用户根本来不及做题前准备（换模型、换题库、改数据集目录），
 * 而且模型选错了会白跑一轮。更实际的问题是：**跑之前选模型和题库，本来就是"实验配置"的一部分**，
 * 它应该发生在实验开始前，而不是实验中途。
 *
 * <p>点开始后本面板会 {@code setVisible(false)}，控制权交回 {@link DatasetApp}，
 * 由它启动主循环。
 */
public final class StartPanel extends StackPane {

    private static final String PANEL =
            "-fx-background-color: rgba(20,22,26,0.97);"
                    + "-fx-background-radius: 14;"
                    + "-fx-border-color: rgba(140,146,154,0.5);"
                    + "-fx-border-radius: 14;"
                    + "-fx-border-width: 1;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 26, 0.25, 0, 10);";

    private static final String LABEL_STYLE =
            "-fx-font-size: 11px; -fx-text-fill: #9aa0a6;";

    private static final String BTN_START =
            "-fx-background-color: rgba(255,138,26,0.95); -fx-text-fill: #ffffff;"
                    + "-fx-font-size: 14px; -fx-font-weight: bold;"
                    + "-fx-padding: 10 46; -fx-background-radius: 8; -fx-cursor: hand;";

    private final ComboBox<String> modelBox = new ComboBox<>();
    private final ComboBox<String> bankBox = new ComboBox<>();
    private final TextField dataField = new TextField();
    private final Label bankInfo = new Label();
    private final Label dataInfo = new Label();
    private final Button startBtn = new Button("开 始");

    /** 点开始时的回调：(modelKey, bankName, datasetRoot)。 */
    private final Consumer<Config> onStart;

    /** 开始页的配置快照。 */
    public record Config(String modelKey, String bankName, Path datasetRoot) {
    }

    public StartPanel(String initialModel, Consumer<Config> onStart) {
        this.onStart = onStart;
        setStyle("-fx-background-color: rgba(15,17,20,0.72);");
        setAlignment(Pos.CENTER);

        Label title = new Label("JFX34D 数据集生成器");
        title.setStyle("-fx-font-size: 21px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");
        Label sub = new Label("先选好被测试模型与题库，再开始。跑的过程中也能换。");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #8a8f94;");

        // ---- 被测试模型 ----
        modelBox.getItems().addAll(DatasetApp.MODEL_DEEPSEEK, DatasetApp.MODEL_QWEN);
        modelBox.setValue(initialModel);
        modelBox.setPrefWidth(300);
        modelBox.setStyle("-fx-font-size: 12px;");

        // ---- 题库 ----
        // 先装监听再填值：refreshBanks 会 setValue，若顺序反了第一次赋值不会触发提示更新
        bankBox.setPrefWidth(300);
        bankBox.setStyle("-fx-font-size: 12px;");
        bankBox.valueProperty().addListener((o, a, b) -> updateBankInfo(b));
        refreshBanks(null);

        // ---- 数据集目录 ----
        dataField.setText(DatasetPaths.DATASET_ROOT.toString());
        dataField.setPrefWidth(300);
        dataField.setStyle("-fx-font-size: 12px; -fx-font-family: 'Consolas', monospace;");
        dataField.textProperty().addListener((o, a, b) -> updateDataInfo(b));
        updateDataInfo(dataField.getText());

        startBtn.setStyle(BTN_START);
        startBtn.setDefaultButton(true);
        startBtn.setOnAction(e -> fireStart());

        VBox card = new VBox(14,
                title, sub,
                spacer(6),
                field("被测试模型", modelBox, null),
                field("题库", bankBox, bankInfo),
                field("数据集目录", dataField, dataInfo),
                spacer(4),
                startBtn);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(30, 38, 30, 38));
        card.setMaxWidth(Region.USE_PREF_SIZE);

        StackPane cardWrap = new StackPane(card);
        cardWrap.setStyle(PANEL);
        cardWrap.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        getChildren().add(cardWrap);
        setAlignment(cardWrap, Pos.CENTER);
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static VBox field(String label, javafx.scene.Node control, Label info) {
        Label l = new Label(label);
        l.setStyle(LABEL_STYLE);
        VBox v = new VBox(4, l, control);
        if (info != null) {
            info.setStyle("-fx-font-size: 11px; -fx-text-fill: #8ab4f8;");
            info.setWrapText(true);
            info.setMaxWidth(300);
            v.getChildren().add(info);
        }
        return v;
    }

    /**
     * 重新扫描题库目录并刷新下拉框（出题完成后调用，新题库立刻可选）。
     *
     * @param preferred 刷新后想选中的题库名；null 表示保留当前选择（没了就退回内置）
     */
    public void refreshBanks(String preferred) {
        String want = preferred != null ? preferred : bankBox.getValue();
        List<String> banks = TaskBanks.list();
        bankBox.getItems().setAll(banks);
        if (want != null && banks.contains(want)) {
            bankBox.setValue(want);
        } else {
            bankBox.setValue(TaskBanks.BUILTIN);
        }
        updateBankInfo(bankBox.getValue());
    }

    /** 供外部（出题完成后）设置题库选择。 */
    public void selectBank(String name) {
        refreshBanks(name);
    }

    private void updateBankInfo(String bank) {
        if (bank == null) {
            bankInfo.setText("");
            return;
        }
        int n = TaskBanks.countTasks(bank);
        Path p = TaskBanks.pathOf(bank);
        bankInfo.setText(n + " 题" + (p == null ? "（写死在代码里）" : " · " + p));
    }

    private void updateDataInfo(String text) {
        if (text == null || text.isBlank()) {
            dataInfo.setText("不能为空");
            return;
        }
        Path p = Path.of(text.strip());
        boolean abs = p.isAbsolute();
        dataInfo.setText((abs ? "绝对路径" : "相对项目根目录")
                + " → " + DatasetPaths.resolve(DatasetPaths.anchorToProjectRoot(p)));
    }

    private void fireStart() {
        String model = modelBox.getValue();
        String bank = bankBox.getValue();
        String data = dataField.getText() == null ? "" : dataField.getText().strip();
        if (data.isEmpty()) {
            dataInfo.setText("数据集目录不能为空");
            return;
        }
        // 相对路径锚到项目根（IDE 的工作目录不一定是项目根）
        Path root = DatasetPaths.anchorToProjectRoot(Path.of(data));
        setVisible(false);
        setManaged(false);
        Platform.runLater(() -> onStart.accept(new Config(model, bank, root)));
    }

    /** 当前选中的题库（供 DatasetApp 读取）。 */
    public String selectedBank() {
        return bankBox.getValue();
    }

    /** 当前选中的模型标识。 */
    public String selectedModel() {
        return modelBox.getValue();
    }

    /** 数据集目录控件（供 DatasetApp 在换题库时同步显示）。 */
    public Path selectedDatasetRoot() {
        String data = dataField.getText() == null ? "" : dataField.getText().strip();
        return DatasetPaths.anchorToProjectRoot(Path.of(data));
    }

    /**
     * 禁用「开始」按钮（例如 Agent 初始化失败时）。
     *
     * <p>这里直接引用字段而不是 {@code lookup()} —— 本方法可能在面板还没挂进 Scene
     * 之前就被调用，那时 {@code lookup} 会返回 null。
     */
    public void setStartDisabled(boolean disabled, String reason) {
        startBtn.setDisable(disabled);
        if (disabled && reason != null) {
            startBtn.setText("无法开始（" + reason + "）");
        } else {
            startBtn.setText("开 始");
        }
    }
}
