-- AI Copilot 数据库建表SQL (MySQL 8.0+)
CREATE DATABASE IF NOT EXISTS ai_copilot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_copilot;

-- ===================== 用户与认证 =====================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(100) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) DEFAULT '' COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '平台角色: admin/user',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/disabled',
    free_quota_used INT NOT NULL DEFAULT 0 COMMENT '当月已用免费额度(token)',
    free_quota_reset_at DATETIME COMMENT '额度重置时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='用户表';

-- ===================== 团队管理 =====================
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '团队名称',
    description TEXT COMMENT '团队描述',
    avatar_url VARCHAR(500) DEFAULT '' COMMENT '团队头像',
    invite_code VARCHAR(32) NOT NULL UNIQUE COMMENT '邀请码',
    owner_id BIGINT NOT NULL COMMENT '创建者ID',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/disabled/dissolved',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner_id (owner_id),
    INDEX idx_invite_code (invite_code),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='团队表';

CREATE TABLE team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL COMMENT '团队ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '团队角色: owner/admin/member',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态: pending/approved/rejected',
    joined_at DATETIME COMMENT '通过审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_team_user (team_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='团队成员表';

-- ===================== 工作区管理 =====================
CREATE TABLE workspaces (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '工作区名称',
    description TEXT COMMENT '描述',
    code_language VARCHAR(20) NOT NULL DEFAULT 'react' COMMENT '代码语言: react/vue/typescript',
    generation_mode VARCHAR(20) NOT NULL DEFAULT 'prototype' COMMENT '生成模式: prototype/development',
    owner_id BIGINT NOT NULL COMMENT '创建者ID',
    team_id BIGINT COMMENT '所属团队ID（可空）',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/archived/deleted',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner_id (owner_id),
    INDEX idx_team_id (team_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='工作区表';

CREATE TABLE workspace_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    permission VARCHAR(20) NOT NULL DEFAULT 'view' COMMENT '权限: view/edit',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_workspace_user (workspace_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='工作区成员表';

CREATE TABLE workspace_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    content LONGTEXT COMMENT '文件内容',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_by BIGINT NOT NULL COMMENT '创建者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workspace_id (workspace_id)
) ENGINE=InnoDB COMMENT='工作区文件表';

-- ===================== 对话与代码生成 =====================
CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    title VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    generation_mode VARCHAR(20) NOT NULL DEFAULT 'prototype' COMMENT '生成模式',
    model_provider VARCHAR(50) COMMENT '模型提供方',
    api_key_source VARCHAR(20) DEFAULT 'platform' COMMENT 'Key来源: platform/personal/team',
    api_key_id BIGINT COMMENT '关联的api_key ID',
    context_summary TEXT COMMENT '对话上下文摘要（滚动压缩）',
    created_by BIGINT NOT NULL COMMENT '创建者',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB COMMENT='对话会话表';

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
    content LONGTEXT COMMENT '消息内容',
    attachments JSON COMMENT '附件信息',
    token_usage INT DEFAULT 0 COMMENT '消耗token数',
    model_used VARCHAR(50) COMMENT '使用的模型',
    code_snapshot_id BIGINT COMMENT '关联的代码快照ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB COMMENT='对话消息表';

CREATE TABLE code_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    message_id BIGINT COMMENT '消息ID',
    files JSON COMMENT '文件快照JSON',
    generation_mode VARCHAR(20) NOT NULL COMMENT '生成模式',
    version INT NOT NULL DEFAULT 1 COMMENT '版本序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB COMMENT='代码快照表';

-- ===================== 知识库 =====================
CREATE TABLE knowledge_bases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '知识库名称',
    description TEXT COMMENT '描述',
    type VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '类型: platform/user',
    visibility VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT '可见性: private/public/pending_review',
    owner_id BIGINT NOT NULL COMMENT '创建者ID',
    team_id BIGINT DEFAULT NULL COMMENT '所属团队ID，NULL表示个人知识库',
    category VARCHAR(100) COMMENT '分类',
    tags JSON COMMENT '标签',
    review_status VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT '审核状态: none/pending/approved/rejected',
    review_comment TEXT COMMENT '审核意见',
    reviewed_by BIGINT COMMENT '审核人',
    reviewed_at DATETIME COMMENT '审核时间',
    usage_count BIGINT DEFAULT 0 COMMENT '累计使用次数',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner_id (owner_id),
    INDEX idx_team_id (team_id),
    INDEX idx_type (type),
    INDEX idx_visibility (visibility),
    INDEX idx_review_status (review_status)
) ENGINE=InnoDB COMMENT='知识库表';

CREATE TABLE knowledge_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL COMMENT '所属知识库ID',
    title VARCHAR(200) NOT NULL COMMENT '条目标题',
    description TEXT COMMENT '描述',
    component_name VARCHAR(100) COMMENT '组件名称',
    code_content LONGTEXT COMMENT '代码内容',
    code_language VARCHAR(20) NOT NULL DEFAULT 'react' COMMENT '代码语言',
    preview_url VARCHAR(500) COMMENT '预览图URL',
    tags JSON COMMENT '标签',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_knowledge_base_id (knowledge_base_id)
) ENGINE=InnoDB COMMENT='知识库条目表';

-- ===================== API-Key管理 =====================
CREATE TABLE api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT 'Key名称',
    scope VARCHAR(20) NOT NULL COMMENT '范围: platform/personal/team',
    owner_type VARCHAR(20) NOT NULL COMMENT '所有者类型: system/user/team',
    owner_id BIGINT NOT NULL DEFAULT 0 COMMENT '所有者ID',
    provider VARCHAR(50) NOT NULL COMMENT '模型提供方',
    model_name VARCHAR(50) NOT NULL COMMENT '模型名称',
    api_key_encrypted TEXT NOT NULL COMMENT '加密后的API Key',
    api_base_url VARCHAR(500) COMMENT '自定义API地址',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/disabled/exhausted',
    rate_limit INT DEFAULT 60 COMMENT '每分钟限流',
    weight INT DEFAULT 1 COMMENT '负载权重',
    total_tokens_used BIGINT NOT NULL DEFAULT 0 COMMENT '累计token',
    last_used_at DATETIME COMMENT '最后使用时间',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scope (scope),
    INDEX idx_owner (owner_type, owner_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='API-Key表';

-- ===================== 用量与计费 =====================
CREATE TABLE usage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '使用者ID',
    team_id BIGINT COMMENT '团队ID',
    workspace_id BIGINT COMMENT '工作区ID',
    conversation_id BIGINT COMMENT '会话ID',
    message_id BIGINT COMMENT '消息ID',
    api_key_source VARCHAR(20) NOT NULL COMMENT 'Key来源',
    api_key_id BIGINT COMMENT 'Key ID',
    model_name VARCHAR(50) COMMENT '模型名称',
    prompt_tokens INT NOT NULL DEFAULT 0 COMMENT '输入token',
    completion_tokens INT NOT NULL DEFAULT 0 COMMENT '输出token',
    total_tokens INT NOT NULL DEFAULT 0 COMMENT '总token',
    cost_amount DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '费用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_team_id (team_id)
) ENGINE=InnoDB COMMENT='使用量记录表';

CREATE TABLE quota_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    type VARCHAR(20) NOT NULL COMMENT '类型: free/paid',
    target_type VARCHAR(20) NOT NULL DEFAULT 'default' COMMENT '对象: default/user/team',
    target_id BIGINT NOT NULL DEFAULT 0 COMMENT '对象ID',
    monthly_token_limit BIGINT NOT NULL COMMENT '月度token限额',
    price DECIMAL(10,2) DEFAULT 0 COMMENT '价格',
    period VARCHAR(20) DEFAULT 'monthly' COMMENT '周期: monthly/yearly',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    description TEXT COMMENT '套餐描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB COMMENT='额度配置表';

CREATE TABLE payment_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '付款用户',
    target_type VARCHAR(20) NOT NULL COMMENT '对象类型: user/team',
    target_id BIGINT NOT NULL COMMENT '对象ID',
    quota_config_id BIGINT NOT NULL COMMENT '套餐ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    payment_method VARCHAR(20) COMMENT '支付方式',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/paid/failed/refunded',
    paid_at DATETIME COMMENT '支付时间',
    expire_at DATETIME COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='支付订单表';

-- ===================== 分享管理 =====================
CREATE TABLE workspace_shares (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    share_token VARCHAR(64) NOT NULL UNIQUE COMMENT '分享令牌',
    snapshot_id BIGINT COMMENT '指定的快照ID（NULL表示最新版本）',
    version INT COMMENT '分享时的版本号',
    created_by BIGINT NOT NULL COMMENT '分享人ID',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_share_token (share_token),
    INDEX idx_workspace_id (workspace_id)
) ENGINE=InnoDB COMMENT='工作区分享表';

-- ===================== 运营管理 =====================
CREATE TABLE operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_type VARCHAR(20) NOT NULL COMMENT '类型: admin/user',
    action VARCHAR(100) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '对象类型',
    target_id BIGINT COMMENT '对象ID',
    detail JSON COMMENT '操作详情',
    ip VARCHAR(45) COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operator (operator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

CREATE TABLE system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(500) COMMENT '说明',
    updated_by BIGINT COMMENT '修改人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='系统配置表';

-- ===================== 初始数据 =====================
INSERT INTO system_configs (config_key, config_value, description) VALUES
('default_free_quota', '100000', '默认每月免费token额度'),
('max_workspace_per_user', '10', '用户最大工作区数量'),
('max_team_members', '50', '团队最大成员数'),
('file_upload_max_size', '10', '文件上传限制(MB)'),
('sandbox_timeout', '30', '沙箱超时时间(秒)');

INSERT INTO quota_configs (name, type, target_type, monthly_token_limit, price, description) VALUES
('免费版', 'free', 'default', 100000, 0, '每月10万token免费额度'),
('基础版', 'paid', 'default', 500000, 29.90, '每月50万token'),
('专业版', 'paid', 'default', 2000000, 99.90, '每月200万token'),
('团队版', 'paid', 'default', 10000000, 399.90, '每月1000万token，适合团队使用');