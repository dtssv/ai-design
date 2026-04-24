package com.aicopilot.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.aicopilot.admin.common.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workspaces")
public class Workspace extends BaseEntity {

    private String name;
    private String description;
    private String codeLanguage;
    private String generationMode;
    private Long ownerId;
    private Long teamId;
    /** 状态: active/archived/deleted */
    private String status;
}