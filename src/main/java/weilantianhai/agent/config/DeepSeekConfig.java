package weilantianhai.agent.config;

import weilantianhai.agent.ir.IRException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class DeepSeekConfig {
    private final String baseURL;
    private final String model;
    private final String apiKeyEnv;

    private DeepSeekConfig(String baseURL, String model, String apiKeyEnv) {
        this.baseURL = baseURL;
        this.model = model;
        this.apiKeyEnv = apiKeyEnv;
    }

    public static DeepSeekConfig load(Path path) throws IRException{
        if(!Files.exists(path)){
            throw new IRException("CONFIG_MISSING","找不到配置文件："+path);
        }
        Properties props =new Properties();
        try(InputStream in = Files.newInputStream(path)){
            props.load(in);
        }catch (IOException e){
            throw new IRException("CONFIG_LOAD_FAIL","读取失败："+e.getMessage(),e);
        }

        String baseURL = props.getProperty("baseURL");
        String model = props.getProperty("model");
        String apiKeyEnv = props.getProperty("apiKeyEnv");

        if(baseURL==null||model==null||apiKeyEnv==null){
            throw new IRException("CONFIG_INCOMPLETE",
                    "deepseek.properties 缺少 baseURL / model / apiKeyEnv");
        }
        return new DeepSeekConfig(baseURL, model, apiKeyEnv);
    }
    public String getBaseURL() { return baseURL; }
    public String getModel() { return model; }
    public String getApiKeyEnv() { return apiKeyEnv; }
    public String getChatURL() { return baseURL + "/chat/completions"; }
}
