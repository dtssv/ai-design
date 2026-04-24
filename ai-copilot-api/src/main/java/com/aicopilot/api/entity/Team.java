package com.aicopilot.api.entity;

import com.aicopilot.api.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

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