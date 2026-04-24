package com.aicopilot.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.common.security.JwtUtil;
import com.aicopilot.api.dto.auth.AuthResponse;
import com.aicopilot.api.dto.auth.LoginRequest;
import com.aicopilot.api.dto.auth.RegisterRequest;
import com.aicopilot.api.entity.Team;
import com.aicopilot.api.entity.TeamMember;
import com.aicopilot.api.entity.User;
import com.aicopilot.api.mapper.TeamMapper;
import com.aicopilot.api.mapper.TeamMemberMapper;
import com.aicopilot.api.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     * 注册时如果携带了团队邀请码，则自动申请加入该团队（状态为pending，需审核）
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 校验邮箱或手机号至少填写一个
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhone())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "邮箱和手机号至少填写一个");
        }

        // 检查邮箱/手机号是否已存在
        if (StringUtils.hasText(request.getEmail())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
            if (count > 0) {
                throw new BizException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "该邮箱已注册");
            }
        }
        if (StringUtils.hasText(request.getPhone())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
            if (count > 0) {
                throw new BizException(ResultCode.DATA_ALREADY_EXISTS.getCode(), "该手机号已注册");
            }
        }

        // 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("user");
        user.setStatus("active");
        user.setFreeQuotaUsed(0);
        user.setDeleted(0);
        userMapper.insert(user);

        // 如果有邀请码，自动申请加入团队
        if (StringUtils.hasText(request.getInviteCode())) {
            Team team = teamMapper.selectOne(new LambdaQueryWrapper<Team>()
                    .eq(Team::getInviteCode, request.getInviteCode())
                    .eq(Team::getStatus, "active"));
            if (team != null) {
                TeamMember member = new TeamMember();
                member.setTeamId(team.getId());
                member.setUserId(user.getId());
                member.setRole("member");
                member.setStatus("pending");
                teamMemberMapper.insert(member);
            }
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return new AuthResponse(user.getId(), token, refreshToken);
    }

    /**
     * 用户登录 - 支持邮箱/手机号登录
     */
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByAccount(request.getAccount());
        if (user == null) {
            throw new BizException(ResultCode.BAD_CREDENTIALS);
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.BAD_CREDENTIALS);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return new AuthResponse(user.getId(), token, refreshToken);
    }
}