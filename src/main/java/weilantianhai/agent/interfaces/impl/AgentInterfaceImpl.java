package weilantianhai.agent.interfaces.impl;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.interfaces.ResponseListener;
import weilantianhai.agent.ir.IRBundle;
import weilantianhai.agent.ir.resolver.IRParser;
import weilantianhai.agent.llm.LLMClient;
import weilantianhai.agent.llm.PromptLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentInterfaceImpl implements AgentInterface {
    private final LLMClient client;
    private final CommandExecutor executor;

    private final BlockingDeque<Request> queue = new LinkedBlockingDeque<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private volatile ResponseListener listener;
    private Thread worker;
    private volatile boolean running = false;

    private volatile int currentRequestId = -1;
    private final Object queueLock =new Object();

    private final IRParser parser = new IRParser();
    private static final int MAX_ROUNDS = 10;

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
        try {
            List<JSONObject> messages = new ArrayList<>();
            messages.add(JSONObject.of("role","system","content",SYSTEM_PROMPT));
            messages.add(JSONObject.of("role","user","content",req.input));

            StringBuilder collected = new StringBuilder();

            for(int round = 1;round <= MAX_ROUNDS;round++){
                String json = client.chat(messages);
                System.out.println("\n=== Round" + round + " LLM JSON ===\n" + json);

                IRBundle bundle = parser.parseAsBundle(json);
                if(bundle.isDone()){
                    System.out.println("=== Round" + round + "done ===");
                    break;
                }

                String md = executor.execute(json);
                System.out.println("\n=== Round" + round + "Markdown ===\n" + md);
                collected.append(md).append("\n\n");

                messages.add(JSONObject.of("role", "assistant", "content", json));
                messages.add(JSONObject.of("role", "user", "content", md));
            }

            notifyResponse(req.id, buildReply(collected.toString().trim()));

        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
            e.printStackTrace();
            notifyError(req.id, "处理失败: " + e.getMessage());
        } finally {
            currentRequestId = -1;
        }
    }

    /** 把 Markdown 拼成给用户看的一句话 */
    private String buildReply(String md) {
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
