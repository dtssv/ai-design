package com.aicopilot.api.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("workspace_shares")
public class WorkspaceShare {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workspaceId;
    private String shareToken;
    /** 指定的快照ID, null表示始终使用最新版本 */
    private Long snapshotId;
    private Integer version;
    private Long createdBy;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}