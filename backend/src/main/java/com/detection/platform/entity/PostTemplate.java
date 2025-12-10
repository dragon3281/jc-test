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
     * 变量配置(JSON数组格式)
     * 示例: [{"key":"header.Authorization","location":"header","name":"Authorization","type":"token","placeholder":"{{Authorization}}"}]
     */
    private String variableConfig;

    /**
     * 成功判断规则(JSON格式)
     */
    private String successRule;

    /**
     * 失败判断规则(JSON格式)
     */
    private String failRule;

    /**
     * 重复手机号关键字(如: customer_mobile_no_duplicated)
     */
    private String duplicateMsg;

    /**
     * 重复时的HTTP状态码(默认: 400)
     */
    private Integer responseCode;

    /**
     * 限流关键字(如: TOO_MANY_REQUEST)
     */
    private String rateLimitKeyword;

    /**
     * 连续限流触发次数阈值(默认: 5)
     */
    private Integer maxConsecutiveRateLimit;

    /**
     * 触发限流后暂停秒数(默认: 2)
     */
    private Integer backoffSeconds;

    /**
     * 最小并发数(默认: 1)
     */
    private Integer minConcurrency;

    /**
     * 最大并发数(默认: 50)
     */
    private Integer maxConcurrency;

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

