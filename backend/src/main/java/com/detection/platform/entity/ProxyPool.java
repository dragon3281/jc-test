package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理池实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_proxy_pool")
public class ProxyPool implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 代理池ID
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 代理类型:1HTTP,2HTTPS,3SOCKS5
     */
    private Integer proxyType;

    /**
     * 认证方式:0无,1用户名密码
     */
    private Integer authType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码(加密)
     */
    private String password;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态:1可用,2不可用,3未检测
     */
    private Integer status;

    /**
     * 健康度0-100
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
    private LocalDateTime lastCheckTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除:0未删除,1已删除
     */
    @TableLogic
    private Integer deleted;
}

