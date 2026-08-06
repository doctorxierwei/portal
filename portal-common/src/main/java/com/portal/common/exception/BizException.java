package com.portal.common.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
