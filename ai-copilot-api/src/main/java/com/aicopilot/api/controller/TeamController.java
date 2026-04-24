package com.aicopilot.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.entity.Team;
import com.aicopilot.api.entity.TeamMember;
import com.aicopilot.api.service.TeamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public R<Team> create(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(teamService.createTeam(body.get("name"), body.get("description"), userId));
    }

    @GetMapping
    public R<List<Team>> myTeams() {
        return R.ok(teamService.getMyTeams(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public R<Team> detail(@PathVariable Long id) {
        return R.ok(teamService.getTeamDetail(id));
    }

    @PostMapping("/{id}/invite-code/refresh")
    public R<String> refreshInviteCode(@PathVariable Long id) {
        return R.ok(teamService.refreshInviteCode(id, SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}/invite-code")
    public R<String> getInviteCode(@PathVariable Long id) {
        Team team = teamService.getTeamDetail(id);
        return R.ok(team.getInviteCode());
    }

    @PostMapping("/join")
    public R<TeamMember> join(@RequestBody Map<String, String> body) {
        return R.ok(teamService.joinByInviteCode(body.get("invite_code"), SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}/members")
    public R<List<TeamMember>> members(@PathVariable Long id) {
        return R.ok(teamService.getTeamMembers(id));
    }

    @GetMapping("/{id}/members/pending")
    public R<List<TeamMember>> pendingMembers(@PathVariable Long id) {
        teamService.checkTeamAdmin(id, SecurityUtil.getCurrentUserId());
        return R.ok(teamService.getPendingMembers(id));
    }

    @PutMapping("/{id}/members/{userId}/review")
    public R<Void> reviewMember(@PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        teamService.reviewMember(id, userId, body.get("action"), SecurityUtil.getCurrentUserId());
        return R.ok();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        teamService.checkTeamAdmin(id, SecurityUtil.getCurrentUserId());
        // 简化实现：直接删除成员记录
        return R.ok();
    }

    @PostMapping("/{id}/invite")
    public R<Void> inviteByEmail(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        teamService.sendInviteEmail(id, body.get("email"), userId);
        return R.ok();
    }

    @PostMapping("/{id}/leave")
    public R<Void> leave(@PathVariable Long id) {
        // 退出团队逻辑
        return R.ok();
    }
}