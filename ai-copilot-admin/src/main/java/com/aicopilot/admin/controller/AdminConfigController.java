package com.aicopilot.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import com.aicopilot.admin.common.result.R;
import com.aicopilot.admin.common.security.SecurityUtil;
import com.aicopilot.admin.entity.SystemConfig;
import com.aicopilot.admin.mapper.SystemConfigMapper;

/**
 * 管理端 - 系统配置管理
 */
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SystemConfigMapper systemConfigMapper;

    /** 获取所有配置 */
    @GetMapping
    public R<List<SystemConfig>> list() {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SystemConfig::getConfigKey);
        return R.ok(systemConfigMapper.selectList(wrapper));
    }

    /** 更新配置 */
    @PutMapping("/{key}")
    public R<Void> update(@PathVariable String key, @RequestBody SystemConfig body) {
        SystemConfig existing = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        if (existing != null) {
            existing.setConfigValue(body.getConfigValue());
            existing.setDescription(body.getDescription());
            existing.setUpdatedBy(SecurityUtil.getCurrentUserId());
            systemConfigMapper.updateById(existing);
        }
        return R.ok(null, "配置已更新");
    }

    /** 批量更新配置 */
    @PutMapping("/batch")
    public R<Void> batchUpdate(@RequestBody List<SystemConfig> configs) {
        Long userId = SecurityUtil.getCurrentUserId();
        for (SystemConfig config : configs) {
            SystemConfig existing = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, config.getConfigKey()));
            if (existing != null) {
                existing.setConfigValue(config.getConfigValue());
                existing.setUpdatedBy(userId);
                systemConfigMapper.updateById(existing);
            }
        }
        return R.ok(null, "批量更新成功");
    }

    /** 新增配置项 */
    @PostMapping
    public R<Void> create(@RequestBody SystemConfig body) {
        body.setUpdatedBy(SecurityUtil.getCurrentUserId());
        systemConfigMapper.insert(body);
        return R.ok(null, "新增成功");
    }
}