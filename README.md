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
- **浏览器自动化**: Playwright 1.45.0

### 前端技术
- **开发框架**: Vue 3
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **路由管理**: Vue Router
- **HTTP客户端**: Axios
- **图表库**: ECharts
- **构建工具**: Vite
- **实时通信**: WebSocket (SockJS + STOMP)

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
- **网站分析**
  - 智能识别注册接口(支持浏览器拦截和JS静态分析)
  - 多国语言注册按钮识别(13种语言)
  - 加密方式自动检测
  - WebSocket实时状态推送
- **自动化注册**(验证码处理、Token提取)
- **POST模板管理**(模板生成、测试、版本管理)

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
- **操作系统**: Ubuntu 22.04 LTS (推荐) / Ubuntu 20.04 LTS
  - 内置 GLIBC 2.35，完美支持 Playwright 浏览器自动化功能
  - 也支持 CentOS 9 / Rocky Linux 9 / AlmaLinux 9 (需要 GLIBC 2.27+)
  - CentOS 7 可运行但浏览器功能会降级到JS静态分析
- **JDK**: 17+
- **Node.js**: 16+
- **Maven**: 3.6+
- **Docker & Docker Compose**: 用于基础服务

### 1. 启动基础服务

```bash
cd docker
docker-compose up -d
```

这将启动以下服务:
- MySQL (端口 3306)
- Redis (端口 6379)
- RabbitMQ (端口 5672, 管理界面 15672)

### 2. 安装 Playwright 浏览器驱动

**重要**: Playwright 用于真实浏览器拦截网络请求，获取准确的注册接口信息。

```bash
cd backend

# 安装 Chromium 浏览器驱动
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI \
              -Dexec.args="install chromium"
```

**验证安装**:
```bash
# 检查 GLIBC 版本 (需要 2.27+)
ldd --version
```

如果看到类似 `GLIBC_2.27 not found` 错误，说明系统版本过低，建议升级到 CentOS 9 或 Ubuntu 20.04+。

### 3. 启动后端服务

```bash
cd backend
mvn clean package -DskipTests
java -jar target/detection-platform-1.0.0.jar

# 或使用 Maven 直接运行
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080/api` 启动

**验证浏览器拦截功能**:
```bash
# 查看日志，应该能看到 [Browser] 相关信息
tail -f backend/app.log | grep '\[Browser\]'

# 成功时会显示:
# [Browser] 捕获注册接口: https://example.com/api/register [POST]
```

### 4. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

### 5. 访问系统

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

## 新增功能: Playwright 浏览器拦截 🎉

### 功能概述
系统现已支持通过 **真实浏览器** 自动化拦截网络请求来分析网站注册流程，相比传统的JS静态分析更加准确可靠。

### 核心能力

#### 1. 多语言注册按钮识别
系统支持 **13种语言** 的注册按钮自动识别：
- **中文**: 注册、立即注册、免费注册
- **英文**: register、Register、REGISTER、sign up、Sign Up、signup
- **孟加拉语**: নিবন্ধন
- **印地语**: निबंधन
- **西班牙语**: registrarse、registro、Registrarse、Registro
- **葡萄牙语**: registro、Registro
- **法语**: inscrire、S'inscrire
- **俄语**: регистрация
- **阿拉伯语**: تسجيل
- **日语**: 登録、新規登録
- **韩语**: 회원가입
- **泰语**: ลงทะเบียน
- **越南语**: đăng ký、Đăng ký

#### 2. 工作流程
```
1. 启动无头 Chromium 浏览器
2. 访问目标网站
3. 监听所有网络请求 (Page.onRequest)
4. 自动查找并点击注册按钮
5. 捕获包含 register/signup 关键词的 API 请求
6. 记录真实的 URL 和 HTTP 方法
7. 如果浏览器失败，自动降级到 JS 静态分析
```

#### 3. 降级策略
系统实现了智能降级机制：
- ✅ **优先级1**: 浏览器真实拦截 (最准确)
- ✅ **优先级2**: JS 静态分析 (兜底方案)
- ✅ 保证在任何环境下都能正常工作

#### 4. WebSocket 实时推送
- 前端自动订阅分析任务状态: `/topic/analysis/{analysisId}`
- 分析完成后自动更新页面，无需手动刷新
- 实时显示分析进度和结果

### 使用示例

**前端操作**:
1. 进入 "业务中心" → "自动化注册分析"
2. 点击 "新增分析"，输入目标网站URL
3. 系统自动执行浏览器拦截分析
4. 页面实时显示分析状态和结果

**查看日志**:
```bash
# 监控浏览器拦截活动
tail -f backend/app.log | grep -E '\[Browser\]|Playwright|捕获注册接口'

# 成功示例:
# [Browser] 捕获注册接口: https://example.com/api/register [POST]
# [Browser] 检测到加密方式: AES-CBC
```

### 环境要求

### 环境要求

| 操作系统 | GLIBC版本 | Playwright支持 | 推荐度 |
|---------|----------|---------------|-------|
| Ubuntu 22.04 LTS | 2.35 | ✅ 完全支持 | ⭐⭐⭐⭐⭐ |
| Ubuntu 20.04 LTS | 2.31 | ✅ 完全支持 | ⭐⭐⭐⭐⭐ |
| CentOS 9 / Rocky Linux 9 | 2.28+ | ✅ 完全支持 | ⭐⭐⭐⭐ |
| CentOS 8 | 2.28+ | ✅ 完全支持 | ⭐⭐⭐⭐ |
| CentOS 7.9 | 2.17 | ⚠️ 降级到JS分析 | ⭐⭐ |

### 故障排查

**问题1: GLIBC 版本错误**
```bash
# 错误信息
GLIBC_2.27 not found
GLIBC_2.28 not found

# 解决方案
1. 升级到 Ubuntu 22.04 LTS (推荐) 或 Ubuntu 20.04 LTS
2. 或接受降级到 JS 静态分析 (系统会自动处理)
```

**问题2: 浏览器拦截失败**
```bash
# 查看日志
tail -f backend/app.log | grep '\[Browser\]'

# 如果看到 "浏览器拦截失败，使用JS分析"
# 说明降级策略已生效，系统仍可正常工作
```

**问题3: Playwright 驱动未安装**
```bash
# 重新安装浏览器驱动
cd backend
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI \
              -Dexec.args="install chromium"
```

### 相关文件

**后端核心文件**:
- `backend/pom.xml` - Playwright 依赖配置
- `backend/src/main/java/com/detection/platform/service/SmartWebAnalyzer.java` - 浏览器拦截逻辑
- `backend/src/main/java/com/detection/platform/service/impl/WebsiteAnalysisServiceImpl.java` - WebSocket 推送

**前端核心文件**:
- `frontend/src/utils/websocket.js` - WebSocket 客户端工具类
- `frontend/src/views/business/AnalysisRegister.vue` - 分析页面 (实时更新)
- `frontend/package.json` - WebSocket 依赖 (sockjs-client, stompjs)

---

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

## Ubuntu 22.04 完整部署指南

### 系统准备

```bash
# 1. 更新系统包
sudo apt update
sudo apt upgrade -y

# 2. 验证 GLIBC 版本 (Ubuntu 22.04 自带 2.35)
ldd --version
# 应显示: ldd (Ubuntu GLIBC 2.35-0ubuntu3.x) 2.35

# 3. 安装基础依赖
sudo apt install -y git wget curl build-essential
```

### 安装 Java 17

```bash
# Ubuntu 22.04 推荐使用 OpenJDK 17
sudo apt install -y openjdk-17-jdk openjdk-17-jre

# 验证安装
java -version
# 应显示: openjdk version "17.0.x"

# 设置 JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# 验证环境变量
echo $JAVA_HOME
```

### 安装 Maven

```bash
# Ubuntu 22.04 可以直接使用 apt 安装 Maven
sudo apt install -y maven

# 验证安装
mvn -v
# 应显示: Apache Maven 3.6.3 或更高版本

# 如果需要最新版本，可以手动安装
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xzf apache-maven-3.9.6-bin.tar.gz
sudo mv apache-maven-3.9.6 /opt/maven

# 配置环境变量（仅当手动安装时）
echo 'export MAVEN_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 安装 Node.js

```bash
# 使用 NodeSource 仓库安装 Node.js 18 LTS
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# 验证安装
node -v
npm -v
# 应显示: v18.x.x 和 9.x.x

# 配置 npm 全局路径（可选，避免权限问题）
mkdir -p ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 克隆项目

```bash
# 配置 Git SSH 密钥 (如果尚未配置)
ssh-keygen -t ed25519 -C "your_email@example.com"
cat ~/.ssh/id_ed25519.pub
# 将公钥添加到 GitHub Settings -> SSH and GPG Keys

# 测试 SSH 连接
ssh -T git@github.com

# 克隆项目
git clone git@github.com:dragon3281/-.git
cd jc-test
```

### 启动基础服务 (Docker)

```bash
# 安装 Docker
sudo apt install -y apt-transport-https ca-certificates gnupg lsb-release

# 添加 Docker 官方 GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 设置 Docker 仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker Engine
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 将当前用户添加到 docker 组（避免每次使用 sudo）
sudo usermod -aG docker $USER

# 注销并重新登录以使组权限生效，或执行：
newgrp docker

# 验证 Docker 安装
docker --version
docker compose version

# 启动基础服务
cd docker
docker compose up -d

# 验证服务状态
docker compose ps
# 应显示 MySQL、Redis、RabbitMQ 都处于 running 状态
```

### 配置数据库

```bash
# 等待 MySQL 启动完成
sleep 30

# 导入初始化脚本
docker exec -i jc-mysql mysql -uroot -p123456 < ../sql/init.sql

# 验证数据库
docker exec -it jc-mysql mysql -uroot -p123456 -e "SHOW DATABASES;"
# 应该能看到 detection_platform 数据库

# 验证表结构
docker exec -it jc-mysql mysql -uroot -p123456 -e "USE detection_platform; SHOW TABLES;"
```

### 安装 Playwright 浏览器

```bash
cd ../backend

# Ubuntu 22.04 需要先安装浏览器依赖
sudo apt install -y \
    libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 \
    libcups2 libdrm2 libxkbcommon0 libxcomposite1 \
    libxdamage1 libxfixes3 libxrandr2 libgbm1 \
    libasound2 libpangocairo-1.0-0 libgtk-3-0

# 安装 Chromium 浏览器驱动
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI \
              -Dexec.args="install chromium"

# 安装系统依赖（Playwright 会提示需要的依赖）
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI \
              -Dexec.args="install-deps chromium"

# 验证安装成功 (应该没有 GLIBC 错误)
echo "✅ Playwright 浏览器安装完成"
```

### 启动后端服务

```bash
# 编译项目
mvn clean package -DskipTests

# 方式1: 直接运行 JAR 包（前台运行）
java -jar target/detection-platform-1.0.0.jar

# 方式2: 后台运行
nohup java -jar target/detection-platform-1.0.0.jar > app.log 2>&1 &

# 查看日志
tail -f app.log

# 验证启动成功 (看到以下信息)
# Started DetectionPlatformApplication in X.XXX seconds
# Tomcat started on port(s): 8080 (http)

# 测试后端 API
curl http://localhost:8080/actuator/health
```

### 启动前端服务

```bash
cd ../frontend

# 安装依赖
npm install

# 方式1: 开发模式（前台运行）
npm run dev

# 方式2: 后台运行
nohup npm run dev > dev.log 2>&1 &

# 查看日志
tail -f dev.log

# 验证启动成功 (看到以下信息)
# VITE v5.x.x ready in XXX ms
# ➜  Local:   http://localhost:3000/

# 测试前端访问
curl http://localhost:3000
```

### 验证系统功能

```bash
# 1. 检查所有服务状态
echo "=== Docker 服务 ==="
docker compose ps

echo "=== 后端服务 ==="
curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"'

echo "=== 前端服务 ==="
curl -s http://localhost:3000 | grep -o '<title>[^<]*</title>'

# 2. 验证 Playwright 浏览器拦截功能
echo "=== 监控浏览器活动 ==="
tail -f ../backend/app.log | grep -E '\[Browser\]|Playwright|浏览器'

# 3. 在前端创建一个分析任务，观察日志输出
# 访问 http://localhost:3000
# 登录后进入 "业务中心" -> "自动化注册分析"
# 创建新分析任务

# 成功时应该看到:
# [Browser] 捕获注册接口: https://example.com/api/register [POST]
# [Browser] 检测到加密方式: AES-CBC
```

### Ubuntu 防火墙配置 (UFW)

```bash
# Ubuntu 使用 UFW 防火墙

# 检查防火墙状态
sudo ufw status

# 如果需要开放端口给外网访问
sudo ufw allow 3000/tcp comment 'Frontend'
sudo ufw allow 8080/tcp comment 'Backend API'
sudo ufw allow 22/tcp comment 'SSH'

# 启用防火墙（如果尚未启用）
sudo ufw enable

# 查看规则
sudo ufw status numbered
```

### 生产环境部署 (systemd)

```bash
# 1. 创建后端服务
sudo tee /etc/systemd/system/detection-backend.service > /dev/null <<'EOF'
[Unit]
Description=Detection Platform Backend Service
After=network.target docker.service
Requires=docker.service

[Service]
Type=simple
User=$USER
WorkingDirectory=/home/$USER/jc-test/backend
ExecStart=/usr/bin/java -jar target/detection-platform-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=append:/home/$USER/jc-test/backend/app.log
StandardError=append:/home/$USER/jc-test/backend/app.log

[Install]
WantedBy=multi-user.target
EOF

# 替换 $USER 为实际用户名
sudo sed -i "s/\$USER/$USER/g" /etc/systemd/system/detection-backend.service

# 2. 创建前端服务（生产环境应该使用构建后的静态文件 + Nginx）
sudo tee /etc/systemd/system/detection-frontend.service > /dev/null <<'EOF'
[Unit]
Description=Detection Platform Frontend Service
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/home/$USER/jc-test/frontend
ExecStart=/usr/bin/npm run dev
Restart=always
RestartSec=10
StandardOutput=append:/home/$USER/jc-test/frontend/dev.log
StandardError=append:/home/$USER/jc-test/frontend/dev.log

[Install]
WantedBy=multi-user.target
EOF

sudo sed -i "s/\$USER/$USER/g" /etc/systemd/system/detection-frontend.service

# 3. 启动服务
sudo systemctl daemon-reload
sudo systemctl start detection-backend
sudo systemctl start detection-frontend

# 4. 设置开机自启
sudo systemctl enable detection-backend
sudo systemctl enable detection-frontend

# 5. 查看服务状态
sudo systemctl status detection-backend
sudo systemctl status detection-frontend

# 6. 查看日志
sudo journalctl -u detection-backend -f
sudo journalctl -u detection-frontend -f
```

### 使用 Nginx 反向代理（生产环境推荐）

```bash
# 1. 安装 Nginx
sudo apt install -y nginx

# 2. 创建配置文件
sudo tee /etc/nginx/sites-available/detection-platform > /dev/null <<'EOF'
server {
    listen 80;
    server_name your-domain.com;  # 替换为你的域名

    # 前端静态文件
    location / {
        root /home/$USER/jc-test/frontend/dist;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 支持
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
EOF

sudo sed -i "s/\$USER/$USER/g" /etc/nginx/sites-available/detection-platform

# 3. 构建前端生产版本
cd /home/$USER/jc-test/frontend
npm run build

# 4. 启用配置
sudo ln -s /etc/nginx/sites-available/detection-platform /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default  # 删除默认配置

# 5. 测试配置
sudo nginx -t

# 6. 重启 Nginx
sudo systemctl restart nginx
sudo systemctl enable nginx

# 7. 验证访问
curl http://localhost
```

### 配置 SSL 证书（HTTPS）

```bash
# 使用 Let's Encrypt 免费证书

# 1. 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx

# 2. 获取证书（自动配置 Nginx）
sudo certbot --nginx -d your-domain.com

# 3. 测试自动续期
sudo certbot renew --dry-run

# 4. Certbot 会自动创建续期定时任务
sudo systemctl status certbot.timer
```

### 常见问题排查（Ubuntu 特定）

**问题1: Playwright 浏览器依赖缺失**
```bash
# 错误信息
Host system is missing dependencies

# 解决方案
sudo apt install -y \
    libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 \
    libcups2 libdrm2 libxkbcommon0 libxcomposite1 \
    libxdamage1 libxfixes3 libxrandr2 libgbm1 \
    libasound2 libpangocairo-1.0-0 libgtk-3-0

# 或使用 Playwright 自动安装
cd backend
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI \
              -Dexec.args="install-deps"
```

**问题2: Docker 权限问题**
```bash
# 错误信息
Got permission denied while trying to connect to the Docker daemon socket

# 解决方案
sudo usermod -aG docker $USER
newgrp docker
# 或注销后重新登录
```

**问题3: 端口被占用**
```bash
# 查看端口占用
sudo lsof -i :8080
sudo lsof -i :3000

# 终止占用进程
sudo kill -9 <PID>

# 或修改配置文件中的端口
```

**问题4: npm install 速度慢**
```bash
# 使用国内镜像源
npm config set registry https://registry.npmmirror.com

# 或使用 cnpm
npm install -g cnpm --registry=https://registry.npmmirror.com
cnpm install
```

### 性能优化建议（Ubuntu）

```bash
# 1. 调整系统文件描述符限制
echo "* soft nofile 65536" | sudo tee -a /etc/security/limits.conf
echo "* hard nofile 65536" | sudo tee -a /etc/security/limits.conf

# 2. 优化 Java 堆内存（根据服务器配置）
# 编辑 systemd 服务文件，添加 JVM 参数
sudo systemctl edit detection-backend
# 添加:
# [Service]
# Environment="JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC"

# 3. 启用 Docker 日志轮转
sudo tee /etc/docker/daemon.json > /dev/null <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF

sudo systemctl restart docker
```

---

## 注意事项

1. **安全性**: 生产环境请务必修改默认密码和密钥
2. **性能**: 根据实际业务量调整并发数和服务器资源
3. **监控**: 定期检查日志和监控指标
4. **备份**: 定期备份数据库和重要配置文件
5. **系统版本**: 强烈推荐使用 Ubuntu 22.04 LTS 以获得完整的 Playwright 浏览器拦截功能
6. **防火墙**: Ubuntu 使用 UFW，CentOS 使用 firewalld，配置方法不同
7. **包管理**: Ubuntu 使用 apt，安装路径和服务管理与 CentOS/yum 有所不同

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题或建议,请联系项目维护者。

---

**注意**: 本系统已完成基础架构搭建,包括项目结构、配置文件和数据库设计。后续功能模块开发中,欢迎参与贡献!
