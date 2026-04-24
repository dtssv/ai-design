package com.aicopilot.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import main.java.com.aicopilot.admin.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE (email = #{account} OR phone = #{account}) AND deleted = 0 LIMIT 1")
    User findByAccount(@Param("account") String account);
}