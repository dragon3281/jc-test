package com.detection.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 检测任务明细实体
 */
@Data
@TableName("t_detection_task_item")
public class DetectionTaskItem {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taskId;
    
    private Long templateId;
    
    private String phone;
    
    private String tokenUsed;
    
    private Integer responseCode;
    
    private String responseBody;
    
    private Integer isDuplicate;
    
    private String status; // PENDING, SUCCESS, ERROR
    
    private String errorMessage;
    
    private Integer isRateLimited;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
