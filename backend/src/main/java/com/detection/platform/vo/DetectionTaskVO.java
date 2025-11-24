package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检测任务VO
 */
@Data
public class DetectionTaskVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String taskName;
    private Long templateId;
    private String templateName;
    private Long poolId;
    private String poolName;
    private Integer status;
    private String statusText;
    private Integer totalCount;
    private Integer completedCount;
    private Integer successCount;
    private Integer failCount;
    private BigDecimal progress;
    private Integer concurrentNum;
    private Integer priority;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
