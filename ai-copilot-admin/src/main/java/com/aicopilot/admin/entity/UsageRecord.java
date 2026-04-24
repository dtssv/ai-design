package com.aicopilot.admin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("usage_records")
public class UsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long teamId;
    private Long workspaceId;
    private Long conversationId;
    private Long messageId;
    private String apiKeySource;
    private Long apiKeyId;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal costAmount;
    private LocalDateTime createdAt;
}