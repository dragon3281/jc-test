package com.detection.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * POST模板VO
 */
@Data
public class PostTemplateVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String templateName;
    private String targetSite;
    private String requestUrl;
    private String requestMethod;
    private String requestHeaders;
    private String requestBody;
    private String successRule;
    private String failRule;
    private Integer enableProxy;
    
    @JsonProperty("timeout")  // 前端使用timeout字段
    private Integer timeoutSeconds;
    
    private Integer retryCount;
    private String version;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
