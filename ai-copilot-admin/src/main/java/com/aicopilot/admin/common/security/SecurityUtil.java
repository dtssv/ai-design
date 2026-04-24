package com.aicopilot.admin.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aicopilot.admin.common.exception.BizException;
import com.aicopilot.admin.common.result.ResultCode;

/**
 * 安全上下文工具类
 */
public class SecurityUtil {

    private SecurityUtil() {
    }

    /** 获取当前登录管理员ID */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return (Long) authentication.getPrincipal();
    }
}