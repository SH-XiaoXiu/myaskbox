package cn.xiuxius.askbox.common.ratelimit;

/** 限流维度。 */
public enum RateLimitType {
    /** 全局（所有请求共享一个桶）。 */
    GLOBAL,
    /** 按客户端 IP。 */
    IP,
    /** 按已登录用户 ID。 */
    USER,
    /** 自定义 key（通过 SpEL 表达式指定）。 */
    CUSTOM
}
