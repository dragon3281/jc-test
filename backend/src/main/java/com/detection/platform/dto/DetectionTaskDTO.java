package com.detection.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 检测任务DTO
 */
@Data
public class DetectionTaskDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100")
    private String taskName;
    
    @NotNull(message = "POST模板ID不能为空")
    private Long templateId;
    
    @NotNull(message = "代理池ID不能为空")
    private Long poolId;
    
    @NotNull(message = "服务器列表不能为空")
    @Size(min = 1, message = "至少选择一台服务器")
    private List<Long> serverIds;
    
    @Min(value = 1, message = "并发数必须大于0")
    private Integer concurrentNum;
    
    private Integer priority;
    
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;
}
