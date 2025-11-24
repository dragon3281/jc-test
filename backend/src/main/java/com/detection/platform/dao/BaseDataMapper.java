package com.detection.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.BaseData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基础数据Mapper
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Mapper
public interface BaseDataMapper extends BaseMapper<BaseData> {
}
