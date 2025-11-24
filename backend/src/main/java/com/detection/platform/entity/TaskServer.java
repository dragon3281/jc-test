package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务服务器关联实体类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Data
@TableName("t_task_server")
public class TaskServer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 服务器ID
     */
    private Long serverId;

    /**
     * 分配数据量
     */
    private Long assignedCount;

    /**
     * 已完成数量
     */
    private Long completedCount;

    /**
     * 执行状态:1执行中,2已完成,3失败
     */
    private Integer execStatus;
}
