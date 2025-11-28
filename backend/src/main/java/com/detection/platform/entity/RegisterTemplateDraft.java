package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_register_template_draft")
public class RegisterTemplateDraft {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String draftName;
    private String websiteUrl;
    private String registerApi;
    private String method; // POST/PUT

    private String usernameField;
    private String passwordField;
    private String defaultPassword;
    private String extraParams; // JSON字符串

    private String encryptionType; // 实际检测到的加密类型
    private Long executorId; // 关联执行器
    private String executorScript; // 自定义脚本内容

    private String rsaKeyApi;
    private String rsaTsParam;
    private String encryptionHeader;
    private String valueFieldName;

    private Integer testResult; // 0未测试,1成功,2失败
    private String testToken;
    private String testError;

    private String autoNotes;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
