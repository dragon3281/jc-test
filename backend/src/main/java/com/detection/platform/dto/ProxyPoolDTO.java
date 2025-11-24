package com.detection.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理池DTO
 */
@Data
public class ProxyPoolDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 代理池ID (编辑时需要)
     */
    private Long id;
    
    /**
     * 代理池名称
     */
    @NotBlank(message = "代理池名称不能为空")
    @Size(max = 100, message = "代理池名称长度不能超过100")
    private String poolName;
    
    /**
     * 代理IP地址
     */
    @NotBlank(message = "代理IP地址不能为空")
    @Size(max = 50, message = "IP地址长度不能超过50")
    private String proxyIp;
    
    /**
     * 代理端口
     */
    @NotNull(message = "代理端口不能为空")
    @Min(value = 1, message = "端口号必须在1-65535之间")
    @Max(value = 65535, message = "端口号必须在1-65535之间")
    private Integer proxyPort;
    
    /**
     * 代理类型: 1-HTTP, 2-HTTPS, 3-SOCKS5
     */
    @NotNull(message = "代理类型不能为空")
    @Min(value = 1, message = "代理类型必须为1-3")
    @Max(value = 3, message = "代理类型必须为1-3")
    private Integer proxyType;
    
    /**
     * 是否需要认证: 0-无, 1-用户名密码
     */
    private Integer needAuth;
    
    /**
     * 用户名
     */
    @Size(max = 100, message = "用户名长度不能超过100")
    private String username;
    
    /**
     * 密码
     */
    @Size(max = 255, message = "密码长度不能超过255")
    private String password;
    
    /**
     * 描述
     */
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;
}
