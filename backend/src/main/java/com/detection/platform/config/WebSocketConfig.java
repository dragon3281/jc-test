package com.detection.platform.config;

import com.detection.platform.websocket.SshWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket配置
 * 用于任务进度实时推送和SSH终端
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private final SshWebSocketHandler sshWebSocketHandler;

    /**
     * 配置消息代理
     * 直接使用SimpleBroker内置的TaskScheduler创建机制
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        log.info("配置 WebSocket 消息代理");
        
        // 启用简单的内存消息代理,用于向客户端发送消息
        // 消息前缀为/topic表示广播,/queue表示点对点
        // 注意:暂时不启用服务端心跳，依靠前端 STOMP 心跳和重连机制
        registry.enableSimpleBroker("/topic", "/queue");
        
        log.info("WebSocket 配置: 使用前端 STOMP 心跳机制 (20秒间隔)");
        
        // 设置应用程序目的地前缀
        // 客户端发送消息时需要加上此前缀
        registry.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目的地前缀
        registry.setUserDestinationPrefix("/user");
        
        log.info("WebSocket 消息代理配置完成");
    }

    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("注册 WebSocket STOMP 端点");
        // 注册一个STOMP的endpoint,并指定使用SockJS协议
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        log.info("STOMP 端点注册完成: /ws");
    }

    /**
     * 注册WebSocket处理器（用于SSH终端）
     */
    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        log.info("注册 SSH WebSocket 处理器");
        // 使用独立的路径，避免与/ws STOMP端点冲突
        registry.addHandler(sshWebSocketHandler, "/terminal/ssh")
                .setAllowedOrigins("*");
        log.info("SSH WebSocket 处理器注册完成: /terminal/ssh");
    }
}
