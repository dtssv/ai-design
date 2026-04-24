package com.aicopilot.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aicopilot.api.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE (email = #{account} OR phone = #{account}) AND deleted = 0 LIMIT 1")
    User findByAccount(@Param("account") String account);

    @Select("SELECT * FROM users WHERE mcp_token = #{mcpToken} AND deleted = 0 LIMIT 1")
    User findByMcpToken(@Param("mcpToken") String mcpToken);
}