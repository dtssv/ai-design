package com.aicopilot.admin.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import com.aicopilot.admin.common.exception.BizException;
import com.aicopilot.admin.common.result.ResultCode;
import com.aicopilot.admin.common.security.JwtUtil;
import com.aicopilot.admin.dto.LoginRequest;
import com.aicopilot.admin.entity.User;
import com.aicopilot.admin.mapper.UserMapper;

/**
 * 管理员认证服务
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 管理员登录 */
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.findByAccount(request.getAccount());
        if (user == null) {
            throw new BizException(ResultCode.BAD_CREDENTIALS);
        }
        if (!"admin".equals(user.getRole())) {
            throw new BizException(ResultCode.NOT_ADMIN);
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.BAD_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        result.put("user", userInfo);
        return result;
    }
}