package com.detection.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;

/**
 * POST模板DTO
 */
@Data
public class PostTemplateDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 模板ID (编辑时需要)
     */
    private Long id;
    
    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String templateName;
    
    /**
     * 目标网站URL
     */
    @NotBlank(message = "目标网站URL不能为空")
    @Size(max = 500, message = "URL长度不能超过500")
    private String targetUrl;
    
    /**
     * 请求方法: GET, POST等
     */
    @NotBlank(message = "请求方法不能为空")
    @Size(max = 10, message = "请求方法长度不能超过10")
    private String requestMethod;
    
    /**
     * 请求头(JSON格式)
     */
    private String headers;
    
    /**
     * 请求体模板
     */
    private String bodyTemplate;
    
    /**
     * 成功标识(JSON格式)
     */
    private String successFlag;
    
    /**
     * 失败标识(JSON格式)
     */
    private String failFlag;
    
    /**
     * 描述
     */
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;
}
