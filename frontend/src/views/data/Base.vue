<template>
  <div class="base-data-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">基础数据管理</div>
          <div class="toolbar-actions">
            <el-button type="primary" icon="Upload" @click="showUploadDialog">上传数据</el-button>
            <el-button class="danger-ghost" plain icon="Delete" @click="handleBatchDeleteUploads">批量删除</el-button>
          </div>
        </div>
      </template>

      <!-- 上传记录表格（以文本为单位） -->
      <el-table
        :data="uploadRecords"
        style="width: 100%"
        v-loading="uploadLoading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="country" label="国家" width="120" />
        <el-table-column prop="dataType" label="数据类型" width="160" />
        <el-table-column prop="fileName" label="文本名称" min-width="200" />
        <el-table-column prop="itemCount" label="条数" width="100" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreview(row)">查看</el-button>
            <el-button type="warning" link @click="handleEdit(row)">修改</el-button>
            <el-button type="danger" link @click="handleDeleteUpload(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="table-footer">
        <el-pagination
          v-model:current-page="uploadPagination.current"
          v-model:page-size="uploadPagination.size"
          :total="uploadPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchUploadRecords"
          @current-change="fetchUploadRecords"
        />
      </div>
    </el-card>

    <!-- 上传数据对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传数据"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-width="100px">
        <el-form-item label="国家" prop="country">
          <el-select
            v-model="uploadForm.country"
            filterable
            placeholder="请选择或搜索国家"
            style="width: 100%"
          >
            <el-option
              v-for="country in countryList"
              :key="country"
              :label="country"
              :value="country"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="数据类型">
          <el-input
            v-model="uploadForm.dataType"
            placeholder=""
            clearable
          />
        </el-form-item>

        <el-form-item label="上传文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".txt"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持.txt文本，每行一个纯数字号码
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUploadSubmit" :loading="uploading">确定上传</el-button>
      </template>
    </el-dialog>

<!-- 已整合：移除上传记录模块 -->

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" title="预览前10行" width="600px">
      <div style="max-height: 400px; overflow: auto;">
        <pre style="white-space: pre-wrap;">{{ previewLines.join('\n') }}</pre>
      </div>
      <template #footer>
        <el-button @click="previewVisible=false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 修改对话框 -->
    <el-dialog v-model="editVisible" title="修改上传元数据" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="国家">
          <el-select v-model="editForm.country" filterable placeholder="请选择或搜索国家" style="width: 100%">
            <el-option v-for="country in countryList" :key="country" :label="country" :value="country" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据类型">
          <el-input v-model="editForm.dataType" placeholder="" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const selectedRows = ref([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 上传对话框相关
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadFormRef = ref()
const uploadRef = ref()

const uploadForm = reactive({
  country: '',
  dataType: '',
  file: null
})

// 国家列表（常用国家）
const countryList = [
  '中国', '美国', '日本', '韩国', '英国', '法国', '德国', '加拿大',
  '澳大利亚', '俄罗斯', '印度', '巴西', '意大利', '西班牙', '墨西哥',
  '印度尼西亚', '荷兰', '沙特阿拉伯', '土耳其', '瑞士', '波兰',
  '比利时', '瑞典', '阿根廷', '挪威', '奥地利', '阿联酋', '新加坡',
  '马来西亚', '泰国', '越南', '菲律宾', '新西兰', '爱尔兰', '丹麦',
  '芬兰', '葡萄牙', '捷克', '罗马尼亚', '希腊', '匈牙利'
]
const uploadRules = {
  country: [{ required: true, message: '请选择国家', trigger: 'change' }],
  file: [{ required: true, message: '请上传文件', trigger: 'change' }]
}

const uploadRecords = ref([])
const uploadLoading = ref(false)
const uploadPagination = reactive({ current: 1, size: 10, total: 0 })
const previewVisible = ref(false)
const previewLines = ref([])
const editVisible = ref(false)
const editForm = reactive({ batch: '', country: '', dataType: '' })

// 获取数据列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/data/base/page', {
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

// 显示上传对话框
const showUploadDialog = () => {
  uploadDialogVisible.value = true
  // 重置表单
  if (uploadFormRef.value) {
    uploadFormRef.value.resetFields()
  }
  uploadForm.country = ''
  uploadForm.dataType = ''
  uploadForm.file = null
}

// 文件选择变化
const handleFileChange = (file) => {
  uploadForm.file = file.raw
  // 手动触发验证
  if (uploadFormRef.value) {
    uploadFormRef.value.validateField('file')
  }
}

// 文件移除
const handleFileRemove = () => {
  uploadForm.file = null
}

// 提交上传
const handleUploadSubmit = async () => {
  if (!uploadFormRef.value) return
  
  await uploadFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    uploading.value = true
    
    try {
      const formData = new FormData()
      formData.append('file', uploadForm.file)
      formData.append('country', uploadForm.country)
      formData.append('dataType', uploadForm.dataType)
      
      const res = await request.post('/data/base/import', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      
      if (res.code === 200) {
        ElMessage.success(`上传成功，共导入 ${res.data} 条数据`)
        uploadDialogVisible.value = false
        fetchUploadRecords()
        // 清空上传组件
        if (uploadRef.value) {
          uploadRef.value.clearFiles()
        }
      } else {
        ElMessage.error(res.message || '上传失败')
      }
    } catch (error) {
      ElMessage.error('上传失败，请重试')
    } finally {
      uploading.value = false
    }
  })
}

// 选择变化（上传记录选择）
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 批量删除上传记录
const handleBatchDeleteUploads = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的上传记录')
    return
  }
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个文本吗？`, '提示', { type: 'warning' })
  try {
    const batches = selectedRows.value.map(r => r.importBatch)
    const res = await request.delete('/data/base/upload/batch', { data: batches })
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchUploadRecords()
      selectedRows.value = []
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 删除单条
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该数据吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    const res = await request.delete(`/data/base/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 分页变化
const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

// 上传记录列表
const fetchUploadRecords = async () => {
  uploadLoading.value = true
  try {
    const res = await request.get('/data/base/upload/page', {
      params: { current: uploadPagination.current, size: uploadPagination.size }
    })
    if (res.code === 200) {
      uploadRecords.value = res.data.records
      uploadPagination.total = res.data.total
    }
  } finally {
    uploadLoading.value = false
  }
}

const handlePreview = async (row) => {
  try {
    const res = await request.get('/data/base/upload/preview', { params: { batch: row.importBatch, limit: 10 } })
    if (res.code === 200) {
      previewLines.value = res.data || []
      previewVisible.value = true
    }
  } catch (e) {
    ElMessage.error('预览失败')
  }
}

const handleEdit = (row) => {
  editForm.batch = row.importBatch
  editForm.country = row.country || ''
  editForm.dataType = row.dataType || ''
  editVisible.value = true
}

const submitEdit = async () => {
  try {
    const params = new URLSearchParams()
    params.append('batch', editForm.batch)
    if (editForm.country) params.append('country', editForm.country)
    if (editForm.dataType) params.append('dataType', editForm.dataType)
    const res = await request.post('/data/base/upload/update', params)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      editVisible.value = false
      fetchUploadRecords()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

const handleDeleteUpload = async (row) => {
  await ElMessageBox.confirm('确定删除该批次的所有数据吗？', '提示', { type: 'warning' })
  try {
    const batches = [row.importBatch]
    const res = await request.delete('/data/base/upload/batch', { data: batches })
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchUploadRecords()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  fetchUploadRecords()
})
</script>

<style scoped>
.base-data-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
