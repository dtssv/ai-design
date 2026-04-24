package com.aicopilot.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import com.aicopilot.admin.common.result.R;
import com.aicopilot.admin.entity.UsageRecord;
import com.aicopilot.admin.entity.User;
import com.aicopilot.admin.entity.Workspace;
import com.aicopilot.admin.mapper.UsageRecordMapper;
import com.aicopilot.admin.mapper.UserMapper;
import com.aicopilot.admin.mapper.WorkspaceMapper;

/**
 * 管理端 - 用户管理
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final WorkspaceMapper workspaceMapper;
    private final UsageRecordMapper usageRecordMapper;

    /** 获取用户列表（分页+筛选） */
    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getNickname, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("items", result.getRecords());
        return R.ok(data);
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public R<User> detail(@PathVariable Long id) {
        return R.ok(userMapper.selectById(id));
    }

    /** 修改用户状态（禁用/启用） */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = new User();
        user.setId(id);
        user.setStatus(body.get("status"));
        userMapper.updateById(user);
        return R.ok(null, "操作成功");
    }

    /** 获取用户用量统计 */
    @GetMapping("/{id}/usage")
    public R<Map<String, Object>> usage(@PathVariable Long id) {
        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageRecord::getUserId, id);
        Long totalRecords = usageRecordMapper.selectCount(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("totalRecords", totalRecords);
        return R.ok(data);
    }

    /** 获取用户工作区列表 */
    @GetMapping("/{id}/workspaces")
    public R<List<Workspace>> workspaces(@PathVariable Long id) {
        LambdaQueryWrapper<Workspace> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Workspace::getOwnerId, id).orderByDesc(Workspace::getCreatedAt);
        return R.ok(workspaceMapper.selectList(wrapper));
    }
}