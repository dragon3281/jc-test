package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.dto.DetectionTaskDTO;
import com.detection.platform.service.DetectionTaskService;
import com.detection.platform.vo.DetectionTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检测任务Controller
 */
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class DetectionTaskController {
    
    private final DetectionTaskService detectionTaskService;
    
    /**
     * 分页查询检测任务
     */
    @GetMapping("/page")
    public Result<Page<DetectionTaskVO>> pageTasks(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) Integer status) {
        Page<DetectionTaskVO> page = detectionTaskService.pageTasks(current, size, taskName, status);
        return Result.success(page);
    }
    
    /**
     * 获取所有任务列表
     */
    @GetMapping("/list")
    public Result<List<DetectionTaskVO>> listAllTasks() {
        List<DetectionTaskVO> list = detectionTaskService.listAllTasks();
        return Result.success(list);
    }
    
    /**
     * 根据ID获取任务详情
     */
    @GetMapping("/{id}")
    public Result<DetectionTaskVO> getTaskById(@PathVariable Long id) {
        DetectionTaskVO task = detectionTaskService.getTaskById(id);
        return Result.success(task);
    }
    
    /**
     * 创建检测任务
     */
    @PostMapping
    public Result<Long> createTask(@Valid @RequestBody DetectionTaskDTO taskDTO) {
        Long id = detectionTaskService.createTask(taskDTO);
        return Result.success("创建任务成功", id);
    }
    
    /**
     * 启动任务
     */
    @PostMapping("/{id}/start")
    public Result<Void> startTask(@PathVariable Long id) {
        detectionTaskService.startTask(id);
        return Result.successMsg("任务已启动");
    }
    
    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    public Result<Void> stopTask(@PathVariable Long id) {
        detectionTaskService.stopTask(id);
        return Result.successMsg("任务已停止");
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        detectionTaskService.deleteTask(id);
        return Result.successMsg("删除任务成功");
    }
    
    /**
     * 获取任务进度
     */
    @GetMapping("/{id}/progress")
    public Result<DetectionTaskVO> getTaskProgress(@PathVariable Long id) {
        DetectionTaskVO progress = detectionTaskService.getTaskProgress(id);
        return Result.success(progress);
    }
}
