<template>
  <div class="analysis-phone-container">
    <el-card shadow="hover">
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">号码检测分析</div>
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
        <el-table-column prop="detectedPort" label="检测端口" width="100" />
        <el-table-column label="接口类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.apiType === 1">JSON</el-tag>
            <el-tag v-else-if="row.apiType === 2" type="success">XML</el-tag>
            <el-tag v-else-if="row.apiType === 3" type="warning">HTML</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.analysisStatus === 1" type="warning">分析中</el-tag>
            <el-tag v-else-if="row.analysisStatus === 2" type="success">已完成</el-tag>
            <el-tag v-else-if="row.analysisStatus === 3" type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.analysisStatus === 2" link type="success" @click="handleGenerateTemplate(row)">生成模板</el-button>
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
    <el-dialog v-model="showAnalyzeDialog" title="新建号码检测分析" width="700px" @close="resetForm">
      <el-form :model="analyzeForm" :rules="analyzeRules" ref="analyzeFormRef" label-width="120px">
        <el-form-item label="网站地址" prop="websiteUrl">
          <el-input v-model="analyzeForm.websiteUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="检测端口" prop="ports">
          <el-input v-model="analyzeForm.ports" placeholder="80,443,8080" />
          <div class="form-tip">多个端口用逗号分隔,留空则自动检测常用端口</div>
        </el-form-item>
        <el-form-item label="接口路径" prop="apiPaths">
          <el-input
            v-model="analyzeForm.apiPaths"
            type="textarea"
            :rows="4"
            placeholder="/api/phone/check&#10;/user/verify&#10;/account/validate"
          />
          <div class="form-tip">每行一个接口路径,用于检测号码验证等接口</div>
        </el-form-item>
        <el-form-item label="超时时间(秒)" prop="timeout">
          <el-input-number v-model="analyzeForm.timeout" :min="5" :max="60" />
        </el-form-item>
        <el-form-item label="使用代理">
          <el-switch v-model="analyzeForm.useProxy" />
        </el-form-item>
        <el-form-item v-if="analyzeForm.useProxy" label="代理池" prop="proxyPoolId">
          <el-select v-model="analyzeForm.proxyPoolId" placeholder="请选择代理池">
            <el-option
              v-for="item in proxyPools"
              :key="item.id"
              :label="item.poolName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAnalyzeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleStartAnalyze" :loading="submitLoading">开始分析</el-button>
      </template>
    </el-dialog>

    <!-- 分析详情对话框 -->
    <el-dialog v-model="detailVisible" title="分析详情" width="900px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="网站地址">{{ currentRow.websiteUrl }}</el-descriptions-item>
        <el-descriptions-item label="检测端口">{{ currentRow.detectedPort }}</el-descriptions-item>
        <el-descriptions-item label="接口类型">
          <el-tag v-if="currentRow.apiType === 1">JSON</el-tag>
          <el-tag v-else-if="currentRow.apiType === 2" type="success">XML</el-tag>
          <el-tag v-else-if="currentRow.apiType === 3" type="warning">HTML</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentRow.analysisStatus === 1" type="warning">分析中</el-tag>
          <el-tag v-else-if="currentRow.analysisStatus === 2" type="success">已完成</el-tag>
          <el-tag v-else-if="currentRow.analysisStatus === 3" type="danger">失败</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ currentRow.finishTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">检测到的接口</el-divider>
      <el-table :data="currentRow.detectedApis || []" border>
        <el-table-column prop="path" label="接口路径" />
        <el-table-column prop="method" label="请求方法" width="100" />
        <el-table-column prop="contentType" label="Content-Type" min-width="150" />
        <el-table-column label="是否需要Token" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.requireToken" type="warning">是</el-tag>
            <el-tag v-else type="success">否</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">分析结果</el-divider>
      <el-input
        v-model="currentRow.analysisResult"
        type="textarea"
        :rows="10"
        readonly
        placeholder="分析结果将在完成后显示"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

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
const analyzeForm = reactive({
  websiteUrl: '',
  ports: '',
  apiPaths: '',
  timeout: 30,
  useProxy: false,
  proxyPoolId: null
})

const analyzeRules = {
  websiteUrl: [
    { required: true, message: '请输入网站地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ],
  timeout: [
    { required: true, message: '请设置超时时间', trigger: 'blur' }
  ]
}

// 代理池列表
const proxyPools = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/business/analysis/list', { params: queryParams })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
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

const fetchProxyPools = async () => {
  try {
    const res = await request.get('/proxy/pool/list')
    proxyPools.value = res.data || []
  } catch (error) {
    console.error('获取代理池失败', error)
  }
}

const handleStartAnalyze = async () => {
  await analyzeFormRef.value.validate()
  submitLoading.value = true
  try {
    await request.post('/business/analysis/start', analyzeForm)
    ElMessage.success('分析任务已启动')
    showAnalyzeDialog.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '启动分析失败')
  } finally {
    submitLoading.value = false
  }
}

const handleViewDetail = async (row) => {
  try {
    const res = await request.get(`/business/analysis/${row.id}`)
    const data = res.data
    
    if (data.detectedApis && typeof data.detectedApis === 'string') {
      try {
        data.detectedApis = JSON.parse(data.detectedApis)
      } catch (e) {
        console.error('解析detectedApis失败', e)
        data.detectedApis = []
      }
    }
    
    currentRow.value = data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleGenerateTemplate = async (row) => {
  try {
    await ElMessageBox.confirm('确定要根据此分析结果生成POST模板吗?', '提示', {
      type: 'warning'
    })
    
    await request.post('/business/analysis/generate-template', { analysisId: row.id })
    ElMessage.success('模板生成成功,请在POST模板管理中查看')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '生成模板失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该分析记录吗?', '提示', {
      type: 'warning'
    })
    
    await request.delete(`/business/analysis/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectionChange = (val) => {
  multipleSelection.value = val
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${multipleSelection.value.length} 条记录吗?`, '提示', {
      type: 'warning'
    })
    
    for (const row of multipleSelection.value) {
      await request.delete(`/business/analysis/${row.id}`)
    }
    
    ElMessage.success('批量删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const resetForm = () => {
  analyzeForm.websiteUrl = ''
  analyzeForm.ports = ''
  analyzeForm.apiPaths = ''
  analyzeForm.timeout = 30
  analyzeForm.useProxy = false
  analyzeForm.proxyPoolId = null
  analyzeFormRef.value?.clearValidate()
}

onMounted(() => {
  fetchData()
  fetchProxyPools()
})
</script>

<style scoped>
.analysis-phone-container {
  padding: 20px;
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 500;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.query-bar {
  margin-bottom: 20px;
}

.table-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
