package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检测任务实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_detection_task")
public class DetectionTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 目标站
     */
    private String targetSite;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 代理池ID
     */
    private Long proxyPoolId;

    /**
     * 代理池ID(别名)
     */
    private Long poolId;

    /**
     * 状态:1待执行,2执行中,3已暂停,4已完成,5失败,6已停止
     */
    private Integer taskStatus;

    /**
     * 任务状态
     */
    private Integer status;

    /**
     * 任务进度
     */
    private BigDecimal progress;

    /**
     * 优先级:1高,2中,3低
     */
    private Integer priority;

    /**
     * 总数据量
     */
    private Long totalCount;

    /**
     * 已完成数量
     */
    private Long completedCount;

    /**
     * 成功数量
     */
    private Long successCount;

    /**
     * 失败数量
     */
    private Long failCount;

    /**
     * 并发数
     */
    private Integer concurrentNum;

    /**
     * 进度百分比
     */
    private BigDecimal progressPercent;

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
     * 预计剩余秒数
     */
    private Long estimateRemainingSeconds;

    /**
     * 逻辑删除:0未删除,1已删除
     */
    @TableLogic
    private Integer deleted;
}

