package com.detection.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP请求包解析器
 * 支持从浏览器开发者工具复制的原始HTTP请求包自动解析
 * 
 * 使用示例：
 * POST /api/register HTTP/1.1
 * Host: example.com
 * Authorization: Bearer {{token}}
 * Content-Type: application/json
 * 
 * {"mobile":"{{phone}}","username":"test"}
 */
@Slf4j
@Service
public class RequestPackageParser {

    /**
     * 解析原始HTTP请求包
     * 
     * @param rawRequest 原始HTTP请求文本
     * @return 解析结果（包含URL、Headers、Body、变量等）
     */
    public ParseResult parseRawRequest(String rawRequest) {
        try {
            log.info("开始解析HTTP请求包，长度: {}", rawRequest.length());
            
            // 1. 分割请求头和请求体
            String[] parts = splitHeaderAndBody(rawRequest);
            String headerPart = parts[0];
            String bodyPart = parts.length > 1 ? parts[1] : "";
            
            // 2. 解析请求行
            String[] lines = headerPart.split("\n");
            RequestLine requestLine = parseRequestLine(lines[0]);
            
            // 3. 解析请求头
            Map<String, String> headers = parseHeaders(lines);
            String host = headers.getOrDefault("Host", "localhost");
            
            // 4. 构建完整URL
            String url = buildUrl(host, requestLine.path, headers);
            
            // 5. 智能识别变量
            List<VariableInfo> variables = extractVariables(headers, bodyPart);
            
            // 6. 创建解析结果
            ParseResult result = new ParseResult();
            result.setUrl(url);
            result.setMethod(requestLine.method);
            result.setHeaders(headers);
            result.setRequestBody(bodyPart);
            result.setVariables(variables);
            result.setSuccess(true);
            
            log.info("请求包解析成功 - URL: {}, Method: {}, Headers: {}, Variables: {}", 
                     url, requestLine.method, headers.size(), variables.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("解析HTTP请求包失败", e);
            ParseResult result = new ParseResult();
            result.setSuccess(false);
            result.setErrorMessage("解析失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 分割请求头和请求体
     */
    private String[] splitHeaderAndBody(String rawRequest) {
        // 尝试多种分隔符
        if (rawRequest.contains("\n\n")) {
            return rawRequest.split("\n\n", 2);
        } else if (rawRequest.contains("\r\n\r\n")) {
            return rawRequest.split("\r\n\r\n", 2);
        } else {
            // 没有请求体
            return new String[]{rawRequest};
        }
    }
    
    /**
     * 解析请求行
     */
    private RequestLine parseRequestLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("无效的请求行: " + line);
        }
        
        RequestLine requestLine = new RequestLine();
        requestLine.method = parts[0];
        requestLine.path = parts[1];
        requestLine.protocol = parts.length > 2 ? parts[2] : "HTTP/1.1";
        
        return requestLine;
    }
    
    /**
     * 解析请求头
     */
    private Map<String, String> parseHeaders(String[] lines) {
        Map<String, String> headers = new LinkedHashMap<>();
        
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }
        
        return headers;
    }
    
    /**
     * 构建完整URL
     */
    private String buildUrl(String host, String path, Map<String, String> headers) {
        // 确保path以/开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        // 去除path中的协议部分（如果有）
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        
        // 判断使用http还是https
        String protocol = "https";
        if (host.contains(":80") || host.equals("localhost") || host.startsWith("127.0.0.1")) {
            protocol = "http";
        }
        
        return protocol + "://" + host + path;
    }
    
    /**
     * 智能识别变量
     * 从Headers和Body中识别占位符变量
     */
    private List<VariableInfo> extractVariables(Map<String, String> headers, String body) {
        List<VariableInfo> variables = new ArrayList<>();
        Set<String> foundVars = new HashSet<>();
        
        // 1. 从Headers中查找 {{变量名}} 格式的占位符
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String value = entry.getValue();
            List<String> vars = extractPlaceholders(value);
            for (String var : vars) {
                if (!foundVars.contains(var)) {
                    VariableInfo info = new VariableInfo();
                    info.setName(var);
                    info.setLocation("header:" + entry.getKey());
                    info.setSuggestedType(guessVariableType(var));
                    variables.add(info);
                    foundVars.add(var);
                }
            }
        }
        
        // 2. 从Body中查找占位符
        List<String> bodyVars = extractPlaceholders(body);
        for (String var : bodyVars) {
            if (!foundVars.contains(var)) {
                VariableInfo info = new VariableInfo();
                info.setName(var);
                info.setLocation("body");
                info.setSuggestedType(guessVariableType(var));
                
                // 尝试从JSON中提取字段名
                String fieldName = extractFieldNameFromJson(body, var);
                if (fieldName != null) {
                    info.setFieldName(fieldName);
                }
                
                variables.add(info);
                foundVars.add(var);
            }
        }
        
        // 3. 如果没有找到变量，尝试智能识别常见字段
        if (variables.isEmpty() && body != null && !body.isEmpty()) {
            variables.addAll(smartDetectVariables(body));
        }
        
        return variables;
    }
    
    /**
     * 提取占位符 {{变量名}}
     */
    private List<String> extractPlaceholders(String text) {
        List<String> placeholders = new ArrayList<>();
        if (text == null) return placeholders;
        
        // 匹配 {{xxx}} 格式
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            placeholders.add(matcher.group(1).trim());
        }
        
        return placeholders;
    }
    
    /**
     * 从JSON中提取字段名
     */
    private String extractFieldNameFromJson(String json, String placeholder) {
        try {
            // 查找 "xxx":"{{placeholder}}" 的模式
            String pattern = "\"([^\"]+)\"\\s*:\\s*\"\\{\\{" + Pattern.quote(placeholder) + "\\}\\}\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);
            
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.debug("提取字段名失败", e);
        }
        return null;
    }
    
    /**
     * 智能检测变量（针对没有占位符的情况）
     */
    private List<VariableInfo> smartDetectVariables(String body) {
        List<VariableInfo> variables = new ArrayList<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(body);
            
            // 检测常见字段
            String[] phoneFields = {"mobile", "phone", "telephone", "cell", "phoneNumber"};
            String[] tokenFields = {"token", "authorization", "auth", "accessToken"};
            
            for (String field : phoneFields) {
                if (json.has(field)) {
                    VariableInfo info = new VariableInfo();
                    info.setName("phone");
                    info.setFieldName(field);
                    info.setLocation("body");
                    info.setSuggestedType("手机号");
                    info.setExample(json.get(field).asText());
                    variables.add(info);
                    break;
                }
            }
            
            for (String field : tokenFields) {
                if (json.has(field)) {
                    VariableInfo info = new VariableInfo();
                    info.setName("token");
                    info.setFieldName(field);
                    info.setLocation("body");
                    info.setSuggestedType("令牌");
                    info.setExample(json.get(field).asText());
                    variables.add(info);
                    break;
                }
            }
            
        } catch (Exception e) {
            log.debug("智能检测变量失败", e);
        }
        
        return variables;
    }
    
    /**
     * 猜测变量类型
     */
    private String guessVariableType(String varName) {
        String lowerName = varName.toLowerCase();
        
        if (lowerName.contains("phone") || lowerName.contains("mobile") || 
            lowerName.contains("tel") || lowerName.contains("cell")) {
            return "手机号";
        } else if (lowerName.contains("token") || lowerName.contains("auth") || 
                   lowerName.contains("bearer")) {
            return "令牌";
        } else if (lowerName.contains("user") || lowerName.contains("account") || 
                   lowerName.contains("name")) {
            return "用户名";
        } else if (lowerName.contains("pass") || lowerName.contains("pwd")) {
            return "密码";
        } else {
            return "自定义";
        }
    }
    
    // ========== 内部类 ==========
    
    /**
     * 请求行
     */
    private static class RequestLine {
        String method;
        String path;
        String protocol;
    }
    
    /**
     * 解析结果
     */
    public static class ParseResult {
        private boolean success;
        private String errorMessage;
        private String url;
        private String method;
        private Map<String, String> headers;
        private String requestBody;
        private List<VariableInfo> variables;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public String getRequestBody() { return requestBody; }
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
        
        public List<VariableInfo> getVariables() { return variables; }
        public void setVariables(List<VariableInfo> variables) { this.variables = variables; }
    }
    
    /**
     * 变量信息
     */
    public static class VariableInfo {
        private String name;           // 变量名
        private String fieldName;      // JSON字段名
        private String location;       // 位置（header:xxx 或 body）
        private String suggestedType;  // 建议类型
        private String example;        // 示例值
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getSuggestedType() { return suggestedType; }
        public void setSuggestedType(String suggestedType) { this.suggestedType = suggestedType; }
        
        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }
    }
}
