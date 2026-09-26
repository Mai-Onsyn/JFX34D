package weilantianhai.agent.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PromptLoader {
    private static final String PATH = "/prompts/system.md";
    private static String cached;

    private PromptLoader() {}

    public static synchronized String loadSystemPrompt() {
        if (cached != null) return cached;
        try (InputStream in = PromptLoader.class.getResourceAsStream(PATH)) {
            if (in == null) {
                throw new IllegalStateException("找不到提示词文件: " + PATH);
            }
            cached = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return cached;
        } catch (IOException e) {
            throw new IllegalStateException("读取提示词失败: " + e.getMessage(), e);
        }
    }
}
