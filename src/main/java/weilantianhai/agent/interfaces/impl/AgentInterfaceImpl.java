package weilantianhai.agent.interfaces.impl;

import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.interfaces.ResponseListener;
import weilantianhai.agent.llm.LLMClient;
import weilantianhai.agent.llm.PromptLoader;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentInterfaceImpl implements AgentInterface {
    private final LLMClient client;
    private final CommandExecutor executor;

    private final BlockingDeque<Request> queue = new LinkedBlockingDeque<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private volatile ResponseListener listener;
    private Thread worker;
    private volatile boolean running = false;

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
            try {
                Request req = queue.take();
                process(req);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                notifyError(-1, "意外错误: " + t.getMessage());
            }
        }
    }

    private void process(Request req) {
        System.out.println("=== 用户输入 (id=" + req.id + ") ===\n" + req.input);
        try {
            String json = client.chat(SYSTEM_PROMPT, req.input);
            System.out.println("\n=== LLM JSON ===\n" + json);

            String md = executor.execute(json);
            System.out.println("\n=== Markdown ===\n" + md);

            notifyResponse(req.id, buildReply(md));
        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
            e.printStackTrace();
            notifyError(req.id, "处理失败: " + e.getMessage());
        }
    }

    /** 把 Markdown 拼成给用户看的一句话 */
    private String buildReply(String md) {
        if (md == null || md.isBlank()) return "操作已执行。";
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
}
