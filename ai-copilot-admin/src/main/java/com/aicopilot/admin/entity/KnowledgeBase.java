package com.aicopilot.admin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import main.java.com.aicopilot.admin.common.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_bases")
public class KnowledgeBase extends BaseEntity {

    private String name;
    private String description;
    /** 类型: platform/user */
    private String type;
    /** 可见性: private/public/pending_review */
    private String visibility;
    private Long ownerId;
    private String category;
    /** 标签JSON */
    private String tags;
    /** 审核状态: none/pending/approved/rejected */
    private String reviewStatus;
    private String reviewComment;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
}