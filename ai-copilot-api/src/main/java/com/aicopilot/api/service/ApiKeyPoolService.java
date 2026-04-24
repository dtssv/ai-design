package com.aicopilot.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.entity.ApiKey;
import com.aicopilot.api.mapper.ApiKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API-Key 池服务 - 加权轮询负载均衡
 *
 * 选择策略优先级：
 * 1. 会话指定的个人/团队Key → 直接使用
 * 2. 平台Key池 → 按weight加权随机选择
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyPoolService {

    private final ApiKeyMapper apiKeyMapper;

    /**
     * 根据会话配置选择一个可用的 API Key
     *
     * @param apiKeySource Key来源: platform / personal / team
     * @param apiKeyId     指定的Key ID（personal/team模式）
     * @param modelName    期望的模型名称（可选）
     * @return 选中的 ApiKey
     */
    public ApiKey selectKey(String apiKeySource, Long apiKeyId, String modelName) {
        // 1. 如果指定了具体Key（个人/团队Key）
        if (apiKeyId != null && !"platform".equals(apiKeySource)) {
            ApiKey key = apiKeyMapper.selectById(apiKeyId);
            if (key == null || !"active".equals(key.getStatus())) {
                throw new BizException(ResultCode.API_KEY_INVALID);
            }
            return key;
        }

        // 2. 从平台Key池中按加权随机选择
        LambdaQueryWrapper<ApiKey> query = new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getScope, "platform")
                .eq(ApiKey::getStatus, "active");

        // 如果指定了模型名称，则筛选对应模型的Key
        if (modelName != null && !modelName.isBlank()) {
            query.eq(ApiKey::getModelName, modelName);
        }

        List<ApiKey> activeKeys = apiKeyMapper.selectList(query);
        if (activeKeys.isEmpty()) {
            throw new BizException(ResultCode.API_KEY_INVALID.getCode(), "没有可用的API Key，请联系管理员");
        }

        return weightedRandomSelect(activeKeys);
    }

    /**
     * 加权随机选择算法
     * 按照 weight 字段进行加权，weight越大被选中概率越高
     */
    private ApiKey weightedRandomSelect(List<ApiKey> keys) {
        // 过滤掉 weight <= 0 的Key
        List<ApiKey> validKeys = keys.stream()
                .filter(k -> k.getWeight() != null && k.getWeight() > 0)
                .collect(Collectors.toList());

        if (validKeys.isEmpty()) {
            // fallback: 所有Key等权随机
            return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        }

        int totalWeight = validKeys.stream().mapToInt(ApiKey::getWeight).sum();
        int randomVal = ThreadLocalRandom.current().nextInt(totalWeight);

        int accumulate = 0;
        for (ApiKey key : validKeys) {
            accumulate += key.getWeight();
            if (randomVal < accumulate) {
                return key;
            }
        }
        // 理论上不会到这里
        return validKeys.get(validKeys.size() - 1);
    }

    /**
     * 记录Key使用情况并更新统计
     */
    public void recordKeyUsage(Long apiKeyId, int tokensUsed) {
        ApiKey key = apiKeyMapper.selectById(apiKeyId);
        if (key != null) {
            long newTotal = (key.getTotalTokensUsed() != null ? key.getTotalTokensUsed() : 0L) + tokensUsed;
            key.setTotalTokensUsed(newTotal);
            key.setLastUsedAt(LocalDateTime.now());
            apiKeyMapper.updateById(key);
        }
    }

    /**
     * 查询所有可用的模型列表（去重）
     */
    public List<ApiKey> listAvailableModels() {
        return apiKeyMapper.selectList(
                new LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getScope, "platform")
                        .eq(ApiKey::getStatus, "active")
                        .select(ApiKey::getProvider, ApiKey::getModelName)
                        .groupBy(ApiKey::getProvider, ApiKey::getModelName));
    }
}