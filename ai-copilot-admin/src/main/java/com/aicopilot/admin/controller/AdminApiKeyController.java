package com.aicopilot.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import main.java.com.aicopilot.admin.common.result.R;
import main.java.com.aicopilot.admin.entity.ApiKey;
import main.java.com.aicopilot.admin.mapper.ApiKeyMapper;

/**
 * 管理端 - API-Key 池管理
 */
@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class AdminApiKeyController {

    private final ApiKeyMapper apiKeyMapper;

    /** Key列表 */
    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getScope, "platform");
        if (StringUtils.hasText(provider)) {
            wrapper.eq(ApiKey::getProvider, provider);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ApiKey::getStatus, status);
        }
        wrapper.orderByDesc(ApiKey::getCreatedAt);
        Page<ApiKey> result = apiKeyMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("items", result.getRecords());
        return R.ok(data);
    }

    /** 新增Key */
    @PostMapping
    public R<Void> create(@RequestBody Map<String, Object> body) {
        ApiKey key = new ApiKey();
        key.setName((String) body.get("name"));
        key.setProvider((String) body.get("provider"));
        key.setModelName((String) body.get("modelName"));
        key.setApiKeyEncrypted((String) body.get("apiKey"));
        key.setApiBaseUrl((String) body.get("apiBaseUrl"));
        key.setScope("platform");
        key.setOwnerType("system");
        key.setOwnerId(0L);
        key.setWeight((Integer) body.getOrDefault("weight", 1));
        key.setRateLimit((Integer) body.getOrDefault("rateLimit", 60));
        key.setStatus("active");
        key.setTotalTokensUsed(0L);
        key.setDeleted(0);
        apiKeyMapper.insert(key);
        return R.ok(null, "新增成功");
    }

    /** 修改Key */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ApiKey key = new ApiKey();
        key.setId(id);
        key.setName((String) body.get("name"));
        key.setWeight((Integer) body.get("weight"));
        key.setRateLimit((Integer) body.get("rateLimit"));
        apiKeyMapper.updateById(key);
        return R.ok(null, "修改成功");
    }

    /** 修改Key状态 */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ApiKey key = new ApiKey();
        key.setId(id);
        key.setStatus(body.get("status"));
        apiKeyMapper.updateById(key);
        return R.ok(null, "操作成功");
    }

    /** 删除Key */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        apiKeyMapper.deleteById(id);
        return R.ok(null, "删除成功");
    }

    /** 获取Key使用统计 */
    @GetMapping("/{id}/usage")
    public R<ApiKey> usage(@PathVariable Long id) {
        return R.ok(apiKeyMapper.selectById(id));
    }

    /** Key池状态概览 */
    @GetMapping("/pool/status")
    public R<Map<String, Object>> poolStatus() {
        Long total = apiKeyMapper.selectCount(new LambdaQueryWrapper<ApiKey>().eq(ApiKey::getScope, "platform"));
        Long active = apiKeyMapper.selectCount(
                new LambdaQueryWrapper<ApiKey>().eq(ApiKey::getScope, "platform").eq(ApiKey::getStatus, "active"));
        Map<String, Object> data = new HashMap<>();
        data.put("totalKeys", total);
        data.put("activeKeys", active);
        data.put("disabledKeys", total - active);
        return R.ok(data);
    }
}