package com.aicopilot.admin.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import com.aicopilot.admin.common.result.R;
import com.aicopilot.admin.entity.UsageRecord;
import com.aicopilot.admin.entity.User;
import com.aicopilot.admin.mapper.TeamMapper;
import com.aicopilot.admin.mapper.UsageRecordMapper;
import com.aicopilot.admin.mapper.UserMapper;
import com.aicopilot.admin.mapper.WorkspaceMapper;

/**
 * 管理端 - 数据看板接口
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final WorkspaceMapper workspaceMapper;
    private final UsageRecordMapper usageRecordMapper;

    /** 概览数据 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalTeams = teamMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalWorkspaces = workspaceMapper.selectCount(new LambdaQueryWrapper<>());

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, todayStart));
        Long todayGenerations = usageRecordMapper.selectCount(
                new LambdaQueryWrapper<UsageRecord>().ge(UsageRecord::getCreatedAt, todayStart));

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("todayNewUsers", todayNewUsers);
        data.put("totalTeams", totalTeams);
        data.put("totalWorkspaces", totalWorkspaces);
        data.put("todayGenerations", todayGenerations);
        return R.ok(data);
    }

    /** 用户增长趋势 */
    @GetMapping("/users/trend")
    public R<Map<String, Object>> usersTrend(@RequestParam(defaultValue = "30") int days) {
        // 简化实现：返回总量数据，前端可根据需要做图表
        LocalDateTime start = LocalDateTime.of(LocalDate.now().minusDays(days), LocalTime.MIN);
        Long newUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, start));
        Map<String, Object> data = new HashMap<>();
        data.put("days", days);
        data.put("newUsers", newUsers);
        return R.ok(data);
    }

    /** 模型使用分布 */
    @GetMapping("/models/distribution")
    public R<Map<String, Object>> modelsDistribution() {
        // 简化实现：返回总数
        Long totalRecords = usageRecordMapper.selectCount(new LambdaQueryWrapper<>());
        Map<String, Object> data = new HashMap<>();
        data.put("totalRecords", totalRecords);
        return R.ok(data);
    }
}