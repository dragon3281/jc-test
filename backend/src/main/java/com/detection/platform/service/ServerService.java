package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.dto.ServerDTO;
import com.detection.platform.entity.Server;
import com.detection.platform.vo.ServerVO;

import java.util.List;

/**
 * 服务器管理Service接口
 */
public interface ServerService extends IService<Server> {
    
    /**
     * 分页查询服务器列表
     * 
     * @param current 当前页
     * @param size 每页大小
     * @param serverName 服务器名称(模糊查询)
     * @param status 服务器状态
     * @return 分页结果
     */
    Page<ServerVO> pageServers(Integer current, Integer size, String serverName, Integer status);
    
    /**
     * 获取所有服务器列表
     * 
     * @return 服务器列表
     */
    List<ServerVO> listAllServers();
    
    /**
     * 根据ID获取服务器详情
     * 
     * @param id 服务器ID
     * @return 服务器详情
     */
    ServerVO getServerById(Long id);
    
    /**
     * 添加服务器
     * 
     * @param serverDTO 服务器信息
     * @return 服务器ID
     */
    Long addServer(ServerDTO serverDTO);
    
    /**
     * 更新服务器
     * 
     * @param serverDTO 服务器信息
     * @return 是否成功
     */
    Boolean updateServer(ServerDTO serverDTO);
    
    /**
     * 删除服务器
     * 
     * @param id 服务器ID
     * @return 是否成功
     */
    Boolean deleteServer(Long id);
    
    /**
     * 测试SSH连接
     * 
     * @param id 服务器ID
     * @return 是否连接成功
     */
    Boolean testConnection(Long id);
    
    /**
     * 获取服务器资源使用情况
     * 
     * @param id 服务器ID
     * @return 是否成功
     */
    Boolean refreshServerStatus(Long id);
    
    /**
     * 获取服务器上的Docker容器列表
     * 
     * @param id 服务器ID
     * @return 容器列表JSON
     */
    String listDockerContainers(Long id);
    
    /**
     * 启动Docker容器
     * 
     * @param serverId 服务器ID
     * @param containerId 容器ID
     * @return 是否成功
     */
    Boolean startDockerContainer(Long serverId, String containerId);
    
    /**
     * 停止Docker容器
     * 
     * @param serverId 服务器ID
     * @param containerId 容器ID
     * @return 是否成功
     */
    Boolean stopDockerContainer(Long serverId, String containerId);
    
    /**
     * 批量刷新服务器状态
     * 
     * @param ids 服务器ID列表
     * @return 成功数量
     */
    Integer batchRefreshStatus(List<Long> ids);
    
    /**
     * 刷新所有服务器状态
     * 
     * @return 成功数量
     */
    Integer refreshAllServersStatus();
    
    /**
     * 批量添加服务器
     * 
     * @param serverDTOList 服务器信息列表
     * @return 成功数量
     */
    Integer batchAddServers(List<ServerDTO> serverDTOList);
}
