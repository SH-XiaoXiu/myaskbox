package cn.xiuxius.askbox.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.MDC;

/** 统一 API 响应体。 */
public record R<T>(int code, String message, T data, OffsetDateTime timestamp, String traceId) {

    public static <T> R<T> ok(T data) {
        return new R<>(200, "ok", data, OffsetDateTime.now(ZoneOffset.UTC), MDC.get("traceId"));
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(BizErrorCode errorCode) {
        return fail(errorCode.code(), errorCode.message());
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, OffsetDateTime.now(ZoneOffset.UTC), MDC.get("traceId"));
    }

    public static <T> R<T> fail(int code, String message, T data) {
        return new R<>(code, message, data, OffsetDateTime.now(ZoneOffset.UTC), MDC.get("traceId"));
    }
}
