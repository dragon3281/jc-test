package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.dto.ProxyPoolDTO;
import com.detection.platform.entity.ProxyPool;
import com.detection.platform.vo.ProxyPoolVO;

import java.util.List;

/**
 * 代理池Service接口
 */
public interface ProxyPoolService extends IService<ProxyPool> {
    
    /**
     * 分页查询代理池列表
     */
    Page<ProxyPoolVO> pageProxyPools(Integer current, Integer size, String poolName, Integer proxyType);
    
    /**
     * 获取所有代理池列表(包含节点信息)
     */
    List<ProxyPoolVO> listAllProxyPools();
    
    /**
     * 根据ID获取代理池详情(包含节点列表)
     */
    ProxyPoolVO getProxyPoolById(Long id);
    
    /**
     * 添加代理池
     */
    Long addProxyPool(ProxyPoolDTO proxyPoolDTO);
    
    /**
     * 更新代理池
     */
    Boolean updateProxyPool(ProxyPoolDTO proxyPoolDTO);
    
    /**
     * 删除代理池
     */
    Boolean deleteProxyPool(Long id);
    
    /**
     * 刷新代理池统计信息
     */
    Boolean refreshPoolStats(Long poolId);
}
