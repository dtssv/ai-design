package com.aicopilot.api.dto.ai;

import java.util.List;

import lombok.Data;

/**
 * AI 代码生成请求
 */
@Data
public class GenerateRequest {

    /** 用户输入的 Prompt */
    private String content;

    /** 附件列表 - 图片URL，支持多模态（如截图、设计稿图片） */
    private List<String> imageUrls;

    /** 附件列表 - 用户上传的文件URL（文档/参考文件等） */
    private List<String> fileUrls;

    /** 引用的知识库ID列表 */
    private List<Long> knowledgeBaseIds;

    /** 模型名称（可选，使用会话默认） */
    private String modelName;

    /**
     * 指定要修改的文件路径列表（可选）
     * 为空时表示AI自行判断需要修改哪些文件
     * 非空时AI只对指定文件进行修改
     */
    private List<String> targetFiles;
}