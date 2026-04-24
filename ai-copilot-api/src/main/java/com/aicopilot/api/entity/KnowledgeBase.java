package com.aicopilot.api.entity;

import java.time.LocalDateTime;

import com.aicopilot.api.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

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

    /** 所属团队ID（null 表示个人知识库） */
    private Long teamId;
    /** 累计使用次数（每次生成引用时 +1） */
    private Long usageCount;
}