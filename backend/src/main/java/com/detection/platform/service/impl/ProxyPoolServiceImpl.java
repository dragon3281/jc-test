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
        
        // 初始化状态
        pool.setStatus(3); // 未检测
        pool.setHealthScore(100);
        pool.setUseCount(0L);
        pool.setSuccessCount(0L);
        pool.setFailCount(0L);
        
        this.save(pool);
        
        log.info("添加代理池成功, ID: {}, 名称: {}, 地址: {}:{}", 
                pool.getId(), pool.getPoolName(), pool.getProxyIp(), pool.getProxyPort());
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
        
        return vo;
    }
}
