package com.detection.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.TaskServer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务服务器关联Mapper
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Mapper
public interface TaskServerMapper extends BaseMapper<TaskServer> {
}
