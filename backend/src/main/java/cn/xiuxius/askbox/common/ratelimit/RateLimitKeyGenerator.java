package cn.xiuxius.askbox.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 根据限流类型生成对应的 Redis/Caffeine key。
 * <p>
 * Key 格式：{@code {prefix}:{type}:{endpoint}[:{identifier}]}，
 * 其中 endpoint 为 {@code HTTP_METHOD:/path}（如 {@code POST:/api/auth/login}）。
 */
public class RateLimitKeyGenerator {

    private final String keyPrefix;

    public RateLimitKeyGenerator(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * 生成限流 key。
     *
     * @param type     限流类型
     * @param endpoint HTTP_METHOD:/path
     * @param customKey SpEL 表达式结果（仅 CUSTOM 类型使用，可为 null）
     * @return 完整的限流 key
     */
    public String generate(RateLimitType type, String endpoint, String customKey) {
        return switch (type) {
            case GLOBAL -> keyPrefix + ":global:" + endpoint;
            case IP -> keyPrefix + ":ip:" + endpoint + ":" + resolveClientIp();
            case USER -> keyPrefix + ":user:" + endpoint + ":" + resolveUserId();
            case CUSTOM -> keyPrefix
                    + ":custom:"
                    + endpoint
                    + ":"
                    + (customKey != null && !customKey.isBlank() ? customKey : "unknown");
        };
    }

    /**
     * 解析客户端真实 IP：X-Forwarded-For > X-Real-IP > remoteAddr。
     */
    private String resolveClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 解析当前登录用户 ID（未登录时返回 "anonymous"）。
     */
    private String resolveUserId() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
