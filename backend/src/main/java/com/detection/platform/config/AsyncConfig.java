package com.detection.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * 优化线程池参数,提升并发处理能力
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 默认异步任务执行器
     * 用于一般性的异步任务
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(10);
        
        // 最大线程数
        executor.setMaxPoolSize(50);
        
        // 队列容量
        executor.setQueueCapacity(1000);
        
        // 线程空闲时间(秒)
        executor.setKeepAliveSeconds(300);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("async-task-");
        
        // 拒绝策略:由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * 检测任务专用执行器
     * 用于处理检测任务的异步执行
     */
    @Bean(name = "detectionTaskExecutor")
    public Executor detectionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数(根据服务器CPU核心数调整)
        executor.setCorePoolSize(20);
        
        // 最大线程数
        executor.setMaxPoolSize(100);
        
        // 队列容量
        executor.setQueueCapacity(2000);
        
        // 线程空闲时间(秒)
        executor.setKeepAliveSeconds(600);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("detection-");
        
        // 拒绝策略:由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * 数据导出专用执行器
     * 用于处理大量数据的导出操作
     */
    @Bean(name = "exportExecutor")
    public Executor exportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程空闲时间(秒)
        executor.setKeepAliveSeconds(300);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("export-");
        
        // 拒绝策略:丢弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * WebSocket推送专用执行器
     * 用于处理实时消息推送
     */
    @Bean(name = "websocketExecutor")
    public Executor websocketExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数
        executor.setMaxPoolSize(20);
        
        // 队列容量
        executor.setQueueCapacity(500);
        
        // 线程空闲时间(秒)
        executor.setKeepAliveSeconds(60);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("ws-push-");
        
        // 拒绝策略:直接丢弃(推送失败不影响主流程)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(false);
        
        executor.initialize();
        return executor;
    }
}
