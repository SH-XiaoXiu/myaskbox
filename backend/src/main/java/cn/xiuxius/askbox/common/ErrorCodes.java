package cn.xiuxius.askbox.common;

public enum ErrorCodes implements BizErrorCode {
    // 10xxx 通用
    VALIDATION_ERROR(10001, "参数校验失败"),
    RESOURCE_NOT_FOUND(10002, "资源不存在"),
    INTERNAL_ERROR(10003, "服务器内部错误"),
    RATE_LIMITED(10004, "请求过于频繁，请稍后再试"),

    // 11xxx 认证
    NOT_LOGIN(11001, "未登录"),
    BAD_CREDENTIALS(11002, "用户名或密码错误"),
    FORBIDDEN(11003, "权限不足"),

    // 20xxx 业务
    QUESTION_NOT_FOUND(20001, "问题不存在"),
    QUESTION_STATUS_INVALID(20002, "问题状态不允许此操作"),
    BOX_NOT_FOUND(20003, "提问箱不存在"),
    AVATAR_NOT_FOUND(20004, "头像不存在"),
    USER_NOT_FOUND(20005, "用户不存在");

    private final int code;
    private final String message;

    ErrorCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
