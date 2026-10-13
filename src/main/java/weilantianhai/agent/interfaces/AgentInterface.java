package weilantianhai.agent.interfaces;

import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.interfaces.impl.AgentInterfaceImpl;
import weilantianhai.agent.llm.LLMClient;
import weilantianhai.agent.trace.TraceListener;

public interface AgentInterface {

    //生命周期
    void start();
    void shutdown();
    boolean isRunning();

    //提交输入
    int submitUserInput(String userInput);

    //接受回复
    void setResponseListener(ResponseListener listener);
    void clearResponseListener();

    //逐轮追踪（数据集采集 / 调试）
    void setTraceListener(TraceListener listener);
    void clearTraceListener();

    //底层模型（数据集跑不同模型对比时切换）
    /**
     * 切换底层 LLM。只影响**后续**轮次；正在执行中的请求不受影响（每轮开头读一次）。
     *
     * @param client 新的 LLM 客户端；null 会被忽略
     */
    void setClient(LLMClient client);

    /** 当前 LLM 客户端（只读）。 */
    LLMClient getClient();

    //队列管理
    int getPendingCount();
    boolean isBusy();
    int clearPending();
    boolean cancelRequest(int requestId);

    //状态查询
    String getStatusSummary();

    static AgentInterface getInstance() {
        if (InterfaceValue._INSTANCE == null) {
            throw new ExceptionInInitializerError("AgentInterface instance is not initialized");
        } else return InterfaceValue._INSTANCE;
    }

    static void initialize(
            LLMClient client,
            CommandExecutor executor
    ) {
        InterfaceValue._INSTANCE = new AgentInterfaceImpl(client, executor);
    }
}

class InterfaceValue {
    static AgentInterface _INSTANCE;
}