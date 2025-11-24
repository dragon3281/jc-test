package com.detection.platform.executor;

import com.detection.platform.entity.*;
import com.detection.platform.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 检测执行器 - 核心检测逻辑
 * 负责实际执行账号检测任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionExecutor {

    private final PostTemplateService postTemplateService;
    private final ProxyNodeService proxyNodeService;
    private final DetectionResultService detectionResultService;
    private final ObjectMapper objectMapper;

    /**
     * 执行单个账号检测
     *
     * @param taskId 任务ID
     * @param templateId 模板ID
     * @param proxyPoolId 代理池ID(可选)
     * @param dataValue 待检测的账号值
     * @return 检测结果
     */
    public DetectionResult executeDetection(Long taskId, Long templateId, Long proxyPoolId, String dataValue) {
        log.info("开始检测账号: {}, 任务ID: {}, 模板ID: {}", dataValue, taskId, templateId);
        
        DetectionResult result = new DetectionResult();
        result.setTaskId(taskId);
        result.setAccountIdentifier(dataValue);
        result.setDetectTime(LocalDateTime.now());
        
        try {
            // 1. 获取POST模板
            PostTemplate template = postTemplateService.getById(templateId);
            if (template == null) {
                result.setDetectStatus(3); // 检测失败
                result.setErrorMessage("POST模板不存在");
                return result;
            }
            
            result.setTargetSite(template.getTargetSite());
            
            // 2. 获取代理(如果启用)
            ProxyNode proxy = null;
            if (template.getEnableProxy() == 1 && proxyPoolId != null) {
                proxy = proxyNodeService.allocateProxy(proxyPoolId);
                if (proxy != null) {
                    result.setUsedProxy(proxy.getProxyIp() + ":" + proxy.getProxyPort());
                }
            }
            
            // 3. 构建请求并发送
            long startTime = System.currentTimeMillis();
            String response = sendRequest(template, dataValue, proxy);
            long responseTime = System.currentTimeMillis() - startTime;
            result.setResponseTime((int) responseTime);
            
            // 4. 解析响应并判断结果
            Integer status = parseResponse(response, template);
            result.setDetectStatus(status);
            result.setResponseData(response);
            
            // 5. 更新代理统计
            if (proxy != null) {
                proxyNodeService.updateProxyStats(proxy.getId(), status == 1 || status == 2);
            }
            
            log.info("检测完成: {}, 状态: {}, 耗时: {}ms", dataValue, getStatusText(status), responseTime);
            
        } catch (IOException e) {
            log.error("检测失败: {}, 错误: {}", dataValue, e.getMessage());
            result.setDetectStatus(3); // 检测失败
            result.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("检测异常: {}, 错误: {}", dataValue, e.getMessage(), e);
            result.setDetectStatus(3); // 检测失败
            result.setErrorMessage("系统异常: " + e.getMessage());
        }
        
        // 6. 保存检测结果
        detectionResultService.save(result);
        
        return result;
    }

    /**
     * 发送HTTP请求
     */
    private String sendRequest(PostTemplate template, String dataValue, ProxyNode proxy) throws IOException {
        URL url = new URL(template.getRequestUrl());
        HttpURLConnection conn;
        
        // 设置代理
        if (proxy != null) {
            Proxy httpProxy = new Proxy(Proxy.Type.HTTP, 
                new InetSocketAddress(proxy.getProxyIp(), proxy.getProxyPort()));
            conn = (HttpURLConnection) url.openConnection(httpProxy);
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        
        // 设置请求方法
        conn.setRequestMethod(template.getRequestMethod());
        conn.setConnectTimeout(template.getTimeoutSeconds() * 1000);
        conn.setReadTimeout(template.getTimeoutSeconds() * 1000);
        conn.setDoOutput(true);
        
        // 设置请求头
        if (template.getRequestHeaders() != null) {
            JsonNode headers = objectMapper.readTree(template.getRequestHeaders());
            headers.fields().forEachRemaining(entry -> {
                String value = entry.getValue().asText();
                // 替换变量占位符
                value = value.replace("{{account}}", dataValue);
                value = value.replace("{{timestamp}}", String.valueOf(System.currentTimeMillis()));
                conn.setRequestProperty(entry.getKey(), value);
            });
        }
        
        // 设置请求体
        if ("POST".equalsIgnoreCase(template.getRequestMethod()) && template.getRequestBody() != null) {
            String requestBody = template.getRequestBody();
            // 替换变量占位符
            requestBody = requestBody.replace("{{account}}", dataValue);
            requestBody = requestBody.replace("{{timestamp}}", String.valueOf(System.currentTimeMillis()));
            
            conn.getOutputStream().write(requestBody.getBytes("UTF-8"));
            conn.getOutputStream().flush();
        }
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            byte[] responseBytes = conn.getInputStream().readAllBytes();
            return new String(responseBytes, "UTF-8");
        } else {
            throw new IOException("HTTP请求失败, 状态码: " + responseCode);
        }
    }

    /**
     * 解析响应并判断检测状态
     */
    private Integer parseResponse(String response, PostTemplate template) {
        try {
            // 解析成功判断规则
            if (template.getSuccessRule() != null) {
                JsonNode successRule = objectMapper.readTree(template.getSuccessRule());
                if (matchRule(response, successRule)) {
                    return 1; // 已注册
                }
            }
            
            // 解析失败判断规则
            if (template.getFailRule() != null) {
                JsonNode failRule = objectMapper.readTree(template.getFailRule());
                if (matchRule(response, failRule)) {
                    return 2; // 未注册
                }
            }
            
            // 无法判断
            return 3; // 检测失败
        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage());
            return 3; // 检测失败
        }
    }

    /**
     * 匹配规则
     */
    private boolean matchRule(String response, JsonNode rule) {
        try {
            JsonNode responseJson = objectMapper.readTree(response);
            
            // 遍历规则字段
            var fields = rule.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String key = field.getKey();
                String expectedValue = field.getValue().asText();
                
                // 获取响应中对应字段的值
                JsonNode actualNode = responseJson.get(key);
                if (actualNode == null) {
                    return false;
                }
                
                String actualValue = actualNode.asText();
                if (!expectedValue.equals(actualValue)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("匹配规则失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        switch (status) {
            case 1: return "已注册";
            case 2: return "未注册";
            case 3: return "检测失败";
            case 4: return "账号异常";
            case 5: return "代理异常";
            default: return "未知状态";
        }
    }
}
