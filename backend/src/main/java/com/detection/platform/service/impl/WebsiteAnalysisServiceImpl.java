package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.exception.BusinessException;
import com.detection.platform.dao.WebsiteAnalysisMapper;
import com.detection.platform.entity.PostTemplate;
import com.detection.platform.entity.WebsiteAnalysis;
import com.detection.platform.service.PostTemplateService;
import com.detection.platform.service.SmartWebAnalyzer;
import com.detection.platform.service.WebsiteAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final SmartWebAnalyzer smartWebAnalyzer;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Page<WebsiteAnalysis> pageAnalysis(Integer current, Integer size, String websiteUrl, Integer status) {
        Page<WebsiteAnalysis> page = new Page<>(current, size);
        LambdaQueryWrapper<WebsiteAnalysis> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(websiteUrl)) {
            wrapper.like(WebsiteAnalysis::getWebsiteUrl, websiteUrl);
        }
        if (status != null) {
            wrapper.eq(WebsiteAnalysis::getAnalysisStatus, status);
        }
        
        wrapper.orderByDesc(WebsiteAnalysis::getCreateTime);
        return websiteAnalysisMapper.selectPage(page, wrapper);
    }

    @Override
    public WebsiteAnalysis getAnalysisById(Long id) {
        log.info("[Service] 查询分析记录, ID={}", id);
        WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(id);
        if (analysis == null) {
            log.error("[Service] 分析记录不存在, ID={}", id);
            throw new BusinessException("分析记录不存在");
        }
        log.info("[Service] 查询到分析记录: ID={}, websiteUrl={}, analysisType={}, status={}, createTime={}",
                 analysis.getId(), 
                 analysis.getWebsiteUrl(), 
                 analysis.getAnalysisType(),
                 analysis.getAnalysisStatus(),
                 analysis.getCreateTime());
        return analysis;
    }

    @Override
    public Long startAnalysis(Map<String, Object> params) {
        // 创建分析记录
        WebsiteAnalysis analysis = new WebsiteAnalysis();
        analysis.setAnalysisType("NUMBER_CHECK");
        analysis.setWebsiteUrl((String) params.get("websiteUrl"));
        analysis.setAnalysisStatus(1); // 分析中
        analysis.setCreateTime(LocalDateTime.now());
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
                log.info("[AsyncAnalysis-{}] ======== 开始异步分析 ========", analysisId);
                WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(analysisId);
                String websiteUrl = analysis.getWebsiteUrl();
                log.info("[AsyncAnalysis-{}] 开始智能分析网站: {}", analysisId, websiteUrl);
                
                // 使用智能分析器
                Map<String, Object> analysisResult = smartWebAnalyzer.analyzeWebsite(websiteUrl);
                
                log.info("[AsyncAnalysis-{}] 智能分析器返回结果: success={}", analysisId, analysisResult.get("success"));
                
                if (Boolean.TRUE.equals(analysisResult.get("success"))) {
                    // 更新分析结果
                    analysis.setAnalysisStatus(2); // 已完成
                    
                    // 提取并保存注册接口信息
                    if (analysisResult.containsKey("registerApi")) {
                        analysis.setRegisterApi((String) analysisResult.get("registerApi"));
                        log.info("[AsyncAnalysis-{}] 注册接口: {}", analysisId, analysisResult.get("registerApi"));
                    }
                    if (analysisResult.containsKey("method")) {
                        analysis.setRegisterMethod((String) analysisResult.get("method"));
                        log.info("[AsyncAnalysis-{}] 请求方法: {}", analysisId, analysisResult.get("method"));
                    } else {
                        analysis.setRegisterMethod("POST");
                    }
                    
                    // 保存加密类型
                    if (analysisResult.containsKey("encryptionType")) {
                        analysis.setEncryptionType((String) analysisResult.get("encryptionType"));
                        log.info("[AsyncAnalysis-{}] 加密类型: {}", analysisId, analysisResult.get("encryptionType"));
                    }
                    
                    // 保存RSA密钥接口
                    if (analysisResult.containsKey("rsaKeyApi")) {
                        analysis.setRsaKeyApi((String) analysisResult.get("rsaKeyApi"));
                    }
                    
                    // 保存请求头
                    if (analysisResult.containsKey("requestHeaders")) {
                        analysis.setRequestHeaders((String) analysisResult.get("requestHeaders"));
                    }
                    
                    // 保存必填字段
                    if (analysisResult.containsKey("requiredFields")) {
                        analysis.setRequiredFields(objectMapper.writeValueAsString(analysisResult.get("requiredFields")));
                    }
                    
                    // 保存完整分析结果
                    String fullResultJson = objectMapper.writeValueAsString(analysisResult);
                    analysis.setAnalysisResult(fullResultJson);
                    analysis.setCompleteTime(LocalDateTime.now());
                    
                    log.info("[AsyncAnalysis-{}] 网站分析完成: {}, 注册接口: {}, 加密方式: {}", 
                            analysisId, websiteUrl, analysis.getRegisterApi(), analysis.getEncryptionType());
                    log.info("[AsyncAnalysis-{}] 完整分析结果JSON长度: {}", analysisId, fullResultJson.length());
                    
                    // 推送完成状态到前端
                    pushStatusToFrontend(analysisId, 2, "分析完成", analysisResult);
                } else {
                    analysis.setAnalysisStatus(3); // 失败
                    analysis.setErrorMessage((String) analysisResult.get("message"));
                    log.error("[AsyncAnalysis-{}] 分析失败: {}", analysisId, analysisResult.get("message"));
                    
                    // 推送失败状态到前端
                    pushStatusToFrontend(analysisId, 3, (String) analysisResult.get("message"), null);
                }
                
                websiteAnalysisMapper.updateById(analysis);
                log.info("[AsyncAnalysis-{}] ======== 分析结果已更新到数据库 ========", analysisId);
                
            } catch (Exception e) {
                log.error("[AsyncAnalysis-{}] 网站分析执行失败", analysisId, e);
                WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(analysisId);
                analysis.setAnalysisStatus(3); // 失败
                analysis.setErrorMessage(e.getMessage());
                websiteAnalysisMapper.updateById(analysis);
                
                // 推送异常状态到前端
                pushStatusToFrontend(analysisId, 3, "分析异常: " + e.getMessage(), null);
            }
        }, "Analysis-" + analysisId).start();
    }

    @Override
    public Long generateTemplate(Long analysisId) {
        WebsiteAnalysis analysis = getAnalysisById(analysisId);
        
        if (analysis.getAnalysisStatus() != 2) {
            throw new BusinessException("分析未完成,无法生成模板");
        }

        // 基于分析结果直接创建实体并保存
        PostTemplate template = new PostTemplate();
        template.setTemplateName("自动生成-" + analysis.getWebsiteUrl());
        template.setTargetSite(analysis.getWebsiteUrl());
        template.setRequestUrl(analysis.getWebsiteUrl() + "/api/check");
        template.setRequestMethod("POST");
        template.setRequestHeaders("{\"Content-Type\":\"application/json\"}");
        template.setRequestBody("{\"mobile\":\"{{phone}}\"}");
        template.setDuplicateMsg("customer_mobile_no_duplicated");
        template.setResponseCode(400);
        // 默认变量配置
        template.setVariableConfig("[{\"key\":\"header.Authorization\",\"location\":\"header\",\"name\":\"Authorization\",\"type\":\"token\"},{\"key\":\"body.mobile\",\"location\":\"body\",\"name\":\"mobile\",\"type\":\"mobile\"}]");
        
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

    @Override
    public Page<WebsiteAnalysis> listRegisterAnalysis(Integer current, Integer size, String websiteUrl, Integer status) {
        Page<WebsiteAnalysis> page = new Page<>(current, size);
        LambdaQueryWrapper<WebsiteAnalysis> wrapper = new LambdaQueryWrapper<>();
        if (org.springframework.util.StringUtils.hasText(websiteUrl)) {
            wrapper.like(WebsiteAnalysis::getWebsiteUrl, websiteUrl);
        }
        if (status != null) {
            wrapper.eq(WebsiteAnalysis::getAnalysisStatus, status);
        }
        wrapper.orderByDesc(WebsiteAnalysis::getCreateTime);
        return websiteAnalysisMapper.selectPage(page, wrapper);
    }

    @Override
    public Long startRegisterAnalysis(String websiteUrl) {
        log.info("[Service] ======== 启动自动化注册分析 ======== websiteUrl={}", websiteUrl);
        WebsiteAnalysis analysis = new WebsiteAnalysis();
        analysis.setAnalysisType("AUTO_REGISTER");
        analysis.setWebsiteUrl(websiteUrl);
        analysis.setAnalysisStatus(1);
        analysis.setCreateTime(LocalDateTime.now());
        websiteAnalysisMapper.insert(analysis);
        log.info("[Service] 分析记录已创建, ID={}, websiteUrl={}, analysisType=AUTO_REGISTER", analysis.getId(), websiteUrl);
        // 复用现有异步分析线程
        executeAnalysisAsync(analysis.getId());
        log.info("[Service] ======== 异步分析线程已启动 ========");
        return analysis.getId();
    }

    @Override
    public java.util.Map<String, Object> getRegisterAnalysisResult(Long id) {
        WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(id);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (analysis == null || analysis.getAnalysisStatus() != 2) {
            map.put("ready", false);
            return map;
        }
        
        map.put("ready", true);
        map.put("websiteUrl", analysis.getWebsiteUrl());
        
        // 从分析结果中提取数据
        map.put("registerApi", analysis.getRegisterApi() != null ? analysis.getRegisterApi() : "/user/register");
        map.put("method", analysis.getRegisterMethod() != null ? analysis.getRegisterMethod() : "POST");
        map.put("encryptionType", analysis.getEncryptionType() != null ? analysis.getEncryptionType() : "NONE");
        map.put("rsaKeyApi", analysis.getRsaKeyApi() != null ? analysis.getRsaKeyApi() : "");
        
        // 解析完整的分析结果获取更多字段
        try {
            if (analysis.getAnalysisResult() != null) {
                java.util.Map<String, Object> fullResult = objectMapper.readValue(
                    analysis.getAnalysisResult(), 
                    java.util.Map.class
                );
                
                // 提取字段映射
                map.put("usernameField", fullResult.getOrDefault("usernameField", "username"));
                map.put("passwordField", fullResult.getOrDefault("passwordField", "password"));
                map.put("emailField", fullResult.getOrDefault("emailField", ""));
                map.put("phoneField", fullResult.getOrDefault("phoneField", ""));
                map.put("encryptionHeader", fullResult.getOrDefault("encryptionHeader", ""));
                map.put("valueFieldName", fullResult.getOrDefault("valueFieldName", "value"));
                
                // 提取必填字段
                if (fullResult.containsKey("requiredFields")) {
                    map.put("requiredFields", fullResult.get("requiredFields"));
                }
            } else {
                // 使用默认值
                map.put("usernameField", "username");
                map.put("passwordField", "password");
                map.put("emailField", "");
                map.put("phoneField", "");
                map.put("encryptionHeader", "");
                map.put("valueFieldName", "value");
            }
        } catch (Exception e) {
            log.error("解析分析结果失败", e);
            // 使用默认值
            map.put("usernameField", "username");
            map.put("passwordField", "password");
        }
        
        return map;
    }
    
    /**
     * 推送分析状态到前端
     */
    private void pushStatusToFrontend(Long analysisId, Integer status, String message, Map<String, Object> result) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "analysis_status");
            notification.put("analysisId", analysisId);
            notification.put("status", status);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            
            if (result != null) {
                notification.put("registerApi", result.get("registerApi"));
                notification.put("encryptionType", result.get("encryptionType"));
            }
            
            // 推送到WebSocket主题 /topic/analysis/{analysisId}
            messagingTemplate.convertAndSend("/topic/analysis/" + analysisId, notification);
            log.info("[WebSocket] 推送分析状态: analysisId={}, status={}, message={}", analysisId, status, message);
            
        } catch (Exception e) {
            log.error("[WebSocket] 推送分析状态失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public java.util.Map<String, Object> buildRegisterTaskConfig(Long analysisId) {
        log.info("[BuildTaskConfig] 根据分析ID={} 构建注册任务配置", analysisId);
        WebsiteAnalysis analysis = websiteAnalysisMapper.selectById(analysisId);
        if (analysis == null) {
            throw new BusinessException("分析记录不存在");
        }
        if (analysis.getAnalysisStatus() != 2) {
            throw new BusinessException("分析未完成或失败，无法生成任务配置");
        }
        
        Map<String, Object> config = new HashMap<>();
        
        // 基础配置
        String domain = analysis.getWebsiteUrl().replaceAll("https?://", "").replaceAll("/.*", "");
        config.put("taskName", "自动注册-" + domain + "-" + System.currentTimeMillis());
        config.put("websiteUrl", analysis.getWebsiteUrl());
        config.put("registerApi", analysis.getRegisterApi() != null ? analysis.getRegisterApi() : "/wps/member/register");
        config.put("method", analysis.getRegisterMethod() != null ? analysis.getRegisterMethod() : "PUT");
        
        // 字段映射（从分析结果解析或使用默认值）
        String usernameField = "username";
        String passwordField = "password";
        String encryptionHeader = "Encryption";
        String valueFieldName = "value";
        String rsaKeyApi = "/wps/session/key/rsa";
        String rsaTsParam = "t";
        String dupMsgSubstring = "Ang username na ito ay ginamit na ng ibang user";
        
        try {
            if (analysis.getAnalysisResult() != null) {
                Map<String, Object> fullResult = objectMapper.readValue(
                    analysis.getAnalysisResult(),
                    java.util.Map.class
                );
                usernameField = (String) fullResult.getOrDefault("usernameField", "username");
                passwordField = (String) fullResult.getOrDefault("passwordField", "password");
                encryptionHeader = (String) fullResult.getOrDefault("encryptionHeader", "Encryption");
                valueFieldName = (String) fullResult.getOrDefault("valueFieldName", "value");
                
                if (fullResult.containsKey("rsaKeyApi")) {
                    rsaKeyApi = (String) fullResult.get("rsaKeyApi");
                }
                if (fullResult.containsKey("rsaTsParam")) {
                    rsaTsParam = (String) fullResult.get("rsaTsParam");
                }
                if (fullResult.containsKey("dupMsgSubstring")) {
                    dupMsgSubstring = (String) fullResult.get("dupMsgSubstring");
                }
            }
        } catch (Exception e) {
            log.warn("[BuildTaskConfig] 解析分析结果失败，使用默认字段: {}", e.getMessage());
        }
        
        config.put("usernameField", usernameField);
        config.put("passwordField", passwordField);
        config.put("defaultPassword", "133adb");
        
        // 加密配置
        String encryptionType = analysis.getEncryptionType() != null ? analysis.getEncryptionType() : "NONE";
        config.put("encryptionType", encryptionType);
        config.put("encryptionHeader", encryptionHeader);
        config.put("valueFieldName", valueFieldName);
        config.put("dupMsgSubstring", dupMsgSubstring);
        
        if ("DES_RSA".equalsIgnoreCase(encryptionType)) {
            config.put("rsaKeyApi", rsaKeyApi);
            config.put("rsaTsParam", rsaTsParam);
        }
        
        // extraParams：根据ppvip或其他域名生成不同的参数集合
        Map<String, Object> extraParams = new HashMap<>();
        
        // headers: Device/Language/Merchant等
        Map<String, String> headers = new HashMap<>();
        headers.put("Device", "web");
        headers.put("Language", "BN");
        if (domain.contains("ppvip")) {
            headers.put("Merchant", "ppvipbdtf5");
        } else {
            headers.put("Merchant", "ck555bdtf3");
        }
        extraParams.put("headers", headers);
        
        // cookies: 默认SHELL_deviceId
        Map<String, String> cookies = new HashMap<>();
        cookies.put("SHELL_deviceId", java.util.UUID.randomUUID().toString());
        extraParams.put("cookies", cookies);
        
        // userAgent/referer
        extraParams.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36");
        extraParams.put("referer", analysis.getWebsiteUrl());
        
        // 业务字段（如果分析结果中有requiredFields，可以提前设置部分默认值）
        // 这里先给出ppvip/wwwtk系列网站的通用字段
        extraParams.put("affiliateCode", "www");
        extraParams.put("domain", "www-tk999");
        extraParams.put("login", true);
        extraParams.put("registerMethod", "WEB");
        
        try {
            config.put("extraParams", objectMapper.writeValueAsString(extraParams));
        } catch (Exception e) {
            log.error("[BuildTaskConfig] 序列化extraParams失败", e);
            config.put("extraParams", "{}");
        }
        
        // 执行配置默认值
        config.put("accountCount", 10);
        config.put("concurrency", 5);
        config.put("autoRetry", false);
        config.put("retryTimes", 0);
        config.put("useProxy", false);
        config.put("needPhone", false);
        config.put("needCaptcha", false);
        config.put("needToken", false);
        
        log.info("[BuildTaskConfig] 任务配置已生成: taskName={}, websiteUrl={}, registerApi={}, encryptionType={}",
                 config.get("taskName"), config.get("websiteUrl"), config.get("registerApi"), config.get("encryptionType"));
        log.info("[BuildTaskConfig] 加密配置: encryptionHeader={}, valueFieldName={}, rsaKeyApi={}",
                 config.get("encryptionHeader"), config.get("valueFieldName"), config.get("rsaKeyApi"));
        
        return config;
    }
}
