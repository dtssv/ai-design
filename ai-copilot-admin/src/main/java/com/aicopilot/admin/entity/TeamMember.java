package com.aicopilot.admin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("team_members")
public class TeamMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long userId;
    /** 角色: owner/admin/member */
    private String role;
    /** 审核状态: pending/approved/rejected */
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
}