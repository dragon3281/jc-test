package com.detection.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务器DTO - 用于添加和编辑服务器
 */
@Data
public class ServerDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 服务器ID (编辑时需要)
     */
    private Long id;
    
    /**
     * 服务器名称
     */
    @NotBlank(message = "服务器名称不能为空")
    @Size(max = 100, message = "服务器名称长度不能超过100")
    private String serverName;
    
    /**
     * IP地址
     */
    @NotBlank(message = "IP地址不能为空")
    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$", 
             message = "IP地址格式不正确")
    private String ipAddress;
    
    /**
     * SSH端口
     */
    @NotNull(message = "SSH端口不能为空")
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号必须小于65536")
    private Integer sshPort;
    
    /**
     * SSH用户名
     */
    @NotBlank(message = "SSH用户名不能为空")
    @Size(max = 50, message = "SSH用户名长度不能超过50")
    private String sshUsername;
    
    /**
     * 认证方式: 1-密码, 2-密钥
     */
    @NotNull(message = "认证方式不能为空")
    @Min(value = 1, message = "认证方式必须为1或2")
    @Max(value = 2, message = "认证方式必须为1或2")
    private Integer authType;
    
    /**
     * 认证凭证 (密码或密钥内容,需要加密存储)
     */
    @NotBlank(message = "认证凭证不能为空")
    private String authCredential;
    
    /**
     * Docker端口
     */
    @Min(value = 1, message = "Docker端口必须大于0")
    @Max(value = 65535, message = "Docker端口必须小于65536")
    private Integer dockerPort;
    
    /**
     * 最大并发任务数
     */
    @Min(value = 1, message = "最大并发数必须大于0")
    private Integer maxConcurrent;
}
