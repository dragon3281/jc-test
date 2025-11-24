package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务器实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_server")
public class Server implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 服务器ID
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 认证方式:1密码,2密钥
     */
    private Integer authType;

    /**
     * 加密后的凭证
     */
    private String authCredential;

    /**
     * Docker API端口
     */
    private Integer dockerPort;

    /**
     * 状态:1在线,2离线,3异常
     */
    private Integer status;

    /**
     * CPU使用率
     */
    private BigDecimal cpuUsage;

    /**
     * 内存使用率
     */
    private BigDecimal memoryUsage;

    /**
     * 磁盘使用率
     */
    private BigDecimal diskUsage;

    /**
     * 网络入流量(KB/s)
     */
    private Long networkIn;

    /**
     * 网络出流量(KB/s)
     */
    private Long networkOut;

    /**
     * 最大并发数
     */
    private Integer maxConcurrent;

    /**
     * 当前任务数
     */
    private Integer currentTasks;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeatTime;

    /**
     * 逻辑删除:0未删除,1已删除
     */
    @TableLogic
    private Integer deleted;
}

