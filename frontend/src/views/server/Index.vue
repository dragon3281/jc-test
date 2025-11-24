<template>
  <div class="server-management">
    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="page-toolbar">
          <div class="header-left">
            <el-icon class="header-icon"><Monitor /></el-icon>
            <div class="toolbar-title">服务器管理</div>
            <el-tag type="info" size="small" class="count-tag">
              总计: {{ serverList.length }} 台
            </el-tag>
          </div>
          <div class="toolbar-actions">
            <el-button type="success" :icon="Refresh" @click="handleRefreshAll" :loading="refreshingAll">
              刷新所有状态
            </el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd" class="add-btn">
              新增服务器
            </el-button>
            <el-button type="primary" :icon="Plus" @click="handleBatchAdd" plain>
              批量新增
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="serverList" v-loading="loading" class="modern-table" stripe>
        <el-table-column prop="serverName" label="服务器名称" min-width="150">
          <template #default="{ row }">
            <div class="server-name">
              <el-icon class="server-icon"><Monitor /></el-icon>
              <span>{{ row.serverName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" min-width="130">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">
              {{ row.ipAddress }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sshPort" label="SSH端口" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : (row.status === 3 ? 'warning' : 'danger')" effect="dark" class="status-tag">
              <el-icon v-if="row.status === 1"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              {{ row.status === 1 ? '在线' : (row.status === 3 ? '异常' : '关机') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资源使用" width="180" align="center">
          <template #default="{ row }">
            <div class="metric-group">
              <div v-if="normalizePercentage(row.cpuUsage) !== null" class="metric-row">
                <div class="metric-bar">
                  <div class="metric-fill" :style="{ width: normalizePercentage(row.cpuUsage) + '%', backgroundColor: getProgressColor(normalizePercentage(row.cpuUsage)) }">
                    <span class="metric-value">{{ normalizePercentage(row.cpuUsage) }}%</span>
                  </div>
                </div>
                <span class="metric-name">CPU</span>
                <span class="metric-percent">{{ normalizePercentage(row.cpuUsage) }}%</span>
              </div>
              <div v-else class="empty-data">-</div>

              <div v-if="normalizePercentage(row.memoryUsage) !== null" class="metric-row">
                <div class="metric-bar">
                  <div class="metric-fill" :style="{ width: normalizePercentage(row.memoryUsage) + '%', backgroundColor: getProgressColor(normalizePercentage(row.memoryUsage)) }">
                    <span class="metric-value">{{ normalizePercentage(row.memoryUsage) }}%</span>
                  </div>
                </div>
                <span class="metric-name">内存</span>
                <span class="metric-percent">{{ normalizePercentage(row.memoryUsage) }}%</span>
              </div>
              <div v-else class="empty-data">-</div>

              <div v-if="normalizePercentage(row.diskUsage) !== null" class="metric-row">
                <div class="metric-bar">
                  <div class="metric-fill" :style="{ width: normalizePercentage(row.diskUsage) + '%', backgroundColor: getProgressColor(normalizePercentage(row.diskUsage)) }">
                    <span class="metric-value">{{ normalizePercentage(row.diskUsage) }}%</span>
                  </div>
                </div>
                <span class="metric-name">磁盘</span>
                <span class="metric-percent">{{ normalizePercentage(row.diskUsage) }}%</span>
              </div>
              <div v-else class="empty-data">-</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="网络流量" width="150" align="center">
          <template #default="{ row }">
            <div v-if="row.networkIn !== null && row.networkOut !== null" style="font-size: 12px">
              <div style="color: #67c23a">↓ {{ formatBytes(row.networkIn) }}/s</div>
              <div style="color: #e6a23c">↑ {{ formatBytes(row.networkOut) }}/s</div>
            </div>
            <span v-else class="empty-data">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="success"
              :icon="Monitor"
              link
              @click="handleTerminal(row)"
            >
              终端
            </el-button>
            <el-button 
              size="small" 
              type="success"
              :icon="Refresh"
              link
              @click="handleRefresh(row)"
              :loading="row.refreshing"
            >
              刷新状态
            </el-button>
            <el-button 
              size="small" 
              type="primary"
              :icon="Edit"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              :icon="Delete"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      class="modern-dialog"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px" class="modern-form">
        <el-form-item label="服务器名称" prop="serverName">
          <el-input v-model="formData.serverName" placeholder="请输入服务器名称" clearable />
        </el-form-item>
        <el-form-item label="IP地址" prop="ipAddress">
          <el-input v-model="formData.ipAddress" placeholder="请输入IP地址" clearable />
        </el-form-item>
        <el-form-item label="SSH端口" prop="sshPort">
          <el-input-number v-model="formData.sshPort" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="SSH用户名" prop="sshUsername">
          <el-input v-model="formData.sshUsername" placeholder="请输入SSH用户名（默认root）" clearable />
        </el-form-item>
        <el-form-item label="认证方式" prop="authType">
          <el-radio-group v-model="formData.authType">
            <el-radio :label="1" border>密码</el-radio>
            <el-radio :label="2" border>密钥</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="凭证" prop="authCredential">
          <el-input
            v-model="formData.authCredential"
            :type="showFormPassword ? 'textarea' : 'password'"
            :rows="3"
            placeholder="请输入密码或密钥"
            show-word-limit
          >
            <template #suffix>
              <el-icon 
                @click="showFormPassword = !showFormPassword" 
                style="cursor: pointer;"
              >
                <View v-if="!showFormPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="最大并发数" prop="maxConcurrent">
          <el-input-number v-model="formData.maxConcurrent" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleSubmit" size="large">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量新增对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量新增服务器"
      width="800px"
      :close-on-click-modal="false"
      class="modern-dialog"
    >
      <el-alert
        title="批量新增说明"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #default>
          <div>每行一个服务器，格式：服务器名称,IP地址,SSH端口,SSH用户名,密码,最大并发数</div>
          <div style="margin-top: 4px">示例：服务器1,192.168.1.100,22,root,password123,10</div>
          <div style="margin-top: 4px; color: #E6A23C">注意：中文逗号分隔，缺省值 - SSH端口:22, SSH用户名:root, 并发数:10</div>
        </template>
      </el-alert>
      <el-input
        v-model="batchServerText"
        type="textarea"
        :rows="12"
        placeholder="请输入服务器信息，每行一个\n示例：\n服务器1,192.168.1.100,22,root,password123,10\n服务器2,192.168.1.101,22,ubuntu,pass456,5"
        show-word-limit
      />
      <div style="margin-top: 12px; color: #909399; font-size: 14px">
        已输入 {{ batchServerText.split('\n').filter(line => line.trim()).length }} 个服务器
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleBatchSubmit" size="large" :loading="batchSubmitting">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Monitor, CircleCheck, CircleClose, Refresh, View, Hide } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加服务器')
const formRef = ref(null)
const serverList = ref([])
const router = useRouter()
const showFormPassword = ref(false)
const batchDialogVisible = ref(false)
const batchServerText = ref('')
const batchSubmitting = ref(false)
const refreshingAll = ref(false)
let autoRefreshTimer = null

const formData = reactive({
  serverName: '',
  ipAddress: '',
  sshPort: 22,
  sshUsername: 'root',
  authType: 1,
  authCredential: '',
  maxConcurrent: 10
})

const formRules = {
  serverName: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  ipAddress: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  sshPort: [{ required: true, message: '请输入SSH端口', trigger: 'blur' }],
  sshUsername: [{ required: true, message: '请输入SSH用户名', trigger: 'blur' }],
  authCredential: [{ required: true, message: '请输入凭证', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
  // 启动自动刷新，每30秒刷新一次
  startAutoRefresh()
})

onActivated(() => {
  loadData()
  // 页面激活时重启自动刷新
  startAutoRefresh()
})

onBeforeUnmount(() => {
  // 页面销毁时清除定时器
  stopAutoRefresh()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/server/list')
    if (res.code === 200) {
      serverList.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('获取服务器列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '添加服务器'
  dialogVisible.value = true
  showFormPassword.value = false
  Object.assign(formData, {
    id: null,
    serverName: '',
    ipAddress: '',
    sshPort: 22,
    sshUsername: 'root',
    authType: 1,
    authCredential: '',
    maxConcurrent: 10
  })
  formRef.value?.clearValidate()
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑服务器'
  dialogVisible.value = true
  showFormPassword.value = false
  // 编辑时不显示密码，需要用户重新输入
  Object.assign(formData, {
    ...row,
    authCredential: '' // 清空密码，要求重新输入
  })
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该服务器吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await request.delete(`/server/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  
  try {
    if (formData.id) {
      await request.put('/server', formData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/server', formData)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const getProgressColor = (percentage) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

// 归一化百分比（支持字符串/小数），无效返回null
const normalizePercentage = (value) => {
  if (value === null || value === undefined) return null
  const n = Number(value)
  if (Number.isNaN(n)) return null
  return Math.max(0, Math.min(100, Math.round(n)))
}

const handleRefresh = async (row) => {
  try {
    row.refreshing = true
    const res = await request.post(`/server/${row.id}/refresh`)
    if (res.code === 200) {
      ElMessage.success('刷新成功')
      // 重新加载数据以获取最新状态
      await loadData()
    }
  } catch (error) {
    ElMessage.error(error.message || '刷新失败')
  } finally {
    row.refreshing = false
  }
}

const togglePassword = (row) => {
  row.showPassword = !row.showPassword
}

const handleTerminal = (row) => {
  // 打开新窗口跳转到终端页面
  const routeData = router.resolve({
    path: '/terminal',
    query: { serverId: row.id, serverName: row.serverName }
  })
  window.open(routeData.href, '_blank')
}

const handleRefreshAll = async () => {
  try {
    refreshingAll.value = true
    const res = await request.post('/server/refresh/all')
    if (res.code === 200) {
      ElMessage.success(`批量刷新成功，共刷新 ${res.data} 台服务器`)
      await loadData()
    }
  } catch (error) {
    ElMessage.error(error.message || '批量刷新失败')
  } finally {
    refreshingAll.value = false
  }
}

const handleBatchAdd = () => {
  batchDialogVisible.value = true
  batchServerText.value = ''
}

const handleBatchSubmit = async () => {
  const lines = batchServerText.value.split('\n').filter(line => line.trim())
  
  if (lines.length === 0) {
    ElMessage.warning('请输入服务器信息')
    return
  }
  
  const serverList = []
  const errors = []
  
  lines.forEach((line, index) => {
    const parts = line.split(',').map(p => p.trim())
    
    if (parts.length < 2) {
      errors.push(`第 ${index + 1} 行格式错误：至少需要服务器名称和IP地址`)
      return
    }
    
    const [serverName, ipAddress, sshPort, sshUsername, authCredential, maxConcurrent] = parts
    
    serverList.push({
      serverName: serverName || `服务器${index + 1}`,
      ipAddress: ipAddress,
      sshPort: sshPort ? parseInt(sshPort) : 22,
      sshUsername: sshUsername || 'root',
      authType: 1,
      authCredential: authCredential || '',
      maxConcurrent: maxConcurrent ? parseInt(maxConcurrent) : 10
    })
  })
  
  if (errors.length > 0) {
    ElMessage.error(errors.join('; '))
    return
  }
  
  if (serverList.some(s => !s.authCredential)) {
    ElMessage.warning('有服务器未填写密码，请检查')
    return
  }
  
  try {
    batchSubmitting.value = true
    const res = await request.post('/server/batch', serverList)
    if (res.code === 200) {
      ElMessage.success(`批量添加成功，共添加 ${res.data} 台服务器`)
      batchDialogVisible.value = false
      batchServerText.value = ''
      await loadData()
    }
  } catch (error) {
    ElMessage.error(error.message || '批量添加失败')
  } finally {
    batchSubmitting.value = false
  }
}

// 启动自动刷新
const startAutoRefresh = () => {
  // 清除之前的定时器
  stopAutoRefresh()
  
  // 每30秒自动刷新，仅当页面可见时执行
  autoRefreshTimer = setInterval(() => {
    if (document.visibilityState === 'visible') {
      loadData()
    }
  }, 30000)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

// 格式化字节数
const formatBytes = (bytes) => {
  if (bytes === null || bytes === undefined) return '0 B'
  if (bytes === 0) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024).toFixed(1) + ' KB'
}
</script>

<style lang="scss" scoped>
.server-management {
  .page-card {
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    border: 1px solid #e4e7ed;

    :deep(.el-card__header) {
      background: #fff;
      padding: 16px 20px;
      border-bottom: 1px solid #e4e7ed;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 20px;
          color: #409eff;
        }

        .header-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }

        .count-tag {
          background: #f4f4f5;
          color: #606266;
          border: none;
        }
      }

      .add-btn {
        font-weight: 500;
      }
    }
  }

  .modern-table {
    :deep(.el-table__header-wrapper) {
      th {
        background: #f5f7fa;
        color: #606266;
        font-weight: 600;
        font-size: 14px;
      }
    }

    .server-name {
      display: flex;
      align-items: center;
      gap: 8px;

      .server-icon {
        font-size: 16px;
        color: #409eff;
      }

      span {
        font-weight: 500;
        color: #303133;
      }
    }

    .status-tag {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 12px;
      border-radius: 4px;
      font-weight: 500;
    }

    .usage-cell {
      padding: 0 10px;

      :deep(.el-progress__text) {
        font-size: 12px;
        font-weight: 600;
      }
    }

    .metric-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: center;
    }
    .metric-row {
      display: flex;
      align-items: center;
      gap: 8px;
      justify-content: center;
    }
    .metric-bar {
      position: relative;
      height: 16px;
      width: 100px;
      background: #f5f7fa;
      border: 1px solid #e4e7ed;
      border-radius: 2px;
      overflow: hidden;
    }
    .metric-fill {
      height: 100%;
      display: flex;
      align-items: center;
      transition: width 0.3s ease;
    }
    .metric-value {
      font-size: 12px;
      font-weight: 600;
      color: #fff;
      padding-left: 4px;
      line-height: 1;
    }
    .metric-name {
      font-size: 12px;
      color: #606266;
      min-width: 28px;
      text-align: left;
    }
    .metric-percent {
      font-size: 12px;
      color: #606266;
      min-width: 34px;
      text-align: right;
    }

    .empty-data {
      color: #909399;
      font-size: 14px;
    }
  }
}

.modern-dialog {
  :deep(.el-dialog__header) {
    background: #fff;
    padding: 20px;
    margin: 0;
    border-bottom: 1px solid #e4e7ed;

    .el-dialog__title {
      color: #303133;
      font-size: 16px;
      font-weight: 600;
    }
  }

  :deep(.el-dialog__body) {
    padding: 24px 20px;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 20px;
    border-top: 1px solid #e4e7ed;
    background: #fafafa;
  }
}

.modern-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #606266;
  }

  :deep(.el-input__inner) {
    border-radius: 4px;
  }

  :deep(.el-textarea__inner) {
    border-radius: 4px;
  }

  :deep(.el-radio.is-bordered) {
    border-radius: 4px;
  }
}
</style>
