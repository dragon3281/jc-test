import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/terminal',
    name: 'Terminal',
    component: () => import('@/views/Terminal.vue'),
    meta: { title: 'SSH终端', requiresAuth: true }
  },
  {
    path: '/',
    redirect: '/dashboard',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'DataAnalysis' }
      },
      {
        path: '/server',
        name: 'Server',
        component: () => import('@/views/server/Index.vue'),
        meta: { title: '服务器管理', icon: 'Monitor' }
      },
      {
        path: '/proxy',
        name: 'Proxy',
        component: () => import('@/views/proxy/Index.vue'),
        meta: { title: '代理资源池', icon: 'Connection' }
      },
      {
        path: '/data',
        name: 'Data',
        meta: { title: '数据中心', icon: 'FolderOpened' },
        children: [
          {
            path: '/data/base',
            name: 'DataBase',
            component: () => import('@/views/data/Base.vue'),
            meta: { title: '基础数据' }
          },
          {
            path: '/data/latest',
            name: 'DataLatest',
            component: () => import('@/views/data/Latest.vue'),
            meta: { title: '最新数据' }
          },
          {
            path: '/data/history',
            name: 'DataHistory',
            component: () => import('@/views/data/History.vue'),
            meta: { title: '历史数据' }
          }
        ]
      },
      {
        path: '/business',
        name: 'Business',
        meta: { title: '业务中心', icon: 'Operation' },
        children: [
          {
            path: '/business/analysis',
            name: 'BusinessAnalysis',
            component: () => import('@/views/business/Analysis.vue'),
            meta: { title: '网站分析' }
          },
          {
            path: '/business/register',
            name: 'BusinessRegister',
            component: () => import('@/views/business/Register.vue'),
            meta: { title: '自动化注册' }
          },
          {
            path: '/business/template',
            name: 'BusinessTemplate',
            component: () => import('@/views/business/Template.vue'),
            meta: { title: 'POST模板' }
          }
        ]
      },
      {
        path: '/task',
        name: 'Task',
        meta: { title: '检测任务', icon: 'List' },
        children: [
          {
            path: '/task/create',
            name: 'TaskCreate',
            component: () => import('@/views/task/Create.vue'),
            meta: { title: '新建检测' }
          },
          {
            path: '/task/list',
            name: 'TaskList',
            component: () => import('@/views/task/List.vue'),
            meta: { title: '任务列表' }
          },
          {
            path: '/task/completed',
            name: 'TaskCompleted',
            component: () => import('@/views/task/Completed.vue'),
            meta: { title: '已完成任务' }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录,跳转到登录页
    next('/login')
  } else if (to.path === '/login' && token) {
    // 已登录但访问登录页,跳转到首页
    next('/dashboard')
  } else {
    next()
  }
})

export default router
