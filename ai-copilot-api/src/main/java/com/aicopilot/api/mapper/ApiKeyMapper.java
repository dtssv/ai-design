package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {
}