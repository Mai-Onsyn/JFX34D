package weilantianhai.dataset.core;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.interfaces.ResponseListener;
import weilantianhai.agent.trace.RoundTrace;
import weilantianhai.agent.trace.TraceListener;
import weilantianhai.dataset.tasks.TaskGoal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AgentTaskRunner {
    /** 单任务超时（毫秒）。超时按 TIMEOUT 处理，已采集的 trace 仍会落盘。 */
    private static final long DEFAULT_TIMEOUT_MS = 300_000L;

    private final AgentInterface agent;
    private final TraceCollector collector;
    private final long timeoutMs;

    public AgentTaskRunner(AgentInterface agent, TraceCollector collector) {
        this(agent, collector, DEFAULT_TIMEOUT_MS);
    }

    public AgentTaskRunner(AgentInterface agent, TraceCollector collector, long timeoutMs) {
        this.agent = agent;
        this.collector = collector;
        this.timeoutMs = timeoutMs;
    }

    /** 一条任务的执行结果。 */
    public record RunOutcome(boolean completed, boolean timedOut, String errorMessage, long elapsedMs) {
    }

    /**
     * 同步执行一条任务。
     *
     * @param record 待填充的记录对象（trace 会直接写进去）
     * @return 执行结局
     */
    public RunOutcome run(TaskGoal task, TaskRunRecord record) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        StringBuilder reply = new StringBuilder();
        Throwable[] err = new Throwable[1];

        record.markStart();
        long start = System.currentTimeMillis();

        // 1. trace 回调：每轮直接进记录
        agent.setTraceListener(new TraceListener() {
            @Override
            public void onRound(int requestId, RoundTrace trace) {
                record.addRound(trace);
                // 采集这一轮之后的世界状态（必须在 FX 线程，由 collector 负责）
                try {
                    trace.setStateAfter(collector.snapshot());
                } catch (Throwable t) {
                    System.err.println("[dataset] 状态快照失败: " + t.getMessage());
                }
            }
        });

        // 2. 回复回调：任务收尾
        agent.setResponseListener(new ResponseListener() {
            @Override
            public void onResponse(int requestId, String responseText) {
                reply.append(responseText == null ? "" : responseText);
                record.setAgentError(false, null);
                latch.countDown();
            }

            @Override
            public void onError(int requestId, String errorMessage) {
                err[0] = new RuntimeException(errorMessage);
                record.setAgentError(true, errorMessage);
                latch.countDown();
            }
        });

        try {
            int requestId = agent.submitUserInput(task.prompt());
            record.setRequestId(requestId);

            boolean done = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!done) {
                timedOut.set(true);
                agent.cancelRequest(requestId);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            err[0] = ie;
        } catch (Throwable t) {
            err[0] = t;
            record.setAgentError(true, t.getMessage());
        } finally {
            agent.clearTraceListener();
            agent.clearResponseListener();
        }

        long elapsed = System.currentTimeMillis() - start;
        record.markFinish(elapsed);
        record.setReplyMarkdown(reply.toString().trim());

        String message = err[0] == null ? null : err[0].getMessage();
        boolean completed = err[0] == null && !timedOut.get();
        return new RunOutcome(completed, timedOut.get(), message, elapsed);
    }

    /** 场景快照采集回调（由数据集应用注入，内部负责 FX 线程切换）。 */
    public interface TraceCollector {
        JSONObject snapshot() throws Exception;
    }
}
