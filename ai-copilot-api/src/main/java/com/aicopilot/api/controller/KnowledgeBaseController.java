package com.aicopilot.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aicopilot.api.common.result.R;
import com.aicopilot.api.common.security.SecurityUtil;
import com.aicopilot.api.dto.KnowledgeBaseVO;
import com.aicopilot.api.entity.KnowledgeBase;
import com.aicopilot.api.entity.KnowledgeEntry;
import com.aicopilot.api.service.KnowledgeBaseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /** 获取当前用户可见的所有知识库（个人 + 团队 + 公共审核通过），含创建人/描述/使用量 */
    @GetMapping("/visible")
    public R<List<KnowledgeBaseVO>> visibleList() {
        return R.ok(knowledgeBaseService.getVisibleList(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/public")
    public R<List<KnowledgeBase>> publicList(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return R.ok(knowledgeBaseService.getPublicList(keyword, category));
    }

    @GetMapping("/mine")
    public R<List<KnowledgeBase>> myList() {
        return R.ok(knowledgeBaseService.getMyList(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public R<KnowledgeBase> detail(@PathVariable Long id) {
        return R.ok(null); // 简化
    }

    @PostMapping
    public R<KnowledgeBase> create(@RequestBody KnowledgeBase kb) {
        return R.ok(knowledgeBaseService.create(kb, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody KnowledgeBase kb) {
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        knowledgeBaseService.submitPublish(id, SecurityUtil.getCurrentUserId());
        return R.ok();
    }

    @GetMapping("/{id}/entries")
    public R<List<KnowledgeEntry>> entries(@PathVariable Long id) {
        return R.ok(knowledgeBaseService.getEntries(id));
    }

    @PostMapping("/{id}/entries")
    public R<KnowledgeEntry> createEntry(@PathVariable Long id, @RequestBody KnowledgeEntry entry) {
        entry.setKnowledgeBaseId(id);
        return R.ok(knowledgeBaseService.createEntry(entry));
    }
}