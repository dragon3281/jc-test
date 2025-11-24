package com.detection.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理节点DTO
 */
@Data
public class ProxyNodeDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 代理节点ID (编辑时需要)
     */
    private Long id;
    
    /**
     * 代理池ID
     */
    @NotNull(message = "代理池ID不能为空")
    private Long poolId;
    
    /**
     * 代理IP
     */
    @NotBlank(message = "代理IP不能为空")
    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$", 
             message = "IP地址格式不正确")
    private String proxyIp;
    
    /**
     * 代理端口
     */
    @NotNull(message = "代理端口不能为空")
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号必须小于65536")
    private Integer proxyPort;
    
    /**
     * 代理类型: 1-HTTP, 2-HTTPS, 3-SOCKS5
     */
    @NotNull(message = "代理类型不能为空")
    @Min(value = 1, message = "代理类型必须为1-3")
    @Max(value = 3, message = "代理类型必须为1-3")
    private Integer proxyType;
    
    /**
     * 是否需要认证: 0-否, 1-是
     */
    private Integer needAuth;
    
    /**
     * 认证用户名
     */
    @Size(max = 100, message = "用户名长度不能超过100")
    private String username;
    
    /**
     * 认证密码(需要加密存储)
     */
    @Size(max = 200, message = "密码长度不能超过200")
    private String password;

    /**
     * 地区
     */
    @Size(max = 100, message = "地区长度不能超过100")
    private String region;

    /**
     * 运营商
     */
    @Size(max = 100, message = "运营商长度不能超过100")
    private String isp;
}
