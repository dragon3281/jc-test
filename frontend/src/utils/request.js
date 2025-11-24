import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 根据响应码处理
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      
      // 401 未授权,跳转登录
      if (res.code === 401) {
        ElMessage.warning('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        setTimeout(() => {
          window.location.href = '/login?expired=true'
        }, 1500)
      }
      
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  error => {
    // 处理HTTP状态码错误
    if (error.response) {
      const status = error.response.status
      
      if (status === 401) {
        // Token过期或无效
        ElMessage.warning('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        setTimeout(() => {
          window.location.href = '/login?expired=true'
        }, 1500)
      } else if (status === 403) {
        ElMessage.error('没有权限访问该资源')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error(error.response.data?.message || `请求失败（${status}）`)
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      // 设置请求时发生了错误
      ElMessage.error(error.message || '请求失败')
    }
    
    return Promise.reject(error)
  }
)

export default request
