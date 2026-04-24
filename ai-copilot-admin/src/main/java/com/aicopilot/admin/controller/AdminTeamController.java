package com.aicopilot.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import main.java.com.aicopilot.admin.common.result.R;
import main.java.com.aicopilot.admin.entity.Team;
import main.java.com.aicopilot.admin.entity.TeamMember;
import main.java.com.aicopilot.admin.entity.UsageRecord;
import main.java.com.aicopilot.admin.mapper.TeamMapper;
import main.java.com.aicopilot.admin.mapper.TeamMemberMapper;
import main.java.com.aicopilot.admin.mapper.UsageRecordMapper;

/**
 * 管理端 - 团队管理
 */
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class AdminTeamController {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UsageRecordMapper usageRecordMapper;

    /** 团队列表 */
    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Team::getName, keyword);
        }
        wrapper.orderByDesc(Team::getCreatedAt);
        Page<Team> result = teamMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("items", result.getRecords());
        return R.ok(data);
    }

    /** 团队详情 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Team team = teamMapper.selectById(id);
        Long memberCount = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, id).eq(TeamMember::getStatus,
                        "approved"));

        Map<String, Object> data = new HashMap<>();
        data.put("team", team);
        data.put("memberCount", memberCount);
        return R.ok(data);
    }

    /** 修改团队状态 */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Team team = new Team();
        team.setId(id);
        team.setStatus(body.get("status"));
        teamMapper.updateById(team);
        return R.ok(null, "操作成功");
    }

    /** 获取团队成员 */
    @GetMapping("/{id}/members")
    public R<List<TeamMember>> members(@PathVariable Long id) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamId, id).orderByAsc(TeamMember::getCreatedAt);
        return R.ok(teamMemberMapper.selectList(wrapper));
    }

    /** 获取团队用量统计 */
    @GetMapping("/{id}/usage")
    public R<Map<String, Object>> usage(@PathVariable Long id) {
        Long totalRecords = usageRecordMapper.selectCount(
                new LambdaQueryWrapper<UsageRecord>().eq(UsageRecord::getTeamId, id));
        Map<String, Object> data = new HashMap<>();
        data.put("totalRecords", totalRecords);
        return R.ok(data);
    }
}