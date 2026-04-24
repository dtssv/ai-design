package com.aicopilot.api.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("workspace_members")
public class WorkspaceMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workspaceId;
    private Long userId;
    /** 权限: view/edit */
    private String permission;
    private LocalDateTime createdAt;
}