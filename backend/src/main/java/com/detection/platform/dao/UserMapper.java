package com.detection.platform.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.detection.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
