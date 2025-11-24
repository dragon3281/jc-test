package com.detection.platform.mq;

import com.detection.platform.entity.DetectionResult;
import com.detection.platform.executor.DetectionExecutor;
import com.detection.platform.service.DetectionTaskService;
import com.detection.platform.vo.DetectionTaskVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 任务消息消费者
 * 消费RabbitMQ中的检测任务消息并执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageConsumer {

    private final DetectionExecutor detectionExecutor;
    private final DetectionTaskService detectionTaskService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 消费检测任务消息
     */
    @RabbitListener(queues = "task.detection.queue")
    public void consumeDetectionTask(String message) {
        try {
            log.info("收到检测任务消息: {}", message);
            
            // 解析消息
            Map<String, Object> taskMessage = objectMapper.readValue(message, Map.class);
            Long taskId = Long.valueOf(taskMessage.get("taskId").toString());
            Long templateId = Long.valueOf(taskMessage.get("templateId").toString());
            Long proxyPoolId = taskMessage.get("proxyPoolId") != null ? 
                Long.valueOf(taskMessage.get("proxyPoolId").toString()) : null;
            String dataValue = taskMessage.get("dataValue").toString();
            
            // 执行检测
            DetectionResult result = detectionExecutor.executeDetection(
                taskId, templateId, proxyPoolId, dataValue
            );
            
            // 更新任务进度
            detectionTaskService.updateTaskProgress(taskId);
            
            // 推送实时结果到前端
            pushResultToFrontend(taskId, result);
            
            log.info("检测任务执行完成, 任务ID: {}, 账号: {}, 状态: {}", 
                taskId, dataValue, result.getDetectStatus());
            
        } catch (Exception e) {
            log.error("消费检测任务消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 推送检测结果到前端
     */
    private void pushResultToFrontend(Long taskId, DetectionResult result) {
        try {
            // 构建推送消息
            Map<String, Object> message = Map.of(
                "type", "detection_result",
                "taskId", taskId,
                "accountIdentifier", result.getAccountIdentifier(),
                "detectStatus", result.getDetectStatus(),
                "responseTime", result.getResponseTime(),
                "detectTime", result.getDetectTime().toString()
            );
            
            // 推送到WebSocket主题
            messagingTemplate.convertAndSend("/topic/task/" + taskId, message);
            
        } catch (Exception e) {
            log.error("推送结果到前端失败: {}", e.getMessage());
        }
    }

    /**
     * 消费任务进度更新消息
     */
    @RabbitListener(queues = "task.progress.queue")
    public void consumeProgressUpdate(String message) {
        try {
            log.debug("收到进度更新消息: {}", message);
            
            Map<String, Object> progressMessage = objectMapper.readValue(message, Map.class);
            Long taskId = Long.valueOf(progressMessage.get("taskId").toString());
            
            // 查询最新进度
            DetectionTaskVO progress = detectionTaskService.getTaskProgress(taskId);
            
            // 推送到前端
            messagingTemplate.convertAndSend("/topic/task/progress/" + taskId, progress);
            
        } catch (Exception e) {
            log.error("消费进度更新消息失败: {}", e.getMessage(), e);
        }
    }
}
