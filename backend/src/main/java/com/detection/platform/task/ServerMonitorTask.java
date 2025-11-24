package com.detection.platform.task;

import com.detection.platform.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 服务器监控定时任务
 * 定期刷新所有服务器的状态和资源使用情况
 * 
 * @author Detection Platform
 * @since 2024-11-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerMonitorTask {
    
    private final ServerService serverService;
    
    /**
     * 定时刷新所有服务器状态
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void refreshAllServersStatus() {
        try {
            log.debug("开始执行服务器状态刷新定时任务");
            Integer count = serverService.refreshAllServersStatus();
            log.debug("服务器状态刷新完成，刷新数量: {}", count);
        } catch (Exception e) {
            log.error("服务器状态刷新定时任务执行失败", e);
        }
    }
}
