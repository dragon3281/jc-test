-- 自动化数据检测平台数据库初始化脚本
-- 创建时间: 2024-11-12
-- 数据库版本: 1.0.0

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 服务器表
-- ----------------------------
DROP TABLE IF EXISTS `t_server`;
CREATE TABLE `t_server` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '服务器ID',
  `server_name` VARCHAR(100) NOT NULL COMMENT '服务器名称',
  `ip_address` VARCHAR(50) NOT NULL COMMENT 'IP地址',
  `ssh_port` INT(11) NOT NULL DEFAULT 22 COMMENT 'SSH端口',
  `ssh_username` VARCHAR(50) NOT NULL DEFAULT 'root' COMMENT 'SSH用户名',
  `auth_type` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '认证方式:1密码,2密钥',
  `auth_credential` TEXT NOT NULL COMMENT '加密后的凭证',
  `docker_port` INT(11) NULL DEFAULT NULL COMMENT 'Docker API端口',
  `status` TINYINT(4) NOT NULL DEFAULT 2 COMMENT '状态:1在线,2关机,3异常',
  `cpu_usage` DECIMAL(5,2) NULL DEFAULT NULL COMMENT 'CPU使用率',
  `memory_usage` DECIMAL(5,2) NULL DEFAULT NULL COMMENT '内存使用率',
  `disk_usage` DECIMAL(5,2) NULL DEFAULT NULL COMMENT '磁盘使用率',
  `network_in` BIGINT NULL DEFAULT NULL COMMENT '网络入流量(KB/s)',
  `network_out` BIGINT NULL DEFAULT NULL COMMENT '网络出流量(KB/s)',
  `max_concurrent` INT(11) NOT NULL DEFAULT 10 COMMENT '最大并发数',
  `current_tasks` INT(11) NOT NULL DEFAULT 0 COMMENT '当前任务数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_heartbeat_time` DATETIME NULL DEFAULT NULL COMMENT '最后心跳时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_server_status` (`status`, `last_heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器表';

-- ----------------------------
-- 2. 代理池表
-- ----------------------------
DROP TABLE IF EXISTS `t_proxy_pool`;
CREATE TABLE `t_proxy_pool` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '代理池ID',
  `pool_name` VARCHAR(100) NOT NULL COMMENT '代理池名称',
  `proxy_ip` VARCHAR(50) NOT NULL COMMENT '代理IP地址',
  `proxy_port` INT(11) NOT NULL COMMENT '代理端口',
  `proxy_type` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '代理类型:1HTTP,2HTTPS,3SOCKS5',
  `auth_type` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '认证方式:0无,1用户名密码',
  `username` VARCHAR(100) NULL DEFAULT NULL COMMENT '用户名',
  `password` VARCHAR(255) NULL DEFAULT NULL COMMENT '加密密码',
  `description` TEXT NULL COMMENT '描述',
  `status` TINYINT(4) NOT NULL DEFAULT 3 COMMENT '状态:1可用,2不可用,3未检测',
  `health_score` INT(11) NOT NULL DEFAULT 100 COMMENT '健康度0-100',
  `use_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '使用次数',
  `success_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '成功次数',
  `fail_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '失败次数',
  `response_time` INT(11) NULL DEFAULT NULL COMMENT '最近响应时间(毫秒)',
  `avg_response_time` INT(11) NULL DEFAULT NULL COMMENT '平均响应时间(毫秒)',
  `last_check_time` DATETIME NULL DEFAULT NULL COMMENT '最后检测时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`, `health_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理池表';

-- ----------------------------
-- 3. 代理节点表
-- ----------------------------
DROP TABLE IF EXISTS `t_proxy_node`;
CREATE TABLE `t_proxy_node` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '代理ID',
  `pool_id` BIGINT(20) NOT NULL COMMENT '所属代理池ID',
  `proxy_address` VARCHAR(100) NOT NULL COMMENT '代理地址(IP:端口)',
  `proxy_type` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '代理类型:1HTTP,2HTTPS,3SOCKS5',
  `auth_type` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '认证方式:0无,1用户名密码',
  `username` VARCHAR(100) NULL DEFAULT NULL COMMENT '用户名',
  `password` VARCHAR(255) NULL DEFAULT NULL COMMENT '加密密码',
  `region` VARCHAR(50) NULL DEFAULT NULL COMMENT '地区',
  `isp` VARCHAR(50) NULL DEFAULT NULL COMMENT '运营商',
  `status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '状态:1可用,2不可用,3检测中',
  `health_score` INT(11) NOT NULL DEFAULT 100 COMMENT '健康度0-100',
  `use_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '使用次数',
  `success_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '成功次数',
  `fail_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '失败次数',
  `response_time` INT(11) NULL DEFAULT NULL COMMENT '响应时间(毫秒)',
  `avg_response_time` INT(11) NULL DEFAULT NULL COMMENT '平均响应时间(毫秒)',
  `last_check_time` DATETIME NULL DEFAULT NULL COMMENT '最后检测时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_pool_status` (`pool_id`, `status`, `health_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理节点表';

-- ----------------------------
-- 4. 基础数据表
-- ----------------------------
DROP TABLE IF EXISTS `t_base_data`;
CREATE TABLE `t_base_data` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '数据ID',
  `data_value` VARCHAR(255) NOT NULL COMMENT '数据值',
  `data_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '数据类型（用户自定义：账号/密码/token等）',
  `country` VARCHAR(50) NULL DEFAULT NULL COMMENT '国家',
  `account_identifier` VARCHAR(255) NULL DEFAULT NULL COMMENT '账号标识（兼容字段）',
  `account_type` TINYINT(4) NULL DEFAULT NULL COMMENT '账号类型:1邮箱,2手机,3用户名（兼容字段）',
  `data_source` VARCHAR(100) NULL DEFAULT NULL COMMENT '数据来源',
  `import_batch` VARCHAR(50) NULL DEFAULT NULL COMMENT '导入批次',
  `import_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  `remark` TEXT NULL COMMENT '备注',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_data_type_country` (`data_type`, `country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基础数据表';

-- ----------------------------
-- 5. POST模板表
-- ----------------------------
DROP TABLE IF EXISTS `t_post_template`;
CREATE TABLE `t_post_template` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `target_site` VARCHAR(255) NOT NULL COMMENT '目标站',
  `request_url` VARCHAR(500) NOT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) NOT NULL DEFAULT 'POST' COMMENT '请求方法',
  `request_headers` JSON NULL COMMENT '请求头模板',
  `request_body` JSON NULL COMMENT '请求体模板',
  `success_rule` JSON NOT NULL COMMENT '成功判断规则',
  `fail_rule` JSON NOT NULL COMMENT '失败判断规则',
  `duplicate_msg` VARCHAR(255) NULL DEFAULT NULL COMMENT '重复手机号关键字(如:customer_mobile_no_duplicated)',
  `token_header` VARCHAR(100) NULL DEFAULT 'Authorization' COMMENT 'Token请求头名称',
  `phone_field` VARCHAR(100) NULL DEFAULT 'mobile' COMMENT '手机号字段名',
  `enable_proxy` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '是否启用代理:0否,1是',
  `timeout_seconds` INT(11) NOT NULL DEFAULT 30 COMMENT '超时时间(秒)',
  `retry_count` INT(11) NOT NULL DEFAULT 3 COMMENT '重试次数',
  `version` VARCHAR(20) NULL DEFAULT NULL COMMENT '版本号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_target_site` (`target_site`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='POST模板表';

-- ----------------------------
-- 5-1. 手机号检测任务表
-- ----------------------------
DROP TABLE IF EXISTS `t_phone_check_task`;
CREATE TABLE `t_phone_check_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `template_id` BIGINT(20) NOT NULL COMMENT '模板ID',
  `token_value` VARCHAR(500) NULL DEFAULT NULL COMMENT 'Token值',
  `cookie_value` TEXT NULL DEFAULT NULL COMMENT 'Cookie值',
  `phone_list` LONGTEXT NOT NULL COMMENT '手机号列表(每行一个)',
  `total_count` INT(11) NOT NULL DEFAULT 0 COMMENT '总数量',
  `checked_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已检测数量',
  `duplicate_count` INT(11) NOT NULL DEFAULT 0 COMMENT '重复数量',
  `task_status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '任务状态:1待执行,2执行中,3已完成,4已停止',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `start_time` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status_create_time` (`task_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手机号检测任务表';

-- ----------------------------
-- 5-2. 手机号检测结果表
-- ----------------------------
DROP TABLE IF EXISTS `t_phone_check_result`;
CREATE TABLE `t_phone_check_result` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `task_id` BIGINT(20) NOT NULL COMMENT '任务ID',
  `phone_number` VARCHAR(20) NOT NULL COMMENT '手机号',
  `is_duplicate` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否重复:0未重复,1已重复',
  `response_code` INT(11) NULL DEFAULT NULL COMMENT '响应状态码',
  `response_message` TEXT NULL DEFAULT NULL COMMENT '响应消息',
  `check_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_is_duplicate` (`is_duplicate`),
  KEY `idx_phone_number` (`phone_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手机号检测结果表';

-- ----------------------------
-- 6. 检测任务表
-- ----------------------------
DROP TABLE IF EXISTS `t_detection_task`;
CREATE TABLE `t_detection_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `target_site` VARCHAR(255) NOT NULL COMMENT '目标站',
  `template_id` BIGINT(20) NOT NULL COMMENT '模板ID',
  `proxy_pool_id` BIGINT(20) NULL DEFAULT NULL COMMENT '代理池ID',
  `task_status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '状态:1待执行,2执行中,3已暂停,4已完成,5失败,6已停止',
  `priority` TINYINT(4) NOT NULL DEFAULT 2 COMMENT '优先级:1高,2中,3低',
  `total_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '总数据量',
  `completed_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '已完成数量',
  `success_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '成功数量',
  `fail_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '失败数量',
  `concurrent_num` INT(11) NOT NULL DEFAULT 10 COMMENT '并发数',
  `progress_percent` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '进度百分比',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `start_time` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `estimate_remaining_seconds` BIGINT(20) NULL DEFAULT NULL COMMENT '预计剩余秒数',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_create_time` (`task_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测任务表';

-- ----------------------------
-- 7. 任务服务器关联表
-- ----------------------------
DROP TABLE IF EXISTS `t_task_server`;
CREATE TABLE `t_task_server` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `task_id` BIGINT(20) NOT NULL COMMENT '任务ID',
  `server_id` BIGINT(20) NOT NULL COMMENT '服务器ID',
  `assigned_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '分配数据量',
  `completed_count` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '已完成数量',
  `exec_status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '执行状态:1执行中,2已完成,3失败',
  PRIMARY KEY (`id`),
  KEY `idx_task_server` (`task_id`, `server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务服务器关联表';

-- ----------------------------
-- 8. 检测结果表(最新数据-分区表)
-- ----------------------------
DROP TABLE IF EXISTS `t_detection_result`;
CREATE TABLE `t_detection_result` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `seq_no` INT(11) NOT NULL COMMENT '序号',
  `task_id` BIGINT(20) NOT NULL COMMENT '任务ID',
  `account_identifier` VARCHAR(255) NOT NULL COMMENT '账号标识',
  `data_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '数据类型（用户自定义）',
  `country` VARCHAR(50) NULL DEFAULT NULL COMMENT '国家',
  `target_site` VARCHAR(255) NOT NULL COMMENT '目标站',
  `detect_status` TINYINT(4) NOT NULL COMMENT '状态:1已注册,2未注册,3检测失败,4账号异常,5代理异常',
  `response_time` INT(11) NULL DEFAULT NULL COMMENT '响应时间(毫秒)',
  `used_proxy` VARCHAR(100) NULL DEFAULT NULL COMMENT '使用的代理',
  `exec_server` VARCHAR(50) NULL DEFAULT NULL COMMENT '执行服务器IP',
  `response_detail` JSON NULL COMMENT '响应详情',
  `error_message` TEXT NULL COMMENT '错误信息',
  `detect_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  `is_archived` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已归档:0否,1是',
  PRIMARY KEY (`id`, `detect_time`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_account` (`account_identifier`(100)),
  KEY `idx_detect_time` (`detect_time`),
  KEY `idx_archived` (`is_archived`, `detect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测结果表(最新数据)'
PARTITION BY RANGE (TO_DAYS(`detect_time`)) (
  PARTITION p202411 VALUES LESS THAN (TO_DAYS('2024-12-01')),
  PARTITION p202412 VALUES LESS THAN (TO_DAYS('2025-01-01')),
  PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
  PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ----------------------------
-- 8-1. 检测结果历史表
-- ----------------------------
DROP TABLE IF EXISTS `t_detection_result_history`;
CREATE TABLE `t_detection_result_history` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `seq_no` INT(11) NOT NULL COMMENT '序号',
  `task_id` BIGINT(20) NOT NULL COMMENT '任务ID',
  `account_identifier` VARCHAR(255) NOT NULL COMMENT '账号标识',
  `data_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '数据类型（用户自定义）',
  `country` VARCHAR(50) NULL DEFAULT NULL COMMENT '国家',
  `target_site` VARCHAR(255) NOT NULL COMMENT '目标站',
  `detect_status` TINYINT(4) NOT NULL COMMENT '状态:1已注册,2未注册,3检测失败,4账号异常,5代理异常',
  `response_time` INT(11) NULL DEFAULT NULL COMMENT '响应时间(毫秒)',
  `used_proxy` VARCHAR(100) NULL DEFAULT NULL COMMENT '使用的代理',
  `exec_server` VARCHAR(50) NULL DEFAULT NULL COMMENT '执行服务器IP',
  `response_detail` JSON NULL COMMENT '响应详情',
  `error_message` TEXT NULL COMMENT '错误信息',
  `detect_time` DATETIME NOT NULL COMMENT '检测时间',
  `archive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_account` (`account_identifier`(100)),
  KEY `idx_detect_time` (`detect_time`),
  KEY `idx_archive_time` (`archive_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测结果历史表';

-- ----------------------------
-- 9. 用户表(单用户系统)
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname` VARCHAR(100) NULL DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号',
  `status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '状态:0禁用,1正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 10. 网站分析表
-- ----------------------------
DROP TABLE IF EXISTS `t_website_analysis`;
CREATE TABLE `t_website_analysis` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `website_url` VARCHAR(500) NOT NULL COMMENT '网站地址',
  `ports` VARCHAR(200) NULL DEFAULT NULL COMMENT '检测端口(多个用逗号分隔)',
  `detected_port` VARCHAR(50) NULL DEFAULT NULL COMMENT '检测到的端口',
  `api_paths` TEXT NULL COMMENT '接口路径列表(JSON数组)',
  `api_type` TINYINT(4) NULL DEFAULT NULL COMMENT '接口类型:1-JSON,2-XML,3-HTML',
  `status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '分析状态:1-分析中,2-已完成,3-失败',
  `timeout` INT(11) NOT NULL DEFAULT 30 COMMENT '超时时间(秒)',
  `use_proxy` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用代理',
  `proxy_pool_id` BIGINT(20) NULL DEFAULT NULL COMMENT '代理池ID',
  `detected_apis` TEXT NULL COMMENT '检测到的接口列表(JSON)',
  `analysis_result` TEXT NULL COMMENT '分析结果(JSON)',
  `error_message` TEXT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finish_time` DATETIME NULL DEFAULT NULL COMMENT '完成时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_website_url` (`website_url`(100)),
  KEY `idx_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站分析表';

-- ----------------------------
-- 11. 自动化注册任务表
-- ----------------------------
DROP TABLE IF EXISTS `t_register_task`;
CREATE TABLE `t_register_task` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `website_url` VARCHAR(500) NOT NULL COMMENT '目标网站',
  `register_api` VARCHAR(500) NOT NULL COMMENT '注册接口',
  `method` VARCHAR(10) NOT NULL DEFAULT 'POST' COMMENT '请求方法',
  `username_field` VARCHAR(100) NOT NULL COMMENT '用户名字段',
  `password_field` VARCHAR(100) NOT NULL COMMENT '密码字段',
  `email_field` VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱字段',
  `phone_field` VARCHAR(100) NULL DEFAULT NULL COMMENT '手机号字段',
  `default_password` VARCHAR(255) NULL DEFAULT NULL COMMENT '默认密码',
  `extra_params` TEXT NULL COMMENT '额外参数(JSON)',
  `need_captcha` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要验证码',
  `captcha_type` TINYINT(4) NULL DEFAULT NULL COMMENT '验证码类型:1-图形,2-短信,3-邮箱',
  `captcha_api` VARCHAR(500) NULL DEFAULT NULL COMMENT '验证码接口',
  `captcha_field` VARCHAR(100) NULL DEFAULT NULL COMMENT '验证码字段',
  `ocr_method` TINYINT(4) NULL DEFAULT NULL COMMENT 'OCR识别方式:1-OCR,2-打码平台,3-跳过',
  `need_token` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要Token',
  `token_field` VARCHAR(100) NULL DEFAULT NULL COMMENT 'Token字段',
  `token_source` TINYINT(4) NULL DEFAULT NULL COMMENT 'Token来源:1-Header,2-Body,3-Cookie',
  `data_source_id` BIGINT(20) NULL DEFAULT NULL COMMENT '数据源ID',
  `use_proxy` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用代理',
  `proxy_pool_id` BIGINT(20) NULL DEFAULT NULL COMMENT '代理池ID',
  `concurrency` INT(11) NOT NULL DEFAULT 5 COMMENT '并发数',
  `auto_retry` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '自动重试',
  `retry_times` INT(11) NOT NULL DEFAULT 3 COMMENT '重试次数',
  `encryption_type` VARCHAR(50) NULL DEFAULT 'NONE' COMMENT '加密类型:NONE-无加密,DES_RSA-DES+RSA双重加密',
  `rsa_key_api` VARCHAR(500) NULL DEFAULT NULL COMMENT 'RSA密钥接口',
  `rsa_ts_param` VARCHAR(50) NULL DEFAULT 't' COMMENT 'RSA时间戳参数名',
  `encryption_header` VARCHAR(100) NULL DEFAULT 'encryption' COMMENT '加密请求头名称',
  `value_field_name` VARCHAR(100) NULL DEFAULT 'value' COMMENT '数据包装字段名',
  `dup_msg_substring` VARCHAR(500) NULL DEFAULT NULL COMMENT '重复用户名提示(用于验证成功)',
  `need_phone` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要手机号',
  `manual_phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '手动手机号(选填)',
  `account_count` INT(11) NULL DEFAULT 50 COMMENT '创建数量',
  `status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '任务状态:1-待执行,2-执行中,3-已完成,4-已暂停,5-失败',
  `total_count` INT(11) NOT NULL DEFAULT 0 COMMENT '总数量',
  `completed_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已完成数量',
  `success_count` INT(11) NOT NULL DEFAULT 0 COMMENT '成功数量',
  `fail_count` INT(11) NOT NULL DEFAULT 0 COMMENT '失败数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `start_time` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `deleted` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删除,1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_name` (`task_name`(100)),
  KEY `idx_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化注册任务表';

-- ----------------------------
-- 初始化默认管理员账号
-- 用户名: admin
-- 密码: admin123 (BCrypt加密后)
-- ----------------------------
INSERT INTO `t_user` (`username`, `password`, `nickname`, `status`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1);

SET FOREIGN_KEY_CHECKS = 1;

