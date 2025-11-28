package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.entity.RegisterTask;
import com.detection.platform.entity.RegisterTemplateDraft;
import com.detection.platform.entity.WebsiteAnalysis;
import com.detection.platform.service.RegisterTaskService;
import com.detection.platform.service.WebsiteAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class BusinessController {

    private final WebsiteAnalysisService websiteAnalysisService;
    private final RegisterTaskService registerTaskService;
    private final com.detection.platform.service.RegisterTemplateService registerTemplateService;
    private final com.detection.platform.service.EncryptionExecutorService encryptionExecutorService;
    private final com.detection.platform.service.RegisterTemplateDraftService draftService;

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
        log.info("======== 获取分析详情开始 ======== ID={}", id);
        WebsiteAnalysis analysis = websiteAnalysisService.getAnalysisById(id);
        log.info("查询到的分析记录: ID={}, websiteUrl={}, status={}", 
                 analysis != null ? analysis.getId() : null,
                 analysis != null ? analysis.getWebsiteUrl() : null,
                 analysis != null ? analysis.getAnalysisStatus() : null);
        log.info("======== 获取分析详情结束 ========");
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
     * 从已完成任务添加注册模板
     */
    @PostMapping("/register/template/add-from-task/{taskId}")
    public Result<Long> addTemplateFromTask(@PathVariable Long taskId, @RequestBody(required = false) Map<String, Object> body) {
        String templateName = body != null && body.get("templateName") != null ? String.valueOf(body.get("templateName")) : null;
        String notes = body != null && body.get("notes") != null ? String.valueOf(body.get("notes")) : null;
        Long id = registerTemplateService.addFromTask(taskId, templateName, notes);
        return Result.success("模板已创建", id);
    }

    /**
     * 模板分页列表
     */
    @GetMapping("/register/template/page")
    public Result<Page<com.detection.platform.entity.RegisterTemplate>> pageRegisterTemplates(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<com.detection.platform.entity.RegisterTemplate> page = registerTemplateService.pageTemplates(pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 模板详情
     */
    @GetMapping("/register/template/{id}")
    public Result<com.detection.platform.entity.RegisterTemplate> getRegisterTemplate(@PathVariable Long id) {
        return Result.success(registerTemplateService.getById(id));
    }

    /**
     * 模板列表（用于下拉选择）
     */
    @GetMapping("/register/template/list")
    public Result<List<com.detection.platform.entity.RegisterTemplate>> listRegisterTemplates() {
        return Result.success(registerTemplateService.listAll());
    }

    /**
     * 删除模板（兼容环境限制，提供POST删除）
     */
    @PostMapping("/register/template/delete/{id}")
    public Result<Void> deleteRegisterTemplatePost(@PathVariable Long id) {
        log.info("删除注册模板, id={}", id);
        boolean ok = registerTemplateService.deleteById(id);
        log.info("删除结果: {}", ok);
        return ok ? Result.successMsg("删除成功") : Result.error("删除失败");
    }

    /**
     * 删除模板（支持DELETE方法）
     */
    @DeleteMapping("/register/template/{id}")
    public Result<Void> deleteRegisterTemplate(@PathVariable Long id) {
        log.info("DELETE 删除注册模板, id={}", id);
        boolean ok = registerTemplateService.deleteById(id);
        log.info("删除结果: {}", ok);
        return ok ? Result.successMsg("删除成功") : Result.error("删除失败");
    }

    /**
     * 获取任务的注册结果
     */
    @GetMapping("/register/results/{id}")
    public Result<List<Map<String, Object>>> getRegisterTaskResults(@PathVariable Long id) {
        List<Map<String, Object>> results = registerTaskService.getTaskResults(id);
        return Result.success(results);
    }
    
    // ==================== 执行器管理相关接口 ====================
    
    /**
     * 获取所有执行器列表
     */
    @GetMapping("/executor/list")
    public Result<List<com.detection.platform.entity.EncryptionExecutor>> listExecutors() {
        return Result.success(encryptionExecutorService.listAll());
    }
    
    /**
     * 获取内置执行器列表
     */
    @GetMapping("/executor/builtin")
    public Result<List<com.detection.platform.entity.EncryptionExecutor>> listBuiltinExecutors() {
        return Result.success(encryptionExecutorService.listBuiltin());
    }
    
    /**
     * 获取执行器详情
     */
    @GetMapping("/executor/{id}")
    public Result<com.detection.platform.entity.EncryptionExecutor> getExecutor(@PathVariable Long id) {
        return Result.success(encryptionExecutorService.getById(id));
    }
    
    /**
     * 创建自定义执行器（上传脚本）
     */
    @PostMapping("/executor/create")
    public Result<Long> createExecutor(@RequestBody com.detection.platform.entity.EncryptionExecutor executor) {
        Long id = encryptionExecutorService.createCustomExecutor(executor);
        return Result.success("执行器创建成功", id);
    }
    
    // ==================== 草稿箱管理相关接口 ====================
    
    /**
     * 上传脚本并保存到草稿箱
     */
    @PostMapping("/draft/upload")
    public Result<Long> uploadDraft(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("draftName") String draftName,
            @RequestParam("websiteUrl") String websiteUrl,
            @RequestParam(required = false) String description) {
        try {
            // 读取脚本内容
            String scriptContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            
            // 创建草稿
            RegisterTemplateDraft draft = new RegisterTemplateDraft();
            draft.setDraftName(draftName);
            draft.setWebsiteUrl(websiteUrl);
            draft.setExecutorScript(scriptContent); // 使用executorScript字段
            draft.setAutoNotes(description); // 使用autoNotes字段
            draft.setTestResult(0); // 0未测试
            
            Long id = draftService.createDraft(draft);
            return Result.success("脚本已上传到草稿箱", id);
        } catch (Exception e) {
            log.error("上传草稿失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 草稿箱分页列表（带查询条件）
     */
    @GetMapping("/draft/list")
    public Result<Page<RegisterTemplateDraft>> listDrafts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String draftName,
            @RequestParam(required = false) String websiteUrl) {
        Page<RegisterTemplateDraft> page = draftService.listDrafts(pageNum, pageSize, draftName, websiteUrl);
        return Result.success(page);
    }
    
    /**
     * 创建草稿
     */
    @PostMapping("/draft/create")
    public Result<Long> createDraft(@RequestBody RegisterTemplateDraft draft) {
        Long id = draftService.createDraft(draft);
        return Result.success("草稿已创建", id);
    }
    
    /**
     * 更新草稿
     */
    @PostMapping("/draft/update")
    public Result<Void> updateDraft(@RequestBody RegisterTemplateDraft draft) {
        draftService.updateDraft(draft);
        return Result.successMsg("草稿已更新");
    }
    
    /**
     * 测试草稿
     */
    @PostMapping("/draft/test/{id}")
    public Result<Map<String, Object>> testDraft(@PathVariable Long id) {
        try {
            Map<String, Object> result = draftService.testDraft(id);
            return Result.success("测试完成", result);
        } catch (Exception e) {
            log.error("测试草稿失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除草稿（支持DELETE方法）
     */
    @DeleteMapping("/draft/{id}")
    public Result<Void> deleteDraftDelete(@PathVariable Long id) {
        boolean ok = draftService.deleteDraft(id);
        return ok ? Result.successMsg("删除成功") : Result.error("删除失败");
    }
    
    /**
     * 删除草稿（兼容环境限制，提供POST删除）
     */
    @PostMapping("/draft/delete/{id}")
    public Result<Void> deleteDraft(@PathVariable Long id) {
        boolean ok = draftService.deleteDraft(id);
        return ok ? Result.successMsg("删除成功") : Result.error("删除失败");
    }
    
    /**
     * 草稿列表
     */
    @GetMapping("/draft/page")
    public Result<Page<RegisterTemplateDraft>> pageDrafts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<RegisterTemplateDraft> page = draftService.pageDrafts(pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 获取草稿详情
     */
    @GetMapping("/draft/{id}")
    public Result<RegisterTemplateDraft> getDraft(@PathVariable Long id) {
        return Result.success(draftService.getById(id));
    }
    
    // ==================== 网站分析-自动化注册相关接口 ====================
    
    /**
     * 自动化注册分析分页列表
     */
    @GetMapping("/analysis/register/list")
    public Result<Page<WebsiteAnalysis>> listRegisterAnalysis(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String websiteUrl,
            @RequestParam(required = false) Integer status) {
        Page<WebsiteAnalysis> page = websiteAnalysisService.listRegisterAnalysis(pageNum, pageSize, websiteUrl, status);
        return Result.success(page);
    }
    
    /**
     * 删除自动化注册分析记录
     */
    @DeleteMapping("/analysis/register/{id}")
    public Result<Void> deleteRegisterAnalysis(@PathVariable Long id) {
        websiteAnalysisService.deleteAnalysis(id);
        return Result.successMsg("删除成功");
    }
    
    /**
     * 启动自动化注册分析
     */
    @PostMapping("/analysis/register/start")
    public Result<Long> startRegisterAnalysis(@RequestBody Map<String, String> params) {
        String websiteUrl = params.get("websiteUrl");
        Long id = websiteAnalysisService.startRegisterAnalysis(websiteUrl);
        return Result.success("分析已启动", id);
    }
    
    /**
     * 获取自动化注册分析结果(用于前端一键填充表单)
     */
    @GetMapping("/analysis/register/result/{id}")
    public Result<Map<String, Object>> getRegisterAnalysisResult(@PathVariable Long id) {
        Map<String, Object> result = websiteAnalysisService.getRegisterAnalysisResult(id);
        return Result.success(result);
    }
    
    /**
     * 从草稿保存为模板
     */
    @PostMapping("/register/template/add-from-draft")
    public Result<Long> addTemplateFromDraft(@RequestBody Map<String, Object> body) {
        Long draftId = Long.valueOf(body.get("draftId").toString());
        String templateName = body.get("templateName") != null ? String.valueOf(body.get("templateName")) : null;
        String notes = body.get("notes") != null ? String.valueOf(body.get("notes")) : null;
        Long id = registerTemplateService.addFromDraft(draftId, templateName, notes);
        return Result.success("模板已创建", id);
    }
}
