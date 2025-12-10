package com.detection.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.detection.platform.entity.DetectionTaskItem;
import com.detection.platform.mapper.DetectionTaskItemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * POST模板检测服务
 * 执行批量检测任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostDetectionService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final DetectionTaskItemMapper detectionTaskItemMapper;

    // 进度监听接口
    public interface ProgressListener {
        void onItemComplete(DetectionItemResult item, int processed, int total);
        default void onRoundComplete(int roundProcessed, int totalProcessed, int total) {}
        
        /**
         * 检查是否应该暂停
         * @return true表示应该暂停
         */
        default boolean shouldPause() {
            return false;
        }
        
        /**
         * 检查是否应该取消
         * @return true表示应该取消
         */
        default boolean shouldCancel() {
            return false;
        }
    }
    
    /**
     * 执行单次检测
     * 
     * @param requestUrl 请求URL
     * @param requestMethod 请求方法
     * @param requestHeaders 请求头模板(JSON字符串)
     * @param requestBody 请求体模板(JSON字符串)
     * @param variableConfig 变量配置(JSON数组字符串)
     * @param variableValues 变量值Map
     * @param duplicateMsg 重复关键字
     * @param responseCode 重复时的状态码
     * @return 检测结果：true=已注册，false=未注册
     */
    public DetectionResult executeDetection(
            String requestUrl,
            String requestMethod,
            String requestHeaders,
            String requestBody,
            String variableConfig,
            Map<String, String> variableValues,
            String duplicateMsg,
            Integer responseCode) {
        
        DetectionResult result = new DetectionResult();
        result.setSuccess(false);
        
        try {
            // 1. 替换请求头中的变量
            Map<String, String> headers = objectMapper.readValue(requestHeaders, Map.class);
            Map<String, String> finalHeaders = replaceVariablesInHeaders(headers, variableConfig, variableValues);
            
            // 2. 替换请求体中的变量
            String finalBody = replaceVariablesInBody(requestBody, variableConfig, variableValues);
            
            // 3. 构建HTTP请求
            HttpHeaders httpHeaders = new HttpHeaders();
            finalHeaders.forEach(httpHeaders::set);
            
            HttpEntity<String> entity = new HttpEntity<>(finalBody, httpHeaders);
            
            log.info("[Detection] 发送请求: URL={}, Method={}, Body={}", requestUrl, requestMethod, finalBody);
            
            // 4. 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                requestUrl, 
                HttpMethod.valueOf(requestMethod.toUpperCase()), 
                entity, 
                String.class);
            
            // 5. 解压响应（如果需要）
            String responseBody = decompressResponse(response);
            
            // 6. 判断结果
            int status = response.getStatusCode().value();
            
            result.setResponseCode(status);
            result.setResponseBody(responseBody);
            result.setSuccess(true);
            
            // 判断是否已注册：状态码匹配（如果设置了） 且 响应包含关键字
            boolean statusMatch = (responseCode == null) || (status == responseCode);
            boolean keywordMatch = (responseBody != null && duplicateMsg != null && responseBody.contains(duplicateMsg));
            boolean isDuplicate = statusMatch && keywordMatch;
            result.setDuplicate(isDuplicate);
            
            // 检测是否触发限流
            result.setRateLimited(false);
            
            log.info("[Detection] 检测完成: 状态码={}, 是否已注册={}", status, isDuplicate);
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx错误也是正常响应（如400表示已注册）
            int status = e.getStatusCode().value();
            String responseBody = decompressErrorResponse(e);
            
            result.setResponseCode(status);
            result.setResponseBody(responseBody);
            result.setSuccess(true);
            
            // 判断是否已注册：状态码匹配（如果设置了） 且 响应包含关键字
            boolean statusMatch = (responseCode == null) || (status == responseCode);
            boolean keywordMatch = (responseBody != null && duplicateMsg != null && responseBody.contains(duplicateMsg));
            boolean isDuplicate = statusMatch && keywordMatch;
            result.setDuplicate(isDuplicate);
            
            // 检测是否触发限流（429 或包含限流关键字）
            result.setRateLimited(status == 429 || (responseBody != null && responseBody.contains("TOO_MANY_REQUEST")));
            
            log.info("[Detection] 检测完成(4xx): 状态码={}, 是否已注册={}, 限流={}", status, isDuplicate, result.isRateLimited());
            
        } catch (Exception e) {
            log.error("[Detection] 检测失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setRateLimited(false);
        }
        
        return result;
    }
    
    /**
     * 解压响应内容（支持gzip）
     */
    private String decompressResponse(ResponseEntity<String> response) {
        try {
            String body = response.getBody();
            HttpHeaders headers = response.getHeaders();
            String encoding = headers.getFirst("Content-Encoding");
            
            if (encoding != null && encoding.toLowerCase().contains("gzip") && body != null) {
                // 如果已经是字符串，说明RestTemplate已自动解压，直接返回
                return body;
            }
            return body;
        } catch (Exception e) {
            log.warn("解压响应失败，返回原始内容: {}", e.getMessage());
            return response.getBody();
        }
    }
    
    /**
     * 解压错误响应内容
     */
    private String decompressErrorResponse(org.springframework.web.client.HttpClientErrorException e) {
        try {
            String body = e.getResponseBodyAsString();
            // 尝试GZIP解压
            if (body != null && body.length() > 0) {
                try {
                    byte[] compressed = body.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                    ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
                    GZIPInputStream gis = new GZIPInputStream(bis);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = gis.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    return bos.toString(java.nio.charset.StandardCharsets.UTF_8.name());
                } catch (Exception ignored) {
                    // 不是gzip压缩，返回原文
                    return body;
                }
            }
            return body;
        } catch (Exception ex) {
            return e.getResponseBodyAsString();
        }
    }
    
    /**
     * 替换请求头中的变量
     */
    private Map<String, String> replaceVariablesInHeaders(
            Map<String, String> headers, 
            String variableConfig,
            Map<String, String> variableValues) throws Exception {
        
        Map<String, String> result = new HashMap<>(headers);
        List<Map<String, String>> configs = objectMapper.readValue(variableConfig, List.class);
        
        for (Map<String, String> config : configs) {
            String location = config.get("location");
            String name = config.get("name");
            String placeholder = config.get("placeholder");
            
            if ("header".equals(location)) {
                String value = variableValues.get(name);
                if (value != null) {
                    // 替换占位符
                    result.put(name, value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 替换请求体中的变量
     */
    private String replaceVariablesInBody(
            String requestBody,
            String variableConfig,
            Map<String, String> variableValues) throws Exception {
        
        Map<String, Object> body = objectMapper.readValue(requestBody, Map.class);
        List<Map<String, String>> configs = objectMapper.readValue(variableConfig, List.class);
        
        for (Map<String, String> config : configs) {
            String location = config.get("location");
            String name = config.get("name");
            
            if ("body".equals(location)) {
                String value = variableValues.get(name);
                if (value != null) {
                    body.put(name, value);
                }
            }
        }
        
        return objectMapper.writeValueAsString(body);
    }
    
    /**
     * 批量检测（并发模式）
     * Token数量 = 并发数
     * 例如: 9个号码 + 3个Token = 3轮并发检测，每轮3个
     * 
     * @param requestUrl 请求URL
     * @param requestMethod 请求方法
     * @param requestHeaders 请求头模板
     * @param requestBody 请求体模板
     * @param variableConfig 变量配置
     * @param tokens Token列表（Token数=并发数）
     * @param phones 手机号列表
     * @param duplicateMsg 重复关键字
     * @param responseCode 重复时的状态码
     * @param strategy 分配策略（并发模式下忽略）
     * @return 批量检测结果
     */
    public BatchDetectionResult batchDetection(
            String requestUrl,
            String requestMethod,
            String requestHeaders,
            String requestBody,
            String variableConfig,
            List<String> tokens,
            List<String> phones,
            String duplicateMsg,
            Integer responseCode,
            String strategy) {
        
        BatchDetectionResult batchResult = new BatchDetectionResult();
        List<DetectionItemResult> results = Collections.synchronizedList(new ArrayList<>());
        
        int concurrency = tokens.size(); // 并发数 = Token数量
        int totalPhones = phones.size();
        int rounds = (int) Math.ceil((double) totalPhones / concurrency); // 需要几轮
        
        log.info("[BatchDetection] 开始并发检测: 手机号{}个, 并发数{}, 预计{}轮", 
                 totalPhones, concurrency, rounds);
        
        // 分轮并发执行
        for (int round = 0; round < rounds; round++) {
            int startIdx = round * concurrency;
            int endIdx = Math.min(startIdx + concurrency, totalPhones);
            
            log.info("[BatchDetection] 第{}/{}轮: 检测索引{}-{}", round + 1, rounds, startIdx, endIdx - 1);
            
            // 创建线程池执行本轮任务
            List<java.util.concurrent.Callable<DetectionItemResult>> tasks = new ArrayList<>();
            
            for (int i = startIdx; i < endIdx; i++) {
                final int index = i;
                final String phone = phones.get(index);
                final String token = tokens.get(index - startIdx); // 每轮重新分配：第0个用token0，第1个用token1...
                
                tasks.add(() -> {
                    // 从变量配置中动态提取变量名
                    Map<String, String> variableValues = new HashMap<>();
                    try {
                        List<Map<String, String>> configs = objectMapper.readValue(variableConfig, List.class);
                        for (Map<String, String> config : configs) {
                            String name = config.get("name");
                            String location = config.get("location");
                            
                            // 根据变量名类型填充值
                            if (name.toLowerCase().contains("auth") || name.toLowerCase().contains("token")) {
                                variableValues.put(name, token);
                            } else if (name.toLowerCase().contains("mobile") || name.toLowerCase().contains("phone")) {
                                variableValues.put(name, phone);
                            } else {
                                // 其他变量，根据位置自动填充：请求头用token，请求体用phone
                                if ("header".equals(location)) {
                                    variableValues.put(name, token);
                                } else if ("body".equals(location)) {
                                    variableValues.put(name, phone);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析变量配置失败", e);
                        // Fallback: 如果解析失败，使用默认值
                        variableValues.put("Authorization", token);
                        variableValues.put("mobile", phone);
                    }
                    
                    // 执行检测
                    DetectionResult result = executeDetection(
                        requestUrl, requestMethod, requestHeaders, requestBody,
                        variableConfig, variableValues, duplicateMsg, responseCode
                    );
                    
                    // 构建结果
                    DetectionItemResult itemResult = new DetectionItemResult();
                    itemResult.setPhone(phone);
                    itemResult.setToken(maskToken(token));
                    itemResult.setSuccess(result.isSuccess());
                    itemResult.setResponseCode(result.getResponseCode());
                    itemResult.setDuplicate(result.isDuplicate());
                    itemResult.setErrorMessage(result.getErrorMessage());
                    
                    log.info("[BatchDetection] 检测完成: {} -> {} ({})", 
                             phone, result.isDuplicate() ? "已注册" : "未注册", maskToken(token));
                    
                    return itemResult;
                });
            }
            
            // 并发执行本轮任务
            try {
                java.util.concurrent.ExecutorService executor = 
                    java.util.concurrent.Executors.newFixedThreadPool(concurrency);
                
                List<java.util.concurrent.Future<DetectionItemResult>> futures = executor.invokeAll(tasks);
                
                // 收集结果
                for (java.util.concurrent.Future<DetectionItemResult> future : futures) {
                    results.add(future.get());
                }
                
                executor.shutdown();
                
            } catch (Exception e) {
                log.error("[BatchDetection] 第{}轮执行失败", round + 1, e);
            }
            
            // 轮次间隔，避免请求过快
            if (round < rounds - 1) {
                try {
                    Thread.sleep(500); // 每轮间隔500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // 统计结果
        int duplicateCount = (int) results.stream().filter(DetectionItemResult::isDuplicate).count();
        
        batchResult.setTotal(totalPhones);
        batchResult.setDuplicateCount(duplicateCount);
        batchResult.setResults(results);
        
        log.info("[BatchDetection] 全部完成: 总数={}, 已注册={}, 未注册={}", 
                 totalPhones, duplicateCount, totalPhones - duplicateCount);
        
        return batchResult;
    }

    // 带进度监听的并发批量检测
    public BatchDetectionResult batchDetection(
            String requestUrl,
            String requestMethod,
            String requestHeaders,
            String requestBody,
            String variableConfig,
            List<String> tokens,
            List<String> phones,
            String duplicateMsg,
            Integer responseCode,
            String strategy,
            ProgressListener listener) {

        BatchDetectionResult batchResult = new BatchDetectionResult();
        List<DetectionItemResult> results = Collections.synchronizedList(new ArrayList<>());

        int concurrency = tokens.size();
        int totalPhones = phones.size();
        int rounds = (int) Math.ceil((double) totalPhones / concurrency);

        log.info("[BatchDetection] 开始并发检测(带进度): 手机号{}个, 并发数{}, 预计{}轮", totalPhones, concurrency, rounds);

        int[] processedCounter = new int[]{0};

        for (int round = 0; round < rounds; round++) {
            int startIdx = round * concurrency;
            int endIdx = Math.min(startIdx + concurrency, totalPhones);
            log.info("[BatchDetection] 第{}/{}轮: 检测索引{}-{}", round + 1, rounds, startIdx, endIdx - 1);

            List<java.util.concurrent.Callable<DetectionItemResult>> tasks = new ArrayList<>();

            for (int i = startIdx; i < endIdx; i++) {
                final int index = i;
                final String phone = phones.get(index);
                final String token = tokens.get(index - startIdx);

                tasks.add(() -> {
                    Map<String, String> variableValues = new HashMap<>();
                    try {
                        List<Map<String, String>> configs = objectMapper.readValue(variableConfig, List.class);
                        for (Map<String, String> config : configs) {
                            String name = config.get("name");
                            String location = config.get("location");
                            if (name.toLowerCase().contains("auth") || name.toLowerCase().contains("token")) {
                                variableValues.put(name, token);
                            } else if (name.toLowerCase().contains("mobile") || name.toLowerCase().contains("phone")) {
                                variableValues.put(name, phone);
                            } else {
                                if ("header".equals(location)) {
                                    variableValues.put(name, token);
                                } else if ("body".equals(location)) {
                                    variableValues.put(name, phone);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析变量配置失败", e);
                        variableValues.put("Authorization", token);
                        variableValues.put("mobile", phone);
                    }

                    DetectionResult result = executeDetection(
                        requestUrl, requestMethod, requestHeaders, requestBody,
                        variableConfig, variableValues, duplicateMsg, responseCode
                    );

                    DetectionItemResult itemResult = new DetectionItemResult();
                    itemResult.setPhone(phone);
                    itemResult.setToken(maskToken(token));
                    itemResult.setSuccess(result.isSuccess());
                    itemResult.setResponseCode(result.getResponseCode());
                    itemResult.setDuplicate(result.isDuplicate());
                    itemResult.setErrorMessage(result.getErrorMessage());

                    synchronized (results) {
                        results.add(itemResult);
                        processedCounter[0]++;
                    }

                    if (listener != null) {
                        listener.onItemComplete(itemResult, processedCounter[0], totalPhones);
                    }

                    return itemResult;
                });
            }

            try {
                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(concurrency);
                List<java.util.concurrent.Future<DetectionItemResult>> futures = executor.invokeAll(tasks);
                for (java.util.concurrent.Future<DetectionItemResult> future : futures) {
                    future.get();
                }
                executor.shutdown();
            } catch (Exception e) {
                log.error("[BatchDetection] 第{}轮执行失败", round + 1, e);
            }

            if (listener != null) {
                listener.onRoundComplete(endIdx - startIdx, processedCounter[0], totalPhones);
            }

            if (round < rounds - 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        int duplicateCount = (int) results.stream().filter(DetectionItemResult::isDuplicate).count();
        batchResult.setTotal(totalPhones);
        batchResult.setDuplicateCount(duplicateCount);
        batchResult.setResults(results);
        log.info("[BatchDetection] 全部完成(带进度): 总数={}, 已注册={}, 未注册={}", totalPhones, duplicateCount, totalPhones - duplicateCount);
        return batchResult;
    }

    
    /**
     * 批量检测（按照Python脚本逻辑：轮询分配token + 自适应限流）
     * 分配逻辑：每轮并发数 = token数量，按轮询顺序分配
     * 限流控制：连续触发限流时降低并发，正常后恢复
     */
    public BatchDetectionResult batchDetectionWithAdaptiveRateLimit(
            String taskId,
            Long templateId,
            String requestUrl,
            String requestMethod,
            String requestHeaders,
            String requestBody,
            String variableConfig,
            List<String> tokens,
            List<String> phones,
            String duplicateMsg,
            Integer responseCode,
            String rateLimitKeyword,
            Integer maxConsecutiveRateLimit,
            Integer backoffSeconds,
            Integer minConcurrency,
            Integer maxConcurrency,
            ProgressListener listener) {

        BatchDetectionResult batchResult = new BatchDetectionResult();
        List<DetectionItemResult> results = Collections.synchronizedList(new ArrayList<>());

        // 默认值
        if (rateLimitKeyword == null || rateLimitKeyword.isEmpty()) {
            rateLimitKeyword = "TOO_MANY_REQUEST";
        }
        if (maxConsecutiveRateLimit == null || maxConsecutiveRateLimit <= 0) {
            maxConsecutiveRateLimit = 5;
        }
        if (backoffSeconds == null || backoffSeconds <= 0) {
            backoffSeconds = 2;
        }
        if (minConcurrency == null || minConcurrency <= 0) {
            minConcurrency = 1;
        }

        // 自适应限流控制
        final String finalRateLimitKeyword = rateLimitKeyword;
        final int finalMaxConsecutiveRateLimit = maxConsecutiveRateLimit;
        final int finalBackoffSeconds = backoffSeconds;
        final int finalMinConcurrency = minConcurrency;
        final int finalMaxConcurrency = (maxConcurrency == null || maxConcurrency <= 0)
                ? tokens.size()
                : Math.min(maxConcurrency, tokens.size());
        AtomicInteger currentConcurrency = new AtomicInteger(Math.max(finalMinConcurrency, finalMaxConcurrency));
        AtomicInteger consecutiveRateLimitCount = new AtomicInteger(0);
        AtomicInteger successStreakCount = new AtomicInteger(0);

        int totalPhones = phones.size();
        AtomicInteger processedCounter = new AtomicInteger(0);

        log.info("[BatchDetection-Enhanced] 开始检测: 手机号{}个, 初始并发={}, token数={}",
                totalPhones, currentConcurrency.get(), tokens.size());

        int phoneIndex = 0;
        while (phoneIndex < totalPhones) {
            // 检查是否应该暂停
            if (listener != null && listener.shouldPause()) {
                log.info("[BatchDetection-Enhanced] 任务已暂停，当前进度: {}/{}", phoneIndex, totalPhones);
                break; // 退出循环，保留当前进度
            }
            
            // 检查是否应该取消
            if (listener != null && listener.shouldCancel()) {
                log.info("[BatchDetection-Enhanced] 任务已取消，当前进度: {}/{}", phoneIndex, totalPhones);
                break; // 退出循环
            }
            
            int concurrency = currentConcurrency.get();
            int endIdx = Math.min(phoneIndex + concurrency, totalPhones);

            log.info("[BatchDetection-Enhanced] 当前轮: 索引{}-{}, 并发={}", phoneIndex, endIdx - 1, concurrency);

            List<java.util.concurrent.Callable<DetectionItemResult>> tasks = new ArrayList<>();

            for (int i = phoneIndex; i < endIdx; i++) {
                final int index = i;
                final String phone = phones.get(index);
                final String token = tokens.get((index - phoneIndex) % tokens.size());

                tasks.add(() -> {
                    Map<String, String> variableValues = buildVariableValues(variableConfig, token, phone);

                    DetectionResult result = executeDetection(
                            requestUrl, requestMethod, requestHeaders, requestBody,
                            variableConfig, variableValues, duplicateMsg, responseCode
                    );

                    DetectionItemResult itemResult = new DetectionItemResult();
                    itemResult.setPhone(phone);
                    itemResult.setToken(maskToken(token));
                    itemResult.setSuccess(result.isSuccess());
                    itemResult.setResponseCode(result.getResponseCode());
                    itemResult.setDuplicate(result.isDuplicate());
                    itemResult.setErrorMessage(result.getErrorMessage());
                    itemResult.setRateLimited(result.isRateLimited());

                    saveDetectionItem(taskId, templateId, phone, token, result);

                    synchronized (results) {
                        results.add(itemResult);
                        int processed = processedCounter.incrementAndGet();

                        if (result.isRateLimited() ||
                                (result.getResponseBody() != null && result.getResponseBody().contains(finalRateLimitKeyword))) {
                            int count = consecutiveRateLimitCount.incrementAndGet();
                            log.warn("[RateLimit] 触发限流! 连续次数={}/{}", count, finalMaxConsecutiveRateLimit);

                            if (count >= finalMaxConsecutiveRateLimit) {
                                int newConcurrency = Math.max(finalMinConcurrency, currentConcurrency.get() - 1);
                                if (newConcurrency < currentConcurrency.get()) {
                                    currentConcurrency.set(newConcurrency);
                                    log.warn("[RateLimit] 降低并发数: {} -> {}", currentConcurrency.get() + 1, newConcurrency);
                                }
                                consecutiveRateLimitCount.set(0);
                            }
                        } else {
                            consecutiveRateLimitCount.set(0);
                            int ok = successStreakCount.incrementAndGet();
                            if (ok >= currentConcurrency.get() * 3 && currentConcurrency.get() < finalMaxConcurrency) {
                                int newConcurrency = Math.min(finalMaxConcurrency, currentConcurrency.get() + 1);
                                if (newConcurrency > currentConcurrency.get()) {
                                    log.info("[RateLimit] 提升并发数: {} -> {}", currentConcurrency.get(), newConcurrency);
                                    currentConcurrency.set(newConcurrency);
                                }
                                successStreakCount.set(0);
                            }
                        }
                    }

                    if (listener != null) {
                        listener.onItemComplete(itemResult, processedCounter.get(), totalPhones);
                    }

                    return itemResult;
                });
            }

            try {
                java.util.concurrent.ExecutorService executor =
                        java.util.concurrent.Executors.newFixedThreadPool(concurrency);
                List<java.util.concurrent.Future<DetectionItemResult>> futures = executor.invokeAll(tasks);
                for (java.util.concurrent.Future<DetectionItemResult> future : futures) {
                    future.get();
                }
                executor.shutdown();
            } catch (Exception e) {
                log.error("[BatchDetection-Enhanced] 执行失败", e);
            }

            phoneIndex = endIdx;
            if (phoneIndex < totalPhones) {
                try {
                    int sleepTime = finalBackoffSeconds * 1000;
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        int duplicateCount = (int) results.stream().filter(DetectionItemResult::isDuplicate).count();
        batchResult.setTotal(totalPhones);
        batchResult.setDuplicateCount(duplicateCount);
        batchResult.setResults(results);

        log.info("[BatchDetection-Enhanced] 全部完成: 总数={}, 已注册={}, 未注册={}, 最终并发={}",
                totalPhones, duplicateCount, totalPhones - duplicateCount, currentConcurrency.get());

        return batchResult;
    }

    private Map<String, String> buildVariableValues(String variableConfig, String token, String phone) {
        Map<String, String> variableValues = new HashMap<>();
        try {
            List<Map<String, String>> configs = objectMapper.readValue(variableConfig, List.class);
            for (Map<String, String> config : configs) {
                String name = config.get("name");
                String location = config.get("location");
                if (name.toLowerCase().contains("auth") || name.toLowerCase().contains("token")) {
                    variableValues.put(name, token);
                } else if (name.toLowerCase().contains("mobile") || name.toLowerCase().contains("phone")) {
                    variableValues.put(name, phone);
                } else {
                    if ("header".equals(location)) {
                        variableValues.put(name, token);
                    } else if ("body".equals(location)) {
                        variableValues.put(name, phone);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析变量配置失败", e);
            variableValues.put("Authorization", token);
            variableValues.put("mobile", phone);
        }
        return variableValues;
    }

    private void saveDetectionItem(String taskId, Long templateId, String phone, String token, DetectionResult result) {
        try {
            DetectionTaskItem item = new DetectionTaskItem();
            item.setTaskId(taskId);
            item.setTemplateId(templateId);
            item.setPhone(phone);
            item.setTokenUsed(maskToken(token));
            item.setResponseCode(result.getResponseCode());
            item.setResponseBody(result.getResponseBody() != null && result.getResponseBody().length() > 500 ?
                    result.getResponseBody().substring(0, 500) : result.getResponseBody());
            item.setIsDuplicate(result.isDuplicate() ? 1 : 0);
            item.setStatus(result.isSuccess() ? "SUCCESS" : "ERROR");
            item.setErrorMessage(result.getErrorMessage());
            item.setIsRateLimited(result.isRateLimited() ? 1 : 0);
            detectionTaskItemMapper.insert(item);
        } catch (Exception e) {
            log.error("[DB] 保存检测结果失败: phone={}", phone, e);
        }
    }

    /**
     * 速率探测：自动发现网站的最优并发数和限流规则
     * 
     * @param requestUrl 请求URL
     * @param requestMethod 请求方法
     * @param requestHeaders 请求头模板
     * @param requestBody 请求体模板
     * @param variableConfig 变量配置
     * @param tokens Token列表
     * @param testPhones 测试手机号列表（建议10-50个）
     * @param duplicateMsg 重复关键字
     * @param responseCode 重复时的状态码
     * @return 速率探测结果
     */
    public RateLimitProfile probeRateLimit(
            String requestUrl,
            String requestMethod,
            String requestHeaders,
            String requestBody,
            String variableConfig,
            List<String> tokens,
            List<String> testPhones,
            String duplicateMsg,
            Integer responseCode) {

        RateLimitProfile profile = new RateLimitProfile();
        profile.setTestedPhones(testPhones.size());
        profile.setTokenCount(tokens.size());
        
        log.info("[RateProbe] 开始速率探测: 测试{}个号码, {}个token", testPhones.size(), tokens.size());

        long startTime = System.currentTimeMillis();
        int rateLimitCount = 0;
        Set<String> detectedKeywords = new HashSet<>();
        List<Long> responseTimes = new ArrayList<>();
        
        // 从并发1开始逐步增加，直到触发限流或达到token数
        int optimalConcurrency = 1;
        boolean rateLimitDetected = false;
        
        for (int concurrency = 1; concurrency <= Math.min(tokens.size(), 20); concurrency++) {
            log.info("[RateProbe] 测试并发={}", concurrency);
            
            // 每个并发级别测试5-10个请求
            int testCount = Math.min(10, testPhones.size());
            int currentRateLimitCount = 0;
            
            for (int i = 0; i < testCount; i++) {
                String phone = testPhones.get(i % testPhones.size());
                String token = tokens.get(i % tokens.size());
                
                long reqStart = System.currentTimeMillis();
                Map<String, String> variableValues = buildVariableValues(variableConfig, token, phone);
                
                DetectionResult result = executeDetection(
                    requestUrl, requestMethod, requestHeaders, requestBody,
                    variableConfig, variableValues, duplicateMsg, responseCode
                );
                
                long reqTime = System.currentTimeMillis() - reqStart;
                responseTimes.add(reqTime);
                
                // 检测限流
                if (result.isRateLimited()) {
                    currentRateLimitCount++;
                    rateLimitCount++;
                    
                    // 提取限流关键字
                    if (result.getResponseBody() != null) {
                        if (result.getResponseBody().contains("TOO_MANY")) {
                            detectedKeywords.add("TOO_MANY_REQUEST");
                        }
                        if (result.getResponseBody().contains("too many")) {
                            detectedKeywords.add("too many");
                        }
                        if (result.getResponseBody().contains("rate limit")) {
                            detectedKeywords.add("rate limit");
                        }
                    }
                    
                    log.warn("[RateProbe] 并发={}时触发限流, 状态码={}", concurrency, result.getResponseCode());
                }
                
                // 添加小延迟避免瞬间过载
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // 如果当前并发级别触发限流次数超过30%，认为已达到限制
            if (currentRateLimitCount >= testCount * 0.3) {
                rateLimitDetected = true;
                optimalConcurrency = Math.max(1, concurrency - 1); // 安全并发数为触发前一级
                log.info("[RateProbe] 检测到限流阈值: 并发={}, 建议安全并发={}", concurrency, optimalConcurrency);
                break;
            } else {
                optimalConcurrency = concurrency; // 更新安全并发数
            }
            
            // 达到token数时停止
            if (concurrency >= tokens.size()) {
                break;
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // 计算平均响应时间
        long avgResponseTime = responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
        
        // 计算建议延迟
        int recommendedDelay = 500; // 默认500ms
        if (rateLimitDetected) {
            recommendedDelay = 1000; // 检测到限流时建议1秒
        } else if (avgResponseTime > 500) {
            recommendedDelay = 300; // 响应慢时减少延迟
        }
        
        // 构建结果
        profile.setRateLimitDetected(rateLimitDetected);
        profile.setOptimalConcurrency(optimalConcurrency);
        profile.setMaxSafeConcurrency(optimalConcurrency);
        profile.setRecommendedMinConcurrency(Math.max(1, optimalConcurrency / 2));
        profile.setAverageResponseTime(avgResponseTime);
        profile.setRecommendedDelay(recommendedDelay);
        profile.setRateLimitCount(rateLimitCount);
        profile.setTotalTestTime(totalTime);
        
        // 设置检测到的限流关键字
        if (!detectedKeywords.isEmpty()) {
            profile.setDetectedRateLimitKeyword(String.join(",", detectedKeywords));
        } else {
            profile.setDetectedRateLimitKeyword("TOO_MANY_REQUEST"); // 默认值
        }
        
        // 生成建议
        StringBuilder recommendation = new StringBuilder();
        recommendation.append("建议配置: ");
        recommendation.append("最大并发=").append(optimalConcurrency);
        recommendation.append(", 最小并发=").append(profile.getRecommendedMinConcurrency());
        recommendation.append(", 延迟=").append(recommendedDelay).append("ms");
        if (rateLimitDetected) {
            recommendation.append(", 限流关键字=").append(profile.getDetectedRateLimitKeyword());
            recommendation.append(", 建议触发阈值=3-5次");
        } else {
            recommendation.append(" (未检测到限流，可适当提高并发)");
        }
        profile.setRecommendation(recommendation.toString());
        
        log.info("[RateProbe] 探测完成: {}", profile.getRecommendation());
        log.info("[RateProbe] 平均响应时间: {}ms, 总耗时: {}ms", avgResponseTime, totalTime);
        
        return profile;
    }
    
    /**
     * 速率探测结果
     */
    public static class RateLimitProfile {
        private boolean rateLimitDetected;           // 是否检测到限流
        private int optimalConcurrency;              // 最优并发数
        private int maxSafeConcurrency;              // 最大安全并发数
        private int recommendedMinConcurrency;       // 建议最小并发数
        private long averageResponseTime;            // 平均响应时间(ms)
        private int recommendedDelay;                // 建议延迟(ms)
        private String detectedRateLimitKeyword;     // 检测到的限流关键字
        private int rateLimitCount;                  // 触发限流次数
        private long totalTestTime;                  // 总测试时间(ms)
        private int testedPhones;                    // 测试号码数
        private int tokenCount;                      // Token数量
        private String recommendation;               // 建议配置说明
        
        // Getters and Setters
        public boolean isRateLimitDetected() { return rateLimitDetected; }
        public void setRateLimitDetected(boolean rateLimitDetected) { this.rateLimitDetected = rateLimitDetected; }
        
        public int getOptimalConcurrency() { return optimalConcurrency; }
        public void setOptimalConcurrency(int optimalConcurrency) { this.optimalConcurrency = optimalConcurrency; }
        
        public int getMaxSafeConcurrency() { return maxSafeConcurrency; }
        public void setMaxSafeConcurrency(int maxSafeConcurrency) { this.maxSafeConcurrency = maxSafeConcurrency; }
        
        public int getRecommendedMinConcurrency() { return recommendedMinConcurrency; }
        public void setRecommendedMinConcurrency(int recommendedMinConcurrency) { this.recommendedMinConcurrency = recommendedMinConcurrency; }
        
        public long getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(long averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public int getRecommendedDelay() { return recommendedDelay; }
        public void setRecommendedDelay(int recommendedDelay) { this.recommendedDelay = recommendedDelay; }
        
        public String getDetectedRateLimitKeyword() { return detectedRateLimitKeyword; }
        public void setDetectedRateLimitKeyword(String detectedRateLimitKeyword) { this.detectedRateLimitKeyword = detectedRateLimitKeyword; }
        
        public int getRateLimitCount() { return rateLimitCount; }
        public void setRateLimitCount(int rateLimitCount) { this.rateLimitCount = rateLimitCount; }
        
        public long getTotalTestTime() { return totalTestTime; }
        public void setTotalTestTime(long totalTestTime) { this.totalTestTime = totalTestTime; }
        
        public int getTestedPhones() { return testedPhones; }
        public void setTestedPhones(int testedPhones) { this.testedPhones = testedPhones; }
        
        public int getTokenCount() { return tokenCount; }
        public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
        
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
    
    /**
     * 根据策略选择token
     */
    private String selectToken(List<String> tokens, int index, String strategy) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Token列表不能为空");
        }
        
        switch (strategy.toLowerCase()) {
            case "round_robin": // 轮询
                return tokens.get(index % tokens.size());
            
            case "random": // 随机
                return tokens.get(new Random().nextInt(tokens.size()));
            
            case "first": // 固定第一个
            default:
                return tokens.get(0);
        }
    }
    
    /**
     * Token脱敏显示（只显示前后各4位）
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
    
    /**
     * 检测结果
     */
    public static class DetectionResult {
        private boolean success;
        private Integer responseCode;
        private String responseBody;
        private boolean duplicate;
        private String errorMessage;
        private boolean rateLimited; // 是否触发限流
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public Integer getResponseCode() { return responseCode; }
        public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
        
        public String getResponseBody() { return responseBody; }
        public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
        
        public boolean isDuplicate() { return duplicate; }
        public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public boolean isRateLimited() { return rateLimited; }
        public void setRateLimited(boolean rateLimited) { this.rateLimited = rateLimited; }
    }
    
    /**
     * 批量检测结果
     */
    public static class BatchDetectionResult {
        private int total;
        private int duplicateCount;
        private List<DetectionItemResult> results;
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public int getDuplicateCount() { return duplicateCount; }
        public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
        
        public List<DetectionItemResult> getResults() { return results; }
        public void setResults(List<DetectionItemResult> results) { this.results = results; }
    }
    
    /**
     * 单个检测项结果
     */
    public static class DetectionItemResult {
        private String phone;
        private String token;
        private boolean success;
        private Integer responseCode;
        private boolean duplicate;
        private String errorMessage;
        private boolean rateLimited; // 是否触发限流
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public Integer getResponseCode() { return responseCode; }
        public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
        
        public boolean isDuplicate() { return duplicate; }
        public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public boolean isRateLimited() { return rateLimited; }
        public void setRateLimited(boolean rateLimited) { this.rateLimited = rateLimited; }
    }
}
