<template>
  <div class="task-list-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">任务列表</div>
          <div class="toolbar-actions">
            <el-button type="primary" icon="Plus" @click="handleCreate">新建任务</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryForm" class="query-bar">
        <el-form-item label="任务状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待执行" :value="1" />
            <el-option label="执行中" :value="2" />
            <el-option label="已暂停" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="失败" :value="5" />
            <el-option label="已停止" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称">
          <el-input v-model="queryForm.taskName" placeholder="请输入任务名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="targetSite" label="目标站" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.progress || 0)"
              :color="getProgressColor(row.status)"
              :status="row.status === 5 ? 'exception' : null"
            />
          </template>
        </el-table-column>
        <el-table-column label="数据统计" width="180">
          <template #default="{ row }">
            <div style="font-size: 12px">
              <div>总数: {{ row.totalCount || 0 }}</div>
              <div>完成: {{ row.completedCount || 0 }}</div>
              <div>成功: {{ row.successCount || 0 }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="success" size="small" @click="handleStart(row)" link>
              启动
            </el-button>
            <el-button v-if="row.status === 2" type="warning" size="small" @click="handlePause(row)" link>
              暂停
            </el-button>
            <el-button v-if="row.status === 2 || row.status === 3" type="danger" size="small" @click="handleStop(row)" link>
              停止
            </el-button>
            <el-button type="primary" size="small" @click="handleViewDetail(row)" link>详情</el-button>
            <el-button v-if="row.status !== 2" type="danger" size="small" @click="handleDelete(row)" link>
              删除
            </el-button>
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="800px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="任务ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentRow.taskName }}</el-descriptions-item>
        <el-descriptions-item label="目标站">{{ currentRow.targetSite }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRow.status)">
            {{ getStatusText(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行进度" :span="2">
          <el-progress
            :percentage="Number(currentRow.progress || 0)"
            :color="getProgressColor(currentRow.status)"
          />
        </el-descriptions-item>
        <el-descriptions-item label="总数据量">{{ currentRow.totalCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已完成">{{ currentRow.completedCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="成功数量">{{ currentRow.successCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败数量">{{ currentRow.failCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="并发数">{{ currentRow.concurrentNum }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag>{{ getPriorityText(currentRow.priority) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentRow.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentRow.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计剩余">{{ formatRemaining(currentRow) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentRow = ref(null)
let timer = null

const queryForm = reactive({
  status: null,
  taskName: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/task/page', {
      params: {
        current: pagination.current,
        size: pagination.size,
        status: queryForm.status,
        taskName: queryForm.taskName
      }
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    ElMessage.error('获取任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  router.push('/task/create')
}

const handleStart = async (row) => {
  try {
    const res = await request.post(`/task/${row.id}/start`)
    if (res.code === 200) {
      ElMessage.success('任务启动成功')
      fetchData()
    }
  } catch (error) {
    ElMessage.error(error.message || '任务启动失败')
  }
}

const handlePause = async (row) => {
  // 暂停功能待实现
  ElMessage.info('暂停功能开发中')
}

const handleStop = async (row) => {
  await ElMessageBox.confirm('确定要停止该任务吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    const res = await request.post(`/task/${row.id}/stop`)
    if (res.code === 200) {
      ElMessage.success('任务已停止')
      fetchData()
    }
  } catch (error) {
    ElMessage.error('停止任务失败')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该任务吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    const res = await request.delete(`/task/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleViewDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

const handleQuery = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryForm.status = null
  queryForm.taskName = ''
  pagination.current = 1
  fetchData()
}

const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const getStatusText = (status) => {
  const map = {
    1: '待执行',
    2: '执行中',
    3: '已暂停',
    4: '已完成',
    5: '失败',
    6: '已停止'
  }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = {
    1: 'info',
    2: '',
    3: 'warning',
    4: 'success',
    5: 'danger',
    6: 'info'
  }
  return map[status] || ''
}

const getProgressColor = (status) => {
  return status === 5 ? '#f56c6c' : status === 4 ? '#67c23a' : '#409eff'
}

const getPriorityText = (priority) => {
  const map = { 1: '高', 2: '中', 3: '低' }
  return map[priority] || '未知'
}

const formatRemaining = (row) => {
  if (!row.estimateRemainingSeconds) return '-'
  const hours = Math.floor(row.estimateRemainingSeconds / 3600)
  const minutes = Math.floor((row.estimateRemainingSeconds % 3600) / 60)
  return `${hours}小时${minutes}分钟`
}

onMounted(() => {
  fetchData()
  // 每10秒自动刷新（仅在页面可见时触发）
  timer = setInterval(() => { if (document.visibilityState === 'visible') fetchData() }, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.task-list-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.query-form {
  display: none;
}
</style>
