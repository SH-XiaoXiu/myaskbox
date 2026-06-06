package cn.xiuxius.askbox.common.ratelimit;

import cn.xiuxius.askbox.common.ratelimit.RateLimitProperties.RuleConfig;

/** 限流器接口。实现类负责令牌桶的 tryConsume 逻辑。 */
public interface RateLimiter {

    /**
     * 尝试消费 1 个令牌。
     *
     * @param key    限流 key（如 {@code askbox:ratelimit:ip:POST:/api/auth/login:192.168.1.1}）
     * @param config 限流规则配置
     * @return true 放行，false 被限流
     */
    boolean tryConsume(String key, RuleConfig config);

    /**
     * 获取剩余可用令牌数（用于 429 响应中的 remaining 字段）。
     *
     * @param key 限流 key
     * @return 剩余令牌数，-1 表示无法获取
     */
    long getAvailableTokens(String key);
}
