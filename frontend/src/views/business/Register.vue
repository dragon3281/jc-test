<template>
  <div class="register-container">
    <el-card shadow="hover">
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">自动化注册任务</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="showCreateDialog = true">
              <el-icon><Plus /></el-icon>
              新建注册任务
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" :model="queryParams" class="query-bar">
        <el-form-item label="任务名称">
          <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable />
        </el-form-item>
        <el-form-item label="执行状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="待执行" :value="1" />
            <el-option label="执行中" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已暂停" :value="4" />
            <el-option label="失败" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 任务列表 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="websiteUrl" label="目标网站" min-width="180" show-overflow-tooltip />
        <el-table-column label="注册进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="getProgress(row)" :color="getProgressColor(row.status)" :status="row.status === 5 ? 'exception' : null" />
            <div class="progress-text">{{ row.completedCount || 0 }} / {{ row.totalCount || 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="成功率" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.successCount > 0" type="success">{{ getSuccessRate(row) }}%</el-tag>
            <el-tag v-else type="info">0%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="info">待执行</el-tag>
            <el-tag v-else-if="row.status === 2" type="warning">执行中</el-tag>
            <el-tag v-else-if="row.status === 3" type="success">已完成</el-tag>
            <el-tag v-else-if="row.status === 4">已暂停</el-tag>
            <el-tag v-else-if="row.status === 5" type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="success" @click="handleStart(row)">启动</el-button>
            <el-button v-if="row.status === 2" link type="warning" @click="handlePause(row)">暂停</el-button>
            <el-button v-if="row.status === 4" link type="primary" @click="handleResume(row)">继续</el-button>
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="table-footer">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新建注册任务对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建注册任务" width="800px" @close="resetForm">
      <el-steps :active="currentStep" finish-status="success" align-center class="steps">
        <el-step title="基础配置" />
        <el-step title="加密配置" />
        <el-step title="执行配置" />
      </el-steps>

      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="130px" class="form-content">
        <!-- 步骤1: 基础配置 -->
        <div v-show="currentStep === 0">
          <el-form-item label="任务名称" prop="taskName">
            <el-input v-model="registerForm.taskName" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="目标网站" prop="websiteUrl">
            <el-input v-model="registerForm.websiteUrl" placeholder="https://www.wwwtk666.com" />
            <div class="form-tip">网站首页地址</div>
          </el-form-item>
          <el-form-item label="注册接口" prop="registerApi">
            <el-input v-model="registerForm.registerApi" placeholder="/wps/member/register" />
            <div class="form-tip">注册接口的路径</div>
          </el-form-item>
          <el-form-item label="请求方法" prop="method">
            <el-radio-group v-model="registerForm.method">
              <el-radio label="POST">POST</el-radio>
              <el-radio label="PUT">PUT</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="用户名字段" prop="usernameField">
            <el-input v-model="registerForm.usernameField" placeholder="username" />
            <div class="form-tip">注册请求中的用户名字段名</div>
          </el-form-item>
          <el-form-item label="密码字段" prop="passwordField">
            <el-input v-model="registerForm.passwordField" placeholder="password" />
            <div class="form-tip">注册请求中的密码字段名</div>
          </el-form-item>
          <el-form-item label="默认密码" prop="defaultPassword">
            <el-input v-model="registerForm.defaultPassword" placeholder="133adb" />
            <div class="form-tip">所有账号使用的相同密码</div>
          </el-form-item>
          <el-form-item label="额外参数">
            <el-input
              v-model="registerForm.extraParams"
              type="textarea"
              :rows="3"
              placeholder='{"affiliateCode":"www","domain":"www-tk999","login":true,"registerMethod":"WEB"}'
            />
            <div class="form-tip">JSON格式，其他需要提交的字段</div>
          </el-form-item>
        </div>

        <!-- 步骤2: 加密配置 -->
        <div v-show="currentStep === 1">
          <el-form-item label="加密类型" prop="encryptionType">
            <el-radio-group v-model="registerForm.encryptionType">
              <el-radio label="NONE">无加密</el-radio>
              <el-radio label="DES_RSA">DES+RSA双重加密</el-radio>
            </el-radio-group>
            <div class="form-tip">根据目标网站的加密方式选择</div>
          </el-form-item>
          <template v-if="registerForm.encryptionType === 'DES_RSA'">
            <el-form-item label="RSA密钥接口" prop="rsaKeyApi">
              <el-input v-model="registerForm.rsaKeyApi" placeholder="/wps/session/key/rsa" />
              <div class="form-tip">获取RSA公钥的接口地址</div>
            </el-form-item>
            <el-form-item label="时间戳参数">
              <el-input v-model="registerForm.rsaTsParam" placeholder="t" />
              <div class="form-tip">RSA接口时间戳参数名，默认为t</div>
            </el-form-item>
            <el-form-item label="加密请求头">
              <el-input v-model="registerForm.encryptionHeader" placeholder="encryption" />
              <div class="form-tip">RSA加密后的密钥放在哪个请求头</div>
            </el-form-item>
            <el-form-item label="数据包装字段">
              <el-input v-model="registerForm.valueFieldName" placeholder="value" />
              <div class="form-tip">加密数据包装的字段名，例如{"value":"加密内容"}</div>
            </el-form-item>
            <el-form-item label="重复用户名提示">
              <el-input v-model="registerForm.dupMsgSubstring" placeholder="Ang username na ito ay ginamit na" />
              <div class="form-tip">用于验证注册成功的重复用户名提示文本</div>
            </el-form-item>
          </template>
        </div>

        <!-- 步骤3: 执行配置 -->
        <div v-show="currentStep === 2">
          <el-form-item label="创建数量" prop="accountCount">
            <el-input-number v-model="registerForm.accountCount" :min="1" :max="1000" />
            <div class="form-tip">本次任务要创建的账号数量</div>
          </el-form-item>
          <el-form-item label="并发数" prop="concurrency">
            <el-input-number v-model="registerForm.concurrency" :min="1" :max="20" />
            <div class="form-tip">同时执行的注册任务数量</div>
          </el-form-item>
          <el-form-item label="需要手机号">
            <el-switch v-model="registerForm.needPhone" />
          </el-form-item>
          <el-form-item v-if="registerForm.needPhone" label="手机号">
            <el-input v-model="registerForm.manualPhone" placeholder="请输入手机号（选填，不填则自动生成）" />
          </el-form-item>
          <el-form-item label="使用代理">
            <el-switch v-model="registerForm.useProxy" />
          </el-form-item>
          <el-form-item v-if="registerForm.useProxy" label="代理池" prop="proxyPoolId">
            <el-select v-model="registerForm.proxyPoolId" placeholder="请选择代理池">
              <el-option v-for="item in proxyPools" :key="item.id" :label="item.poolName" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
          <el-button v-if="currentStep < 2" type="primary" @click="nextStep">下一步</el-button>
          <el-button v-if="currentStep === 2" type="primary" @click="handleSubmit" :loading="submitLoading">提交并启动</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="1000px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border v-if="currentRow">
            <el-descriptions-item label="任务名称">{{ currentRow.taskName }}</el-descriptions-item>
            <el-descriptions-item label="目标网站">{{ currentRow.websiteUrl }}</el-descriptions-item>
            <el-descriptions-item label="加密类型">{{ currentRow.encryptionType || 'NONE' }}</el-descriptions-item>
            <el-descriptions-item label="创建数量">{{ currentRow.accountCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总数量">{{ currentRow.totalCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已完成">{{ currentRow.completedCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="成功数">{{ currentRow.successCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="失败数">{{ currentRow.failCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="成功率">{{ getSuccessRate(currentRow) }}%</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="currentRow.status === 1" type="info">待执行</el-tag>
              <el-tag v-else-if="currentRow.status === 2" type="warning">执行中</el-tag>
              <el-tag v-else-if="currentRow.status === 3" type="success">已完成</el-tag>
              <el-tag v-else-if="currentRow.status === 4">已暂停</el-tag>
              <el-tag v-else-if="currentRow.status === 5" type="danger">失败</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentRow.createTime }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ currentRow.endTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="注册结果" name="results">
          <div style="margin-bottom: 10px">
            <el-button type="success" size="small" @click="exportResults">导出结果</el-button>
          </div>
          <el-table :data="registerResults" border stripe max-height="500">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="username" label="账号" min-width="120" show-overflow-tooltip />
            <el-table-column prop="password" label="密码" min-width="100" show-overflow-tooltip />
            <el-table-column prop="token" label="Token" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.token">{{ row.token }}</span>
                <span v-else style="color: #909399">-</span>
              </template>
            </el-table-column>
            <el-table-column label="注册状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" type="success">成功</el-tag>
                <el-tag v-else type="danger">失败</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="返回信息" min-width="180" show-overflow-tooltip />
            <el-table-column prop="registerTime" label="注册时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.registerTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const queryParams = reactive({
  taskName: '',
  status: null,
  pageNum: 1,
  pageSize: 10
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const showCreateDialog = ref(false)
const detailVisible = ref(false)
const currentRow = ref(null)
const submitLoading = ref(false)
const activeTab = ref('basic')
const currentStep = ref(0)
const registerFormRef = ref(null)

const registerForm = reactive({
  taskName: '',
  websiteUrl: '',
  registerApi: '',
  method: 'PUT',
  usernameField: 'username',
  passwordField: 'password',
  defaultPassword: '133adb',
  extraParams: '',
  encryptionType: 'DES_RSA',
  rsaKeyApi: '/wps/session/key/rsa',
  rsaTsParam: 't',
  encryptionHeader: 'encryption',
  valueFieldName: 'value',
  dupMsgSubstring: 'Ang username na ito ay ginamit na ng ibang user',
  useProxy: false,
  proxyPoolId: null,
  concurrency: 5,
  needPhone: false,
  manualPhone: '',
  accountCount: 50
})

const registerRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  websiteUrl: [
    { required: true, message: '请输入目标网站', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ],
  registerApi: [{ required: true, message: '请输入注册接口', trigger: 'blur' }],
  usernameField: [{ required: true, message: '请输入用户名字段', trigger: 'blur' }],
  passwordField: [{ required: true, message: '请输入密码字段', trigger: 'blur' }],
  defaultPassword: [{ required: true, message: '请输入默认密码', trigger: 'blur' }],
  accountCount: [{ required: true, message: '请设置创建数量', trigger: 'change' }]
}

const proxyPools = ref([])
const registerResults = ref([])
let timer = null

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/business/register/list', { params: queryParams })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.taskName = ''
  queryParams.status = null
  queryParams.pageNum = 1
  fetchData()
}

const fetchProxyPools = async () => {
  try {
    const res = await request.get('/proxy/pool/list')
    proxyPools.value = res.data || []
  } catch (error) {
    console.error('获取代理池失败', error)
  }
}

const nextStep = async () => {
  try {
    await registerFormRef.value.validate()
    currentStep.value++
  } catch (error) {
    console.log('验证失败', error)
  }
}

const handleSubmit = async () => {
  await registerFormRef.value.validate()
  submitLoading.value = true
  try {
    await request.post('/business/register/create', registerForm)
    ElMessage.success('注册任务已创建并启动')
    showCreateDialog.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '创建任务失败')
  } finally {
    submitLoading.value = false
  }
}

const handleStart = async (row) => {
  try {
    await request.post(`/business/register/start/${row.id}`)
    ElMessage.success('任务已启动')
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '启动失败')
  }
}

const handlePause = async (row) => {
  try {
    await request.post(`/business/register/pause/${row.id}`)
    ElMessage.success('任务已暂停')
    fetchData()
  } catch (error) {
    ElMessage.error('暂停失败')
  }
}

const handleResume = async (row) => {
  try {
    await request.post(`/business/register/resume/${row.id}`)
    ElMessage.success('任务已继续')
    fetchData()
  } catch (error) {
    ElMessage.error('继续失败')
  }
}

const handleViewDetail = async (row) => {
  try {
    const res = await request.get(`/business/register/${row.id}`)
    currentRow.value = res.data
    const resultRes = await request.get(`/business/register/results/${row.id}`)
    registerResults.value = resultRes.data || []
    activeTab.value = 'basic'
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该注册任务吗?', '提示', { type: 'warning' })
    await request.delete(`/business/register/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const exportResults = () => {
  if (!registerResults.value || registerResults.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  // 只导出token值，每行一个
  let content = ''
  registerResults.value.forEach(item => {
    if (item.token) {
      content += `${item.token}\n`
    }
  })
  if (!content) {
    ElMessage.warning('没有可用的Token')
    return
  }
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `tokens_${currentRow.value.taskName}_${new Date().getTime()}.txt`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

const getProgress = (row) => {
  if (!row.totalCount) return 0
  return Math.round((row.completedCount / row.totalCount) * 100)
}

const getProgressColor = (status) => {
  const colors = { 1: '#909399', 2: '#E6A23C', 3: '#67C23A', 4: '#909399', 5: '#F56C6C' }
  return colors[status] || '#409EFF'
}

const getSuccessRate = (row) => {
  if (!row || !row.totalCount) return 0
  return ((row.successCount / row.totalCount) * 100).toFixed(2)
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  // 统一按北京时间显示到年月日小时分
  try {
    // 如果无时区信息，按UTC解析再转本地（避免旧数据少8小时）
    const src = /Z$|[+-]\d{2}:?\d{2}$/.test(timeStr) ? timeStr : (timeStr + 'Z')
    const d = new Date(src)
    if (isNaN(d.getTime())) return timeStr
    const Y = d.getFullYear()
    const M = String(d.getMonth() + 1).padStart(2, '0')
    const D = String(d.getDate()).padStart(2, '0')
    const h = String(d.getHours()).padStart(2, '0')
    const m = String(d.getMinutes()).padStart(2, '0')
    return `${Y}-${M}-${D} ${h}:${m}`
  } catch (e) {
    return timeStr
  }
}

const resetForm = () => {
  currentStep.value = 0
  Object.assign(registerForm, {
    taskName: '',
    websiteUrl: '',
    registerApi: '',
    method: 'PUT',
    usernameField: 'username',
    passwordField: 'password',
    defaultPassword: '133adb',
    extraParams: '',
    encryptionType: 'DES_RSA',
    rsaKeyApi: '/wps/session/key/rsa',
    rsaTsParam: 't',
    encryptionHeader: 'encryption',
    valueFieldName: 'value',
    dupMsgSubstring: 'Ang username na ito ay ginamit na ng ibang user',
    useProxy: false,
    proxyPoolId: null,
    concurrency: 5,
    needPhone: false,
    manualPhone: '',
    accountCount: 50
  })
  registerFormRef.value?.clearValidate()
}

onMounted(() => {
  fetchData()
  fetchProxyPools()
  timer = setInterval(() => { if (document.visibilityState === 'visible') fetchData() }, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.register-container { padding: 20px; }
.progress-text { font-size: 12px; color: #909399; margin-top: 5px; text-align: center; }
.steps { margin-bottom: 30px; }
.form-content { min-height: 300px; margin-top: 20px; }
.form-tip { font-size: 12px; color: #909399; margin-top: 5px; }
.dialog-footer { display: flex; justify-content: center; gap: 10px; }
</style>
