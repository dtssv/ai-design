package com.aicopilot.api.entity;

import java.time.LocalDateTime;

import com.aicopilot.api.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    private String email;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    /** 平台角色: admin/user */
    private String role;
    /** 状态: active/disabled */
    private String status;
    /** 当月已用免费额度 */
    private Integer freeQuotaUsed;
    /** 额度重置时间 */
    private LocalDateTime freeQuotaResetAt;
    /** 用户MCP令牌，用于MCP服务端点身份绑定 */
    private String mcpToken;
}