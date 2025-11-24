package com.detection.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应VO
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /**
     * Token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserVO user;
}
