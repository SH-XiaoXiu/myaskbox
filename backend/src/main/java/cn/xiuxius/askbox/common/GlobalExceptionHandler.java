package cn.xiuxius.askbox.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.xiuxius.askbox.common.ratelimit.RateLimitException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex) {
        log.warn("BizException: code={} message={}", ex.getErrorCode().code(), ex.getMessage());
        return ResponseEntity.status(resolveHttpStatus(ex.getErrorCode().code()))
                .body(R.fail(ex.getErrorCode().code(), ex.getMessage()));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<R<Map<String, Object>>> handleRateLimit(RateLimitException ex) {
        log.warn(
                "RateLimit exceeded: retryAfter={}s limit={} remaining={}",
                ex.getRetryAfter(),
                ex.getLimit(),
                ex.getRemaining());
        Map<String, Object> data = Map.of(
                "retryAfter", ex.getRetryAfter(),
                "limit", ex.getLimit(),
                "remaining", ex.getRemaining());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(R.fail(ErrorCodes.RATE_LIMITED.code(), ErrorCodes.RATE_LIMITED.message(), data));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String detail = fieldError != null ? fieldError.getField() + ": " + fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("Validation failed: {}", detail);
        return ResponseEntity.badRequest().body(R.fail(ErrorCodes.VALIDATION_ERROR.code(), detail));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String detail = fieldError != null ? fieldError.getField() + ": " + fieldError.getDefaultMessage() : "参数绑定失败";
        log.warn("Bind failed: {}", detail);
        return ResponseEntity.badRequest().body(R.fail(ErrorCodes.VALIDATION_ERROR.code(), detail));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String detail = "缺少参数：" + ex.getParameterName();
        log.warn("Missing parameter: {}", detail);
        return ResponseEntity.badRequest().body(R.fail(ErrorCodes.VALIDATION_ERROR.code(), detail));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "参数类型错误：" + ex.getName();
        log.warn("Type mismatch: {}", detail);
        return ResponseEntity.badRequest().body(R.fail(ErrorCodes.VALIDATION_ERROR.code(), detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Request body unreadable: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(R.fail(ErrorCodes.VALIDATION_ERROR.code(), "请求体格式错误"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(ErrorCodes.VALIDATION_ERROR.code(), "请求方法不允许"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource not found: method={} path={}", ex.getHttpMethod(), ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(R.fail(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<R<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("Handler not found: method={} path={}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(R.fail(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLogin(NotLoginException ex) {
        log.warn("NotLogin: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail(ErrorCodes.NOT_LOGIN));
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<R<Void>> handleNotPermission(NotPermissionException ex) {
        log.warn("NotPermission: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(ErrorCodes.FORBIDDEN));
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<R<Void>> handleNotRole(NotRoleException ex) {
        log.warn("NotRole: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(ErrorCodes.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.fail(ErrorCodes.INTERNAL_ERROR));
    }

    private HttpStatus resolveHttpStatus(int code) {
        return switch (code / 1000) {
            case 10 -> {
                if (code == 10001) yield HttpStatus.BAD_REQUEST;
                if (code == 10002 || code == 10004) yield HttpStatus.NOT_FOUND;
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
            case 11 -> code == ErrorCodes.FORBIDDEN.code() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
            case 20 -> {
                if (code == 20001 || code == 20003 || code == 20004 || code == 20005) yield HttpStatus.NOT_FOUND;
                yield HttpStatus.BAD_REQUEST;
            }
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
