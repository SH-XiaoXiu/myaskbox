package cn.xiuxius.askbox.common.ratelimit;

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cn.xiuxius.askbox.common.ratelimit.RateLimitProperties.RuleConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AOP 切面：拦截标注了 {@link RateLimit} 的方法，执行令牌桶检查。
 * <p>
 * 对注解中的每种限流类型独立检查（AND 逻辑），所有类型均未超限才放行。
 * 达到限流上限时抛出 {@link RateLimitException}（除非 bypass 模式）。
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final RateLimitKeyGenerator keyGenerator;

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(cn.xiuxius.askbox.common.ratelimit.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        String endpoint = buildEndpoint();
        String spelValue = resolveSpelKey(annotation.key(), joinPoint, method);

        for (RateLimitType type : annotation.types()) {
            RuleConfig config = resolveConfig(annotation, type);
            String key = keyGenerator.generate(type, endpoint, spelValue);

            if (!rateLimiter.tryConsume(key, config)) {
                long remaining = rateLimiter.getAvailableTokens(key);
                log.warn(
                        "Rate limit exceeded: type={} endpoint={} key={} capacity={} window={}s remaining={}",
                        type,
                        endpoint,
                        key,
                        config.getCapacity(),
                        config.getTimeWindowSeconds(),
                        remaining);

                if (properties.isBypass()) {
                    log.info("Rate limit bypass enabled, allowing request through");
                } else {
                    throw new RateLimitException(config.getTimeWindowSeconds(), config.getCapacity(), remaining);
                }
            }
        }

        return joinPoint.proceed();
    }

    /** 构造 endpoint 字符串：{@code HTTP_METHOD:/path}。 */
    private String buildEndpoint() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "UNKNOWN";
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getMethod() + ":" + request.getRequestURI();
    }

    /** 解析 SpEL 表达式（如 {@code #request.username}）。 */
    private String resolveSpelKey(String spel, ProceedingJoinPoint joinPoint, Method method) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        try {
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            Expression expression = SPEL_PARSER.parseExpression(spel);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to resolve SpEL expression '{}': {}", spel, e.getMessage());
            return spel;
        }
    }

    /** 解析最终使用的限流规则：注解值优先，未指定则回退到配置默认值。 */
    private RuleConfig resolveConfig(RateLimit annotation, RateLimitType type) {
        RuleConfig defaults = properties.getDefaultForType(type);
        if (annotation.capacity() <= 0) {
            return defaults;
        }
        RuleConfig overridden = new RuleConfig();
        overridden.setCapacity(annotation.capacity());
        overridden.setRefillTokens(annotation.refillTokens() > 0 ? annotation.refillTokens() : annotation.capacity());
        overridden.setTimeWindowSeconds(
                annotation.timeWindowSeconds() > 0 ? annotation.timeWindowSeconds() : defaults.getTimeWindowSeconds());
        return overridden;
    }
}
