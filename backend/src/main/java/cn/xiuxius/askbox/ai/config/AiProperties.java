package cn.xiuxius.askbox.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "askbox.ai")
public class AiProperties {
    private boolean enabled = true;
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-4.1-mini";
    private String promptDirectory = "prompts";
    private int timeoutSeconds = 60;
    private int historyCandidateLimit = 12;
    private int popularExampleLimit = 8;
    private int rateLimitCapacity = 20;
    private int rateLimitWindowSeconds = 3600;
    private int circuitBreakerSeconds = 3600;
}
