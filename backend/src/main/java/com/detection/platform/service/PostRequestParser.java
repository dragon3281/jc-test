package com.detection.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * POST请求解析器
 * 自动解析HTTP原始请求，提取URL、请求头、请求体等信息
 */
@Slf4j
@Service
public class PostRequestParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析原始POST请求
     * 
     * @param rawRequest 原始HTTP请求文本
     * @return 解析结果Map
     */
    public Map<String, Object> parseRawRequest(String rawRequest) {
        log.info("[RequestParser] 开始解析原始POST请求");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 按行分割
            String[] lines = rawRequest.split("\\r?\\n");
            
            // 1. 解析请求行 (POST /path HTTP/1.1)
            String requestLine = lines[0].trim();
            String[] requestParts = requestLine.split("\\s+");
            String method = requestParts[0];
            String path = requestParts.length > 1 ? requestParts[1] : "/";
            
            result.put("method", method);
            result.put("path", path);
            
            // 2. 解析请求头
            Map<String, String> headers = new HashMap<>();
            String host = null;
            int bodyStartIndex = 0;
            
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // 空行表示请求头结束
                if (line.isEmpty()) {
                    bodyStartIndex = i + 1;
                    break;
                }
                
                // 解析请求头 (Key: Value)
                int colonIndex = line.indexOf(":");
                if (colonIndex > 0) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    headers.put(key, value);
                    
                    if ("Host".equalsIgnoreCase(key)) {
                        host = value;
                    }
                }
            }
            
            result.put("headers", headers);
            
            // 3. 构建完整URL
            if (host == null || host.isEmpty()) {
                log.warn("[RequestParser] 未找到Host请求头，无法构建完整URL");
                throw new RuntimeException("请求中缺少Host请求头，无法解析URL");
            }
            
            String protocol = "https"; // 默认HTTPS
            String fullUrl = protocol + "://" + host + path;
            result.put("url", fullUrl);
            result.put("host", host);
            result.put("targetSite", host);
            
            // 4. 解析请求体
            if (bodyStartIndex < lines.length) {
                StringBuilder bodyBuilder = new StringBuilder();
                for (int i = bodyStartIndex; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (!line.isEmpty() && !line.equals("​")) { // 过滤零宽字符
                        bodyBuilder.append(line);
                    }
                }
                String body = bodyBuilder.toString();
                
                if (!body.isEmpty()) {
                    result.put("body", body);
                    
                    // 尝试解析JSON
                    try {
                        Map<String, Object> bodyJson = objectMapper.readValue(body, Map.class);
                        result.put("bodyJson", bodyJson);
                    } catch (Exception e) {
                        log.warn("[RequestParser] 请求体不是有效的JSON格式");
                    }
                }
            }
            
            // 5. 识别关键变量
            identifyVariables(result);
            
            log.info("[RequestParser] 解析完成: URL={}, Headers={}, Body={}", 
                     fullUrl, headers.size(), result.containsKey("body"));
            
        } catch (Exception e) {
            log.error("[RequestParser] 解析失败", e);
            throw new RuntimeException("解析POST请求失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 识别所有可能的变量参数（请求头和请求体）
     * 返回参数列表供用户选择
     */
    private void identifyVariables(Map<String, Object> result) {
        Map<String, String> headers = (Map<String, String>) result.get("headers");
        Map<String, Object> allParams = new HashMap<>();
        Map<String, Object> suggestedVariables = new HashMap<>();
        
        // 收集所有请求头参数
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 跳过固定的请求头
            if (isFixedHeader(key)) {
                continue;
            }
            
            Map<String, String> paramInfo = new HashMap<>();
            paramInfo.put("location", "header");
            paramInfo.put("name", key);
            paramInfo.put("value", value);
            paramInfo.put("type", guessParamType(key, value));
            allParams.put("header." + key, paramInfo);
            
            // 自动识别常见的Token字段
            if (key.equalsIgnoreCase("Authorization") || 
                key.equalsIgnoreCase("Token") || 
                key.equalsIgnoreCase("X-Token")) {
                suggestedVariables.put("header." + key, paramInfo);
            }
        }
        
        // 收集请求体参数
        if (result.containsKey("bodyJson")) {
            Map<String, Object> bodyJson = (Map<String, Object>) result.get("bodyJson");
            
            for (Map.Entry<String, Object> entry : bodyJson.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                Map<String, Object> paramInfo = new HashMap<>();
                paramInfo.put("location", "body");
                paramInfo.put("name", key);
                paramInfo.put("value", value);
                paramInfo.put("type", guessParamType(key, String.valueOf(value)));
                allParams.put("body." + key, paramInfo);
                
                // 自动识别常见的手机号字段
                if (key.toLowerCase().contains("mobile") || 
                    key.toLowerCase().contains("phone")) {
                    suggestedVariables.put("body." + key, paramInfo);
                }
            }
        }
        
        result.put("allParams", allParams);
        result.put("suggestedVariables", suggestedVariables);
    }
    
    /**
     * 判断是否为固定请求头（不应作为变量）
     */
    private boolean isFixedHeader(String headerName) {
        String[] fixedHeaders = {
            "Host", "Content-Type", "Content-Length", "User-Agent",
            "Accept", "Accept-Encoding", "Accept-Language",
            "Sec-Ch-Ua", "Sec-Ch-Ua-Mobile", "Sec-Ch-Ua-Platform",
            "Sec-Fetch-Site", "Sec-Fetch-Mode", "Sec-Fetch-Dest",
            "Origin", "Referer", "X-Requested-With", "Priority"
        };
        
        for (String fixed : fixedHeaders) {
            if (fixed.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 根据字段名和值推测参数类型
     */
    private String guessParamType(String name, String value) {
        String lowerName = name.toLowerCase();
        
        if (lowerName.contains("token") || lowerName.contains("authorization")) {
            return "token";
        }
        if (lowerName.contains("mobile") || lowerName.contains("phone")) {
            return "mobile";
        }
        if (lowerName.contains("cookie")) {
            return "cookie";
        }
        if (lowerName.contains("timestamp") || lowerName.contains("time")) {
            return "timestamp";
        }
        
        return "text";
    }
    
    /**
     * 将解析结果转换为模板格式
     * 
     * @param parseResult 解析结果
     * @param templateName 模板名称
     * @param selectedVariables 用户选择的变量列表 ["header.Authorization", "body.mobile"]
     * @return 模板数据
     */
    public Map<String, Object> convertToTemplate(Map<String, Object> parseResult, 
                                                  String templateName, 
                                                  java.util.List<String> selectedVariables) {
        Map<String, Object> template = new HashMap<>();
        
        // 基本信息
        template.put("templateName", templateName);
        template.put("targetSite", parseResult.get("host"));
        template.put("requestUrl", parseResult.get("url"));
        template.put("requestMethod", parseResult.get("method"));
        
        // 获取所有参数
        Map<String, Object> allParams = (Map<String, Object>) parseResult.get("allParams");
        if (allParams == null) {
            allParams = new HashMap<>();
        }
        
        // 构建变量配置
        java.util.List<Map<String, String>> variableConfigs = new java.util.ArrayList<>();
        
        // 请求头（将选中的变量替换为占位符）
        Map<String, String> originalHeaders = (Map<String, String>) parseResult.get("headers");
        Map<String, String> headers = originalHeaders != null ? new HashMap<>(originalHeaders) : new HashMap<>();
        
        for (String varKey : selectedVariables) {
            if (!allParams.containsKey(varKey)) {
                continue;
            }
            
            Map<String, Object> paramInfo = (Map<String, Object>) allParams.get(varKey);
            String location = (String) paramInfo.get("location");
            String paramName = (String) paramInfo.get("name");
            String paramType = (String) paramInfo.get("type");
            
            // 记录变量配置
            Map<String, String> varConfig = new HashMap<>();
            varConfig.put("key", varKey);
            varConfig.put("location", location);
            varConfig.put("name", paramName);
            varConfig.put("type", paramType);
            varConfig.put("placeholder", "{{" + paramName + "}}");
            variableConfigs.add(varConfig);
            
            // 替换请求头中的变量
            if ("header".equals(location)) {
                headers.put(paramName, "{{" + paramName + "}}");
            }
        }
        
        template.put("requestHeaders", convertToJsonString(headers));
        
        // 请求体（将选中的变量替换为占位符）
        if (parseResult.containsKey("bodyJson")) {
            Map<String, Object> body = new HashMap<>((Map<String, Object>) parseResult.get("bodyJson"));
            
            for (String varKey : selectedVariables) {
                if (!allParams.containsKey(varKey)) {
                    continue;
                }
                
                Map<String, Object> paramInfo = (Map<String, Object>) allParams.get(varKey);
                String location = (String) paramInfo.get("location");
                String paramName = (String) paramInfo.get("name");
                
                // 替换请求体中的变量
                if ("body".equals(location)) {
                    body.put(paramName, "{{" + paramName + "}}");
                }
            }
            
            template.put("requestBody", convertToJsonString(body));
        }
        
        // 变量配置（JSON格式保存）
        template.put("variableConfig", convertToJsonString(variableConfigs));
        template.put("variableConfigs", variableConfigs); // 保留原始数据供前端使用
        
        // 默认判断规则
        template.put("duplicateMsg", ""); // 需要用户手动输入
        template.put("responseCode", 400);
        
        return template;
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    private String convertToJsonString(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
