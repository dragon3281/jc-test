package com.detection.platform.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.detection.platform.entity.ProxyNode;
import com.detection.platform.entity.ProxyPool;
import com.detection.platform.service.ProxyNodeService;
import com.detection.platform.service.ProxyPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 代理健康检查定时任务
 * 定期检测代理节点状态，确保实时性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyHealthCheckTask implements ApplicationRunner {
    
    private final ProxyNodeService proxyNodeService;
    private final ProxyPoolService proxyPoolService;
    
    // 创建固定大小的线程池用于并发检测
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    /**
     * 应用启动时执行：立即检测所有处于"检测中"状态的节点
     * 防止系统重启后遗留"检测中"状态
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("应用启动，开始检查遗留的'检测中'状态节点...");
        
        try {
            // 查找所有状态为3（检测中）的节点
            LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProxyNode::getStatus, 3);
            List<ProxyNode> pendingNodes = proxyNodeService.list(wrapper);
            
            if (!pendingNodes.isEmpty()) {
                log.warn("发现 {} 个处于'检测中'状态的节点，立即重新检测", pendingNodes.size());
                
                // 异步检测所有处于"检测中"的节点
                for (ProxyNode node : pendingNodes) {
                    executorService.submit(() -> {
                        try {
                            log.info("重新检测节点, ID: {}, 地址: {}", node.getId(), node.getProxyAddress());
                            proxyNodeService.checkProxyNode(node.getId());
                        } catch (Exception e) {
                            log.error("启动时检测节点失败, ID: {}, 错误: {}", node.getId(), e.getMessage());
                        }
                    });
                }
                
                log.info("启动检测任务已提交，共 {} 个节点", pendingNodes.size());
            } else {
                log.info("没有遗留的'检测中'状态节点");
            }
        } catch (Exception e) {
            log.error("启动检测任务失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定时检测所有代理节点
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkAllProxyNodes() {
        log.info("开始定时代理健康检查...");
        
        try {
            // 查询所有代理池
            List<ProxyPool> pools = proxyPoolService.list();
            
            for (ProxyPool pool : pools) {
                // 查询该代理池的所有节点
                LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ProxyNode::getPoolId, pool.getId());
                List<ProxyNode> nodes = proxyNodeService.list(wrapper);
                
                if (nodes.isEmpty()) {
                    continue;
                }
                
                log.info("检测代理池: {}, 节点数: {}", pool.getPoolName(), nodes.size());
                
                // 并发检测所有节点
                for (ProxyNode node : nodes) {
                    executorService.submit(() -> {
                        try {
                            proxyNodeService.checkProxyNode(node.getId());
                        } catch (Exception e) {
                            log.error("检测代理节点异常, ID: {}, 错误: {}", node.getId(), e.getMessage());
                        }
                    });
                }
            }
            
            log.info("定时代理健康检查任务提交完成");
            
        } catch (Exception e) {
            log.error("定时代理健康检查异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 快速心跳检测 - 检测所有状态为"检测中"的节点和最近使用的代理
     * 每30秒执行一次（加快检测频率）
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void quickHeartbeatCheck() {
        log.debug("开始快速心跳检测...");
        
        try {
            // 1. 优先检测所有状态为3(检测中)的节点
            LambdaQueryWrapper<ProxyNode> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.eq(ProxyNode::getStatus, 3); // 检测中
            List<ProxyNode> pendingNodes = proxyNodeService.list(pendingWrapper);
            
            if (!pendingNodes.isEmpty()) {
                log.info("发现 {} 个处于检测中状态的节点，立即检测", pendingNodes.size());
                for (ProxyNode node : pendingNodes) {
                    executorService.submit(() -> {
                        try {
                            log.info("检测处于检测中状态的节点, ID: {}, 地址: {}", node.getId(), node.getProxyAddress());
                            proxyNodeService.checkProxyNode(node.getId());
                        } catch (Exception e) {
                            log.error("检测节点失败, ID: {}, 错误: {}", node.getId(), e.getMessage());
                        }
                    });
                }
            }
            
            // 2. 检测最近使用的活跃节点
            LambdaQueryWrapper<ProxyNode> activeWrapper = new LambdaQueryWrapper<>();
            activeWrapper.ge(ProxyNode::getLastCheckTime, LocalDateTime.now().minusMinutes(5));
            activeWrapper.ne(ProxyNode::getStatus, 3); // 排除检测中的（已经在上面处理）
            activeWrapper.orderByDesc(ProxyNode::getUseCount);
            activeWrapper.last("LIMIT 20"); // 只检测使用最频繁的20个
            
            List<ProxyNode> activeNodes = proxyNodeService.list(activeWrapper);
            
            if (!activeNodes.isEmpty()) {
                log.debug("快速检测活跃节点数: {}", activeNodes.size());
                
                for (ProxyNode node : activeNodes) {
                    executorService.submit(() -> {
                        try {
                            proxyNodeService.checkProxyNode(node.getId());
                        } catch (Exception e) {
                            log.warn("快速检测代理节点失败, ID: {}", node.getId());
                        }
                    });
                }
            }
            
            if (pendingNodes.isEmpty() && activeNodes.isEmpty()) {
                log.debug("没有需要检测的节点");
            }
            
        } catch (Exception e) {
            log.error("快速心跳检测异常: {}", e.getMessage());
        }
    }
}
