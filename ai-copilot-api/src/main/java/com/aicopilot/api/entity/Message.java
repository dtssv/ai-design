package com.aicopilot.api.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    /** 角色: user/assistant/system */
    private String role;
    private String content;
    /** 附件JSON */
    private String attachments;
    private Integer tokenUsage;
    private String modelUsed;
    private Long codeSnapshotId;
    private LocalDateTime createdAt;
}