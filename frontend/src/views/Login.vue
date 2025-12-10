<template>
  <div class="login-container">
    <el-form 
      ref="loginFormRef" 
      :model="loginForm" 
      :rules="loginRules" 
      class="login-form"
      auto-complete="on"
      label-position="left"
    >
      <div class="title-container">
        <div class="logo-icon">
          <el-icon :size="48"><Monitor /></el-icon>
        </div>
        <h3 class="title">自动化数据检测平台</h3>
        <p class="subtitle">系统登录</p>
      </div>

      <el-form-item prop="username">
        <el-input
          ref="username"
          v-model="loginForm.username"
          placeholder="请输入用户名"
          name="username"
          type="text"
          tabindex="1"
          auto-complete="on"
          size="large"
        >
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          ref="password"
          v-model="loginForm.password"
          type="password"
          placeholder="请输入密码"
          name="password"
          tabindex="2"
          auto-complete="on"
          size="large"
          show-password
          @keyup.enter="handleLogin"
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-button
        :loading="loading"
        type="primary"
        size="large"
        class="login-button"
        @click="handleLogin"
      >
        <span v-if="!loading">登 录</span>
        <span v-else>登录中...</span>
      </el-button>


    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  // 检查是否因为token过期跳转过来的
  const fromExpired = route.query.expired
  if (fromExpired === 'true') {
    ElMessage.warning('登录已过期，请重新登录')
  }
})

const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request.post('/user/login', loginForm)
        if (res.code === 200) {
          // 保存token
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('user', JSON.stringify(res.data.user))
          
          ElMessage.success('登录成功')
          router.push('/')
        } else {
          ElMessage.error(res.message || '登录失败')
        }
      } catch (error) {
        ElMessage.error('登录失败: ' + (error.message || '网络错误'))
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background: #f0f2f5;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  .login-form {
    position: relative;
    width: 420px;
    max-width: 90%;
    padding: 40px 40px 30px;
    margin: 0 auto;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    border: 1px solid #e4e7ed;

    :deep(.el-form-item) {
      margin-bottom: 22px;

      .el-input__wrapper {
        padding: 10px 12px;
        border-radius: 4px;

        .el-input__inner {
          font-size: 14px;
        }
      }
    }
  }

  .title-container {
    text-align: center;
    margin-bottom: 32px;

    .logo-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 80px;
      height: 80px;
      background: #409eff;
      border-radius: 50%;
      margin-bottom: 16px;

      .el-icon {
        color: #fff;
      }
    }

    .title {
      font-size: 24px;
      color: #303133;
      margin: 0 0 8px;
      font-weight: 600;
    }

    .subtitle {
      font-size: 14px;
      color: #909399;
      margin: 0;
      font-weight: 400;
    }
  }

  .login-button {
    width: 100%;
    height: 44px;
    margin: 10px 0 24px;
    font-size: 15px;
    font-weight: 500;
    border-radius: 4px;
  }

  .tips {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 13px;
    color: #909399;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;

    .el-icon {
      color: #409eff;
      font-size: 14px;
    }

    span {
      font-weight: 400;
    }
  }
}
</style>
