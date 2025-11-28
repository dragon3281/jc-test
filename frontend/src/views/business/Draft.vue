<template>
  <div class="draft-container">
    <el-card shadow="hover">
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">注册模板草稿箱</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="showUpload = true">
              <el-icon><Upload /></el-icon>
              脚本上传并创建草稿
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="draftName" label="草稿名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="websiteUrl" label="网站地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="registerApi" label="注册接口" min-width="160" show-overflow-tooltip />
        <el-table-column label="测试结果" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.testResult === 1" type="success">成功</el-tag>
            <el-tag v-else-if="row.testResult === 2" type="danger">失败</el-tag>
            <el-tag v-else type="info">未测试</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTest(row)">运行测试</el-button>
            <el-button v-if="row.testResult === 1 && row.testToken" link type="success" @click="handleSaveToTemplate(row)">保存至模板</el-button>
            <el-button v-else link type="info" @click="handleKeepDraft(row)">保存至草稿箱</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 上传脚本并创建草稿 -->
    <el-dialog v-model="showUpload" title="脚本上传并创建草稿" width="700px" @close="resetUploadForm">
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-width="120px">
        <el-form-item label="草稿名称" prop="draftName">
          <el-input v-model="uploadForm.draftName" placeholder="请输入草稿名称" />
        </el-form-item>
        <el-form-item label="网站地址" prop="websiteUrl">
          <el-input v-model="uploadForm.websiteUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="注册接口" prop="registerApi">
          <el-input v-model="uploadForm.registerApi" placeholder="/user/register" />
        </el-form-item>
        <el-form-item label="请求方法" prop="method">
          <el-radio-group v-model="uploadForm.method">
            <el-radio label="POST">POST</el-radio>
            <el-radio label="PUT">PUT</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名字段" prop="usernameField">
          <el-input v-model="uploadForm.usernameField" placeholder="username" />
        </el-form-item>
        <el-form-item label="密码字段" prop="passwordField">
          <el-input v-model="uploadForm.passwordField" placeholder="password" />
        </el-form-item>
        <el-form-item label="默认密码" prop="defaultPassword">
          <el-input v-model="uploadForm.defaultPassword" placeholder="133adb" />
        </el-form-item>
        <el-form-item label="额外参数">
          <el-input v-model="uploadForm.extraParams" type="textarea" :rows="4" placeholder='{"login":true}' />
        </el-form-item>
        <el-form-item label="执行脚本" prop="executorScript">
          <el-input v-model="uploadForm.executorScript" type="textarea" :rows="8" placeholder="请粘贴脚本内容（Python/JS）" />
          <div class="form-tip">脚本将在沙箱中运行，仅当输出结果包含token时可保存至模板</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="handleUpload">上传并创建草稿</el-button>
      </template>
    </el-dialog>

    <!-- 测试结果展示 -->
    <el-dialog v-model="testVisible" title="测试结果" width="700px">
      <div v-if="currentDraft">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="草稿名称">{{ currentDraft.draftName }}</el-descriptions-item>
          <el-descriptions-item label="网站地址">{{ currentDraft.websiteUrl }}</el-descriptions-item>
          <el-descriptions-item label="注册接口">{{ currentDraft.registerApi }}</el-descriptions-item>
          <el-descriptions-item label="方法">{{ currentDraft.method }}</el-descriptions-item>
          <el-descriptions-item label="测试结果">
            <el-tag v-if="currentDraft.testResult === 1" type="success">成功</el-tag>
            <el-tag v-else-if="currentDraft.testResult === 2" type="danger">失败</el-tag>
            <el-tag v-else type="info">未测试</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Token" :span="2">{{ currentDraft.testToken || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="2">{{ currentDraft.testError || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 12px">
          <el-button v-if="currentDraft.testResult === 1 && currentDraft.testToken" type="success" @click="handleSaveToTemplate(currentDraft)">保存至模板</el-button>
          <el-button v-else type="info" @click="handleKeepDraft(currentDraft)">保存至草稿箱</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

const showUpload = ref(false)
const uploadLoading = ref(false)
const uploadFormRef = ref(null)
const uploadForm = reactive({
  draftName: '',
  websiteUrl: '',
  registerApi: '',
  method: 'POST',
  usernameField: 'username',
  passwordField: 'password',
  defaultPassword: '133adb',
  extraParams: '',
  executorScript: ''
})

const uploadRules = {
  draftName: [{ required: true, message: '请输入草稿名称', trigger: 'blur' }],
  websiteUrl: [
    { required: true, message: '请输入网站地址', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ],
  registerApi: [{ required: true, message: '请输入注册接口', trigger: 'blur' }],
  executorScript: [{ required: true, message: '请输入执行脚本', trigger: 'blur' }]
}

const testVisible = ref(false)
const currentDraft = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/business/draft/list', { params: {
      pageNum: pagination.current,
      pageSize: pagination.size
    }})
    const page = res.data || { records: [], total: 0 }
    tableData.value = page.records
    pagination.total = page.total
  } catch (e) {
    ElMessage.error('获取草稿列表失败')
  } finally {
    loading.value = false
  }
}

const handleUpload = async () => {
  await uploadFormRef.value.validate()
  uploadLoading.value = true
  try {
    const res = await request.post('/business/draft/upload', uploadForm)
    ElMessage.success('草稿创建成功')
    showUpload.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

const handleTest = async (row) => {
  try {
    const res = await request.post(`/business/draft/test/${row.id}`)
    const data = res.data || {}
    // 更新本地展示（后端已落库）
    row.testResult = data.success ? 1 : 2
    row.testToken = data.token || null
    row.testError = data.error || null
    currentDraft.value = { ...row }
    testVisible.value = true
    ElMessage.success('测试完成')
  } catch (e) {
    ElMessage.error(e.message || '测试失败')
  }
}

const handleSaveToTemplate = async (row) => {
  try {
    await ElMessageBox.confirm('保存为模板后可在注册页面选择并使用，确定继续吗？', '确认', { type: 'warning' })
    const name = `${row.websiteUrl}_模板`
    await request.post('/business/register/template/add-from-draft', { draftId: row.id, templateName: name })
    ElMessage.success('已保存至模板')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('保存模板失败')
  }
}

const handleKeepDraft = async (row) => {
  // 草稿本身已存在，此处仅提示
  ElMessage.info('已保留在草稿箱')
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该草稿吗？', '提示', { type: 'warning' })
    await request.post(`/business/draft/delete/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const resetUploadForm = () => {
  Object.assign(uploadForm, {
    draftName: '', websiteUrl: '', registerApi: '', method: 'POST',
    usernameField: 'username', passwordField: 'password', defaultPassword: '133adb',
    extraParams: '', executorScript: ''
  })
  uploadFormRef.value?.clearValidate()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.draft-container { padding: 20px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; }
.table-footer { margin-top: 10px; display: flex; justify-content: center; }
.form-tip { font-size: 12px; color: #909399; margin-top: 5px; }
</style>
