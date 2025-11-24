# 自动化数据检测平台

## 项目简介

自动化数据检测平台是一个分布式任务调度与执行系统,用于批量检测账号在目标网站的注册状态。系统通过Web界面统一管理多台服务器资源、代理IP池和检测任务,实现高并发、可扩展的自动化检测能力。

## 技术栈

### 后端技术
- **开发语言**: Java 17
- **开发框架**: Spring Boot 3.2.x
- **ORM框架**: MyBatis Plus 3.5.x
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.0
- **消息队列**: RabbitMQ 3.12.x
- **任务调度**: Spring Task + Quartz
- **SSH客户端**: JSch
- **Docker客户端**: docker-java 3.3.x

### 前端技术
- **开发框架**: Vue 3
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **路由管理**: Vue Router
- **HTTP客户端**: Axios
- **图表库**: ECharts
- **构建工具**: Vite

### 基础设施
- **容器化**: Docker + Docker Compose
- **数据库分区**: 按月分区策略
- **缓存策略**: Redis多级缓存

## 系统功能模块

### 1. 服务器管理模块
- 服务器注册与配置
- SSH连接管理
- 服务器状态监控(CPU、内存、磁盘)
- Docker容器管理
- 健康检查与心跳检测

### 2. 代理资源池模块
- 代理池分组管理
- 代理节点增删改查
- 代理健康度评分
- 智能分配与负载均衡
- 使用统计与监控

### 3. 数据中心模块
- 基础数据导入(支持Excel/CSV)
- 检测结果查询与导出
- 历史数据追溯
- 数据统计与分析

### 4. 业务中心模块
- 网站分析(端口检测、接口识别)
- 自动化注册(验证码处理、Token提取)
- POST模板管理(模板生成、测试、版本管理)

### 5. 检测任务模块
- 任务创建与配置
- 任务调度与分发
- 实时进度监控
- 任务结果统计
- WebSocket实时推送

## 项目结构

```
jc-test/
├── backend/                # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/detection/platform/
│   │   │   │   ├── controller/      # 控制器层
│   │   │   │   ├── service/         # 业务逻辑层
│   │   │   │   ├── dao/             # 数据访问层
│   │   │   │   ├── entity/          # 实体类
│   │   │   │   ├── dto/             # 数据传输对象
│   │   │   │   ├── vo/              # 视图对象
│   │   │   │   ├── config/          # 配置类
│   │   │   │   ├── common/          # 公共组件
│   │   │   │   ├── converter/       # 对象转换器
│   │   │   │   ├── manager/         # 通用业务封装
│   │   │   │   └── scheduler/       # 定时任务
│   │   │   └── resources/
│   │   │       ├── application.yml  # 应用配置
│   │   │       └── mapper/          # MyBatis映射文件
│   │   └── test/                    # 测试代码
│   └── pom.xml                      # Maven配置
├── frontend/               # 前端项目
│   ├── src/
│   │   ├── views/          # 页面组件
│   │   ├── components/     # 公共组件
│   │   ├── router/         # 路由配置
│   │   ├── stores/         # 状态管理
│   │   ├── api/            # API接口
│   │   ├── utils/          # 工具函数
│   │   └── assets/         # 静态资源
│   ├── package.json        # npm配置
│   └── vite.config.js      # Vite配置
├── docker/                 # Docker配置
│   ├── docker-compose.yml  # Docker Compose配置
│   └── .env                # 环境变量
├── sql/                    # 数据库脚本
│   └── init.sql            # 初始化脚本
└── docs/                   # 文档目录
```

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- Docker & Docker Compose
- Maven 3.6+

### 1. 启动基础服务

```bash
cd docker
docker-compose up -d
```

这将启动以下服务:
- MySQL (端口 3306)
- Redis (端口 6379)
- RabbitMQ (端口 5672, 管理界面 15672)

### 2. 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080/api` 启动

### 3. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

### 4. 访问系统

- 前端地址: http://localhost:3000
- 后端API: http://localhost:8080/api
- RabbitMQ管理界面: http://localhost:15672 (用户名/密码: admin/admin)

默认管理员账号:
- 用户名: admin
- 密码: admin123

## 数据库设计

系统包含以下核心数据表:

1. **t_server** - 服务器表
2. **t_proxy_pool** - 代理池表
3. **t_proxy_node** - 代理节点表
4. **t_base_data** - 基础数据表
5. **t_post_template** - POST模板表
6. **t_detection_task** - 检测任务表
7. **t_task_server** - 任务服务器关联表
8. **t_detection_result** - 检测结果表(分区表)
9. **t_user** - 用户表

详细的表结构请查看 `sql/init.sql` 文件。

## 环境变量配置

在 `docker/.env` 文件中配置以下环境变量:

```env
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=detection_platform
DB_USER=root
DB_PASSWORD=123456

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# RabbitMQ配置
MQ_HOST=localhost
MQ_PORT=5672
MQ_USER=admin
MQ_PASSWORD=admin

# JWT密钥
JWT_SECRET=detection_platform_secret_key_2024

# 加密密钥
ENCRYPT_KEY=detection_encrypt_key_2024_32b
```

## 核心特性

### 1. 分布式任务调度
- 支持多服务器并发执行
- 任务自动拆分与分配
- 失败自动重试机制

### 2. 智能代理管理
- 代理健康度评分系统
- 自动负载均衡
- 代理池动态切换

### 3. 实时监控
- WebSocket实时进度推送
- 服务器状态实时监控
- 任务执行情况可视化

### 4. 数据安全
- SSH密码/密钥AES-256加密
- JWT Token认证
- 操作日志审计

### 5. 高性能优化
- Redis多级缓存
- 数据库分区策略
- 异步任务处理
- 消息队列削峰填谷

## 开发阶段规划

### 第一阶段:基础设施搭建 ✅
- [x] 后端Spring Boot项目结构
- [x] 前端Vue3项目结构
- [x] Docker Compose配置
- [x] 数据库表结构设计

### 第二阶段:后端核心功能开发
- [ ] 实体类和DTO/VO对象
- [ ] 服务器管理模块
- [ ] 代理资源池模块
- [ ] POST模板管理模块
- [ ] 数据中心模块

### 第三阶段:前端核心页面开发
- [ ] 主布局和路由配置
- [ ] 仪表盘页面
- [ ] 服务器管理页面
- [ ] 代理资源池页面

### 第四阶段:任务调度功能开发
- [ ] 任务管理后端模块
- [ ] 任务调度服务
- [ ] 任务相关前端页面

### 第五阶段:测试与部署
- [ ] 单元测试
- [ ] 集成测试
- [ ] 性能测试
- [ ] 生产环境部署

## 注意事项

1. **安全性**: 生产环境请务必修改默认密码和密钥
2. **性能**: 根据实际业务量调整并发数和服务器资源
3. **监控**: 定期检查日志和监控指标
4. **备份**: 定期备份数据库和重要配置文件

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题或建议,请联系项目维护者。

---

**注意**: 本系统已完成基础架构搭建,包括项目结构、配置文件和数据库设计。后续功能模块开发中,欢迎参与贡献!
