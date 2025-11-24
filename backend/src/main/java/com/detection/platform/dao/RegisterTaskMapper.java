package com.detection.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.RegisterTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动化注册任务Mapper
 * 
 * @author Detection Platform
 * @since 2024-11-12
 */
@Mapper
public interface RegisterTaskMapper extends BaseMapper<RegisterTask> {
}
