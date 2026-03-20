# BikeShare 后端 API 接口文档

> 基础路径: `/api`
> 
> 统一响应格式: `{ "code": 200, "message": "...", "data": {} }`

---

## 目录

- [认证接口 (Auth)](#认证接口-auth)
- [自行车接口 (Bicycles)](#自行车接口-bicycles)
- [租赁订单接口 (Rentals)](#租赁订单接口-rentals)
- [社交接口 (Social)](#社交接口-social)
- [统计接口 (Statistics)](#统计接口-statistics)
- [文件上传接口 (Files)](#文件上传接口-files)
- [背景图接口 (Backgrounds)](#背景图接口-backgrounds)
- [论坛接口 (Forum)](#论坛接口-forum)
- [个人出租市场接口 (Marketplace)](#个人出租市场接口-marketplace)
- [后台管理接口 (Admin)](#后台管理接口-admin)
- [公共接口 (Public)](#公共接口-public)

---

## 认证接口 (Auth)

**基础路径**: `/api/auth`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/register` | 用户注册 | 公开 |
| POST | `/login` | 用户名密码登录 | 公开 |
| POST | `/email/login` | 邮箱验证码登录 | 公开 |
| POST | `/email/send-code` | 发送邮箱验证码 | 公开 |
| POST | `/email/reset-password` | 邮箱重置密码 | 公开 |
| POST | `/logout` | 用户注销 | 登录用户 |
| GET | `/me` | 获取当前登录用户信息 | 登录用户 |
| PUT | `/update` | 更新用户资料 | 登录用户 |
| PUT | `/password` | 修改密码 | 登录用户 |
| POST | `/avatar` | 上传/更新头像 | USER/ADMIN |
| DELETE | `/avatar` | 删除头像 | USER/ADMIN |

### 详细说明

#### POST `/api/auth/register`
用户注册
- **请求体**: `{ username, password, email, verifyCode }`
- **响应**: `AuthResponse { token, userId, username, role, avatar }`

#### POST `/api/auth/login`
用户名密码登录
- **请求体**: `{ username, password }`
- **响应**: `AuthResponse`

#### POST `/api/auth/email/login`
邮箱验证码登录
- **请求体**: `{ email, verifyCode }`
- **响应**: `AuthResponse`

#### POST `/api/auth/email/send-code`
发送邮箱验证码
- **请求体**: `{ email, type }` (type: REGISTER/RESET_PASSWORD/UPDATE_EMAIL)
- **响应**: 无

#### POST `/api/auth/email/reset-password`
通过邮箱验证码重置密码
- **请求体**: `{ email, verifyCode, newPassword }`
- **响应**: 无

---

## 自行车接口 (Bicycles)

**基础路径**: `/api/bicycles`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/` | 获取车辆列表（支持筛选） | 公开 |
| GET | `/page` | 分页查询车辆（后台用） | 登录用户 |
| GET | `/available` | 获取可租车辆列表 | 公开 |
| GET | `/{id}` | 获取车辆详情 | 公开 |
| GET | `/type/{type}` | 按车型筛选 | 公开 |
| GET | `/status/{status}` | 按状态筛选 | 公开 |
| POST | `/` | 新增车辆 | ADMIN |
| PUT | `/{id}` | 更新车辆信息 | ADMIN |
| DELETE | `/{id}` | 删除车辆 | ADMIN |
| PUT | `/{id}/status` | 更新车辆状态 | ADMIN |

### 查询参数

#### GET `/api/bicycles`
- `type`: 车型 (MOUNTAIN, ROAD, CITY, ELECTRIC, TANDEM)
- `status`: 状态 (AVAILABLE, RENTED, MAINTENANCE, DISABLED)

---

## 租赁订单接口 (Rentals)

**基础路径**: `/api/rentals`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/` | 创建租赁订单 | 登录用户 |
| POST | `/{id}/end` | 结束租赁 | 登录用户 |
| POST | `/{id}/cancel` | 取消租赁 | 登录用户 |
| GET | `/my` | 分页获取我的租赁记录 | 登录用户 |
| GET | `/my/active` | 获取进行中的租赁 | 登录用户 |
| GET | `/{id}` | 获取租赁详情 | 登录用户 |
| GET | `/` | 获取所有租赁（后台） | ADMIN |

### 详细说明

#### POST `/api/rentals`
创建租赁订单
- **请求体**: `{ bicycleId, quantity, startTime, expectedEndTime }`
- **响应**: `RentalResponse`

---

## 社交接口 (Social)

**基础路径**: `/api/social`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/users/search` | 搜索用户 | USER/ADMIN |
| GET | `/users/{userId}` | 获取用户详情 | USER/ADMIN |
| GET | `/contacts` | 获取联系人列表 | USER/ADMIN |
| POST | `/friend-requests` | 发起好友申请 | USER/ADMIN |
| GET | `/friend-requests/received` | 获取收到的好友申请 | USER/ADMIN |
| GET | `/friend-requests/sent` | 获取发出的好友申请 | USER/ADMIN |
| POST | `/friend-requests/{requestId}/accept` | 接受好友申请 | USER/ADMIN |
| POST | `/friend-requests/{requestId}/reject` | 拒绝好友申请 | USER/ADMIN |
| GET | `/messages/{targetUserId}` | 分页获取会话消息 | USER/ADMIN |
| POST | `/messages/{targetUserId}/read` | 标记会话已读 | USER/ADMIN |
| POST | `/messages` | 发送私聊消息 | USER/ADMIN |

### 详细说明

#### GET `/api/social/users/search`
搜索用户
- **参数**: `keyword`
- **响应**: `List<UserSearchResponse>`

#### GET `/api/social/messages/{targetUserId}`
获取会话消息
- **参数**: `page` (默认1), `size` (默认24)
- **响应**: `ConversationMessagesResponse { records, total, page, size, hasMore }`

#### POST `/api/social/messages`
发送消息
- **请求体**: `{ receiverId, type, content, mediaUrl }`
- **响应**: `ChatMessageResponse`

---

## 统计接口 (Statistics)

**基础路径**: `/api/statistics`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/` | 获取系统统计数据 | 公开 |

### 响应数据
```json
{
  "totalRentals": 100,
  "activeRentals": 20,
  "availableBicycles": 50,
  "typeDistribution": [...],
  "popularBicycles": [...]
}
```

---

## 文件上传接口 (Files)

**基础路径**: `/api/files`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/upload-image` | 上传图片 | USER/ADMIN |
| DELETE | `/delete-image` | 删除图片 | USER/ADMIN |

### 详细说明

#### POST `/api/files/upload-image`
上传图片到 MinIO
- **请求**: `multipart/form-data` (file字段)
- **响应**: `{ url: "http://..." }`

---

## 背景图接口 (Backgrounds)

**基础路径**: `/api/backgrounds`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/` | 获取启用的背景图列表 | 公开 |
| GET | `/selectable` | 获取可选背景图库 | 公开 |
| GET | `/all` | 获取全部背景图（后台） | ADMIN |
| GET | `/{id}` | 获取背景图详情 | 公开 |
| POST | `/upload` | 上传背景图 | ADMIN |
| PUT | `/{id}` | 更新背景图信息 | ADMIN |
| DELETE | `/{id}` | 删除背景图 | ADMIN |
| POST | `/{id}/enabled` | 设置启用状态 | ADMIN |

---

## 论坛接口 (Forum)

**基础路径**: `/api/forum`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/posts` | 获取帖子列表 | 公开 |
| GET | `/posts/pending` | 获取待审核帖子 | ADMIN |
| GET | `/posts/{postId}` | 获取帖子详情 | 公开 |
| POST | `/posts` | 发布帖子 | USER/ADMIN |
| POST | `/posts/{postId}/approve` | 审核通过帖子 | ADMIN |
| POST | `/posts/{postId}/reject` | 驳回帖子 | ADMIN |
| POST | `/posts/{postId}/comments` | 发布评论 | USER/ADMIN |
| DELETE | `/posts/{postId}` | 删除帖子 | USER/ADMIN |
| POST | `/posts/{postId}/like` | 点赞/取消点赞 | USER/ADMIN |
| POST | `/posts/{postId}/favorite` | 收藏/取消收藏 | USER/ADMIN |
| GET | `/users/{userId}` | 获取作者主页 | 公开 |

### 查询参数

#### GET `/api/forum/posts`
- `page`: 页码 (默认1)
- `size`: 每页数量 (默认10)
- `keyword`: 搜索关键词

---

## 个人出租市场接口 (Marketplace)

**基础路径**: `/api/marketplace`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/discover` | 发现附近可租 | 公开 |
| GET | `/listings/my` | 获取我的挂牌 | 登录用户 |
| POST | `/listings` | 发布挂牌 | 登录用户 |
| PUT | `/listings/{id}` | 更新挂牌 | 登录用户 |
| POST | `/listings/{id}/consult` | 咨询挂牌 | 登录用户 |
| POST | `/listings/{id}/applications` | 提交租用申请 | 登录用户 |
| GET | `/applications/owner` | 获取收到的申请（车主） | 登录用户 |
| GET | `/applications/renter` | 获取提交的申请（租客） | 登录用户 |
| PUT | `/applications/{id}/status` | 更新申请状态 | 登录用户 |

### 查询参数

#### GET `/api/marketplace/discover`
- `latitude`: 纬度
- `longitude`: 经度
- `radiusKm`: 半径（公里）
- `type`: 车型

---

## 后台管理接口 (Admin)

### 系统管理 `/api/admin/system`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/overview` | 系统总览 | ADMIN |
| GET | `/users` | 分页查询用户 | ADMIN |
| PUT | `/users/{id}` | 修改用户资料 | ADMIN |
| DELETE | `/users/{id}` | 删除用户 | ADMIN |
| GET | `/blacklist` | 查询黑名单 | ADMIN |
| POST | `/blacklist` | 加入黑名单 | ADMIN |
| DELETE | `/blacklist/{ip}` | 移除黑名单 | ADMIN |
| GET | `/login-logs` | 登录日志 | ADMIN |
| GET | `/visit-logs` | 访问日志 | ADMIN |
| GET | `/operation-logs` | 操作日志 | ADMIN |
| DELETE | `/operation-logs/{id}` | 删除操作日志 | ADMIN |
| POST | `/operation-logs/batch-delete` | 批量删除操作日志 | ADMIN |

### 个人出租审核 `/api/admin/marketplace`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/listings` | 获取挂牌列表 | ADMIN |
| POST | `/listings/{id}/approve` | 通过审核 | ADMIN |
| POST | `/listings/{id}/reject` | 驳回审核 | ADMIN |

---

## 公共接口 (Public)

**基础路径**: `/api/public`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/site-visits` | 记录页面访问 | 公开 |
| GET | `/location-hint` | 获取位置提示（IP定位） | 公开 |

---

## WebSocket 接口

**路径**: `/ws`

用于实时聊天消息推送。

### 连接方式
- 握手时需要在 Header 中携带 `Authorization: Bearer {token}`
- 订阅地址: `/user/queue/messages`

---

## 权限说明

| 权限标识 | 说明 |
|----------|------|
| 公开 | 无需登录即可访问 |
| 登录用户 | 需要有效的 JWT Token |
| USER | 普通用户权限 |
| ADMIN | 管理员权限 |

---

## 响应状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误或业务异常 |
| 401 | 未登录或 Token 已过期 |
| 403 | 无权限访问 |
| 500 | 服务器内部错误 |

---

*文档生成时间: 2026-03-20*
