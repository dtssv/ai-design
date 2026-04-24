package com.aicopilot.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.entity.ApiKey;
import com.aicopilot.api.mapper.ApiKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyMapper apiKeyMapper;

    /** 获取个人API-Key列表 */
    @GetMapping("/api-keys/personal")
    public R<List<ApiKey>> personalList() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<ApiKey> keys = apiKeyMapper.selectList(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getScope, "personal")
                        .eq(ApiKey::getOwnerType, "user")
                        .eq(ApiKey::getOwnerId, userId));
        keys.forEach(k -> k.setApiKeyEncrypted("***")); // 隐藏Key
        return R.ok(keys);
    }

    /** 创建个人API-Key */
    @PostMapping("/api-keys/personal")
    public R<ApiKey> createPersonal(@RequestBody ApiKey apiKey) {
        Long userId = SecurityUtil.getCurrentUserId();
        apiKey.setScope("personal");
        apiKey.setOwnerType("user");
        apiKey.setOwnerId(userId);
        apiKey.setStatus("active");
        apiKey.setTotalTokensUsed(0L);
        apiKey.setDeleted(0);
        apiKeyMapper.insert(apiKey);
        apiKey.setApiKeyEncrypted("***");
        return R.ok(apiKey);
    }

    /** 修改个人API-Key */
    @PutMapping("/api-keys/personal/{id}")
    public R<ApiKey> updatePersonal(@PathVariable Long id, @RequestBody ApiKey apiKey) {
        Long userId = SecurityUtil.getCurrentUserId();
        ApiKey existing = apiKeyMapper.selectById(id);
        if (existing == null || !"personal".equals(existing.getScope()) || !userId.equals(existing.getOwnerId())) {
            return R.fail("无权修改此API Key");
        }
        existing.setName(apiKey.getName());
        existing.setProvider(apiKey.getProvider());
        existing.setModelName(apiKey.getModelName());
        existing.setApiBaseUrl(apiKey.getApiBaseUrl());
        // 仅当用户传入了新Key时才更新
        if (apiKey.getApiKeyEncrypted() != null && !apiKey.getApiKeyEncrypted().isEmpty()) {
            existing.setApiKeyEncrypted(apiKey.getApiKeyEncrypted());
        }
        apiKeyMapper.updateById(existing);
        existing.setApiKeyEncrypted("***");
        return R.ok(existing);
    }

    /** 删除个人API-Key */
    @DeleteMapping("/api-keys/personal/{id}")
    public R<Void> deletePersonal(@PathVariable Long id) {
        return R.ok();
    }

    /** 获取团队API-Key列表 */
    @GetMapping("/teams/{tid}/api-keys")
    public R<List<ApiKey>> teamList(@PathVariable Long tid) {
        List<ApiKey> keys = apiKeyMapper.selectList(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getScope, "team")
                        .eq(ApiKey::getOwnerType, "team")
                        .eq(ApiKey::getOwnerId, tid));
        keys.forEach(k -> k.setApiKeyEncrypted("***"));
        return R.ok(keys);
    }

    /** 创建团队API-Key */
    @PostMapping("/teams/{tid}/api-keys")
    public R<ApiKey> createTeamKey(@PathVariable Long tid, @RequestBody ApiKey apiKey) {
        apiKey.setScope("team");
        apiKey.setOwnerType("team");
        apiKey.setOwnerId(tid);
        apiKey.setStatus("active");
        apiKey.setTotalTokensUsed(0L);
        apiKey.setDeleted(0);
        apiKeyMapper.insert(apiKey);
        return R.ok(apiKey);
    }
}