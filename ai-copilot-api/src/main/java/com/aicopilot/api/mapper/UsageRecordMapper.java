package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.UsageRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {
}