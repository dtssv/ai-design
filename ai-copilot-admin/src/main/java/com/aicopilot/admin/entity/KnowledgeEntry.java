package com.aicopilot.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import main.java.com.aicopilot.admin.common.base.BaseEntity;

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