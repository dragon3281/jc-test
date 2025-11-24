package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.dto.DetectionTaskDTO;
import com.detection.platform.entity.DetectionTask;
import com.detection.platform.vo.DetectionTaskVO;

import java.util.List;

/**
 * 检测任务Service接口
 */
public interface DetectionTaskService extends IService<DetectionTask> {
    
    /**
     * 分页查询检测任务
     */
    Page<DetectionTaskVO> pageTasks(Integer current, Integer size, String taskName, Integer status);
    
    /**
     * 获取所有任务列表
     */
    List<DetectionTaskVO> listAllTasks();
    
    /**
     * 根据ID获取任务详情
     */
    DetectionTaskVO getTaskById(Long id);
    
    /**
     * 创建检测任务
     */
    Long createTask(DetectionTaskDTO taskDTO);
    
    /**
     * 启动任务
     */
    Boolean startTask(Long taskId);
    
    /**
     * 停止任务
     */
    Boolean stopTask(Long taskId);
    
    /**
     * 删除任务
     */
    Boolean deleteTask(Long taskId);
    
    /**
     * 获取任务进度
     */
    DetectionTaskVO getTaskProgress(Long taskId);
    
    /**
     * 更新任务进度
     */
    void updateTaskProgress(Long taskId);
}
