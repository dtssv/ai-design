package com.aicopilot.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.aicopilot.admin.common.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teams")
public class Team extends BaseEntity {

    private String name;
    private String description;
    private String avatarUrl;
    private String inviteCode;
    private Long ownerId;
    /** 状态: active/disabled/dissolved */
    private String status;
}