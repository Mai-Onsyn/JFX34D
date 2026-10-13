package weilantianhai.dataset.ui;

import javafx.application.Application;

/**
 * 数据集脚本的**启动器**（绕开 JavaFX 运行时模块检查）。
 *
 * <h3>为什么需要这个类</h3>
 *
 * <p>当 {@code main} 所在的主类**本身继承 {@code javafx.application.Application}** 时，
 * JVM 会在启动阶段做一次"JavaFX 运行时组件是否存在"的检查；在**类路径（classpath）模式**
 * 下（IDE 默认的 Run 配置就是这种）会直接报：
 *
 * <pre>
 * 错误: 缺少 JavaFX 运行时组件, 需要使用该组件来运行此应用程序
 * </pre>
 *
 * <p>把 {@code main} 挪到一个**不继承 Application** 的普通类里，这个检查就不触发，
 * JavaFX 会自己从类路径加载所需模块。这是官方在 classpath 模式下的标准做法。
 *
 * <p>所以：**Run 这个类，不要 Run {@link DatasetApp}。**
 */
public final class DatasetLauncher {

    private DatasetLauncher() {
    }

    public static void main(String[] args) {
        // 把命令行参数原样交给 DatasetApp.main（它会解析 --tasks/--data/--auto 后再 launch）
        DatasetApp.main(args);
    }
}
