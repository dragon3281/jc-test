<template>
  <div class="template-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">POST模板管理</div>
          <div class="toolbar-actions">
            <el-button type="primary" icon="Plus" @click="handleAdd">新建模板</el-button>
          </div>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="targetSite" label="目标站" min-width="150" />
        <el-table-column prop="requestMethod" label="请求方法" width="100">
          <template #default="{ row }">
            <el-tag :type="row.requestMethod === 'POST' ? 'success' : 'info'" size="small">
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enableProxy" label="使用代理" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enableProxy === 1 ? 'success' : 'info'" size="small">
              {{ row.enableProxy === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timeout" label="超时时间" width="100">
          <template #default="{ row }">{{ row.timeout }}秒</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)" link>编辑</el-button>
            <el-button type="success" size="small" @click="handleTest(row)" link>测试</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)" link>删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="table-footer">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="目标站" prop="targetSite">
          <el-input v-model="form.targetSite" placeholder="请输入目标站URL" />
        </el-form-item>
        <el-form-item label="请求URL" prop="requestUrl">
          <el-input v-model="form.requestUrl" placeholder="请输入完整的API地址" />
        </el-form-item>
        <el-form-item label="请求方法" prop="requestMethod">
          <el-radio-group v-model="form.requestMethod">
            <el-radio label="GET">GET</el-radio>
            <el-radio label="POST">POST</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="请求头" prop="requestHeaders">
          <el-input
            v-model="form.requestHeaders"
            type="textarea"
            :rows="4"
            placeholder='{"Content-Type": "application/json", "User-Agent": "Mozilla/5.0"}'
          />
          <el-text type="info" size="small">JSON格式,支持{{account}}, {{timestamp}}等占位符</el-text>
        </el-form-item>
        <el-form-item label="请求体" prop="requestBody" v-if="form.requestMethod === 'POST'">
          <el-input
            v-model="form.requestBody"
            type="textarea"
            :rows="4"
            placeholder='{"account": "{{account}}", "timestamp": "{{timestamp}}"}'
          />
          <el-text type="info" size="small">JSON格式,使用{{account}}作为账号占位符</el-text>
        </el-form-item>
        <el-form-item label="成功判断规则" prop="successRule">
          <el-input
            v-model="form.successRule"
            type="textarea"
            :rows="3"
            placeholder='{"code": "200", "message": "已注册"}'
          />
        </el-form-item>
        <el-form-item label="失败判断规则" prop="failRule">
          <el-input
            v-model="form.failRule"
            type="textarea"
            :rows="3"
            placeholder='{"code": "404", "message": "未注册"}'
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="使用代理" prop="enableProxy">
              <el-switch v-model="form.enableProxy" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时时间(秒)" prop="timeout">
              <el-input-number v-model="form.timeout" :min="5" :max="300" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重试次数" prop="retryCount">
              <el-input-number v-model="form.retryCount" :min="0" :max="5" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="如: v1.0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试对话框 -->
    <el-dialog v-model="testVisible" title="测试模板" width="600px">
      <el-form :model="testForm" label-width="100px">
        <el-form-item label="测试账号">
          <el-input v-model="testForm.testAccount" placeholder="请输入测试账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTestSubmit" :loading="testLoading">开始测试</el-button>
      </template>
      <el-divider v-if="testResult" />
      <div v-if="testResult" style="margin-top: 20px">
        <el-alert :title="testResult.success ? '测试成功' : '测试失败'" 
                  :type="testResult.success ? 'success' : 'error'" 
                  :closable="false" />
        <pre style="margin-top: 10px; padding: 10px; background: #f5f5f5; border-radius: 4px; max-height: 300px; overflow-y: auto">{{ testResult.message }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const testLoading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const testVisible = ref(false)
const dialogTitle = ref('新建模板')
const formRef = ref(null)
const testResult = ref(null)

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const form = reactive({
  id: null,
  templateName: '',
  targetSite: '',
  requestUrl: '',
  requestMethod: 'POST',
  requestHeaders: '',
  requestBody: '',
  successRule: '',
  failRule: '',
  enableProxy: 1,
  timeout: 30,
  retryCount: 3,
  version: ''
})

const testForm = reactive({
  templateId: null,
  testAccount: ''
})

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  targetSite: [{ required: true, message: '请输入目标站', trigger: 'blur' }],
  requestUrl: [{ required: true, message: '请输入请求URL', trigger: 'blur' }],
  successRule: [{ required: true, message: '请输入成功判断规则', trigger: 'blur' }],
  failRule: [{ required: true, message: '请输入失败判断规则', trigger: 'blur' }]
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/template/page', {
      params: {
        current: pagination.current,
        size: pagination.size
      }
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 新建
const handleAdd = () => {
  dialogTitle.value = '新建模板'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row) => {
  dialogTitle.value = '编辑模板'
  try {
    const res = await request.get(`/template/${row.id}`)
    if (res.code === 200) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取模板详情失败')
  }
}

// 删除
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该模板吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    const res = await request.delete(`/template/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 测试
const handleTest = (row) => {
  testForm.templateId = row.id
  testForm.testAccount = ''
  testResult.value = null
  testVisible.value = true
}

// 提交测试
const handleTestSubmit = async () => {
  if (!testForm.testAccount) {
    ElMessage.warning('请输入测试账号')
    return
  }

  testLoading.value = true
  try {
    const res = await request.post(`/template/${testForm.templateId}/test`, {
      testAccount: testForm.testAccount
    })
    testResult.value = {
      success: res.code === 200,
      message: JSON.stringify(res.data || res.message, null, 2)
    }
  } catch (error) {
    testResult.value = {
      success: false,
      message: error.message || '测试失败'
    }
  } finally {
    testLoading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate()

  submitLoading.value = true
  try {
    const url = form.id ? '/template' : '/template'
    const method = form.id ? 'put' : 'post'
    
    const res = await request[method](url, form)
    if (res.code === 200) {
      ElMessage.success(form.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchData()
    }
  } catch (error) {
    ElMessage.error(form.id ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.templateName = ''
  form.targetSite = ''
  form.requestUrl = ''
  form.requestMethod = 'POST'
  form.requestHeaders = ''
  form.requestBody = ''
  form.successRule = ''
  form.failRule = ''
  form.enableProxy = 1
  form.timeout = 30
  form.retryCount = 3
  form.version = ''
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 分页
const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.template-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
