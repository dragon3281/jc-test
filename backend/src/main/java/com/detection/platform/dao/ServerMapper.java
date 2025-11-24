package com.detection.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.Server;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务器Mapper
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Mapper
public interface ServerMapper extends BaseMapper<Server> {
}
