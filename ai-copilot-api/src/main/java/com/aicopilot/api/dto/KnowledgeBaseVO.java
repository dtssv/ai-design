package com.aicopilot.api.dto;

import lombok.Data;

/**
 * 知识库展示对象（含创建人、使用量等聚合信息）
 */
@Data
public class KnowledgeBaseVO {
    private Long id;
    private String name;
    private String description;
    /** 来源分类: personal / team / public */
    private String source;
    /** 类型: platform/user */
    private String type;
    private String visibility;
    private String category;
    private Long ownerId;
    /** 创建人昵称 */
    private String ownerName;
    /** 所属团队名称 */
    private String teamName;
    private Long teamId;
    /** 累计使用次数 */
    private Long usageCount;
    private String createdAt;
}