package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自动化注册任务实体类
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_register_task")
public class RegisterTask {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 目标网站
     */
    private String websiteUrl;

    /**
     * 注册接口
     */
    private String registerApi;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 用户名字段
     */
    private String usernameField;

    /**
     * 密码字段
     */
    private String passwordField;

    /**
     * 邮箱字段
     */
    private String emailField;

    /**
     * 手机号字段
     */
    private String phoneField;

    /**
     * 是否需要手机号
     */
    private Boolean needPhone;

    /**
     * 手动手机号（选填）
     */
    private String manualPhone;

    /**
     * 创建数量
     */
    private Integer accountCount;

    /**
     * 默认密码
     */
    private String defaultPassword;

    /**
     * 额外参数(JSON)
     */
    private String extraParams;

    /**
     * 是否需要验证码
     */
    private Boolean needCaptcha;

    /**
     * 验证码类型: 1-图形, 2-短信, 3-邮箱
     */
    private Integer captchaType;

    /**
     * 验证码接口
     */
    private String captchaApi;

    /**
     * 验证码字段
     */
    private String captchaField;

    /**
     * OCR识别方式: 1-OCR, 2-打码平台, 3-跳过
     */
    private Integer ocrMethod;

    /**
     * 是否需要Token
     */
    private Boolean needToken;

    /**
     * Token字段
     */
    private String tokenField;

    /**
     * Token来源: 1-Header, 2-Body, 3-Cookie
     */
    private Integer tokenSource;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 是否使用代理
     */
    private Boolean useProxy;

    /**
     * 代理池ID
     */
    private Long proxyPoolId;

    /**
     * 并发数
     */
    private Integer concurrency;

    /**
     * 自动重试
     */
    private Boolean autoRetry;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 加密类型: NONE-无加密, DES_RSA-DES+RSA双重加密
     */
    private String encryptionType;

    /**
     * RSA密钥接口
     */
    private String rsaKeyApi;

    /**
     * RSA时间戳参数名
     */
    private String rsaTsParam;

    /**
     * 加密请求头名称
     */
    private String encryptionHeader;

    /**
     * 数据包装字段名
     */
    private String valueFieldName;

    /**
     * 重复用户名提示(用于验证成功)
     */
    private String dupMsgSubstring;

    /**
     * 任务状态: 1-待执行, 2-执行中, 3-已完成, 4-已暂停, 5-失败
     */
    private Integer status;

    /**
     * 总数量
     */
    private Integer totalCount;

    /**
     * 已完成数量
     */
    private Integer completedCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
