package com.detection.platform.common.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;

/**
 * Docker客户端工具类
 * 用于远程管理服务器上的Docker容器
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Slf4j
@Component
public class DockerUtil {

    /**
     * 创建Docker客户端
     *
     * @param host Docker主机地址
     * @param port Docker API端口
     * @return DockerClient实例
     */
    public DockerClient createClient(String host, int port) {
        try {
            String dockerHost = String.format("tcp://%s:%d", host, port);
            
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();

            return DockerClientImpl.getInstance(config, httpClient);
            
        } catch (Exception e) {
            log.error("创建Docker客户端失败: host={}, port={}", host, port, e);
            throw new RuntimeException("创建Docker客户端失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出所有容器
     *
     * @param client Docker客户端
     * @param showAll 是否显示所有容器(包括停止的)
     * @return 容器列表
     */
    public List<Container> listContainers(DockerClient client, boolean showAll) {
        try {
            return client.listContainersCmd()
                    .withShowAll(showAll)
                    .exec();
        } catch (Exception e) {
            log.error("列出容器失败", e);
            throw new RuntimeException("列出容器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 启动容器
     *
     * @param client Docker客户端
     * @param containerId 容器ID
     */
    public void startContainer(DockerClient client, String containerId) {
        try {
            client.startContainerCmd(containerId).exec();
            log.info("容器启动成功: {}", containerId);
        } catch (Exception e) {
            log.error("启动容器失败: containerId={}", containerId, e);
            throw new RuntimeException("启动容器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止容器
     *
     * @param client Docker客户端
     * @param containerId 容器ID
     */
    public void stopContainer(DockerClient client, String containerId) {
        try {
            client.stopContainerCmd(containerId).exec();
            log.info("容器停止成功: {}", containerId);
        } catch (Exception e) {
            log.error("停止容器失败: containerId={}", containerId, e);
            throw new RuntimeException("停止容器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重启容器
     *
     * @param client Docker客户端
     * @param containerId 容器ID
     */
    public void restartContainer(DockerClient client, String containerId) {
        try {
            client.restartContainerCmd(containerId).exec();
            log.info("容器重启成功: {}", containerId);
        } catch (Exception e) {
            log.error("重启容器失败: containerId={}", containerId, e);
            throw new RuntimeException("重启容器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除容器
     *
     * @param client Docker客户端
     * @param containerId 容器ID
     * @param force 是否强制删除
     */
    public void removeContainer(DockerClient client, String containerId, boolean force) {
        try {
            client.removeContainerCmd(containerId)
                    .withForce(force)
                    .exec();
            log.info("容器删除成功: {}", containerId);
        } catch (Exception e) {
            log.error("删除容器失败: containerId={}", containerId, e);
            throw new RuntimeException("删除容器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取容器详细信息
     *
     * @param client Docker客户端
     * @param containerId 容器ID
     * @return 容器信息
     */
    public InspectContainerResponse inspectContainer(DockerClient client, String containerId) {
        try {
            return client.inspectContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.error("获取容器信息失败: containerId={}", containerId, e);
            throw new RuntimeException("获取容器信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取容器日志
     *
     * @param client Docker客户端
     * @param containerId 容器ID
     * @param tail 返回最后多少行
     * @return 日志内容
     */
    public String getContainerLogs(DockerClient client, String containerId, int tail) {
        try {
            StringBuilder logs = new StringBuilder();
            client.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(tail)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            logs.append(new String(frame.getPayload()));
                        }
                    })
                    .awaitCompletion();
            
            return logs.toString();
        } catch (Exception e) {
            log.error("获取容器日志失败: containerId={}", containerId, e);
            throw new RuntimeException("获取容器日志失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取容器统计信息
     *
     * @param client Docker客户端
     * @param containerId 容器ID
     * @return 统计信息
     */
    public Statistics getContainerStats(DockerClient client, String containerId) {
        try {
            StatsCmd statsCmd = client.statsCmd(containerId)
                    .withNoStream(true);
            
            Statistics[] stats = {null};
            statsCmd.exec(new ResultCallback.Adapter<Statistics>() {
                @Override
                public void onNext(Statistics object) {
                    stats[0] = object;
                }
            }).awaitCompletion();
            
            return stats[0];
        } catch (Exception e) {
            log.error("获取容器统计信息失败: containerId={}", containerId, e);
            throw new RuntimeException("获取容器统计信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建容器
     *
     * @param client Docker客户端
     * @param imageName 镜像名称
     * @param containerName 容器名称
     * @return 容器ID
     */
    public String createContainer(DockerClient client, String imageName, String containerName) {
        try {
            CreateContainerResponse container = client.createContainerCmd(imageName)
                    .withName(containerName)
                    .exec();
            
            log.info("容器创建成功: id={}, name={}", container.getId(), containerName);
            return container.getId();
        } catch (Exception e) {
            log.error("创建容器失败: image={}, name={}", imageName, containerName, e);
            throw new RuntimeException("创建容器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 测试Docker连接
     *
     * @param host Docker主机地址
     * @param port Docker API端口
     * @return 是否连接成功
     */
    public boolean testConnection(String host, int port) {
        DockerClient client = null;
        try {
            client = createClient(host, port);
            client.pingCmd().exec();
            return true;
        } catch (Exception e) {
            log.error("Docker连接测试失败: host={}, port={}", host, port, e);
            return false;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.warn("关闭Docker客户端失败", e);
                }
            }
        }
    }

    /**
     * 关闭Docker客户端
     *
     * @param client Docker客户端
     */
    public void closeClient(DockerClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭Docker客户端失败", e);
            }
        }
    }
}
