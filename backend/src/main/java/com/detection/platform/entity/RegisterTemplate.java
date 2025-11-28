package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 注册模板实体
 */
@Data
@TableName("t_register_template")
public class RegisterTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateName;
    private String websiteUrl;
    private String registerApi;
    private String method;

    private String usernameField;
    private String passwordField;
    private String defaultPassword;
    private String extraParams;

    private String encryptionType;
    private String rsaKeyApi;
    private String rsaTsParam;
    private String encryptionHeader;
    private String valueFieldName;

    private String notes; // 关键逻辑备注（例如RSA需老旧JS库）

    private LocalDateTime createTime;
}
