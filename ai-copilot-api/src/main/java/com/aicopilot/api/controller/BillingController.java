package com.aicopilot.api.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.entity.QuotaConfig;
import com.aicopilot.api.entity.UsageRecord;
import com.aicopilot.api.mapper.QuotaConfigMapper;
import com.aicopilot.api.mapper.UsageRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BillingController {

    private final UsageRecordMapper usageRecordMapper;
    private final QuotaConfigMapper quotaConfigMapper;

    /** 获取当月用量概览 */
    @GetMapping("/usage/overview")
    public R<Map<String, Object>> usageOverview() {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // 批量查询当月使用量
        List<UsageRecord> records = usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getUserId, userId)
                        .eq(UsageRecord::getApiKeySource, "platform")
                        .ge(UsageRecord::getCreatedAt, startOfMonth));

        int totalUsed = records.stream().mapToInt(UsageRecord::getTotalTokens).sum();

        // 获取默认免费额度
        QuotaConfig freeConfig = quotaConfigMapper.selectOne(
                new LambdaQueryWrapper<QuotaConfig>()
                        .eq(QuotaConfig::getType, "free")
                        .eq(QuotaConfig::getTargetType, "default"));

        Map<String, Object> data = new HashMap<>();
        data.put("month", LocalDateTime.now().getMonthValue());
        Map<String, Object> freeQuota = new HashMap<>();
        freeQuota.put("total", freeConfig != null ? freeConfig.getMonthlyTokenLimit() : 100000);
        freeQuota.put("used", totalUsed);
        freeQuota.put("remaining",
                Math.max(0, (freeConfig != null ? freeConfig.getMonthlyTokenLimit() : 100000) - totalUsed));
        data.put("free_quota", freeQuota);
        return R.ok(data);
    }

    /** 获取可用套餐列表 */
    @GetMapping("/billing/plans")
    public R<List<QuotaConfig>> plans() {
        return R.ok(quotaConfigMapper.selectList(
                new LambdaQueryWrapper<QuotaConfig>()
                        .eq(QuotaConfig::getType, "paid")
                        .eq(QuotaConfig::getStatus, "active")));
    }

    /** 获取用量明细 */
    @GetMapping("/usage/records")
    public R<List<UsageRecord>> usageRecords(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.ok(usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                        .eq(UsageRecord::getUserId, userId)
                        .orderByDesc(UsageRecord::getCreatedAt)
                        .last("LIMIT " + size + " OFFSET " + (page - 1) * size)));
    }
}