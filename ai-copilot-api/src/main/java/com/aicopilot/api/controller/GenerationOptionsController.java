package com.aicopilot.api.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.entity.ApiKey;
import com.aicopilot.api.entity.QuotaConfig;
import com.aicopilot.api.entity.Team;
import com.aicopilot.api.mapper.ApiKeyMapper;
import com.aicopilot.api.mapper.QuotaConfigMapper;
import com.aicopilot.api.service.TeamService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

/**
 * 生成时可选项聚合接口
 * 一次返回：
 * - 平台默认套餐可用模型（platform）
 * - 个人 API Key 列表（personal）
 * - 用户所在团队的 API Key 列表（team）
 * - 个人/团队可用付费套餐
 *
 * 前端在生成对话框中以此构建 Key/模型选择器。
 */
@RestController
@RequiredArgsConstructor
public class GenerationOptionsController {

    private final ApiKeyMapper apiKeyMapper;
    private final QuotaConfigMapper quotaConfigMapper;
    private final TeamService teamService;

    @GetMapping("/generation/options")
    public R<Map<String, Object>> options() {
        Long userId = SecurityUtil.getCurrentUserId();

        // 1. 批量查询 - 平台默认可用模型（按 provider+modelName 去重）
        List<ApiKey> platformKeys = apiKeyMapper.selectList(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getScope, "platform")
                        .eq(ApiKey::getStatus, "active"));
        List<Map<String, Object>> platformModels = platformKeys.stream()
                .map(k -> modelItem(k.getProvider(), k.getModelName()))
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询 - 个人 API Key
        List<ApiKey> personalKeys = apiKeyMapper.selectList(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getScope, "personal")
                        .eq(ApiKey::getOwnerType, "user")
                        .eq(ApiKey::getOwnerId, userId)
                        .eq(ApiKey::getStatus, "active"));
        List<Map<String, Object>> personalKeyList = personalKeys.stream()
                .map(this::keyItem).collect(Collectors.toList());

        // 3. 一次拿到我的全部 team，再批量查询所有 team 的 API Key
        List<Team> teams = teamService.getMyTeams(userId);
        List<Map<String, Object>> teamKeyList = new ArrayList<>();
        if (!teams.isEmpty()) {
            List<Long> teamIds = teams.stream().map(Team::getId).collect(Collectors.toList());
            // 批量 IN 查询，避免 N+1
            List<ApiKey> teamKeys = apiKeyMapper.selectList(
                    new LambdaQueryWrapper<ApiKey>()
                            .eq(ApiKey::getScope, "team")
                            .eq(ApiKey::getOwnerType, "team")
                            .in(ApiKey::getOwnerId, teamIds)
                            .eq(ApiKey::getStatus, "active"));
            // 内存中按 teamId 关联 teamName
            Map<Long, String> teamNameMap = teams.stream()
                    .collect(Collectors.toMap(Team::getId, Team::getName));
            teamKeyList = teamKeys.stream().map(k -> {
                Map<String, Object> m = keyItem(k);
                m.put("teamId", k.getOwnerId());
                m.put("teamName", teamNameMap.get(k.getOwnerId()));
                return m;
            }).collect(Collectors.toList());
        }

        // 4. 可用付费套餐（默认 + 个人 + 我所在团队）
        List<QuotaConfig> plans = quotaConfigMapper.selectList(
                new LambdaQueryWrapper<QuotaConfig>()
                        .eq(QuotaConfig::getStatus, "active")
                        .and(w -> w.eq(QuotaConfig::getTargetType, "default")
                                .or(q -> q.eq(QuotaConfig::getTargetType, "user")
                                        .eq(QuotaConfig::getTargetId, userId))
                                .or(teams.isEmpty() ? q -> q.apply("1=0")
                                        : q -> q.eq(QuotaConfig::getTargetType, "team")
                                                .in(QuotaConfig::getTargetId,
                                                        teams.stream().map(Team::getId)
                                                                .collect(Collectors.toList())))));

        Map<String, Object> data = new HashMap<>();
        data.put("platformModels", platformModels);
        data.put("personalKeys", personalKeyList);
        data.put("teamKeys", teamKeyList);
        data.put("plans", plans);
        return R.ok(data);
    }

    private Map<String, Object> modelItem(String provider, String modelName) {
        Map<String, Object> m = new HashMap<>();
        m.put("provider", provider);
        m.put("modelName", modelName);
        return m;
    }

    private Map<String, Object> keyItem(ApiKey k) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", k.getId());
        m.put("name", k.getName());
        m.put("provider", k.getProvider());
        m.put("modelName", k.getModelName());
        return m;
    }
}