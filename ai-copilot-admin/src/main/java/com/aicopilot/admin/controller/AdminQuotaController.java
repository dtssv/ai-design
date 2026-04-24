package com.aicopilot.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import main.java.com.aicopilot.admin.common.result.R;
import main.java.com.aicopilot.admin.entity.QuotaConfig;
import main.java.com.aicopilot.admin.mapper.QuotaConfigMapper;

/**
 * 管理端 - 额度与套餐配置
 */
@RestController
@RequestMapping("/quota")
@RequiredArgsConstructor
public class AdminQuotaController {

    private final QuotaConfigMapper quotaConfigMapper;

    /** 获取免费额度配置 */
    @GetMapping("/free")
    public R<QuotaConfig> getFreeQuota() {
        QuotaConfig config = quotaConfigMapper.selectOne(
                new LambdaQueryWrapper<QuotaConfig>()
                        .eq(QuotaConfig::getType, "free")
                        .eq(QuotaConfig::getTargetType, "default"));
        return R.ok(config);
    }

    /** 修改免费额度配置 */
    @PutMapping("/free")
    public R<Void> updateFreeQuota(@RequestBody Map<String, Object> body) {
        QuotaConfig existing = quotaConfigMapper.selectOne(
                new LambdaQueryWrapper<QuotaConfig>()
                        .eq(QuotaConfig::getType, "free")
                        .eq(QuotaConfig::getTargetType, "default"));
        if (existing != null) {
            existing.setMonthlyTokenLimit(Long.valueOf(body.get("monthlyTokenLimit").toString()));
            quotaConfigMapper.updateById(existing);
        } else {
            QuotaConfig config = new QuotaConfig();
            config.setName("默认免费额度");
            config.setType("free");
            config.setTargetType("default");
            config.setTargetId(0L);
            config.setMonthlyTokenLimit(Long.valueOf(body.get("monthlyTokenLimit").toString()));
            config.setStatus("active");
            quotaConfigMapper.insert(config);
        }
        return R.ok(null, "更新成功");
    }

    /** 获取付费套餐列表 */
    @GetMapping("/plans")
    public R<List<QuotaConfig>> plans() {
        LambdaQueryWrapper<QuotaConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuotaConfig::getType, "paid").eq(QuotaConfig::getTargetType, "default")
                .orderByAsc(QuotaConfig::getMonthlyTokenLimit);
        return R.ok(quotaConfigMapper.selectList(wrapper));
    }

    /** 创建付费套餐 */
    @PostMapping("/plans")
    public R<Void> createPlan(@RequestBody QuotaConfig body) {
        body.setType("paid");
        body.setTargetType("default");
        body.setTargetId(0L);
        body.setStatus("active");
        quotaConfigMapper.insert(body);
        return R.ok(null, "创建成功");
    }

    /** 更新付费套餐 */
    @PutMapping("/plans/{id}")
    public R<Void> updatePlan(@PathVariable Long id, @RequestBody QuotaConfig body) {
        body.setId(id);
        quotaConfigMapper.updateById(body);
        return R.ok(null, "修改成功");
    }

    /** 禁用/启用套餐 */
    @PutMapping("/plans/{id}/status")
    public R<Void> updatePlanStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        QuotaConfig config = new QuotaConfig();
        config.setId(id);
        config.setStatus(body.get("status"));
        quotaConfigMapper.updateById(config);
        return R.ok(null, "操作成功");
    }

    /** 为用户/团队设置特殊额度 */
    @PostMapping("/special")
    public R<Void> createSpecial(@RequestBody QuotaConfig body) {
        body.setType("free");
        body.setStatus("active");
        quotaConfigMapper.insert(body);
        return R.ok(null, "设置成功");
    }

    /** 获取特殊额度列表 */
    @GetMapping("/special")
    public R<List<QuotaConfig>> specialList() {
        LambdaQueryWrapper<QuotaConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(QuotaConfig::getTargetType, "default")
                .orderByDesc(QuotaConfig::getCreatedAt);
        return R.ok(quotaConfigMapper.selectList(wrapper));
    }
}