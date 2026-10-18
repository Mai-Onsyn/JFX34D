package weilantianhai.agent.ir.operations;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
