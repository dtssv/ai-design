package com.aicopilot.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.dto.auth.AuthResponse;
import com.aicopilot.api.dto.auth.LoginRequest;
import com.aicopilot.api.dto.auth.RegisterRequest;
import com.aicopilot.api.entity.User;
import com.aicopilot.api.mapper.UserMapper;
import com.aicopilot.api.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    /** 用户注册 */
    @PostMapping("/register")
    public R<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }

    /** 用户登录 */
    @PostMapping("/login")
    public R<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /** 获取当前用户信息 */
    @GetMapping("/me")
    public R<User> me() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPasswordHash(null); // 隐藏密码
        }
        return R.ok(user);
    }

    /** 刷新Token */
    @PostMapping("/refresh-token")
    public R<AuthResponse> refreshToken(@RequestHeader("Authorization") String authorization) {
        // 简化实现：验证refresh token后重新签发
        // 实际项目中需要更完善的refresh token机制
        return R.ok(null, "刷新Token功能待完善");
    }
}