package com.detection.platform.config;

import com.detection.platform.websocket.SshWebSocketHandler;
import lombok.RequiredArgsConstructor;
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
@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private final SshWebSocketHandler sshWebSocketHandler;

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单的内存消息代理,用于向客户端发送消息
        // 消息前缀为/topic表示广播,/queue表示点对点
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 设置应用程序目的地前缀
        // 客户端发送消息时需要加上此前缀
        registry.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目的地前缀
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册一个STOMP的endpoint,并指定使用SockJS协议
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 注册WebSocket处理器（用于SSH终端）
     */
    @Override
    public void registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry registry) {
        // 使用独立的路径，避免与/ws STOMP端点冲突
        registry.addHandler(sshWebSocketHandler, "/terminal/ssh")
                .setAllowedOrigins("*");
    }
}
