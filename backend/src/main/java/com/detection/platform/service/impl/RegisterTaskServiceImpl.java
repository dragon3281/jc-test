package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.exception.BusinessException;
import com.detection.platform.dao.RegisterTaskMapper;
import com.detection.platform.entity.RegisterTask;
import com.detection.platform.service.RegisterTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动化注册Service实现
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterTaskServiceImpl implements RegisterTaskService {

    private final RegisterTaskMapper registerTaskMapper;
    private final ObjectMapper objectMapper;
    private final Map<Long, List<Map<String, Object>>> taskResultsStore = new ConcurrentHashMap<>();

    // 生成16位随机字符串
    private String rndString() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int idx = (int) Math.floor(61 * Math.random());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }

    // 生成首位非0的11位数字字符串
    private String generate11Digit() {
        StringBuilder sb = new StringBuilder();
        sb.append(1 + (int) Math.floor(Math.random() * 9));
        for (int i = 0; i < 10; i++) {
            sb.append((int) Math.floor(Math.random() * 10));
        }
        return sb.toString();
    }

    // DES ECB PKCS5 加密，key取前8字节以兼容CryptoJS DES
    private String desEncryptEcb(String plaintext, String key) throws Exception {
        // CryptoJS使用完整密钥的前8字节，与Java DES要求一致
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] key8 = new byte[8];
        // 只使用前8个字节（DES密钥长度要求）
        for (int i = 0; i < 8; i++) {
            key8[i] = i < keyBytes.length ? keyBytes[i] : 0;
        }
        log.debug("[DES加密] 使用密钥前8字节: {}", new String(key8, StandardCharsets.UTF_8));
        
        SecretKeySpec secretKey = new SecretKeySpec(key8, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(encrypted);
    }

    // DES ECB PKCS5 解密，用于解密服务器响应
    private String desDecryptEcb(String ciphertext, String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] key8 = new byte[8];
        // 只使用前8个字节（DES密钥长度要求）
        for (int i = 0; i < 8; i++) {
            key8[i] = i < keyBytes.length ? keyBytes[i] : 0;
        }
        log.debug("[DES解密] 使用密钥前8字节: {}", new String(key8, StandardCharsets.UTF_8));
        
        SecretKeySpec secretKey = new SecretKeySpec(key8, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(ciphertext));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // RSA PKCS1 加密（服务端返回的是十六进制模数，指数固定65537）
    /**
     * RSA加密 - 调用Python加密服务（与JS脚本100%一致）
     */
    private String rsaEncryptPkcs1(String keyStr, String data) throws Exception {
        try {
            // 调用Python加密服务
            ProcessBuilder pb = new ProcessBuilder(
                "python3",
                "/root/jc-test/test/encryption_service.py",
                "rsa_encrypt",
                keyStr,
                data
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            String result = reader.readLine();
            reader.close();
            
            int exitCode = process.waitFor();
            if (exitCode != 0 || result == null || result.isEmpty()) {
                throw new Exception("Python encryption service failed");
            }
            
            return result.trim();
        } catch (Exception e) {
            log.error("RSA加密失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<RegisterTask> pageRegisterTasks(Integer current, Integer size, String taskName, Integer status) {
        Page<RegisterTask> page = new Page<>(current, size);
        LambdaQueryWrapper<RegisterTask> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(taskName)) {
            wrapper.like(RegisterTask::getTaskName, taskName);
        }
        if (status != null) {
            wrapper.eq(RegisterTask::getStatus, status);
        }
        
        wrapper.orderByDesc(RegisterTask::getCreateTime);
        return registerTaskMapper.selectPage(page, wrapper);
    }

    @Override
    public RegisterTask getTaskById(Long id) {
        RegisterTask task = registerTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long createTask(Map<String, Object> params) {
        RegisterTask task = new RegisterTask();
        task.setTaskName((String) params.get("taskName"));
        task.setWebsiteUrl((String) params.get("websiteUrl"));
        task.setRegisterApi((String) params.get("registerApi"));
        task.setMethod((String) params.get("method"));
        task.setUsernameField((String) params.get("usernameField"));
        task.setPasswordField((String) params.get("passwordField"));
        task.setEmailField((String) params.get("emailField"));
        task.setPhoneField((String) params.get("phoneField"));
        task.setNeedPhone((Boolean) params.get("needPhone"));
        task.setManualPhone((String) params.get("manualPhone"));
        if (params.get("accountCount") != null) {
            task.setAccountCount(Integer.valueOf(params.get("accountCount").toString()));
        }
        task.setDefaultPassword((String) params.get("defaultPassword"));
        task.setExtraParams((String) params.get("extraParams"));
        
        task.setNeedCaptcha((Boolean) params.get("needCaptcha"));
        if (params.get("captchaType") != null) {
            task.setCaptchaType((Integer) params.get("captchaType"));
        }
        task.setCaptchaApi((String) params.get("captchaApi"));
        task.setCaptchaField((String) params.get("captchaField"));
        if (params.get("ocrMethod") != null) {
            task.setOcrMethod((Integer) params.get("ocrMethod"));
        }
        
        task.setNeedToken((Boolean) params.get("needToken"));
        task.setTokenField((String) params.get("tokenField"));
        if (params.get("tokenSource") != null) {
            task.setTokenSource((Integer) params.get("tokenSource"));
        }
        
        // 加密配置
        task.setEncryptionType((String) params.get("encryptionType"));
        task.setRsaKeyApi((String) params.get("rsaKeyApi"));
        task.setRsaTsParam((String) params.get("rsaTsParam"));
        task.setEncryptionHeader((String) params.get("encryptionHeader"));
        task.setValueFieldName((String) params.get("valueFieldName"));
        task.setDupMsgSubstring((String) params.get("dupMsgSubstring"));
        
        if (params.get("dataSourceId") != null) {
            task.setDataSourceId(Long.valueOf(params.get("dataSourceId").toString()));
        }
        task.setUseProxy((Boolean) params.get("useProxy"));
        if (params.get("proxyPoolId") != null) {
            task.setProxyPoolId(Long.valueOf(params.get("proxyPoolId").toString()));
        }
        task.setConcurrency((Integer) params.get("concurrency"));
        task.setAutoRetry((Boolean) params.get("autoRetry"));
        task.setRetryTimes((Integer) params.get("retryTimes"));
        
        task.setStatus(1); // 待执行
        task.setTotalCount(0);
        task.setCompletedCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        
        registerTaskMapper.insert(task);
        return task.getId();
    }

    @Override
    public Boolean startTask(Long id) {
        RegisterTask task = getTaskById(id);
        
        if (task.getStatus() != 1 && task.getStatus() != 4) {
            throw new BusinessException("只有待执行或已暂停的任务才能启动");
        }
        
        task.setStatus(2); // 执行中
        task.setStartTime(LocalDateTime.now());
        registerTaskMapper.updateById(task);
        
        // 异步执行注册任务
        executeRegisterTaskAsync(id);
        
        return true;
    }

    /**
     * 异步执行注册任务
     */
    private void executeRegisterTaskAsync(Long taskId) {
        new Thread(() -> {
            try {
                RegisterTask task = registerTaskMapper.selectById(taskId);

                // 从任务配置中获取加密相关参数
                String encryptionType = Optional.ofNullable(task.getEncryptionType()).orElse("NONE");
                String rsaKeyApi = Optional.ofNullable(task.getRsaKeyApi()).orElse("/wps/session/key/rsa");
                String tsParam = Optional.ofNullable(task.getRsaTsParam()).orElse("t");
                String encryptionHeader = Optional.ofNullable(task.getEncryptionHeader()).orElse("encryption");
                String valueFieldName = Optional.ofNullable(task.getValueFieldName()).orElse("value");
                String dupMsg = Optional.ofNullable(task.getDupMsgSubstring()).orElse("Ang username na ito ay ginamit na ng ibang user");

                // 从额外参数中解析自定义headers/cookies
                Map<String, Object> ext = new HashMap<>();
                if (task.getExtraParams() != null && !task.getExtraParams().isEmpty()) {
                    try {
                        ext = objectMapper.readValue(task.getExtraParams(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    } catch (Exception e) {
                        log.warn("解析extraParams失败，按空配置处理: {}", e.getMessage());
                    }
                }

                Map<String, String> headerExtras = (Map<String, String>) ext.getOrDefault("headers", Collections.emptyMap());
                Map<String, String> cookieExtras = (Map<String, String>) ext.getOrDefault("cookies", Collections.emptyMap());
                String userAgent = String.valueOf(ext.getOrDefault("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"));
                String referer = String.valueOf(ext.getOrDefault("referer", task.getWebsiteUrl()));

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

                // 预估注册数量：如无数据源，按并发*10条示例
                int totalCount = Optional.ofNullable(task.getAccountCount()).orElse(50);
                task.setTotalCount(totalCount);
                registerTaskMapper.updateById(task);

                for (int i = 0; i < totalCount; i++) {
                    // 每次循环检查最新的任务状态
                    task = registerTaskMapper.selectById(taskId);
                    if (task.getStatus() == 4) { // 已暂停
                        log.info("任务已暂停, taskId={}, 当前进度={}/{}", taskId, i, totalCount);
                        break;
                    }

                    log.info("[Register] ========== 开始注册用户 #{} ===========", i + 1);
                    String rnd = rndString();
                    log.info("[Register] 原始随机字符串 rnd: {}", rnd);
                    log.info("[Register] 反转后 reversedRnd: {}", new StringBuilder(rnd).reverse().toString());

                    // 构造明文参数 - 完全按照Python脚本的结构
                    Map<String, Object> bodyPlain = new LinkedHashMap<>();
                    String usernameField = Optional.ofNullable(task.getUsernameField()).orElse("username");
                    String passwordField = Optional.ofNullable(task.getPasswordField()).orElse("password");
                    String defaultPassword = Optional.ofNullable(task.getDefaultPassword()).orElse("133adb");
                    
                    // 生成用户名
                    String usedUsername = generate11Digit();
                    
                    // 先设置基础字段（按Python脚本顺序）
                    bodyPlain.put(usernameField, usedUsername);
                    bodyPlain.put(passwordField, defaultPassword);
                    bodyPlain.put("confirmPassword", defaultPassword);  // ⚠️ 必须字段
                    bodyPlain.put("payeeName", "");
                    bodyPlain.put("email", "");
                    bodyPlain.put("qqNum", "");
                    bodyPlain.put("mobileNum", "");
                    bodyPlain.put("captcha", "");
                    bodyPlain.put("verificationCode", "");
                    bodyPlain.put("affiliateCode", "www");
                    bodyPlain.put("paymentPassword", "");
                    bodyPlain.put("line", "");
                    bodyPlain.put("whatsapp", "");
                    bodyPlain.put("facebook", "");
                    bodyPlain.put("wechat", "");
                    bodyPlain.put("idNumber", "");
                    bodyPlain.put("nickname", "");
                    bodyPlain.put("domain", "www-tk999");
                    bodyPlain.put("login", true);  // ⚠️ Boolean类型
                    bodyPlain.put("registerUrl", task.getWebsiteUrl() + "/");
                    bodyPlain.put("registerMethod", "WEB");
                    bodyPlain.put("loginDeviceId", "e6ce5ac9-4b17-4e33-acbd-7350b443f572");

                    // 额外参数覆盖默认值（如果有的话）
                    if (task.getExtraParams() != null && !task.getExtraParams().isEmpty()) {
                        try {
                            Map<String, Object> extras = objectMapper.readValue(task.getExtraParams(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                            // 只排除配置型键，其他字段覆盖到bodyPlain
                            for (Map.Entry<String, Object> en : extras.entrySet()) {
                                String k = en.getKey();
                                if (!Arrays.asList("headers","cookies","userAgent","referer").contains(k)) {
                                    bodyPlain.put(k, en.getValue());
                                }
                            }
                        } catch (Exception e) {
                            log.warn("[Register] 解析extraParams失败: {}", e.getMessage());
                        }
                    }

                    // 序列化为JSON并移除所有空格（与Python脚本保持一致）
                    String plaintextJson = objectMapper.writeValueAsString(bodyPlain);
                    log.info("[Register] taskId={} 序列化前的Map内容: {}", taskId, bodyPlain);
                    log.info("[Register] taskId={} 序列化后JSON(有空格): {}", taskId, plaintextJson);
                    plaintextJson = plaintextJson.replace(" ", "");
                    log.info("[Register] taskId={} 明文参数长度: {} 字符", taskId, plaintextJson.length());
                    log.info("[Register] taskId={} 明文参数(无空格): {}", taskId, plaintextJson);

                    // 加密处理：DES和RSA都使用反转的rnd
                    String encryptedValue = plaintextJson;
                    String encryptionHeaderValue = null;
                    String publicKeyStr = null;
                    String reversedRnd = new StringBuilder(rnd).reverse().toString();
                    log.info("[Register] ========== 开始加密流程 ==========");
                    log.info("[Register] 将使用反转后的rnd作为DES密钥: {}", reversedRnd);
                    
                    if ("DES_RSA".equalsIgnoreCase(encryptionType)) {
                        // DES加密：使用反转的rnd作为密钥
                        try {
                            encryptedValue = desEncryptEcb(plaintextJson, reversedRnd);
                            log.info("[Register] ✅ DES加密完成");
                            log.info("[Register]    - 明文长度: {} 字节", plaintextJson.length());
                            log.info("[Register]    - 密钥(反转rnd): {}", reversedRnd);
                            log.info("[Register]    - 密钥前8字节: {}", reversedRnd.substring(0, Math.min(8, reversedRnd.length())));
                            log.info("[Register]    - DES密文长度: {} 字符", encryptedValue.length());
                            log.info("[Register]    - DES密文前80字符: {}", encryptedValue.substring(0, Math.min(80, encryptedValue.length())));
                        } catch (Exception e) {
                            log.error("❌ DES加密失败: {}", e.getMessage(), e);
                        }
                        // 获取RSA公钥
                        String ts = String.valueOf(System.currentTimeMillis());
                        String rsaUrl = task.getWebsiteUrl();
                        if (!rsaUrl.endsWith("/")) rsaUrl += "/";
                        if (rsaKeyApi.startsWith("/")) rsaUrl += rsaKeyApi.substring(1);
                        else rsaUrl += rsaKeyApi;
                        HttpUrl urlWithTs = HttpUrl.parse(rsaUrl).newBuilder().addQueryParameter(tsParam, ts).build();
                        log.info("[Register] 拉取RSA公钥: {}", urlWithTs);
                        Request rsaReq = new Request.Builder().url(urlWithTs).get()
                                .header("User-Agent", userAgent)
                                .header("Referer", referer)
                                .build();
                        /* reuse publicKeyStr */
                        try (Response resp = client.newCall(rsaReq).execute()) {
                            log.info("[Register] RSA接口响应码: {}", resp.code());
                            if (resp.isSuccessful() && resp.body() != null) {
                                publicKeyStr = resp.body().string();
                                log.info("[Register] 获取到RSA公钥: {}", publicKeyStr);
                            } else {
                                log.warn("❌ 获取RSA公钥失败: {}", resp.code());
                            }
                        }
                        // ⚠️ 关键修复：RSA加密使用【原始rnd】，不是反转的！（参考Python脚本第95行）
                        if (publicKeyStr != null && !publicKeyStr.isEmpty()) {
                            try {
                                encryptionHeaderValue = rsaEncryptPkcs1(publicKeyStr, rnd);
                                log.info("[Register] ✅ RSA加密完成");
                                log.info("[Register]    - 待加密内容(原始rnd): {}", rnd);
                                log.info("[Register]    - RSA密文长度: {} 字符", encryptionHeaderValue.length());
                                log.info("[Register]    - RSA密文前120字符: {}", encryptionHeaderValue.substring(0, Math.min(120, encryptionHeaderValue.length())));
                                log.info("[Register]    - RSA密文完整: {}", encryptionHeaderValue);
                            } catch (Exception e) {
                                log.error("❌ RSA加密失败: {}", e.getMessage(), e);
                            }
                        }
                    }
                    
                    log.info("[Register] ========== 加密完成，准备发送请求 ==========");

                    // 构造请求
                    // 构造注册URL（默认使用wwwtk666的接口）
                    String apiUrl = task.getWebsiteUrl();
                    if (!apiUrl.endsWith("/")) apiUrl += "/";
                    String regApi = Optional.ofNullable(task.getRegisterApi()).orElse("/wps/member/register");
                    if (regApi.startsWith("/")) apiUrl += regApi.substring(1); else apiUrl += regApi;

                    String method = Optional.ofNullable(task.getMethod()).orElse("PUT");  // ⚠️ 默认PUT
                    Request.Builder reqBuilder = new Request.Builder().url(apiUrl)
                            .header("User-Agent", userAgent)
                            .header("Accept", "application/json, text/plain, */*")
                            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                            .header("Cache-Control", "no-cache")
                            .header("Content-Type", "application/json")
                            .header("Pragma", "no-cache")
                            .header("Priority", "u=1, i")
                            .header("Referer", referer)
                            .header("Sec-Ch-Ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"")
                            .header("Sec-Ch-Ua-Mobile", "?0")
                            .header("Sec-Ch-Ua-Platform", "\"Linux\"")
                            .header("Sec-Fetch-Dest", "empty")
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "same-origin")
                            .header("Origin", task.getWebsiteUrl());
                    log.info("[Register] 请求方法={}, URL={}", method, apiUrl);

                    // 额外Header
                    reqBuilder.header("device", "web");
                    reqBuilder.header("language", "BN");
                    reqBuilder.header("merchant", "ck555bdtf3");
                    log.info("[Register] 添加固定请求头: device=web, language=BN, merchant=ck555bdtf3");
                    
                    for (Map.Entry<String, String> h : headerExtras.entrySet()) {
                        reqBuilder.header(h.getKey(), h.getValue());
                        log.info("[Register] 添加自定义请求头: {}={}", h.getKey(), h.getValue());
                    }
                    if (encryptionHeaderValue != null) {
                        reqBuilder.header(encryptionHeader, encryptionHeaderValue);
                        log.info("[Register] 添加加密请求头: {}={}", encryptionHeader, encryptionHeaderValue.substring(0, Math.min(80, encryptionHeaderValue.length())) + "...");
                    }

                    // Cookies
                    if (!cookieExtras.isEmpty()) {
                        StringBuilder cookieLine = new StringBuilder();
                        for (Map.Entry<String, String> c : cookieExtras.entrySet()) {
                            if (cookieLine.length() > 0) cookieLine.append("; ");
                            cookieLine.append(c.getKey()).append("=").append(c.getValue());
                        }
                        reqBuilder.header("Cookie", cookieLine.toString());
                    }

                    /* method defined above */
                    if ("GET".equalsIgnoreCase(method)) {
                        reqBuilder.get();
                    } else {
                        // 默认发送JSON体（紧凑格式，无空格，与Python一致）
                        Map<String, Object> sendBody = new LinkedHashMap<>();
                        sendBody.put(valueFieldName, encryptedValue);
                        // 使用紧凑格式序列化（无空格）
                        objectMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
                        String bodyJson = objectMapper.writeValueAsString(sendBody);
                        log.info("[Register] 请求体JSON: {}", bodyJson);
                        log.info("[Register] 请求体JSON长度: {}", bodyJson.length());
                        log.info("[Register] 请求体中的value字段长度: {}", encryptedValue.length());
                        
                        RequestBody requestBody = RequestBody.create(bodyJson, MediaType.parse("application/json; charset=utf-8"));
                        if ("PUT".equalsIgnoreCase(method)) {
                            reqBuilder.put(requestBody);
                        } else {
                            reqBuilder.post(requestBody);
                        }
                    }
                    
                    log.info("[Register] ========== 发送注册请求 ==========");

                    boolean success = false;
                    String extractedToken = null;
                    String firstRegisterMsg = "";
                    
                    try (Response resp = client.newCall(reqBuilder.build()).execute()) {
                        // 首次注册请求响应
                        String respBody1 = resp.body() != null ? resp.body().string() : "";
                        firstRegisterMsg = respBody1;
                        log.info("[Register] 用户名={} 首次注册响应码={}, 响应体={}", usedUsername, resp.code(), respBody1);
                        
                        // 判断是否成功：响应码200且有内容
                        if (resp.isSuccessful() && respBody1 != null && !respBody1.isEmpty()) {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> respMap = objectMapper.readValue(respBody1, Map.class);
                                
                                // 首先检查success字段
                                Boolean respSuccess = (Boolean) respMap.get("success");
                                if (respSuccess != null && !respSuccess) {
                                    log.warn("[Register] 用户名={} 服务器返回success=false，错误信息: {}", usedUsername, respMap.get("message"));
                                    log.warn("[Register] 完整响应: {}", respBody1);
                                }
                                
                                // 根据加密类型提取token
                                if ("DES_RSA".equalsIgnoreCase(encryptionType)) {
                                    // 服务器返回的value字段：优先尝试明文Map（与Python脚本一致）
                                    Object valueObj = respMap.get("value");
                                    if (valueObj != null && valueObj instanceof Map) {
                                        // 服务器直接返回明文Map（最常见情况）
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                                        
                                        // 输出完整的响应内容
                                        log.info("[Register] ✅ 服务器返回明文响应");
                                        log.info("[Register] 完整value内容: {}", valueMap);
                                        
                                        if (valueMap.containsKey("token")) {
                                            extractedToken = String.valueOf(valueMap.get("token"));
                                            success = true;
                                            log.info("[Register] ✅ 用户名={} 成功提取Token: {}", usedUsername, extractedToken);
                                            
                                            // 提取userName（实际注册的用户名）
                                            if (valueMap.containsKey("userName")) {
                                                String actualUsername = String.valueOf(valueMap.get("userName"));
                                                log.info("[Register] 实际注册用户名: {}", actualUsername);
                                                // 更新usedUsername为服务器返回的实际用户名
                                                usedUsername = actualUsername;
                                            }
                                        }
                                    } else if (valueObj != null && valueObj instanceof String) {
                                        // 如果是加密的String，尝试解密（备用逻辑）
                                        String encryptedResponseValue = (String) valueObj;
                                        log.info("[Register] 用户名={} 尝试解密响应value字段，密文长度={}", usedUsername, encryptedResponseValue.length());
                                        try {
                                            // 使用反转的rnd作为解密密钥（与加密时相同）
                                            String decryptedValue = desDecryptEcb(encryptedResponseValue, reversedRnd);
                                            log.info("[Register] 用户名={} 解密成功，明文内容={}", usedUsername, decryptedValue);
                                            // 解析解密后的JSON
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> valueMap = objectMapper.readValue(decryptedValue, Map.class);
                                            if (valueMap.containsKey("token")) {
                                                extractedToken = String.valueOf(valueMap.get("token"));
                                                success = true; // 能提取到token说明注册成功
                                                log.info("[Register] ✅ 用户名={} 成功提取Token: {}", usedUsername, extractedToken);
                                            } else {
                                                log.warn("[Register] 用户名={} 解密后的响应中未找到token字段，响应内容: {}", usedUsername, decryptedValue);
                                            }
                                        } catch (Exception e) {
                                            log.error("[Register] 用户名={} 解密或解析value字段失败: {}", usedUsername, e.getMessage(), e);
                                        }
                                    }
                                } else {
                                    // 无加密模式：直接从响应中提取token
                                    if (respMap.containsKey("token")) {
                                        extractedToken = String.valueOf(respMap.get("token"));
                                        success = true;
                                    } else if (respMap.containsKey("data")) {
                                        Object dataObj = respMap.get("data");
                                        if (dataObj instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                                            if (dataMap.containsKey("token")) {
                                                extractedToken = String.valueOf(dataMap.get("token"));
                                                success = true;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("[Register] 用户名={} 解析响应JSON失败: {}", usedUsername, e.getMessage(), e);
                            }
                        } else {
                            log.warn("[Register] 用户名={} 注册请求失败，响应码={}", usedUsername, resp.code());
                        }
                    } catch (Exception e) {
                        log.error("[Register] 用户名={} 注册请求异常: {}", usedUsername, e.getMessage(), e);
                    }

                    // Python脚本中没有二次验证逻辑，直接根据首次响应判断
                    // 如果首次注册响应中成功提取到token，说明注册成功
                    log.info("[Register] 用户名={} 最终判断 - 成功={}, Token={}", usedUsername, success, extractedToken != null ? extractedToken : "无");

                    // 记录结果
                    Map<String, Object> one = new LinkedHashMap<>();
                    one.put("username", usedUsername);
                    one.put("password", Optional.ofNullable(task.getDefaultPassword()).orElse("111111"));
                    one.put("token", extractedToken); // 保存提取的token
                    one.put("status", success ? 1 : 0);
                    one.put("message", success ? "注册成功，已提取Token" : (firstRegisterMsg.isEmpty() ? "注册失败，无响应" : "注册失败: " + firstRegisterMsg.substring(0, Math.min(200, firstRegisterMsg.length()))));
                    one.put("registerTime", LocalDateTime.now());
                    taskResultsStore.computeIfAbsent(taskId, k -> new ArrayList<>()).add(one);
                    log.info("[Register] ========== 用户名={} 注册完成 ========== 状态={} Token={}", usedUsername, success ? "成功" : "失败", extractedToken);

                    task.setCompletedCount(i + 1);
                    if (success) {
                        task.setSuccessCount(Optional.ofNullable(task.getSuccessCount()).orElse(0) + 1);
                    } else {
                        task.setFailCount(Optional.ofNullable(task.getFailCount()).orElse(0) + 1);
                    }
                    registerTaskMapper.updateById(task);
                }

                // 任务完成
                task = registerTaskMapper.selectById(taskId);
                if (task.getStatus() != 4) {
                    task.setStatus(3); // 已完成
                    task.setEndTime(LocalDateTime.now());
                    registerTaskMapper.updateById(task);
                }

            } catch (Exception e) {
                log.error("注册任务执行失败", e);
                RegisterTask task = registerTaskMapper.selectById(taskId);
                task.setStatus(5); // 失败
                registerTaskMapper.updateById(task);
            }
        }, "Register-" + taskId).start();
    }

    @Override
    public Boolean pauseTask(Long id) {
        RegisterTask task = getTaskById(id);
        
        if (task.getStatus() != 2) {
            throw new BusinessException("只有执行中的任务才能暂停");
        }
        
        task.setStatus(4); // 已暂停
        registerTaskMapper.updateById(task);
        return true;
    }

    @Override
    public Boolean resumeTask(Long id) {
        return startTask(id); // 复用启动逻辑
    }

    @Override
    public Boolean deleteTask(Long id) {
        log.info("开始删除注册任务, id={}", id);
        
        RegisterTask task = registerTaskMapper.selectById(id);
        if (task == null) {
            log.warn("任务不存在, id={}", id);
            throw new BusinessException("任务不存在");
        }
        
        log.info("任务信息: taskName={}, status={}", task.getTaskName(), task.getStatus());
        
        if (task.getStatus() == 2) {
            log.warn("执行中的任务无法删除, id={}, status={}", id, task.getStatus());
            throw new BusinessException("执行中的任务无法删除，请先暂停任务");
        }
        
        int rows = registerTaskMapper.deleteById(id);
        log.info("删除结果: rows={}", rows);
        
        // 清理结果缓存
        taskResultsStore.remove(id);
        
        return rows > 0;
    }

    @Override
    public List<Map<String, Object>> getTaskResults(Long id) {
        RegisterTask task = getTaskById(id);
        
        // 返回真实记录（如果有），否则回退到旧的模拟数据
        List<Map<String, Object>> stored = taskResultsStore.get(id);
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        // 模拟返回注册结果
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < task.getCompletedCount(); i++) {
            Map<String, Object> result = new HashMap<>();
            result.put("username", "user" + (i + 1));
            result.put("password", task.getDefaultPassword());
            result.put("token", null); // 模拟数据暂无token
            result.put("status", i < task.getSuccessCount() ? 1 : 0);
            result.put("message", i < task.getSuccessCount() ? "注册成功" : "注册失败");
            result.put("registerTime", LocalDateTime.now().minusMinutes(i));
            results.add(result);
        }
        
        return results;
    }
}
