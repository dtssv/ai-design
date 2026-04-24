package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.WorkspaceMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface WorkspaceMemberMapper extends BaseMapper<WorkspaceMember> {
}