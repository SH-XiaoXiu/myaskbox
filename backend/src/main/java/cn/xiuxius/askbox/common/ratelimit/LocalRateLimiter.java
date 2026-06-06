package cn.xiuxius.askbox.common.ratelimit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import cn.xiuxius.askbox.common.ratelimit.RateLimitProperties.RuleConfig;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * 基于 Caffeine + Bucket4j 的本地内存限流器。
 * <p>
 * 每个限流 key 对应一个 Bucket，缓存在 Caffeine 中（最大 20,000 条目，10 分钟无访问过期）。
 */
public class LocalRateLimiter implements RateLimiter {

    private final Cache<String, Bucket> bucketCache;

    public LocalRateLimiter() {
        this.bucketCache = Caffeine.newBuilder()
                .maximumSize(20_000L)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public boolean tryConsume(String key, RuleConfig config) {
        Bucket bucket = bucketCache.get(key, k -> createBucket(config));
        return bucket.tryConsume(1);
    }

    @Override
    public long getAvailableTokens(String key) {
        Bucket bucket = bucketCache.getIfPresent(key);
        return bucket != null ? bucket.getAvailableTokens() : -1;
    }

    private Bucket createBucket(RuleConfig config) {
        Bandwidth limit = Bandwidth.classic(
                config.getCapacity(),
                Refill.greedy(config.getRefillTokens(), Duration.ofSeconds(config.getTimeWindowSeconds())));
        return Bucket.builder().addLimit(limit).build();
    }
}
