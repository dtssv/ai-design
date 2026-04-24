package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.Team;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface TeamMapper extends BaseMapper<Team> {
}