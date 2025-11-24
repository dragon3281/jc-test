package com.detection.platform.common.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 成功响应(无数据)
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, System.currentTimeMillis());
    }

    /**
     * 成功响应(带数据)
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, System.currentTimeMillis());
    }

    /**
     * 成功响应(自定义消息)
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }

    /**
     * 成功响应(带消息，无数据)
     */
    public static <T> Result<T> successMsg(String message) {
        return new Result<>(200, message, null, System.currentTimeMillis());
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, System.currentTimeMillis());
    }

    /**
     * 失败响应(自定义错误码)
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    /**
     * 失败响应(带数据)
     */
    public static <T> Result<T> error(String message, T data) {
        return new Result<>(500, message, data, System.currentTimeMillis());
    }

    /**
     * 参数校验失败
     */
    public static <T> Result<T> validateFailed(String message) {
        return new Result<>(400, message, null, System.currentTimeMillis());
    }

    /**
     * 未授权
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null, System.currentTimeMillis());
    }

    /**
     * 禁止访问
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null, System.currentTimeMillis());
    }
}
