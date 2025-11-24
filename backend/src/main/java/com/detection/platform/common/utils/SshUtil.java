package com.detection.platform.common.utils;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * SSH连接工具类
 * 用于远程服务器管理
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Slf4j
@Component
public class SshUtil {

    private static final int DEFAULT_TIMEOUT = 5000; // 5秒超时

    /**
     * 执行SSH命令(密码认证)
     *
     * @param host 主机地址
     * @param port SSH端口
     * @param username 用户名
     * @param password 密码
     * @param command 要执行的命令
     * @return 命令输出结果
     */
    public String executeCommand(String host, int port, String username, String password, String command) {
        Session session = null;
        ChannelExec channel = null;
        try {
            // 创建JSch实例
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // 配置
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(5000); // 5秒超时

            // 连接
            session.connect(5000); // 连接超时5秒

            // 执行命令
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            channel.setErrStream(errorStream);

            channel.connect(5000); // 通道连接超时5秒

            // 等待命令执行完成，最多等待10秒
            int maxWait = 100; // 100 * 100ms = 10秒
            int count = 0;
            while (!channel.isClosed() && count < maxWait) {
                Thread.sleep(100);
                count++;
            }
            
            if (!channel.isClosed()) {
                log.warn("SSH命令执行超时: host={}, command={}", host, command);
                throw new RuntimeException("命令执行超时");
            }

            // 获取输出
            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");

            if (channel.getExitStatus() != 0 && !error.isEmpty()) {
                log.warn("SSH命令执行有错误输出: {}", error);
            }

            return output;

        } catch (Exception e) {
            log.warn("SSH命令执行失败: host={}, port={}, command={}, error={}", host, port, command, e.getMessage());
            throw new RuntimeException("SSH命令执行失败: " + e.getMessage(), e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 执行SSH命令(密钥认证)
     *
     * @param host 主机地址
     * @param port SSH端口
     * @param username 用户名
     * @param privateKey 私钥内容
     * @param command 要执行的命令
     * @return 命令输出结果
     */
    public String executeCommandWithKey(String host, int port, String username, String privateKey, String command) {
        Session session = null;
        ChannelExec channel = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity("key", privateKey.getBytes(), null, null);
            
            session = jsch.getSession(username, host, port);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(5000); // 5秒超时

            session.connect(5000); // 连接超时5秒

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            channel.connect(5000); // 通道连接超时5秒

            // 等待命令执行完成，最多等待10秒
            int maxWait = 100; // 100 * 100ms = 10秒
            int count = 0;
            while (!channel.isClosed() && count < maxWait) {
                Thread.sleep(100);
                count++;
            }
            
            if (!channel.isClosed()) {
                log.warn("SSH命令执行超时: host={}, command={}", host, command);
                throw new RuntimeException("命令执行超时");
            }

            return outputStream.toString("UTF-8");

        } catch (Exception e) {
            log.warn("SSH密钥认证命令执行失败: host={}, port={}, error={}", host, port, e.getMessage());
            throw new RuntimeException("SSH密钥认证失败: " + e.getMessage(), e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 测试SSH连接(密码认证)
     *
     * @param host 主机地址
     * @param port SSH端口
     * @param username 用户名
     * @param password 密码
     * @return 是否连接成功
     */
    public boolean testConnection(String host, int port, String username, String password) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(5000); // 5秒超时

            session.connect(5000); // 连接超时
            boolean connected = session.isConnected();
            
            if (connected) {
                log.info("SSH连接测试成功: host={}, port={}", host, port);
            }
            
            return connected;

        } catch (Exception e) {
            log.warn("SSH连接测试失败: host={}, port={}, error={}", host, port, e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 测试SSH连接(密钥认证)
     *
     * @param host 主机地址
     * @param port SSH端口
     * @param username 用户名
     * @param privateKey 私钥内容
     * @return 是否连接成功
     */
    public boolean testConnectionWithKey(String host, int port, String username, String privateKey) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity("key", privateKey.getBytes(), null, null);
            
            session = jsch.getSession(username, host, port);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(5000); // 5秒超时

            session.connect(5000); // 连接超时5秒
            return session.isConnected();

        } catch (Exception e) {
            log.warn("SSH密钥连接测试失败: host={}, port={}, error={}", host, port, e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 获取服务器CPU使用率
     */
    public String getCpuUsage(String host, int port, String username, String password) {
        String command = "top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - $1}'";
        return executeCommand(host, port, username, password, command).trim();
    }

    /**
     * 获取服务器内存使用率
     */
    public String getMemoryUsage(String host, int port, String username, String password) {
        String command = "free | grep Mem | awk '{print ($3/$2) * 100.0}'";
        return executeCommand(host, port, username, password, command).trim();
    }

    /**
     * 获取服务器磁盘使用率
     */
    public String getDiskUsage(String host, int port, String username, String password) {
        String command = "df -h / | tail -1 | awk '{print $5}' | sed 's/%//'";
        return executeCommand(host, port, username, password, command).trim();
    }
    
    /**
     * 获取服务器网络流量 (KB/s)
     * 返回格式: "入流量,出流量"
     */
    public String getNetworkTraffic(String host, int port, String username, String password) {
        // 获取网络接口流量，采样间隔1秒
        String command = "RX1=$(cat /sys/class/net/eth0/statistics/rx_bytes 2>/dev/null || cat /sys/class/net/ens*/statistics/rx_bytes 2>/dev/null | head -1); " +
                "TX1=$(cat /sys/class/net/eth0/statistics/tx_bytes 2>/dev/null || cat /sys/class/net/ens*/statistics/tx_bytes 2>/dev/null | head -1); " +
                "sleep 1; " +
                "RX2=$(cat /sys/class/net/eth0/statistics/rx_bytes 2>/dev/null || cat /sys/class/net/ens*/statistics/rx_bytes 2>/dev/null | head -1); " +
                "TX2=$(cat /sys/class/net/eth0/statistics/tx_bytes 2>/dev/null || cat /sys/class/net/ens*/statistics/tx_bytes 2>/dev/null | head -1); " +
                "echo \"$((($RX2-$RX1)/1024)),$((($TX2-$TX1)/1024))\"";
        return executeCommand(host, port, username, password, command).trim();
    }
}
