package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.entity.DetectionResult;
import com.detection.platform.vo.DetectionResultVO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检测结果Service接口
 */
public interface DetectionResultService extends IService<DetectionResult> {
    
    /**
     * 分页查询检测结果
     */
    Page<DetectionResultVO> pageResults(Integer current, Integer size, Long taskId, Integer status, 
                                        LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据任务ID查询结果
     */
    List<DetectionResultVO> listResultsByTaskId(Long taskId);
    
    /**
     * 导出检测结果为Excel
     */
    byte[] exportResultsToExcel(Long taskId) throws IOException;
    
    /**
     * 统计任务结果
     */
    DetectionResultVO getTaskStatistics(Long taskId);
}
