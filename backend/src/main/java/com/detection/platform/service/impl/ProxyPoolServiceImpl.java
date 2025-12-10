package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.ProxyPoolMapper;
import com.detection.platform.dto.ProxyPoolDTO;
import com.detection.platform.entity.ProxyNode;
import com.detection.platform.entity.ProxyPool;
import com.detection.platform.service.ProxyNodeService;
import com.detection.platform.service.ProxyPoolService;
import com.detection.platform.vo.ProxyNodeVO;
import com.detection.platform.vo.ProxyPoolVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代理池Service实现类
 */
@Slf4j
@Service
public class ProxyPoolServiceImpl extends ServiceImpl<ProxyPoolMapper, ProxyPool> implements ProxyPoolService {
    
    @Lazy
    @Autowired
    private ProxyNodeService proxyNodeService;
    
    @Override
    public Page<ProxyPoolVO> pageProxyPools(Integer current, Integer size, String poolName, Integer proxyType) {
        LambdaQueryWrapper<ProxyPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(poolName), ProxyPool::getPoolName, poolName);
        wrapper.eq(proxyType != null, ProxyPool::getProxyType, proxyType);
        wrapper.orderByDesc(ProxyPool::getCreateTime);
        
        Page<ProxyPool> page = this.page(new Page<>(current, size), wrapper);
        
        Page<ProxyPoolVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ProxyPoolVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    public List<ProxyPoolVO> listAllProxyPools() {
        LambdaQueryWrapper<ProxyPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ProxyPool::getCreateTime);
        List<ProxyPool> list = this.list(wrapper);
        
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @Override
    public ProxyPoolVO getProxyPoolById(Long id) {
        ProxyPool pool = this.getById(id);
        if (pool == null) {
            throw new GlobalExceptionHandler.BusinessException("代理池不存在");
        }
        
        return convertToVO(pool);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addProxyPool(ProxyPoolDTO proxyPoolDTO) {
        // 检查名称是否已存在
        LambdaQueryWrapper<ProxyPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyPool::getPoolName, proxyPoolDTO.getPoolName());
        if (this.count(wrapper) > 0) {
            throw new GlobalExceptionHandler.BusinessException("该代理池名称已存在");
        }
        
        ProxyPool pool = new ProxyPool();
        BeanUtils.copyProperties(proxyPoolDTO, pool);
        
        // 设置认证类型
        pool.setAuthType(proxyPoolDTO.getNeedAuth() != null && proxyPoolDTO.getNeedAuth() == 1 ? 1 : 0);
        
        // 加密密码
        if (StringUtils.hasText(proxyPoolDTO.getPassword())) {
            // TODO: 使用AES加密密码
            pool.setPassword(proxyPoolDTO.getPassword());
        }
        
        // 初始化状态（设为检测中，等待异步检测）
        pool.setStatus(3); // 1=可用, 2=不可用, 3=检测中
        pool.setHealthScore(0); // 初始健康度为0
        pool.setUseCount(0L);
        pool.setSuccessCount(0L);
        pool.setFailCount(0L);
        
        this.save(pool);
        
        log.info("💾 [代理池管理] 添加代理池成功");
        log.info("  ├─ ID: {}", pool.getId());
        log.info("  ├─ 名称: {}", pool.getPoolName());
        log.info("  ├─ 地址: {}:{}", pool.getProxyIp(), pool.getProxyPort());
        log.info("  ├─ 类型: {}", pool.getProxyType() == 1 ? "HTTP" : pool.getProxyType() == 2 ? "HTTPS" : "SOCKS5");
        log.info("  ├─ 认证: {}", pool.getAuthType() == 1 ? "需要" : "无");
        log.info("  ├─ 国家: {}", pool.getCountry() != null ? pool.getCountry() : "未设置");
        log.info("  ├─ 分组: {}", pool.getGroupName() != null ? pool.getGroupName() : "未分组");
        log.info("  └─ 初始状态: 检测中（等待实际检测）");
        
        // 立即触发异步检测
        Long poolId = pool.getId();
        new Thread(() -> {
            try {
                Thread.sleep(500); // 等待事务提交
                log.info("🔍 [代理检测] 开始自动检测新添加的代理池, ID: {}", poolId);
                testProxyConnection(poolId);
            } catch (Exception e) {
                log.error("❌ [代理检测] 自动检测失败, ID: {}, 错误: {}", poolId, e.getMessage());
            }
        }, "proxy-auto-check-" + poolId).start();
        
        return pool.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProxyPool(ProxyPoolDTO proxyPoolDTO) {
        if (proxyPoolDTO.getId() == null) {
            throw new GlobalExceptionHandler.BusinessException("代理池ID不能为空");
        }
        
        ProxyPool existPool = this.getById(proxyPoolDTO.getId());
        if (existPool == null) {
            throw new GlobalExceptionHandler.BusinessException("代理池不存在");
        }
        
        // 检查名称是否被其他代理池占用
        LambdaQueryWrapper<ProxyPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyPool::getPoolName, proxyPoolDTO.getPoolName());
        wrapper.ne(ProxyPool::getId, proxyPoolDTO.getId());
        if (this.count(wrapper) > 0) {
            throw new GlobalExceptionHandler.BusinessException("该代理池名称已被占用");
        }
        
        ProxyPool pool = new ProxyPool();
        BeanUtils.copyProperties(proxyPoolDTO, pool);
        
        // 编辑模式下，如果密码为空则不更新密码字段
        if (proxyPoolDTO.getPassword() == null || proxyPoolDTO.getPassword().trim().isEmpty()) {
            pool.setPassword(existPool.getPassword()); // 保持原密码
            log.info("编辑节点未提供密码，保持原密码不变");
        } else {
            // 如果提供了密码，则更新密码
            pool.setPassword(proxyPoolDTO.getPassword());
            log.info("编辑节点提供了新密码，将更新密码");
        }
        
        boolean success = this.updateById(pool);
        
        if (success) {
            log.info("更新代理池成功, ID: {}, 名称: {}", pool.getId(), pool.getPoolName());
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProxyPool(Long id) {
        ProxyPool pool = this.getById(id);
        if (pool == null) {
            throw new GlobalExceptionHandler.BusinessException("代理池不存在");
        }
        
        boolean success = this.removeById(id);
        
        if (success) {
            log.info("删除代理池成功, ID: {}, 名称: {}", id, pool.getPoolName());
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean refreshPoolStats(Long poolId) {
        // 新版本不再需要节点统计，保留接口以便兼容
        log.info("新版本代理池不需要节点统计, poolId: {}", poolId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkProxyPool(Long poolId) {
        log.info("🔍 [API检测] 开始检测代理池, ID: {}", poolId);
        testProxyConnection(poolId);
        log.info("✅ [API检测] 检测完成, ID: {}", poolId);
    }
    
    /**
     * 实体转VO
     */
    private ProxyPoolVO convertToVO(ProxyPool pool) {
        ProxyPoolVO vo = new ProxyPoolVO();
        BeanUtils.copyProperties(pool, vo);
        
        // 设置代理类型文本
        if (pool.getProxyType() != null) {
            switch (pool.getProxyType()) {
                case 1 -> vo.setProxyTypeText("HTTP");
                case 2 -> vo.setProxyTypeText("HTTPS");
                case 3 -> vo.setProxyTypeText("SOCKS5");
                default -> vo.setProxyTypeText("未知");
            }
        }
        
        // 查询关联的代理节点，获取最新状态
        LambdaQueryWrapper<ProxyNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(ProxyNode::getPoolId, pool.getId());
        nodeWrapper.orderByDesc(ProxyNode::getLastCheckTime);
        nodeWrapper.last("LIMIT 1");
        ProxyNode node = proxyNodeService.getOne(nodeWrapper);
        
        if (node != null) {
            // 从代理节点获取实时状态
            vo.setStatus(node.getStatus());
            vo.setHealthScore(node.getHealthScore());
            vo.setUseCount(node.getUseCount());
            vo.setSuccessCount(node.getSuccessCount());
            vo.setFailCount(node.getFailCount());
            vo.setResponseTime(node.getResponseTime());
            vo.setAvgResponseTime(node.getAvgResponseTime());
            vo.setLastCheckTime(node.getLastCheckTime());
            
            log.debug("代理池 {} 的状态从节点 {} 同步: status={}, responseTime={}ms", 
                    pool.getId(), node.getId(), node.getStatus(), node.getResponseTime());
        } else {
            // 没有关联节点时，使用代理池自身的状态
            log.debug("代理池 {} 没有关联的代理节点，使用代理池状态: status={}", pool.getId(), pool.getStatus());
        }
        
        return vo;
    }
    
    /**
     * 测试代理连接（实际检测）
     */
    private void testProxyConnection(Long poolId) {
        log.info("🔍 [代理检测] 开始检测代理池, ID: {}", poolId);
        
        ProxyPool pool = this.getById(poolId);
        if (pool == null) {
            log.error("❌ [代理检测] 代理池不存在, ID: {}", poolId);
            return;
        }
        
        boolean isAvailable = false;
        long startTime = System.currentTimeMillis();
        int responseTime = 0;
        String errorMessage = null;
        
        try {
            String proxyHost = pool.getProxyIp();
            int proxyPort = pool.getProxyPort();
            
            log.info("  ├─ 代理地址: {}:{}", proxyHost, proxyPort);
            log.info("  ├─ 代理类型: {}", pool.getProxyType() == 1 ? "HTTP" : pool.getProxyType() == 2 ? "HTTPS" : "SOCKS5");
            log.info("  ├─ 是否认证: {}", pool.getAuthType() == 1 ? "是" : "否");
            
            // 根据代理类型创建不同的Proxy对象
            java.net.Proxy.Type proxyType;
            if (pool.getProxyType() == 3) {
                // SOCKS5代理
                proxyType = java.net.Proxy.Type.SOCKS;
                log.info("  ├─ 使用 SOCKS5 代理");
            } else {
                // HTTP/HTTPS代理
                proxyType = java.net.Proxy.Type.HTTP;
                log.info("  ├─ 使用 HTTP 代理");
            }
            
            // 创建代理对象
            java.net.Proxy proxy = new java.net.Proxy(proxyType, new java.net.InetSocketAddress(proxyHost, proxyPort));
            
            // 如果需要认证，设置认证器
            if (pool.getAuthType() == 1 && org.springframework.util.StringUtils.hasText(pool.getUsername())) {
                String username = pool.getUsername();
                String password = pool.getPassword();
                
                log.info("  ├─ 设置认证: 用户名={}", username);
                
                final String finalPassword = password;
                java.net.Authenticator.setDefault(new java.net.Authenticator() {
                    @Override
                    protected java.net.PasswordAuthentication getPasswordAuthentication() {
                        return new java.net.PasswordAuthentication(username, finalPassword.toCharArray());
                    }
                });
            }
            
            // 测试连接 - 使用百度作为测试目标
            java.net.URL url = new java.net.URL("http://www.baidu.com");
            log.info("  ├─ 测试目标: {}", url);
            
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection(proxy);
            conn.setConnectTimeout(15000);  // 15秒连接超时
            conn.setReadTimeout(15000);     // 15秒读取超时
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            responseTime = (int) (System.currentTimeMillis() - startTime);
            
            log.info("  ├─ HTTP响应码: {}", responseCode);
            log.info("  ├─ 响应时间: {}ms", responseTime);
            
            // 200, 301, 302 都认为可用
            isAvailable = (responseCode == 200 || responseCode == 301 || responseCode == 302);
            
            // 读取响应内容验证
            if (isAvailable) {
                try (java.io.InputStream is = conn.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int bytesRead = is.read(buffer);
                    log.info("  ├─ 读取响应: {} 字节", bytesRead);
                }
            }
            
            conn.disconnect();
            
            // 清除认证器
            java.net.Authenticator.setDefault(null);
            
        } catch (java.net.SocketTimeoutException e) {
            errorMessage = "连接超时: " + e.getMessage();
            log.error("  └─ ❌ 连接超时: {}", e.getMessage());
        } catch (java.net.ConnectException e) {
            errorMessage = "连接被拒绝: " + e.getMessage();
            log.error("  └─ ❌ 连接被拒绝: {}", e.getMessage());
        } catch (java.net.UnknownHostException e) {
            errorMessage = "无法解析主机: " + e.getMessage();
            log.error("  └─ ❌ 无法解析主机: {}", e.getMessage());
        } catch (java.io.IOException e) {
            errorMessage = "IO异常: " + e.getMessage();
            log.error("  └─ ❌ IO异常: {}", e.getMessage());
        } catch (Exception e) {
            errorMessage = "未知错误: " + e.getMessage();
            log.error("  └─ ❌ 未知错误: {}", e.getMessage(), e);
        } finally {
            // 确保清除认证器
            java.net.Authenticator.setDefault(null);
        }
        
        // 更新代理池状态
        pool.setStatus(isAvailable ? 1 : 2);
        pool.setHealthScore(isAvailable ? 100 : 0);
        pool.setLastCheckTime(java.time.LocalDateTime.now());
        
        if (isAvailable) {
            pool.setSuccessCount(pool.getSuccessCount() + 1);
            log.info("  └─ ✅ 检测结果: 代理可用，响应时间 {}ms", responseTime);
        } else {
            pool.setFailCount(pool.getFailCount() + 1);
            log.warn("  └─ ❌ 检测结果: 代理不可用 - {}", errorMessage != null ? errorMessage : "未知原因");
        }
        
        this.updateById(pool);
        
        log.info("📊 [代理检测] 检测完成 - 代理池ID: {}, 最终状态: {}", 
                poolId, isAvailable ? "✅ 正常" : "❌ 异常");
    }
    
    @Override
    public List<ProxyPool> listProxyPoolsByGroup(String groupName) {
        LambdaQueryWrapper<ProxyPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProxyPool::getGroupName, groupName);
        wrapper.eq(ProxyPool::getStatus, 1); // 只返回可用状态的代理
        wrapper.orderByDesc(ProxyPool::getHealthScore);
        
        List<ProxyPool> pools = this.list(wrapper);
        log.info("📋 [代理分组] 查询分组 '{}' 下的可用代理，共找到 {} 个", groupName, pools.size());
        
        return pools;
    }
}
