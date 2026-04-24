package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.OperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}