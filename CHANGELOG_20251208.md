# 更新日志 - 2025-12-08

## 修改内容

### 1. 移除速率探测功能

**前端修改** (`/root/jc-test/frontend/src/views/business/Template.vue`):
- 移除运行对话框中的速率探测步骤显示
- 移除详情对话框中的"当前速率"和"最优并发数"字段
- 简化运行流程为：上传文件 → 批量检测 → 完成

**影响范围**:
- 用户界面更简洁，减少了中间步骤
- 系统仍保留自适应限流功能，只是不再显示速率探测界面

### 2. 下载文件格式改为TXT

**后端修改** (`/root/jc-test/backend/src/main/java/com/detection/platform/controller/PostTemplateController.java`):
- 修改 `/template/detect/export/{taskId}` 接口
- 导出格式从CSV改为TXT
- TXT格式：每行一个手机号（仅导出未注册号码）
- 文件名从 `detect_xxx.csv` 改为 `detect_xxx.txt`

**前端修改** (`/root/jc-test/frontend/src/views/business/Template.vue`):
- 下载时MIME类型从 `text/csv` 改为 `text/plain`
- 保持与后端文件格式一致

**示例输出**:
```
13800138000
13800138001
13800138002
```

## 部署说明

1. 服务已重启并正常运行
   - 前端地址: http://103.246.246.4:3000/
   - 后端API: http://localhost:8080

2. Docker服务正常
   - MySQL
   - Redis
   - RabbitMQ

3. 修改即时生效，无需数据库迁移

## 测试建议

1. 访问POST模板页面
2. 创建任务并运行
3. 检查详情对话框是否已移除速率相关字段
4. 下载完成的任务结果，验证文件格式为TXT
5. 确认TXT文件内容为每行一个手机号

## 回滚方案

如需回滚，可使用Git还原以下文件：
- `/root/jc-test/frontend/src/views/business/Template.vue`
- `/root/jc-test/backend/src/main/java/com/detection/platform/controller/PostTemplateController.java`

---
更新时间: 2025-12-08 08:42
更新人员: AI Assistant
