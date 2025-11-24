package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.common.utils.AesUtil;
import com.detection.platform.common.utils.DockerUtil;
import com.detection.platform.common.utils.SshUtil;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.ServerMapper;
import com.detection.platform.dto.ServerDTO;
import com.detection.platform.entity.Server;
import com.detection.platform.service.ServerService;
import com.detection.platform.vo.ServerVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务器管理Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerServiceImpl extends ServiceImpl<ServerMapper, Server> implements ServerService {
    
    private final AesUtil aesUtil;
    private final SshUtil sshUtil;
    private final DockerUtil dockerUtil;
    private final ObjectMapper objectMapper;
    
    @Override
    public Page<ServerVO> pageServers(Integer current, Integer size, String serverName, Integer status) {
        // 构建查询条件
        LambdaQueryWrapper<Server> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(serverName), Server::getServerName, serverName);
        wrapper.eq(status != null, Server::getStatus, status);
        wrapper.orderByDesc(Server::getCreateTime);
        
        // 分页查询
        Page<Server> page = this.page(new Page<>(current, size), wrapper);
        
        // 转换为VO
        Page<ServerVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ServerVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    public List<ServerVO> listAllServers() {
        LambdaQueryWrapper<Server> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Server::getCreateTime);
        List<Server> list = this.list(wrapper);
        
        // 返回VO列表，包含最新状态
        return list.stream().map(server -> {
            ServerVO vo = convertToVO(server);
            // 自动检测并更新状态（异步，不影响返回）
            try {
                checkServerStatus(server.getId());
            } catch (Exception e) {
                log.debug("检测服务器状态失败, ID: {}", server.getId());
            }
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public ServerVO getServerById(Long id) {
        Server server = this.getById(id);
        if (server == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        return convertToVO(server);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addServer(ServerDTO serverDTO) {
        // 检查IP地址是否已存在
        LambdaQueryWrapper<Server> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Server::getIpAddress, serverDTO.getIpAddress());
        if (this.count(wrapper) > 0) {
            throw new GlobalExceptionHandler.BusinessException("该IP地址的服务器已存在");
        }
        
        // 构建实体
        Server server = new Server();
        BeanUtils.copyProperties(serverDTO, server);
        
        // 加密认证凭证
        String encryptedCredential = aesUtil.encrypt(serverDTO.getAuthCredential());
        server.setAuthCredential(encryptedCredential);
        
        // 设置初始状态为离线
        server.setStatus(2); // 离线
        server.setCurrentTasks(0);
        
        // 保存
        this.save(server);
        
        log.info("添加服务器成功, ID: {}, 名称: {}", server.getId(), server.getServerName());
        
        // 异步测试连接并更新状态
        try {
            testAndUpdateStatus(server.getId());
        } catch (Exception e) {
            log.warn("自动检测服务器状态失败, ID: {}, 错误: {}", server.getId(), e.getMessage());
        }
        
        return server.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateServer(ServerDTO serverDTO) {
        if (serverDTO.getId() == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器ID不能为空");
        }
        
        // 检查服务器是否存在
        Server existServer = this.getById(serverDTO.getId());
        if (existServer == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        
        // 检查IP地址是否被其他服务器占用
        LambdaQueryWrapper<Server> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Server::getIpAddress, serverDTO.getIpAddress());
        wrapper.ne(Server::getId, serverDTO.getId());
        if (this.count(wrapper) > 0) {
            throw new GlobalExceptionHandler.BusinessException("该IP地址已被其他服务器占用");
        }
        
        // 更新实体
        Server server = new Server();
        BeanUtils.copyProperties(serverDTO, server);
        
        // 加密认证凭证
        String encryptedCredential = aesUtil.encrypt(serverDTO.getAuthCredential());
        server.setAuthCredential(encryptedCredential);
        
        // 更新
        boolean success = this.updateById(server);
        
        if (success) {
            log.info("更新服务器成功, ID: {}, 名称: {}", server.getId(), server.getServerName());
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteServer(Long id) {
        Server server = this.getById(id);
        if (server == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        
        // 检查是否有正在运行的任务
        if (server.getCurrentTasks() != null && server.getCurrentTasks() > 0) {
            throw new GlobalExceptionHandler.BusinessException("该服务器有正在运行的任务,无法删除");
        }
        
        boolean success = this.removeById(id);
        
        if (success) {
            log.info("删除服务器成功, ID: {}, 名称: {}", id, server.getServerName());
        }
        
        return success;
    }
    
    @Override
    public Boolean testConnection(Long id) {
        Server server = this.getById(id);
        if (server == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        
        try {
            // 解密认证凭证
            String credential = aesUtil.decrypt(server.getAuthCredential());
            
            // 测试SSH连接
            boolean connected = sshUtil.testConnection(
                    server.getIpAddress(),
                    server.getSshPort(),
                    server.getSshUsername(),
                    credential
            );
            
            if (connected) {
                log.info("服务器连接测试成功, ID: {}, IP: {}", id, server.getIpAddress());
                return true;
            } else {
                log.warn("服务器连接测试失败, ID: {}, IP: {}", id, server.getIpAddress());
                return false;
            }
        } catch (Exception e) {
            log.error("服务器连接测试异常, ID: {}, 错误: {}", id, e.getMessage());
            throw new GlobalExceptionHandler.BusinessException("连接测试失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean refreshServerStatus(Long id) {
        Server server = this.getById(id);
        if (server == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        
        try {
            // 解密认证凭证
            String credential = aesUtil.decrypt(server.getAuthCredential());
            
            // 获取CPU使用率
            String cpuResult = sshUtil.getCpuUsage(
                    server.getIpAddress(),
                    server.getSshPort(),
                    server.getSshUsername(),
                    credential
            );
            BigDecimal cpuUsage = new BigDecimal(cpuResult.trim());
            
            // 获取内存使用率
            String memResult = sshUtil.getMemoryUsage(
                    server.getIpAddress(),
                    server.getSshPort(),
                    server.getSshUsername(),
                    credential
            );
            BigDecimal memoryUsage = new BigDecimal(memResult.trim());
            
            // 获取磁盘使用率
            String diskResult = sshUtil.getDiskUsage(
                    server.getIpAddress(),
                    server.getSshPort(),
                    server.getSshUsername(),
                    credential
            );
            BigDecimal diskUsage = new BigDecimal(diskResult.trim());
            
            // 获取网络流量
            String networkResult = sshUtil.getNetworkTraffic(
                    server.getIpAddress(),
                    server.getSshPort(),
                    server.getSshUsername(),
                    credential
            );
            String[] networkParts = networkResult.split(",");
            Long networkIn = networkParts.length > 0 ? Long.parseLong(networkParts[0].trim()) : 0L;
            Long networkOut = networkParts.length > 1 ? Long.parseLong(networkParts[1].trim()) : 0L;
            
            // 更新服务器状态
            server.setCpuUsage(cpuUsage);
            server.setMemoryUsage(memoryUsage);
            server.setDiskUsage(diskUsage);
            server.setNetworkIn(networkIn);
            server.setNetworkOut(networkOut);
            server.setStatus(1); // 在线
            server.setLastHeartbeatTime(LocalDateTime.now());
            
            this.updateById(server);
            
            log.info("刷新服务器状态成功, ID: {}, CPU: {}%, 内存: {}%, 磁盘: {}%, 网络: {}/{} KB/s", 
                    id, cpuUsage, memoryUsage, diskUsage, networkIn, networkOut);
            return true;
            
        } catch (Exception e) {
            // 更新为离线状态，不抛异常
            server.setStatus(2); // 离线
            server.setCpuUsage(null);
            server.setMemoryUsage(null);
            server.setDiskUsage(null);
            server.setNetworkIn(null);
            server.setNetworkOut(null);
            this.updateById(server);
            
            log.warn("服务器无法连接，状态已更新为离线, ID: {}, 错误: {}", id, e.getMessage());
            return true; // 返回true表示状态已成功更新
        }
    }
    
    @Override
    public String listDockerContainers(Long id) {
        Server server = this.getById(id);
        if (server == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        
        if (server.getDockerPort() == null) {
            throw new GlobalExceptionHandler.BusinessException("该服务器未配置Docker端口");
        }
        
        try {
            DockerClient client = dockerUtil.createClient(
                    server.getIpAddress(), 
                    server.getDockerPort()
            );
            
            List<Container> containers = dockerUtil.listContainers(client, true);
            
            // 转换为简化的JSON格式
            List<Map<String, Object>> result = containers.stream().map(c -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("names", c.getNames());
                map.put("image", c.getImage());
                map.put("status", c.getStatus());
                map.put("state", c.getState());
                return map;
            }).collect(Collectors.toList());
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            log.error("获取Docker容器列表失败, ID: {}, 错误: {}", id, e.getMessage());
            throw new GlobalExceptionHandler.BusinessException("获取容器列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public Boolean startDockerContainer(Long serverId, String containerId) {
        Server server = this.getById(serverId);
        if (server == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        
        try {
            DockerClient client = dockerUtil.createClient(
                    server.getIpAddress(), 
                    server.getDockerPort()
            );
            
            dockerUtil.startContainer(client, containerId);
            
            log.info("启动Docker容器成功, 服务器ID: {}, 容器ID: {}", serverId, containerId);
            return true;
            
        } catch (Exception e) {
            log.error("启动Docker容器失败, 错误: {}", e.getMessage());
            throw new GlobalExceptionHandler.BusinessException("启动容器失败: " + e.getMessage());
        }
    }
    
    @Override
    public Boolean stopDockerContainer(Long serverId, String containerId) {
        Server server = this.getById(serverId);
        if (server == null) {
            throw new GlobalExceptionHandler.BusinessException("服务器不存在");
        }
        
        try {
            DockerClient client = dockerUtil.createClient(
                    server.getIpAddress(), 
                    server.getDockerPort()
            );
            
            dockerUtil.stopContainer(client, containerId);
            
            log.info("停止Docker容器成功, 服务器ID: {}, 容器ID: {}", serverId, containerId);
            return true;
            
        } catch (Exception e) {
            log.error("停止Docker容器失败, 错误: {}", e.getMessage());
            throw new GlobalExceptionHandler.BusinessException("停止容器失败: " + e.getMessage());
        }
    }
    
    /**
     * 实体转VO
     */
    private ServerVO convertToVO(Server server) {
        ServerVO vo = new ServerVO();
        BeanUtils.copyProperties(server, vo);
        
        // 设置状态文本
        if (server.getStatus() != null) {
            switch (server.getStatus()) {
                case 1 -> vo.setStatusText("在线");
                case 2 -> vo.setStatusText("关机");
                case 3 -> vo.setStatusText("异常");
                default -> vo.setStatusText("未知");
            }
        }
        
        return vo;
    }
    
    /**
     * 测试连接并更新服务器状态
     */
    private void testAndUpdateStatus(Long id) {
        Server server = this.getById(id);
        if (server == null) {
            return;
        }
        
        try {
            // 解密认证凭证
            String credential = aesUtil.decrypt(server.getAuthCredential());
            
            // 测试SSH连接
            boolean connected = sshUtil.testConnection(
                    server.getIpAddress(),
                    server.getSshPort(),
                    server.getSshUsername(),
                    credential
            );
            
            // 更新状态
            if (connected) {
                server.setStatus(1); // 在线
                server.setLastHeartbeatTime(LocalDateTime.now());
                log.info("服务器连接成功, ID: {}, IP: {}", id, server.getIpAddress());
            } else {
                server.setStatus(2); // 离线
                log.warn("服务器连接失败, ID: {}, IP: {}", id, server.getIpAddress());
            }
            
            this.updateById(server);
            
        } catch (Exception e) {
            // 更新为异常状态
            server.setStatus(3);
            this.updateById(server);
            log.error("服务器状态检测异常, ID: {}, 错误: {}", id, e.getMessage());
        }
    }
    
    /**
     * 检查服务器状态（轻量级）
     */
    private void checkServerStatus(Long id) {
        Server server = this.getById(id);
        if (server == null) {
            return;
        }
        
        // 如果最后心跳时间超过5分钟，尝试刷新状态
        if (server.getLastHeartbeatTime() == null || 
            server.getLastHeartbeatTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
            try {
                testAndUpdateStatus(id);
            } catch (Exception e) {
                log.debug("自动检测服务器状态失败, ID: {}", id);
            }
        }
    }
    
    @Override
    public Integer batchRefreshStatus(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        for (Long id : ids) {
            try {
                refreshServerStatus(id);
                successCount++;
            } catch (Exception e) {
                log.error("批量刷新服务器状态失败, ID: {}", id, e);
            }
        }
        
        log.info("批量刷新服务器状态完成, 总数: {}, 成功: {}", ids.size(), successCount);
        return successCount;
    }
    
    @Override
    public Integer refreshAllServersStatus() {
        List<Server> allServers = this.list();
        if (allServers == null || allServers.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        for (Server server : allServers) {
            try {
                refreshServerStatus(server.getId());
                successCount++;
            } catch (Exception e) {
                log.error("刷新服务器状态失败, ID: {}", server.getId(), e);
            }
        }
        
        log.info("刷新所有服务器状态完成, 总数: {}, 成功: {}", allServers.size(), successCount);
        return successCount;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchAddServers(List<ServerDTO> serverDTOList) {
        if (serverDTOList == null || serverDTOList.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < serverDTOList.size(); i++) {
            ServerDTO dto = serverDTOList.get(i);
            try {
                addServer(dto);
                successCount++;
            } catch (Exception e) {
                String error = String.format("第%d个服务器(%s)添加失败: %s", 
                    i + 1, dto.getIpAddress(), e.getMessage());
                errors.add(error);
                log.error(error, e);
            }
        }
        
        if (!errors.isEmpty()) {
            log.warn("批量添加服务器部分失败: {}", String.join("; ", errors));
        }
        
        log.info("批量添加服务器完成, 总数: {}, 成功: {}, 失败: {}", 
            serverDTOList.size(), successCount, serverDTOList.size() - successCount);
        
        return successCount;
    }
}
