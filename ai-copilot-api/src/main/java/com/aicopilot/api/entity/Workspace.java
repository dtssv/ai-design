package com.aicopilot.api.entity;

import com.aicopilot.api.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workspaces")
public class Workspace extends BaseEntity {

    private String name;
    private String description;
    /** 代码语言: react/vue/typescript */
    private String codeLanguage;
    /** 生成模式: prototype/development */
    private String generationMode;
    private Long ownerId;
    private Long teamId;
    /** 状态: active/archived/deleted */
    private String status;
}