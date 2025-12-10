package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.dto.PostTemplateDTO;
import com.detection.platform.entity.PostTemplate;
import com.detection.platform.service.PostTemplateService;
import com.detection.platform.service.PostRequestParser;
import com.detection.platform.service.PostDetectionService;
import com.detection.platform.vo.PostTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * POST模板Controller
 */
@Slf4j
@RestController
@RequestMapping("/template")
@RequiredArgsConstructor
public class PostTemplateController {
    
    private final PostTemplateService postTemplateService;
    private final PostRequestParser postRequestParser;
    private final PostDetectionService postDetectionService;
    private final com.detection.platform.service.RequestPackageParser requestPackageParser;

    // 检测任务内存管理（后续可替换为Redis/DB持久化）
    private final java.util.concurrent.ExecutorService taskExecutor = java.util.concurrent.Executors.newFixedThreadPool(4);
    private final java.util.concurrent.ConcurrentHashMap<String, TaskInfo> tasks = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, String> latestTaskByTemplate = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger dailyTaskCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile String lastTaskDate = ""; // 记录上一次生成任务的日期

    private static class TaskInfo {
        String taskId;
        Long templateId;
        int total;
        volatile int processed;
        volatile int duplicateCount;
        volatile String status; // PENDING/RUNNING/PAUSED/COMPLETE/ERROR/CANCELLED
        long startTime;
        long endTime;
        volatile boolean cancelled = false; // 取消标记
        volatile boolean paused = false; // 暂停标记（新增）
        java.util.List<java.util.Map<String, Object>> duplicated = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.List<java.util.Map<String, Object>> available = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        String errorMessage;
    }
    
    @GetMapping("/page")
    public Result<Page<PostTemplateVO>> pageTemplates(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String templateName) {
        Page<PostTemplateVO> page = postTemplateService.pageTemplates(current, size, templateName);
        return Result.success(page);
    }
    
    @GetMapping("/list")
    public Result<List<PostTemplateVO>> listAllTemplates() {
        List<PostTemplateVO> list = postTemplateService.listAllTemplates();
        return Result.success(list);
    }
    
    @GetMapping("/{id}")
    public Result<PostTemplateVO> getTemplateById(@PathVariable Long id) {
        PostTemplateVO template = postTemplateService.getTemplateById(id);
        return Result.success(template);
    }
    
    @PostMapping
    public Result<Long> addTemplate(@RequestBody Map<String, Object> params) {
        PostTemplate template = new PostTemplate();
        template.setTemplateName((String) params.get("templateName"));
        template.setTargetSite((String) params.get("targetSite"));
        template.setRequestUrl((String) params.get("requestUrl"));
        template.setRequestMethod((String) params.get("requestMethod"));
        template.setRequestHeaders((String) params.get("requestHeaders"));
        template.setRequestBody((String) params.get("requestBody"));
        template.setVariableConfig((String) params.get("variableConfig")); // 保存变量配置
        template.setDuplicateMsg((String) params.get("duplicateMsg"));
        
        // 状态码：处理null和number类型
        Object responseCode = params.get("responseCode");
        if (responseCode != null) {
            template.setResponseCode(responseCode instanceof Integer ? (Integer) responseCode : Integer.parseInt(responseCode.toString()));
        }
        
        postTemplateService.save(template);
        return Result.success("添加模板成功", template.getId());
    }
    
    @PutMapping
    public Result<Void> updateTemplate(@RequestBody Map<String, Object> params) {
        Long id = params.get("id") != null ? Long.valueOf(params.get("id").toString()) : null;
        if (id == null) {
            return Result.error("模板ID不能为空");
        }
        
        PostTemplate template = postTemplateService.getById(id);
        if (template == null) {
            return Result.error("模板不存在");
        }
        
        // 更新字段
        if (params.containsKey("templateName")) template.setTemplateName((String) params.get("templateName"));
        if (params.containsKey("targetSite")) template.setTargetSite((String) params.get("targetSite"));
        if (params.containsKey("requestUrl")) template.setRequestUrl((String) params.get("requestUrl"));
        if (params.containsKey("requestMethod")) template.setRequestMethod((String) params.get("requestMethod"));
        if (params.containsKey("requestHeaders")) template.setRequestHeaders((String) params.get("requestHeaders"));
        if (params.containsKey("requestBody")) template.setRequestBody((String) params.get("requestBody"));
        if (params.containsKey("variableConfig")) template.setVariableConfig((String) params.get("variableConfig"));
        if (params.containsKey("duplicateMsg")) template.setDuplicateMsg((String) params.get("duplicateMsg"));
        
        if (params.containsKey("responseCode")) {
            Object responseCode = params.get("responseCode");
            template.setResponseCode(responseCode != null ? 
                (responseCode instanceof Integer ? (Integer) responseCode : Integer.parseInt(responseCode.toString())) : null);
        }
        
        postTemplateService.updateById(template);
        return Result.successMsg("更新模板成功");
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        postTemplateService.deleteTemplate(id);
        return Result.successMsg("删除模板成功");
    }
    
    @PostMapping("/{id}/test")
    public Result<Boolean> testTemplate(@PathVariable Long id, @RequestBody String testData) {
        Boolean result = postTemplateService.testTemplate(id, testData);
        return Result.success("测试完成", result);
    }
    
    /**
     * 解析原始POST请求，返回所有可用参数供用户选择
     */
    @PostMapping("/parse")
    public Result<java.util.Map<String, Object>> parseRawRequest(@RequestBody java.util.Map<String, String> params) {
        String rawRequest = params.get("rawRequest");
        
        // 解析原始请求
        java.util.Map<String, Object> parseResult = postRequestParser.parseRawRequest(rawRequest);
        
        // 返回解析结果（包含所有参数和建议的变量）
        return Result.success("解析成功", parseResult);
    }
    
    /**
     * 导入HTTP请求包（新功能）
     * 支持从浏览器开发者工具复制的完整请求包
     */
    @PostMapping("/import-request")
    public Result<com.detection.platform.service.RequestPackageParser.ParseResult> importRequest(
            @RequestBody Map<String, String> params) {
        String rawRequest = params.get("rawRequest");
        
        if (rawRequest == null || rawRequest.trim().isEmpty()) {
            return Result.error("请求包内容不能为空");
        }
        
        // 使用新的请求包解析器
        com.detection.platform.service.RequestPackageParser.ParseResult result = 
            requestPackageParser.parseRawRequest(rawRequest);
        
        if (!result.isSuccess()) {
            return Result.error(result.getErrorMessage());
        }
        
        return Result.success("解析成功，发现 " + result.getVariables().size() + " 个变量", result);
    }
    
    /**
     * 根据用户指定的变量生成模板
     */
    @PostMapping("/generate")
    public Result<java.util.Map<String, Object>> generateTemplate(@RequestBody java.util.Map<String, Object> params) {
        java.util.Map<String, Object> parseResult = (java.util.Map<String, Object>) params.get("parseResult");
        String templateName = (String) params.get("templateName");
        java.util.List<Map<String, Object>> manualVariables = (java.util.List<Map<String, Object>>) params.get("manualVariables");
        Map<String, Object> detectionConfig = (Map<String, Object>) params.get("detectionConfig");
        
        // 将手动指定的变量转换为解析键列表（如 header.Authorization / body.mobile）
        java.util.List<String> selectedVariables = new java.util.ArrayList<>();
        if (manualVariables != null) {
            for (Map<String, Object> var : manualVariables) {
                String name = (String) var.get("name");
                String location = (String) var.get("location");
                if (name != null && !name.trim().isEmpty() && ("header".equals(location) || "body".equals(location))) {
                    selectedVariables.add(location + "." + name.trim());
                }
            }
        }
        
        // 生成模板
        java.util.Map<String, Object> template = postRequestParser.convertToTemplate(
            parseResult, templateName, selectedVariables);
        
        // 处理检测条件（单一条件：状态码可选 + 关键字必填）
        if (detectionConfig != null) {
            Object statusCode = detectionConfig.get("statusCode");
            String keyword = (String) detectionConfig.get("keyword");
            
            // 状态码可选，如果设置了则保存
            if (statusCode != null) {
                template.put("responseCode", statusCode);
            } else {
                template.put("responseCode", null);
            }
            
            // 关键字必填
            if (keyword != null && !keyword.trim().isEmpty()) {
                template.put("duplicateMsg", keyword.trim());
            }
        }
        
        return Result.success("生成模板成功", template);
    }
    
    /**
     * 批量检测接口（智能分配token）
     * 请求体格式：
     * {
     *   "templateId": 1,
     *   "tokens": ["token1", "token2", "token3"],
     *   "phones": ["13800138000", "13800138001", ...],
     *   "strategy": "round_robin"  // 可选: round_robin(轮询), random(随机), first(固定)
     * }
     */
    @PostMapping("/detect")
    public Result<Map<String, Object>> batchDetect(@RequestBody Map<String, Object> params) {
        Long templateId = Long.valueOf(String.valueOf(params.get("templateId")));
        List<String> tokens = (List<String>) params.get("tokens");
        List<String> phones = (List<String>) params.get("phones");
        String strategy = (String) params.getOrDefault("strategy", "round_robin");
        
        // 获取模板
        PostTemplate template = postTemplateService.getById(templateId);
        if (template == null) {
            return Result.error("模板不存在");
        }
        
        // 执行批量检测
        PostDetectionService.BatchDetectionResult batchResult = postDetectionService.batchDetection(
            template.getRequestUrl(),
            template.getRequestMethod(),
            template.getRequestHeaders(),
            template.getRequestBody(),
            template.getVariableConfig(),
            tokens,
            phones,
            template.getDuplicateMsg(),
            template.getResponseCode(),
            strategy
        );
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("total", batchResult.getTotal());
        response.put("duplicateCount", batchResult.getDuplicateCount());
        
        // 分类结果
        List<Map<String, Object>> duplicatedList = new ArrayList<>();
        List<Map<String, Object>> availableList = new ArrayList<>();
        
        for (PostDetectionService.DetectionItemResult item : batchResult.getResults()) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("phone", item.getPhone());
            itemMap.put("token", item.getToken());
            itemMap.put("responseCode", item.getResponseCode());
            itemMap.put("success", item.isSuccess());
            itemMap.put("errorMessage", item.getErrorMessage());
            
            if (item.isDuplicate()) {
                duplicatedList.add(itemMap);
            } else {
                availableList.add(itemMap);
            }
        }
        
        response.put("duplicated", duplicatedList);
        response.put("available", availableList);
        
        return Result.success("检测完成", response);
    }

    /**
     * 启动异步批量检测任务
     * 返回 taskId，前端可轮询进度与结果
     * 
     * 请求参数:
     * - templateId: 模板ID (必填)
     * - taskId: 任务ID (可选，如果提供则使用该任务ID，否则生成新ID)
     * - tokens: Token列表 (必填)
     * - phones: 手机号列表 (必填)
     * - strategy: 分配策略 (可选，默认round_robin)
     */
    @PostMapping("/detect/start")
    public Result<Map<String, Object>> startDetect(@RequestBody Map<String, Object> params) {
        Long templateId = Long.valueOf(String.valueOf(params.get("templateId")));
        List<String> tokens = (List<String>) params.get("tokens");
        List<String> phones = (List<String>) params.get("phones");
        String strategy = (String) params.getOrDefault("strategy", "round_robin");
        String providedTaskId = (String) params.get("taskId"); // 前端传入的任务ID

        PostTemplate template = postTemplateService.getById(templateId);
        if (template == null) {
            return Result.error("模板不存在");
        }
        if (tokens == null || tokens.isEmpty()) {
            return Result.error("Token列表不能为空");
        }
        if (phones == null || phones.isEmpty()) {
            return Result.error("手机号列表不能为空");
        }

        // 使用提供的taskId或已存在的任务，否则生成新UUID
        final String taskId;
        final TaskInfo info;
        
        if (providedTaskId != null && !providedTaskId.isEmpty()) {
            // 检查任务是否已存在
            TaskInfo existingInfo = tasks.get(providedTaskId);
            if (existingInfo != null) {
                // 任务已存在，重置并重新运行
                log.info("📌 [POST模板] 使用已存在任务ID: {}, 状态: {} -> RUNNING", providedTaskId, existingInfo.status);
                taskId = providedTaskId;
                info = existingInfo;
                // 重置任务数据
                info.total = phones.size();
                info.processed = 0;
                info.duplicateCount = 0;
                info.status = "RUNNING";
                info.startTime = System.currentTimeMillis();
                info.endTime = 0;
                info.cancelled = false;
                info.paused = false;
                info.duplicated.clear();
                info.available.clear();
                info.errorMessage = null;
            } else {
                // 任务不存在，创建新任务
                log.info("📌 [POST模板] 使用前端提供的任务ID创建新任务: {}", providedTaskId);
                taskId = providedTaskId;
                info = new TaskInfo();
                info.taskId = taskId;
                info.templateId = templateId;
                info.total = phones.size();
                info.processed = 0;
                info.duplicateCount = 0;
                info.status = "RUNNING";
                info.startTime = System.currentTimeMillis();
                tasks.put(taskId, info);
            }
        } else {
            // 未提供taskId，生成新UUID
            taskId = java.util.UUID.randomUUID().toString();
            log.info("📌 [POST模板] 生成新的UUID任务ID: {}", taskId);
            info = new TaskInfo();
            info.taskId = taskId;
            info.templateId = templateId;
            info.total = phones.size();
            info.processed = 0;
            info.duplicateCount = 0;
            info.status = "RUNNING";
            info.startTime = System.currentTimeMillis();
            tasks.put(taskId, info);
        }
        
        latestTaskByTemplate.put(templateId, taskId);
        log.info("🚀 [POST模板] 启动检测任务 taskId={}, templateId={}, total={}, tokens={}", 
                taskId, templateId, phones.size(), tokens.size());

        taskExecutor.submit(() -> {
            try {
                // 获取模板的限流配置
                String rateLimitKeyword = template.getRateLimitKeyword();
                Integer maxConsecutiveRateLimit = template.getMaxConsecutiveRateLimit();
                Integer backoffSeconds = template.getBackoffSeconds();
                Integer minConcurrency = template.getMinConcurrency();
                
                // 使用增强版批量检测（自适应限流）
                postDetectionService.batchDetectionWithAdaptiveRateLimit(
                    taskId,
                    templateId,
                    template.getRequestUrl(),
                    template.getRequestMethod(),
                    template.getRequestHeaders(),
                    template.getRequestBody(),
                    template.getVariableConfig(),
                    tokens,
                    phones,
                    template.getDuplicateMsg(),
                    template.getResponseCode(),
                    rateLimitKeyword,
                    maxConsecutiveRateLimit,
                    backoffSeconds,
                    minConcurrency,
                    template.getMaxConcurrency(),
                    new PostDetectionService.ProgressListener() {
                        @Override
                        public void onItemComplete(PostDetectionService.DetectionItemResult item, int processed, int total) {
                            info.processed = processed;
                            java.util.Map<String, Object> itemMap = new java.util.HashMap<>();
                            itemMap.put("phone", item.getPhone());
                            itemMap.put("token", item.getToken());
                            itemMap.put("responseCode", item.getResponseCode());
                            itemMap.put("success", item.isSuccess());
                            itemMap.put("errorMessage", item.getErrorMessage());
                            if (item.isDuplicate()) {
                                info.duplicated.add(itemMap);
                                info.duplicateCount++;
                            } else {
                                info.available.add(itemMap);
                            }
                        }
                        
                        @Override
                        public boolean shouldPause() {
                            return info.paused;
                        }
                        
                        @Override
                        public boolean shouldCancel() {
                            return info.cancelled;
                        }
                    }
                );
                // 检查是否被暂停或取消
                if (info.paused) {
                    info.status = "PAUSED";
                } else if (info.cancelled) {
                    info.status = "CANCELLED";
                } else {
                    info.status = "COMPLETE";
                }
                info.endTime = System.currentTimeMillis();
            } catch (Exception e) {
                info.status = "ERROR";
                info.errorMessage = e.getMessage();
            }
        });

        Map<String, Object> resp = new HashMap<>();
        resp.put("taskId", taskId);
        resp.put("total", info.total);
        return Result.success("任务已启动", resp);
    }

    /**
     * 轮询任务进度
     */
    @GetMapping("/detect/status/{taskId}")
    public Result<Map<String, Object>> detectStatus(@PathVariable String taskId) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("taskId", taskId);
        resp.put("status", info.status);
        resp.put("total", info.total);
        resp.put("processed", info.processed);
        resp.put("duplicateCount", info.duplicateCount);
        resp.put("startTime", info.startTime);
        resp.put("endTime", info.endTime);
        return Result.success("查询成功", resp);
    }

    /**
     * 拉取任务结果（可分页）
     */
    @GetMapping("/detect/result/{taskId}")
    public Result<Map<String, Object>> detectResult(@PathVariable String taskId,
                                                    @RequestParam(defaultValue = "0") Integer offset,
                                                    @RequestParam(defaultValue = "500") Integer limit) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", info.status);
        resp.put("total", info.total);
        resp.put("processed", info.processed);
        resp.put("duplicateCount", info.duplicateCount);

        // 简单分页
        java.util.List<java.util.Map<String, Object>> dupSlice = info.duplicated.subList(
            Math.min(offset, info.duplicated.size()),
            Math.min(offset + limit, info.duplicated.size())
        );
        java.util.List<java.util.Map<String, Object>> availSlice = info.available.subList(
            Math.min(offset, info.available.size()),
            Math.min(offset + limit, info.available.size())
        );
        resp.put("duplicated", dupSlice);
        resp.put("available", availSlice);
        return Result.success("查询成功", resp);
    }

    /**
     * 查询模板的最近一次检测任务
     */
    @GetMapping("/detect/latest/{templateId}")
    public Result<Map<String, Object>> latestDetect(@PathVariable Long templateId) {
        String taskId = latestTaskByTemplate.get(templateId);
        Map<String, Object> resp = new HashMap<>();
        if (taskId == null) {
            resp.put("taskId", null);
            resp.put("status", "PENDING");
            resp.put("total", 0);
            resp.put("processed", 0);
            resp.put("duplicateCount", 0);
            return Result.success("暂无任务", resp);
        }
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            resp.put("taskId", taskId);
            resp.put("status", "PENDING");
            resp.put("total", 0);
            resp.put("processed", 0);
            resp.put("duplicateCount", 0);
            return Result.success("任务不存在或已清理", resp);
        }
        resp.put("taskId", taskId);
        resp.put("status", info.status);
        resp.put("total", info.total);
        resp.put("processed", info.processed);
        resp.put("duplicateCount", info.duplicateCount);
        resp.put("startTime", info.startTime);
        resp.put("endTime", info.endTime);
        return Result.success("查询成功", resp);
    }

    /**
     * 导出任务结果为CSV（返回Base64字符串，前端自行下载）
     */
    @GetMapping("/detect/export/{taskId}")
    public Result<Map<String, Object>> exportDetect(@PathVariable String taskId) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("phone,statusCode,token,isDuplicate\n");
        info.duplicated.forEach(m -> sb.append(m.get("phone")).append(',')
                .append(m.get("responseCode")).append(',')
                .append(m.get("token")).append(',')
                .append('1').append('\n'));
        info.available.forEach(m -> sb.append(m.get("phone")).append(',')
                .append(m.get("responseCode")).append(',')
                .append(m.get("token")).append(',')
                .append('0').append('\n'));
        String base64 = java.util.Base64.getEncoder().encodeToString(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, Object> resp = new HashMap<>();
        resp.put("filename", "detect_" + taskId + ".csv");
        resp.put("content", base64);
        return Result.success("导出成功", resp);
    }
    
    /**
     * 导出已注册号码（仅手机号，TXT格式）
     */
    @GetMapping("/detect/export/registered/{taskId}")
    public Result<Map<String, Object>> exportRegistered(@PathVariable String taskId) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        
        // 构建包含已注册数据的响应
        Map<String, Object> resp = new HashMap<>();
        resp.put("duplicated", info.duplicated);
        resp.put("available", new ArrayList<>()); // 空列表
        return Result.success("导出成功", resp);
    }
    
    /**
     * 导出未注册号码（仅手机号，TXT格式）
     */
    @GetMapping("/detect/export/unregistered/{taskId}")
    public Result<Map<String, Object>> exportUnregistered(@PathVariable String taskId) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        
        // 构建包含未注册数据的响应
        Map<String, Object> resp = new HashMap<>();
        resp.put("duplicated", new ArrayList<>()); // 空列表
        resp.put("available", info.available);
        return Result.success("导出成功", resp);
    }
    
    /**
     * 创建新任务（PENDING状态）
     */
    @PostMapping("/detect/create-task")
    public Result<Map<String, Object>> createTask(@RequestBody Map<String, Object> params) {
        Long templateId = Long.valueOf(String.valueOf(params.get("templateId")));
        String templateName = (String) params.get("templateName");
        String targetSite = (String) params.get("targetSite");
        
        // 验证模板是否存在
        PostTemplate template = postTemplateService.getById(templateId);
        if (template == null) {
            return Result.error("模板不存在");
        }
        
        // 生成任务ID：格式为 {日期(月日)}-{序号}
        String currentDate = new java.text.SimpleDateFormat("MMdd").format(new java.util.Date());
        
        // 如果日期变化，重置计数器
        synchronized (this) {
            if (!currentDate.equals(lastTaskDate)) {
                lastTaskDate = currentDate;
                dailyTaskCounter.set(0);
            }
        }
        
        // 生成序号（递增）
        int sequence = dailyTaskCounter.incrementAndGet();
        String taskId = String.format("%s-%d", currentDate, sequence);
        
        // 创建任务信息
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.taskId = taskId;
        taskInfo.templateId = templateId;
        taskInfo.total = 0; // 初始为0，上传文件后更新
        taskInfo.processed = 0;
        taskInfo.duplicateCount = 0;
        taskInfo.status = "PENDING";
        taskInfo.startTime = System.currentTimeMillis();
        taskInfo.endTime = 0;
        
        // 保存到内存
        tasks.put(taskId, taskInfo);
        latestTaskByTemplate.put(templateId, taskId);
        
        // 返回任务信息
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("templateId", templateId);
        result.put("templateName", templateName);
        result.put("targetSite", targetSite);
        result.put("status", "PENDING");
        
        return Result.success("任务创建成功", result);
    }
    
    /**
     * 暂停正在运行的任务
     */
    @PostMapping("/detect/pause/{taskId}")
    public Result<Void> pauseTask(@PathVariable String taskId) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        
        if (!"RUNNING".equals(info.status)) {
            return Result.error("只有运行中的任务才能暂停");
        }
        
        // 设置暂停标记
        info.paused = true;
        info.status = "PAUSED";
        
        return Result.successMsg("任务已暂停");
    }
    
    /**
     * 继续运行已暂停的任务
     */
    @PostMapping("/detect/resume/{taskId}")
    public Result<Void> resumeTask(@PathVariable String taskId) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        
        if (!"PAUSED".equals(info.status)) {
            return Result.error("只有已暂停的任务才能继续运行");
        }
        
        // 取消暂停标记，重新启动任务
        info.paused = false;
        info.status = "RUNNING";
        
        // 重新提交到线程池继续执行
        taskExecutor.submit(() -> {
            try {
                // 获取模板信息
                PostTemplate template = postTemplateService.getById(info.templateId);
                if (template == null) {
                    info.status = "ERROR";
                    info.errorMessage = "模板不存在";
                    return;
                }
                
                // TODO: 这里需要从任务中恢复 tokens 和 phones 数据
                // 暂时留空，实际需要持久化存储
                
            } catch (Exception e) {
                info.status = "ERROR";
                info.errorMessage = e.getMessage();
                info.endTime = System.currentTimeMillis();
            }
        });
        
        return Result.successMsg("任务已继续运行");
    }
    
    /**
     * 停止/取消正在运行的检测任务
     */
    @PostMapping("/detect/stop/{taskId}")
    public Result<Void> stopDetect(@PathVariable String taskId) {
        TaskInfo info = tasks.get(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        
        if ("COMPLETE".equals(info.status) || "ERROR".equals(info.status) || "CANCELLED".equals(info.status)) {
            return Result.error("任务已结束，无法停止");
        }
        
        // 设置取消标记
        info.cancelled = true;
        info.status = "CANCELLED";
        info.endTime = System.currentTimeMillis();
        
        return Result.successMsg("任务已停止");
    }
    
    /**
     * 获取所有任务列表（历史任务 + 运行中任务）
     */
    @GetMapping("/detect/tasks")
    public Result<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        List<Map<String, Object>> taskList = new ArrayList<>();
        
        // 遍历所有任务
        for (Map.Entry<String, TaskInfo> entry : tasks.entrySet()) {
            TaskInfo info = entry.getValue();
            
            // 获取模板信息
            PostTemplate template = postTemplateService.getById(info.templateId);
            String templateName = template != null ? template.getTemplateName() : "未知模板";
            String targetSite = template != null ? template.getTargetSite() : "";
            
            Map<String, Object> task = new HashMap<>();
            task.put("taskId", info.taskId);
            task.put("templateId", info.templateId);
            task.put("templateName", templateName);
            task.put("targetSite", targetSite);
            task.put("status", info.status);
            task.put("total", info.total);
            task.put("processed", info.processed);
            task.put("duplicateCount", info.duplicateCount);
            task.put("startTime", info.startTime);
            task.put("endTime", info.endTime);
            task.put("errorMessage", info.errorMessage);
            
            taskList.add(task);
        }
        
        // 按开始时间降序排列
        taskList.sort((a, b) -> Long.compare(
            (Long) b.get("startTime"),
            (Long) a.get("startTime")
        ));
        
        // 简单分页（内存分页）
        int start = (current - 1) * size;
        int end = Math.min(start + size, taskList.size());
        List<Map<String, Object>> pagedTasks = start < taskList.size() ? 
            taskList.subList(start, end) : new ArrayList<>();
        
        // 构建分页结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", pagedTasks);
        result.put("total", taskList.size());
        result.put("current", current);
        result.put("size", size);
        
        return Result.success(result);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/detect/task/{taskId}")
    public Result<Void> deleteTask(@PathVariable String taskId) {
        TaskInfo info = tasks.remove(taskId);
        if (info == null) {
            return Result.error("任务不存在");
        }
        
        // 如果是某个模板的最新任务，也删除
        latestTaskByTemplate.remove(info.templateId, taskId);
        
        return Result.successMsg("删除成功");
    }

    /**
     * 速率探测接口：自动检测网站的最优并发数和限流规则
     * 请求体格式：
     * {
     *   "templateId": 1,
     *   "tokens": ["token1", "token2", ...],
     *   "testPhones": ["13800138000", "13800138001", ...],  // 10-50个测试号码
     *   "autoApply": true  // 是否自动应用到模板
     * }
     */
    @PostMapping("/detect/probe")
    public Result<Map<String, Object>> probeRateLimit(@RequestBody Map<String, Object> params) {
        Long templateId = Long.valueOf(String.valueOf(params.get("templateId")));
        List<String> tokens = (List<String>) params.get("tokens");
        List<String> testPhones = (List<String>) params.get("testPhones");
        Boolean autoApply = (Boolean) params.getOrDefault("autoApply", false);
        
        // 验证参数
        if (tokens == null || tokens.isEmpty()) {
            return Result.error("Token列表不能为空");
        }
        if (testPhones == null || testPhones.size() < 5) {
            return Result.error("测试手机号至少需要05个");
        }
        if (testPhones.size() > 100) {
            return Result.error("测试手机号最多100个");
        }
        
        // 获取模板
        PostTemplate template = postTemplateService.getById(templateId);
        if (template == null) {
            return Result.error("模板不存在");
        }
        
        // 执行速率探测
        PostDetectionService.RateLimitProfile profile = postDetectionService.probeRateLimit(
            template.getRequestUrl(),
            template.getRequestMethod(),
            template.getRequestHeaders(),
            template.getRequestBody(),
            template.getVariableConfig(),
            tokens,
            testPhones,
            template.getDuplicateMsg(),
            template.getResponseCode()
        );
        
        // 如果选择自动应用，更新模板配置
        if (autoApply) {
            template.setMaxConcurrency(profile.getMaxSafeConcurrency());
            template.setMinConcurrency(profile.getRecommendedMinConcurrency());
            template.setRateLimitKeyword(profile.getDetectedRateLimitKeyword());
            template.setBackoffSeconds(profile.getRecommendedDelay() / 1000); // 转换为秒
            template.setMaxConsecutiveRateLimit(profile.isRateLimitDetected() ? 3 : 5);
            postTemplateService.updateById(template);
        }
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("rateLimitDetected", profile.isRateLimitDetected());
        response.put("optimalConcurrency", profile.getOptimalConcurrency());
        response.put("maxSafeConcurrency", profile.getMaxSafeConcurrency());
        response.put("recommendedMinConcurrency", profile.getRecommendedMinConcurrency());
        response.put("averageResponseTime", profile.getAverageResponseTime());
        response.put("recommendedDelay", profile.getRecommendedDelay());
        response.put("detectedRateLimitKeyword", profile.getDetectedRateLimitKeyword());
        response.put("rateLimitCount", profile.getRateLimitCount());
        response.put("totalTestTime", profile.getTotalTestTime());
        response.put("testedPhones", profile.getTestedPhones());
        response.put("tokenCount", profile.getTokenCount());
        response.put("recommendation", profile.getRecommendation());
        response.put("autoApplied", autoApply);
        
        // 计算预估速率
        if (profile.getAverageResponseTime() > 0) {
            // 预估速率 = 并发数 / (平均响应时间 + 延迟)秒
            double estimatedRate = (double) profile.getOptimalConcurrency() / 
                ((profile.getAverageResponseTime() + profile.getRecommendedDelay()) / 1000.0);
            response.put("estimatedRate", String.format("%.2f个/秒", estimatedRate));
            
            // 预估完成1万个手机号需要的时间
            double estimatedTimeFor10k = 10000 / estimatedRate;
            int minutes = (int) (estimatedTimeFor10k / 60);
            int seconds = (int) (estimatedTimeFor10k % 60);
            response.put("estimatedTimeFor10k", String.format("%d分%d秒", minutes, seconds));
        }
        
        return Result.success("速率探测完成", response);
    }

}
