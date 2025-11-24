<template>
  <div class="history-data-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">历史检测数据</div>
          <div class="toolbar-actions">
            <el-button class="ghost-button" icon="Download" @click="handleExport">导出数据</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryForm" class="query-bar">
        <el-form-item label="任务ID">
          <el-input v-model="queryForm.taskId" placeholder="请输入任务ID" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="检测状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="已注册" :value="1" />
            <el-option label="未注册" :value="2" />
            <el-option label="检测失败" :value="3" />
            <el-option label="账号异常" :value="4" />
            <el-option label="代理异常" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计信息 -->
      <el-row :gutter="20" class="statistics-row" v-if="statistics">
        <el-col :span="6">
          <el-statistic title="总检测数" :value="statistics.totalCount" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="成功数" :value="statistics.successCount">
            <template #suffix>
              <span style="font-size: 14px; color: #67c23a">
                ({{ ((statistics.successCount / statistics.totalCount) * 100).toFixed(2) }}%)
              </span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="失败数" :value="statistics.failCount">
            <template #suffix>
              <span style="font-size: 14px; color: #f56c6c">
                ({{ ((statistics.failCount / statistics.totalCount) * 100).toFixed(2) }}%)
              </span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均响应时间" :value="statistics.avgResponseTime" suffix="ms" />
        </el-col>
      </el-row>

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskId" label="任务ID" width="100" />
        <el-table-column prop="dataValue" label="检测账号" min-width="160" />
        <el-table-column prop="targetSite" label="目标站" min-width="140" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应时间" width="100">
          <template #default="{ row }">
            {{ row.responseTime ? row.responseTime + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="detectTime" label="检测时间" width="160" />
      </el-table>

      <!-- 分页 -->
      <div class="table-footer">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[20, 50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const statistics = reactive({
  totalCount: 0,
  successCount: 0,
  failCount: 0,
  avgResponseTime: 0
})

const queryForm = reactive({
  taskId: null,
  status: null,
  dateRange: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 获取数据列表
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }

    if (queryForm.taskId) {
      params.taskId = queryForm.taskId
    }
    if (queryForm.status !== null) {
      params.status = queryForm.status
    }
    if (queryForm.dateRange && queryForm.dateRange.length === 2) {
      params.startTime = queryForm.dateRange[0]
      params.endTime = queryForm.dateRange[1]
    }

    const res = await request.get('/data/result/page', { params })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
      
      // 获取统计信息
      await fetchStatistics()
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 获取统计信息（仅在首次访问时请求）
const fetchStatistics = async () => {
  if (statistics.totalCount > 0) return // 已加载过，跳过
  try {
    const params = {}
    if (queryForm.taskId) {
      params.taskId = queryForm.taskId
    }
    
    const res = await request.get('/data/result/statistics', { params })
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('获取统计信息失败', error)
  }
}

// 查询
const handleQuery = () => {
  pagination.current = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryForm.taskId = null
  queryForm.status = null
  queryForm.dateRange = null
  pagination.current = 1
  fetchData()
}

// 导出数据
const handleExport = async () => {
  try {
    const params = {}
    if (queryForm.taskId) {
      params.taskId = queryForm.taskId
    }

    const res = await request.get('/data/result/export', {
      params,
      responseType: 'blob'
    })

    // 创建下载链接
    const blob = new Blob([res], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `检测结果_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

// 分页变化
const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

// 状态文本
const getStatusText = (status) => {
  const map = {
    1: '已注册',
    2: '未注册',
    3: '检测失败',
    4: '账号异常',
    5: '代理异常'
  }
  return map[status] || '未知'
}

// 状态类型
const getStatusType = (status) => {
  const map = {
    1: 'success',
    2: 'info',
    3: 'danger',
    4: 'warning',
    5: 'danger'
  }
  return map[status] || ''
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.history-data-container {
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

.statistics-row {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
