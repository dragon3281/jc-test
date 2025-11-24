package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础数据实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_base_data")
public class BaseData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 数据值
     */
    private String dataValue;

    /**
     * 数据类型（用户自定义：账号/密码/token等）
     */
    private String dataType;

    /**
     * 国家
     */
    private String country;

    /**
     * 账号标识（兼容字段）
     */
    private String accountIdentifier;

    /**
     * 账号类型:1邮箱,2手机,3用户名（兼容字段）
     */
    private Integer accountType;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 导入批次
     */
    private String importBatch;

    /**
     * 导入时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime importTime;

    /**
     * 备注
     */
    private String remark; // 用于存储上传文件名

    /**
     * 逻辑删除:0未删除,1已删除
     */
    @TableLogic
    private Integer deleted;
}

