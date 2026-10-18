package weilantianhai.dataset.ui;

import javafx.application.Application;

public final class DatasetLauncher {
    private DatasetLauncher() {
    }

    public static void main(String[] args) {
        // 把命令行参数原样交给 DatasetApp.main（它会解析 --tasks/--data/--auto 后再 launch）
        DatasetApp.main(args);
    }
}
