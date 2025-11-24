#!/bin/bash

###############################################################################
# 自动化数据检测平台 - 依赖安装脚本
# 适用于: CentOS 7/8, RHEL 7/8, Ubuntu 18/20/22
# 用途: 自动检测并安装所有必需的依赖软件
###############################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检测操作系统
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        VER=$VERSION_ID
    elif [ -f /etc/redhat-release ]; then
        OS="centos"
        VER=$(cat /etc/redhat-release | sed 's/.*release //;s/ .*//')
    else
        log_error "无法检测操作系统类型"
        exit 1
    fi
    
    log_info "检测到操作系统: $OS $VER"
}

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then 
        log_error "请使用root用户或sudo权限运行此脚本"
        exit 1
    fi
}

# 安装JDK 17
install_jdk() {
    log_info "检查JDK安装状态..."
    
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            log_success "JDK已安装,版本: $(java -version 2>&1 | head -n 1)"
            return 0
        else
            log_warn "检测到旧版本JDK,将安装JDK 17"
        fi
    fi
    
    log_info "开始安装JDK 17..."
    
    if [[ "$OS" == "centos" ]] || [[ "$OS" == "rhel" ]]; then
        yum install -y java-17-openjdk java-17-openjdk-devel
    elif [[ "$OS" == "ubuntu" ]] || [[ "$OS" == "debian" ]]; then
        apt-get update
        apt-get install -y openjdk-17-jdk
    fi
    
    log_success "JDK 17安装完成"
    java -version
}

# 安装Maven
install_maven() {
    log_info "检查Maven安装状态..."
    
    if command -v mvn &> /dev/null; then
        log_success "Maven已安装,版本: $(mvn -version | head -n 1)"
        return 0
    fi
    
    log_info "开始安装Maven 3.9.5..."
    
    cd /opt
    wget -q https://dlcdn.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz
    tar -xzf apache-maven-3.9.5-bin.tar.gz
    rm -f apache-maven-3.9.5-bin.tar.gz
    
    # 配置环境变量
    echo 'export MAVEN_HOME=/opt/apache-maven-3.9.5' >> /etc/profile
    echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> /etc/profile
    source /etc/profile
    
    # 创建软链接
    ln -sf /opt/apache-maven-3.9.5/bin/mvn /usr/local/bin/mvn
    
    # 配置国内镜像
    mkdir -p ~/.m2
    cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF
    
    log_success "Maven安装完成"
    mvn -version
}

# 安装Node.js
install_nodejs() {
    log_info "检查Node.js安装状态..."
    
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
        if [ "$NODE_VERSION" -ge 16 ]; then
            log_success "Node.js已安装,版本: $(node -v)"
            return 0
        else
            log_warn "检测到旧版本Node.js,将安装最新版本"
        fi
    fi
    
    log_info "开始安装Node.js 18..."
    
    if [[ "$OS" == "centos" ]] || [[ "$OS" == "rhel" ]]; then
        curl -sL https://rpm.nodesource.com/setup_18.x | bash -
        yum install -y nodejs
    elif [[ "$OS" == "ubuntu" ]] || [[ "$OS" == "debian" ]]; then
        curl -sL https://deb.nodesource.com/setup_18.x | bash -
        apt-get install -y nodejs
    fi
    
    # 配置npm国内镜像
    npm config set registry https://registry.npmmirror.com
    
    log_success "Node.js安装完成"
    node -v
    npm -v
}

# 安装Docker
install_docker() {
    log_info "检查Docker安装状态..."
    
    if command -v docker &> /dev/null; then
        log_success "Docker已安装,版本: $(docker -v)"
        return 0
    fi
    
    log_info "开始安装Docker..."
    
    if [[ "$OS" == "centos" ]] || [[ "$OS" == "rhel" ]]; then
        # 卸载旧版本
        yum remove -y docker docker-client docker-client-latest docker-common \
                     docker-latest docker-latest-logrotate docker-logrotate docker-engine
        
        # 安装依赖
        yum install -y yum-utils device-mapper-persistent-data lvm2
        
        # 添加Docker仓库
        yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
        
        # 安装Docker
        yum install -y docker-ce docker-ce-cli containerd.io
        
    elif [[ "$OS" == "ubuntu" ]] || [[ "$OS" == "debian" ]]; then
        # 卸载旧版本
        apt-get remove -y docker docker-engine docker.io containerd runc
        
        # 安装依赖
        apt-get update
        apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
        
        # 添加Docker GPG密钥
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
        
        # 添加Docker仓库
        echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
        
        # 安装Docker
        apt-get update
        apt-get install -y docker-ce docker-ce-cli containerd.io
    fi
    
    # 启动Docker
    systemctl start docker
    systemctl enable docker
    
    # 添加当前用户到docker组
    if [ -n "$SUDO_USER" ]; then
        usermod -aG docker $SUDO_USER
        log_info "已将用户 $SUDO_USER 添加到docker组,请重新登录以生效"
    fi
    
    log_success "Docker安装完成"
    docker --version
}

# 安装Docker Compose
install_docker_compose() {
    log_info "检查Docker Compose安装状态..."
    
    if command -v docker-compose &> /dev/null; then
        log_success "Docker Compose已安装,版本: $(docker-compose version --short)"
        return 0
    fi
    
    log_info "开始安装Docker Compose..."
    
    # 下载最新版本
    COMPOSE_VERSION="v2.20.0"
    curl -L "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" \
         -o /usr/local/bin/docker-compose
    
    # 添加执行权限
    chmod +x /usr/local/bin/docker-compose
    
    # 创建软链接
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    
    log_success "Docker Compose安装完成"
    docker-compose --version
}

# 主函数
main() {
    echo "=========================================="
    echo "自动化数据检测平台 - 依赖安装"
    echo "=========================================="
    echo ""
    
    # 检查权限
    check_root
    
    # 检测操作系统
    detect_os
    
    echo ""
    log_info "开始安装依赖软件..."
    echo ""
    
    # 安装各个组件
    install_jdk
    echo ""
    
    install_maven
    echo ""
    
    install_nodejs
    echo ""
    
    install_docker
    echo ""
    
    install_docker_compose
    echo ""
    
    echo "=========================================="
    echo "依赖安装完成"
    echo "=========================================="
    echo ""
    
    log_success "所有依赖已成功安装!"
    echo ""
    echo "已安装的软件版本:"
    echo "  - Java: $(java -version 2>&1 | head -n 1)"
    echo "  - Maven: $(mvn -version | head -n 1)"
    echo "  - Node.js: $(node -v)"
    echo "  - npm: $(npm -v)"
    echo "  - Docker: $(docker --version)"
    echo "  - Docker Compose: $(docker-compose --version)"
    echo ""
    echo "下一步操作:"
    echo "  1. 启动基础服务: cd /root/jc-test/docker && docker-compose up -d"
    echo "  2. 初始化数据库: docker exec -i detection-mysql mysql -uroot -p123456 < /root/jc-test/sql/init.sql"
    echo "  3. 启动后端服务: cd /root/jc-test/backend && mvn spring-boot:run"
    echo "  4. 启动前端服务: cd /root/jc-test/frontend && npm install && npm run dev"
    echo ""
    echo "或直接运行一键启动脚本:"
    echo "  ./start.sh"
    echo ""
}

# 执行主函数
main
