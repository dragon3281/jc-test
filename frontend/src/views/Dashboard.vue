<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon server">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">服务器总数</div>
              <div class="stat-value">{{ stats.serverCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon proxy">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">代理总数</div>
              <div class="stat-value">{{ stats.proxyCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon task">
              <el-icon><List /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">运行中任务</div>
              <div class="stat-value">{{ stats.runningTasks }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon data">
              <el-icon><DocumentCopy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">检测总数</div>
              <div class="stat-value">{{ stats.totalDetections }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务列表 -->
    <el-card class="mt-20">
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">最近任务</div>
          <div class="toolbar-actions">
            <el-button type="primary" size="small" @click="$router.push('/task/create')">
              新建检测任务
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="recentTasks" style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" />
        <el-table-column prop="targetSite" label="目标站" />
        <el-table-column prop="totalCount" label="总数据量" />
        <el-table-column prop="progressPercent" label="进度">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.progressPercent)" />
          </template>
        </el-table-column>
        <el-table-column prop="taskStatus" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.taskStatus)">
              {{ getStatusText(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const stats = ref({
  serverCount: 0,
  proxyCount: 0,
  runningTasks: 0,
  totalDetections: 0
})

const recentTasks = ref([])

const fetchStats = async () => {
  try {
    // 并发请求统计数据
    const [serverRes, taskRes] = await Promise.all([
      request.get('/server/list'),
      request.get('/task/page', { params: { current: 1, size: 5 } })
    ])
    
    if (serverRes.code === 200) {
      stats.value.serverCount = serverRes.data?.length || 0
    }
    
    if (taskRes.code === 200) {
      recentTasks.value = taskRes.data?.records || []
      stats.value.runningTasks = taskRes.data?.records?.filter(t => t.taskStatus === 2).length || 0
    }
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

onMounted(() => {
  fetchStats()
})

const getStatusType = (status) => {
  const map = {
    1: 'info',
    2: 'warning',
    3: 'warning',
    4: 'success',
    5: 'danger',
    6: 'info'
  }
  return map[status] || 'info'
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
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-card {
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    border: 1px solid #e4e7ed;

    :deep(.el-card__body) {
      padding: 20px;
    }

    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        color: white;

        &.server {
          background: #409eff;
        }
        &.proxy {
          background: #67c23a;
        }
        &.task {
          background: #e6a23c;
        }
        &.data {
          background: #f56c6c;
        }
      }

      .stat-info {
        flex: 1;

        .stat-label {
          font-size: 14px;
          color: #909399;
          margin-bottom: 6px;
          font-weight: 500;
        }

        .stat-value {
          font-size: 28px;
          font-weight: 600;
          color: #303133;
        }
      }
    }
  }

  .mt-20 {
    margin-top: 20px;

    :deep(.el-card__header) {
      background: #fff;
      padding: 16px 20px;
      border-bottom: 1px solid #e4e7ed;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        span {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }
      }
    }

    :deep(.el-table) {
      .el-table__header-wrapper th {
        background: #f5f7fa;
        color: #606266;
        font-weight: 600;
      }
    }
  }
}
</style>
