package com.detection.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.DetectionTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测任务Mapper
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Mapper
public interface DetectionTaskMapper extends BaseMapper<DetectionTask> {
}
