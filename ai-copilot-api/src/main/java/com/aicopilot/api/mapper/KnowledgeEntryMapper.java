package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.KnowledgeEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface KnowledgeEntryMapper extends BaseMapper<KnowledgeEntry> {
}