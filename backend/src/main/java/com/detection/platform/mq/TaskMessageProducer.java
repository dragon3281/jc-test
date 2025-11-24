package com.detection.platform.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务消息生产者
 * 发送检测任务消息到RabbitMQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送检测任务消息
     *
     * @param taskId 任务ID
     * @param templateId 模板ID
     * @param proxyPoolId 代理池ID
     * @param dataValue 待检测数据
     */
    public void sendDetectionTask(Long taskId, Long templateId, Long proxyPoolId, String dataValue) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("taskId", taskId);
            message.put("templateId", templateId);
            message.put("proxyPoolId", proxyPoolId);
            message.put("dataValue", dataValue);
            
            String messageJson = objectMapper.writeValueAsString(message);
            
            rabbitTemplate.convertAndSend("task.detection.exchange", "task.detection", messageJson);
            
            log.debug("发送检测任务消息: {}", messageJson);
            
        } catch (Exception e) {
            log.error("发送检测任务消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 批量发送检测任务消息
     */
    public void sendBatchDetectionTasks(Long taskId, Long templateId, Long proxyPoolId, 
                                       java.util.List<String> dataValues) {
        for (String dataValue : dataValues) {
            sendDetectionTask(taskId, templateId, proxyPoolId, dataValue);
        }
        log.info("批量发送检测任务消息完成, 数量: {}", dataValues.size());
    }

    /**
     * 发送进度更新消息
     */
    public void sendProgressUpdate(Long taskId) {
        try {
            Map<String, Object> message = Map.of("taskId", taskId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            rabbitTemplate.convertAndSend("task.progress.exchange", "task.progress", messageJson);
            
            log.debug("发送进度更新消息: {}", messageJson);
            
        } catch (Exception e) {
            log.error("发送进度更新消息失败: {}", e.getMessage(), e);
        }
    }
}
