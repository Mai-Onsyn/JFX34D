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
import java.util.List;
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
        root.put("max_tokens", maxTokens);
        root.put("stream", false);
        applyThinking(root);

        JSONArray messages = new JSONArray();
        messages.add(JSONObject.of("role", "system", "content", systemPrompt));
        messages.add(JSONObject.of("role", "user", "content", userInput));
        root.put("messages", messages);

        return post(root);
    }

    public String chat(List<JSONObject> message) throws Exception{
        JSONObject root = new JSONObject();
        root.put("model",model);
        root.put("max_tokens", maxTokens);
        root.put("stream", false);
        applyThinking(root);

        JSONArray arr = new JSONArray();
        arr.addAll(message);
        root.put("messages", arr);

        return post(root);
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 设置思考模式。
     *
     * <p><b>这里踩过一个坑</b>：DeepSeek 关闭思考用的是
     * {@code thinking: {"type": "disabled"}}，而 {@code chat_template_kwargs.enable_thinking}
     * 是 Qwen / vLLM 的写法，DeepSeek 会**静默忽略**。之前 {@code chat(List)} 用的正是后者，
     * 等于思考根本没被关掉；而 DeepSeek V4 系列**默认开启思考**，
     * 于是 {@code max_tokens} 会被推理过程吃光，{@code content} 返回空串，
     * 上层只看到"JSON 解析为空"，完全看不出原因。
     *
     * <p>另外思考模式**不支持 {@code temperature}**，所以只在非思考模式下传这个字段。
     */
    private void applyThinking(JSONObject root) {
        JSONObject thinking = new JSONObject();
        if (enableThinking) {
            thinking.put("type", "enabled");
            root.put("thinking", thinking);
            if (reasoningEffort != null && !reasoningEffort.isBlank()) {
                root.put("reasoning_effort", reasoningEffort);
            }
        } else {
            thinking.put("type", "disabled");
            root.put("thinking", thinking);
            root.put("temperature", AppConfig.TEMPERATURE);
        }
    }

    private String post(JSONObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/chat/completions"))
                .timeout(Duration.ofSeconds(AppConfig.LLM_REQUEST_TIMEOUT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM 请求失败: " + response.statusCode()
                    + "\n" + response.body());
        }
        return extractContent(response.body());
    }

    /**
     * 从响应体里取 content，并在取不到时给出**可诊断**的错误。
     *
     * <p>之前是直接取 {@code choices[0].message.content}：一旦模型把输出预算花在推理上
     * （DeepSeek V4 系列默认开启思考模式），content 就是空串，
     * 调用方只能看到"JSON 解析为空"，完全猜不到原因。这里把
     * {@code finish_reason} / {@code reasoning_content} / {@code usage} 一起带进错误信息。
     */
    private String extractContent(String rawBody) {
        JSONObject json = JSON.parseObject(rawBody);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM 响应里没有 choices：" + abbreviate(rawBody));
        }
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        String content = message == null ? null : message.getString("content");
        if (content != null && !content.isBlank()) {
            return content;
        }

        String finishReason = choice.getString("finish_reason");
        String reasoning = message == null ? null : message.getString("reasoning_content");
        Object usage = json.get("usage");

        StringBuilder sb = new StringBuilder("LLM 返回了空 content（finish_reason=")
                .append(finishReason);
        if (reasoning != null && !reasoning.isBlank()) {
            sb.append("，reasoning_content 有 ").append(reasoning.length()).append(" 字符");
        }
        sb.append("，usage=").append(usage).append("）");

        if ("length".equals(finishReason)) {
            sb.append("\n原因：max_tokens=").append(maxTokens)
              .append(" 被耗尽。开了思考模式时推理过程会占满预算，content 就会是空的 ——")
              .append("请确认 thinking 已按 DeepSeek 格式关闭，或调大 max_tokens。");
        } else if (reasoning != null && !reasoning.isBlank()) {
            sb.append("\n原因：内容都落在 reasoning_content 里，说明思考模式没有被关掉。");
        }
        throw new RuntimeException(sb.toString());
    }

    private static String abbreviate(String s) {
        if (s == null) return "null";
        return s.length() <= 500 ? s : s.substring(0, 500) + "...(truncated)";
    }
}
