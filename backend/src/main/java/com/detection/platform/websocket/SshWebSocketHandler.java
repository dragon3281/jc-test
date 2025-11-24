package com.detection.platform.websocket;

import com.detection.platform.common.utils.AesUtil;
import com.detection.platform.entity.Server;
import com.detection.platform.service.ServerService;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSH WebSocket处理器
 * 用于Web终端功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshWebSocketHandler extends TextWebSocketHandler {

    private final ServerService serverService;
    private final AesUtil aesUtil;
    
    // 存储WebSocket会话和SSH会话的映射
    private final Map<String, Session> sshSessions = new ConcurrentHashMap<>();
    private final Map<String, Channel> sshChannels = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        String serverId = getServerId(webSocketSession);
        if (serverId == null) {
            webSocketSession.close(CloseStatus.BAD_DATA.withReason("缺少服务器ID"));
            return;
        }

        try {
            // 获取服务器信息
            Server server = serverService.getById(Long.parseLong(serverId));
            if (server == null) {
                webSocketSession.close(CloseStatus.BAD_DATA.withReason("服务器不存在"));
                return;
            }

            // 解密认证凭证
            String credential = aesUtil.decrypt(server.getAuthCredential());

            // 创建SSH连接
            JSch jsch = new JSch();
            Session session = jsch.getSession(server.getSshUsername(), server.getIpAddress(), server.getSshPort());
            session.setPassword(credential);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(30000);

            session.connect();

            // 打开Shell通道
            Channel channel = session.openChannel("shell");
            
            // 设置终端类型
            if (channel instanceof com.jcraft.jsch.ChannelShell) {
                com.jcraft.jsch.ChannelShell shellChannel = (com.jcraft.jsch.ChannelShell) channel;
                shellChannel.setPtyType("xterm");
                shellChannel.setPtySize(120, 30, 960, 400);
            }
            
            channel.connect();

            // 保存会话
            String sessionId = webSocketSession.getId();
            sshSessions.put(sessionId, session);
            sshChannels.put(sessionId, channel);

            // 启动输出监听线程
            startOutputReader(webSocketSession, channel);

            log.info("SSH WebSocket连接建立成功, sessionId={}, serverId={}", sessionId, serverId);

        } catch (Exception e) {
            log.error("建立SSH连接失败", e);
            webSocketSession.close(CloseStatus.SERVER_ERROR.withReason("连接服务器失败: " + e.getMessage()));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
        String sessionId = webSocketSession.getId();
        Channel channel = sshChannels.get(sessionId);

        if (channel != null && channel.isConnected()) {
            try {
                OutputStream outputStream = channel.getOutputStream();
                outputStream.write(message.asBytes());
                outputStream.flush();
            } catch (IOException e) {
                log.error("发送SSH命令失败", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws Exception {
        String sessionId = webSocketSession.getId();
        
        // 关闭SSH连接
        Channel channel = sshChannels.remove(sessionId);
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }

        Session session = sshSessions.remove(sessionId);
        if (session != null && session.isConnected()) {
            session.disconnect();
        }

        log.info("SSH WebSocket连接关闭, sessionId={}", sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        afterConnectionClosed(webSocketSession, CloseStatus.SERVER_ERROR);
    }

    /**
     * 启动SSH输出读取线程
     */
    private void startOutputReader(WebSocketSession webSocketSession, Channel channel) {
        executorService.execute(() -> {
            try {
                InputStream inputStream = channel.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;

                while (channel.isConnected() && (bytesRead = inputStream.read(buffer)) != -1) {
                    if (webSocketSession.isOpen()) {
                        String output = new String(buffer, 0, bytesRead, "UTF-8");
                        webSocketSession.sendMessage(new TextMessage(output));
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("读取SSH输出失败", e);
            }
        });
    }

    /**
     * 从WebSocket会话中获取服务器ID
     */
    private String getServerId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("serverId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("serverId=")) {
                    return param.substring("serverId=".length());
                }
            }
        }
        return null;
    }
}
