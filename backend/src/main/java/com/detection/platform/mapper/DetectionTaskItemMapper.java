package com.detection.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.DetectionTaskItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测任务明细 Mapper
 */
@Mapper
public interface DetectionTaskItemMapper extends BaseMapper<DetectionTaskItem> {
}
