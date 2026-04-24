package com.aicopilot.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.aicopilot.admin.common.result.R;
import com.aicopilot.admin.common.security.SecurityUtil;
import com.aicopilot.admin.dto.LoginRequest;
import com.aicopilot.admin.entity.User;
import com.aicopilot.admin.mapper.UserMapper;
import com.aicopilot.admin.service.AdminAuthService;

/**
 * 管理端 - 管理员认证
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final UserMapper userMapper;

    /** 管理员登录 */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Validated @RequestBody LoginRequest request) {
        return R.ok(adminAuthService.login(request));
    }

    /** 获取当前管理员信息 */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("nickname", user.getNickname());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        data.put("avatarUrl", user.getAvatarUrl());
        return R.ok(data);
    }
}