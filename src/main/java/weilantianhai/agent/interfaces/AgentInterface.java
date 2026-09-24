package weilantianhai.agent.interfaces;

import weilantianhai.agent.execute.CommandExecutor;
import weilantianhai.agent.interfaces.impl.AgentInterfaceImpl;
import weilantianhai.agent.llm.LLMClient;

public interface AgentInterface {

    void sendToLLM(String userInput);

    void startThread();

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