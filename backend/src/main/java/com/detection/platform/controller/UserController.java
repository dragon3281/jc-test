package com.detection.platform.controller;

import com.detection.platform.common.utils.JwtUtil;
import com.detection.platform.common.utils.Result;
import com.detection.platform.dto.ChangePasswordDTO;
import com.detection.platform.dto.LoginDTO;
import com.detection.platform.service.UserService;
import com.detection.platform.vo.LoginVO;
import com.detection.platform.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 测试密码编码（临时测试接口）
     */
    @GetMapping("/test-password")
    public Result<String> testPassword(@RequestParam String password) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encoded = encoder.encode(password);
        return Result.success("Encoded: " + encoded);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<UserVO> getCurrentUser(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        UserVO userVO = userService.getCurrentUser(userId);
        return Result.success(userVO);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestHeader("Authorization") String token,
                                       @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        userService.changePassword(userId, changePasswordDTO);
        return Result.successMsg("密码修改成功");
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT是无状态的,退出登录只需前端删除Token即可
        return Result.successMsg("退出成功");
    }
}
