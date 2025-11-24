<template>
  <div class="app-container">
    <el-container>
      <!-- 左侧菜单 -->
      <el-aside width="240px" class="sidebar-container">
        <div class="logo">
          <div class="logo-icon">
            <el-icon><Monitor /></el-icon>
          </div>
          <h2>检测平台</h2>
        </div>
        <el-scrollbar class="menu-scrollbar">
          <el-menu
            :default-active="$route.path"
            class="el-menu-vertical"
            router
            :unique-opened="true"
          >
            <el-menu-item index="/dashboard" class="menu-item-dashboard">
              <el-icon><DataLine /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>

            <el-sub-menu index="1" class="custom-submenu">
              <template #title>
                <el-icon><Monitor /></el-icon>
                <span>资源管理</span>
              </template>
              <el-menu-item index="/server">
                <span class="submenu-dot"></span>
                服务器管理
              </el-menu-item>
              <el-menu-item index="/proxy">
                <span class="submenu-dot"></span>
                代理资源池
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="2" class="custom-submenu">
              <template #title>
                <el-icon><Document /></el-icon>
                <span>数据中心</span>
              </template>
              <el-menu-item index="/data/base">
                <span class="submenu-dot"></span>
                基础数据
              </el-menu-item>
              <el-menu-item index="/data/latest">
                <span class="submenu-dot"></span>
                最新数据
              </el-menu-item>
              <el-menu-item index="/data/history">
                <span class="submenu-dot"></span>
                历史数据
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="3" class="custom-submenu">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>业务中心</span>
              </template>
              <el-menu-item index="/business/analysis">
                <span class="submenu-dot"></span>
                网站分析
              </el-menu-item>
              <el-menu-item index="/business/register">
                <span class="submenu-dot"></span>
                自动化注册
              </el-menu-item>
              <el-menu-item index="/business/template">
                <span class="submenu-dot"></span>
                POST模板
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="4" class="custom-submenu">
              <template #title>
                <el-icon><List /></el-icon>
                <span>检测任务</span>
              </template>
              <el-menu-item index="/task/create">
                <span class="submenu-dot"></span>
                新建检测
              </el-menu-item>
              <el-menu-item index="/task/list">
                <span class="submenu-dot"></span>
                任务列表
              </el-menu-item>
              <el-menu-item index="/task/completed">
                <span class="submenu-dot"></span>
                已完成任务
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <!-- 右侧内容区 -->
      <el-container>
        <!-- 顶部导航 -->
        <el-header class="header-container">
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumbList.length > 0" v-for="item in breadcrumbList" :key="item.path">
              {{ item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <div class="header-right">
            <el-dropdown @command="handleCommand" class="user-dropdown">
              <span class="user-info">
                <el-avatar :size="32" class="user-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ userInfo.nickname || userInfo.username }}</span>
                <el-icon class="arrow-down"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="password">
                    <el-icon><Lock /></el-icon>
                    修改密码
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容区 -->
        <el-main class="main-container">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :exclude="['Dashboard']">
              <router-view />
            </keep-alive>
          </transition>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userInfo = ref({})

const routeMap = {
  '/dashboard': { name: '仪表盘' },
  '/server': { name: '服务器管理' },
  '/proxy': { name: '代理资源池' },
  '/data/base': { name: '基础数据' },
  '/data/latest': { name: '最新数据' },
  '/data/history': { name: '历史数据' },
  '/business/analysis': { name: '网站分析' },
  '/business/register': { name: '自动化注册' },
  '/business/template': { name: 'POST模板' },
  '/task/create': { name: '新建检测' },
  '/task/list': { name: '任务列表' },
  '/task/completed': { name: '已完成任务' }
}

const breadcrumbList = computed(() => {
  const path = route.path
  if (path === '/dashboard') return []
  
  const routes = []
  if (routeMap[path]) {
    routes.push({
      name: routeMap[path].name,
      path: path
    })
  }
  return routes
})

onMounted(() => {
  const user = localStorage.getItem('user')
  if (user) {
    userInfo.value = JSON.parse(user)
  }
})

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.success('已退出登录')
      router.push('/login')
    })
  } else if (command === 'password') {
    ElMessage.info('修改密码功能开发中')
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  height: 100vh;
  
  .el-container {
    height: 100%;
  }

  .sidebar-container {
    background: #2c3e50;
    box-shadow: 2px 0 6px rgba(0, 0, 0, 0.05);
    
    .logo {
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      background: #1a252f;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      
      .logo-icon {
        font-size: 24px;
        color: #409eff;
        display: flex;
        align-items: center;
      }
      
      h2 {
        color: #fff;
        margin: 0;
        font-size: 20px;
        font-weight: 600;
      }
    }

    .menu-scrollbar {
      height: calc(100vh - 64px);
    }

    .el-menu {
      border-right: none;
      background: transparent;
      padding: 10px 0;
      
      :deep(.el-menu-item) {
        color: rgba(255, 255, 255, 0.85);
        margin: 2px 8px;
        border-radius: 4px;
        transition: all 0.2s ease;
        
        &:hover {
          background: rgba(255, 255, 255, 0.08);
          color: #fff;
        }
        
        &.is-active {
          background: #409eff;
          color: #fff;
          font-weight: 500;
        }

        .submenu-dot {
          display: inline-block;
          width: 4px;
          height: 4px;
          background: #909399;
          border-radius: 50%;
          margin-right: 8px;
        }

        &.is-active .submenu-dot {
          background: #fff;
        }
      }

      :deep(.el-sub-menu) {
        .el-sub-menu__title {
          color: rgba(255, 255, 255, 0.85);
          margin: 2px 8px;
          border-radius: 4px;
          transition: all 0.2s ease;
          
          &:hover {
            background: rgba(255, 255, 255, 0.08);
            color: #fff;
          }
        }

        &.is-active > .el-sub-menu__title {
          color: #fff;
          font-weight: 500;
        }
      }

      :deep(.el-menu) {
        background: rgba(0, 0, 0, 0.05);
        margin: 4px 0;
      }
    }
  }

  .header-container {
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;

    .breadcrumb {
      font-size: 14px;
      
      :deep(.el-breadcrumb__item) {
        .el-breadcrumb__inner {
          color: #606266;
          font-weight: 400;
          
          &:hover {
            color: #409eff;
          }
        }

        &:last-child .el-breadcrumb__inner {
          color: #303133;
          font-weight: 500;
        }
      }
    }

    .header-right {
      .user-dropdown {
        .user-info {
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 12px;
          border-radius: 4px;
          transition: background 0.2s;
          
          &:hover {
            background: #f5f7fa;
          }

          .user-avatar {
            background: #409eff;
          }

          .username {
            font-size: 14px;
            color: #303133;
            font-weight: 500;
          }

          .arrow-down {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
  }

  .main-container {
    background: #f5f7fa;
    padding: 24px;
    overflow-y: auto;
  }
}

// 页面切换动画
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: opacity 0.2s;
}

.fade-transform-enter-from {
  opacity: 0;
}

.fade-transform-leave-to {
  opacity: 0;
}
</style>
