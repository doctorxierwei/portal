package com.portal.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 返回结果
 */
@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    public static final int SUCCESS = 200;
    public static final int FAIL = 500;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(SUCCESS, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, "success", data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(SUCCESS, msg, data);
    }

    public static <T> R<T> fail() {
        return new R<>(FAIL, "error", null);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(FAIL, msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }
}
