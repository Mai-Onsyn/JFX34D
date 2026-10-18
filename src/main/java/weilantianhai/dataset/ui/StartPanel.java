package weilantianhai.dataset.ui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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

    private static final String BTN_GEN =
            "-fx-background-color: rgba(90,95,102,0.9); -fx-text-fill: #e8eaed;"
                    + "-fx-font-size: 12px; -fx-padding: 10 18; -fx-background-radius: 8;"
                    + "-fx-cursor: hand;";

    private final ComboBox<String> modelBox = new ComboBox<>();
    private final ComboBox<String> bankBox = new ComboBox<>();
    private final TextField dataField = new TextField();
    private final CheckBox writeToggle = new CheckBox("写入数据集");
    private final CheckBox keepToggle = new CheckBox("题目之间保留场景");
    private final Label bankInfo = new Label();
    private final Label dataInfo = new Label();
    private final Button startBtn = new Button("开 始");

    /** 点开始时的回调。 */
    private final Consumer<Config> onStart;

    /** 开始页的配置快照。 */
    public record Config(String modelKey, String bankName, Path datasetRoot,
                         boolean writeToDataset, boolean keepScene) {
    }

    /**
     * @param initialModel     初始被测试模型
     * @param writeToDataset   「写入数据集」开关（与顶栏那个共用同一个属性，双向同步）
     * @param keepScene        「题目之间保留场景」开关（同上）
     * @param onStart          点「开始」时回调
     * @param onGenerate       点「出题…」时回调（复用顶栏那套出题流程）
     */
    public StartPanel(String initialModel,
                      BooleanProperty writeToDataset,
                      BooleanProperty keepScene,
                      Consumer<Config> onStart,
                      Runnable onGenerate) {
        this.onStart = onStart;
        setStyle("-fx-background-color: rgba(15,17,20,0.72);");
        setAlignment(Pos.CENTER);

        Label title = new Label("JFX34D 数据集生成器");
        title.setStyle("-fx-font-size: 21px; -fx-font-weight: bold; -fx-text-fill: #e8eaed;");
        Label sub = new Label("先生成/选好题库，再开始。跑的过程中也能换题库与模型。");
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

        // ---- 写入数据集开关 ----
        writeToggle.setSelected(writeToDataset.get());
        // 双向：从开关到属性（用户点它），也从属性到开关（顶栏那个被点了）
        writeToggle.selectedProperty().bindBidirectional(writeToDataset);
        writeToggle.setStyle("-fx-font-size: 12px; -fx-text-fill: #c7ccd1;");
        Label writeHint = new Label("关闭 = 只跑不落盘（试跑，不污染已有数据集）");
        writeHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a8f94;");

        // ---- 题目之间保留场景开关 ----
        keepToggle.setSelected(keepScene.get());
        keepToggle.selectedProperty().bindBidirectional(keepScene);
        keepToggle.setStyle("-fx-font-size: 12px; -fx-text-fill: #c7ccd1;");
        Label keepHint = new Label("开启 = 题目之间完全不复位，下一题接着上一题继续。"
                + "关掉则每题复位（空场景，或顶栏设的「初始场景」）");
        keepHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #e0a86c;");
        keepHint.setWrapText(true);
        keepHint.setMaxWidth(300);

        // ---- 按钮 ----
        startBtn.setStyle(BTN_START);
        startBtn.setDefaultButton(true);
        startBtn.setOnAction(e -> fireStart());

        Button genBtn = new Button("出题…");
        genBtn.setStyle(BTN_GEN);
        genBtn.setOnAction(e -> {
            if (onGenerate != null) {
                onGenerate.run();
            }
        });

        HBox buttons = new HBox(12, startBtn, genBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14,
                title, sub,
                spacer(6),
                field("被测试模型", modelBox, null),
                field("题库", bankBox, bankInfo),
                field("数据集目录", dataField, dataInfo),
                new VBox(3, writeToggle, writeHint),
                new VBox(3, keepToggle, keepHint),
                spacer(4),
                buttons);
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
        Platform.runLater(() -> onStart.accept(
                new Config(model, bank, root, writeToggle.isSelected(), keepToggle.isSelected())));
    }

    /** 当前选中的题库。 */
    public String selectedBank() {
        return bankBox.getValue();
    }

    /** 当前选中的模型标识。 */
    public String selectedModel() {
        return modelBox.getValue();
    }

    public void setStartDisabled(boolean disabled, String reason) {
        startBtn.setDisable(disabled);
        if (disabled && reason != null) {
            startBtn.setText("无法开始（" + reason + "）");
        } else {
            startBtn.setText("开 始");
        }
    }
}
