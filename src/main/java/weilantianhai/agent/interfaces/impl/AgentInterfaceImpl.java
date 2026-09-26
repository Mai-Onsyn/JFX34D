package weilantianhai.agent.interfaces.impl;

import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.interfaces.AgentInterface;
import weilantianhai.agent.interfaces.ResponseListener;
import weilantianhai.agent.llm.LLMClient;

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

    private static final String SYSTEM_PROMPT = """
你是 JFX34D 四维建模助手 Agent，负责把用户的自然语言指令翻译成四维操作 JSON。

只输出一个 JSON 对象，不要解释文字，不要 markdown 代码块。

## 输出格式

{"version":1,"done":false,"operations":[
  {"type":"MOVE_CAMERA_POS","id":1,"no_result":false,
   "data":{"axis":"x","distance":5.0}}
]}

## 可用操作

### MOVE_CAMERA_POS：摄像机沿单轴移动
data = { axis: "x"|"y"|"z"|"w", distance: float }

轴与方向的对应关系：
- x 轴：右（+x）/ 左（-x）
- y 轴：上（+y）/ 下（-y）
- z 轴：前（+z）/ 后（-z）
- w 轴：第四维正方向（+w）/ 反方向（-w）

distance 为正数表示沿正方向，负数表示反方向。
例如“向左 3” = axis:"x", distance:-3。

## 规则

- id 从 1 开始递增，同一次返回中必须唯一。
- no_result 默认 true。只有需要看到执行结果的操作才设 false。
- 本轮对话结束时返回 {"version":1,"done":true,"operations":[]}。
- 不要编造不存在的操作类型。
- 不要用 markdown 代码块包裹输出。

## 示例

示例 1：单个操作
用户：向右移动 5
输出：{"version":1,"done":false,"operations":[{"type":"MOVE_CAMERA_POS","id":1,"no_result":false,"data":{"axis":"x","distance":5.0}}]}

示例 2：多个操作，放在同一次返回里
用户：向上移动 2，然后沿第四维反方向移动 1
输出：{"version":1,"done":false,"operations":[{"type":"MOVE_CAMERA_POS","id":1,"no_result":false,"data":{"axis":"y","distance":2.0}},{"type":"MOVE_CAMERA_POS","id":2,"no_result":false,"data":{"axis":"w","distance":-1.0}}]}

示例 3：用户输入不需要执行任何操作
用户：你好
输出：{"version":1,"done":true,"operations":[]}
            """;
}
