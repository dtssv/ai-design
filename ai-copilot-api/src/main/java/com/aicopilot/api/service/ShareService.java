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
import com.aicopilot.api.entity.User;
import com.aicopilot.api.entity.Workspace;
import com.aicopilot.api.entity.WorkspaceShare;
import com.aicopilot.api.mapper.CodeSnapshotMapper;
import com.aicopilot.api.mapper.UserMapper;
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
    private final UserMapper userMapper;
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
        // 返回用户的 MCP 令牌（首次自动生成）
        result.put("mcpToken", ensureUserMcpToken(userId));
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

    // ========== MCP 协议入口（带用户身份校验）==========

    /**
     * MCP 协议 - 获取最新版本代码
     * 使用用户的 mcpToken 鉴权，且仅允许访问该用户名下创建的分享
     */
    public Map<String, Object> getMcpLatestCodeByMcpToken(String mcpToken, String shareToken) {
        WorkspaceShare share = getActiveShareForUser(shareToken, mcpToken);
        CodeSnapshot latest = getLatestSnapshotByWorkspace(share.getWorkspaceId());
        if (latest == null) {
            throw new BizException(ResultCode.SHARE_NO_SNAPSHOT);
        }
        return buildMcpResponse(latest);
    }

    /**
     * MCP 协议 - 获取指定版本代码
     */
    public Map<String, Object> getMcpVersionCodeByMcpToken(String mcpToken, String shareToken, int version) {
        WorkspaceShare share = getActiveShareForUser(shareToken, mcpToken);
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

    /**
     * 校验 mcpToken 对应用户是否有权访问该分享（即该分享必须由本人创建）
     */
    private WorkspaceShare getActiveShareForUser(String shareToken, String mcpToken) {
        if (mcpToken == null || mcpToken.isBlank()) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.findByMcpToken(mcpToken);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        WorkspaceShare share = getActiveShare(shareToken);
        if (!user.getId().equals(share.getCreatedBy())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return share;
    }

    /**
     * 确保用户拥有 MCP Token，没有则生成并保存
     */
    @Transactional
    public String ensureUserMcpToken(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (user.getMcpToken() != null && !user.getMcpToken().isBlank()) {
            return user.getMcpToken();
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setMcpToken(token);
        userMapper.updateById(user);
        return token;
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