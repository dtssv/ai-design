package com.aicopilot.api.service;

import org.springframework.stereotype.Service;

import com.aicopilot.api.entity.ApiKey;
import com.aicopilot.api.entity.Conversation;
import com.aicopilot.api.entity.Message;
import com.aicopilot.api.entity.UsageRecord;
import com.aicopilot.api.entity.Workspace;
import com.aicopilot.api.mapper.UsageRecordMapper;
import com.aicopilot.api.mapper.WorkspaceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用量记录服务
 *
 * 负责记录每次AI生成的Token用量，用于计费和统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageRecordService {

    private final UsageRecordMapper usageRecordMapper;
    private final WorkspaceMapper workspaceMapper;

    /**
     * 记录一次AI生成的用量
     *
     * @param userId       用户ID
     * @param conversation 会话信息
     * @param message      AI响应消息
     * @param apiKey       使用的API Key
     * @param totalTokens  总Token数
     */
    public void recordUsage(Long userId, Conversation conversation, Message message,
            ApiKey apiKey, int totalTokens) {
        try {
            UsageRecord record = new UsageRecord();
            record.setUserId(userId);
            record.setConversationId(conversation.getId());
            record.setMessageId(message.getId());
            record.setWorkspaceId(conversation.getWorkspaceId());
            record.setApiKeySource(conversation.getApiKeySource());
            record.setApiKeyId(apiKey.getId());
            record.setModelName(apiKey.getModelName());
            record.setTotalTokens(totalTokens);

            // 尝试获取团队ID
            Workspace workspace = workspaceMapper.selectById(conversation.getWorkspaceId());
            if (workspace != null && workspace.getTeamId() != null) {
                record.setTeamId(workspace.getTeamId());
            }

            usageRecordMapper.insert(record);
        } catch (Exception e) {
            // 用量记录失败不应影响主流程
            log.error("记录用量失败, userId={}, conversationId={}", userId, conversation.getId(), e);
        }
    }
}