package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.dto.ProxyNodeDTO;
import com.detection.platform.dto.ProxyPoolDTO;
import com.detection.platform.service.ProxyNodeService;
import com.detection.platform.service.ProxyPoolService;
import com.detection.platform.vo.ProxyNodeVO;
import com.detection.platform.vo.ProxyPoolVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代理资源池Controller
 */
@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class ProxyController {
    
    private final ProxyPoolService proxyPoolService;
    private final ProxyNodeService proxyNodeService;
    
    // ==================== 代理池管理 ====================
    
    /**
     * 分页查询代理池列表
     */
    @GetMapping("/pool/page")
    public Result<Page<ProxyPoolVO>> pageProxyPools(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String poolName,
            @RequestParam(required = false) Integer proxyType) {
        Page<ProxyPoolVO> page = proxyPoolService.pageProxyPools(current, size, poolName, proxyType);
        return Result.success(page);
    }
    
    /**
     * 获取所有代理池列表
     */
    @GetMapping("/pool/list")
    public Result<List<ProxyPoolVO>> listAllProxyPools() {
        List<ProxyPoolVO> list = proxyPoolService.listAllProxyPools();
        return Result.success(list);
    }
    
    /**
     * 根据ID获取代理池详情
     */
    @GetMapping("/pool/{id}")
    public Result<ProxyPoolVO> getProxyPoolById(@PathVariable Long id) {
        ProxyPoolVO pool = proxyPoolService.getProxyPoolById(id);
        return Result.success(pool);
    }
    
    /**
     * 添加代理池
     */
    @PostMapping("/pool")
    public Result<Long> addProxyPool(@Valid @RequestBody ProxyPoolDTO proxyPoolDTO) {
        Long id = proxyPoolService.addProxyPool(proxyPoolDTO);
        return Result.success("添加代理池成功", id);
    }
    
    /**
     * 更新代理池
     */
    @PutMapping("/pool")
    public Result<Void> updateProxyPool(@Valid @RequestBody ProxyPoolDTO proxyPoolDTO) {
        proxyPoolService.updateProxyPool(proxyPoolDTO);
        return Result.successMsg("更新代理池成功");
    }
    
    /**
     * 删除代理池
     */
    @DeleteMapping("/pool/{id}")
    public Result<Void> deleteProxyPool(@PathVariable Long id) {
        proxyPoolService.deleteProxyPool(id);
        return Result.successMsg("删除代理池成功");
    }
    
    /**
     * 刷新代理池统计
     */
    @PostMapping("/pool/{id}/refresh")
    public Result<Void> refreshPoolStats(@PathVariable Long id) {
        proxyPoolService.refreshPoolStats(id);
        return Result.successMsg("刷新统计成功");
    }
    
    // ==================== 代理节点管理 ====================
    
    /**
     * 根据代理池ID获取节点列表
     */
    @GetMapping("/node/list")
    public Result<List<ProxyNodeVO>> listNodesByPoolId(@RequestParam Long poolId) {
        List<ProxyNodeVO> list = proxyNodeService.listNodesByPoolId(poolId);
        return Result.success(list);
    }
    
    /**
     * 分页查询代理节点
     */
    @GetMapping("/node/page")
    public Result<Page<ProxyNodeVO>> pageProxyNodes(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) Integer status) {
        Page<ProxyNodeVO> page = proxyNodeService.pageProxyNodes(current, size, poolId, status);
        return Result.success(page);
    }
    
    /**
     * 根据ID获取代理节点详情
     */
    @GetMapping("/node/{id}")
    public Result<ProxyNodeVO> getProxyNodeById(@PathVariable Long id) {
        ProxyNodeVO node = proxyNodeService.getProxyNodeById(id);
        return Result.success(node);
    }
    
    /**
     * 添加代理节点
     */
    @PostMapping("/node")
    public Result<Long> addProxyNode(@Valid @RequestBody ProxyNodeDTO proxyNodeDTO) {
        Long id = proxyNodeService.addProxyNode(proxyNodeDTO);
        return Result.success("添加代理节点成功", id);
    }
    
    /**
     * 批量添加代理节点
     */
    @PostMapping("/node/batch")
    public Result<Integer> batchAddProxyNodes(@Valid @RequestBody List<ProxyNodeDTO> proxyNodeDTOList) {
        Integer count = proxyNodeService.batchAddProxyNodes(proxyNodeDTOList);
        return Result.success("批量添加成功,成功数量: " + count, count);
    }
    
    /**
     * 更新代理节点
     */
    @PutMapping("/node")
    public Result<Void> updateProxyNode(@Valid @RequestBody ProxyNodeDTO proxyNodeDTO) {
        proxyNodeService.updateProxyNode(proxyNodeDTO);
        return Result.successMsg("更新代理节点成功");
    }
    
    /**
     * 删除代理节点
     */
    @DeleteMapping("/node/{id}")
    public Result<Void> deleteProxyNode(@PathVariable Long id) {
        proxyNodeService.deleteProxyNode(id);
        return Result.successMsg("删除代理节点成功");
    }
    
    /**
     * 批量删除代理节点
     */
    @DeleteMapping("/node/batch")
    public Result<Void> batchDeleteProxyNodes(@RequestBody List<Long> ids) {
        proxyNodeService.batchDeleteProxyNodes(ids);
        return Result.successMsg("批量删除成功");
    }
    
    /**
     * 检测代理节点可用性
     */
    @PostMapping("/node/{id}/check")
    public Result<Boolean> checkProxyNode(@PathVariable Long id) {
        Boolean result = proxyNodeService.checkProxyNode(id);
        String message = result ? "代理可用" : "代理不可用";
        return Result.success(message, result);
    }
    
    /**
     * 批量检测代理池中的所有节点
     */
    @PostMapping("/node/check/batch")
    public Result<Integer> batchCheckProxyNodes(@RequestParam Long poolId) {
        Integer count = proxyNodeService.batchCheckProxyNodes(poolId);
        return Result.success("批量检测完成,可用数量: " + count, count);
    }
}
