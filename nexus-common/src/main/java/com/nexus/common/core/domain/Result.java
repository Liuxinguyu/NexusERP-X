package com.nexus.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(IResultCode code) {
        return build(code.getCode(), code.getMessage(), null);
    }

    public static <T> Result<T> fail(IResultCode code, String message) {
        return build(code.getCode(), message, null);
    }

    public static <T> Result<T> fail(String message) {
        return build(ResultCode.FAILURE.getCode(), message, null);
    }

    public static <T> Result<T> badRequest(String message) {
        return build(ResultCode.BAD_REQUEST.getCode(), message, null);
    }

    public static <T> Result<T> error(String message) {
        return build(ResultCode.INTERNAL_ERROR.getCode(), message, null);
    }

    public static <T> Result<T> build(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }
}

