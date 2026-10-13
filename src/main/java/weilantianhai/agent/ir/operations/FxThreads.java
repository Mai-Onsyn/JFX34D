package weilantianhai.agent.ir.operations;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 把对 UI 机位悬浮窗的调用**切回 JavaFX 线程**并等待完成。
 *
 * <h3>为什么需要它</h3>
 *
 * <p>agent 的 {@code IOperation.execute} 跑在 {@code agent-worker} 线程上，
 * 而 {@link HCloudbyte.ui.viewport.CameraBookmarksView} 是 JavaFX 控件：
 * 它内部的 {@code records} 是非线程安全集合，且控件操作必须在 FX 线程做。
 * 从 worker 线程直接调用会有**竞态与 IllegalStateException** 风险。
 *
 * <h3>行为</h3>
 *
 * <ul>
 *   <li>已在 FX 线程 → 直接执行，不做线程切换；</li>
 *   <li>否则 → {@code Platform.runLater} 投递，并用 {@link CountDownLatch} 等它跑完，
 *       保证"AI 改完，UI 立刻一致"；</li>
 *   <li>被调代码抛的异常原样在调用线程重新抛出（含 {@code RuntimeException}）。</li>
 * </ul>
 */
public final class FxThreads {

    private FxThreads() {}

    /** 在 FX 线程上执行 {@code action} 并等待结果。 */
    public static <T> T call(FxCallable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<T> result = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    result.set(action.call());
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
            Throwable t = error.get();
            if (t != null) {
                if (t instanceof Exception e) throw e;
                throw new RuntimeException(t);
            }
            return result.get();
        }
        // 理论上不可达
        return action.call();
    }

    /** 在 FX 线程上执行 {@code action}，不关心返回值。 */
    public static void run(FxRunnable action) throws Exception {
        call(() -> {
            action.run();
            return null;
        });
    }

    @FunctionalInterface
    public interface FxCallable<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    public interface FxRunnable {
        void run() throws Exception;
    }
}
