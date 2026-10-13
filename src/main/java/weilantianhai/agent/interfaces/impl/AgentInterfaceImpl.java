package weilantianhai.agent.interfaces.impl;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.execute.ResultFormatter;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.interfaces.ResponseListener;
import weilantianhai.agent.ir.IRBundle;
import weilantianhai.agent.ir.IRCommand;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.ir.resolver.IRParser;
import weilantianhai.agent.llm.LLMClient;
import weilantianhai.agent.llm.PromptLoader;
import weilantianhai.agent.trace.RoundTrace;
import weilantianhai.agent.trace.TraceListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentInterfaceImpl implements AgentInterface {
    /** 当前 LLM 客户端。非 final：数据集脚本可在两个任务之间切换模型做横向对比。 */
    private volatile LLMClient client;
    private final CommandExecutor executor;

    private final BlockingDeque<Request> queue = new LinkedBlockingDeque<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private volatile ResponseListener listener;
    private volatile TraceListener traceListener;
    private Thread worker;
    private volatile boolean running = false;

    private volatile int currentRequestId = -1;
    private final Object queueLock =new Object();

    private final IRParser parser = new IRParser();
    private static final int MAX_ROUNDS = 30;

    public AgentInterfaceImpl(LLMClient client, CommandExecutor executor) {
        this.client = client;
        this.executor = executor;
    }

    //生命周期
    @Override
    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::loop,"agent-worker");
        worker.setDaemon(true);
        worker.start();
    }
    @Override
    public void shutdown() {
        if(!running) return;
        running = false;
        Thread w = worker;
        if(w != null) w.interrupt();
        if (w != null) {
            try { w.join(2000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
    @Override
    public boolean isRunning() {
        return running;
    }

    //提交输入
    @Override
    public int submitUserInput(String userInput) {
        if(userInput == null || userInput.isBlank()){
            throw new IllegalArgumentException("userInput can't be null or blank");
        }
        int id = idGenerator.getAndIncrement();
        queue.offer(new Request(id,userInput));
        return id;
    }

    //回调
    @Override
    public void setResponseListener(ResponseListener listener) {
        this.listener = listener;
    }

    @Override
    public void clearResponseListener() {
        this.listener = null;
    }

    @Override
    public void setTraceListener(TraceListener listener) {
        this.traceListener = listener;
    }

    @Override
    public void clearTraceListener() {
        this.traceListener = null;
    }

    @Override
    public void setClient(LLMClient client) {
        if (client == null) return;
        this.client = client;
    }

    @Override
    public LLMClient getClient() {
        return this.client;
    }


    private void loop() {
        while (running) {
            Request req;
            try {
                synchronized (queueLock) {
                    req = queue.take();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            process(req);
        }
    }

    private void process(Request req) {
        currentRequestId = req.id;   // ← 加这一行
        System.out.println("=== 用户输入 (id=" + req.id + ") ===\n" + req.input);
        boolean ok = false;
        try {
            List<JSONObject> messages = new ArrayList<>();
            messages.add(JSONObject.of("role","system","content",SYSTEM_PROMPT));
            messages.add(JSONObject.of("role","user","content",req.input));

            StringBuilder collected = new StringBuilder();
            String finalContent = null;

            for(int round = 1;round <= MAX_ROUNDS;round++){
                long roundStart = System.nanoTime();

                // 每轮开头快照一次 client：允许任务之间切换模型而不影响进行中的轮次
                LLMClient activeClient = client;
                String json = activeClient.chat(messages);
                RoundTrace trace = new RoundTrace(round, json);
                System.out.println("\n=== Round" + round + " LLM JSON ===\n" + json);

                // 解析失败不终止任务：降级成 Error Markdown 回传给 LLM，让它下一轮自己改。
                IRBundle bundle;
                try{
                    bundle = parser.parseAsBundle(json);
                }catch(IRException e){
                    String err = ResultFormatter.error(json, e.getCode() + ":" + e.getMessage());
                    System.out.println("\n=== Round" + round + "Parse Error ===\n" + err);
                    trace.parseFailed(e.getCode(), e.getMessage());
                    trace.setReceipts(err);
                    collected.append(err).append("\n\n");
                    messages.add(JSONObject.of("role", "assistant", "content", json));
                    messages.add(JSONObject.of("role", "user", "content", "[Tool Results]\n\n" + err));
                    trace.setElapsedMs((System.nanoTime() - roundStart) / 1_000_000L);
                    fireRound(req.id, trace);
                    continue;
                }

                // 解析成功：把类型清单与 done/content 记进 trace
                List<String> types = new ArrayList<>();
                for (IRCommand c : bundle.getOperations()) types.add(c.getType());
                trace.parseSucceeded(types, bundle.isDone(), bundle.getContent());

                if(bundle.isDone()){
                    System.out.println("=== Round" + round + "done ===");
                    finalContent = bundle.getContent();
                    trace.setElapsedMs((System.nanoTime() - roundStart) / 1_000_000L);
                    fireRound(req.id, trace);
                    ok = true;
                    break;
                }

                String md = executor.execute(bundle, json);
                System.out.println("\n=== Round" + round + "Markdown ===\n" + md);
                trace.setReceipts(md);
                // 从回执里反推 type / exec 级错误（GuardedOperation 把参数错误吞到执行期）
                TraceAnalysis.analyze(md, trace);
                collected.append(md).append("\n\n");

                messages.add(JSONObject.of("role", "assistant", "content", json));

                String toolResult = (md == null || md.isBlank())
                        ? "[Tool Results]\n\n(no output)"
                        : "[Tool Results]\n\n" + md;
                messages.add(JSONObject.of("role", "user", "content", toolResult));

                trace.setElapsedMs((System.nanoTime() - roundStart) / 1_000_000L);
                fireRound(req.id, trace);
            }

            notifyResponse(req.id, buildReply(finalContent, collected.toString().trim()));
            ok = true;

        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
            e.printStackTrace();
            notifyError(req.id, "处理失败: " + e.getMessage());
        } finally {
            currentRequestId = -1;
            TraceListener tl = traceListener;
            if (tl != null) {
                try { tl.onRequestFinished(req.id, ok); }
                catch (Throwable ignored) { }
            }
        }
    }

    /** 把一轮 trace 交给 listener；listener 抛异常绝不影响主流程。 */
    private void fireRound(int requestId, RoundTrace trace) {
        TraceListener tl = traceListener;
        if (tl == null) return;
        try {
            tl.onRound(requestId, trace);
        } catch (Throwable t) {
            System.err.println("traceListener.onRound 抛异常（已忽略）: " + t.getMessage());
        }
    }

    /**
     * 把模型的总结（{@code content}）或累积的执行结果拼成给用户看的一句话。
     *
     * <p>优先用模型自己写的 {@code content}——它是"给用户看的一句话总结"；
     * 没有时才退回累积的 Markdown。
     */
    private String buildReply(String content, String md) {
        if (content != null && !content.isBlank()) return content.trim();
        if (md == null || md.isBlank()) return "完成。";
        if (CommandExecutor.DONE_MARKER.equals(md)) return "完成。";
        return "操作已执行：\n" + md;
    }

    private void notifyResponse(int id, String text) {
        ResponseListener l = listener;
        if (l != null) l.onResponse(id, text);
    }

    private void notifyError(int id, String msg) {
        ResponseListener l = listener;
        if (l != null) l.onError(id, msg);
    }

    private record Request(int id, String input) {}

    private static final String SYSTEM_PROMPT = PromptLoader.loadSystemPrompt();

    //队列管理

    @Override
    public int getPendingCount() {
        return queue.size();
    }

    @Override
    public boolean isBusy() {
        return currentRequestId != -1;
    }

    @Override
    public int clearPending() {
        synchronized (queueLock) {
            int count = queue.size();
            queue.clear();
            return count;
        }
    }

    @Override
    public boolean cancelRequest(int requestId) {
        if (currentRequestId == requestId) return false;
        synchronized (queueLock) {
            return queue.removeIf(r -> r.id() == requestId);
        }
    }

//状态查询

    @Override
    public String getStatusSummary() {
        int pending = queue.size();
        if (currentRequestId != -1) {
            return pending > 0
                    ? "处理中 (id=" + currentRequestId + ")，队列 " + pending + " 条"
                    : "处理中 (id=" + currentRequestId + ")";
        }
        return pending > 0 ? "空闲，队列 " + pending + " 条" : "空闲";
    }
}
