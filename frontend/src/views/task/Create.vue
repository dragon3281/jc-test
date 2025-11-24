<template>
  <div class="create-task-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">新建检测任务</div>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="基础配置" />
        <el-step title="数据源" />
        <el-step title="资源配置" />
        <el-step title="确认提交" />
      </el-steps>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="margin-top: 30px; max-width: 800px"
      >
        <!-- 步骤1: 基础配置 -->
        <div v-show="currentStep === 0">
          <el-form-item label="任务名称" prop="taskName">
            <el-input v-model="form.taskName" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="目标站" prop="targetSite">
            <el-input v-model="form.targetSite" placeholder="请输入目标网站" />
          </el-form-item>
          <el-form-item label="POST模板" prop="templateId">
            <el-select v-model="form.templateId" placeholder="请选择POST模板" style="width: 100%">
              <el-option
                v-for="item in templates"
                :key="item.id"
                :label="item.templateName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <!-- 步骤2: 数据源 -->
        <div v-show="currentStep === 1">
          <el-alert
            title="提示"
            type="info"
            description="系统将自动使用'基础数据'中的所有数据作为检测源"
            :closable="false"
            style="margin-bottom: 20px"
          />
          <el-form-item label="数据总量">
            <el-statistic :value="dataCount" />
          </el-form-item>
        </div>

        <!-- 步骤3: 资源配置 -->
        <div v-show="currentStep === 2">
          <el-form-item label="代理池" prop="poolId">
            <el-select v-model="form.poolId" placeholder="请选择代理池(可选)" clearable style="width: 100%">
              <el-option
                v-for="item in proxyPools"
                :key="item.id"
                :label="`${item.poolName} (${item.totalCount}个代理)`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="执行服务器" prop="serverIds">
            <el-select
              v-model="form.serverIds"
              placeholder="请选择执行服务器"
              multiple
              style="width: 100%"
            >
              <el-option
                v-for="item in servers"
                :key="item.id"
                :label="`${item.serverName} (${item.ipAddress})`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="并发数" prop="concurrentNum">
            <el-input-number v-model="form.concurrentNum" :min="1" :max="100" />
            <el-text type="info" size="small" style="margin-left: 10px">建议: 10-50</el-text>
          </el-form-item>
          <el-form-item label="失败重试" prop="retryCount">
            <el-input-number v-model="form.retryCount" :min="0" :max="5" />
          </el-form-item>
          <el-form-item label="任务优先级" prop="priority">
            <el-radio-group v-model="form.priority">
              <el-radio :label="1">高</el-radio>
              <el-radio :label="2">中</el-radio>
              <el-radio :label="3">低</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <!-- 步骤4: 确认提交 -->
        <div v-show="currentStep === 3">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="任务名称">{{ form.taskName }}</el-descriptions-item>
            <el-descriptions-item label="目标站">{{ form.targetSite }}</el-descriptions-item>
            <el-descriptions-item label="POST模板">
              {{ getTemplateName(form.templateId) }}
            </el-descriptions-item>
            <el-descriptions-item label="数据总量">{{ dataCount }} 条</el-descriptions-item>
            <el-descriptions-item label="代理池">
              {{ form.poolId ? getPoolName(form.poolId) : '不使用代理' }}
            </el-descriptions-item>
            <el-descriptions-item label="执行服务器">
              {{ getServerNames(form.serverIds) }}
            </el-descriptions-item>
            <el-descriptions-item label="并发数">{{ form.concurrentNum }}</el-descriptions-item>
            <el-descriptions-item label="失败重试">{{ form.retryCount }} 次</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="getPriorityType(form.priority)">
                {{ getPriorityText(form.priority) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 步骤按钮 -->
        <el-form-item style="margin-top: 30px">
          <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
          <el-button v-if="currentStep < 3" type="primary" @click="nextStep">下一步</el-button>
          <el-button v-if="currentStep === 3" type="success" @click="handleSubmit" :loading="submitLoading">
            创建任务
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const currentStep = ref(0)
const submitLoading = ref(false)
const templates = ref([])
const proxyPools = ref([])
const servers = ref([])
const dataCount = ref(0)

const form = reactive({
  taskName: '',
  targetSite: '',
  templateId: null,
  poolId: null,
  serverIds: [],
  concurrentNum: 20,
  retryCount: 3,
  priority: 2
})

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  targetSite: [{ required: true, message: '请输入目标站', trigger: 'blur' }],
  templateId: [{ required: true, message: '请选择POST模板', trigger: 'change' }],
  serverIds: [{ required: true, message: '请选择执行服务器', trigger: 'change' }],
  concurrentNum: [{ required: true, message: '请输入并发数', trigger: 'blur' }]
}

const nextStep = async () => {
  if (currentStep.value === 0) {
    await formRef.value.validateField(['taskName', 'targetSite', 'templateId'])
  } else if (currentStep.value === 2) {
    await formRef.value.validateField(['serverIds', 'concurrentNum'])
  }
  currentStep.value++
}

const prevStep = () => {
  currentStep.value--
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    const res = await request.post('/task', form)
    if (res.code === 200) {
      ElMessage.success('创建任务成功')
      router.push('/task/list')
    }
  } catch (error) {
    ElMessage.error('创建任务失败')
  } finally {
    submitLoading.value = false
  }
}

const handleCancel = () => {
  router.back()
}

const getTemplateName = (id) => {
  return templates.value.find(t => t.id === id)?.templateName || ''
}

const getPoolName = (id) => {
  return proxyPools.value.find(p => p.id === id)?.poolName || ''
}

const getServerNames = (ids) => {
  return servers.value.filter(s => ids.includes(s.id)).map(s => s.serverName).join(', ')
}

const getPriorityText = (priority) => {
  const map = { 1: '高', 2: '中', 3: '低' }
  return map[priority]
}

const getPriorityType = (priority) => {
  const map = { 1: 'danger', 2: '', 3: 'info' }
  return map[priority]
}

onMounted(async () => {
  await Promise.all([
    fetchTemplates(),
    fetchProxyPools(),
    fetchServers(),
    fetchDataCount()
  ])
})

async function fetchTemplates() {
  const res = await request.get('/template/list')
  if (res.code === 200) templates.value = res.data
}

async function fetchProxyPools() {
  const res = await request.get('/proxy/pool/list')
  if (res.code === 200) proxyPools.value = res.data
}

async function fetchServers() {
  const res = await request.get('/server/list')
  if (res.code === 200) servers.value = res.data.filter(s => s.status === 1)
}

async function fetchDataCount() {
  const res = await request.get('/data/base/page', { params: { current: 1, size: 1 } })
  if (res.code === 200) dataCount.value = res.data.total
}
</script>

<style scoped>
.create-task-container {
  padding: 20px;
}
</style>
