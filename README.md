# POST模板批量检测系统

自动化数据检测平台 - 支持POST请求模板批量检测与自适应限流控制

## 主要功能

### 核心特性
- ✅ **自适应限流与并发控制** - 自动降速/加速，避免触发网站限流
- ✅ **批量检测任务持久化** - 支持断点续跑，任务进度实时保存
- ✅ **三态数据管理** - 明确区分未处理/已注册/未注册状态
- ✅ **gzip响应解压** - 自动识别并解压gzip压缩响应
- ✅ **前端分步流程UI** - 上传文件 → 批量检测 → 完成
- ✅ **详情页实时进度** - 支持关闭弹窗后继续运行，详情页实时查看
- ✅ **轮询分配策略** - 固定轮询逻辑，自动分配token
- ✅ **任务后台运行** - 支持关闭浏览器后任务继续执行

### 技术亮点
- 🚀 **自适应并发算法** - 连续限流→降并发+休眠；持续成功→提升并发
- 💾 **任务持久化机制** - 每个检测结果实时写入数据库，支持断点续跑
- 📊 **实时进度展示** - WebSocket + 轮询双重保障，进度实时更新
- 🔄 **响应解压兼容** - 支持gzip压缩响应的自动解析
- 🎯 **精准状态判定** - 支持状态码+关键字双重判定

## 技术栈

### 后端
- **框架**: Spring Boot 3.x
- **ORM**: MyBatis Plus
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **构建工具**: Maven

### 前端
- **框架**: Vue 3
- **UI组件**: Element Plus
- **构建工具**: Vite
- **状态管理**: Pinia
- **实时通信**: WebSocket

### 基础设施
- **容器化**: Docker + Docker Compose
- **反向代理**: Nginx
- **版本控制**: Git

## 快速开始

### 前置要求
- Docker & Docker Compose
- JDK 17+
- Node.js 16+
- Maven 3.6+

### 启动步骤

1. **启动基础服务**
```bash
cd docker
docker-compose up -d
```

2. **启动后端**
```bash
cd backend
mvn clean package -DskipTests
java -jar target/detection-platform-1.0.0.jar
```

3. **启动前端**
```bash
cd frontend
npm install
npm run dev
```

4. **访问系统**
- 前端地址: http://localhost:3000
- 后端API: http://localhost:8080
- 默认账号: admin / admin123

## 核心功能说明

### POST模板批量检测

#### 运行流程
1. **上传文件** - 上传token.txt和phone.txt（每行一个）
2. **批量检测** - 自动启动批量检测，按轮询分配token
3. **完成下载** - 检测完成后可下载已注册/未注册号码

#### 自适应限流算法
```
初始并发 = token数量（或maxConcurrency）
连续触发限流达阈值（默认5次）→ 并发-1 + 固定休眠（默认2秒）
持续成功（成功次数达到"当前并发×3"）→ 并发+1（不超过maxConcurrency）
```

#### 数据状态管理
- **未处理**: 上传后的初始状态
- **已注册**: 检测后符合判定条件（状态码+关键字）
- **未注册**: 检测后不符合判定条件

### 性能评估

#### 服务器配置建议（8核/16G）
- **轻载场景**（≈12KB响应）
  - 20Mbps: 并发≈103, 每日≈1,440万
  - 100Mbps: 并发≈517, 每日≈7,200万

- **中载场景**（≈50KB响应）
  - 20Mbps: 并发≈124, 每日≈1,730万
  - 100Mbps: 并发≈248, 每日≈3,460万

- **重载场景**（≈200KB响应）
  - 20Mbps: 并发≈6, 每日≈86万
  - 100Mbps: 并发≈31, 每日≈435万

## 数据库设计

### 核心表
- `t_post_template` - POST请求模板
- `t_detection_task_item` - 检测任务明细（支持断点续跑）
- `t_user` - 用户表
- `t_register_task` - 注册任务
- `t_proxy_pool` - 代理池

### 关键字段
- `rate_limit_keyword` - 限流关键字
- `max_consecutive_rate_limit` - 连续限流触发次数阈值
- `backoff_seconds` - 触发限流后暂停秒数
- `min_concurrency` / `max_concurrency` - 最小/最大并发数

## API文档

### POST模板相关
- `POST /template/detect/start` - 启动批量检测
- `GET /template/detect/status/{taskId}` - 查询任务状态
- `GET /template/detect/result/{taskId}` - 获取任务结果
- `GET /template/detect/latest/{templateId}` - 获取模板最近任务
- `GET /template/detect/export/{taskId}` - 导出检测结果

## 项目结构
```
jc-test/
├── backend/              # Spring Boot后端
│   ├── src/main/
│   │   ├── java/com/detection/platform/
│   │   │   ├── controller/      # 控制器
│   │   │   ├── service/         # 服务层
│   │   │   ├── entity/          # 实体类
│   │   │   ├── mapper/          # MyBatis Mapper
│   │   │   └── config/          # 配置类
│   │   └── resources/
│   │       └── application.yml  # 应用配置
│   └── pom.xml
├── frontend/             # Vue 3前端
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   ├── router/             # 路由配置
│   │   ├── utils/              # 工具类
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
├── docker/               # Docker配置
│   └── docker-compose.yml
├── sql/                  # SQL脚本
│   ├── init.sql
│   └── upgrade-detection-task-enhancement.sql
└── docs/                 # 文档
```

## 贡献指南

欢迎提交Issue和Pull Request！

## 许可证

MIT License

## 联系方式

如有问题，请提交Issue或联系项目维护者。

---

**注意**: 本项目仅供学习交流使用，请遵守相关法律法规，不得用于非法用途。
