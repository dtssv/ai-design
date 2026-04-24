package com.aicopilot.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.entity.Team;
import com.aicopilot.api.entity.TeamMember;
import com.aicopilot.api.mapper.TeamMapper;
import com.aicopilot.api.mapper.TeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final EmailService emailService;

    /** 创建团队 */
    @Transactional
    public Team createTeam(String name, String description, Long userId) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setInviteCode(IdUtil.simpleUUID().substring(0, 8).toUpperCase());
        team.setOwnerId(userId);
        team.setStatus("active");
        team.setDeleted(0);
        teamMapper.insert(team);

        // 创建者自动成为团队owner
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole("owner");
        member.setStatus("approved");
        member.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);

        return team;
    }

    /** 获取用户加入的所有团队 */
    public List<Team> getMyTeams(Long userId) {
        // 先批量查询用户的团队成员记录，再批量查询团队信息
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, "approved"));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> teamIds = members.stream().map(TeamMember::getTeamId).toList();
        return teamMapper.selectList(
                new LambdaQueryWrapper<Team>()
                        .in(Team::getId, teamIds)
                        .eq(Team::getStatus, "active"));
    }

    /** 获取团队详情 */
    public Team getTeamDetail(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        return team;
    }

    /** 刷新邀请码 */
    public String refreshInviteCode(Long teamId, Long userId) {
        Team team = getTeamDetail(teamId);
        checkTeamAdmin(teamId, userId);
        String newCode = IdUtil.simpleUUID().substring(0, 8).toUpperCase();
        team.setInviteCode(newCode);
        teamMapper.updateById(team);
        return newCode;
    }

    /** 通过邀请码申请加入团队 */
    @Transactional
    public TeamMember joinByInviteCode(String inviteCode, Long userId) {
        Team team = teamMapper.selectOne(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getInviteCode, inviteCode)
                        .eq(Team::getStatus, "active"));
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND.getCode(), "邀请码无效");
        }

        // 检查是否已经是成员或已申请
        TeamMember existing = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, team.getId())
                        .eq(TeamMember::getUserId, userId));
        if (existing != null) {
            if ("approved".equals(existing.getStatus())) {
                throw new BizException("已经是该团队成员");
            }
            if ("pending".equals(existing.getStatus())) {
                throw new BizException(ResultCode.TEAM_MEMBER_PENDING);
            }
        }

        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole("member");
        member.setStatus("pending");
        teamMemberMapper.insert(member);
        return member;
    }

    /** 审核加入申请 */
    @Transactional
    public void reviewMember(Long teamId, Long targetUserId, String action, Long operatorId) {
        checkTeamAdmin(teamId, operatorId);
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, targetUserId)
                        .eq(TeamMember::getStatus, "pending"));
        if (member == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if ("approve".equals(action)) {
            member.setStatus("approved");
            member.setJoinedAt(LocalDateTime.now());
        } else {
            member.setStatus("rejected");
        }
        teamMemberMapper.updateById(member);
    }

    /** 获取团队成员列表 */
    public List<TeamMember> getTeamMembers(Long teamId) {
        return teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getStatus, "approved"));
    }

    /** 获取待审核列表 */
    public List<TeamMember> getPendingMembers(Long teamId) {
        return teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getStatus, "pending"));
    }

    /** 检查用户是否为团队管理员（owner或admin） */
    public void checkTeamAdmin(Long teamId, Long userId) {
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, "approved")
                        .in(TeamMember::getRole, List.of("owner", "admin")));
        if (member == null) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    /** 发送团队邀请邮件 */
    public void sendInviteEmail(Long teamId, String email, Long operatorId) {
        if (!StringUtils.hasText(email)) {
            throw new BizException("邮箱不能为空");
        }
        checkTeamAdmin(teamId, operatorId);
        Team team = getTeamDetail(teamId);
        emailService.sendTeamInviteEmail(email, team.getName(), team.getInviteCode());
    }
}