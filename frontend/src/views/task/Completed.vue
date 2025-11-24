<template>
  <div class="completed-task-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">已完成任务</div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="queryForm" class="query-bar">
        <el-form-item label="任务名称">
          <el-input v-model="queryForm.taskName" placeholder="请输入任务名称" clearable style="width: 200px" />
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

      <!-- 数据表格 -->
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="targetSite" label="目标站" min-width="150" show-overflow-tooltip />
        <el-table-column label="数据统计" width="200">
          <template #default="{ row }">
            <div style="font-size: 12px">
              <div>总数: {{ row.totalCount || 0 }}</div>
              <div>成功: <span style="color: #67c23a">{{ row.successCount || 0 }}</span></div>
              <div>失败: <span style="color: #f56c6c">{{ row.failCount || 0 }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="成功率" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="getSuccessRate(row)"
              :color="getSuccessRateColor(row)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column label="执行时间" width="150">
          <template #default="{ row }">
            {{ formatDuration(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="endTime" label="完成时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)" link>详情</el-button>
            <el-button type="success" size="small" @click="handleExport(row)" link>导出</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="900px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border v-if="currentRow">
            <el-descriptions-item label="任务ID">{{ currentRow.id }}</el-descriptions-item>
            <el-descriptions-item label="任务名称">{{ currentRow.taskName }}</el-descriptions-item>
            <el-descriptions-item label="目标站">{{ currentRow.targetSite }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag type="success">已完成</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总数据量">{{ currentRow.totalCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已完成">{{ currentRow.completedCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="成功数量">
              <span style="color: #67c23a; font-weight: bold">{{ currentRow.successCount || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="失败数量">
              <span style="color: #f56c6c; font-weight: bold">{{ currentRow.failCount || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="成功率">
              <el-progress
                :percentage="getSuccessRate(currentRow)"
                :color="getSuccessRateColor(currentRow)"
              />
            </el-descriptions-item>
            <el-descriptions-item label="平均响应时间">
              {{ currentRow.avgResponseTime || 0 }} ms
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentRow.createTime }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ currentRow.startTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ currentRow.endTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="执行时长">{{ formatDuration(currentRow) }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="统计分析" name="statistics">
          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="8">
              <el-statistic title="总检测数" :value="currentRow?.totalCount || 0" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="成功数" :value="currentRow?.successCount || 0">
                <template #suffix>
                  <span style="font-size: 14px; color: #67c23a">
                    ({{ getSuccessRate(currentRow) }}%)
                  </span>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="失败数" :value="currentRow?.failCount || 0">
                <template #suffix>
                  <span style="font-size: 14px; color: #f56c6c">
                    ({{ (100 - getSuccessRate(currentRow)).toFixed(2) }}%)
                  </span>
                </template>
              </el-statistic>
            </el-col>
          </el-row>

          <el-divider />

          <div style="margin-top: 20px">
            <h4>状态分布</h4>
            <el-descriptions :column="3" border style="margin-top: 10px">
              <el-descriptions-item label="已注册">
                <el-tag type="success">{{ statistics.registered || 0 }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="未注册">
                <el-tag type="info">{{ statistics.unregistered || 0 }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="检测失败">
                <el-tag type="danger">{{ statistics.failed || 0 }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentRow = ref(null)
const activeTab = ref('basic')
const statistics = reactive({
  registered: 0,
  unregistered: 0,
  failed: 0
})

const queryForm = reactive({
  taskName: '',
  dateRange: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      status: 4, // 只查询已完成的任务
      taskName: queryForm.taskName
    }

    if (queryForm.dateRange && queryForm.dateRange.length === 2) {
      params.startTime = queryForm.dateRange[0]
      params.endTime = queryForm.dateRange[1]
    }

    const res = await request.get('/task/page', { params })
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

const handleViewDetail = async (row) => {
  currentRow.value = row
  detailVisible.value = true
  activeTab.value = 'basic'
  
  // 获取详细统计
  try {
    const res = await request.get(`/data/result/statistics`, {
      params: { taskId: row.id }
    })
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('获取统计失败', error)
  }
}

const handleExport = async (row) => {
  try {
    const res = await request.get(`/data/result/export`, {
      params: { taskId: row.id },
      responseType: 'blob'
    })

    const blob = new Blob([res], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `任务${row.id}_检测结果_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该任务吗？删除后无法恢复！', '提示', {
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

const handleQuery = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryForm.taskName = ''
  queryForm.dateRange = null
  pagination.current = 1
  fetchData()
}

const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const getSuccessRate = (row) => {
  if (!row || !row.totalCount) return 0
  return ((row.successCount / row.totalCount) * 100).toFixed(2)
}

const getSuccessRateColor = (row) => {
  const rate = getSuccessRate(row)
  if (rate >= 90) return '#67c23a'
  if (rate >= 70) return '#e6a23c'
  return '#f56c6c'
}

const formatDuration = (row) => {
  if (!row.startTime || !row.endTime) return '-'
  const start = new Date(row.startTime).getTime()
  const end = new Date(row.endTime).getTime()
  const duration = Math.floor((end - start) / 1000)
  
  const hours = Math.floor(duration / 3600)
  const minutes = Math.floor((duration % 3600) / 60)
  const seconds = duration % 60
  
  return `${hours}小时${minutes}分${seconds}秒`
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.completed-task-container {
  padding: 20px;
}

.query-form {
  display: none;
}
</style>
