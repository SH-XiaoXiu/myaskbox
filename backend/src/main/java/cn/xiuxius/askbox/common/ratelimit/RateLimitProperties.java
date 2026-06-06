package cn.xiuxius.askbox.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 限流配置属性，前缀 {@code askbox.rate-limit}。
 * <p>
 * 支持为每种限流类型设置默认规则，也支持 bypass 模式（不拦截但记录日志）。
 */
@Data
@ConfigurationProperties(prefix = "askbox.rate-limit")
public class RateLimitProperties {

    /** 限流总开关。false 时自动配置不加载。 */
    private boolean enabled = true;

    /**
     * Bypass 模式：仍然消费令牌并记录日志，但不抛出异常拦截请求。
     * 适用于压力测试或集成调试。
     */
    private boolean bypass = false;

    /** Redis/Caffeine key 前缀。 */
    private String keyPrefix = "askbox:ratelimit";

    private RuleConfig global = new RuleConfig(1000, 1000, 60);
    private RuleConfig ip = new RuleConfig(100, 100, 60);
    private RuleConfig user = new RuleConfig(50, 50, 60);
    private RuleConfig custom = new RuleConfig(60, 60, 60);

    /** 根据限流类型获取默认规则配置。 */
    public RuleConfig getDefaultForType(RateLimitType type) {
        return switch (type) {
            case GLOBAL -> global;
            case IP -> ip;
            case USER -> user;
            case CUSTOM -> custom;
        };
    }

    @Data
    public static class RuleConfig {
        private int capacity;
        private int refillTokens;
        private int timeWindowSeconds;

        public RuleConfig() {}

        public RuleConfig(int capacity, int refillTokens, int timeWindowSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.timeWindowSeconds = timeWindowSeconds;
        }
    }
}
