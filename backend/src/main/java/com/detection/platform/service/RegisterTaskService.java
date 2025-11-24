package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.entity.RegisterTask;

import java.util.List;
import java.util.Map;

/**
 * 自动化注册Service接口
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
public interface RegisterTaskService {

    /**
     * 分页查询注册任务列表
     */
    Page<RegisterTask> pageRegisterTasks(Integer current, Integer size, String taskName, Integer status);

    /**
     * 根据ID获取任务详情
     */
    RegisterTask getTaskById(Long id);

    /**
     * 创建注册任务
     */
    Long createTask(Map<String, Object> params);

    /**
     * 启动任务
     */
    Boolean startTask(Long id);

    /**
     * 暂停任务
     */
    Boolean pauseTask(Long id);

    /**
     * 继续任务
     */
    Boolean resumeTask(Long id);

    /**
     * 删除任务
     */
    Boolean deleteTask(Long id);

    /**
     * 获取任务的注册结果
     */
    List<Map<String, Object>> getTaskResults(Long id);
}
