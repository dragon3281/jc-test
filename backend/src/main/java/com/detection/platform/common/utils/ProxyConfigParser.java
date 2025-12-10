package com.detection.platform.common.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代理配置解析工具类
 * 支持解析多种代理协议格式
 */
@Slf4j
public class ProxyConfigParser {
    
    /**
     * 代理配置信息
     */
    @Data
    public static class ProxyConfig {
        private String protocol;      // 协议: http, https, socks, socks5
        private String host;          // 主机地址
        private Integer port;         // 端口
        private String username;      // 用户名
        private String password;      // 密码
        private String label;         // 标签/备注
        
        public Integer getProxyType() {
            if (protocol == null) {
                return 1; // 默认HTTP
            }
            return switch (protocol.toLowerCase()) {
                case "http" -> 1;
                case "https" -> 2;
                case "socks", "socks5" -> 3;
                default -> 1;
            };
        }
        
        public boolean hasAuth() {
            return username != null && !username.isEmpty();
        }
    }
    
    /**
     * 解析代理配置字符串
     * 支持格式：
     * 1. socks://base64(username:password)@host:port#label
     * 2. http://username:password@host:port
     * 3. host:port
     * 
     * @param configStr 代理配置字符串
     * @return 解析后的配置对象
     */
    public static ProxyConfig parse(String configStr) {
        if (configStr == null || configStr.trim().isEmpty()) {
            throw new IllegalArgumentException("代理配置字符串不能为空");
        }
        
        configStr = configStr.trim();
        ProxyConfig config = new ProxyConfig();
        
        try {
            // 提取标签 (# 后面的部分)
            String[] parts = configStr.split("#", 2);
            String mainPart = parts[0];
            if (parts.length > 1) {
                config.setLabel(parts[1]);
            }
            
            // 提取协议
            Pattern protocolPattern = Pattern.compile("^(https?|socks5?)://(.+)");
            Matcher protocolMatcher = protocolPattern.matcher(mainPart);
            
            String bodyPart;
            if (protocolMatcher.matches()) {
                config.setProtocol(protocolMatcher.group(1));
                bodyPart = protocolMatcher.group(2);
            } else {
                config.setProtocol("http"); // 默认HTTP
                bodyPart = mainPart;
            }
            
            // 分离认证信息和地址
            String authPart = null;
            String addressPart;
            
            if (bodyPart.contains("@")) {
                String[] authSplit = bodyPart.split("@", 2);
                authPart = authSplit[0];
                addressPart = authSplit[1];
            } else {
                addressPart = bodyPart;
            }
            
            // 解析地址和端口
            String[] hostPort = addressPart.split(":", 2);
            config.setHost(hostPort[0]);
            if (hostPort.length > 1) {
                config.setPort(Integer.parseInt(hostPort[1]));
            } else {
                // 根据协议设置默认端口
                config.setPort(getDefaultPort(config.getProtocol()));
            }
            
            // 解析认证信息
            if (authPart != null && !authPart.isEmpty()) {
                parseAuth(authPart, config);
            }
            
            log.info("解析代理配置成功: protocol={}, host={}, port={}, hasAuth={}, label={}", 
                    config.getProtocol(), config.getHost(), config.getPort(), config.hasAuth(), config.getLabel());
            
            return config;
            
        } catch (Exception e) {
            log.error("解析代理配置失败: {}, 错误: {}", configStr, e.getMessage());
            throw new IllegalArgumentException("代理配置格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 解析认证信息
     * 支持：
     * 1. base64编码: base64(username:password)
     * 2. 明文: username:password
     */
    private static void parseAuth(String authPart, ProxyConfig config) {
        try {
            // 尝试Base64解码
            String decoded = new String(Base64.getDecoder().decode(authPart));
            String[] credentials = decoded.split(":", 2);
            if (credentials.length == 2) {
                config.setUsername(credentials[0]);
                config.setPassword(credentials[1]);
                log.debug("使用Base64解码认证信息");
                return;
            }
        } catch (IllegalArgumentException e) {
            // Base64解码失败，尝试明文解析
            log.debug("Base64解码失败，尝试明文解析");
        }
        
        // 明文解析
        String[] credentials = authPart.split(":", 2);
        config.setUsername(credentials[0]);
        if (credentials.length > 1) {
            config.setPassword(credentials[1]);
        }
    }
    
    /**
     * 获取协议默认端口
     */
    private static int getDefaultPort(String protocol) {
        return switch (protocol.toLowerCase()) {
            case "http" -> 80;
            case "https" -> 443;
            case "socks", "socks5" -> 1080;
            default -> 8080;
        };
    }
    
    /**
     * 构建代理配置字符串
     */
    public static String buildConfigString(ProxyConfig config) {
        StringBuilder sb = new StringBuilder();
        
        // 协议
        sb.append(config.getProtocol()).append("://");
        
        // 认证信息
        if (config.hasAuth()) {
            String auth = config.getUsername() + ":" + config.getPassword();
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes());
            sb.append(encoded).append("@");
        }
        
        // 地址和端口
        sb.append(config.getHost()).append(":").append(config.getPort());
        
        // 标签
        if (config.getLabel() != null && !config.getLabel().isEmpty()) {
            sb.append("#").append(config.getLabel());
        }
        
        return sb.toString();
    }
}
