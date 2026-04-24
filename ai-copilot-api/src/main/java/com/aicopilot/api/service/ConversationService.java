package com.aicopilot.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.entity.CodeSnapshot;
import com.aicopilot.api.entity.Conversation;
import com.aicopilot.api.entity.Message;
import com.aicopilot.api.mapper.CodeSnapshotMapper;
import com.aicopilot.api.mapper.ConversationMapper;
import com.aicopilot.api.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final CodeSnapshotMapper codeSnapshotMapper;
    private final ObjectMapper objectMapper;

    /** 创建会话 */
    public Conversation createConversation(Long workspaceId, String title, String generationMode, Long userId) {
        Conversation conversation = new Conversation();
        conversation.setWorkspaceId(workspaceId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setGenerationMode(generationMode != null ? generationMode : "prototype");
        conversation.setApiKeySource("platform");
        conversation.setCreatedBy(userId);
        conversation.setDeleted(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    /** 获取工作区下的会话列表 */
    public List<Conversation> listByWorkspace(Long workspaceId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getWorkspaceId, workspaceId)
                        .orderByDesc(Conversation::getUpdatedAt));
    }

    /** 获取会话消息历史 */
    public List<Message> getMessages(Long conversationId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt));
    }

    /** 保存用户消息 */
    @Transactional
    public Message saveUserMessage(Long conversationId, String content, String attachments) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(content);
        message.setAttachments(attachments);
        messageMapper.insert(message);
        return message;
    }

    /** 保存AI响应消息及代码快照 */
    @Transactional
    public Message saveAssistantMessage(Long conversationId, String content,
            String modelUsed, int tokenUsage,
            Long workspaceId, String files, String generationMode) {
        // 1. 保存代码快照
        CodeSnapshot snapshot = new CodeSnapshot();
        snapshot.setWorkspaceId(workspaceId);
        snapshot.setConversationId(conversationId);
        snapshot.setFiles(files);
        snapshot.setGenerationMode(generationMode);
        // 版本自增
        Long maxVersion = codeSnapshotMapper.selectCount(
                new LambdaQueryWrapper<CodeSnapshot>().eq(CodeSnapshot::getConversationId, conversationId));
        snapshot.setVersion(maxVersion.intValue() + 1);
        codeSnapshotMapper.insert(snapshot);

        // 2. 保存消息
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("assistant");
        message.setContent(content);
        message.setModelUsed(modelUsed);
        message.setTokenUsage(tokenUsage);
        message.setCodeSnapshotId(snapshot.getId());
        messageMapper.insert(message);
        return message;
    }

    /** 获取代码版本历史 */
    public List<CodeSnapshot> getSnapshots(Long conversationId) {
        return codeSnapshotMapper.selectList(
                new LambdaQueryWrapper<CodeSnapshot>()
                        .eq(CodeSnapshot::getConversationId, conversationId)
                        .orderByDesc(CodeSnapshot::getVersion));
    }

    /** 根据ID获取会话 */
    public Conversation getById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /** 切换生成模式 */
    public void switchMode(Long conversationId, String generationMode) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setGenerationMode(generationMode);
            conversationMapper.updateById(conversation);
        }
    }

    /** 切换API Key来源和指定Key */
    public void switchApiKeySource(Long conversationId, String apiKeySource, Long apiKeyId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setApiKeySource(apiKeySource);
            conversation.setApiKeyId(apiKeyId);
            conversationMapper.updateById(conversation);
        }
    }

    /** 获取会话的最新代码快照 */
    public CodeSnapshot getLatestSnapshot(Long conversationId) {
        List<CodeSnapshot> list = codeSnapshotMapper.selectList(
                new LambdaQueryWrapper<CodeSnapshot>()
                        .eq(CodeSnapshot::getConversationId, conversationId)
                        .orderByDesc(CodeSnapshot::getVersion)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 根据ID获取单个快照 */
    public CodeSnapshot getSnapshotById(Long snapshotId) {
        return codeSnapshotMapper.selectById(snapshotId);
    }

    /** 更新对话摘要（滚动压缩，避免历史消息超Token） */
    public void updateSummary(Long conversationId, String summary) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setContextSummary(summary);
            conversationMapper.updateById(conversation);
        }
    }

    /** 获取最近N条消息（用于构建Prompt上下文） */
    public List<Message> getRecentMessages(Long conversationId, int limit) {
        List<Message> all = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT " + limit));
        // 倒序取的，反转为正序
        java.util.Collections.reverse(all);
        return all;
    }

    /** 更新快照文件内容（仅修改已有文件内容，不可增删文件） */
    public void updateSnapshotFiles(Long snapshotId, Object incomingFiles) {
        CodeSnapshot snapshot = codeSnapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        try {
            String newFilesJson = objectMapper.writeValueAsString(incomingFiles);
            snapshot.setFiles(newFilesJson);
            codeSnapshotMapper.updateById(snapshot);
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "更新快照文件失败: " + e.getMessage());
        }
    }
}