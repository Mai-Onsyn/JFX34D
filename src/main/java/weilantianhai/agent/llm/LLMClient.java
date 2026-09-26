package weilantianhai.agent.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.config.AppConfig;
import weilantianhai.agent.config.DeepSeekConfig;
import weilantianhai.agent.config.EnvLoader;
import weilantianhai.agent.ir.IRException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public class LLMClient {

    private final HttpClient http;
    private final String baseURL;
    private final String model;
    private final String apiKey;
    private final int maxTokens;
    private final boolean enableThinking;
    private final String reasoningEffort;

    public LLMClient(String baseURL, String model, String apiKey,
                     int maxTokens, boolean enableThinking , String reasoningEffort) {
        this.baseURL = baseURL;
        this.model = model;
        this.apiKey = apiKey;
        this.maxTokens = maxTokens;
        this.enableThinking = enableThinking;
        this.reasoningEffort = reasoningEffort;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(AppConfig.LLM_CONNECT_TIMEOUT))
                .build();
    }

    /** 本地 LM Studio，默认关思考 */
    public static LLMClient local() {
        return local(false);
    }

    /** 本地 LM Studio，可指定思考开关 */
    public static LLMClient local(boolean enableThinking) {
        return new LLMClient(
                "http://localhost:1234/v1",
                "qwen/qwen3.5-9b",
                "lm-studio",
                enableThinking ? 4096 : 2048,   // 开思考要留更多 token
                enableThinking,
                ""
        );
    }

    /** DeepSeek，默认关思考 */
    public static LLMClient deepSeek() throws IRException {
        return deepSeek(false, "high");
    }

    /** DeepSeek，可指定思考开关 */
    public static LLMClient deepSeek(boolean enableThinking , String reasoningEffort) throws IRException {
        Map<String, String> envFile = EnvLoader.load(Path.of(AppConfig.DOTENV_PATH));
        DeepSeekConfig cfg = DeepSeekConfig.load(Path.of(AppConfig.DEEPSEEK_CONFIG));
        String key = EnvLoader.resolveApiKey(cfg.getApiKeyEnv(), envFile);
        return new LLMClient(
                cfg.getBaseURL(),
                cfg.getModel(),
                key,
                enableThinking ? 8192 : 4096,
                enableThinking,
                reasoningEffort
        );
    }

    public String chat(String systemPrompt, String userInput) throws Exception {
        JSONObject root = new JSONObject();
        root.put("model", model);
        root.put("temperature", AppConfig.TEMPERATURE);
        root.put("max_tokens", maxTokens);
        root.put("stream", false);

        // 思考模式控制（DeepSeek 格式）
        if (enableThinking) {
            // 开启思考（默认就是 enabled，显式传入更明确）
            JSONObject thinking = new JSONObject();
            thinking.put("type", "enabled");
            root.put("thinking", thinking);

            // 思考强度（可选）
            if (reasoningEffort != null && !reasoningEffort.isBlank()) {
                root.put("reasoning_effort", reasoningEffort);
            }
            // 注意：思考模式下 temperature 可能不生效，可以考虑不传或传默认值
        } else {
            // 关闭思考
            JSONObject thinking = new JSONObject();
            thinking.put("type", "disabled");
            root.put("thinking", thinking);
            // 非思考模式下可以使用 temperature
            root.put("temperature", AppConfig.TEMPERATURE);
        }

        JSONArray messages = new JSONArray();
        messages.add(JSONObject.of("role", "system", "content", systemPrompt));
        messages.add(JSONObject.of("role", "user", "content", userInput));
        root.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/chat/completions"))
                .timeout(Duration.ofSeconds(AppConfig.LLM_REQUEST_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(root.toJSONString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM 请求失败: " + response.statusCode()
                    + "\n" + response.body());
        }

        JSONObject json = JSON.parseObject(response.body());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
}