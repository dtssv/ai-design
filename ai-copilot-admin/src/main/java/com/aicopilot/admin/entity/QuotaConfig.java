package com.aicopilot.admin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("quota_configs")
public class QuotaConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 类型: free/paid */
    private String type;
    /** 对象: default/user/team */
    private String targetType;
    private Long targetId;
    private Long monthlyTokenLimit;
    private BigDecimal price;
    /** 周期: monthly/yearly */
    private String period;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}