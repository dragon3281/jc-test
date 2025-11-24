package com.detection.platform.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录DTO
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
public class LoginDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
