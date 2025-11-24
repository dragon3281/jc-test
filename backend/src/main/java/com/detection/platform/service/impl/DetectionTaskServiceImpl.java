package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.DetectionTaskMapper;
import com.detection.platform.dao.TaskServerMapper;
import com.detection.platform.dto.DetectionTaskDTO;
import com.detection.platform.entity.DetectionTask;
import com.detection.platform.entity.PostTemplate;
import com.detection.platform.entity.ProxyPool;
import com.detection.platform.entity.TaskServer;
import com.detection.platform.common.utils.RedisLockUtil;
import com.detection.platform.mq.TaskMessageProducer;
import com.detection.platform.service.BaseDataService;
import com.detection.platform.service.DetectionResultService;
import com.detection.platform.service.DetectionTaskService;
import com.detection.platform.service.PostTemplateService;
import com.detection.platform.service.ProxyPoolService;
import com.detection.platform.vo.DetectionTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检测任务Service实现类
 */
@Slf4j
@Service
public class DetectionTaskServiceImpl extends ServiceImpl<DetectionTaskMapper, DetectionTask> 
        implements DetectionTaskService {
    
    @Autowired
    private TaskServerMapper taskServerMapper;
    @Autowired
    private PostTemplateService postTemplateService;
    @Lazy
    @Autowired
    private ProxyPoolService proxyPoolService;
    @Autowired
    private BaseDataService baseDataService;
    @Autowired
    private DetectionResultService detectionResultService;
    @Autowired
    private TaskMessageProducer taskMessageProducer;
    @Autowired
    private RedisLockUtil redisLockUtil;
    
    @Override
    public Page<DetectionTaskVO> pageTasks(Integer current, Integer size, String taskName, Integer status) {
        LambdaQueryWrapper<DetectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(taskName), DetectionTask::getTaskName, taskName);
        wrapper.eq(status != null, DetectionTask::getTaskStatus, status);
        wrapper.orderByDesc(DetectionTask::getCreateTime);
        
        Page<DetectionTask> page = this.page(new Page<>(current, size), wrapper);
        
        Page<DetectionTaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<DetectionTaskVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    public List<DetectionTaskVO> listAllTasks() {
        LambdaQueryWrapper<DetectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DetectionTask::getCreateTime);
        
        List<DetectionTask> list = this.list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public DetectionTaskVO getTaskById(Long id) {
        DetectionTask task = this.getById(id);
        if (task == null) {
            throw new GlobalExceptionHandler.BusinessException("任务不存在");
        }
        return convertToVO(task);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(DetectionTaskDTO taskDTO) {
        // 验证POST模板是否存在
        PostTemplate template = postTemplateService.getById(taskDTO.getTemplateId());
        if (template == null) {
            throw new GlobalExceptionHandler.BusinessException("POST模板不存在");
        }
        
        // 验证代理池是否存在
        ProxyPool pool = proxyPoolService.getById(taskDTO.getPoolId());
        if (pool == null) {
            throw new GlobalExceptionHandler.BusinessException("代理池不存在");
        }
        
        // 创建任务
        DetectionTask task = new DetectionTask();
        BeanUtils.copyProperties(taskDTO, task);
        
        // 设置初始状态
        task.setTaskStatus(1); // 待执行
        task.setTotalCount(0L);
        task.setCompletedCount(0L);
        task.setSuccessCount(0L);
        task.setFailCount(0L);
        task.setProgress(BigDecimal.ZERO);
        
        this.save(task);
        
        // 保存任务与服务器的关联关系
        if (taskDTO.getServerIds() != null && !taskDTO.getServerIds().isEmpty()) {
            for (Long serverId : taskDTO.getServerIds()) {
                TaskServer taskServer = new TaskServer();
                taskServer.setTaskId(task.getId());
                taskServer.setServerId(serverId);
                taskServerMapper.insert(taskServer);
            }
        }
        
        log.info("创建检测任务成功, ID: {}, 名称: {}", task.getId(), task.getTaskName());
        return task.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean startTask(Long taskId) {
        // 分布式锁，防止多实例同时启动同一任务
        String lockKey = "task:start:" + taskId;
        String requestId = Thread.currentThread().getId() + "_" + System.currentTimeMillis();
        
        boolean locked = redisLockUtil.tryLock(lockKey, requestId, 10);
        if (!locked) {
            throw new GlobalExceptionHandler.BusinessException("任务正在启动中，请勿重复操作");
        }
        
        try {
            DetectionTask task = this.getById(taskId);
            if (task == null) {
                throw new GlobalExceptionHandler.BusinessException("任务不存在");
            }
            
            if (task.getTaskStatus() == 2) {
                throw new GlobalExceptionHandler.BusinessException("任务正在执行中");
            }
            
            if (task.getTaskStatus() == 3) {
                throw new GlobalExceptionHandler.BusinessException("任务已完成");
            }
            
            // 更新任务状态为执行中
            task.setTaskStatus(2);
            task.setStartTime(LocalDateTime.now());
            
            boolean success = this.updateById(task);
            
            if (success) {
                log.info("启动检测任务成功, ID: {}, 名称: {}", taskId, task.getTaskName());
                // 异步执行任务逻辑 - 从数据库中获取待检测数据并发送到消息队列
                executeTaskAsync(task);
            }
            
            return success;
        } finally {
            // 释放锁
            redisLockUtil.releaseLock(lockKey, requestId);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean stopTask(Long taskId) {
        DetectionTask task = this.getById(taskId);
        if (task == null) {
            throw new GlobalExceptionHandler.BusinessException("任务不存在");
        }
        
        if (task.getTaskStatus() != 2) {
            throw new GlobalExceptionHandler.BusinessException("只能停止执行中的任务");
        }
        
        // 更新任务状态为已停止
        task.setTaskStatus(4);
        task.setEndTime(LocalDateTime.now());
        
        boolean success = this.updateById(task);
        
        if (success) {
            log.info("停止检测任务成功, ID: {}, 名称: {}", taskId, task.getTaskName());
            // TODO: 停止任务执行逻辑
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTask(Long taskId) {
        DetectionTask task = this.getById(taskId);
        if (task == null) {
            throw new GlobalExceptionHandler.BusinessException("任务不存在");
        }
        
        if (task.getTaskStatus() == 2) {
            throw new GlobalExceptionHandler.BusinessException("正在执行的任务不能删除");
        }
        
        // 删除任务与服务器的关联关系
        LambdaQueryWrapper<TaskServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskServer::getTaskId, taskId);
        taskServerMapper.delete(wrapper);
        
        // 删除任务
        boolean success = this.removeById(taskId);
        
        if (success) {
            log.info("删除检测任务成功, ID: {}, 名称: {}", taskId, task.getTaskName());
        }
        
        return success;
    }
    
    @Override
    public DetectionTaskVO getTaskProgress(Long taskId) {
        DetectionTask task = this.getById(taskId);
        if (task == null) {
            throw new GlobalExceptionHandler.BusinessException("任务不存在");
        }
        
        // 计算进度百分比
        if (task.getTotalCount() != null && task.getTotalCount() > 0) {
            BigDecimal progress = BigDecimal.valueOf(task.getCompletedCount())
                    .divide(BigDecimal.valueOf(task.getTotalCount()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            task.setProgress(progress);
            this.updateById(task);
        }
        
        return convertToVO(task);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskProgress(Long taskId) {
        // 查询任务完成情况
        LambdaQueryWrapper<com.detection.platform.entity.DetectionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.detection.platform.entity.DetectionResult::getTaskId, taskId);
        long completedCount = detectionResultService.count(wrapper);
        
        DetectionTask task = this.getById(taskId);
        if (task == null) {
            return;
        }
        
        // 更新完成数量
        task.setCompletedCount(completedCount);
        
        // 统计成功和失败数量
        LambdaQueryWrapper<com.detection.platform.entity.DetectionResult> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.eq(com.detection.platform.entity.DetectionResult::getTaskId, taskId)
                .in(com.detection.platform.entity.DetectionResult::getDetectStatus, 1, 2); // 已注册或未注册
        long successCount = detectionResultService.count(successWrapper);
        task.setSuccessCount(successCount);
        
        LambdaQueryWrapper<com.detection.platform.entity.DetectionResult> failWrapper = new LambdaQueryWrapper<>();
        failWrapper.eq(com.detection.platform.entity.DetectionResult::getTaskId, taskId)
                .in(com.detection.platform.entity.DetectionResult::getDetectStatus, 3, 4, 5); // 失败、异常
        long failCount = detectionResultService.count(failWrapper);
        task.setFailCount(failCount);
        
        // 计算进度
        if (task.getTotalCount() != null && task.getTotalCount() > 0) {
            BigDecimal progress = BigDecimal.valueOf(completedCount)
                    .divide(BigDecimal.valueOf(task.getTotalCount()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            task.setProgress(progress);
            
            // 如果全部完成，更新任务状态
            if (completedCount >= task.getTotalCount()) {
                task.setTaskStatus(3); // 已完成
                task.setEndTime(LocalDateTime.now());
                log.info("检测任务全部完成, ID: {}, 名称: {}", taskId, task.getTaskName());
            }
        }
        
        this.updateById(task);
    }
    
    /**
     * 异步执行任务
     */
    private void executeTaskAsync(DetectionTask task) {
        new Thread(() -> {
            try {
                log.info("开始异步执行任务, ID: {}", task.getId());
                
                // 获取所有待检测数据
                List<com.detection.platform.entity.BaseData> baseDataList = baseDataService.list();
                
                // 更新任务总数量
                task.setTotalCount((long) baseDataList.size());
                this.updateById(task);
                
                // 批量发送检测任务到RabbitMQ
                List<String> dataValues = baseDataList.stream()
                        .map(com.detection.platform.entity.BaseData::getAccountIdentifier)
                        .collect(Collectors.toList());
                
                taskMessageProducer.sendBatchDetectionTasks(
                    task.getId(), 
                    task.getTemplateId(), 
                    task.getPoolId(), 
                    dataValues
                );
                
                log.info("任务发送完成, ID: {}, 数据量: {}", task.getId(), dataValues.size());
                
            } catch (Exception e) {
                log.error("异步执行任务失败, ID: {}, 错误: {}", task.getId(), e.getMessage(), e);
                // 更新任务状态为失败
                task.setStatus(5);
                task.setEndTime(LocalDateTime.now());
                this.updateById(task);
            }
        }, "Task-Executor-" + task.getId()).start();
    }
    
    /**
     * 实体转VO
     */
    private DetectionTaskVO convertToVO(DetectionTask task) {
        DetectionTaskVO vo = new DetectionTaskVO();
        BeanUtils.copyProperties(task, vo);
        
        // 设置模板名称
        if (task.getTemplateId() != null) {
            PostTemplate template = postTemplateService.getById(task.getTemplateId());
            if (template != null) {
                vo.setTemplateName(template.getTemplateName());
            }
        }
        
        // 设置代理池名称
        if (task.getPoolId() != null) {
            ProxyPool pool = proxyPoolService.getById(task.getPoolId());
            if (pool != null) {
                vo.setPoolName(pool.getPoolName());
            }
        }
        
        // 设置状态文本
        if (task.getTaskStatus() != null) {
            vo.setStatus(task.getTaskStatus());
            switch (task.getTaskStatus()) {
                case 1 -> vo.setStatusText("待执行");
                case 2 -> vo.setStatusText("执行中");
                case 3 -> vo.setStatusText("已完成");
                case 4 -> vo.setStatusText("已停止");
                case 5 -> vo.setStatusText("执行失败");
                default -> vo.setStatusText("未知");
            }
        }
        
        return vo;
    }
}
