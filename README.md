# AI Copilot - AI 前端代码生成平台

AI 驱动的前端代码生成平台，支持通过自然语言描述、文件/图片上传等方式生成高质量的前端代码（React/Vue/TypeScript），并提供沙箱预览、团队协作、知识库管理等能力。

## 功能特性

- **AI 代码生成** - 通过自然语言描述或上传设计稿（图片/PDF/Figma）自动生成前端代码，支持流式输出
- **双模式生成** - 原型模式（快速生成交互原型）和开发模式（生成高质量可维护代码），支持原型转开发
- **沙箱实时预览** - 基于 WebContainer/iframe 沙箱的实时代码预览，支持桌面/平板/手机多设备视图
- **团队协作** - 团队创建与管理、邀请码加入、成员角色与权限控制
- **工作区管理** - 多工作区支持，协作成员权限管理，代码版本历史与回滚
- **知识库系统** - 平台默认知识库 + 用户私有知识库，支持在代码生成时引用知识库组件
- **API-Key 管理** - 平台级 Key 池（负载均衡）、个人 Key、团队 Key 多来源支持
- **用量与计费** - 免费额度管理、付费套餐、用量统计与账单管理
- **运营管理后台** - 用户/团队管理、知识库审核、数据统计看板、系统配置

## 项目结构

```
ai-design/
├── ai-copilot-web/          # 用户端前端 (React + TypeScript + Vite)
├── ai-copilot-admin-web/    # 管理端前端 (React + TypeScript + Vite)
├── ai-copilot-api/          # 用户端后端 (Spring Boot 3 + Java 17)
├── ai-copilot-admin/        # 管理端后端 (Spring Boot 3 + Java 17)
└── docs/                    # 技术设计文档
```

## 技术栈

### 前端

| 技术 | 说明 |
|------|------|
| React 19 | 前端框架 |
| TypeScript | 类型安全 |
| Vite | 构建工具 |
| Zustand | 状态管理 |
| React Router 7 | 路由管理 |
| Axios | HTTP 请求 |

### 后端

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2 | 后端框架 |
| Java 17 | 开发语言 |
| MyBatis-Plus | ORM 框架 |
| Spring Security | 安全框架 |
| JWT | 认证方案 |
| Redis | 缓存/会话管理 |
| RabbitMQ | 消息队列 |
| MinIO/S3 | 对象存储 |
| PostgreSQL | 主数据库 |

### 部署

| 技术 | 说明 |
|------|------|
| Docker | 容器化 |
| Kubernetes | 容器编排 |
| Nginx | 反向代理/API 网关 |

## 系统模块

| 模块 | 说明 |
|------|------|
| 用户与认证模块 | 注册/登录、OAuth2.0、个人信息管理 |
| 团队管理模块 | 团队创建、邀请码、成员审核与角色管理 |
| 工作区管理模块 | 工作区 CRUD、协作成员权限、文件管理 |
| AI 代码生成模块 | 多轮对话、文件上传生成、模式切换、版本历史 |
| 沙箱预览模块 | 实时预览、响应式视图、分享链接、代码导出 |
| 知识库模块 | 公开/私有知识库、发布审核、条目管理 |
| API-Key 管理模块 | 平台/个人/团队 Key 管理、负载均衡、有效性验证 |
| 用量与计费模块 | 免费额度、付费套餐、用量统计、账单管理 |
| 运营管理模块 | 用户/团队管理、知识库审核、数据看板、系统配置 |

## 快速开始

### 环境要求

- Node.js >= 18
- Java >= 17
- PostgreSQL >= 14 or MySQL >= 8.0
- Redis >= 7
- Maven >= 3.8

### 前端启动

```bash
# 用户端
cd ai-copilot-web
npm install
npm run dev

# 管理端
cd ai-copilot-admin-web
npm install
npm run dev
```

### 后端启动

```bash
# 用户端 API
cd ai-copilot-api
mvn spring-boot:run

# 管理端 API
cd ai-copilot-admin
mvn spring-boot:run
```

## 域名方案

| 端 | 域名 |
|------|------|
| 用户端 | app.example.com |
| 管理端 | admin.example.com |

## API 接口

- **用户端接口**: `/api/v1/*` - 共 85 个接口
- **管理端接口**: `/api/admin/v1/*` - 共 58 个接口
- 认证方式: Bearer Token (JWT)
- 响应格式: `{ code: number, message: string, data: any }`

详细接口文档请参阅 [技术设计文档](docs/technical-design.md)。

## 许可证

本项目基于 [GPL-3.0](LICENSE) 许可证开源。