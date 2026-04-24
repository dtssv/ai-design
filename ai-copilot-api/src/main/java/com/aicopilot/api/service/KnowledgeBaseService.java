package com.aicopilot.api.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aicopilot.api.common.exception.BizException;
import com.aicopilot.api.common.result.ResultCode;
import com.aicopilot.api.dto.KnowledgeBaseVO;
import com.aicopilot.api.entity.KnowledgeBase;
import com.aicopilot.api.entity.KnowledgeEntry;
import com.aicopilot.api.entity.Team;
import com.aicopilot.api.entity.User;
import com.aicopilot.api.mapper.KnowledgeBaseMapper;
import com.aicopilot.api.mapper.KnowledgeEntryMapper;
import com.aicopilot.api.mapper.TeamMapper;
import com.aicopilot.api.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final TeamService teamService;

    /** 获取公开知识库（平台+已审核公开） */
    public List<KnowledgeBase> getPublicList(String keyword, String category) {
        LambdaQueryWrapper<KnowledgeBase> query = new LambdaQueryWrapper<KnowledgeBase>()
                .and(w -> w.eq(KnowledgeBase::getType, "platform")
                        .or().eq(KnowledgeBase::getVisibility, "public"));
        if (keyword != null) {
            query.like(KnowledgeBase::getName, keyword);
        }
        if (category != null) {
            query.eq(KnowledgeBase::getCategory, category);
        }
        return knowledgeBaseMapper.selectList(query);
    }

    /** 获取我的知识库 */
    public List<KnowledgeBase> getMyList(Long userId) {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getOwnerId, userId)
                        .eq(KnowledgeBase::getType, "user"));
    }

    /** 创建知识库 */
    public KnowledgeBase create(KnowledgeBase kb, Long userId) {
        kb.setOwnerId(userId);
        kb.setType("user");
        kb.setVisibility("private");
        kb.setReviewStatus("none");
        kb.setDeleted(0);
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    /** 提交发布审核 */
    public void submitPublish(Long id, Long userId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null)
            throw new BizException(ResultCode.KNOWLEDGE_BASE_NOT_FOUND);
        if (!kb.getOwnerId().equals(userId))
            throw new BizException(ResultCode.FORBIDDEN);
        kb.setVisibility("pending_review");
        kb.setReviewStatus("pending");
        knowledgeBaseMapper.updateById(kb);
    }

    /** 获取知识库条目列表 */
    public List<KnowledgeEntry> getEntries(Long knowledgeBaseId) {
        return knowledgeEntryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeEntry>()
                        .eq(KnowledgeEntry::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByAsc(KnowledgeEntry::getSortOrder));
    }

    /** 创建条目 */
    public KnowledgeEntry createEntry(KnowledgeEntry entry) {
        entry.setDeleted(0);
        knowledgeEntryMapper.insert(entry);
        return entry;
    }

    /**
     * 获取用户可见的知识库列表（三类合并：个人 + 团队 + 公共审核通过）
     * 批量查询，内存组装创建人/团队名称
     */
    public List<KnowledgeBaseVO> getVisibleList(Long userId) {
        // 1. 获取用户所在的团队 ID 列表
        List<Team> myTeams = teamService.getMyTeams(userId);
        List<Long> myTeamIds = myTeams.stream().map(Team::getId).collect(Collectors.toList());
        Map<Long, String> teamNameMap = myTeams.stream()
                .collect(Collectors.toMap(Team::getId, Team::getName));

        // 2. 一次查询拿到三类知识库：个人 + 团队 + 公共审核通过
        LambdaQueryWrapper<KnowledgeBase> query = new LambdaQueryWrapper<KnowledgeBase>()
                .and(w -> {
                    // 个人知识库（自己创建的）
                    w.eq(KnowledgeBase::getOwnerId, userId)
                            .isNull(KnowledgeBase::getTeamId);
                    // 团队知识库
                    if (!myTeamIds.isEmpty()) {
                        w.or(q -> q.in(KnowledgeBase::getTeamId, myTeamIds));
                    }
                    // 公共可见（平台知识库 or 审核通过的公开知识库）
                    w.or(q -> q.eq(KnowledgeBase::getVisibility, "public")
                            .eq(KnowledgeBase::getReviewStatus, "approved"));
                    w.or(q -> q.eq(KnowledgeBase::getType, "platform"));
                })
                .orderByDesc(KnowledgeBase::getUsageCount);
        List<KnowledgeBase> allKbs = knowledgeBaseMapper.selectList(query);

        if (allKbs.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 批量查询所有关联的用户（创建人），避免 N+1
        Set<Long> ownerIds = allKbs.stream().map(KnowledgeBase::getOwnerId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userNameMap = Collections.emptyMap();
        if (!ownerIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(ownerIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        }

        // 4. 内存组装 VO
        Map<Long, String> finalUserNameMap = userNameMap;
        return allKbs.stream().map(kb -> {
            KnowledgeBaseVO vo = new KnowledgeBaseVO();
            vo.setId(kb.getId());
            vo.setName(kb.getName());
            vo.setDescription(kb.getDescription());
            vo.setType(kb.getType());
            vo.setVisibility(kb.getVisibility());
            vo.setCategory(kb.getCategory());
            vo.setOwnerId(kb.getOwnerId());
            vo.setOwnerName(finalUserNameMap.getOrDefault(kb.getOwnerId(), "未知用户"));
            vo.setTeamId(kb.getTeamId());
            vo.setTeamName(kb.getTeamId() != null ? teamNameMap.get(kb.getTeamId()) : null);
            vo.setUsageCount(kb.getUsageCount() != null ? kb.getUsageCount() : 0L);
            vo.setCreatedAt(kb.getCreatedAt() != null ? kb.getCreatedAt().toString() : null);

            // 判定来源分类
            if (kb.getTeamId() != null && myTeamIds.contains(kb.getTeamId())) {
                vo.setSource("team");
            } else if (kb.getOwnerId() != null && kb.getOwnerId().equals(userId)
                    && kb.getTeamId() == null) {
                vo.setSource("personal");
            } else {
                vo.setSource("public");
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量累加知识库使用次数（一次 UPDATE 多条）
     */
    public void incrementUsageCount(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return;
        }
        // 批量更新：UPDATE knowledge_bases SET usage_count = usage_count + 1 WHERE id IN
        // (...)
        knowledgeBaseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<KnowledgeBase>()
                        .in(KnowledgeBase::getId, knowledgeBaseIds)
                        .setSql("usage_count = IFNULL(usage_count, 0) + 1"));
    }
}