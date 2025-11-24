package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理节点实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_proxy_node")
public class ProxyNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 代理ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属代理池ID
     */
    private Long poolId;

    /**
     * 代理地址(IP:端口)
     */
    private String proxyAddress;

    /**
     * 代理IP
     */
    @TableField(exist = false)
    private String proxyIp;

    /**
     * 代理端口
     */
    @TableField(exist = false)
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
     * 加密密码
     */
    private String password;

    /**
     * 地区
     */
    private String region;

    /**
     * 运营商
     */
    private String isp;

    /**
     * 状态:1可用,2不可用,3检测中
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
     * 响应时间(毫秒)
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

