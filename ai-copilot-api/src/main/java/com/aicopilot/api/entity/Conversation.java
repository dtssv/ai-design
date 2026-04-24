package com.aicopilot.api.entity;

import com.aicopilot.api.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversations")
public class Conversation extends BaseEntity {

    private Long workspaceId;
    private String title;
    /** 生成模式: prototype/development */
    private String generationMode;
    private String modelProvider;
    /** Key来源: platform/personal/team */
    private String apiKeySource;
    private Long apiKeyId;
    /** 对话上下文摘要（滚动压缩，避免超Token） */
    private String contextSummary;
    private Long createdBy;
}