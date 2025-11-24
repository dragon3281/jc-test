package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 上传记录VO（按批次聚合）
 */
@Data
public class UploadRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String importBatch;
    private String country;
    private String dataType;
    private String fileName;
    private Long itemCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
}
