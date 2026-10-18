package HCloudbyte.ui.scenetree;

import javafx.application.Platform;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class SceneTreePoller {
    private final Supplier<List<String>> pathSupplier;
    private final Consumer<Set<String>> onKeysChanged;
    private final AtomicReference<Set<String>> lastKeys = new AtomicReference<>(Set.of());

    SceneTreePoller(Supplier<List<String>> pathSupplier, Consumer<Set<String>> onKeysChanged) {
        this.pathSupplier = pathSupplier;
        this.onKeysChanged = onKeysChanged;
    }

    /** 启动轮询线程（不管理生命周期：随进程退出而终止）。 */
    void start(long intervalMillis) {
        lastKeys.set(snapshot());
        Thread.ofVirtual().name("scene-tree-poller").start(() -> loop(intervalMillis));
    }

    /** UI 侧重建完成后同步基准，避免下一秒重复重建。 */
    void acknowledge(Collection<String> keys) {
        lastKeys.set(new HashSet<>(keys));
    }

    private void loop(long intervalMillis) {
        while (true) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                return;
            }
            Set<String> now = snapshot();
            Set<String> previous = lastKeys.getAndSet(now);
            if (!now.equals(previous)) {
                Platform.runLater(() -> onKeysChanged.accept(now));
            }
        }
    }

    /** 取当前 keySet 快照；读取异常时保持上次结果（不触发无意义重建）。 */
    private Set<String> snapshot() {
        try {
            return new HashSet<>(pathSupplier.get());
        } catch (Exception e) {
            return lastKeys.get();
        }
    }
}
