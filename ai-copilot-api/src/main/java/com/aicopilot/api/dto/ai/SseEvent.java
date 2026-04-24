package com.aicopilot.api.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 推送事件封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEvent {

    /** 事件类型: text_delta / code_delta / snapshot / done / error */
    private String type;
    /** 文本增量内容 */
    private String content;
    /** 快照ID（type=done时） */
    private Long snapshotId;
    /** 使用的模型 */
    private String model;
    /** Token用量 */
    private Integer tokenUsage;
    /** 消息ID（type=done时） */
    private Long messageId;
    /** 错误信息（type=error时） */
    private String error;
    /** 本轮变更的文件路径列表（type=done时） */
    private List<String> changedFiles;
    /** 对话摘要（type=done时，前端可展示） */
    private String summary;
    /** 快照版本号（type=done时） */
    private Integer snapshotVersion;
}