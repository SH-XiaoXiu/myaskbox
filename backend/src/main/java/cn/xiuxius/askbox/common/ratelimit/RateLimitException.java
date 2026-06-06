package cn.xiuxius.askbox.common.ratelimit;

/**
 * 限流异常——请求被限流拦截时抛出。
 * <p>
 * 携带 {@code retryAfter}、{@code limit}、{@code remaining} 等附加信息，
 * 由 {@code GlobalExceptionHandler} 统一返回 HTTP 429。
 */
public class RateLimitException extends RuntimeException {

    private final int retryAfter;
    private final int limit;
    private final long remaining;

    public RateLimitException(int retryAfter, int limit, long remaining) {
        super("请求过于频繁，请稍后再试");
        this.retryAfter = retryAfter;
        this.limit = limit;
        this.remaining = Math.max(0, remaining);
    }

    public int getRetryAfter() {
        return retryAfter;
    }

    public int getLimit() {
        return limit;
    }

    public long getRemaining() {
        return remaining;
    }
}
