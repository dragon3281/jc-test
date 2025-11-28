<template>
  <div class="analysis-register-container">
    <el-card shadow="hover">
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">自动化注册分析</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="showAnalyzeDialog = true">
              <el-icon><Plus /></el-icon>
              新建分析
            </el-button>
            <el-button type="danger" :disabled="multipleSelection.length === 0" @click="handleBatchDelete">
              <el-icon><Delete /></el-icon>
              批量删除
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" :model="queryParams" class="query-bar">
        <el-form-item label="网站地址">
          <el-input v-model="queryParams.websiteUrl" placeholder="请输入网站地址" clearable />
        </el-form-item>
        <el-form-item label="分析状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="分析中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="失败" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 分析记录列表 -->
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="websiteUrl" label="网站地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.analysisStatus === 1" type="warning">分析中</el-tag>
            <el-tag v-else-if="row.analysisStatus === 2" type="success">已完成</el-tag>
            <el-tag v-else-if="row.analysisStatus === 3" type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
            <!-- 按要求：不需要生成模板按钮 -->
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

    <!-- 新建分析对话框 -->
    <el-dialog v-model="showAnalyzeDialog" title="新建自动化注册分析" width="600px" @close="resetForm">
      <el-form :model="analyzeForm" :rules="analyzeRules" ref="analyzeFormRef" label-width="120px">
        <el-form-item label="网站地址" prop="websiteUrl">
          <el-input v-model="analyzeForm.websiteUrl" placeholder="https://example.com" />
          <div class="form-tip">系统将自动抓取网站信息,分析注册表单、JS文件、加密方式等</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAnalyzeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleStartAnalyze" :loading="submitLoading">开始分析</el-button>
      </template>
    </el-dialog>

    <!-- 分析详情对话框 -->
    <el-dialog v-model="detailVisible" title="分析详情" width="1000px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="网站地址">{{ currentRow.websiteUrl }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentRow.analysisStatus === 1" type="warning">分析中</el-tag>
          <el-tag v-else-if="currentRow.analysisStatus === 2" type="success">已完成</el-tag>
          <el-tag v-else-if="currentRow.analysisStatus === 3" type="danger">失败</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ currentRow.completeTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="currentRow.analysisStatus === 2">
        <!-- 成功状态横幅 -->
        <el-alert 
          v-if="parsedResult && parsedResult.token" 
          type="success" 
          title="✅ 分析成功！已通过实际测试并获取Token" 
          :description="`成功获取Token: ${parsedResult.token.substring(0, 30)}...`"
          show-icon 
          :closable="false" 
          style="margin-bottom: 20px"
        />
        <el-alert 
          v-else-if="parsedResult && !parsedResult.token" 
          type="info" 
          title="📋 分析完成 - 已识别注册逻辑" 
          description="已成功分析网站的注册接口、加密方式和所需参数，但实际注册测试未获取到Token（可能需要验证码或其他验证）"
          show-icon 
          :closable="false" 
          style="margin-bottom: 20px"
        />
        
        <el-divider content-position="left">🔍 智能分析结果</el-divider>
        
        <el-descriptions :column="2" border>
          <el-descriptions-item label="注册接口">
            <el-tag type="success">{{ parsedResult?.registerApi || currentRow.registerApi || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="请求方法">
            <el-tag>{{ parsedResult?.method || currentRow.registerMethod || 'POST' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="加密方式">
            <el-tag v-if="currentRow.encryptionType === 'NONE'" type="info">无加密</el-tag>
            <el-tag v-else-if="currentRow.encryptionType === 'DES_RSA'" type="warning">DES+RSA(老式JS)</el-tag>
            <el-tag v-else-if="currentRow.encryptionType === 'DES_RSA_STANDARD'" type="warning">DES+RSA(标准PKCS1)</el-tag>
            <el-tag v-else-if="currentRow.encryptionType === 'AES_RSA'" type="success">AES+RSA</el-tag>
            <el-tag v-else-if="currentRow.encryptionType === 'MD5'" type="primary">MD5</el-tag>
            <el-tag v-else-if="currentRow.encryptionType === 'BASE64'" type="info">BASE64</el-tag>
            <el-tag v-else>{{ currentRow.encryptionType || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="RSA密钥接口">
            {{ parsedResult?.rsaKeyApi || currentRow.rsaKeyApi || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="加密细节" :span="2" v-if="parsedResult?.encryptionDetails">
            {{ parsedResult.encryptionDetails }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">📝 检测到的必需参数</el-divider>
        <el-descriptions :column="2" border v-if="parsedResult">
          <el-descriptions-item label="所有必需参数" :span="2">
            <el-tag 
              v-for="param in (parsedResult.requiredFields || [])"
              :key="param"
              size="small"
              style="margin: 2px 5px"
            >
              {{ param }}
            </el-tag>
            <span v-if="!parsedResult.requiredFields || parsedResult.requiredFields.length === 0">-</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">🧪 注册测试结果</el-divider>
        <el-descriptions :column="2" border v-if="parsedResult">
          <el-descriptions-item label="测试状态">
            <el-tag :type="parsedResult.testSuccess ? 'success' : 'danger'">
              {{ parsedResult.testSuccess ? '✅ 成功' : '❌ 失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="HTTP状态码">
            <el-tag :type="parsedResult.statusCode === 200 ? 'success' : 'warning'">
              {{ parsedResult.statusCode || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Token" :span="2" v-if="parsedResult.token">
            <el-input :value="parsedResult.token" readonly>
              <template #append>
                <el-button @click="copyToken(parsedResult.token)">复制</el-button>
              </template>
            </el-input>
          </el-descriptions-item>
          <el-descriptions-item label="测试消息" :span="2">
            {{ parsedResult.testMessage || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="响应内容" :span="2" v-if="parsedResult.responseBody">
            <el-input
              :value="parsedResult.responseBody"
              type="textarea"
              :rows="3"
              readonly
            />
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">📊 分析报告</el-divider>
        <el-input
          v-if="parsedResult?.analysisReport"
          :value="parsedResult.analysisReport"
          type="textarea"
          :rows="10"
          readonly
          style="font-family: monospace"
        />

        <el-divider content-position="left">💾 完整分析结果 (JSON)</el-divider>
        <el-input
          :model-value="formatAnalysisResult(currentRow.analysisResult)"
          type="textarea"
          :rows="10"
          readonly
        />

        <div style="margin-top: 20px; text-align: right">
          <el-button type="primary" @click="handlePushToRegister(currentRow)">
            <el-icon><Right /></el-icon>
            一键填充到注册任务
          </el-button>
        </div>
      </template>

      <template v-else-if="currentRow.analysisStatus === 3">
        <el-alert type="error" :title="currentRow.errorMessage || '分析失败'" show-icon :closable="false" />
      </template>

      <template v-else>
        <el-alert type="info" title="分析中,请稍候..." show-icon :closable="false" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Right } from '@element-plus/icons-vue'
import request from '@/utils/request'
import wsClient from '@/utils/websocket'
import { useRouter } from 'vue-router'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  websiteUrl: '',
  status: null,
  pageNum: 1,
  pageSize: 10
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const multipleSelection = ref([])

// 对话框
const showAnalyzeDialog = ref(false)
const detailVisible = ref(false)
const currentRow = ref(null)
const submitLoading = ref(false)

// 表单
const analyzeFormRef = ref(null)
const analyzeForm = reactive({ websiteUrl: '' })

const analyzeRules = {
  websiteUrl: [
    { required: true, message: '请输入网站地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/business/analysis/register/list', { params: queryParams })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
    
    // 为正在分析中的任务订阅WebSocket消息
    tableData.value.forEach(row => {
      if (row.analysisStatus === 1) {
        subscribeAnalysisStatus(row.id)
      }
    })
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.websiteUrl = ''
  queryParams.status = null
  queryParams.pageNum = 1
  fetchData()
}

const handleStartAnalyze = async () => {
  await analyzeFormRef.value.validate()
  submitLoading.value = true
  try {
    const res = await request.post('/business/analysis/register/start', { websiteUrl: analyzeForm.websiteUrl })
    const analysisId = res.data
    
    ElMessage.success('分析任务已启动')
    showAnalyzeDialog.value = false
    fetchData()
    
    // 订阅该任务的WebSocket消息
    subscribeAnalysisStatus(analysisId)
    
  } catch (error) {
    ElMessage.error(error.message || '启动分析失败')
  } finally {
    submitLoading.value = false
  }
}

// 订阅分析状态更新
const subscribeAnalysisStatus = (analysisId) => {
  const topic = `/topic/analysis/${analysisId}`
  console.log('[自动化注册分析] 订阅任务状态:', topic)
  
  wsClient.subscribe(topic, (message) => {
    console.log('[自动化注册分析] 收到状态更新:', message)
    
    if (message.type === 'analysis_status') {
      // 更新表格中对应的行
      const index = tableData.value.findIndex(item => item.id === message.analysisId)
      if (index !== -1) {
        tableData.value[index].analysisStatus = message.status
        
        // 如果当前正在查看该任务的详情，刷新详情
        if (currentRow.value && currentRow.value.id === message.analysisId) {
          handleViewDetail(tableData.value[index])
        }
      }
      
      // 显示通知
      if (message.status === 2) {
        ElMessage.success({
          message: `分析任务完成: ${message.message}`,
          duration: 5000
        })
        // 取消订阅
        wsClient.unsubscribe(topic)
      } else if (message.status === 3) {
        ElMessage.error({
          message: `分析任务失败: ${message.message}`,
          duration: 5000
        })
        // 取消订阅
        wsClient.unsubscribe(topic)
      }
    }
  })
}

const handleViewDetail = async (row) => {
  try {
    const res = await request.get(`/business/analysis/${row.id}`)
    currentRow.value = res.data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该分析记录吗?', '提示', { type: 'warning' })
    await request.delete(`/business/analysis/register/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleSelectionChange = (val) => {
  multipleSelection.value = val
}

const handleBatchDelete = async () => {
  if (!multipleSelection.value || multipleSelection.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${multipleSelection.value.length} 条记录吗?`, '提示', { type: 'warning' })
    for (const item of multipleSelection.value) {
      await request.delete(`/business/analysis/register/${item.id}`)
    }
    ElMessage.success('批量删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量删除失败')
  }
}

const handlePushToRegister = (row) => {
  router.push({ path: '/business/register', query: { analysisId: row.id } })
}

const resetForm = () => {
  analyzeForm.websiteUrl = ''
  analyzeFormRef.value?.clearValidate()
}

// 复制Token
const copyToken = (token) => {
  navigator.clipboard.writeText(token).then(() => {
    ElMessage.success('Token已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 解析分析结果
const parsedResult = computed(() => {
  if (!currentRow.value || !currentRow.value.analysisResult) return null
  try {
    return JSON.parse(currentRow.value.analysisResult)
  } catch (e) {
    return null
  }
})

// 格式化分析结果
const formatAnalysisResult = (result) => {
  if (!result) return ''
  try {
    const obj = typeof result === 'string' ? JSON.parse(result) : result
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return result
  }
}

onMounted(() => {
  fetchData()
  
  // 连接WebSocket
  wsClient.connect(
    () => {
      console.log('[自动化注册分析] WebSocket连接成功')
    },
    (error) => {
      console.error('[自动化注册分析] WebSocket连接失败', error)
    }
  )
})

onUnmounted(() => {
  // 组件销毁时取消所有订阅
  tableData.value.forEach(row => {
    if (row.analysisStatus === 1) {
      wsClient.unsubscribe(`/topic/analysis/${row.id}`)
    }
  })
})
</script>

<style scoped>
.analysis-register-container { padding: 20px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; }
.table-footer { margin-top: 10px; display: flex; justify-content: center; }
.form-tip { font-size: 12px; color: #909399; margin-top: 5px; }
</style>
