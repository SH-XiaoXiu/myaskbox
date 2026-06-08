package cn.xiuxius.askbox.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("AskBox API").description("匿名提问箱 API 文档").version("2.0"))
                .components(new Components()
                        .addSecuritySchemes(
                                "Authorization",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("Bearer {token}")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("公开接口")
                .pathsToMatch("/api/boxes/**", "/api/attachments/**", "/api/health")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .displayName("认证接口")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi boxApi() {
        return GroupedOpenApi.builder()
                .group("box")
                .displayName("Box Owner 接口")
                .pathsToMatch("/api/box/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("系统管理接口")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
