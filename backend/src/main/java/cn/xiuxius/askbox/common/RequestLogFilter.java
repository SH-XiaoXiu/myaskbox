package cn.xiuxius.askbox.common;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cn.dev33.satoken.stp.StpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 请求日志过滤器：为每个 HTTP 请求绑定 traceId/actorUserId 到 MDC，
 * 请求完成后输出一行管道格式日志。
 * <p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.nanoTime();

        // Trace ID
        String traceId = resolveTraceId(request);
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        // IP
        String ip = resolveClientIp(request);

        // User
        String userId = resolveUserId();
        if (!"-".equals(userId)) {
            MDC.put("userId", userId);
        }

        // User-Agent
        String ua = request.getHeader("User-Agent");

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info(
                    "HTTP请求完成 | method={} uri={} queryString={} status={} durationMs={} traceId={} actorUserId={} ip={} ua={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString() != null ? request.getQueryString() : "",
                    response.getStatus(),
                    durationMs,
                    traceId,
                    userId,
                    ip,
                    ua != null ? ua.substring(0, Math.min(ua.length(), 100)) : "-");
            MDC.remove("traceId");
            MDC.remove("userId");
        }
    }

    /** 解析 traceId：优先从请求头获取，否则生成 UUID。 */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader("X-Trace-Id");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    /** 获取当前登录用户 ID 字符串，未登录时返回 "-"。 */
    private String resolveUserId() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return "-";
        }
    }

    /** 解析客户端 IP */
    public static String resolveClientIp(HttpServletRequest request) {
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
}
