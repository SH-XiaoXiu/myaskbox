package cn.xiuxius.askbox.ai.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cn.xiuxius.askbox.ai.config.AiProperties;

@Component
public class OpenAiCompatibleClient {
    private final AiProperties properties;

    public OpenAiCompatibleClient(AiProperties properties) {
        this.properties = properties;
    }

    public ChatResult chat(String systemPrompt, String userPrompt) {
        RestClient client = RestClient.builder()
                .baseUrl(normalizedBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory())
                .build();
        Map<?, ?> body = Map.of(
                "model",
                properties.getModel(),
                "response_format",
                Map.of("type", "json_object"),
                "messages",
                List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        String requestPayload = serialize(body);
        OpenAiResponse response =
                client.post().uri("/chat/completions").body(body).retrieve().body(OpenAiResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("AI响应为空");
        }
        return new ChatResult(
                requestPayload,
                serialize(response),
                response.choices().get(0).message().content());
    }

    private String normalizedBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String serialize(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private org.springframework.http.client.ClientHttpRequestFactory requestFactory() {
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        return factory;
    }

    record OpenAiResponse(List<Choice> choices) {
        record Choice(Message message) {}

        record Message(String content) {}
    }

    public record ChatResult(String requestPayload, String responsePayload, String content) {}
}
