package com.aicopilot.api.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("code_snapshots")
public class CodeSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workspaceId;
    private Long conversationId;
    private Long messageId;
    /** 文件快照JSON */
    private String files;
    private String generationMode;
    private Integer version;
    private LocalDateTime createdAt;
}