package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aicopilot.api.entity.CodeSnapshot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface CodeSnapshotMapper extends BaseMapper<CodeSnapshot> {
}