package com.detection.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.detection.platform.dto.ChangePasswordDTO;
import com.detection.platform.dto.LoginDTO;
import com.detection.platform.entity.User;
import com.detection.platform.vo.LoginVO;
import com.detection.platform.vo.UserVO;

/**
 * 用户服务接口
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果(Token和用户信息)
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 获取当前用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param changePasswordDTO 修改密码信息
     */
    void changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    /**
     * 更新最后登录时间
     *
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);
}
