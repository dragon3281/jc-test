package com.detection.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.detection.platform.common.utils.JwtUtil;
import com.detection.platform.common.utils.PasswordUtil;
import com.detection.platform.config.GlobalExceptionHandler;
import com.detection.platform.dao.UserMapper;
import com.detection.platform.dto.ChangePasswordDTO;
import com.detection.platform.dto.LoginDTO;
import com.detection.platform.entity.User;
import com.detection.platform.vo.LoginVO;
import com.detection.platform.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements com.detection.platform.service.UserService {

    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;

    /**
     * 用户登录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO loginDTO) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(wrapper);

        if (user == null) {
            throw new GlobalExceptionHandler.BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new GlobalExceptionHandler.BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new GlobalExceptionHandler.BusinessException("账号已被禁用");
        }

        // 更新最后登录时间
        updateLastLoginTime(user.getId());

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 构造返回数据
        UserVO userVO = convertToVO(user);
        return new LoginVO(token, userVO);
    }

    /**
     * 获取当前用户信息
     */
    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new GlobalExceptionHandler.BusinessException("用户不存在");
        }
        return convertToVO(user);
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO changePasswordDTO) {
        // 获取用户
        User user = this.getById(userId);
        if (user == null) {
            throw new GlobalExceptionHandler.BusinessException("用户不存在");
        }

        // 验证旧密码
        if (!passwordUtil.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new GlobalExceptionHandler.BusinessException("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordUtil.encode(changePasswordDTO.getNewPassword()));
        this.updateById(user);
    }

    /**
     * 更新最后登录时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);
    }

    /**
     * 转换为VO对象
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }
}
