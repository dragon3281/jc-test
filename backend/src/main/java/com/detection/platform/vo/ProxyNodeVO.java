package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理节点VO
 */
@Data
public class ProxyNodeVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 代理节点ID
     */
    private Long id;
    
    /**
     * 代理池ID
     */
    private Long poolId;
    
    /**
     * 代理IP
     */
    private String proxyIp;
    
    /**
     * 代理端口
     */
    private Integer proxyPort;
    
    /**
     * 是否需要认证: 0-否, 1-是
     */
    private Integer needAuth;
    
    /**
     * 认证用户名
     */
    private String username;
    
    /**
     * 状态: 1-可用, 2-不可用, 3-未检测
     */
    private Integer status;
    
    /**
     * 状态文本
     */
    private String statusText;
    
    /**
     * 响应时间 (ms)
     */
    private Integer responseTime;
    
    /**
     * 成功次数
     */
    private Integer successCount;
    
    /**
     * 失败次数
     */
    private Integer failCount;
    
    /**
     * 健康度评分 (0-100)
     */
    private BigDecimal healthScore;
    
    /**
     * 上次检测时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCheckTime;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
