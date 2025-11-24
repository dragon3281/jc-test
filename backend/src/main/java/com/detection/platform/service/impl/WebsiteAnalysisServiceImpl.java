package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.exception.BusinessException;
import com.detection.platform.dao.WebsiteAnalysisMapper;
import com.detection.platform.entity.PostTemplate;
import com.detection.platform.entity.WebsiteAnalysis;
import com.detection.platform.service.PostTemplateService;
import com.detection.platform.service.WebsiteAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 网站分析Service实现
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebsiteAnalysisServiceImpl implements WebsiteAnalysisService {

    private final WebsiteAnalysisMapper websiteAnalysisMapper;
    private final PostTemplateService postTemplateService;
    private final ObjectMapper objectMapper;

    @Override
    public Page<WebsiteAnalysis> pageAnalysis(Integer current, Integer size, String websiteUrl, Integer status) {
        Page<WebsiteAnalysis> page = new Page<>(current, size);
        LambdaQueryWrapper<WebsiteAnalysis> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(websiteUrl)) {
            wrapper.like(WebsiteAnalysis::getWebsiteUrl, websiteUrl);
        }
        if (status != null) {
            wrapper.eq(WebsiteAnalysis::getStatus, status);
        }
        
        wrapper.orderByDesc(WebsiteAnalysis::getCreateTime);
        return websiteAnalysisMapper.selectPage(page, wrapper);
    }

    @Override
    public WebsiteAnalysis getAnalysisById(Long id) {
        WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(id);
        if (analysis == null) {
            throw new BusinessException("分析记录不存在");
        }
        return analysis;
    }

    @Override
    public Long startAnalysis(Map<String, Object> params) {
        // 创建分析记录
        WebsiteAnalysis analysis = new WebsiteAnalysis();
        analysis.setWebsiteUrl((String) params.get("websiteUrl"));
        analysis.setPorts((String) params.get("ports"));
        analysis.setApiPaths((String) params.get("apiPaths"));
        analysis.setTimeout((Integer) params.get("timeout"));
        analysis.setUseProxy((Boolean) params.get("useProxy"));
        
        if (params.get("proxyPoolId") != null) {
            analysis.setProxyPoolId(Long.valueOf(params.get("proxyPoolId").toString()));
        }
        
        analysis.setStatus(1); // 分析中
        websiteAnalysisMapper.insert(analysis);

        // 异步执行分析任务
        executeAnalysisAsync(analysis.getId());
        
        return analysis.getId();
    }

    /**
     * 异步执行网站分析
     */
    private void executeAnalysisAsync(Long analysisId) {
        new Thread(() -> {
            try {
                WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(analysisId);
                
                // 模拟分析过程
                Thread.sleep(5000);
                
                // 更新分析结果
                analysis.setStatus(2); // 已完成
                analysis.setDetectedPort("443");
                analysis.setApiType(1); // JSON
                analysis.setAnalysisResult("{\"接口\":[\"/api/check\",\"/user/register\"],\"方法\":\"POST\",\"需要Token\":true}");
                analysis.setDetectedApis("[{\"path\":\"/api/check\",\"method\":\"POST\",\"contentType\":\"application/json\",\"requireToken\":true}]");
                analysis.setFinishTime(LocalDateTime.now());
                
                websiteAnalysisMapper.updateById(analysis);
                
            } catch (Exception e) {
                log.error("网站分析执行失败", e);
                WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(analysisId);
                analysis.setStatus(3); // 失败
                analysis.setErrorMessage(e.getMessage());
                websiteAnalysisMapper.updateById(analysis);
            }
        }, "Analysis-" + analysisId).start();
    }

    @Override
    public Long generateTemplate(Long analysisId) {
        WebsiteAnalysis analysis = getAnalysisById(analysisId);
        
        if (analysis.getStatus() != 2) {
            throw new BusinessException("分析未完成,无法生成模板");
        }

        // 基于分析结果直接创建实体并保存
        PostTemplate template = new PostTemplate();
        template.setTemplateName("自动生成-" + analysis.getWebsiteUrl());
        template.setTargetSite(analysis.getWebsiteUrl());
        template.setRequestUrl(analysis.getWebsiteUrl() + "/api/check");
        template.setRequestMethod("POST");
        template.setRequestHeaders("{\"Content-Type\":\"application/json\"}");
        template.setRequestBody("{\"account\":\"{{account}}\"}");
        template.setSuccessRule("{\"code\":200}");
        template.setFailRule("{\"code\":400}");
        template.setEnableProxy(analysis.getUseProxy() ? 1 : 0);
        template.setTimeoutSeconds(30);
        template.setRetryCount(3);
        template.setVersion("1.0");
        
        // 直接保存到数据库
        boolean saved = postTemplateService.save(template);
        if (!saved) {
            throw new BusinessException("生成模板失败");
        }
        
        log.info("基于分析记录ID:{} 生成POST模板成功, 模板ID: {}", analysisId, template.getId());
        return template.getId();
    }

    @Override
    public Boolean deleteAnalysis(Long id) {
        int rows = websiteAnalysisMapper.deleteById(id);
        return rows > 0;
    }
}
