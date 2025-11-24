package com.detection.platform.scheduler;

import com.detection.platform.service.ProxyNodeService;
import com.detection.platform.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {
    
    private final ServerService serverService;
    private final ProxyNodeService proxyNodeService;
    
    /**
     * 服务器健康检查定时任务
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void checkServerHealth() {
        log.info("开始执行服务器健康检查定时任务");
        
        try {
            // 获取所有服务器并刷新状态
            serverService.listAllServers().forEach(server -> {
                try {
                    serverService.refreshServerStatus(server.getId());
                    log.debug("服务器健康检查完成, ID: {}, 名称: {}", server.getId(), server.getServerName());
                } catch (Exception e) {
                    log.error("服务器健康检查失败, ID: {}, 错误: {}", server.getId(), e.getMessage());
                }
            });
            
            log.info("服务器健康检查定时任务执行完成");
        } catch (Exception e) {
            log.error("服务器健康检查定时任务执行异常", e);
        }
    }
    
    /**
     * 代理健康检查定时任务
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000)
    public void checkProxyHealth() {
        log.info("开始执行代理健康检查定时任务");
        
        try {
            // TODO: 实现代理池健康检查逻辑
            // 可以根据需要批量检测代理节点
            
            log.info("代理健康检查定时任务执行完成");
        } catch (Exception e) {
            log.error("代理健康检查定时任务执行异常", e);
        }
    }
}
