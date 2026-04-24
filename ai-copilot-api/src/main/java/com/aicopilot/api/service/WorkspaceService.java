package com.aicopilot.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.entity.TeamMember;
import com.aicopilot.api.entity.Workspace;
import com.aicopilot.api.entity.WorkspaceFile;
import com.aicopilot.api.entity.WorkspaceMember;
import com.aicopilot.api.mapper.TeamMemberMapper;
import com.aicopilot.api.mapper.WorkspaceFileMapper;
import com.aicopilot.api.mapper.WorkspaceMapper;
import com.aicopilot.api.mapper.WorkspaceMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final WorkspaceFileMapper workspaceFileMapper;
    private final TeamMemberMapper teamMemberMapper;

    /** 创建工作区 */
    public Workspace createWorkspace(Workspace workspace, Long userId) {
        workspace.setOwnerId(userId);
        workspace.setStatus("active");
        workspace.setDeleted(0);
        workspaceMapper.insert(workspace);
        return workspace;
    }

    /** 获取工作区列表（我创建的 + 我参与的） */
    public List<Workspace> getMyWorkspaces(Long userId) {
        // 1. 我创建的
        List<Workspace> owned = workspaceMapper.selectList(
                new LambdaQueryWrapper<Workspace>()
                        .eq(Workspace::getOwnerId, userId)
                        .ne(Workspace::getStatus, "deleted"));

        // 2. 我参与的 - 先查workspace_members，再批量查workspace
        List<WorkspaceMember> memberships = workspaceMemberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getUserId, userId));
        List<Workspace> result = new ArrayList<>(owned);
        if (!memberships.isEmpty()) {
            List<Long> wsIds = memberships.stream().map(WorkspaceMember::getWorkspaceId).toList();
            List<Workspace> participated = workspaceMapper.selectList(
                    new LambdaQueryWrapper<Workspace>()
                            .in(Workspace::getId, wsIds)
                            .ne(Workspace::getStatus, "deleted"));
            result.addAll(participated);
        }
        return result;
    }

    /** 获取工作区详情 */
    public Workspace getWorkspaceDetail(Long workspaceId, Long userId) {
        Workspace workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(ResultCode.WORKSPACE_NOT_FOUND);
        }
        // 检查权限：owner 或 成员
        if (!workspace.getOwnerId().equals(userId)) {
            WorkspaceMember member = workspaceMemberMapper.selectOne(
                    new LambdaQueryWrapper<WorkspaceMember>()
                            .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                            .eq(WorkspaceMember::getUserId, userId));
            if (member == null) {
                throw new BizException(ResultCode.FORBIDDEN);
            }
        }
        return workspace;
    }

    /** 添加协作成员 - 必须是同一团队的成员 */
    @Transactional
    public void addMember(Long workspaceId, Long targetUserId, String permission, Long operatorId) {
        Workspace workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(ResultCode.WORKSPACE_NOT_FOUND);
        }
        if (!workspace.getOwnerId().equals(operatorId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        // 无团队用户无法添加协作成员
        if (workspace.getTeamId() == null) {
            throw new BizException(ResultCode.NO_TEAM_NO_COLLABORATION);
        }
        // 检查目标用户是否在同一团队
        TeamMember targetTeamMember = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, workspace.getTeamId())
                        .eq(TeamMember::getUserId, targetUserId)
                        .eq(TeamMember::getStatus, "approved"));
        if (targetTeamMember == null) {
            throw new BizException(ResultCode.MEMBER_NOT_IN_SAME_TEAM);
        }

        // 添加工作区成员
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(targetUserId);
        member.setPermission(permission);
        workspaceMemberMapper.insert(member);
    }

    /** 获取工作区成员列表 */
    public List<WorkspaceMember> getMembers(Long workspaceId) {
        return workspaceMemberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId));
    }

    /** 获取工作区文件列表 */
    public List<WorkspaceFile> getFiles(Long workspaceId) {
        return workspaceFileMapper.selectList(
                new LambdaQueryWrapper<WorkspaceFile>()
                        .eq(WorkspaceFile::getWorkspaceId, workspaceId)
                        .select(WorkspaceFile::getId, WorkspaceFile::getFilePath,
                                WorkspaceFile::getFileName, WorkspaceFile::getVersion));
    }

    /** 获取可添加的同团队成员 */
    public List<TeamMember> getAvailableMembers(Long workspaceId) {
        Workspace workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null || workspace.getTeamId() == null) {
            return List.of();
        }
        return teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, workspace.getTeamId())
                        .eq(TeamMember::getStatus, "approved"));
    }
}