package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.dto.ServerDTO;
import com.detection.platform.service.ServerService;
import com.detection.platform.vo.ServerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务器管理Controller
 */
@RestController
@RequestMapping("/server")
@RequiredArgsConstructor
public class ServerController {
    
    private final ServerService serverService;
    
    /**
     * 分页查询服务器列表
     */
    @GetMapping("/page")
    public Result<Page<ServerVO>> pageServers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String serverName,
            @RequestParam(required = false) Integer status) {
        Page<ServerVO> page = serverService.pageServers(current, size, serverName, status);
        return Result.success(page);
    }
    
    /**
     * 获取所有服务器列表(不分页)
     */
    @GetMapping("/list")
    public Result<List<ServerVO>> listAllServers() {
        List<ServerVO> list = serverService.listAllServers();
        return Result.success(list);
    }
    
    /**
     * 根据ID获取服务器详情
     */
    @GetMapping("/{id}")
    public Result<ServerVO> getServerById(@PathVariable Long id) {
        ServerVO server = serverService.getServerById(id);
        return Result.success(server);
    }
    
    /**
     * 添加服务器
     */
    @PostMapping
    public Result<Long> addServer(@Valid @RequestBody ServerDTO serverDTO) {
        Long id = serverService.addServer(serverDTO);
        return Result.success("添加服务器成功", id);
    }
    
    /**
     * 更新服务器
     */
    @PutMapping
    public Result<Void> updateServer(@Valid @RequestBody ServerDTO serverDTO) {
        serverService.updateServer(serverDTO);
        return Result.successMsg("更新服务器成功");
    }
    
    /**
     * 删除服务器
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return Result.successMsg("删除服务器成功");
    }
    
    /**
     * 测试SSH连接
     */
    @PostMapping("/{id}/test")
    public Result<Void> testConnection(@PathVariable Long id) {
        Boolean success = serverService.testConnection(id);
        if (success) {
            return Result.successMsg("连接成功");
        } else {
            return Result.error("连接失败");
        }
    }
    
    /**
     * 刷新服务器状态
     */
    @PostMapping("/{id}/refresh")
    public Result<Void> refreshServerStatus(@PathVariable Long id) {
        serverService.refreshServerStatus(id);
        return Result.successMsg("刷新成功");
    }
    
    /**
     * 获取Docker容器列表
     */
    @GetMapping("/{id}/containers")
    public Result<String> listDockerContainers(@PathVariable Long id) {
        String containers = serverService.listDockerContainers(id);
        return Result.success(containers);
    }
    
    /**
     * 启动Docker容器
     */
    @PostMapping("/{serverId}/containers/{containerId}/start")
    public Result<Void> startDockerContainer(
            @PathVariable Long serverId,
            @PathVariable String containerId) {
        serverService.startDockerContainer(serverId, containerId);
        return Result.successMsg("启动容器成功");
    }
    
    /**
     * 停止Docker容器
     */
    @PostMapping("/{serverId}/containers/{containerId}/stop")
    public Result<Void> stopDockerContainer(
            @PathVariable Long serverId,
            @PathVariable String containerId) {
        serverService.stopDockerContainer(serverId, containerId);
        return Result.successMsg("停止容器成功");
    }
    
    /**
     * 批量刷新服务器状态
     */
    @PostMapping("/batch/refresh")
    public Result<Integer> batchRefreshStatus(@RequestBody List<Long> ids) {
        Integer count = serverService.batchRefreshStatus(ids);
        return Result.success("批量刷新完成", count);
    }
    
    /**
     * 刷新所有服务器状态
     */
    @PostMapping("/refresh/all")
    public Result<Integer> refreshAllServersStatus() {
        Integer count = serverService.refreshAllServersStatus();
        return Result.success("刷新所有服务器状态完成", count);
    }
    
    /**
     * 批量添加服务器
     */
    @PostMapping("/batch")
    public Result<Integer> batchAddServers(@Valid @RequestBody List<ServerDTO> serverDTOList) {
        Integer count = serverService.batchAddServers(serverDTOList);
        return Result.success("批量添加完成", count);
    }
}
