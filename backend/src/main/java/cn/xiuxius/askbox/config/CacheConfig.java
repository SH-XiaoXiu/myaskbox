package cn.xiuxius.askbox.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CacheConfig implements CachingConfigurer {

    public static final String CACHE_KEY_PREFIX = "askbox:cache:v3:";

    public static final String CACHE_PUBLISHED = "published";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = defaultCacheConfig(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CACHE_PUBLISHED, defaultCacheConfig(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    private RedisCacheConfiguration defaultCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .prefixCacheNameWith(CACHE_KEY_PREFIX)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn(
                        "Cache get failed, fallback to data source: cache={} key={} error={}",
                        cache.getName(),
                        key,
                        exception.getClass().getSimpleName());
                evictQuietly(cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn(
                        "Cache put failed, response will continue: cache={} key={} error={}",
                        cache.getName(),
                        key,
                        exception.getClass().getSimpleName());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn(
                        "Cache evict failed, response will continue: cache={} key={} error={}",
                        cache.getName(),
                        key,
                        exception.getClass().getSimpleName());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn(
                        "Cache clear failed, response will continue: cache={} error={}",
                        cache.getName(),
                        exception.getClass().getSimpleName());
            }

            private void evictQuietly(Cache cache, Object key) {
                try {
                    cache.evict(key);
                } catch (RuntimeException evictException) {
                    log.warn(
                            "Cache eviction after get failure also failed: cache={} key={} error={}",
                            cache.getName(),
                            key,
                            evictException.getClass().getSimpleName());
                }
            }
        };
    }
}
