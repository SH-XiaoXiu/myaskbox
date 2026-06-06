package cn.xiuxius.askbox.common.ratelimit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 限流组件自动配置。
 * <p>
 * 默认启用，可通过 {@code askbox.rate-limit.enabled=false} 关闭。
 * 当前仅使用本地 Caffeine 实现，后续可扩展 Redisson 分布式实现。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "askbox.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimitKeyGenerator rateLimitKeyGenerator(RateLimitProperties properties) {
        return new RateLimitKeyGenerator(properties.getKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter() {
        return new LocalRateLimiter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(
            RateLimiter rateLimiter, RateLimitProperties properties, RateLimitKeyGenerator keyGenerator) {
        return new RateLimitAspect(rateLimiter, properties, keyGenerator);
    }
}
