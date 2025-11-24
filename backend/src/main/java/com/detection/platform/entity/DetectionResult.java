package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 检测结果实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_detection_result")
public class DetectionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 结果ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 序号
     */
    private Integer seqNo;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 账号标识
     */
    private String accountIdentifier;

    /**
     * 数据类型（用户自定义）
     */
    private String dataType;

    /**
     * 国家
     */
    private String country;

    /**
     * 目标站
     */
    private String targetSite;

    /**
     * 状态:1已注册,2未注册,3检测失败,4账号异常,5代理异常
     */
    private Integer detectStatus;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;

    /**
     * 使用的代理
     */
    private String usedProxy;

    /**
     * 代理IP
     */
    private String proxyIp;

    /**
     * 代理端口
     */
    private Integer proxyPort;

    /**
     * 执行服务器IP
     */
    private String execServer;

    /**
     * 响应数据
     */
    private String responseData;

    /**
     * 响应详情(JSON格式)
     */
    private String responseDetail;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 检测时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime detectTime;

    /**
     * 是否已归档:0否,1是
     */
    private Integer isArchived;
}
