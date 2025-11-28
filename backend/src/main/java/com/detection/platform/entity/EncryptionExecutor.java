package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_encryption_executor")
public class EncryptionExecutor {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String executorName;
    private String executorType; // DES_RSA_OLD_JS, DES_RSA_STANDARD, AES_RSA, MD5, CUSTOM, NONE
    private String scriptLanguage; // PYTHON, JAVASCRIPT, JAVA
    private String scriptPath;
    private String scriptContent;
    private String encryptionConfig; // JSON 字符串
    private Integer isBuiltin; // 0 自定义, 1 内置
    private String description;
    private String version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
