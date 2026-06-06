package cn.xiuxius.askbox.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final BizErrorCode errorCode;

    public BizException(BizErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BizException(BizErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
