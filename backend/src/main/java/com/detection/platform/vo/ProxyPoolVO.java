package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理池VO
 */
@Data
public class ProxyPoolVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 代理池ID
     */
    private Long id;
    
    /**
     * 代理池名称
     */
    private String poolName;
    
    /**
     * 代理IP地址
     */
    private String proxyIp;
    
    /**
     * 代理端口
     */
    private Integer proxyPort;
    
    /**
     * 代理类型: 1-HTTP, 2-HTTPS, 3-SOCKS5
     */
    private Integer proxyType;
    
    /**
     * 代理类型文本
     */
    private String proxyTypeText;
    
    /**
     * 认证方式: 0-无, 1-用户名密码
     */
    private Integer authType;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 状态: 1-可用, 2-不可用, 3-未检测
     */
    private Integer status;
    
    /**
     * 健康度 (0-100)
     */
    private Integer healthScore;
    
    /**
     * 使用次数
     */
    private Long useCount;
    
    /**
     * 成功次数
     */
    private Long successCount;
    
    /**
     * 失败次数
     */
    private Long failCount;
    
    /**
     * 最近响应时间(毫秒)
     */
    private Integer responseTime;
    
    /**
     * 平均响应时间(毫秒)
     */
    private Integer avgResponseTime;
    
    /**
     * 最后检测时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCheckTime;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
