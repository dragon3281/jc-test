package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.dto.ProxyNodeDTO;
import com.detection.platform.entity.ProxyNode;
import com.detection.platform.vo.ProxyNodeVO;

import java.util.List;

/**
 * 代理节点Service接口
 */
public interface ProxyNodeService extends IService<ProxyNode> {
    
    /**
     * 根据代理池ID获取节点列表
     */
    List<ProxyNodeVO> listNodesByPoolId(Long poolId);
    
    /**
     * 分页查询代理节点
     */
    Page<ProxyNodeVO> pageProxyNodes(Integer current, Integer size, Long poolId, Integer status);
    
    /**
     * 根据ID获取代理节点详情
     */
    ProxyNodeVO getProxyNodeById(Long id);
    
    /**
     * 添加代理节点
     */
    Long addProxyNode(ProxyNodeDTO proxyNodeDTO);
    
    /**
     * 批量添加代理节点
     */
    Integer batchAddProxyNodes(List<ProxyNodeDTO> proxyNodeDTOList);
    
    /**
     * 更新代理节点
     */
    Boolean updateProxyNode(ProxyNodeDTO proxyNodeDTO);
    
    /**
     * 删除代理节点
     */
    Boolean deleteProxyNode(Long id);
    
    /**
     * 批量删除代理节点
     */
    Boolean batchDeleteProxyNodes(List<Long> ids);
    
    /**
     * 检测代理节点可用性
     */
    Boolean checkProxyNode(Long id);
    
    /**
     * 批量检测代理池中的所有节点
     */
    Integer batchCheckProxyNodes(Long poolId);
    
    /**
     * 分配可用代理
     */
    ProxyNode allocateProxy(Long poolId);
    
    /**
     * 更新代理统计数据
     */
    void updateProxyStats(Long proxyId, boolean success);
}
