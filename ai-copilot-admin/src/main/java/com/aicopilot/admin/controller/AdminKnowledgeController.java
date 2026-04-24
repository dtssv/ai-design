package com.aicopilot.admin.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import main.java.com.aicopilot.admin.common.result.R;
import main.java.com.aicopilot.admin.common.security.SecurityUtil;
import main.java.com.aicopilot.admin.entity.KnowledgeBase;
import main.java.com.aicopilot.admin.entity.KnowledgeEntry;
import main.java.com.aicopilot.admin.mapper.KnowledgeBaseMapper;
import main.java.com.aicopilot.admin.mapper.KnowledgeEntryMapper;

/**
 * 管理端 - 知识库审核管理 + 平台知识库维护
 */
@RestController
@RequestMapping("/knowledge-bases")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeEntryMapper knowledgeEntryMapper;

    /** 获取待审核知识库列表 */
    @GetMapping("/pending")
    public R<List<KnowledgeBase>> pending() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getReviewStatus, "pending").orderByDesc(KnowledgeBase::getCreatedAt);
        return R.ok(knowledgeBaseMapper.selectList(wrapper));
    }

    /** 获取所有知识库列表（分页） */
    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String reviewStatus) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(KnowledgeBase::getType, type);
        }
        if (StringUtils.hasText(reviewStatus)) {
            wrapper.eq(KnowledgeBase::getReviewStatus, reviewStatus);
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        Page<KnowledgeBase> result = knowledgeBaseMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("items", result.getRecords());
        return R.ok(data);
    }

    /** 知识库详情 */
    @GetMapping("/{id}")
    public R<KnowledgeBase> detail(@PathVariable Long id) {
        return R.ok(knowledgeBaseMapper.selectById(id));
    }

    /** 获取知识库条目列表 */
    @GetMapping("/{id}/entries")
    public R<List<KnowledgeEntry>> entries(@PathVariable Long id) {
        LambdaQueryWrapper<KnowledgeEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeEntry::getKnowledgeBaseId, id).orderByAsc(KnowledgeEntry::getSortOrder);
        return R.ok(knowledgeEntryMapper.selectList(wrapper));
    }

    /** 审核知识库 */
    @PutMapping("/{id}/review")
    public R<Void> review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        String action = body.get("action");
        kb.setReviewStatus("approve".equals(action) ? "approved" : "rejected");
        kb.setReviewComment(body.get("comment"));
        kb.setReviewedBy(SecurityUtil.getCurrentUserId());
        kb.setReviewedAt(LocalDateTime.now());
        if ("approve".equals(action)) {
            kb.setVisibility("public");
        }
        knowledgeBaseMapper.updateById(kb);
        return R.ok(null, "审核完成");
    }

    /** 下架公开知识库 */
    @PutMapping("/{id}/unpublish")
    public R<Void> unpublish(@PathVariable Long id) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setVisibility("private");
        kb.setReviewStatus("none");
        knowledgeBaseMapper.updateById(kb);
        return R.ok(null, "已下架");
    }

    // ======== 平台默认知识库维护 ========

    /** 创建平台知识库 */
    @PostMapping("/platform")
    public R<Void> createPlatform(@RequestBody KnowledgeBase body) {
        body.setType("platform");
        body.setVisibility("public");
        body.setReviewStatus("approved");
        body.setOwnerId(SecurityUtil.getCurrentUserId());
        body.setDeleted(0);
        knowledgeBaseMapper.insert(body);
        return R.ok(null, "创建成功");
    }

    /** 添加平台知识库条目 */
    @PostMapping("/{id}/entries")
    public R<Void> createEntry(@PathVariable Long id, @RequestBody KnowledgeEntry body) {
        body.setKnowledgeBaseId(id);
        body.setDeleted(0);
        knowledgeEntryMapper.insert(body);
        return R.ok(null, "添加成功");
    }

    /** 删除知识库条目 */
    @DeleteMapping("/entries/{entryId}")
    public R<Void> deleteEntry(@PathVariable Long entryId) {
        knowledgeEntryMapper.deleteById(entryId);
        return R.ok(null, "删除成功");
    }
}