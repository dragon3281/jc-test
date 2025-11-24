package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.common.utils.AesUtil;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.ProxyNodeMapper;
import com.detection.platform.dto.ProxyNodeDTO;
import com.detection.platform.entity.ProxyNode;
import com.detection.platform.service.ProxyNodeService;
import com.detection.platform.service.ProxyPoolService;
import com.detection.platform.vo.ProxyNodeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代理节点Service实现类
 */
@Slf4j
@Service
public class ProxyNodeServiceImpl extends ServiceImpl<ProxyNodeMapper, ProxyNode> implements ProxyNodeService {
    
    @Autowired
    private AesUtil aesUtil;
    
    @Lazy
    @Autowired
    private ProxyPoolService proxyPoolService;
    
    @Override
    public List<ProxyNodeVO> listNodesByPoolId(Long poolId) {
        LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyNode::getPoolId, poolId);
        wrapper.orderByDesc(ProxyNode::getCreateTime);
        
        List<ProxyNode> list = this.list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public Page<ProxyNodeVO> pageProxyNodes(Integer current, Integer size, Long poolId, Integer status) {
        LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(poolId != null, ProxyNode::getPoolId, poolId);
        wrapper.eq(status != null, ProxyNode::getStatus, status);
        wrapper.orderByDesc(ProxyNode::getHealthScore, ProxyNode::getCreateTime);
        
        Page<ProxyNode> page = this.page(new Page<>(current, size), wrapper);
        
        Page<ProxyNodeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ProxyNodeVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    public ProxyNodeVO getProxyNodeById(Long id) {
        ProxyNode node = this.getById(id);
        if (node == null) {
            throw new GlobalExceptionHandler.BusinessException("代理节点不存在");
        }
        return convertToVO(node);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addProxyNode(ProxyNodeDTO proxyNodeDTO) {
        // 检查代理是否已存在
        String proxyAddress = proxyNodeDTO.getProxyIp() + ":" + proxyNodeDTO.getProxyPort();
        LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyNode::getPoolId, proxyNodeDTO.getPoolId());
        wrapper.eq(ProxyNode::getProxyAddress, proxyAddress);
        if (this.count(wrapper) > 0) {
            throw new GlobalExceptionHandler.BusinessException("该代理节点已存在");
        }
        
        ProxyNode node = new ProxyNode();
        BeanUtils.copyProperties(proxyNodeDTO, node);
        
        // 构建代理地址
        node.setProxyAddress(proxyNodeDTO.getProxyIp() + ":" + proxyNodeDTO.getProxyPort());
        
        // 设置认证类型
        if (proxyNodeDTO.getNeedAuth() != null && proxyNodeDTO.getNeedAuth() == 1) {
            node.setAuthType(1);
        } else {
            node.setAuthType(0);
        }
        
        // 加密密码
        if (StringUtils.hasText(proxyNodeDTO.getPassword())) {
            String encryptedPassword = aesUtil.encrypt(proxyNodeDTO.getPassword());
            node.setPassword(encryptedPassword);
        }
        
        // 初始化状态
        node.setStatus(3); // 未检测
        node.setSuccessCount(0L);
        node.setFailCount(0L);
        node.setUseCount(0L);
        node.setHealthScore(0);
        
        this.save(node);
        
        // 刷新代理池统计
        proxyPoolService.refreshPoolStats(node.getPoolId());
        
        log.info("添加代理节点成功, ID: {}, IP: {}:{}", 
                node.getId(), proxyNodeDTO.getProxyIp(), proxyNodeDTO.getProxyPort());
        return node.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchAddProxyNodes(List<ProxyNodeDTO> proxyNodeDTOList) {
        if (proxyNodeDTOList == null || proxyNodeDTOList.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        Long poolId = null;
        
        for (ProxyNodeDTO dto : proxyNodeDTOList) {
            try {
                addProxyNode(dto);
                successCount++;
                poolId = dto.getPoolId();
            } catch (Exception e) {
                log.warn("批量添加代理节点失败: {}:{}, 错误: {}", 
                        dto.getProxyIp(), dto.getProxyPort(), e.getMessage());
            }
        }
        
        // 刷新代理池统计
        if (poolId != null) {
            proxyPoolService.refreshPoolStats(poolId);
        }
        
        log.info("批量添加代理节点完成, 成功: {}, 总数: {}", successCount, proxyNodeDTOList.size());
        return successCount;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProxyNode(ProxyNodeDTO proxyNodeDTO) {
        if (proxyNodeDTO.getId() == null) {
            throw new GlobalExceptionHandler.BusinessException("代理节点ID不能为空");
        }
        
        ProxyNode existNode = this.getById(proxyNodeDTO.getId());
        if (existNode == null) {
            throw new GlobalExceptionHandler.BusinessException("代理节点不存在");
        }
        
        // 检查代理是否被其他节点占用
        String proxyAddress = proxyNodeDTO.getProxyIp() + ":" + proxyNodeDTO.getProxyPort();
        LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyNode::getPoolId, proxyNodeDTO.getPoolId());
        wrapper.eq(ProxyNode::getProxyAddress, proxyAddress);
        wrapper.ne(ProxyNode::getId, proxyNodeDTO.getId());
        if (this.count(wrapper) > 0) {
            throw new GlobalExceptionHandler.BusinessException("该代理地址已被其他节点占用");
        }
        
        ProxyNode node = new ProxyNode();
        BeanUtils.copyProperties(proxyNodeDTO, node);
        
        // 构建代理地址
        node.setProxyAddress(proxyAddress);
        
        // 设置认证类型
        if (proxyNodeDTO.getNeedAuth() != null && proxyNodeDTO.getNeedAuth() == 1) {
            node.setAuthType(1);
        } else {
            node.setAuthType(0);
        }
        
        // 加密密码
        if (StringUtils.hasText(proxyNodeDTO.getPassword())) {
            String encryptedPassword = aesUtil.encrypt(proxyNodeDTO.getPassword());
            node.setPassword(encryptedPassword);
        }
        
        boolean success = this.updateById(node);
        
        if (success) {
            // 刷新代理池统计
            proxyPoolService.refreshPoolStats(node.getPoolId());
            log.info("更新代理节点成功, ID: {}", node.getId());
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProxyNode(Long id) {
        ProxyNode node = this.getById(id);
        if (node == null) {
            throw new GlobalExceptionHandler.BusinessException("代理节点不存在");
        }
        
        Long poolId = node.getPoolId();
        boolean success = this.removeById(id);
        
        if (success) {
            // 刷新代理池统计
            proxyPoolService.refreshPoolStats(poolId);
            log.info("删除代理节点成功, ID: {}", id);
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteProxyNodes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        // 获取第一个节点的poolId用于刷新统计
        ProxyNode firstNode = this.getById(ids.get(0));
        Long poolId = firstNode != null ? firstNode.getPoolId() : null;
        
        boolean success = this.removeByIds(ids);
        
        if (success && poolId != null) {
            // 刷新代理池统计
            proxyPoolService.refreshPoolStats(poolId);
            log.info("批量删除代理节点成功, 数量: {}", ids.size());
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean checkProxyNode(Long id) {
        ProxyNode node = this.getById(id);
        if (node == null) {
            throw new GlobalExceptionHandler.BusinessException("代理节点不存在");
        }
        
        boolean isAvailable = false;
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用代理访问测试URL
            Proxy proxy = new Proxy(Proxy.Type.HTTP, 
                    new InetSocketAddress(node.getProxyAddress().split(":")[0], 
                            Integer.parseInt(node.getProxyAddress().split(":")[1])));
            
            URL url = new URL("http://www.baidu.com");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            isAvailable = (responseCode == 200);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 更新节点信息
            if (isAvailable) {
                node.setStatus(1); // 可用
                node.setResponseTime((int) responseTime);
                node.setSuccessCount(node.getSuccessCount() + 1);
            } else {
                node.setStatus(2); // 不可用
                node.setFailCount(node.getFailCount() + 1);
            }
            
        } catch (Exception e) {
            node.setStatus(2); // 不可用
            node.setFailCount(node.getFailCount() + 1);
            log.warn("检测代理节点失败, ID: {}, 错误: {}", id, e.getMessage());
        }
        
        // 计算健康度评分
        long total = node.getSuccessCount() + node.getFailCount();
        if (total > 0) {
            BigDecimal score = BigDecimal.valueOf(node.getSuccessCount())
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            node.setHealthScore(score.intValue());
        }
        
        node.setLastCheckTime(LocalDateTime.now());
        this.updateById(node);
        
        // 刷新代理池统计
        proxyPoolService.refreshPoolStats(node.getPoolId());
        
        log.info("检测代理节点完成, ID: {}, 结果: {}", id, isAvailable ? "可用" : "不可用");
        return isAvailable;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchCheckProxyNodes(Long poolId) {
        LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyNode::getPoolId, poolId);
        List<ProxyNode> nodes = this.list(wrapper);
        
        if (nodes.isEmpty()) {
            return 0;
        }
        
        int availableCount = 0;
        for (ProxyNode node : nodes) {
            try {
                Boolean result = checkProxyNode(node.getId());
                if (result) {
                    availableCount++;
                }
            } catch (Exception e) {
                log.warn("批量检测代理节点异常, ID: {}, 错误: {}", node.getId(), e.getMessage());
            }
        }
        
        log.info("批量检测代理节点完成, 代理池ID: {}, 总数: {}, 可用: {}", 
                poolId, nodes.size(), availableCount);
        return availableCount;
    }
    
    @Override
    public ProxyNode allocateProxy(Long poolId) {
        // 查询可用代理，按健康度排序，优先分配健康度高的
        LambdaQueryWrapper<ProxyNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyNode::getPoolId, poolId);
        wrapper.eq(ProxyNode::getStatus, 1); // 可用
        wrapper.ge(ProxyNode::getHealthScore, 60); // 健康度要求
        wrapper.orderByDesc(ProxyNode::getHealthScore); // 优先选择健康度高的
        wrapper.orderByAsc(ProxyNode::getUseCount); // 其次选择使用次数少的
        wrapper.last("LIMIT 1");
        
        ProxyNode proxy = this.getOne(wrapper);
        
        if (proxy != null) {
            // 增加使用次数
            proxy.setUseCount(proxy.getUseCount() + 1);
            this.updateById(proxy);
            log.debug("分配代理成功, ID: {}, IP: {}:{}", proxy.getId(), proxy.getProxyIp(), proxy.getProxyPort());
        } else {
            log.warn("无可用代理, 代理池ID: {}", poolId);
        }
        
        return proxy;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProxyStats(Long proxyId, boolean success) {
        ProxyNode proxy = this.getById(proxyId);
        if (proxy == null) {
            return;
        }
        
        // 更新成功/失败次数
        if (success) {
            proxy.setSuccessCount(proxy.getSuccessCount() + 1);
        } else {
            proxy.setFailCount(proxy.getFailCount() + 1);
        }
        
        // 重新计算健康度
        long totalCount = proxy.getSuccessCount() + proxy.getFailCount();
        if (totalCount > 0) {
            BigDecimal healthScore = BigDecimal.valueOf(proxy.getSuccessCount())
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            proxy.setHealthScore(healthScore.intValue());
            
            // 根据健康度更新状态
            if (healthScore.compareTo(BigDecimal.valueOf(40)) < 0) {
                proxy.setStatus(2); // 不可用
            } else {
                proxy.setStatus(1); // 可用
            }
        }
        
        this.updateById(proxy);
        log.debug("更新代理统计, ID: {}, 成功: {}, 失败: {}, 健康度: {}", 
                proxyId, proxy.getSuccessCount(), proxy.getFailCount(), proxy.getHealthScore());
    }
    
    /**
     * 实体转VO
     */
    private ProxyNodeVO convertToVO(ProxyNode node) {
        ProxyNodeVO vo = new ProxyNodeVO();
        BeanUtils.copyProperties(node, vo);
        
        // 设置状态文本
        if (node.getStatus() != null) {
            switch (node.getStatus()) {
                case 1 -> vo.setStatusText("可用");
                case 2 -> vo.setStatusText("不可用");
                case 3 -> vo.setStatusText("未检测");
                default -> vo.setStatusText("未知");
            }
        }
        
        return vo;
    }
}
