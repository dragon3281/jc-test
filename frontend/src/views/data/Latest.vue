<template>
  <div class="latest-data-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">最新检测数据</div>
          <div class="toolbar-actions">
            <el-button type="primary" icon="Refresh" @click="fetchData">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryForm" class="query-bar">
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
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="dataValue" label="检测账号" min-width="180" />
        <el-table-column prop="targetSite" label="目标站" min-width="150" />
        <el-table-column prop="status" label="检测状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应时间" width="120">
          <template #default="{ row }">
            {{ row.responseTime ? row.responseTime + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="proxyIp" label="使用代理" width="150">
          <template #default="{ row }">
            {{ row.proxyIp && row.proxyPort ? `${row.proxyIp}:${row.proxyPort}` : '无代理' }}
          </template>
        </el-table-column>
        <el-table-column prop="detectTime" label="检测时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)" link>
              详情
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="检测详情" width="600px">
      <el-descriptions :column="1" border v-if="currentRow">
        <el-descriptions-item label="检测账号">{{ currentRow.dataValue }}</el-descriptions-item>
        <el-descriptions-item label="目标站">{{ currentRow.targetSite }}</el-descriptions-item>
        <el-descriptions-item label="检测状态">
          <el-tag :type="getStatusType(currentRow.status)">
            {{ getStatusText(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="响应时间">
          {{ currentRow.responseTime ? currentRow.responseTime + 'ms' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="使用代理">
          {{ currentRow.proxyIp && currentRow.proxyPort ? `${currentRow.proxyIp}:${currentRow.proxyPort}` : '无代理' }}
        </el-descriptions-item>
        <el-descriptions-item label="检测时间">{{ currentRow.detectTime }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentRow.errorMessage">
          <el-text type="danger">{{ currentRow.errorMessage }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="响应详情" v-if="currentRow.responseData">
          <pre style="max-height: 200px; overflow-y: auto">{{ formatJson(currentRow.responseData) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentRow = ref(null)

const queryForm = reactive({
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

    // 添加筛选条件
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
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  pagination.current = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryForm.status = null
  queryForm.dateRange = null
  pagination.current = 1
  fetchData()
}

// 查看详情
const handleViewDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
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

// 格式化JSON
const formatJson = (jsonStr) => {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.latest-data-container {
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

pre {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
}
</style>
