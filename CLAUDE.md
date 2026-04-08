# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目结构

| 模块 | 路径 | 端口   | 启动命令 |
|------|------|------|----------|
| 后端 | `bickdemo-backend/` | 8080 | `mvn spring-boot:run` |
| 用户端 | `bickdemo-frontend/` | 5173 | `npm run dev` |
| 管理端 | `bickdemo-admin/` | 3000 | `npm run dev` |
| 数据库 | MySQL 3306 | -    | - |
| 缓存 | Redis 6379 | -    | - |

## 技术栈

### 后端
Spring Boot 3.2, Spring Security 6, JWT (jjwt 0.12), MyBatis-Plus 3.5, MySQL 8, Redis, RabbitMQ, MinIO, WebSocket

### 前端
Vue 3.4, Vite 5, Element Plus 2.5, Pinia 2.1, Vue Router 4.2

## 核心规范

### 后端铁律

**规则 1：依赖注入必须使用 `@Resource`**
- 禁止使用 `@Autowired`

**规则 2：查询必须使用 `LambdaQueryWrapper`**
- 禁止使用 XML 配置查询

**规则 3：异常处理必须使用 `GlobalExceptionHandler`**
- 禁止硬编码错误信息，使用统一异常类

**规则 4：管理端 Controller 必须添加 `@LogOperation` 注解**

### 实体规范
```java
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class XxxEntity {}
```
- 所有实体支持逻辑删除（`deleted` 字段）
- MyBatis-Plus 自动过滤 `deleted = 0`

### 命名规范
```
XxxController  → 控制器
XxxService     → 服务接口
XxxServiceImpl → 服务实现
XxxMapper      → 数据访问
XxxDto         → 数据传输对象
XxxVo          → 视图对象
XxxEntity      → 实体类
```

### Service 模式
```java
@Service
public class XxxServiceImpl extends ServiceImpl<XxxMapper, XxxEntity> implements XxxService {}
```

### 日志规范
- 使用 `@Slf4j`
- 只记录错误：`log.error("错误信息", e)`
- 禁止使用 `log.info`

## 前端规范

### 三大铁律

**规则 1：所有页面必须适配黑夜模式 + 移动端响应式**
- 必须使用 CSS 变量 + `html.dark` 覆盖
- 禁止硬编码颜色值
- 移动端需用 `min-height` 或固定定位

**规则 2：SCSS 嵌套层次必须匹配 DOM 结构**
- 嵌套层次 100% 匹配 template DOM 结构
- 不允许跳过中间层级
- 每个样式模块必须有中文注释

**规则 3：禁止假数据，所有功能必须真实实现**
- 必须 API 调用获取真实数据
- 按钮点击必须实现真实业务功能
- 表单提交必须调用真实 API 持久化

### 组件规范
- 语法：`<script setup>` + Composition API
- 命名：PascalCase（`Home.vue`, `ArticleDetail.vue`）
- SVG 图标：`<svg-icon name="xxx" />`

### 设计规范（去 AI 味）

核心原则：少即是多，克制比表达更重要

| 问题 | 反面 | 正确 |
|------|------|------|
| 渐变滥用 | `linear-gradient(135deg, ...)` | 纯色 |
| 多层阴影 | 3-4 层 `box-shadow` | 单层 `0 1px 3px` |
| 毛玻璃滥用 | 到处 `backdrop-filter: blur()` | 仅模态框 |
| 夸张悬停 | 位移+缩放+变色+阴影 | 只变阴影或颜色 |

### 标准色板
```scss
--bg-page: #f8fafc;       // 浅色 / #0f172a 深色
--bg-card: #ffffff;       // 浅色 / #1e293b 深色
--text-primary: #1e293b;  // 浅色 / #f1f5f9 深色
--text-regular: #475569; // 浅色 / #cbd5e1 深色
--text-muted: #64748b;    // 浅色 / #94a3b8 深色
--border: #e2e8f0;        // 浅色 / #334155 深色
```

### 颜色使用规范
- 所有颜色必须定义在 CSS 变量文件中
- 禁止组件内硬编码 `#xxx` 或 `rgb()`
- 必须用 `var(--xxx)` 引用全局变量

## API 设计

- RESTful 风格
- 用户端：`/api/*`
- 管理端：`/api/admin/*`
- 统一响应：`{code, message, data}`

### 错误码
| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

## 认证流程

```
登录 → JWT Token → 前端存储 → 请求拦截器自动添加 Header
→ JwtAuthenticationFilter 验证 → Spring Security 角色授权
```

## 缓存策略

| 类型 | 用途 | 清除方式 |
|------|------|----------|
| Redis | 全局缓存 | `@CacheEvict` |
| Caffeine | 热点数据 | TTL 过期 |

## IP 限流

- 基于 AOP 实现
- 未登录/已登录：60 次/分钟
- 封禁时长：15 分钟

## 模块详解

### 活动 (Activity)
- 状态流转：`DRAFT → PUBLISHED → COMPLETED/CANCELLED`
- 难度等级：`EASY, MODERATE, HARD, EXTREME`
- 定时任务：每 5 分钟检查过期活动

### 论坛 (Forum)
- 帖子状态：`PUBLISHED, DELETED`
- 反应类型：`LIKE, DISLIKE`
- 支持多图上传 (MinIO)

### 市场 (Marketplace)
- 物品状态：`AVAILABLE, SOLD, DELETED`
- 审核状态：`PENDING, APPROVED, REJECTED`

### 社交 (Social)
- WebSocket + RabbitMQ 实现实时消息
- 消息类型：`TEXT, IMAGE, SYSTEM`

### 租赁 (Rental)
- 状态：`ACTIVE, COMPLETED, CANCELLED`
- 位置校验：检查是否超出最大距离

## 常用命令

### 后端
```bash
cd bickdemo-backend
mvn spring-boot:run          # 开发启动
mvn clean package -DskipTests # 打包
mvn spotless:apply           # 代码格式化
```

### 前端
```bash
cd bickdemo-frontend  # 或 bickdemo-admin
npm install           # 安装依赖
npm run dev            # 开发模式
npm run build          # 生产构建
```

### Docker
```bash
cd script/prod
docker compose up -d --build  # 部署
docker compose logs -f app    # 查看日志
docker compose down -v        # 停止
```

## 数据库

- **迁移管理**：统一在 `sql/init.sql`
- **新增字段**：需同步更新 `sql/init.sql`
- **敏感配置**：使用环境变量 `${VAR_NAME:default}`

### 默认账号
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 用户 | user | user123 |

## 修改后检查

### 后端
```bash
cd bickdemo-backend && mvn clean compile -DskipTests
```
- [ ] 编译通过
- [ ] 导入完整无冗余
- [ ] 无拼写/注解错误
- [ ] 已补充中文注释
- [ ] 管理端 Controller 已添加 `@LogOperation`

### 前端
```bash
cd bickdemo-frontend && npm run build
cd bickdemo-admin && npm run build
```
- [ ] 编译通过，无 JS/语法错误
- [ ] 页面正常加载，无白屏
- [ ] 黑夜模式正常
- [ ] 响应式布局正常

## 新功能开发

**核心原则：优先沿用项目现有设计和规范**

添加新功能时，必须先参考现有类似页面的代码结构、样式风格、API 调用模式。

**禁止**：
- 不看现有代码直接生成全新结构
- 引入项目外 UI 库
- 忽略黑夜模式适配

## Git 工作流

### Commit 规范
```
<type>(<scope>): <subject>

types: feat, fix, docs, style, refactor, test, chore
examples:
  feat(auth): add email login support
  fix(rental): correct location check logic
  docs(api): update endpoint documentation
```

### PR 流程
1. 从 `main` 创建功能分支
2. 完成后发起 PR
3. CI 通过后合并
