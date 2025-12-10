package com.detection.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.detection.platform.common.utils.Result;
import com.detection.platform.common.utils.ProxyConfigParser;
import com.detection.platform.dto.ProxyNodeDTO;
import com.detection.platform.dto.ProxyPoolDTO;
import com.detection.platform.service.ProxyNodeService;
import com.detection.platform.service.ProxyPoolService;
import com.detection.platform.vo.ProxyNodeVO;
import com.detection.platform.vo.ProxyPoolVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * 代理资源池Controller
 */
@Slf4j
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
    public Result<List<ProxyPoolVO>> listAllProxyPools(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String keyword) {
        List<ProxyPoolVO> list = proxyPoolService.listAllProxyPools();
        
        // 前端筛选（也可以在Service层实现）
        if (country != null && !country.isEmpty()) {
            list = list.stream()
                    .filter(p -> country.equals(p.getCountry()))
                    .collect(Collectors.toList());
        }
        if (groupName != null && !groupName.isEmpty()) {
            list = list.stream()
                    .filter(p -> groupName.equals(p.getGroupName()))
                    .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isEmpty()) {
            list = list.stream()
                    .filter(p -> (p.getPoolName() != null && p.getPoolName().contains(keyword))
                            || (p.getProxyIp() != null && p.getProxyIp().contains(keyword)))
                    .collect(Collectors.toList());
        }
        
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
    
    /**
     * 快速测试代理配置（通过配置字符串）
     * 用于测试SOCKS等代理格式
     * 
     * @param configStr 代理配置字符串，格式如：socks://base64(username:password)@host:port#label
     * @return 测试结果
     */
    @PostMapping("/test/config")
    public Result<Map<String, Object>> testProxyConfig(@RequestParam String configStr) {
        log.info("收到代理配置测试请求: {}", configStr);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析配置
            ProxyConfigParser.ProxyConfig config = ProxyConfigParser.parse(configStr);
            result.put("parsed", true);
            result.put("protocol", config.getProtocol());
            result.put("host", config.getHost());
            result.put("port", config.getPort());
            result.put("hasAuth", config.hasAuth());
            result.put("username", config.getUsername());
            result.put("password", config.getPassword());
            result.put("label", config.getLabel());
            result.put("proxyType", config.getProxyType());
            
            log.info("代理配置解析成功: {}://{}:{} (type={})", 
                    config.getProtocol(), config.getHost(), config.getPort(), config.getProxyType());
            
            // 创建临时代理池和节点进行测试
            ProxyPoolDTO poolDTO = new ProxyPoolDTO();
            poolDTO.setPoolName("临时测试-" + System.currentTimeMillis());
            poolDTO.setProxyIp(config.getHost());
            poolDTO.setProxyPort(config.getPort());
            poolDTO.setProxyType(config.getProxyType());
            
            if (config.hasAuth()) {
                poolDTO.setNeedAuth(1);
                poolDTO.setUsername(config.getUsername());
                poolDTO.setPassword(config.getPassword());
            } else {
                poolDTO.setNeedAuth(0);
            }
            
            // 添加代理池
            Long poolId = proxyPoolService.addProxyPool(poolDTO);
            result.put("poolId", poolId);
            
            // 添加代理节点
            ProxyNodeDTO nodeDTO = new ProxyNodeDTO();
            nodeDTO.setPoolId(poolId);
            nodeDTO.setProxyIp(config.getHost());
            nodeDTO.setProxyPort(config.getPort());
            if (config.hasAuth()) {
                nodeDTO.setNeedAuth(1);
                nodeDTO.setUsername(config.getUsername());
                nodeDTO.setPassword(config.getPassword());
            }
            
            Long nodeId = proxyNodeService.addProxyNode(nodeDTO);
            result.put("nodeId", nodeId);
            
            log.info("开始测试代理: poolId={}, nodeId={}", poolId, nodeId);
            
            // 测试代理
            Boolean testResult = proxyNodeService.checkProxyNode(nodeId);
            result.put("testResult", testResult);
            result.put("available", testResult);
            
            // 获取测试后的节点信息
            ProxyNodeVO nodeVO = proxyNodeService.getProxyNodeById(nodeId);
            result.put("responseTime", nodeVO.getResponseTime());
            result.put("healthScore", nodeVO.getHealthScore());
            result.put("status", nodeVO.getStatus());
            result.put("statusText", nodeVO.getStatusText());
            
            String message = testResult ? 
                    "✓ 代理可用！响应时间: " + nodeVO.getResponseTime() + "ms" : 
                    "✗ 代理不可用";
            
            log.info("代理测试完成: {}", message);
            
            return Result.success(message, result);
            
        } catch (Exception e) {
            log.error("代理配置测试失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return Result.error("测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析代理配置字符串（不测试，只解析）
     * 用于一键识别功能
     * 
     * @param configStr 代理配置字符串
     * @return 解析结果
     */
    @PostMapping("/parse/config")
    public Result<Map<String, Object>> parseProxyConfig(@RequestParam String configStr) {
        log.info("收到代理配置解析请求: {}", configStr);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析配置
            ProxyConfigParser.ProxyConfig config = ProxyConfigParser.parse(configStr);
            
            result.put("parsed", true);
            result.put("protocol", config.getProtocol());
            result.put("host", config.getHost());
            result.put("port", config.getPort());
            result.put("hasAuth", config.hasAuth());
            result.put("username", config.getUsername());
            result.put("password", config.getPassword());
            result.put("label", config.getLabel());
            result.put("proxyType", config.getProxyType());
            
            log.info("代理配置解析成功: {}://{}:{} (type={})", 
                    config.getProtocol(), config.getHost(), config.getPort(), config.getProxyType());
            
            return Result.success("解析成功", result);
            
        } catch (Exception e) {
            log.error("代理配置解析失败: {}", e.getMessage());
            result.put("error", e.getMessage());
            return Result.error("解析失败: " + e.getMessage());
        }
    }
    
    // ==================== 分组管理（基于标签设计）====================
    
    /**
     * 获取所有分组列表（从代理节点的groupName字段去重获取）
     */
    @GetMapping("/groups")
    public Result<List<String>> getAllGroups() {
        // 从代理节点的groupName字段去重获取所有分组
        List<ProxyPoolVO> pools = proxyPoolService.listAllProxyPools();
        List<String> groupNames = pools.stream()
                .map(ProxyPoolVO::getGroupName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return Result.success(groupNames);
    }
    
    /**
     * 获取分组详情列表（带节点数量统计）
     */
    @GetMapping("/groups/detail")
    public Result<List<Map<String, Object>>> getGroupsDetail() {
        List<ProxyPoolVO> pools = proxyPoolService.listAllProxyPools();
        
        // 统计每个分组的节点数量
        Map<String, Long> groupCounts = pools.stream()
                .filter(p -> p.getGroupName() != null && !p.getGroupName().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        p -> p.getGroupName().trim(),
                        Collectors.counting()
                ));
        
        // 构建返回结果
        List<Map<String, Object>> result = groupCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("groupName", entry.getKey());
                    map.put("nodeCount", entry.getValue());
                    return map;
                })
                .sorted((g1, g2) -> ((String)g1.get("groupName")).compareTo((String)g2.get("groupName")))
                .collect(Collectors.toList());
        
        return Result.success(result);
    }
    
    /**
     * 重命名分组（批量更新节点的groupName字段）
     */
    @PutMapping("/groups/rename")
    public Result<Void> renameGroup(@RequestParam String oldName, @RequestParam String newName) {
        if (oldName == null || oldName.trim().isEmpty()) {
            return Result.error("旧分组名称不能为空");
        }
        if (newName == null || newName.trim().isEmpty()) {
            return Result.error("新分组名称不能为空");
        }
        if (oldName.trim().equals(newName.trim())) {
            return Result.error("新名称与原名称相同");
        }
        
        try {
            // 检查新名称是否已存在
            List<ProxyPoolVO> pools = proxyPoolService.listAllProxyPools();
            boolean newNameExists = pools.stream()
                    .anyMatch(p -> newName.trim().equals(p.getGroupName()));
            if (newNameExists) {
                return Result.error("分组名称\"" + newName + "\"已存在");
            }
            
            // 查找所有使用旧分组名的节点
            List<ProxyPoolVO> nodesToUpdate = pools.stream()
                    .filter(p -> oldName.trim().equals(p.getGroupName()))
                    .collect(Collectors.toList());
            
            if (nodesToUpdate.isEmpty()) {
                return Result.error("分组\"" + oldName + "\"不存在或没有节点");
            }
            
            // 批量更新节点分组名
            for (ProxyPoolVO node : nodesToUpdate) {
                ProxyPoolDTO dto = new ProxyPoolDTO();
                dto.setId(node.getId());
                dto.setGroupName(newName.trim());
                dto.setPoolName(node.getPoolName());
                dto.setProxyIp(node.getProxyIp());
                dto.setProxyPort(node.getProxyPort());
                dto.setProxyType(node.getProxyType());
                dto.setNeedAuth(node.getAuthType());
                dto.setCountry(node.getCountry());
                
                proxyPoolService.updateProxyPool(dto);
            }
            
            log.info("分组重命名成功: \"{}\" -> \"{}\", 更新节点数: {}", oldName, newName, nodesToUpdate.size());
            return Result.successMsg("分组重命名成功，已更新 " + nodesToUpdate.size() + " 个节点");
        } catch (Exception e) {
            log.error("重命名分组失败: {}", e.getMessage());
            return Result.error("重命名分组失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除分组（清空所有节点的该分组标签）
     */
    @DeleteMapping("/groups")
    public Result<Void> deleteGroup(@RequestParam String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return Result.error("分组名称不能为空");
        }
        
        try {
            // 查找使用该分组的所有节点
            List<ProxyPoolVO> pools = proxyPoolService.listAllProxyPools();
            List<ProxyPoolVO> nodesToClear = pools.stream()
                    .filter(p -> groupName.trim().equals(p.getGroupName()))
                    .collect(Collectors.toList());
            
            if (nodesToClear.isEmpty()) {
                return Result.error("分组\"" + groupName + "\"不存在或没有节点");
            }
            
            // 批量清空节点分组
            for (ProxyPoolVO node : nodesToClear) {
                ProxyPoolDTO dto = new ProxyPoolDTO();
                dto.setId(node.getId());
                dto.setGroupName(""); // 清空分组
                dto.setPoolName(node.getPoolName());
                dto.setProxyIp(node.getProxyIp());
                dto.setProxyPort(node.getProxyPort());
                dto.setProxyType(node.getProxyType());
                dto.setNeedAuth(node.getAuthType());
                dto.setCountry(node.getCountry());
                
                proxyPoolService.updateProxyPool(dto);
            }
            
            log.info("分组删除成功: \"{}\", 清空节点数: {}", groupName, nodesToClear.size());
            return Result.successMsg("分组删除成功，已清空 " + nodesToClear.size() + " 个节点的分组");
        } catch (Exception e) {
            log.error("删除分组失败: {}", e.getMessage());
            return Result.error("删除分组失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有国家列表
     */
    @GetMapping("/countries")
    public Result<List<String>> getAllCountries() {
        List<ProxyPoolVO> list = proxyPoolService.listAllProxyPools();
        Set<String> countries = list.stream()
                .map(ProxyPoolVO::getCountry)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());
        return Result.success(countries.stream().sorted().collect(Collectors.toList()));
    }
    
    /**
     * 更新代理节点国家
     */
    @PutMapping("/pool/{id}/country")
    public Result<Void> updateCountry(@PathVariable Long id, @RequestParam String country) {
        ProxyPoolDTO dto = new ProxyPoolDTO();
        dto.setId(id);
        dto.setCountry(country);
        
        // 只更新国家字段
        ProxyPoolVO existing = proxyPoolService.getProxyPoolById(id);
        dto.setPoolName(existing.getPoolName());
        dto.setProxyIp(existing.getProxyIp());
        dto.setProxyPort(existing.getProxyPort());
        dto.setProxyType(existing.getProxyType());
        dto.setNeedAuth(existing.getAuthType());
        dto.setGroupName(existing.getGroupName());
        
        proxyPoolService.updateProxyPool(dto);
        return Result.successMsg("更新国家成功");
    }
    
    /**
     * 更新代理节点分组
     */
    @PutMapping("/pool/{id}/group")
    public Result<Void> updateGroup(@PathVariable Long id, @RequestParam String groupName) {
        ProxyPoolDTO dto = new ProxyPoolDTO();
        dto.setId(id);
        dto.setGroupName(groupName);
        
        // 只更新分组字段
        ProxyPoolVO existing = proxyPoolService.getProxyPoolById(id);
        dto.setPoolName(existing.getPoolName());
        dto.setProxyIp(existing.getProxyIp());
        dto.setProxyPort(existing.getProxyPort());
        dto.setProxyType(existing.getProxyType());
        dto.setNeedAuth(existing.getAuthType());
        dto.setCountry(existing.getCountry());
        
        proxyPoolService.updateProxyPool(dto);
        return Result.successMsg("更新分组成功");
    }
    
    /**
     * 批量更新代理节点分组
     */
    @PutMapping("/pool/batch/group")
    public Result<Void> batchUpdateGroup(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Object> idsObj = (List<Object>) params.get("ids");
        String groupName = (String) params.get("groupName");
        
        if (idsObj == null || idsObj.isEmpty()) {
            return Result.error("请选择要更新的节点");
        }
        
        // 将Integer/Long统一转换为Long类型
        List<Long> ids = idsObj.stream()
                .map(obj -> obj instanceof Integer ? ((Integer) obj).longValue() : (Long) obj)
                .collect(Collectors.toList());
        
        int successCount = 0;
        for (Long id : ids) {
            try {
                ProxyPoolVO existing = proxyPoolService.getProxyPoolById(id);
                if (existing != null) {
                    ProxyPoolDTO dto = new ProxyPoolDTO();
                    dto.setId(id);
                    dto.setGroupName(groupName != null ? groupName : "");
                    dto.setPoolName(existing.getPoolName());
                    dto.setProxyIp(existing.getProxyIp());
                    dto.setProxyPort(existing.getProxyPort());
                    dto.setProxyType(existing.getProxyType());
                    dto.setNeedAuth(existing.getAuthType());
                    dto.setCountry(existing.getCountry());
                    
                    proxyPoolService.updateProxyPool(dto);
                    successCount++;
                }
            } catch (Exception e) {
                log.error("更新节点 {} 分组失败: {}", id, e.getMessage());
            }
        }
        
        log.info("批量更新分组完成，成功: {}/{}", successCount, ids.size());
        return Result.successMsg("批量更新完成，成功 " + successCount + "/" + ids.size() + " 个节点");
    }
    
    /**
     * 手动检测代理池（实际连接测试）
     */
    @PostMapping("/pool/{id}/check")
    public Result<Map<String, Object>> checkProxyPool(@PathVariable Long id) {
        log.info("🔍 [手动检测] 收到代理池检测请求, ID: {}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 调用service层的检测方法
            proxyPoolService.checkProxyPool(id);
            
            // 获取检测后的状态
            ProxyPoolVO pool = proxyPoolService.getProxyPoolById(id);
            result.put("poolId", pool.getId());
            result.put("poolName", pool.getPoolName());
            result.put("status", pool.getStatus());
            result.put("healthScore", pool.getHealthScore());
            result.put("lastCheckTime", pool.getLastCheckTime());
            result.put("responseTime", pool.getResponseTime());
            
            String message = pool.getStatus() == 1 ? 
                    "✅ 代理可用！响应时间: " + pool.getResponseTime() + "ms" : 
                    "❌ 代理不可用";
            
            log.info("✅ [手动检测] 检测完成, 结果: {}", message);
            return Result.success(message, result);
            
        } catch (Exception e) {
            log.error("❌ [手动检测] 检测失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return Result.error("检测失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量检测代理池
     */
    @PostMapping("/pool/batch/check")
    public Result<Map<String, Object>> batchCheckProxyPools(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Object> idsObj = (List<Object>) params.get("ids");
        
        if (idsObj == null || idsObj.isEmpty()) {
            return Result.error("请选择要检测的代理池");
        }
        
        // 将Integer/Long统一转换为Long类型
        List<Long> ids = idsObj.stream()
                .map(obj -> obj instanceof Integer ? ((Integer) obj).longValue() : (Long) obj)
                .collect(Collectors.toList());
        
        log.info("🔍 [批量检测] 开始批量检测代理池, 总数: {}", ids.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Long id : ids) {
            try {
                proxyPoolService.checkProxyPool(id);
                ProxyPoolVO pool = proxyPoolService.getProxyPoolById(id);
                if (pool.getStatus() == 1) {
                    successCount++;
                    log.info("  ✅ 代理池 {} 检测成功", id);
                } else {
                    failCount++;
                    log.warn("  ❌ 代理池 {} 检测失败", id);
                }
            } catch (Exception e) {
                failCount++;
                log.error("检测代理池 {} 失败: {}", id, e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", successCount);
        result.put("fail", failCount);
        
        String message = String.format("检测完成：总数 %d，成功 %d，失败 %d", ids.size(), successCount, failCount);
        log.info("📊 [批量检测] {}", message);
        
        return Result.success(message, result);
    }
}
