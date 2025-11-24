package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 检测结果VO
 */
@Data
public class DetectionResultVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long taskId;
    private String taskName;
    private Long dataId;
    private String dataValue;
    private Integer status;
    private String statusText;
    private String responseData;
    private Integer responseTime;
    private String errorMsg;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectTime;
    
    // 统计字段
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
}
