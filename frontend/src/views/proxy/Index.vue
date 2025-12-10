<template>
  <div class="proxy-management">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">代理资源池</div>
          <div class="toolbar-actions">
            <el-button @click="handleRefresh" icon="Refresh">刷新</el-button>
            <el-button type="warning" @click="handleManageGroups" icon="FolderOpened">分组管理</el-button>
            <el-button type="success" @click="handleQuickRecognize" icon="MagicStick">一键识别</el-button>
            <el-button type="primary" @click="handleAddPool">添加代理节点</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 20px">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="关键词">
            <el-input v-model="searchForm.keyword" placeholder="搜索节点名称/IP" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="国家">
            <el-select v-model="searchForm.country" placeholder="选择国家" clearable filterable style="width: 150px">
              <el-option v-for="c in countryList" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="分组">
            <el-select v-model="searchForm.groupName" placeholder="选择分组" clearable filterable style="width: 150px">
              <el-option v-for="g in groupList" :key="g" :label="g" :value="g" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch" icon="Search">搜索</el-button>
            <el-button @click="handleResetSearch" icon="RefreshLeft">重置</el-button>
          </el-form-item>
        </el-form>
        
        <!-- 批量操作栏 -->
        <div v-if="selectedRows.length > 0" style="margin-top: 10px; padding: 10px; background: #f0f9ff; border-radius: 4px; border: 1px solid #91d5ff;">
          <span style="margin-right: 16px; color: #1890ff; font-weight: 500;">
            <el-icon style="vertical-align: middle;"><Check /></el-icon>
            已选择 {{ selectedRows.length }} 个节点
          </span>
          <el-button size="small" type="primary" @click="handleBatchSetGroup" icon="FolderOpened">批量设置分组</el-button>
          <el-button size="small" @click="handleClearSelection" icon="Close">取消选择</el-button>
        </div>
      </div>

      <el-table :data="proxyPoolList" v-loading="loading" @selection-change="handleSelectionChange" ref="tableRef" stripe>
        <el-table-column type="selection" width="50" fixed />
        <el-table-column prop="poolName" label="代理节点" min-width="180" show-overflow-tooltip />
        <el-table-column prop="proxyIp" label="IP地址" width="140" />
        <el-table-column prop="proxyPort" label="端口" width="70" />
        <el-table-column prop="country" label="国家" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.country" size="small" type="info">{{ row.country }}</el-tag>
            <span v-else style="color: #ccc">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="分组" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.groupName" size="small" type="success">{{ row.groupName }}</el-tag>
            <span v-else style="color: #ccc">未分组</span>
          </template>
        </el-table-column>
        <el-table-column prop="proxyType" label="类型" width="85">
          <template #default="{ row }">
            {{ getProxyTypeText(row.proxyType) }}
          </template>
        </el-table-column>
        <el-table-column prop="authType" label="认证" width="70">
          <template #default="{ row }">
            <el-tag :type="row.authType === 1 ? 'warning' : 'info'" size="small">
              {{ row.authType === 1 ? '需要' : '无' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 6px;">
              <span 
                :style="{
                  display: 'inline-block',
                  width: '10px',
                  height: '10px',
                  borderRadius: '50%',
                  backgroundColor: getStatusColor(row.status),
                  boxShadow: `0 0 4px ${getStatusColor(row.status)}`
                }"
              ></span>
              <span style="font-size: 13px;">{{ getStatusText(row.status) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastCheckTime" label="最后检测" width="120">
          <template #default="{ row }">
            <span style="font-size: 13px;">{{ row.lastCheckTime ? formatTime(row.lastCheckTime) : '未检测' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
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
        <el-form-item label="节点名称" prop="poolName">
          <el-input v-model="formData.poolName" placeholder="请输入节点名称" clearable />
        </el-form-item>
        
        <el-form-item label="国家" prop="country">
          <el-select v-model="formData.country" placeholder="请选择或输入国家" filterable allow-create clearable style="width: 100%">
            <el-option v-for="c in countryList" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="分组" prop="groupName">
          <el-select v-model="formData.groupName" placeholder="请选择或输入分组" filterable allow-create clearable style="width: 100%">
            <el-option v-for="g in groupList" :key="g" :label="g" :value="g" />
          </el-select>
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
            <el-input 
              v-model="formData.password" 
              type="password" 
              :placeholder="formData.id ? '不填写则不修改密码' : '请输入密码'" 
              show-password 
              clearable 
            />
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

    <!-- 一键识别对话框 -->
    <el-dialog
      v-model="recognizeDialogVisible"
      title="一键识别代理配置"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        <template #title>
          <div style="font-size: 14px">
            <strong>支持的格式</strong>
          </div>
        </template>
        <div style="font-size: 13px; line-height: 1.8">
          • <strong>SOCKS代理</strong>: socks://base64(user:pass)@host:port#label<br/>
          • <strong>HTTP代理</strong>: http://username:password@host:port<br/>
          • <strong>简化格式</strong>: host:port<br/>
          <br/>
          <strong>示例：</strong><br/>
          socks://d2hzX3FteDo1OGdhbmppQDEyMw==@123.254.105.253:22201#my-proxy
        </div>
      </el-alert>

      <el-form label-width="100px">
        <el-form-item label="代理配置" required>
          <el-input
            v-model="recognizeConfig"
            type="textarea"
            :rows="4"
            placeholder="请粘贴代理配置链接，例如：socks://d2hzX3FteDo1OGdhbmppQDEyMw==@123.254.105.253:22201#233boy-socks"
            clearable
          />
        </el-form-item>
      </el-form>

      <template v-if="parsedConfig">
        <el-divider content-position="left">识别结果</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="协议类型">
            <el-tag :type="parsedConfig.proxyType === 3 ? 'success' : 'primary'" size="small">
              {{ getProxyTypeText(parsedConfig.proxyType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="需要认证">
            <el-tag :type="parsedConfig.hasAuth ? 'warning' : 'info'" size="small">
              {{ parsedConfig.hasAuth ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="主机地址">{{ parsedConfig.host }}</el-descriptions-item>
          <el-descriptions-item label="端口">{{ parsedConfig.port }}</el-descriptions-item>
          <el-descriptions-item label="用户名" v-if="parsedConfig.username">{{ parsedConfig.username }}</el-descriptions-item>
          <el-descriptions-item label="标签" v-if="parsedConfig.label">{{ parsedConfig.label }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="recognizeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleParseConfig" :loading="recognizeLoading" v-if="!parsedConfig">识别配置</el-button>
        <el-button type="success" @click="handleAddFromRecognize" :loading="addLoading" v-if="parsedConfig">添加到代理池</el-button>
      </template>
    </el-dialog>

    <!-- 分组管理对话框 -->
    <el-dialog
      v-model="groupManageDialogVisible"
      title="分组管理"
      width="700px"
      :close-on-click-modal="false"
    >
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleCreateGroup" icon="Plus">创建分组</el-button>
        <el-button @click="loadGroupsData" icon="Refresh">刷新</el-button>
      </div>

      <el-table :data="groupsData" v-loading="groupsLoading" border>
        <el-table-column prop="groupName" label="分组名称" min-width="200">
          <template #default="{ row }">
            <el-tag type="success" size="large">
              <el-icon style="margin-right: 4px"><FolderOpened /></el-icon>
              {{ row.groupName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nodeCount" label="节点数量" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="row.nodeCount > 0 ? 'primary' : 'info'" size="small">
              {{ row.nodeCount }} 个节点
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="handleRenameGroup(row)" icon="Edit">重命名</el-button>
            <el-button size="small" type="danger" @click="handleDeleteGroup(row)" icon="Delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!groupsData || groupsData.length === 0" description="暂无分组数据" />

      <template #footer>
        <el-button @click="groupManageDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 批量设置分组对话框 -->
    <el-dialog
      v-model="batchGroupDialogVisible"
      title="批量设置分组"
      width="500px"
      :close-on-click-modal="false"
    >
      <div style="padding: 10px 0;">
        <p style="margin-bottom: 16px; color: #606266; font-size: 14px;">
          将 <strong style="color: #409eff;">{{ batchSelectedIds.length }}</strong> 个节点加入分组
        </p>
        <el-form label-width="80px">
          <el-form-item label="目标分组">
            <el-select 
              v-model="batchGroupName" 
              placeholder="请选择或输入分组名称" 
              filterable 
              allow-create 
              clearable
              style="width: 100%"
            >
              <el-option label="清空分组" value="" />
              <el-option 
                v-for="g in groupList" 
                :key="g" 
                :label="g" 
                :value="g" 
              />
            </el-select>
          </el-form-item>
        </el-form>
        <el-alert 
          type="info" 
          :closable="false" 
          style="margin-top: 12px;"
        >
          <template #default>
            <div style="font-size: 13px;">
              • 选择“清空分组”将移除节点的分组标签<br/>
              • 可直接输入新分组名称，系统会自动创建
            </div>
          </template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="batchGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchSetGroup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, onBeforeUnmount, watch, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, Check, Close, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const proxyPoolList = ref([])
const selectedRows = ref([])
const tableRef = ref(null)
const batchGroupName = ref('')

// 常用国家列表（内置）
const builtInCountries = [
  '中国',
  '美国',
  '日本',
  '韩国',
  '新加坡',
  '香港',
  '台湾',
  '英国',
  '德国',
  '法国',
  '加拿大',
  '澳大利亚',
  '俄罗斯',
  '印度',
  '泰国',
  '越南',
  '马来西亚',
  '印度尼西亚',
  '菲律宾',
  '巴西',
  '墨西哥',
  '荷兰',
  '瑞士',
  '瑞典',
  '西班牙',
  '意大利',
  '土耳其',
  '波兰',
  '乌克兰',
  '南非'
]

// 搜索和分组数据
const searchForm = reactive({
  keyword: '',
  country: '',
  groupName: ''
})
const countryList = ref([...builtInCountries]) // 初始化为内置列表
const groupList = ref([])

// 自动刷新定时器
let autoRefreshTimer = null
const AUTO_REFRESH_INTERVAL = 10000 // 10秒自动刷新一次

const dialogVisible = ref(false)
const dialogTitle = ref('添加代理节点')
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
  country: '',
  groupName: '',
  description: ''
})
const formRules = {
  poolName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  proxyIp: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  proxyPort: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  proxyType: [{ required: true, message: '请选择代理类型', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { 
      validator: (rule, value, callback) => {
        // 编辑模式下密码可以为空（不修改密码）
        // 添加模式下，如果需要认证则必填
        if (!formData.id && needAuth.value && (!value || !value.trim())) {
          callback(new Error('请输入密码'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  country: [{ required: true, message: '请输入国家', trigger: 'blur' }],
  description: [{ max: 500, message: '描述长度不能超过500', trigger: 'blur' }]
}

// 监听认证开关变化
watch(needAuth, (val) => {
  formData.needAuth = val ? 1 : 0
})

// 启动自动刷新
const startAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
  }
  autoRefreshTimer = setInterval(() => {
    loadData(true) // 静默刷新，不显示Loading
  }, AUTO_REFRESH_INTERVAL)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

onMounted(() => {
  loadData()
  loadCountriesAndGroups()
  startAutoRefresh() // 启动自动刷新
})

onActivated(() => {
  loadData()
  loadCountriesAndGroups()
  startAutoRefresh() // 重新启动自动刷新
})

onBeforeUnmount(() => {
  stopAutoRefresh() // 组件销毁时清除定时器
})

const loadData = async (silent = false) => {
  if (!silent) {
    loading.value = true
  }
  try {
    const params = {}
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.country) params.country = searchForm.country
    if (searchForm.groupName) params.groupName = searchForm.groupName
    
    const res = await request.get('/proxy/pool/list', { params })
    if (res.code === 200) {
      proxyPoolList.value = res.data || []
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error('获取代理池列表失败')
    }
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

// 加载国家和分组列表
const loadCountriesAndGroups = async () => {
  try {
    const [countriesRes, groupsRes] = await Promise.all([
      request.get('/proxy/countries'),
      request.get('/proxy/groups')
    ])
    
    // 合并后端返回的国家与内置国家列表
    if (countriesRes.code === 200) {
      const backendCountries = countriesRes.data || []
      // 去重合并：内置 + 后端返回
      const allCountries = [...new Set([...builtInCountries, ...backendCountries])]
      countryList.value = allCountries.sort()
    } else {
      // 如果后端请求失败，使用内置列表
      countryList.value = [...builtInCountries]
    }
    
    if (groupsRes.code === 200) {
      groupList.value = groupsRes.data || []
    }
  } catch (error) {
    console.error('加载国家/分组列表失败', error)
    // 失败时使用内置国家列表
    countryList.value = [...builtInCountries]
  }
}

// 刷新数据
const handleRefresh = () => {
  loadData()
  loadCountriesAndGroups()
  ElMessage.success('刷新成功')
}

// 搜索
const handleSearch = () => {
  loadData()
}

// 重置搜索
const handleResetSearch = () => {
  searchForm.keyword = ''
  searchForm.country = ''
  searchForm.groupName = ''
  loadData()
}

const getProxyTypeText = (type) => {
  const map = { 1: 'HTTP', 2: 'HTTPS', 3: 'SOCKS5' }
  return map[type] || '未知'
}

const getStatusText = (status) => {
  const map = { 1: '正常', 2: '异常', 3: '检测中' }
  return map[status] || '未知'
}

const getStatusColor = (status) => {
  const map = { 
    1: '#67C23A',  // 绿色 - 正常
    2: '#F56C6C',  // 红色 - 异常
    3: '#E6A23C'   // 黄色 - 检测中
  }
  return map[status] || '#909399'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟：显示“刚刚”
  if (diff < 60000) return '刚刚'
  // 小于1小时：显示“X分钟前”
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  // 小于24小时：显示“X小时前”
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  // 超过24小时：显示精简日期格式 "MM-DD HH:mm"
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  
  return `${month}-${day} ${hour}:${minute}`
}

const handleAddPool = () => {
  dialogTitle.value = '添加代理节点'
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
    country: '',
    groupName: '',
    description: ''
  })
  formRef.value?.clearValidate()
}

const handleEditPool = (row) => {
  dialogTitle.value = '编辑代理节点'
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
    country: row.country || '',
    groupName: row.groupName || '',
    description: row.description
  })
  formRef.value?.clearValidate()
}

const handleDeletePool = async (row) => {
  await ElMessageBox.confirm('确定要删除该代理节点吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await request.delete(`/proxy/pool/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
    loadCountriesAndGroups()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

// 管理分组
 const handleManageGroups = () => {
  groupManageDialogVisible.value = true
  loadGroupsData()
}

// 分组管理相关数据
const groupManageDialogVisible = ref(false)
const groupsData = ref([])
const groupsLoading = ref(false)

// 加载分组数据（带节点数量统计）
const loadGroupsData = async () => {
  groupsLoading.value = true
  try {
    const res = await request.get('/proxy/groups/detail')
    if (res.code === 200) {
      const backendGroups = res.data || []
      
      // 合并后端返回的分组和前端创建的空分组
      // 保留所有在groupList中但没有节点的分组
      const allGroups = [...backendGroups]
      
      // 添加在groupList中存在但后端没有返回的分组（nodeCount=0的分组）
      groupList.value.forEach(groupName => {
        if (!backendGroups.some(g => g.groupName === groupName)) {
          allGroups.push({
            groupName: groupName,
            nodeCount: 0
          })
        }
      })
      
      // 排序
      groupsData.value = allGroups.sort((a, b) => a.groupName.localeCompare(b.groupName))
    } else {
      ElMessage.error(res.message || '获取分组数据失败')
    }
  } catch (error) {
    console.error('加载分组数据失败', error)
    ElMessage.error('获取分组数据失败')
  } finally {
    groupsLoading.value = false
  }
}

// 创建分组
const handleCreateGroup = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新分组名称', '创建分组', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入分组名称',
      inputValidator: (value) => {
        if (!value || !value.trim()) {
          return '分组名称不能为空'
        }
        // 检查是否已存在（在groupList中检查，包含所有实际存在的分组）
        if (groupList.value.includes(value.trim())) {
          return '该分组已存在'
        }
        return true
      }
    })
    
    if (value && value.trim()) {
      const newGroupName = value.trim()
      
      // 添加到分组下拉列表（这是关键！）
      if (!groupList.value.includes(newGroupName)) {
        groupList.value.push(newGroupName)
        groupList.value.sort()
      }
      
      // 同时添加到分组管理显示列表（即使nodeCount为0也显示）
      if (!groupsData.value.some(g => g.groupName === newGroupName)) {
        groupsData.value.push({
          groupName: newGroupName,
          nodeCount: 0
        })
        // 重新排序
        groupsData.value.sort((a, b) => a.groupName.localeCompare(b.groupName))
      }
      
      ElMessage.success({
        message: `分组 "${newGroupName}" 创建成功，请选择节点并批量加入此分组`,
        duration: 4000
      })
      
      // 不关闭对话框，让用户可以继续创建或查看
      // groupManageDialogVisible.value = false
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建分组失败', error)
      ElMessage.error('创建失败')
    }
  }
}

// 重命名分组
const handleRenameGroup = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的分组名称', '重命名分组', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.groupName,
      inputPlaceholder: '请输入新的分组名称',
      inputValidator: (value) => {
        if (!value || !value.trim()) {
          return '分组名称不能为空'
        }
        if (value.trim() === row.groupName) {
          return '新名称与原名称相同'
        }
        // 检查是否已存在
        if (groupsData.value.some(g => g.groupName === value.trim())) {
          return '该分组已存在'
        }
        return true
      }
    })
    
    if (value && value.trim() && value.trim() !== row.groupName) {
      const res = await request.put('/proxy/groups/rename', null, {
        params: {
          oldName: row.groupName,
          newName: value.trim()
        }
      })
      
      if (res.code === 200) {
        ElMessage.success(`分组重命名成功: "${row.groupName}" → "${value.trim()}"`)
        loadGroupsData()
        loadCountriesAndGroups()
        loadData() // 刷新主列表，显示更新后的分组名
      } else {
        ElMessage.error(res.message || '重命名失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重命名分组失败', error)
      ElMessage.error(error.response?.data?.message || error.message || '重命名失败')
    }
  }
}

// 删除分组
const handleDeleteGroup = async (row) => {
  try {
    // 如果分组下有节点，需要二次确认
    if (row.nodeCount > 0) {
      await ElMessageBox.confirm(
        `分组"${row.groupName}"下有 ${row.nodeCount} 个节点，删除后这些节点的分组将被清空。确定要删除吗？`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true
        }
      )
    } else {
      await ElMessageBox.confirm(
        `确定要删除分组"${row.groupName}"吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    }
    
    const res = await request.delete('/proxy/groups', {
      params: { groupName: row.groupName }
    })
    
    if (res.code === 200) {
      ElMessage.success(`分组"${row.groupName}"删除成功`)
      loadGroupsData()
      loadCountriesAndGroups()
      loadData() // 刷新主列表
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除分组失败', error)
      ElMessage.error(error.response?.data?.message || error.message || '删除失败')
    }
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
    loadCountriesAndGroups()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '操作失败')
  }
}

// 一键识别相关
const recognizeDialogVisible = ref(false)
const recognizeConfig = ref('')
const parsedConfig = ref(null)
const recognizeLoading = ref(false)
const addLoading = ref(false)

const handleQuickRecognize = () => {
  recognizeDialogVisible.value = true
  recognizeConfig.value = ''
  parsedConfig.value = null
}

const handleParseConfig = async () => {
  if (!recognizeConfig.value.trim()) {
    ElMessage.warning('请输入代理配置')
    return
  }
  
  recognizeLoading.value = true
  try {
    // 调用后端解析接口（不测试，只解析）
    const res = await request.post('/proxy/parse/config', null, {
      params: { configStr: recognizeConfig.value.trim() }
    })
    
    if (res.code === 200 && res.data) {
      parsedConfig.value = res.data
      ElMessage.success('✓ 识别成功！')
    } else {
      ElMessage.error(res.message || '识别失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '识别失败')
    parsedConfig.value = null
  } finally {
    recognizeLoading.value = false
  }
}

const handleAddFromRecognize = async () => {
  if (!parsedConfig.value) {
    return
  }
  
  addLoading.value = true
  try {
    const poolName = parsedConfig.value.label || `${parsedConfig.value.protocol}-${parsedConfig.value.host}`
    
    const payload = {
      poolName: poolName,
      proxyIp: parsedConfig.value.host,
      proxyPort: parsedConfig.value.port,
      proxyType: parsedConfig.value.proxyType,
      needAuth: parsedConfig.value.hasAuth ? 1 : 0,
      username: parsedConfig.value.username || '',
      password: parsedConfig.value.password || '',
      country: '',  // 默认为空，后续可通过“国家”按钮设置
      groupName: '',  // 默认为空，后续可通过“分组”按钮设置
      description: `通过一键识别添加: ${recognizeConfig.value}`
    }
    
    await request.post('/proxy/pool', payload)
    ElMessage.success('✓ 代理节点添加成功！')
    recognizeDialogVisible.value = false
    loadData()
    loadCountriesAndGroups()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '添加失败')
  } finally {
    addLoading.value = false
  }
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 清空选择
const handleClearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

// 批量设置分组对话框显示状态
const batchGroupDialogVisible = ref(false)
// 保存要批量操作的节点ID列表（防止刷新后丢失）
const batchSelectedIds = ref([])

// 批量设置分组
const handleBatchSetGroup = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要设置分组的节点')
    return
  }
  
  console.log('🔍 [批量设置分组] 开始操作')
  console.log('  ├─ 选中节点数量:', selectedRows.value.length)
  console.log('  ├─ 选中节点ID:', selectedRows.value.map(r => r.id))
  console.log('  └─ 选中节点名称:', selectedRows.value.map(r => r.poolName))
  
  // 保存选中的节点ID（关键！）
  batchSelectedIds.value = selectedRows.value.map(row => row.id)
  
  console.log('  ✓ 已保存待操作节点ID:', batchSelectedIds.value)
  
  // 重置分组名称
  batchGroupName.value = ''
  // 显示对话框
  batchGroupDialogVisible.value = true
}

// 确认批量设置分组
const confirmBatchSetGroup = async () => {
  console.log('🔍 [确认批量设置] 开始执行')
  console.log('  ├─ 待操作节点ID:', batchSelectedIds.value)
  console.log('  ├─ 目标分组名称:', batchGroupName.value || '(清空分组)')
  console.log('  └─ 当前选中行数:', selectedRows.value.length)
  
  if (!batchSelectedIds.value || batchSelectedIds.value.length === 0) {
    ElMessage.error('没有要操作的节点')
    return
  }
  
  try {
    console.log('  → 发送批量更新请求...')
    
    // 批量更新分组
    const res = await request.put('/proxy/pool/batch/group', {
      ids: batchSelectedIds.value,
      groupName: batchGroupName.value || ''
    })
    
    console.log('  ← 服务器响应:', res)
    
    if (res.code === 200) {
      // 明确提示设置到哪个分组
      const groupNameDisplay = batchGroupName.value ? `"${batchGroupName.value}"` : '(已清空分组)'
      ElMessage.success({
        message: `成功将 ${batchSelectedIds.value.length} 个节点设置到分组 ${groupNameDisplay}`,
        duration: 3000
      })
      
      console.log('  ✓ 批量设置成功')
      
      // 关闭对话框
      batchGroupDialogVisible.value = false
      
      console.log('  → 开始刷新数据...')
      
      // 刷新数据
      await loadData()
      await loadCountriesAndGroups()
      
      console.log('  → 恢复选中状态...')
      
      // 重新选中之前选中的行
      await restoreSelection(batchSelectedIds.value)
      
      console.log('  ✓ 操作完成')
      
      batchGroupName.value = '' // 重置选择
      // 清空保存的ID列表
      // batchSelectedIds.value = [] // 暂时不清空，方便调试
    } else {
      console.error('  ✗ 批量设置失败:', res.message)
      ElMessage.error(res.message || '批量设置失败')
    }
  } catch (error) {
    console.error('  ✗ 批量设置异常:', error)
    ElMessage.error(error.response?.data?.message || error.message || '批量设置失败')
  }
}

// 恢复选中状态
const restoreSelection = async (selectedIds) => {
  console.log('🔍 [恢复选中] 开始恢复选中状态')
  console.log('  ├─ 目标节点ID:', selectedIds)
  console.log('  ├─ 当前表格数据数量:', proxyPoolList.value.length)
  
  // 等待DOM更新
  await new Promise(resolve => setTimeout(resolve, 200))
  
  console.log('  → 开始匹配并选中节点...')
  
  let successCount = 0
  // 根据ID重新选中行
  proxyPoolList.value.forEach(row => {
    if (selectedIds.includes(row.id)) {
      console.log('    ├─ 选中节点:', row.id, row.poolName)
      tableRef.value?.toggleRowSelection(row, true)
      successCount++
    }
  })
  
  console.log('  ✓ 恢复完成，成功选中:', successCount, '/', selectedIds.length)
  
  // 验证选中状态
  setTimeout(() => {
    console.log('  ✓ 验证：当前选中节点数量:', selectedRows.value.length)
    if (selectedRows.value.length !== selectedIds.length) {
      console.warn('  ⚠️ 警告：选中数量不匹配！')
      console.warn('    ├─ 期望:', selectedIds.length)
      console.warn('    └─ 实际:', selectedRows.value.length)
    }
  }, 100)
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
