# 管理端 (bickdemo-admin)

基于 Vue 3 + Element Plus 的自行车租赁系统管理后台。

## 功能模块

- **仪表盘** - 系统数据概览
- **用户管理** - 用户列表、状态管理
- **自行车管理** - 自行车 CRUD
- **论坛管理** - 帖子/评论审核
- **市场管理** - 二手交易审核
- **活动管理** - 骑行活动管理
- **公告管理** - 系统公告发布
- **工单管理** - 用户反馈处理
- **系统设置** - 黑名单、日志查看

## 实时通知功能

管理端支持实时 WebSocket 通知，管理员可以在第一时间收到以下事件通知：

### 通知类型

| 事件类型 | 说明 | 触发时机 |
|---------|------|---------|
| `USER_REGISTERED` | 新用户注册 | 用户完成注册时 |
| `BLACKLIST_IP_ADDED` | IP加入黑名单 | IP被封禁时 |
| `BLACKLIST_IP_REMOVED` | IP移出黑名单 | IP解封时 |
| `FORUM_POST_PENDING` | 帖子待审核 | 用户发布帖子时 |
| `FORUM_POST_APPROVED` | 帖子已通过 | 帖子审核通过时 |
| `FORUM_POST_REJECTED` | 帖子已驳回 | 帖子审核驳回时 |
| `FORUM_COMMENT_PENDING` | 评论待审核 | 用户发布评论时 |
| `FORUM_COMMENT_APPROVED` | 评论已通过 | 评论审核通过时 |
| `FORUM_COMMENT_REJECTED` | 评论已驳回 | 评论审核驳回时 |
| `MARKETPLACE_LISTING_PENDING` | 挂牌待审核 | 车主发布物品时 |
| `MARKETPLACE_LISTING_APPROVED` | 挂牌已通过 | 挂牌审核通过时 |
| `MARKETPLACE_LISTING_REJECTED` | 挂牌已驳回 | 挂牌审核驳回时 |

### 通知面板

- 右上角铃铛图标显示未读通知数量
- 鼠标悬停展开下拉面板
- 点击通知可查看详情（类型、标题、内容、操作人、关联对象、时间）
- 支持"全部已读"和"清空"操作
- 清空操作仅隐藏通知，不删除数据库记录

### 技术实现

- **WebSocket** - 通过 STOMP 协议实现实时推送
- **RabbitMQ** - 消息队列确保通知可靠传递
- **localStorage** - 持久化已隐藏的通知 ID，刷新页面后状态保持

## 快速开始

```bash
# 安装依赖
npm install

# 开发模式
npm run dev     # http://localhost:5174

# 生产构建
npm run build
```

## 目录结构

```
src/
├── api/          # API 请求封装
├── assets/       # 静态资源
├── components/   # 公共组件
├── layouts/      # 布局组件
├── router/       # 路由配置
├── services/     # WebSocket 服务
├── stores/       # Pinia 状态管理
├── utils/        # 工具函数
└── views/        # 页面组件
```

## 技术栈

- Vue 3.4.0
- Vite 5.0.8
- Element Plus 2.5.0
- Pinia 2.1.7
- Vue Router 4.2.5
- Axios 1.6.2
- ECharts 6.0.0
- stompjs + sockjs-client (WebSocket)
