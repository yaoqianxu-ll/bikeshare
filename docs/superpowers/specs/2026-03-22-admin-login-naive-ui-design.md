# 管理端登录界面 Naive UI 改造设计

**日期:** 2026-03-22
**状态:** 已批准

## 设计目标

将管理端登录界面从 Element Plus 迁移到 Naive UI，采用"全屏背景 + 悬浮玻璃卡片"的沉浸式视觉风格，打造大气、有冲击力的登录体验。

## 视觉规格

### 整体布局

- 全屏背景层（深色渐变 + 粒子动效）
- 中央悬浮毛玻璃登录卡片（420px × 520px）
- 响应式：移动端单栏布局

### 背景层

| 属性 | 值 |
|------|-----|
| 底色渐变 | `linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)` |
| 动效 | Canvas 粒子漂浮动画（缓慢移动的光点） |

### 登录卡片

| 属性 | 值 |
|------|-----|
| 尺寸 | 420px × 520px |
| 背景 | `rgba(255, 255, 255, 0.1)` |
| 毛玻璃 | `backdrop-filter: blur(20px)` |
| 边框 | `1px solid rgba(255, 255, 255, 0.2)` |
| 圆角 | 24px |
| 阴影 | `0 25px 80px rgba(0, 0, 0, 0.3)` |

### 标题区

| 元素 | 样式 |
|------|------|
| 主标题 "BikeShare" | 48px, 白色，700 粗体 |
| 副标题 "管理后台" | 16px, rgba(255,255,255,0.7) |
| 图标 | Naive UI icon 或自定义 SVG |

### 输入框 (Naive UI n-input)

| 属性 | 值 |
|------|-----|
| 背景 | 深色半透明 |
| 边框 | `1px solid rgba(255,255,255,0.2)` |
| Focus 边框 | 蓝色高亮 `#667eea` |
| 文字颜色 | 白色 |
| 图标前缀 | 用户/锁 icon |

### 登录按钮 (Naive UI n-button)

| 属性 | 值 |
|------|-----|
| 类型 | primary |
| 背景渐变 | `linear-gradient(135deg, #667eea 0%, #764ba2 100%)` |
| 高度 | 48px |
| 圆角 | 12px |
| Hover 效果 | 上浮 2px + 阴影增强 |

## 技术实现

### 依赖安装

```bash
cd bickdemo-admin
npm install naive-ui vfonts
```

### 组件迁移

| 原 Element Plus | 新 Naive UI |
|-----------------|-------------|
| el-input | n-input |
| el-button | n-button |
| el-form | n-form |
| el-icon | n-icon |

### 文件结构

```
bickdemo-admin/src/
├── views/Login.vue          # 主登录页面（重写）
├── components/Particles.vue # 粒子背景组件（新增）
└── main.js                  # 注册 Naive UI
```

## 验收标准

1. [ ] 视觉上呈现"大气"的沉浸式效果
2. [ ] 毛玻璃卡片质感精致
3. [ ] 粒子背景动画流畅不卡顿
4. [ ] 登录功能正常工作
5. [ ] 响应式布局适配移动端
