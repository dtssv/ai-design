package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.TeamMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {
}