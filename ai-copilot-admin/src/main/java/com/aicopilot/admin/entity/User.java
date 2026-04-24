package com.aicopilot.admin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import main.java.com.aicopilot.admin.common.base.BaseEntity;

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
}