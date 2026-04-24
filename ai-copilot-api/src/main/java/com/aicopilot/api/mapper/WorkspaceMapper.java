package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.Workspace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface WorkspaceMapper extends BaseMapper<Workspace> {
}