package com.aicopilot.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import com.aicopilot.admin.common.result.R;
import com.aicopilot.admin.entity.OperationLog;
import com.aicopilot.admin.mapper.OperationLogMapper;

/**
 * 管理端 - 操作日志
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final OperationLogMapper operationLogMapper;

    /** 获取操作日志（分页+筛选） */
    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(OperationLog::getTargetType, targetType);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("items", result.getRecords());
        return R.ok(data);
    }

    /** 导出操作日志（简化实现，返回全量数据） */
    @PostMapping("/export")
    public R<String> export() {
        return R.ok("导出功能请对接文件下载服务");
    }
}