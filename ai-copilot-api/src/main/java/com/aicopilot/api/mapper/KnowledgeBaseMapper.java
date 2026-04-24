package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.KnowledgeBase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
}