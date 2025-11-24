package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.entity.WebsiteAnalysis;

import java.util.Map;

/**
 * 网站分析Service接口
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
public interface WebsiteAnalysisService {

    /**
     * 分页查询网站分析列表
     */
    Page<WebsiteAnalysis> pageAnalysis(Integer current, Integer size, String websiteUrl, Integer status);

    /**
     * 根据ID获取分析详情
     */
    WebsiteAnalysis getAnalysisById(Long id);

    /**
     * 启动网站分析
     */
    Long startAnalysis(Map<String, Object> params);

    /**
     * 根据分析结果生成POST模板
     */
    Long generateTemplate(Long analysisId);

    /**
     * 删除分析记录
     */
    Boolean deleteAnalysis(Long id);
}
