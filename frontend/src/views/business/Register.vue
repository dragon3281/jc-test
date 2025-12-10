<template>
  <div class="register-container">
    <el-card shadow="hover">
      <template #header>
        <div class="page-toolbar">
          <div class="toolbar-title">自动化注册任务</div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="showCreateDialog = true">
              <el-icon><Plus /></el-icon>
              新建注册任务
            </el-button>
            <el-button type="success" @click="showUploadDialog = true">
              <el-icon><Upload /></el-icon>
              脚本上传
            </el-button>
            <el-button type="warning" @click="showDraftDialog = true">
              <el-icon><Memo /></el-icon>
              草稿箱
            </el-button>
            <el-button type="info" @click="showTemplateDialog = true">
              <el-icon><Tickets /></el-icon>
              注册模板
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <el-form :inline="true" :model="queryParams" class="query-bar">
        <el-form-item label="任务名称">
          <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable />
        </el-form-item>
        <el-form-item label="执行状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="待执行" :value="1" />
            <el-option label="执行中" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已暂停" :value="4" />
            <el-option label="失败" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 任务列表 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="websiteUrl" label="目标网站" min-width="180" show-overflow-tooltip />
        <el-table-column label="注册进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="getProgress(row)" :color="getProgressColor(row.status)" :status="row.status === 5 ? 'exception' : null" />
            <div class="progress-text">{{ row.completedCount || 0 }} / {{ row.totalCount || 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="成功率" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.successCount > 0" type="success">{{ getSuccessRate(row) }}%</el-tag>
            <el-tag v-else type="info">0%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="info">待执行</el-tag>
            <el-tag v-else-if="row.status === 2" type="warning">执行中</el-tag>
            <el-tag v-else-if="row.status === 3" type="success">已完成</el-tag>
            <el-tag v-else-if="row.status === 4">已暂停</el-tag>
            <el-tag v-else-if="row.status === 5" type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="success" @click="handleStart(row)">启动</el-button>
            <el-button v-if="row.status === 2" link type="warning" @click="handlePause(row)">暂停</el-button>
            <el-button v-if="row.status === 4" link type="primary" @click="handleResume(row)">继续</el-button>
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 3 && row.successCount > 0 && !hasTemplateFor(row.websiteUrl)" link type="success" @click="handleAddToTemplate(row)">保存为模板</el-button>
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

    <!-- 脚本上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="脚本上传" width="600px">
      <el-form :model="uploadForm" label-width="120px">
        <el-form-item label="草稿名称">
          <el-input v-model="uploadForm.draftName" placeholder="如：自动化注册脚本" />
        </el-form-item>
        <el-form-item label="目标站URL">
          <el-input v-model="uploadForm.websiteUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="脚本文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".py"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持Python脚本，例如 自动化注册_linux.py</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="success" :loading="uploadLoading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 草稿箱管理对话框 -->
    <el-dialog v-model="showDraftDialog" title="草稿箱管理" width="1200px">
      <div style="margin-bottom: 10px">
        <el-button v-if="draftList.length > 0" type="danger" size="small" @click="handleClearDrafts">清空草稿箱</el-button>
      </div>
      <el-table :data="draftList" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="draftName" label="草稿名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="websiteUrl" label="目标网站" min-width="150" show-overflow-tooltip />
        <el-table-column label="测试结果" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.testResult === 0" type="info">未测试</el-tag>
            <el-tag v-else-if="row.testResult === 1" type="success">通过</el-tag>
            <el-tag v-else-if="row.testResult === 2" type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="autoNotes" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTestDraft(row)">测试</el-button>
            <el-button v-if="row.testResult === 1 && row.testToken" link type="success" @click="handleSaveDraftToTemplate(row)">保存为模板</el-button>
            <el-button link type="danger" @click="handleDeleteDraft(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新建注册任务对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建注册任务" width="800px" @close="resetForm">
      <!-- 顶部模板选择（仅当非分析结果填充时显示） -->
      <el-alert v-if="!templateLoaded && !route.query.analysisId" type="info" :closable="false" style="margin-bottom: 15px">
        <template #title>
          <span>可选择已有模板快速填充：</span>
          <el-select v-model="selectedTemplateId" placeholder="选择模板" size="small" style="width: 300px; margin-left: 10px" @change="loadTemplate">
            <el-option v-for="t in templateList" :key="t.id" :label="t.templateName" :value="t.id" />
          </el-select>
        </template>
      </el-alert>
      <el-alert v-else-if="templateLoaded" type="success" :closable="false" style="margin-bottom: 15px">
        <template #title>
          <span>已加载模板：{{ loadedTemplateName }}</span>
          <el-button link type="primary" size="small" @click="clearTemplate" style="margin-left: 10px">清除</el-button>
        </template>
      </el-alert>
      <el-alert v-else-if="route.query.analysisId" type="success" :closable="false" style="margin-bottom: 15px">
        <template #title>
          <span>✨ 已从网站分析结果自动填充参数</span>
        </template>
      </el-alert>
      <el-steps :active="currentStep" finish-status="success" align-center class="steps">
        <el-step title="基础配置" />
        <el-step title="加密配置" />
        <el-step title="执行配置" />
      </el-steps>

      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="130px" class="form-content">
        <!-- 步骤1: 基础配置 -->
        <div v-show="currentStep === 0">
          <el-form-item label="任务名称" prop="taskName">
            <el-input v-model="registerForm.taskName" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="目标网站" prop="websiteUrl">
            <el-input v-model="registerForm.websiteUrl" placeholder="https://www.wwwtk666.com" />
            <div class="form-tip">网站首页地址</div>
          </el-form-item>
          <el-form-item label="注册接口" prop="registerApi">
            <el-input v-model="registerForm.registerApi" placeholder="/wps/member/register" />
            <div class="form-tip">注册接口的路径</div>
          </el-form-item>
          <el-form-item label="请求方法" prop="method">
            <el-radio-group v-model="registerForm.method">
              <el-radio label="POST">POST</el-radio>
              <el-radio label="PUT">PUT</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="用户名字段" prop="usernameField">
            <el-input v-model="registerForm.usernameField" placeholder="username" />
            <div class="form-tip">注册请求中的用户名字段名</div>
          </el-form-item>
          <el-form-item label="密码字段" prop="passwordField">
            <el-input v-model="registerForm.passwordField" placeholder="password" />
            <div class="form-tip">注册请求中的密码字段名</div>
          </el-form-item>
          <el-form-item label="默认密码" prop="defaultPassword">
            <el-input v-model="registerForm.defaultPassword" placeholder="133adb" />
            <div class="form-tip">所有账号使用的相同密码</div>
          </el-form-item>
          <el-form-item label="额外参数">
            <el-input
              v-model="registerForm.extraParams"
              type="textarea"
              :rows="3"
              placeholder='{"affiliateCode":"www","domain":"www-tk999","login":true,"registerMethod":"WEB"}'
            />
            <div class="form-tip">JSON格式，其他需要提交的字段</div>
          </el-form-item>
        </div>

        <!-- 步骤2: 加密配置 -->
        <div v-show="currentStep === 1">
          <el-form-item label="加密类型" prop="encryptionType">
            <el-select v-model="registerForm.encryptionType" placeholder="请选择加密方式" style="width: 100%">
              <el-option label="无加密" value="NONE">
                <span>无加密</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px">明文传输</span>
              </el-option>
              <el-option label="DES+RSA (老式JS库)" value="DES_RSA">
                <span>DES+RSA (老式JS库)</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px">使用老式encryptedString方法</span>
              </el-option>
              <el-option label="DES+RSA (标准PKCS1)" value="DES_RSA_STANDARD">
                <span>DES+RSA (标准PKCS1)</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px">使用标准RSA PKCS1填充</span>
              </el-option>
              <el-option label="AES+RSA" value="AES_RSA">
                <span>AES+RSA</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px">AES-CBC + RSA加密</span>
              </el-option>
              <el-option label="MD5哈希" value="MD5">
                <span>MD5哈希</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px">仅对密码MD5加密</span>
              </el-option>
              <el-option label="BASE64编码" value="BASE64">
                <span>BASE64编码</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px">简单Base64编码</span>
              </el-option>
              <el-option label="自定义脚本" value="CUSTOM">
                <span>自定义脚本</span>
                <span style="color: #8492a6; font-size: 13px; margin-left: 10px">上传Python/JS脚本</span>
              </el-option>
            </el-select>
            <div class="form-tip">系统会自动识别目标网站使用的加密方式</div>
          </el-form-item>
          
          <!-- DES+RSA相关配置 -->
          <template v-if="['DES_RSA', 'DES_RSA_STANDARD', 'AES_RSA'].includes(registerForm.encryptionType)">
            <el-form-item label="RSA密钥接口" prop="rsaKeyApi">
              <el-input v-model="registerForm.rsaKeyApi" placeholder="/api/get-key 或 /session/key/rsa" />
              <div class="form-tip">获取RSA公钥的接口地址</div>
            </el-form-item>
            <el-form-item label="时间戳参数">
              <el-input v-model="registerForm.rsaTsParam" placeholder="t, timestamp, ts" />
              <div class="form-tip">RSA接口时间戳参数名(常见: t, timestamp, ts)</div>
            </el-form-item>
            <el-form-item label="加密请求头">
              <el-input v-model="registerForm.encryptionHeader" placeholder="encryption, X-Encrypt-Key, Authorization" />
              <div class="form-tip">RSA加密后的密钥放在哪个请求头</div>
            </el-form-item>
            <el-form-item label="数据包装字段">
              <el-input v-model="registerForm.valueFieldName" placeholder="value, data, payload, encrypted" />
              <div class="form-tip">加密数据包装的字段名</div>
            </el-form-item>
          </template>
          
          <!-- MD5相关配置 -->
          <template v-if="registerForm.encryptionType === 'MD5'">
            <el-form-item label="加盐值(可选)">
              <el-input v-model="registerForm.md5Salt" placeholder="留空则不加盐" />
              <div class="form-tip">MD5加密时的盐值,不填则直接MD5</div>
            </el-form-item>
            <el-form-item label="加密字段">
              <el-checkbox-group v-model="registerForm.md5Fields">
                <el-checkbox label="password">密码</el-checkbox>
                <el-checkbox label="username">用户名</el-checkbox>
              </el-checkbox-group>
              <div class="form-tip">选择需要MD5加密的字段</div>
            </el-form-item>
          </template>
          
          <!-- 通用验证配置 -->
          <el-form-item label="成功验证方式">
            <el-radio-group v-model="registerForm.successCheckType">
              <el-radio label="token">检测Token</el-radio>
              <el-radio label="message">检测成功消息</el-radio>
              <el-radio label="duplicate">检测重复提示</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="registerForm.successCheckType === 'duplicate'" label="重复用户名提示">
            <el-input v-model="registerForm.dupMsgSubstring" placeholder="用户名已存在, username already exists" />
            <div class="form-tip">用于验证注册成功的重复用户名提示文本</div>
          </el-form-item>
          <el-form-item v-if="registerForm.successCheckType === 'message'" label="成功消息关键词">
            <el-input v-model="registerForm.successMessage" placeholder="注册成功, success, registered" />
            <div class="form-tip">注册成功时响应中包含的关键词</div>
          </el-form-item>
        </div>

        <!-- 步骤3: 执行配置 -->
        <div v-show="currentStep === 2">
          <el-form-item label="创建数量" prop="accountCount">
            <el-input-number v-model="registerForm.accountCount" :min="1" :max="1000" />
            <div class="form-tip">本次任务要创建的账号数量</div>
          </el-form-item>
          <el-form-item label="并发数" prop="concurrency">
            <el-input-number v-model="registerForm.concurrency" :min="1" :max="20" />
            <div class="form-tip">同时执行的注册任务数量</div>
          </el-form-item>
          <el-form-item label="需要手机号">
            <el-switch v-model="registerForm.needPhone" />
          </el-form-item>
          <el-form-item v-if="registerForm.needPhone" label="手机号">
            <el-input v-model="registerForm.manualPhone" placeholder="请输入手机号（选填，不填则自动生成）" />
          </el-form-item>
          <el-form-item label="使用代理">
            <el-switch v-model="registerForm.useProxy" />
          </el-form-item>
          <el-form-item v-if="registerForm.useProxy" label="选择方式">
            <el-radio-group v-model="proxySelectMode">
              <el-radio label="pool">按代理节点</el-radio>
              <el-radio label="group">按分组</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="registerForm.useProxy && proxySelectMode === 'pool'" label="代理池" prop="proxyPoolId">
            <el-select v-model="registerForm.proxyPoolId" placeholder="请选择代理节点" clearable filterable style="width: 100%">
              <el-option v-for="item in proxyPools" :key="item.id" :label="item.poolName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="registerForm.useProxy && proxySelectMode === 'group'" label="代理分组" prop="proxyGroupName">
            <el-select 
              v-model="registerForm.proxyGroupName" 
              placeholder="请选择代理分组" 
              clearable 
              filterable 
              style="width: 100%"
              @change="handleGroupChange"
            >
              <el-option v-for="group in proxyGroups" :key="group.groupName" :label="`${group.groupName} (${group.nodeCount}个代理)`" :value="group.groupName" />
            </el-select>
            <div class="form-tip" v-if="registerForm.proxyGroupName">已选择分组 "{{ registerForm.proxyGroupName }}"，将使用分组中的所有代理节点</div>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
          <el-button v-if="currentStep < 2" type="primary" @click="nextStep">下一步</el-button>
          <el-button v-if="currentStep === 2" type="primary" @click="handleSubmit" :loading="submitLoading">提交并启动</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="1000px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border v-if="currentRow">
            <el-descriptions-item label="任务名称">{{ currentRow.taskName }}</el-descriptions-item>
            <el-descriptions-item label="目标网站">{{ currentRow.websiteUrl }}</el-descriptions-item>
            <el-descriptions-item label="加密类型">
              <el-tag v-if="currentRow.encryptionType === 'NONE' || !currentRow.encryptionType" type="info" size="small">无加密</el-tag>
              <el-tag v-else-if="currentRow.encryptionType === 'DES_RSA'" type="warning" size="small">DES+RSA(老式JS)</el-tag>
              <el-tag v-else-if="currentRow.encryptionType === 'DES_RSA_STANDARD'" type="warning" size="small">DES+RSA(标准)</el-tag>
              <el-tag v-else-if="currentRow.encryptionType === 'AES_RSA'" type="success" size="small">AES+RSA</el-tag>
              <el-tag v-else-if="currentRow.encryptionType === 'MD5'" type="primary" size="small">MD5哈希</el-tag>
              <el-tag v-else-if="currentRow.encryptionType === 'BASE64'" type="" size="small">BASE64</el-tag>
              <el-tag v-else-if="currentRow.encryptionType === 'CUSTOM'" type="danger" size="small">自定义</el-tag>
              <span v-else>{{ currentRow.encryptionType }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建数量">{{ currentRow.accountCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总数量">{{ currentRow.totalCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已完成">{{ currentRow.completedCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="成功数">{{ currentRow.successCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="失败数">{{ currentRow.failCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="成功率">{{ getSuccessRate(currentRow) }}%</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="currentRow.status === 1" type="info">待执行</el-tag>
              <el-tag v-else-if="currentRow.status === 2" type="warning">执行中</el-tag>
              <el-tag v-else-if="currentRow.status === 3" type="success">已完成</el-tag>
              <el-tag v-else-if="currentRow.status === 4">已暂停</el-tag>
              <el-tag v-else-if="currentRow.status === 5" type="danger">失败</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentRow.createTime }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ currentRow.endTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="注册结果" name="results">
          <div style="margin-bottom: 10px">
            <el-button type="success" size="small" @click="exportResults">导出结果</el-button>
          </div>
          <el-table :data="registerResults" border stripe max-height="500">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="username" label="账号" min-width="120" show-overflow-tooltip />
            <el-table-column prop="password" label="密码" min-width="100" show-overflow-tooltip />
            <el-table-column prop="token" label="Token" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.token">{{ row.token }}</span>
                <span v-else style="color: #909399">-</span>
              </template>
            </el-table-column>
            <el-table-column label="注册状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" type="success">成功</el-tag>
                <el-tag v-else type="danger">失败</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="返回信息" min-width="180" show-overflow-tooltip />
            <el-table-column prop="registerTime" label="注册时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.registerTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 模板管理对话框 -->
    <el-dialog v-model="showTemplateDialog" title="注册模板管理" width="1200px">
      <el-table :data="templateList" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="templateName" label="模板名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="websiteUrl" label="目标网站" min-width="150" show-overflow-tooltip />
        <el-table-column prop="registerApi" label="注册接口" min-width="150" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" width="70" />
        <el-table-column prop="encryptionType" label="加密类型" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.encryptionType === 'NONE'" type="info" size="small">无加密</el-tag>
            <el-tag v-else-if="row.encryptionType === 'DES_RSA'" type="warning" size="small">DES+RSA(老式JS)</el-tag>
            <el-tag v-else-if="row.encryptionType === 'DES_RSA_STANDARD'" type="warning" size="small">DES+RSA(标准)</el-tag>
            <el-tag v-else-if="row.encryptionType === 'AES_RSA'" type="success" size="small">AES+RSA</el-tag>
            <el-tag v-else-if="row.encryptionType === 'MD5'" type="primary" size="small">MD5哈希</el-tag>
            <el-tag v-else-if="row.encryptionType === 'BASE64'" type="" size="small">BASE64</el-tag>
            <el-tag v-else-if="row.encryptionType === 'CUSTOM'" type="danger" size="small">自定义</el-tag>
            <span v-else>{{ row.encryptionType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="关键逻辑备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-text v-if="row.notes" type="warning" size="small">{{ row.notes }}</el-text>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; gap: 8px; justify-content: center;">
              <el-button link type="primary" @click="useTemplate(row)">使用</el-button>
              <el-button link type="info" @click="viewTemplateDetail(row)">详情</el-button>
              <el-button link type="danger" @click="deleteTemplate(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 添加模板对话框 -->
    <el-dialog v-model="showAddTemplateDialog" title="保存为注册模板" width="600px">
      <el-form :model="addTemplateForm" label-width="120px">
        <el-form-item label="模板名称">
          <el-input v-model="addTemplateForm.templateName" placeholder="请输入模板名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddTemplateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddTemplate" :loading="addTemplateLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 模板详情对话框 -->
    <el-dialog v-model="showTemplateDetailDialog" title="模板详情" width="700px">
      <el-descriptions :column="2" border v-if="currentTemplate">
        <el-descriptions-item label="模板名称">{{ currentTemplate.templateName }}</el-descriptions-item>
        <el-descriptions-item label="目标网站">{{ currentTemplate.websiteUrl }}</el-descriptions-item>
        <el-descriptions-item label="注册接口">{{ currentTemplate.registerApi }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentTemplate.method }}</el-descriptions-item>
        <el-descriptions-item label="用户名字段">{{ currentTemplate.usernameField }}</el-descriptions-item>
        <el-descriptions-item label="密码字段">{{ currentTemplate.passwordField }}</el-descriptions-item>
        <el-descriptions-item label="默认密码">{{ currentTemplate.defaultPassword }}</el-descriptions-item>
        <el-descriptions-item label="加密类型">{{ currentTemplate.encryptionType || 'NONE' }}</el-descriptions-item>
        <el-descriptions-item label="RSA密钥接口" :span="2">{{ currentTemplate.rsaKeyApi || '-' }}</el-descriptions-item>
        <el-descriptions-item label="加密请求头">{{ currentTemplate.encryptionHeader || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据包装字段">{{ currentTemplate.valueFieldName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="额外参数" :span="2">
          <pre v-if="currentTemplate.extraParams" style="margin:0; font-size:12px">{{ currentTemplate.extraParams }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="关键逻辑备注" :span="2">
          <el-text v-if="currentTemplate.notes" type="warning">{{ currentTemplate.notes }}</el-text>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatTime(currentTemplate.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Tickets, Memo, Upload, UploadFilled, Document, StarFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const queryParams = reactive({
  taskName: '',
  status: null,
  pageNum: 1,
  pageSize: 10
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const showCreateDialog = ref(false)
const detailVisible = ref(false)
const currentRow = ref(null)
const submitLoading = ref(false)
const activeTab = ref('basic')
const currentStep = ref(0)
const registerFormRef = ref(null)
const route = useRoute()
const router = useRouter()
const goToDraft = () => {
  router.push({ path: '/business/draft' })
}

const registerForm = reactive({
  taskName: '',
  websiteUrl: '',
  registerApi: '',
  method: 'POST',
  usernameField: 'username',
  passwordField: 'password',
  defaultPassword: '',
  extraParams: '',
  encryptionType: 'NONE',
  rsaKeyApi: '',
  rsaTsParam: 't',
  encryptionHeader: '',
  valueFieldName: '',
  dupMsgSubstring: '',
  md5Salt: '',
  md5Fields: ['password'],
  successCheckType: 'duplicate',
  successMessage: '',
  useProxy: false,
  proxyPoolId: null,
  proxyGroupName: '',  // 新增：代理分组名称
  concurrency: 5,
  needPhone: false,
  manualPhone: '',
  accountCount: 50
})

const registerRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  websiteUrl: [
    { required: true, message: '请输入目标网站', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL', trigger: 'blur' }
  ],
  registerApi: [{ required: true, message: '请输入注册接口', trigger: 'blur' }],
  usernameField: [{ required: true, message: '请输入用户名字段', trigger: 'blur' }],
  passwordField: [{ required: true, message: '请输入密码字段', trigger: 'blur' }],
  defaultPassword: [{ required: true, message: '请输入默认密码', trigger: 'blur' }],
  accountCount: [{ required: true, message: '请设置创建数量', trigger: 'change' }]
}

const proxyPools = ref([])
const proxyGroups = ref([])  // 新增：代理分组列表
const proxySelectMode = ref('pool')  // 新增：选择模式，pool=按节点, group=按分组
const registerResults = ref([])
const showTemplateDialog = ref(false)
const showDraftDialog = ref(false)
const showUploadDialog = ref(false)
const uploadLoading = ref(false)
const uploadRef = ref(null)
const uploadForm = reactive({
  draftName: '',
  websiteUrl: ''
})
const templateList = ref([])
const draftList = ref([])

const fetchDraftList = async () => {
  try {
    const res = await request.get('/business/draft/list', { 
      params: { pageNum: 1, pageSize: 100 },
      headers: { 'X-Silent-Error': 'true' } // 静默失败，不显示错误提示
    })
    draftList.value = res.data?.records || []
  } catch (error) {
    // 草稿箱功能暂未实现，静默失败
    draftList.value = []
  }
}

const handleScriptUpload = async (file) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('draftName', file.name)
    formData.append('websiteUrl', registerForm.websiteUrl || '')
    formData.append('description', '自动上传脚本')
    await request.post('/business/draft/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('脚本已上传到草稿箱')
    fetchDraftList()
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  }
  return false
}

const submitUpload = async () => {
  if (!uploadForm.websiteUrl) {
    ElMessage.warning('请输入目标站URL')
    return
  }
  if (!uploadRef.value || !uploadRef.value.uploadFiles || uploadRef.value.uploadFiles.length === 0) {
    ElMessage.warning('请选择脚本文件')
    return
  }
  uploadLoading.value = true
  try {
    const fileObj = uploadRef.value.uploadFiles[0]
    const formData = new FormData()
    formData.append('file', fileObj.raw)
    formData.append('draftName', uploadForm.draftName || fileObj.name)
    formData.append('websiteUrl', uploadForm.websiteUrl)
    formData.append('description', '手动上传脚本')
    const res = await request.post('/business/draft/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (res.code === 200) {
      ElMessage.success(res.message || '上传成功')
      showUploadDialog.value = false
      uploadForm.draftName = ''
      uploadForm.websiteUrl = ''
      fetchDraftList()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

const handleTestDraft = async (draft) => {
  try {
    const res = await request.post(`/business/draft/test/${draft.id}`)
    const data = res.data || {}
    draft.testResult = data.success ? 1 : 2
    draft.testToken = data.token || null
    if (draft.testResult === 1 && draft.testToken) {
      ElMessage.success('测试成功，检测到token')
    } else {
      ElMessage.info('测试完成，未检测到token')
    }
  } catch (error) {
    ElMessage.error(error.message || '测试失败')
  }
}

const handleSaveDraftToTemplate = async (draft) => {
  try {
    const templateName = `${draft.draftName}_模板`
    await request.post('/business/register/template/add-from-draft', { draftId: draft.id, templateName })
    ElMessage.success('已保存为模板')
    fetchTemplateList()
  } catch (error) {
    ElMessage.error(error.message || '保存模板失败')
  }
}

const handleDeleteDraft = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该草稿吗？', '提示', { type: 'warning' })
    await request.post(`/business/draft/delete/${id}`)
    ElMessage.success('删除成功')
    fetchDraftList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleClearDrafts = async () => {
  try {
    if (!draftList.value.length) return
    await ElMessageBox.confirm('确定清空所有草稿吗？', '提示', { type: 'warning' })
    for (const d of draftList.value) {
      await request.post(`/business/draft/delete/${d.id}`)
    }
    ElMessage.success('已清空草稿箱')
    fetchDraftList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('清空失败')
  }
}
const showAddTemplateDialog = ref(false)
const addTemplateLoading = ref(false)
const addTemplateForm = reactive({ templateName: '', taskId: null })
const currentTemplate = ref(null)
const showTemplateDetailDialog = ref(false)
const selectedTemplateId = ref(null)
const templateLoaded = ref(false)
const loadedTemplateName = ref('')
let timer = null

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/business/register/list', { params: queryParams })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.taskName = ''
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

// 新增：获取代理分组列表
const fetchProxyGroups = async () => {
  try {
    const res = await request.get('/proxy/groups/detail')
    proxyGroups.value = res.data || []
    console.log('📊 [代理分组] 获取分组列表:', proxyGroups.value)
  } catch (error) {
    console.error('获取代理分组失败', error)
    proxyGroups.value = []
  }
}

// 新增：处理分组选择变化
const handleGroupChange = (groupName) => {
  console.log('🔍 [代理分组] 选中分组:', groupName)
  if (groupName) {
    // 清空之前选中的单个代理
    registerForm.proxyPoolId = null
    const selectedGroup = proxyGroups.value.find(g => g.groupName === groupName)
    if (selectedGroup) {
      console.log(`  └─ 分组信息: ${selectedGroup.nodeCount} 个代理节点`)
    }
  }
}

const nextStep = async () => {
  try {
    await registerFormRef.value.validate()
    currentStep.value++
  } catch (error) {
    console.log('验证失败', error)
  }
}

const handleSubmit = async () => {
  await registerFormRef.value.validate()
  submitLoading.value = true
  try {
    await request.post('/business/register/create', registerForm)
    ElMessage.success('注册任务已创建并启动')
    showCreateDialog.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '创建任务失败')
  } finally {
    submitLoading.value = false
  }
}

const handleStart = async (row) => {
  try {
    await request.post(`/business/register/start/${row.id}`)
    ElMessage.success('任务已启动')
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '启动失败')
  }
}

const handlePause = async (row) => {
  try {
    await ElMessageBox.confirm('确定要暂停该任务吗?', '提示', { type: 'warning' })
    await request.post(`/business/register/pause/${row.id}`)
    ElMessage.success('任务已暂停')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '暂停失败')
  }
}

const handleResume = async (row) => {
  try {
    await request.post(`/business/register/resume/${row.id}`)
    ElMessage.success('任务已继续')
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '继续失败')
  }
}

const viewTemplateDetail = async (row) => {
  try {
    const res = await request.get(`/business/register/template/${row.id}`)
    currentTemplate.value = res.data
    showTemplateDetailDialog.value = true
  } catch (error) {
    ElMessage.error('获取模板详情失败')
  }
}


  const handleViewDetail = async (row) => {
  try {
    const res = await request.get(`/business/register/${row.id}`)
    currentRow.value = res.data
    const resultRes = await request.get(`/business/register/results/${row.id}`)
    registerResults.value = resultRes.data || []
    activeTab.value = 'basic'
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该注册任务吗?', '提示', { type: 'warning' })
    await request.delete(`/business/register/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const exportResults = () => {
  if (!registerResults.value || registerResults.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  // 只导出token值，每行一个
  let content = ''
  registerResults.value.forEach(item => {
    if (item.token) {
      content += `${item.token}\n`
    }
  })
  if (!content) {
    ElMessage.warning('没有可用的Token')
    return
  }
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `tokens_${currentRow.value.taskName}_${new Date().getTime()}.txt`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

const getProgress = (row) => {
  if (!row.totalCount) return 0
  return Math.round((row.completedCount / row.totalCount) * 100)
}

const getProgressColor = (status) => {
  const colors = { 1: '#909399', 2: '#E6A23C', 3: '#67C23A', 4: '#909399', 5: '#F56C6C' }
  return colors[status] || '#409EFF'
}

const getSuccessRate = (row) => {
  if (!row || !row.totalCount) return 0
  return ((row.successCount / row.totalCount) * 100).toFixed(2)
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  // 统一按北京时间显示到年月日小时分
  try {
    // 如果无时区信息，按UTC解析再转本地（避免旧数据少8小时）
    const src = /Z$|[+-]\d{2}:?\d{2}$/.test(timeStr) ? timeStr : (timeStr + 'Z')
    const d = new Date(src)
    if (isNaN(d.getTime())) return timeStr
    const Y = d.getFullYear()
    const M = String(d.getMonth() + 1).padStart(2, '0')
    const D = String(d.getDate()).padStart(2, '0')
    const h = String(d.getHours()).padStart(2, '0')
    const m = String(d.getMinutes()).padStart(2, '0')
    return `${Y}-${M}-${D} ${h}:${m}`
  } catch (e) {
    return timeStr
  }
}

const resetForm = () => {
  currentStep.value = 0
  templateLoaded.value = false
  selectedTemplateId.value = null
  loadedTemplateName.value = ''
  Object.assign(registerForm, {
    taskName: '',
    websiteUrl: '',
    registerApi: '',
    method: 'POST',
    usernameField: 'username',
    passwordField: 'password',
    defaultPassword: '',
    extraParams: '',
    encryptionType: 'NONE',
    rsaKeyApi: '',
    rsaTsParam: 't',
    encryptionHeader: '',
    valueFieldName: '',
    dupMsgSubstring: '',
    md5Salt: '',
    md5Fields: ['password'],
    successCheckType: 'duplicate',
    successMessage: '',
    useProxy: false,
    proxyPoolId: null,
    concurrency: 5,
    needPhone: false,
    manualPhone: '',
    accountCount: 50
  })
  registerFormRef.value?.clearValidate()
}

const fetchTemplateList = async () => {
  try {
    const res = await request.get('/business/register/template/list', {
      headers: { 'X-Silent-Error': 'true' } // 静默失败，不显示错误提示
    })
    templateList.value = res.data || []
  } catch (error) {
    // 模板功能暂未实现，静默失败
    templateList.value = []
  }
}

const handleAddToTemplate = (row) => {
  addTemplateForm.taskId = row.id
  addTemplateForm.templateName = row.taskName + '_模板'
  showAddTemplateDialog.value = true
}

const submitAddTemplate = async () => {
  if (!addTemplateForm.templateName) {
    ElMessage.warning('请输入模板名称')
    return
  }
  addTemplateLoading.value = true
  try {
    await request.post(`/business/register/template/add-from-task/${addTemplateForm.taskId}`, {
      templateName: addTemplateForm.templateName
    })
    ElMessage.success('模板已保存')
    showAddTemplateDialog.value = false
    fetchTemplateList()
  } catch (error) {
    ElMessage.error(error.message || '保存模板失败')
  } finally {
    addTemplateLoading.value = false
  }
}

const loadTemplate = async () => {
  if (!selectedTemplateId.value) return
  try {
    const res = await request.get(`/business/register/template/${selectedTemplateId.value}`)
    const t = res.data
    if (!t) return
    Object.assign(registerForm, {
      websiteUrl: t.websiteUrl || '',
      registerApi: t.registerApi || '',
      method: t.method || 'PUT',
      usernameField: t.usernameField || 'username',
      passwordField: t.passwordField || 'password',
      defaultPassword: t.defaultPassword || '133adb',
      extraParams: t.extraParams || '',
      encryptionType: t.encryptionType || 'DES_RSA',
      rsaKeyApi: t.rsaKeyApi || '/wps/session/key/rsa',
      rsaTsParam: t.rsaTsParam || 't',
      encryptionHeader: t.encryptionHeader || 'encryption',
      valueFieldName: t.valueFieldName || 'value'
    })
    templateLoaded.value = true
    loadedTemplateName.value = t.templateName
    ElMessage.success(`已加载模板：${t.templateName}`)
  } catch (error) {
    ElMessage.error('加载模板失败')
  }
}

const clearTemplate = () => {
  templateLoaded.value = false
  selectedTemplateId.value = null
  loadedTemplateName.value = ''
  ElMessage.info('已清除模板')
}

const useTemplate = (row) => {
  selectedTemplateId.value = row.id
  loadTemplate()
  showTemplateDialog.value = false
  showCreateDialog.value = true
}

const deleteTemplate = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该模板吗？', '提示', { type: 'warning' })
    await request.post(`/business/register/template/delete/${row.id}`)
    ElMessage.success('删除成功')
    fetchTemplateList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const hasTemplateFor = (url) => {
  return templateList.value && templateList.value.some(t => t.websiteUrl === url)
}

// 从网站分析结果自动填充表单（一键填充）
const loadFromAnalysisResult = async () => {
  const analysisId = route.query.analysisId
  if (!analysisId) return
  
  try {
    console.log('[Register] 从分析结果一键填充, analysisId=', analysisId)
    const res = await request.get(`/business/analysis/register/to-task/${analysisId}`)
    const config = res.data
    if (!config) {
      ElMessage.error('分析结果为空')
      return
    }
    
    console.log('[Register] 获取到配置:', config)
    
    // 完整填充所有字段（基础+加密+执行配置）
    Object.assign(registerForm, {
      taskName: config.taskName || '',
      websiteUrl: config.websiteUrl || '',
      registerApi: config.registerApi || '',
      method: config.method || 'POST',
      usernameField: config.usernameField || 'username',
      passwordField: config.passwordField || 'password',
      defaultPassword: config.defaultPassword || '133adb',
      extraParams: config.extraParams || '',
      
      // 加密配置
      encryptionType: config.encryptionType || 'NONE',
      rsaKeyApi: config.rsaKeyApi || '/wps/session/key/rsa',
      rsaTsParam: config.rsaTsParam || 't',
      encryptionHeader: config.encryptionHeader || 'Encryption',
      valueFieldName: config.valueFieldName || 'value',
      dupMsgSubstring: config.dupMsgSubstring || '',
      
      // 执行配置
      accountCount: config.accountCount || 10,
      concurrency: config.concurrency || 5,
      useProxy: config.useProxy || false,
      proxyPoolId: config.proxyPoolId || null,
      needPhone: config.needPhone || false,
      needCaptcha: config.needCaptcha || false,
      needToken: config.needToken || false,
      autoRetry: config.autoRetry || false,
      retryTimes: config.retryTimes || 0
    })
    
    // 自动打开新建对话框
    showCreateDialog.value = true
    ElMessage.success('✨ 已根据分析结果自动填充，可按需修改')
    console.log('[Register] 一键填充完成')
  } catch (error) {
    console.error('[Register] 加载分析结果失败:', error)
    ElMessage.error('加载分析结果失败: ' + (error.message || '未知错误'))
  }
}

onMounted(() => {
  fetchData()
  fetchProxyPools()
  fetchProxyGroups()  // 新增：加载代理分组列表
  fetchTemplateList()
  fetchDraftList()
  loadFromAnalysisResult()
  timer = setInterval(() => { if (document.visibilityState === 'visible') fetchData() }, 10000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.register-container { padding: 20px; }
.progress-text { font-size: 12px; color: #909399; margin-top: 5px; text-align: center; }
.steps { margin-bottom: 30px; }
.form-content { min-height: 300px; margin-top: 20px; }
.form-tip { font-size: 12px; color: #909399; margin-top: 5px; }
.dialog-footer { display: flex; justify-content: center; gap: 10px; }
.upload-card{min-height:200px}.card-header{display:flex;align-items:center;gap:8px}.script-upload{width:100%}.draft-list{display:flex;flex-direction:column;gap:12px}.draft-item{display:flex;justify-content:space-between;align-items:center;border-bottom:1px dashed #ebeef5;padding-bottom:8px}.draft-name{font-weight:500}.draft-meta{font-size:12px;color:#909399}.draft-actions{display:flex;gap:8px}.template-list{display:flex;flex-direction:column;gap:12px}.template-item{display:flex;justify-content:space-between;align-items:center}
</style>
