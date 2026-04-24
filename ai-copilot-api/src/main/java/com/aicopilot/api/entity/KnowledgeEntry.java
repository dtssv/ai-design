package com.aicopilot.api.entity;

import com.aicopilot.api.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_entries")
public class KnowledgeEntry extends BaseEntity {

    private Long knowledgeBaseId;
    private String title;
    private String description;
    private String componentName;
    private String codeContent;
    /** 代码语言: react/vue/typescript */
    private String codeLanguage;
    private String previewUrl;
    /** 标签JSON */
    private String tags;
    private Integer sortOrder;
}