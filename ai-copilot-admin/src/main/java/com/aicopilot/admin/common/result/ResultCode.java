package com.aicopilot.admin.common.result;

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

    UNAUTHORIZED(1001, "未登录或Token已过期"),
    FORBIDDEN(1002, "无权限访问"),
    BAD_CREDENTIALS(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被禁用"),
    NOT_ADMIN(1005, "非管理员账号"),

    PARAM_ERROR(2001, "参数校验失败"),
    DATA_NOT_FOUND(2002, "数据不存在"),
    DATA_ALREADY_EXISTS(2003, "数据已存在"),

    REFUND_FAILED(3001, "退款失败");

    private final int code;
    private final String message;
}