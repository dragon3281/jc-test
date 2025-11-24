package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.entity.RegisterTask;
import com.detection.platform.entity.WebsiteAnalysis;
import com.detection.platform.service.RegisterTaskService;
import com.detection.platform.service.WebsiteAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务中心Controller
 * 包含网站分析和自动化注册功能
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessController {

    private final WebsiteAnalysisService websiteAnalysisService;
    private final RegisterTaskService registerTaskService;

    // ==================== 网站分析相关接口 ====================

    /**
     * 分页查询网站分析列表
     */
    @GetMapping("/analysis/list")
    public Result<Page<WebsiteAnalysis>> pageAnalysis(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String websiteUrl,
            @RequestParam(required = false) Integer status) {
        Page<WebsiteAnalysis> page = websiteAnalysisService.pageAnalysis(pageNum, pageSize, websiteUrl, status);
        return Result.success(page);
    }

    /**
     * 根据ID获取分析详情
     */
    @GetMapping("/analysis/{id}")
    public Result<WebsiteAnalysis> getAnalysisById(@PathVariable Long id) {
        WebsiteAnalysis analysis = websiteAnalysisService.getAnalysisById(id);
        return Result.success(analysis);
    }

    /**
     * 启动网站分析
     */
    @PostMapping("/analysis/start")
    public Result<Long> startAnalysis(@RequestBody Map<String, Object> params) {
        Long id = websiteAnalysisService.startAnalysis(params);
        return Result.success("分析任务已启动", id);
    }

    /**
     * 根据分析结果生成POST模板
     */
    @PostMapping("/analysis/generate-template")
    public Result<Long> generateTemplate(@RequestBody Map<String, Object> params) {
        Long analysisId = Long.valueOf(params.get("analysisId").toString());
        Long templateId = websiteAnalysisService.generateTemplate(analysisId);
        return Result.success("模板生成成功", templateId);
    }

    /**
     * 删除分析记录
     */
    @DeleteMapping("/analysis/{id}")
    public Result<Void> deleteAnalysis(@PathVariable Long id) {
        websiteAnalysisService.deleteAnalysis(id);
        return Result.successMsg("删除成功");
    }

    // ==================== 自动化注册相关接口 ====================

    /**
     * 分页查询注册任务列表
     */
    @GetMapping("/register/list")
    public Result<Page<RegisterTask>> pageRegisterTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) Integer status) {
        Page<RegisterTask> page = registerTaskService.pageRegisterTasks(pageNum, pageSize, taskName, status);
        return Result.success(page);
    }

    /**
     * 根据ID获取任务详情
     */
    @GetMapping("/register/{id}")
    public Result<RegisterTask> getRegisterTaskById(@PathVariable Long id) {
        RegisterTask task = registerTaskService.getTaskById(id);
        return Result.success(task);
    }

    /**
     * 创建注册任务
     */
    @PostMapping("/register/create")
    public Result<Long> createRegisterTask(@RequestBody Map<String, Object> params) {
        Long id = registerTaskService.createTask(params);
        return Result.success("注册任务创建成功", id);
    }

    /**
     * 启动任务
     */
    @PostMapping("/register/start/{id}")
    public Result<Void> startRegisterTask(@PathVariable Long id) {
        registerTaskService.startTask(id);
        return Result.successMsg("任务已启动");
    }

    /**
     * 暂停任务
     */
    @PostMapping("/register/pause/{id}")
    public Result<Void> pauseRegisterTask(@PathVariable Long id) {
        registerTaskService.pauseTask(id);
        return Result.successMsg("任务已暂停");
    }

    /**
     * 继续任务
     */
    @PostMapping("/register/resume/{id}")
    public Result<Void> resumeRegisterTask(@PathVariable Long id) {
        registerTaskService.resumeTask(id);
        return Result.successMsg("任务已继续");
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/register/{id}")
    public Result<Void> deleteRegisterTask(@PathVariable Long id) {
        registerTaskService.deleteTask(id);
        return Result.successMsg("删除成功");
    }

    /**
     * 获取任务的注册结果
     */
    @GetMapping("/register/results/{id}")
    public Result<List<Map<String, Object>>> getRegisterTaskResults(@PathVariable Long id) {
        List<Map<String, Object>> results = registerTaskService.getTaskResults(id);
        return Result.success(results);
    }
}
