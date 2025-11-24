<template>
  <div class="proxy-management">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">代理资源池</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="handleAddPool">添加代理池</el-button>
          </div>
        </div>
      </template>

      <el-table :data="proxyPoolList" v-loading="loading">
        <el-table-column prop="poolName" label="代理池名称" />
        <el-table-column prop="proxyIp" label="IP地址" />
        <el-table-column prop="proxyPort" label="端口" width="100" />
        <el-table-column prop="proxyType" label="类型" width="100">
          <template #default="{ row }">
            {{ getProxyTypeText(row.proxyType) }}
          </template>
        </el-table-column>
        <el-table-column prop="authType" label="认证" width="80">
          <template #default="{ row }">
            <el-tag :type="row.authType === 1 ? 'warning' : 'info'" size="small">
              {{ row.authType === 1 ? '需要' : '无' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="healthScore" label="健康度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.healthScore" :color="getHealthColor(row.healthScore)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEditPool(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeletePool(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="代理池名称" prop="poolName">
          <el-input v-model="formData.poolName" placeholder="请输入代理池名称" clearable />
        </el-form-item>
        
        <el-divider content-position="left">代理参数</el-divider>
        
        <el-form-item label="IP地址" prop="proxyIp">
          <el-input v-model="formData.proxyIp" placeholder="例如：1.2.3.4" clearable />
        </el-form-item>
        
        <el-form-item label="端口" prop="proxyPort">
          <el-input-number v-model="formData.proxyPort" :min="1" :max="65535" :controls="true" style="width: 100%;" />
        </el-form-item>
        
        <el-form-item label="协议类型" prop="proxyType">
          <el-radio-group v-model="formData.proxyType">
            <el-radio :label="1">HTTP</el-radio>
            <el-radio :label="2">HTTPS</el-radio>
            <el-radio :label="3">SOCKS5</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="是否认证" prop="needAuth">
          <el-switch v-model="needAuth" />
        </el-form-item>
        
        <template v-if="needAuth">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          
          <el-form-item label="密码" prop="password">
            <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password clearable />
          </el-form-item>
        </template>
        
        <el-divider />
        
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const proxyPoolList = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('添加代理池')
const formRef = ref(null)
const needAuth = ref(false)
const formData = reactive({
  id: null,
  poolName: '',
  proxyIp: '',
  proxyPort: 8080,
  proxyType: 1,
  needAuth: 0,
  username: '',
  password: '',
  description: ''
})
const formRules = {
  poolName: [{ required: true, message: '请输入代理池名称', trigger: 'blur' }],
  proxyIp: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  proxyPort: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  proxyType: [{ required: true, message: '请选择代理类型', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  description: [{ max: 500, message: '描述长度不能超过500', trigger: 'blur' }]
}

// 监听认证开关变化
watch(needAuth, (val) => {
  formData.needAuth = val ? 1 : 0
})

onMounted(() => {
  loadData()
})

onActivated(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/proxy/pool/list')
    if (res.code === 200) {
      proxyPoolList.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('获取代理池列表失败')
  } finally {
    loading.value = false
  }
}

const getProxyTypeText = (type) => {
  const map = { 1: 'HTTP', 2: 'HTTPS', 3: 'SOCKS5' }
  return map[type] || '未知'
}

const getStatusText = (status) => {
  const map = { 1: '可用', 2: '不可用', 3: '未检测' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

const getHealthColor = (score) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

const handleAddPool = () => {
  dialogTitle.value = '添加代理池'
  dialogVisible.value = true
  needAuth.value = false
  Object.assign(formData, {
    id: null,
    poolName: '',
    proxyIp: '',
    proxyPort: 8080,
    proxyType: 1,
    needAuth: 0,
    username: '',
    password: '',
    description: ''
  })
  formRef.value?.clearValidate()
}

const handleEditPool = (row) => {
  dialogTitle.value = '编辑代理池'
  dialogVisible.value = true
  needAuth.value = row.authType === 1
  Object.assign(formData, {
    id: row.id,
    poolName: row.poolName,
    proxyIp: row.proxyIp,
    proxyPort: row.proxyPort,
    proxyType: row.proxyType,
    needAuth: row.authType,
    username: row.username || '',
    password: '', // 密码不回显
    description: row.description
  })
  formRef.value?.clearValidate()
}

const handleDeletePool = async (row) => {
  await ElMessageBox.confirm('确定要删除该代理池吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await request.delete(`/proxy/pool/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    const payload = { ...formData }
    // 如果不需要认证，清空用户名和密码
    if (!needAuth.value) {
      payload.username = ''
      payload.password = ''
    }
    
    if (formData.id) {
      await request.put('/proxy/pool', payload)
      ElMessage.success('更新成功')
    } else {
      await request.post('/proxy/pool', payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '操作失败')
  }
}


</script>

<style lang="scss" scoped>
.proxy-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .proxy-nodes {
    padding: 20px;
    background: #f5f7fa;

    .mt-10 {
      margin-top: 10px;
    }
  }
}
</style>
