package com.detection.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.DetectionResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测结果Mapper
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Mapper
public interface DetectionResultMapper extends BaseMapper<DetectionResult> {
}
