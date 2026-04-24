package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}