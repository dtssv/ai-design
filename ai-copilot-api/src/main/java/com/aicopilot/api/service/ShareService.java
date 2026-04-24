package com.aicopilot.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.entity.CodeSnapshot;
import com.aicopilot.api.entity.Workspace;
import com.aicopilot.api.entity.WorkspaceShare;
import com.aicopilot.api.mapper.CodeSnapshotMapper;
import com.aicopilot.api.mapper.WorkspaceMapper;
import com.aicopilot.api.mapper.WorkspaceShareMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {

    private final WorkspaceShareMapper shareMapper;
    private final WorkspaceMapper workspaceMapper;
    private final CodeSnapshotMapper codeSnapshotMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建分享链接
     * 如果指定了 snapshotId，则分享该具体版本；否则分享最新版本
     */
    @Transactional
    public Map<String, Object> createShare(Long workspaceId, Long snapshotId, Long userId) {
        Workspace workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(ResultCode.WORKSPACE_NOT_FOUND);
        }

        // 确定要分享的快照
        CodeSnapshot snapshot;
        if (snapshotId != null) {
            snapshot = codeSnapshotMapper.selectById(snapshotId);
            if (snapshot == null || !snapshot.getWorkspaceId().equals(workspaceId)) {
                throw new BizException(ResultCode.DATA_NOT_FOUND);
            }
        } else {
            // 获取工作区下最新的快照
            snapshot = getLatestSnapshotByWorkspace(workspaceId);
            if (snapshot == null) {
                throw new BizException(ResultCode.SHARE_NO_SNAPSHOT);
            }
        }

        // 生成分享令牌
        String shareToken = UUID.randomUUID().toString().replace("-", "");

        WorkspaceShare share = new WorkspaceShare();
        share.setWorkspaceId(workspaceId);
        share.setShareToken(shareToken);
        share.setSnapshotId(snapshot.getId());
        share.setVersion(snapshot.getVersion());
        share.setCreatedBy(userId);
        share.setStatus("active");
        shareMapper.insert(share);

        Map<String, Object> result = new HashMap<>();
        result.put("shareToken", shareToken);
        result.put("snapshotId", snapshot.getId());
        result.put("version", snapshot.getVersion());
        return result;
    }

    /**
     * 通过分享令牌获取预览数据（公开接口，无需登录）
     */
    public Map<String, Object> getSharePreviewData(String shareToken) {
        WorkspaceShare share = getActiveShare(shareToken);

        CodeSnapshot snapshot = codeSnapshotMapper.selectById(share.getSnapshotId());
        if (snapshot == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }

        Workspace workspace = workspaceMapper.selectById(share.getWorkspaceId());

        Map<String, Object> result = new HashMap<>();
        result.put("workspaceName", workspace != null ? workspace.getName() : "未知工作区");
        result.put("version", snapshot.getVersion());
        result.put("createdAt", snapshot.getCreatedAt());
        result.put("files", parseFiles(snapshot.getFiles()));
        return result;
    }

    /**
     * MCP 工具 - 获取最新版本代码
     */
    public Map<String, Object> getMcpLatestCode(String shareToken) {
        WorkspaceShare share = getActiveShare(shareToken);

        CodeSnapshot latest = getLatestSnapshotByWorkspace(share.getWorkspaceId());
        if (latest == null) {
            throw new BizException(ResultCode.SHARE_NO_SNAPSHOT);
        }
        return buildMcpResponse(latest);
    }

    /**
     * MCP 工具 - 获取指定版本代码
     */
    public Map<String, Object> getMcpVersionCode(String shareToken, int version) {
        WorkspaceShare share = getActiveShare(shareToken);

        CodeSnapshot snapshot = codeSnapshotMapper.selectOne(
                new LambdaQueryWrapper<CodeSnapshot>()
                        .eq(CodeSnapshot::getWorkspaceId, share.getWorkspaceId())
                        .eq(CodeSnapshot::getVersion, version)
                        .last("LIMIT 1"));
        if (snapshot == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return buildMcpResponse(snapshot);
    }

    private WorkspaceShare getActiveShare(String shareToken) {
        WorkspaceShare share = shareMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceShare>()
                        .eq(WorkspaceShare::getShareToken, shareToken)
                        .eq(WorkspaceShare::getStatus, "active"));
        if (share == null) {
            throw new BizException(ResultCode.SHARE_NOT_FOUND);
        }
        return share;
    }

    private CodeSnapshot getLatestSnapshotByWorkspace(Long workspaceId) {
        List<CodeSnapshot> list = codeSnapshotMapper.selectList(
                new LambdaQueryWrapper<CodeSnapshot>()
                        .eq(CodeSnapshot::getWorkspaceId, workspaceId)
                        .orderByDesc(CodeSnapshot::getVersion)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<String, Object> buildMcpResponse(CodeSnapshot snapshot) {
        Map<String, Object> result = new HashMap<>();
        result.put("version", snapshot.getVersion());
        result.put("generationMode", snapshot.getGenerationMode());
        result.put("createdAt", snapshot.getCreatedAt());
        result.put("files", parseFiles(snapshot.getFiles()));
        return result;
    }

    private Object parseFiles(String filesJson) {
        if (filesJson == null || filesJson.isBlank())
            return List.of();
        try {
            return objectMapper.readValue(filesJson, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("解析文件JSON失败: {}", e.getMessage());
            return List.of();
        }
    }
}