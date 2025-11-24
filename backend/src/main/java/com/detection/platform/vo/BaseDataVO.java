package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础数据VO
 */
@Data
public class BaseDataVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String dataValue;
    private String dataType;
    private String country;
    private String dataSource;
    private String importBatch;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime importTime;
}
