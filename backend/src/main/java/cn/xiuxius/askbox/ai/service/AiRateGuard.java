package cn.xiuxius.askbox.ai.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cn.xiuxius.askbox.ai.config.AiProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiRateGuard {
    private static final String COUNT_KEY = "askbox:ai:rate:global";
    private static final String BREAKER_KEY = "askbox:ai:circuit:open";

    private final StringRedisTemplate redisTemplate;
    private final AiProperties properties;

    public GuardResult tryAcquire() {
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BREAKER_KEY))) {
                return GuardResult.blocked("AI调用熔断中");
            }
            Long count = redisTemplate.opsForValue().increment(COUNT_KEY);
            if (count != null && count == 1L) {
                redisTemplate.expire(COUNT_KEY, Duration.ofSeconds(properties.getRateLimitWindowSeconds()));
            }
            if (count != null && count > properties.getRateLimitCapacity()) {
                openBreaker();
                return GuardResult.blocked("AI调用超过限流阈值，已触发熔断");
            }
            return GuardResult.permit();
        } catch (RuntimeException ex) {
            log.warn("AI rate guard failed closed: {}", ex.getMessage());
            return GuardResult.blocked("AI限流组件不可用");
        }
    }

    private void openBreaker() {
        redisTemplate.opsForValue().set(BREAKER_KEY, "1", Duration.ofSeconds(properties.getCircuitBreakerSeconds()));
    }

    public record GuardResult(boolean allowed, String reason) {
        static GuardResult permit() {
            return new GuardResult(true, null);
        }

        static GuardResult blocked(String reason) {
            return new GuardResult(false, reason);
        }
    }
}
