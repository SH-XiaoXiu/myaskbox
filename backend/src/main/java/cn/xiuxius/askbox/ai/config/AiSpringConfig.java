package cn.xiuxius.askbox.ai.config;

import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiSpringConfig {
    @Bean
    OpenAiApi askboxOpenAiApi(AiProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
        return OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl(properties.getBaseUrl()))
                .apiKey(properties.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }

    @Bean
    OpenAiChatModel askboxOpenAiChatModel(OpenAiApi askboxOpenAiApi, AiProperties properties) {
        return OpenAiChatModel.builder()
                .openAiApi(askboxOpenAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .responseFormat(ResponseFormat.builder()
                                .type(ResponseFormat.Type.JSON_OBJECT)
                                .build())
                        .build())
                .build();
    }

    @Bean
    ChatClient askboxChatClient(OpenAiChatModel askboxOpenAiChatModel) {
        return ChatClient.builder(askboxOpenAiChatModel).build();
    }

    private String normalizedBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com";
        }
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed.endsWith("/v1") ? trimmed.substring(0, trimmed.length() - 3) : trimmed;
    }
}
