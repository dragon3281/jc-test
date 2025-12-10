<template>
  <div class="template-container">
    <el-card>
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">POST模板管理</div>
          <div class="toolbar-actions">
            <el-button type="success" icon="MagicStick" @click="showParseDialog = true">自动识别</el-button>
            <el-button type="info" icon="Refresh" @click="handleRefresh" :loading="loading">刷新</el-button>
            <el-button type="warning" icon="Postcard" @click="handleShowPostTemplates">POST模板</el-button>
            <el-button type="primary" icon="Plus" @click="handleAdd">新建模板</el-button>
          </div>
        </div>
      </template>

      <!-- 数据表格 - 任务列表 -->
      <el-table :data="taskData" style="width: 100%" v-loading="loading" stripe border size="small">
        <el-table-column prop="taskId" label="任务ID" width="100" fixed="left" />
        <el-table-column prop="templateName" label="模板名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="targetSite" label="目标站" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="total" label="总数" width="65" align="center" />
        <el-table-column prop="processed" label="已处理" width="75" align="center" />
        <el-table-column prop="duplicateCount" label="已注册" width="75" align="center" />
        <el-table-column label="进度" width="100">
          <template #default="{ row }">
            <el-progress 
              :percentage="getProgress(row)" 
              :status="row.status === 'COMPLETE' ? 'success' : (row.status === 'ERROR' ? 'exception' : '')" 
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <!-- PENDING状态：显示运行 -->
            <el-button 
              v-if="row.status === 'PENDING'" 
              type="primary" 
              size="small" 
              @click="handleRunTask(row)" 
              link 
              icon="CaretRight"
            >运行</el-button>
            
            <!-- RUNNING状态：显示暂停 -->
            <el-button 
              v-if="row.status === 'RUNNING'" 
              type="warning" 
              size="small" 
              @click="handlePauseTask(row)" 
              link 
              icon="VideoPause"
            >暂停</el-button>
            
            <!-- PAUSED状态：显示继续运行 -->
            <el-button 
              v-if="row.status === 'PAUSED'" 
              type="success" 
              size="small" 
              @click="handleResumeTask(row)" 
              link 
              icon="VideoPlay"
            >继续运行</el-button>
            
            <!-- 所有状态都显示详情 -->
            <el-button type="info" size="small" @click="handleTaskDetail(row)" link icon="Document">详情</el-button>
            
            <!-- COMPLETE状态：显示下载下拉菜单 -->
            <el-dropdown 
              v-if="row.status === 'COMPLETE'"
              trigger="click"
              @command="(cmd) => handleDownloadTask(row, cmd)"
            >
              <el-button type="primary" size="small" link>
                下载<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="registered">
                    <el-icon><document-checked /></el-icon>
                    下载已注册
                  </el-dropdown-item>
                  <el-dropdown-item command="unregistered">
                    <el-icon><document /></el-icon>
                    下载未注册
                  </el-dropdown-item>
                  <el-dropdown-item command="original" divided>
                    <el-icon><download /></el-icon>
                    下载原文件
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            
            <!-- 所有状态都显示删除 -->
            <el-button type="danger" size="small" @click="handleDeleteTask(row)" link icon="Delete">删除</el-button>
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
          <el-input v-model="form.targetSite" placeholder="如: www.ppvip2.com" />
        </el-form-item>
        <el-form-item label="请求URL" prop="requestUrl">
          <el-input v-model="form.requestUrl" placeholder="完整URL地址" />
        </el-form-item>
        <el-form-item label="请求头" prop="requestHeaders">
          <el-input
            v-model="form.requestHeaders"
            type="textarea"
            :rows="6"
            placeholder='{"Authorization": "{{token}}", "Cookie": "{{cookie}}", "Content-Type": "application/json"}'
          />
          <el-text type="info" size="small">JSON格式，变量将按手动指定的名称替换为占位符</el-text>
        </el-form-item>
        <el-form-item label="请求体" prop="requestBody">
          <el-input
            v-model="form.requestBody"
            type="textarea"
            :rows="3"
            placeholder='{"mobile": "{{phone}}"}'
          />
          <el-text type="info" size="small">JSON格式，变量将按手动指定的名称替换为占位符</el-text>
        </el-form-item>
        <el-form-item label="检测关键字" prop="duplicateMsg">
          <el-row :gutter="10">
            <el-col :span="16">
              <el-input v-model="form.duplicateMsg" placeholder="如: customer_mobile_no_duplicated" />
            </el-col>
            <el-col :span="8">
              <el-input-number v-model="form.responseCode" :min="100" :max="599" placeholder="状态码（可选）" style="width: 100%" />
            </el-col>
          </el-row>
          <el-text type="info" size="small">左：响应中包含此关键字判断为已注册（必填） | 右：状态码（可选，如400）</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- POST模板管理对话框 -->
    <el-dialog
      v-model="showUseTemplateDialog"
      title="POST模板管理"
      width="1200px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <el-table :data="templateList" style="width: 100%" max-height="70vh" stripe border size="small">
        <el-table-column prop="id" label="ID" width="50" align="center" />
        <el-table-column prop="templateName" label="模板名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="targetSite" label="目标站" min-width="150" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.requestMethod === 'POST' ? 'success' : 'info'" size="small">
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enableProxy" label="代理" width="60" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enableProxy === 1 ? 'success' : 'info'" size="small">
              {{ row.enableProxy === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timeout" label="超时" width="70" align="center">
          <template #default="{ row }">{{ row.timeout }}秒</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="155" />
        <el-table-column label="操作" width="330" align="center">
          <template #default="{ row }">
            <el-space :size="6" wrap>
              <el-button type="primary" size="small" @click="handleCreateTask(row)" icon="Plus">创建任务</el-button>
              <el-button type="warning" size="small" @click="handleEditInDialog(row)" icon="Edit">编辑</el-button>
              <el-button type="success" size="small" @click="handleTestInDialog(row)" icon="Operation">测试</el-button>
              <el-button type="danger" size="small" @click="handleDeleteInDialog(row)" icon="Delete">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showUseTemplateDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 自动识别对话框 -->
    <el-dialog
      v-model="showParseDialog"
      title="自动识别POST请求"
      width="700px"
    >
      <el-form label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="parseForm.templateName" placeholder="为该模板起个名字" />
        </el-form-item>
        <el-form-item label="原始请求">
          <el-input
            v-model="parseForm.rawRequest"
            type="textarea"
            :rows="8"
            placeholder="粘贴原始的POST请求，包括请求行、请求头和请求体"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showParseDialog = false">取消</el-button>
        <el-button type="primary" @click="handleParse" :loading="parseLoading">开始识别</el-button>
      </template>
    </el-dialog>

    <!-- 配置变量和检测条件对话框 -->
    <el-dialog
      v-model="showConfigDialog"
      title="配置模板变量和检测条件"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-alert type="success" :closable="false" style="margin-bottom: 20px">
        <template #title>
          识别成功！URL: {{ parseResult?.url }}
        </template>
      </el-alert>

      <!-- 请求详情展示 -->
      <el-collapse style="margin-bottom: 20px">
        <el-collapse-item title="查看完整请求详情" name="1">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="请求方法">
              <el-tag type="success">{{ parseResult?.method }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="请求URL">
              {{ parseResult?.url }}
            </el-descriptions-item>
            <el-descriptions-item label="请求头">
              <pre style="margin: 0; max-height: 200px; overflow-y: auto; background: #f5f5f5; padding: 10px; border-radius: 4px">{{ formatHeaders(parseResult?.headers) }}</pre>
            </el-descriptions-item>
            <el-descriptions-item label="请求体" v-if="parseResult?.body">
              <pre style="margin: 0; max-height: 150px; overflow-y: auto; background: #f5f5f5; padding: 10px; border-radius: 4px">{{ parseResult?.body }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>
      </el-collapse>

      <el-form label-width="120px">
        <!-- 变量说明提示 -->
        <el-alert type="info" :closable="false" style="margin-bottom: 20px">
          <template #title>
            <div style="font-size: 14px">
              <strong>变量配置说明</strong>
            </div>
          </template>
          <div style="font-size: 12px; line-height: 1.6">
            • <strong>Authorization（请求头）</strong>: 用于填充token值<br/>
            • <strong>mobile/phone（请求体）</strong>: 用于填充手机号码<br/>
            • 批量检测时，系统会根据变量名称自动识别：包含auth/token的用token，包含mobile/phone的用手机号<br/>
            • 未识别的变量：请求头位置的使用token，请求体位置的使用手机号
          </div>
        </el-alert>

        <!-- 手动指定变量 -->
        <el-form-item label="指定变量" required>
          <div style="width: 100%">
            <div v-for="(variable, index) in manualVariables" :key="index" style="margin-bottom: 12px">
              <el-row :gutter="10">
                <el-col :span="10">
                  <el-input
                    v-model="variable.name"
                    placeholder="变量名（如Authorization）"
                  />
                </el-col>
                <el-col :span="11">
                  <el-select v-model="variable.location" placeholder="位置" style="width: 100%">
                    <el-option label="请求头" value="header" />
                    <el-option label="请求体" value="body" />
                  </el-select>
                </el-col>
                <el-col :span="3">
                  <el-button
                    type="danger"
                    :icon="Delete"
                    circle
                    @click="removeVariable(index)"
                    :disabled="manualVariables.length === 1"
                  />
                </el-col>
              </el-row>
            </div>
            <el-button type="primary" :icon="Plus" size="small" @click="addVariable">
              添加变量
            </el-button>
            <el-text type="info" size="small" style="display: block; margin-top: 8px">
              示例：Authorization（请求头）、mobile（请求体）
            </el-text>
          </div>
        </el-form-item>

        <el-divider />

        <!-- 检测条件配置 -->
        <el-alert type="warning" :closable="false" style="margin-bottom: 15px">
          <template #title>
            <div style="font-size: 14px">
              <strong>检测条件说明</strong>
            </div>
          </template>
          <div style="font-size: 12px; line-height: 1.6">
            <strong>判断逻辑：</strong><br/>
            1. <strong>状态码匹配</strong>：如果设置了状态码（如400），则必须匹配；如果留空，则不检查状态码<br/>
            2. <strong>关键字匹配</strong>：响应体中必须包含该关键字<br/>
            3. <strong>最终判断</strong>：状态码匹配（或未设置） <strong>AND</strong> 关键字匹配 = 已注册<br/><br/>
            <strong>示例：</strong><br/>
            • 当前配置：状态码=400，关键字=customer_mobile_no_duplicated<br/>
            • 已注册：HTTP 400 + 响应包含customer_mobile_no_duplicated<br/>
            • 未注册：HTTP 200 或 响应不包含关键字
          </div>
        </el-alert>

        <el-form-item label="检测条件" required>
          <div style="width: 100%">
            <el-form-item label="响应状态码" label-width="120px" style="margin-bottom: 15px">
              <el-row :gutter="10">
                <el-col :span="8">
                  <el-input-number
                    v-model="detectionConfig.statusCode"
                    :min="0"
                    :max="599"
                    placeholder="可选"
                    style="width: 100%"
                    :controls="false"
                  />
                </el-col>
                <el-col :span="16">
                  <el-text type="info" size="small">可选，留空则不检查状态码</el-text>
                </el-col>
              </el-row>
            </el-form-item>

            <el-form-item label="响应关键字" label-width="120px" required>
              <el-input
                v-model="detectionConfig.keyword"
                placeholder="必填，如：customer_mobile_no_duplicated"
              />
              <el-text type="info" size="small" style="display: block; margin-top: 4px">
                响应体或响应头中包含此关键字，且状态码匹配（如果设置了），则判断为已注册
              </el-text>
            </el-form-item>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleResetConfig">重新识别</el-button>
        <el-button type="primary" @click="handleGenerateTemplate" :loading="generateLoading">
          生成模板
        </el-button>
      </template>
    </el-dialog>

    <!-- 测试对话框 -->
    <el-dialog v-model="testVisible" title="批量检测测试" width="700px">
      <el-form :model="testForm" label-width="120px">
        <el-form-item label="Token列表" required>
          <el-input
            v-model="testForm.tokens"
            type="textarea"
            :rows="3"
            placeholder="每行一个token，例如：&#10;3a65986b-7536-4a70-bbd8-57d36b4019d7&#10;9eb2ed02-f736-4d62-ae33-ff40ee5b3583"
          />
          <el-text type="info" size="small">每行一个token，token数量=并发数</el-text>
        </el-form-item>
        <el-form-item label="手机号列表" required>
          <el-input
            v-model="testForm.phones"
            type="textarea"
            :rows="5"
            placeholder="每行一个手机号，例如：&#10;1677453989&#10;1677458081&#10;1677478290"
          />
          <el-text type="info" size="small">每行一个手机号，系统会按轮询逻辑自动分配token</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTestSubmit" :loading="testLoading">开始批量检测</el-button>
        <el-button v-if="progress.status === 'COMPLETE' && currentTaskId" type="success" @click="handleExportCsv">导出CSV</el-button>
      </template>
      <el-divider v-if="testResult" />
      <div v-if="progress.total" style="margin-top: 12px">
        <el-progress :percentage="Math.round((progress.processed / progress.total) * 100)"
                     :status="progress.status === 'COMPLETE' ? 'success' : (progress.status === 'ERROR' ? 'exception' : '')"/>
        <div style="margin-top: 6px; font-size: 12px; color: #666">进度：{{ progress.processed }}/{{ progress.total }}（已注册：{{ progress.duplicateCount }}）</div>
      </div>
      <div v-if="testResult" style="margin-top: 20px">
        <el-alert :title="testResult.success ? '检测完成' : '检测失败'" 
                  :type="testResult.success ? 'success' : 'error'" 
                  :closable="false" />
        <div v-if="testResult.success" style="margin-top: 15px">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="总数">{{ testResult.data.total }}</el-descriptions-item>
            <el-descriptions-item label="已注册"><el-tag type="danger">{{ testResult.data.duplicateCount }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="未注册"><el-tag type="success">{{ testResult.data.total - testResult.data.duplicateCount }}</el-tag></el-descriptions-item>
          </el-descriptions>
          
          <el-tabs v-model="activeTab" style="margin-top: 15px">
            <el-tab-pane label="已注册号码" name="duplicated">
              <el-table :data="testResult.data.duplicated" size="small" max-height="300">
                <el-table-column prop="phone" label="手机号" width="150" />
                <el-table-column prop="responseCode" label="状态码" width="100" />
                <el-table-column prop="token" label="Token" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="未注册号码" name="available">
              <el-table :data="testResult.data.available" size="small" max-height="300">
                <el-table-column prop="phone" label="手机号" width="150" />
                <el-table-column prop="responseCode" label="状态码" width="100" />
                <el-table-column prop="token" label="Token" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
        <pre v-else style="margin-top: 10px; padding: 10px; background: #f5f5f5; border-radius: 4px; max-height: 300px; overflow-y: auto">{{ testResult.message }}</pre>
      </div>
    </el-dialog>

    <!-- 运行对话框：上传文件 + 自动速率探测 + 批量检测 -->
    <el-dialog v-model="runVisible" title="运行批量检测" width="800px">
      <el-steps :active="runStep" finish-status="success" style="margin-bottom: 20px">
        <el-step title="上传文件" />
        <el-step title="批量检测" />
        <el-step title="完成" />
      </el-steps>

      <!-- 步骤1：上传文件 -->
      <div v-if="runStep === 0">
        <el-form label-width="120px">
          <el-form-item label="Token文件" required>
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".txt"
              :on-change="handleTokenFileChange"
              :file-list="runFiles.tokenFile ? [runFiles.tokenFile] : []"
            >
              <el-button type="primary" icon="Upload">选择Token文件</el-button>
            </el-upload>
            <el-text type="info" size="small" style="display: block; margin-top: 8px">
              .txt文件，每行一个token
            </el-text>
          </el-form-item>
          <el-form-item label="手机号文件" required>
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".txt"
              :on-change="handlePhoneFileChange"
              :file-list="runFiles.phoneFile ? [runFiles.phoneFile] : []"
            >
              <el-button type="primary" icon="Upload">选择手机号文件</el-button>
            </el-upload>
            <el-text type="info" size="small" style="display: block; margin-top: 8px">
              .txt文件，每行一个手机号
            </el-text>
          </el-form-item>
          <el-alert v-if="runFiles.tokenFile && runFiles.phoneFile" type="success" :closable="false" style="margin-top: 15px">
            <template #title>
              <div>已选择：Token {{runData.tokens.length}}个、手机号 {{runData.phones.length}}个</div>
            </template>
          </el-alert>
        </el-form>
      </div>

      <!-- 步骤2：速率探测 -->
      <div v-if="false">
        <el-result icon="info" title="正在进行速率探测..." v-if="probeLoading">
          <template #sub-title>
            使用少量数据检测网站的最佳并发数和限流规则
          </template>
          <template #extra>
            <el-progress :percentage="probeProgress" :indeterminate="true" />
          </template>
        </el-result>
        <div v-else-if="probeResult">
          <el-result icon="success" title="速率探测完成">
            <template #sub-title>{{ probeResult.recommendation }}</template>
          </el-result>
          <el-descriptions :column="2" border size="small" style="margin-top: 20px">
            <el-descriptions-item label="最优并发数">
              <el-tag type="success">{{ probeResult.optimalConcurrency }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="平均响应时间">
              {{ probeResult.averageResponseTime }}ms
            </el-descriptions-item>
            <el-descriptions-item label="预估速率">
              <el-tag type="primary">{{ probeResult.estimatedRate }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="预估1万个时间">
              {{ probeResult.estimatedTimeFor10k }}
            </el-descriptions-item>
            <el-descriptions-item label="限流检测">
              <el-tag :type="probeResult.rateLimitDetected ? 'warning' : 'success'">
                {{ probeResult.rateLimitDetected ? '检测到限流' : '未检测到限流' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="测试样本">
              {{ probeResult.testedPhones }}个手机号
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <!-- 步骤2：批量检测 -->
      <div v-if="runStep === 1">
        <el-alert type="info" :closable="false" style="margin-bottom: 20px">
          <template #title>
            正在执行批量检测，请耐心等待...
          </template>
        </el-alert>
        <el-progress 
          :percentage="Math.round((runProgress.processed / runProgress.total) * 100)"
          :status="runProgress.status === 'COMPLETE' ? 'success' : (runProgress.status === 'ERROR' ? 'exception' : '')"
        />
        <div style="margin-top: 10px; font-size: 14px; color: #666">
          进度：{{ runProgress.processed }}/{{ runProgress.total }} | 
          已注册：<span style="color: #f56c6c">{{ runProgress.duplicateCount }}</span> | 
          未注册：<span style="color: #67c23a">{{ runProgress.processed - runProgress.duplicateCount }}</span> | 
          未处理：<span style="color: #909399">{{ runProgress.total - runProgress.processed }}</span>
        </div>
        <el-descriptions :column="1" border size="small" style="margin-top: 20px">
          <el-descriptions-item label="当前状态">
            <el-tag :type="runProgress.status === 'RUNNING' ? 'primary' : (runProgress.status === 'COMPLETE' ? 'success' : 'danger')">
              {{ runProgress.status === 'RUNNING' ? '运行中' : (runProgress.status === 'COMPLETE' ? '已完成' : '错误') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ new Date(runProgress.startTime).toLocaleString() }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间" v-if="runProgress.endTime">
            {{ new Date(runProgress.endTime).toLocaleString() }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 步骤3：完成 -->
      <div v-if="runStep === 2">
        <el-result icon="success" title="批量检测完成">
          <template #sub-title>
            总数：{{ runProgress.total }} | 已注册：{{ runProgress.duplicateCount }} | 未注册：{{ runProgress.total - runProgress.duplicateCount }}
          </template>
          <template #extra>
            <el-button type="primary" @click="handleDownloadRunResult">下载结果</el-button>
            <el-button @click="runVisible = false">关闭</el-button>
          </template>
        </el-result>
      </div>

      <template #footer>
        <el-button @click="runVisible = false" v-if="runStep === 0">取消</el-button>
        <el-button type="primary" @click="handleRunStepNext" :loading="runStepLoading" 
                   :disabled="runStep === 0 && (!runFiles.tokenFile || !runFiles.phoneFile)">
          {{ runStep === 0 ? '下一步' : (runStep === 2 ? '完成' : '请等待...') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框：显示当前速率和统计信息 -->
    <el-dialog 
      v-model="detailVisible" 
      title="任务详情" 
      width="700px" 
      :append-to-body="true"
      :close-on-click-modal="false"
      @close="handleDetailDialogClose"
    >
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="模板名称">{{ currentDetail.templateName }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="currentDetail.status === 'RUNNING' ? 'primary' : (currentDetail.status === 'COMPLETE' ? 'success' : 'info')">
            {{ currentDetail.status === 'RUNNING' ? '运行中' : (currentDetail.status === 'COMPLETE' ? '已完成' : '等待中') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前速率">
          <el-tag type="primary">{{ currentDetail.currentRate || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最优并发数">
          {{ currentDetail.optimalConcurrency || 'N/A' }}
        </el-descriptions-item>
        <el-descriptions-item label="已检测数量">
          <el-tag>{{ currentDetail.processed || 0 }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总数量">
          {{ currentDetail.total || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="已注册">
          <el-tag type="danger">{{ currentDetail.duplicateCount || 0 }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="未注册">
          <el-tag type="success">{{ (currentDetail.processed || 0) - (currentDetail.duplicateCount || 0) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-space>
          <el-button type="primary" @click="handleRefreshTaskDetail" :loading="refreshRateLoading">刷新</el-button>
          <el-dropdown 
            v-if="currentDetail.status === 'COMPLETE' && currentDetail.taskId"
            trigger="click"
            @command="handleDownloadDetailTask"
          >
            <el-button type="success">
              下载结果<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="registered">
                  <el-icon><document-checked /></el-icon>
                  下载已注册
                </el-dropdown-item>
                <el-dropdown-item command="unregistered">
                  <el-icon><document /></el-icon>
                  下载未注册
                </el-dropdown-item>
                <el-dropdown-item command="original" divided>
                  <el-icon><download /></el-icon>
                  下载原文件
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button @click="detailVisible = false">关闭</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, ArrowDown, Download, Document, DocumentChecked } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const testLoading = ref(false)
const parseLoading = ref(false)
const generateLoading = ref(false)
const tableData = ref([]) // 模板列表（保留）
const taskData = ref([]) // 任务列表（新增）
const dialogVisible = ref(false)
const testVisible = ref(false)
const showParseDialog = ref(false)
const showConfigDialog = ref(false)
const showUseTemplateDialog = ref(false) // 使用模板对话框
const dialogTitle = ref('新建模板')
const formRef = ref(null)
const testResult = ref(null)
const parseResult = ref(null)
const manualVariables = ref([{ name: '', location: 'header' }])
const detectionConfig = reactive({ statusCode: null, keyword: '' })
const templateList = ref([]) // 所有模板列表（使用模板时选择）
const selectedTemplateId = ref(null) // 选中的模板ID

// 任务化进度
const currentTaskId = ref(null)
const progress = reactive({ total: 0, processed: 0, duplicateCount: 0, status: 'PENDING' })
let pollTimer = null

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
  variableConfig: '', // 变量配置JSON字符串
  duplicateMsg: '',
  responseCode: null // 默认为null，不检查状态码
})

const parseForm = reactive({
  templateName: '',
  rawRequest: ''
})

const testForm = reactive({
  templateId: null,
  tokens: '',
  phones: '',
  strategy: 'round_robin'
})

const activeTab = ref('duplicated')

// 运行对话框状态
const runVisible = ref(false)
const runStep = ref(0) // 0=上传文件, 1=速率探测, 2=批量检测, 3=完成
const runStepLoading = ref(false)
const runFiles = reactive({ tokenFile: null, phoneFile: null })
const runData = reactive({ tokens: [], phones: [], templateId: null, taskId: null })
const probeLoading = ref(false)
const probeProgress = ref(0)
const probeResult = ref(null)
const runProgress = reactive({ total: 0, processed: 0, duplicateCount: 0, status: 'PENDING', startTime: null, endTime: null })
const runTaskId = ref(null)
let runPollTimer = null
let detailPollTimer = null
let autoRefreshTimer = null // 自动刷新定时器

// 详情对话框状态
const detailVisible = ref(false)
const currentDetail = reactive({ 
  templateName: '', 
  taskId: null, // 添加taskId
  status: 'PENDING', 
  currentRate: null, 
  optimalConcurrency: null,
  processed: 0, 
  total: 0, 
  duplicateCount: 0 
})
const refreshRateLoading = ref(false)

// 格式化请求头显示
const formatHeaders = (headers) => {
  if (!headers) return ''
  return Object.entries(headers)
    .map(([key, value]) => `${key}: ${value}`)
    .join('\n')
}

// 添加变量
const addVariable = () => {
  manualVariables.value.push({ name: '', location: 'header' })
}

// 删除变量
const removeVariable = (index) => {
  if (manualVariables.value.length > 1) {
    manualVariables.value.splice(index, 1)
  }
}

// 获取任务数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/template/detect/tasks', {
      params: {
        current: pagination.current,
        size: pagination.size
      }
    })
    if (res.code === 200) {
      taskData.value = res.data.records || res.data || []
      pagination.total = res.data.total || taskData.value.length
      
      // 检查是否有运行中的任务，自动开启刷新
      checkAndStartAutoRefresh()
    }
  } catch (error) {
    ElMessage.error('获取任务数据失败')
  } finally {
    loading.value = false
  }
}

// 手动刷新
const handleRefresh = async () => {
  await fetchData()
  ElMessage.success('刷新成功')
}

// 检查并启动自动刷新
const checkAndStartAutoRefresh = () => {
  // 检查是否有运行中或暂停的任务
  const hasRunningTask = taskData.value.some(task => 
    task.status === 'RUNNING' || task.status === 'PAUSED'
  )
  
  if (hasRunningTask) {
    // 启动自动刷新，每15秒刷新一次
    if (!autoRefreshTimer) {
      console.log('⚙️ 检测到运行中的任务，启动自动刷新 (15秒/次)')
      autoRefreshTimer = setInterval(() => {
        console.log('🔄 自动刷新任务列表...')
        fetchData()
      }, 15000) // 15秒刷新一次
    }
  } else {
    // 没有运行中的任务，停止自动刷新
    if (autoRefreshTimer) {
      console.log('⚠️ 没有运行中的任务，停止自动刷新')
      clearInterval(autoRefreshTimer)
      autoRefreshTimer = null
    }
  }
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
    console.log('🛑 停止自动刷新')
  }
}

// 获取模板列表（使用模板时调用）
const fetchTemplates = async () => {
  try {
    const res = await request.get('/template/page', {
      params: { current: 1, size: 100 }
    })
    if (res.code === 200) {
      templateList.value = res.data.records || []
    }
  } catch (error) {
    ElMessage.error('获取模板列表失败')
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
  testForm.tokens = ''
  testForm.phones = ''
  testForm.strategy = 'round_robin'
  testResult.value = null
  testVisible.value = true
}

// 提交测试（任务化 + 轮询进度）
const handleTestSubmit = async () => {
  if (!testForm.tokens || !testForm.tokens.trim()) {
    ElMessage.warning('请输入Token列表')
    return
  }
  if (!testForm.phones || !testForm.phones.trim()) {
    ElMessage.warning('请输入手机号列表')
    return
  }

  const tokens = testForm.tokens.split('\n').map(t => t.trim()).filter(t => t)
  const phones = testForm.phones.split('\n').map(p => p.trim()).filter(p => p)
  if (tokens.length === 0) {
    ElMessage.warning('请至少输入一个Token')
    return
  }
  if (phones.length === 0) {
    ElMessage.warning('请至少输入一个手机号')
    return
  }

  testLoading.value = true
  progress.total = 0
  progress.processed = 0
  progress.duplicateCount = 0
  progress.status = 'PENDING'
  currentTaskId.value = null
  testResult.value = null
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }

  try {
    const res = await request.post('/template/detect/start', {
      templateId: testForm.templateId,
      tokens,
      phones,
      strategy: testForm.strategy
    })
    if (res.code === 200) {
      currentTaskId.value = res.data.taskId
      progress.total = res.data.total
      progress.processed = 0
      progress.status = 'RUNNING'

      // 开始轮询进度
      pollTimer = setInterval(async () => {
        try {
          const st = await request.get(`/template/detect/status/${currentTaskId.value}`)
          if (st.code === 200) {
            progress.processed = st.data.processed
            progress.duplicateCount = st.data.duplicateCount
            progress.status = st.data.status
          }
          // 拉取部分结果（展示用）
          const rr = await request.get(`/template/detect/result/${currentTaskId.value}`, { params: { offset: 0, limit: 500 } })
          if (rr.code === 200) {
            testResult.value = { success: true, data: {
              total: rr.data.total,
              duplicateCount: rr.data.duplicateCount,
              duplicated: rr.data.duplicated,
              available: rr.data.available
            } }
            activeTab.value = 'duplicated'
          }
          if (progress.status === 'COMPLETE' || progress.status === 'ERROR') {
            clearInterval(pollTimer)
            pollTimer = null
            testLoading.value = false
          }
        } catch (e) {
          // 忽略单次轮询错误
        }
      }, 2000)
    }
  } catch (error) {
    testResult.value = { success: false, message: error.message || '检测任务启动失败' }
  }
}

const handleExportCsv = async () => {
  if (!currentTaskId.value) {
    ElMessage.warning('请先启动并完成检测任务')
    return
  }
  try {
    const res = await request.get(`/template/detect/export/${currentTaskId.value}`)
    if (res.code === 200) {
      const link = document.createElement('a')
      link.href = `data:text/csv;base64,${res.data.content}`
      link.download = res.data.filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }
  } catch (e) {
    ElMessage.error('导出失败')
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
  form.variableConfig = ''
  form.duplicateMsg = ''
  form.responseCode = null
}

// 解析原始POST请求
const handleParse = async () => {
  if (!parseForm.rawRequest) {
    ElMessage.warning('请粘贴原始请求')
    return
  }

  parseLoading.value = true
  try {
    const res = await request.post('/template/parse', {
      rawRequest: parseForm.rawRequest,
      templateName: parseForm.templateName || '未命名模板'
    })
    
    if (res.code === 200) {
      parseResult.value = res.data
      
      // 关闭输入对话框，打开配置对话框
      showParseDialog.value = false
      showConfigDialog.value = true
      
      ElMessage.success('识别成功！请指定变量和检测条件')
    }
  } catch (error) {
    ElMessage.error('识别失败: ' + (error.message || '未知错误'))
  } finally {
    parseLoading.value = false
  }
}

// 生成模板
const handleGenerateTemplate = async () => {
  // 验证变量
  const validVariables = manualVariables.value.filter(v => v.name && v.name.trim())
  if (validVariables.length === 0) {
    ElMessage.warning('请至少指定一个变量')
    return
  }
  
  // 验证检测条件
  if (!detectionConfig.keyword || !detectionConfig.keyword.trim()) {
    ElMessage.warning('请输入检测关键字')
    return
  }

  generateLoading.value = true
  try {
    const res = await request.post('/template/generate', {
      parseResult: parseResult.value,
      templateName: parseForm.templateName || '未命名模板',
      manualVariables: validVariables,
      detectionConfig: detectionConfig
    })
    
    if (res.code === 200) {
      const template = res.data
      
      // 填充表单
      form.templateName = template.templateName
      form.targetSite = template.targetSite
      form.requestUrl = template.requestUrl
      form.requestMethod = template.requestMethod
      form.requestHeaders = template.requestHeaders
      form.requestBody = template.requestBody
      form.variableConfig = template.variableConfig // 保存变量配置JSON
      form.duplicateMsg = detectionConfig.keyword
      form.responseCode = detectionConfig.statusCode || null
      
      // 关闭配置对话框，打开编辑对话框
      showConfigDialog.value = false
      dialogTitle.value = '编辑模板（自动识别）'
      dialogVisible.value = true
      
      ElMessage.success('模板生成成功！请检查并保存')
    }
  } catch (error) {
    ElMessage.error('生成模板失败: ' + (error.message || '未知错误'))
  } finally {
    generateLoading.value = false
  }
}

// 重置配置
const handleResetConfig = () => {
  showConfigDialog.value = false
  parseResult.value = null
  manualVariables.value = [{ name: '', location: 'header' }]
  detectionConfig.statusCode = null
  detectionConfig.keyword = ''
  showParseDialog.value = true
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 分页
const handleSizeChange = () => {
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

// 运行按钮
const handleRun = (row) => {
  runData.templateId = row.id
  runData.tokens = []
  runData.phones = []
  runFiles.tokenFile = null
  runFiles.phoneFile = null
  runStep.value = 0
  probeResult.value = null
  runVisible.value = true
}

// 处理Token文件上传
const handleTokenFileChange = (file) => {
  runFiles.tokenFile = file
  const reader = new FileReader()
  reader.onload = (e) => {
    const content = e.target.result
    runData.tokens = content.split('\n').map(line => line.trim()).filter(line => line)
  }
  reader.readAsText(file.raw)
}

// 处理手机号文件上传
const handlePhoneFileChange = (file) => {
  runFiles.phoneFile = file
  const reader = new FileReader()
  reader.onload = (e) => {
    const content = e.target.result
    runData.phones = content.split('\n').map(line => line.trim()).filter(line => line)
  }
  reader.readAsText(file.raw)
}

// 运行步骤下一步
const handleRunStepNext = async () => {
  if (runStep.value === 0) {
    // 步骤1 -> 2: 直接开始批量检测
    runStep.value = 1
    await startBatchDetection()
  } else if (runStep.value === 2) {
    // 完成，关闭对话框
    runVisible.value = false
  }
}

// 开始速率探测
const startProbe = async () => {
  probeLoading.value = true
  probeProgress.value = 30
  
  try {
    // 使用前30个手机号进行探测
    const testPhones = runData.phones.slice(0, Math.min(30, runData.phones.length))
    
    const res = await request.post('/template/detect/probe', {
      templateId: runData.templateId,
      tokens: runData.tokens,
      testPhones: testPhones,
      autoApply: true // 自动应用最佳配置
    })
    
    if (res.code === 200) {
      probeResult.value = res.data
      probeProgress.value = 100
      ElMessage.success('速率探测完成')
      // 自动进入批量检测
      runStep.value = 2
      startBatchDetection()
    }
  } catch (error) {
    ElMessage.error('速率探测失败: ' + (error.message || '未知错误'))
  } finally {
    probeLoading.value = false
  }
}

// 开始批量检测
const startBatchDetection = async () => {
  try {
    // 如果存在taskId（从任务列表点击运行），则传入taskId
    const requestData = {
      templateId: runData.templateId,
      tokens: runData.tokens,
      phones: runData.phones
    }
    
    // 如果有任务ID，则使用该任务ID
    if (runData.taskId) {
      requestData.taskId = runData.taskId
      console.log('🎯 使用已存在的任务ID:', runData.taskId)
    } else {
      console.log('🆕 将生成新的任务ID')
    }
    
    const res = await request.post('/template/detect/start', requestData)
    
    if (res.code === 200) {
      runTaskId.value = res.data.taskId
      runProgress.total = res.data.total
      runProgress.processed = 0
      runProgress.duplicateCount = 0
      runProgress.status = 'RUNNING'
      runProgress.startTime = Date.now()
      
      console.log('✅ 任务已启动，taskId:', res.data.taskId)
      
      // 立即刷新一次任务列表
      await fetchData()
      
      // 开始轮询
      runPollTimer = setInterval(async () => {
        await pollRunProgress()
      }, 2000)
    }
  } catch (error) {
    ElMessage.error('启动批量检测失败: ' + (error.message || '未知错误'))
  }
}

// 轮询运行进度
const pollRunProgress = async () => {
  try {
    const statusRes = await request.get(`/template/detect/status/${runTaskId.value}`)
    if (statusRes.code === 200) {
      const status = statusRes.data
      runProgress.processed = status.processed
      runProgress.duplicateCount = status.duplicateCount
      runProgress.status = status.status
      runProgress.endTime = status.endTime
      
      if (status.status === 'COMPLETE' || status.status === 'ERROR') {
        clearInterval(runPollTimer)
        runPollTimer = null
        runStep.value = 2
        
        if (status.status === 'COMPLETE') {
          ElMessage.success('批量检测完成')
        } else {
          ElMessage.error('批量检测失败')
        }
      }
    }
  } catch (error) {
    console.error('轮询进度失败', error)
  }
}

// 下载运行结果
const handleDownloadRunResult = async () => {
  try {
    const res = await request.get(`/template/detect/export/${runTaskId.value}`)
    if (res.code === 200) {
      const { filename, content } = res.data
      const blob = new Blob([atob(content)], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('下载成功')
    }
  } catch (error) {
    ElMessage.error('下载失败: ' + (error.message || '未知错误'))
  }
}

// 详情按钮
const handleDetail = async (row) => {
  currentDetail.templateName = row.templateName
  currentDetail.status = 'PENDING'
  currentDetail.currentRate = null
  currentDetail.optimalConcurrency = null
  currentDetail.processed = 0
  currentDetail.total = 0
  currentDetail.duplicateCount = 0
  
  detailVisible.value = true
  
  try {
    const latest = await request.get(`/template/detect/latest/${row.id}`)
    if (latest.code === 200 && latest.data.taskId) {
      const st = latest.data
      currentDetail.status = st.status
      currentDetail.processed = st.processed
      currentDetail.total = st.total
      currentDetail.duplicateCount = st.duplicateCount
      
      if (detailPollTimer) {
        clearInterval(detailPollTimer)
      }
      detailPollTimer = setInterval(async () => {
        try {
          const s = await request.get(`/template/detect/status/${st.taskId}`)
          if (s.code === 200) {
            currentDetail.status = s.data.status
            currentDetail.processed = s.data.processed
            currentDetail.total = s.data.total
            currentDetail.duplicateCount = s.data.duplicateCount
          }
        } catch (e) {}
      }, 2000)
    }
  } catch (e) {
    // 忽略错误
  }
}

// 刷新速率
const handleRefreshRate = async () => {
  refreshRateLoading.value = true
  try {
    // 重新进行速率探测（使用少量样本）
    ElMessage.info('功能开发中，请使用运行功能进行完整检测')
  } finally {
    refreshRateLoading.value = false
  }
}

// 下载按钮
const handleDownload = async (row) => {
  ElMessage.info('请先使用运行功能执行批量检测，然后在完成后下载结果')
}

// ========== 任务管理相关函数 ==========

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    'PENDING': 'info',
    'RUNNING': 'warning',
    'PAUSED': 'warning',
    'COMPLETE': 'success',
    'ERROR': 'danger',
    'CANCELLED': 'info'
  }
  return map[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    'PENDING': '等待中',
    'RUNNING': '运行中',
    'PAUSED': '已暂停',
    'COMPLETE': '已完成',
    'ERROR': '错误',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

// 计算进度
const getProgress = (row) => {
  if (!row.total || row.total === 0) return 0
  return Math.floor((row.processed / row.total) * 100)
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit'
  })
}

// 任务详情
const handleTaskDetail = async (row) => {
  currentDetail.templateName = row.templateName
  currentDetail.taskId = row.taskId // 保存taskId
  currentDetail.status = row.status
  currentDetail.processed = row.processed
  currentDetail.total = row.total
  currentDetail.duplicateCount = row.duplicateCount
  
  detailVisible.value = true
  
  // 启动实时刷新
  if (detailPollTimer) {
    clearInterval(detailPollTimer)
  }
  
  if (row.status === 'RUNNING') {
    detailPollTimer = setInterval(async () => {
      try {
        const s = await request.get(`/template/detect/status/${row.taskId}`)
        if (s.code === 200) {
          currentDetail.status = s.data.status
          currentDetail.processed = s.data.processed
          currentDetail.total = s.data.total
          currentDetail.duplicateCount = s.data.duplicateCount
          
          // 如果任务已结束，停止轮询
          if (s.data.status === 'COMPLETE' || s.data.status === 'ERROR' || s.data.status === 'CANCELLED') {
            clearInterval(detailPollTimer)
            detailPollTimer = null
          }
        }
      } catch (e) {}
    }, 2000)
  }
}

// 关闭详情对话框
const handleDetailDialogClose = () => {
  if (detailPollTimer) {
    clearInterval(detailPollTimer)
    detailPollTimer = null
  }
}

// 刷新任务详情（手动点击刷新按钮）
const handleRefreshTaskDetail = async () => {
  refreshRateLoading.value = true
  try {
    // 使用保存的taskId
    if (currentDetail.taskId) {
      const s = await request.get(`/template/detect/status/${currentDetail.taskId}`)
      if (s.code === 200) {
        currentDetail.status = s.data.status
        currentDetail.processed = s.data.processed
        currentDetail.total = s.data.total
        currentDetail.duplicateCount = s.data.duplicateCount
        ElMessage.success('刷新成功')
      }
    } else {
      ElMessage.warning('未找到任务ID')
    }
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshRateLoading.value = false
  }
}

// 从详情对话框下载任务结果
const handleDownloadDetailTask = async (command) => {
  try {
    let apiUrl = ''
    let filename = ''
    
    if (command === 'registered') {
      apiUrl = `/template/detect/export/registered/${currentDetail.taskId}`
      filename = `已注册_${currentDetail.taskId}.txt`
    } else if (command === 'unregistered') {
      apiUrl = `/template/detect/export/unregistered/${currentDetail.taskId}`
      filename = `未注册_${currentDetail.taskId}.txt`
    } else if (command === 'original') {
      apiUrl = `/template/detect/export/${currentDetail.taskId}`
      filename = `原数据_${currentDetail.taskId}.txt`
    }
    
    const res = await request.get(apiUrl)
    if (res.code === 200) {
      // 解析响应数据，只提取手机号
      let phones = []
      
      if (command === 'original') {
        // 原数据：直接下载base64内容
        const content = atob(res.data.content)
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = res.data.filename || filename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
      } else {
        // 已注册/未注册：提取手机号
        const data = res.data
        if (data.duplicated && command === 'registered') {
          phones = data.duplicated.map(item => item.phone)
        } else if (data.available && command === 'unregistered') {
          phones = data.available.map(item => item.phone)
        }
        
        // 生成txt文件（每行一个手机号）
        const content = phones.join('\n')
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
      }
      
      ElMessage.success('下载成功')
    }
  } catch (error) {
    ElMessage.error('下载失败: ' + (error.message || '未知错误'))
  }
}

// 下载任务结果
const handleDownloadTask = async (row, command) => {
  try {
    let apiUrl = ''
    let filename = ''
    
    if (command === 'registered') {
      apiUrl = `/template/detect/export/registered/${row.taskId}`
      filename = `已注册_${row.taskId}.txt`
    } else if (command === 'unregistered') {
      apiUrl = `/template/detect/export/unregistered/${row.taskId}`
      filename = `未注册_${row.taskId}.txt`
    } else if (command === 'original') {
      apiUrl = `/template/detect/export/${row.taskId}`
      filename = `原数据_${row.taskId}.txt`
    }
    
    const res = await request.get(apiUrl)
    if (res.code === 200) {
      // 解析响应数据，只提取手机号
      let phones = []
      
      if (command === 'original') {
        // 原数据：直接下载base64内容
        const content = atob(res.data.content)
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = res.data.filename || filename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
      } else {
        // 已注册/未注册：提取手机号
        const data = res.data
        if (data.duplicated && command === 'registered') {
          phones = data.duplicated.map(item => item.phone)
        } else if (data.available && command === 'unregistered') {
          phones = data.available.map(item => item.phone)
        }
        
        // 生成txt文件（每行一个手机号）
        const content = phones.join('\n')
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
      }
      
      ElMessage.success('导出成功')
    }
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// 删除任务
const handleDeleteTask = async (row) => {
  await ElMessageBox.confirm('确定要删除该任务吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    const res = await request.delete(`/template/detect/task/${row.taskId}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 打开POST模板管理对话框
const handleShowPostTemplates = async () => {
  await fetchTemplates()
  showUseTemplateDialog.value = true
}

// 创建任务（点击POST模板对话框内的“创建任务”按钮）
const handleCreateTask = async (row) => {
  try {
    // 调用后端API创建任务
    const res = await request.post('/template/detect/create-task', {
      templateId: row.id,
      templateName: row.templateName,
      targetSite: row.targetSite
    })
    
    if (res.code === 200) {
      ElMessage.success('任务创建成功，请在任务列表中点击“使用”按钮上传文件')
      // 关闭POST模板对话框
      showUseTemplateDialog.value = false
      // 刷新任务列表
      await fetchData()
    }
  } catch (error) {
    ElMessage.error('创建任务失败：' + (error.message || '未知错误'))
  }
}

// 运行任务（点击任务列表中的"运行"按钮）
const handleRunTask = (row) => {
  // 打开运行对话框，传入任务信息
  runData.templateId = row.templateId
  runData.taskId = row.taskId // 保存任务ID
  runData.tokens = []
  runData.phones = []
  runFiles.tokenFile = null
  runFiles.phoneFile = null
  runStep.value = 0
  probeResult.value = null
  runVisible.value = true
}

// 暂停任务
const handlePauseTask = async (row) => {
  try {
    const res = await request.post(`/template/detect/pause/${row.taskId}`)
    if (res.code === 200) {
      ElMessage.success('任务已暂停')
      fetchData()
    }
  } catch (error) {
    ElMessage.error('暂停失败：' + (error.message || '未知错误'))
  }
}

// 继续运行任务
const handleResumeTask = async (row) => {
  try {
    const res = await request.post(`/template/detect/resume/${row.taskId}`)
    if (res.code === 200) {
      ElMessage.success('任务已继续运行')
      fetchData()
    }
  } catch (error) {
    ElMessage.error('继续运行失败：' + (error.message || '未知错误'))
  }
}
const handleEditInDialog = async (row) => {
  // 关闭POST模板对话框
  showUseTemplateDialog.value = false
  // 调用原有的编辑功能
  await handleEdit(row)
}

// POST模板对话框内的测试
const handleTestInDialog = (row) => {
  // 关闭POST模板对话框
  showUseTemplateDialog.value = false
  // 调用原有的测试功能
  handleTest(row)
}

// POST模板对话框内的删除
const handleDeleteInDialog = async (row) => {
  try {
    await handleDelete(row)
    // 删除成功后刷新对话框内的列表
    await fetchTemplates()
  } catch (error) {
    // 删除失败或取消，不做处理
  }
}

onMounted(() => {
  fetchData()
})

onBeforeUnmount(() => {
  // 清理所有定时器
  stopAutoRefresh()
  
  if (runPollTimer) {
    clearInterval(runPollTimer)
    runPollTimer = null
  }
  
  if (detailPollTimer) {
    clearInterval(detailPollTimer)
    detailPollTimer = null
  }
  
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  
  console.log('📦 组件卸载，清理所有定时器')
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

/* 下载按钮下拉菜单样式优化 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 16px;
}

.el-icon--right {
  margin-left: 4px;
}
</style>
