package weilantianhai.agent.config;

public class AppConfig {
    public static final String LLM_BASE_URL = "http://localhost:1234/v1";
    public static final String LLM_CHAT_URL = LLM_BASE_URL + "/chat/completions";

    public static final String MODEL_NAME = "qwen/qwen3.5-9b";

    //生成参数
    public static final double TEMPERATURE = 0.1;
    public static final int MAX_TOKENS = 1024;

    //http
    public static final int SERVER_PORT = 8080;

    //超时
    public static final int LLM_CONNECT_TIMEOUT = 10;
    public static final int LLM_REQUEST_TIMEOUT = 180;

    //资源限制
    public static final int MAX_OPERATIONS =50;
    public static final float MAX_DISTANCE = 1000f;

    private AppConfig() {}
}
