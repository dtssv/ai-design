package com.aicopilot.api.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    FAIL(500, "操作失败"),

    // 认证相关 1xxx
    UNAUTHORIZED(1001, "未登录或Token已过期"),
    FORBIDDEN(1002, "无权限访问"),
    BAD_CREDENTIALS(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被禁用"),
    TOKEN_EXPIRED(1005, "Token已过期"),
    TOKEN_INVALID(1006, "Token无效"),

    // 参数校验 2xxx
    PARAM_ERROR(2001, "参数校验失败"),
    DATA_NOT_FOUND(2002, "数据不存在"),
    DATA_ALREADY_EXISTS(2003, "数据已存在"),

    // 业务异常 3xxx
    TEAM_NOT_FOUND(3001, "团队不存在"),
    NOT_TEAM_MEMBER(3002, "非团队成员"),
    TEAM_MEMBER_PENDING(3003, "加入申请待审核"),
    WORKSPACE_NOT_FOUND(3004, "工作区不存在"),
    QUOTA_EXCEEDED(3005, "使用额度已用尽"),
    API_KEY_INVALID(3006, "API Key无效"),
    KNOWLEDGE_BASE_NOT_FOUND(3007, "知识库不存在"),
    NO_TEAM_NO_COLLABORATION(3008, "无团队用户无法添加协作成员"),
    MEMBER_NOT_IN_SAME_TEAM(3009, "只能添加同一团队的成员"),
    SHARE_NOT_FOUND(3010, "分享链接不存在或已失效"),
    SHARE_NO_SNAPSHOT(3011, "工作区暂无可分享的代码版本");

    private final int code;
    private final String message;
}