package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网站分析实体类
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_website_analysis")
public class WebsiteAnalysis {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 网站地址
     */
    private String websiteUrl;

    /**
     * 检测端口(多个用逗号分隔)
     */
    private String ports;

    /**
     * 检测到的端口
     */
    private String detectedPort;

    /**
     * 接口路径列表(JSON数组)
     */
    private String apiPaths;

    /**
     * 接口类型: 1-JSON, 2-XML, 3-HTML
     */
    private Integer apiType;

    /**
     * 分析状态: 1-分析中, 2-已完成, 3-失败
     */
    private Integer status;

    /**
     * 超时时间(秒)
     */
    private Integer timeout;

    /**
     * 是否使用代理
     */
    private Boolean useProxy;

    /**
     * 代理池ID
     */
    private Long proxyPoolId;

    /**
     * 检测到的接口列表(JSON)
     */
    private String detectedApis;

    /**
     * 分析结果(JSON)
     */
    private String analysisResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
