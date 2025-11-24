package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务器VO - 用于返回给前端
 */
@Data
public class ServerVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 服务器ID
     */
    private Long id;
    
    /**
     * 服务器名称
     */
    private String serverName;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * SSH端口
     */
    private Integer sshPort;
    
    /**
     * SSH用户名
     */
    private String sshUsername;
    
    /**
     * 认证方式: 1-密码, 2-密钥
     */
    private Integer authType;
    
    /**
     * Docker端口
     */
    private Integer dockerPort;
    
    /**
     * 服务器状态: 1-在线, 2-离线, 3-异常
     */
    private Integer status;
    
    /**
     * 服务器状态文本
     */
    private String statusText;
    
    /**
     * CPU使用率 (%)
     */
    private BigDecimal cpuUsage;
    
    /**
     * 内存使用率 (%)
     */
    private BigDecimal memoryUsage;
    
    /**
     * 磁盘使用率 (%)
     */
    private BigDecimal diskUsage;
    
    /**
     * 网络入流量 (KB/s)
     */
    private Long networkIn;
    
    /**
     * 网络出流量 (KB/s)
     */
    private Long networkOut;
    
    /**
     * 最大并发任务数
     */
    private Integer maxConcurrent;
    
    /**
     * 当前运行任务数
     */
    private Integer currentTasks;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 最后心跳时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeatTime;
}
