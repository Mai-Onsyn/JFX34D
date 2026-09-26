package weilantianhai.agent.config;

import weilantianhai.agent.ir.IRException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvLoader {
    private EnvLoader() {}

    public static Map<String, String> load(Path envPath) {
        Map<String, String> map = new HashMap<>();
        if(!Files.exists(envPath)) return map;
        try {
            List<String> lines=Files.readAllLines(envPath);
            for(String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty()) ;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        }catch(IOException ignored){}
        return map;
    }

    public static String resolveApiKey(String envName,Map<String,String> envFile) throws IRException {
        if(envName == null || envName.isBlank()){
            throw new IRException("NO_API_KEY_ENV","未配置 apiKeyEnv");
        }
        String v = System.getenv(envName);
        if(v!=null && !v.isBlank()) return v;

        v = envFile.get(envName);

        if(v!=null && !v.isBlank()) return v;

        throw new IRException("MISSING_API_KEY",
                "未找到"+envName +",请设置系统环境变量或在 .env 中配置");
    }
}
