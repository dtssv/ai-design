package com.aicopilot.admin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.aicopilot.admin.common.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_keys")
public class ApiKey extends BaseEntity {

    private String name;
    /** 范围: platform/personal/team */
    private String scope;
    /** 所有者类型: system/user/team */
    private String ownerType;
    private Long ownerId;
    private String provider;
    private String modelName;
    private String apiKeyEncrypted;
    private String apiBaseUrl;
    private String status;
    private Integer rateLimit;
    private Integer weight;
    private Long totalTokensUsed;
    private LocalDateTime lastUsedAt;
}