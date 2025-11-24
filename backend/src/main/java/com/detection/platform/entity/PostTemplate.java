package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * POST模板实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_post_template")
public class PostTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 目标站
     */
    private String targetSite;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求头模板(JSON格式)
     */
    private String requestHeaders;

    /**
     * 请求体模板(JSON格式)
     */
    private String requestBody;

    /**
     * 成功判断规则(JSON格式)
     */
    private String successRule;

    /**
     * 失败判断规则(JSON格式)
     */
    private String failRule;

    /**
     * 是否启用代理:0否,1是
     */
    private Integer enableProxy;

    /**
     * 超时时间(秒)
     */
    private Integer timeoutSeconds;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 版本号
     */
    private String version;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除:0未删除,1已删除
     */
    @TableLogic
    private Integer deleted;
}

