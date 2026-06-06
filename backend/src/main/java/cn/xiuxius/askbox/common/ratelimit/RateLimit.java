package cn.xiuxius.askbox.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解，标注在 Controller 方法上即可生效。
 * <p>
 * 支持多种限流类型组合（AND 逻辑），默认按 IP 限流。
 * 可通过 SpEL 表达式自定义限流 key。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流类型数组，默认 IP。多个类型为 AND 关系：所有类型都未超限才放行。 */
    RateLimitType[] types() default {RateLimitType.IP};

    /** SpEL 表达式，用于 CUSTOM 类型的 key（如 {@code #request.username}）。 */
    String key() default "";

    /** 令牌桶容量，<=0 时使用配置中的默认值。 */
    int capacity() default -1;

    /** 每个 timeWindowSeconds 补充的令牌数，<=0 时等于 capacity。 */
    int refillTokens() default -1;

    /** 补充时间窗口（秒），<=0 时使用配置中的默认值。 */
    int timeWindowSeconds() default -1;
}
