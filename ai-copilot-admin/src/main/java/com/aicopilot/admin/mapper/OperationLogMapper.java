package com.aicopilot.admin.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.aicopilot.admin.entity.OperationLog;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}