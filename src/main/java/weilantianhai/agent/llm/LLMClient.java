package weilantianhai.agent.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LLMClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(AppConfig.LLM_CONNECT_TIMEOUT))
            .build();

    public String chat(String systemPrompt,String userInput) throws Exception{
        JSONObject root = new JSONObject();
        root.put("model",AppConfig.MODEL_NAME);
        root.put("temperature",AppConfig.TEMPERATURE);
        root.put("max_tokens",AppConfig.MAX_TOKENS);
        root.put("stream",false);

        //强制json输出
        //root.put("response_format",JSONObject.of("type","json_object"));

        //2.构建 messages 数组
        JSONArray messages = new JSONArray();
        messages.add(JSONObject.of(
                "role","system",
                "content",systemPrompt
        ));
        messages.add(JSONObject.of(
                "role","user",
                "content",userInput
        ));
        root.put("messages",messages);
        //关闭思考
        JSONObject templateKwargs = new JSONObject();
        templateKwargs.put("enable_thinking", false);
        root.put("chat_template_kwargs", templateKwargs);

        String body = root.toJSONString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.LLM_CHAT_URL))
                .timeout(Duration.ofSeconds(AppConfig.LLM_REQUEST_TIMEOUT))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request,HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200){
            throw new RuntimeException("LLM 请求失败："+ response.statusCode() +"\n" +response.body());
        }

        JSONObject json = JSON.parseObject(response.body());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

    }
}
