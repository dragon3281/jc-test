package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.common.utils.AesUtil;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.ProxyNodeMapper;
import com.detection.platform.dto.ProxyNodeDTO;
import com.detection.platform.entity.ProxyNode;
import com.detection.platform.entity.ProxyPool;
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
import java.net.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.io.IOException;

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
        
        // 获取代理池信息以确定代理类型
        ProxyPool pool = proxyPoolService.getById(node.getPoolId());
        if (pool == null) {
            throw new GlobalExceptionHandler.BusinessException("代理池不存在");
        }
        
        log.info("开始检测代理节点, ID: {}, 地址: {}, 类型: {}", 
                id, node.getProxyAddress(), getProxyTypeText(pool.getProxyType()));
        
        boolean isAvailable = false;
        long startTime = System.currentTimeMillis();
        
        try {
            String[] addressParts = node.getProxyAddress().split(":");
            String proxyHost = addressParts[0];
            int proxyPort = Integer.parseInt(addressParts[1]);
            
            // 根据代理类型创建不同的Proxy对象
            Proxy.Type proxyType;
            if (pool.getProxyType() == 3) {
                // SOCKS5代理
                proxyType = Proxy.Type.SOCKS;
                log.debug("使用SOCKS代理: {}:{}", proxyHost, proxyPort);
            } else {
                // HTTP/HTTPS代理
                proxyType = Proxy.Type.HTTP;
                log.debug("使用HTTP代理: {}:{}", proxyHost, proxyPort);
            }
            
            // 创建代理对象
            Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort));
            
            // 如果需要认证，设置认证器
            if (pool.getAuthType() == 1 && StringUtils.hasText(pool.getUsername())) {
                String username = pool.getUsername();
                String password = pool.getPassword();
                
                // 解密密码
                if (StringUtils.hasText(password)) {
                    try {
                        password = aesUtil.decrypt(password);
                    } catch (Exception e) {
                        log.warn("解密代理密码失败，使用原始值");
                    }
                }
                
                final String finalPassword = password;
                log.debug("设置代理认证: 用户名={}", username);
                
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, finalPassword.toCharArray());
                    }
                });
            }
            
            // 测试连接
            URL url = new URL("http://www.baidu.com");
            log.debug("尝试通过代理访问: {}", url);
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setConnectTimeout(10000);  // 增加超时时间到10秒
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            isAvailable = (responseCode == 200 || responseCode == 301 || responseCode == 302);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            log.info("代理响应: HTTP {}, 耗时: {}ms", responseCode, responseTime);
            
            // 读取响应内容（可选，验证代理确实工作）
            if (isAvailable) {
                try (InputStream is = conn.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int bytesRead = is.read(buffer);
                    log.debug("成功读取响应内容: {} 字节", bytesRead);
                }
            }
            
            // 更新节点信息
            if (isAvailable) {
                node.setStatus(1); // 正常
                node.setResponseTime((int) responseTime);
                node.setSuccessCount(node.getSuccessCount() + 1);
                log.info("✓ 代理节点检测成功, ID: {}, 响应时间: {}ms, 状态: 正常", id, responseTime);
            } else {
                node.setStatus(2); // 异常
                node.setFailCount(node.getFailCount() + 1);
                log.warn("✗ 代理节点不可用, ID: {}, HTTP状态码: {}, 状态: 异常", id, responseCode);
            }
            
            conn.disconnect();
            
        } catch (IOException e) {
            node.setStatus(2); // 不可用
            node.setFailCount(node.getFailCount() + 1);
            log.error("✗ 检测代理节点失败, ID: {}, 地址: {}, 错误类型: {}, 错误信息: {}", 
                    id, node.getProxyAddress(), e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            node.setStatus(2); // 不可用
            node.setFailCount(node.getFailCount() + 1);
            log.error("✗ 检测代理节点异常, ID: {}, 错误: {}", id, e.getMessage(), e);
        } finally {
            // 清除认证器
            Authenticator.setDefault(null);
        }
        
        // 不再计算复杂的健康度，直接根据最后一次检测结果设置状态
        // status: 1=正常(绿色), 2=异常(红色), 3=检测中(黄色)
        
        node.setLastCheckTime(LocalDateTime.now());
        this.updateById(node);
        
        // 刷新代理池统计
        proxyPoolService.refreshPoolStats(node.getPoolId());
        
        log.info("检测代理节点完成, ID: {}, 最终状态: {}", 
                id, isAvailable ? "✓ 正常" : "✗ 异常");
        return isAvailable;
    }
    
    /**
     * 获取代理类型文本
     */
    private String getProxyTypeText(Integer proxyType) {
        if (proxyType == null) {
            return "未知";
        }
        return switch (proxyType) {
            case 1 -> "HTTP";
            case 2 -> "HTTPS";
            case 3 -> "SOCKS5";
            default -> "未知";
        };
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
                case 1 -> vo.setStatusText("正常");
                case 2 -> vo.setStatusText("异常");
                case 3 -> vo.setStatusText("检测中");
                default -> vo.setStatusText("未知");
            }
        }
        
        return vo;
    }
}
