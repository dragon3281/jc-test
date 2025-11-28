-- ============================================================================
-- 执行器 + 草稿箱 + 网站分析功能升级脚本
-- 创建时间: 2025-11-25
-- 功能: 支持自定义执行器、草稿箱、网站分析模块
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 执行器库表 (预置常见加密执行器)
-- ============================================================================
DROP TABLE IF EXISTS `t_encryption_executor`;
CREATE TABLE `t_encryption_executor` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '执行器ID',
  `executor_name` VARCHAR(100) NOT NULL COMMENT '执行器名称',
  `executor_type` VARCHAR(50) NOT NULL COMMENT '加密类型: DES_RSA_OLD_JS, DES_RSA_STANDARD, AES_RSA, MD5, CUSTOM',
  `script_language` VARCHAR(20) NOT NULL DEFAULT 'PYTHON' COMMENT '脚本语言: PYTHON, JAVASCRIPT, JAVA',
  `script_path` VARCHAR(500) NULL COMMENT '脚本文件路径',
  `script_content` LONGTEXT NULL COMMENT '脚本内容(自定义上传时使用)',
  `encryption_config` JSON NULL COMMENT '加密配置JSON',
  `is_builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置: 0自定义, 1内置',
  `description` TEXT NULL COMMENT '执行器描述',
  `version` VARCHAR(20) NULL DEFAULT '1.0.0' COMMENT '版本号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_executor_type` (`executor_type`, `deleted`),
  KEY `idx_is_builtin` (`is_builtin`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加密执行器库';

-- ============================================================================
-- 2. 注册模板草稿箱表
-- ============================================================================
DROP TABLE IF EXISTS `t_register_template_draft`;
CREATE TABLE `t_register_template_draft` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '草稿ID',
  `draft_name` VARCHAR(100) NOT NULL COMMENT '草稿名称',
  `website_url` VARCHAR(255) NOT NULL COMMENT '目标网站',
  `register_api` VARCHAR(255) NOT NULL COMMENT '注册接口',
  `method` VARCHAR(10) NOT NULL DEFAULT 'POST' COMMENT '请求方法',
  `username_field` VARCHAR(50) NULL COMMENT '用户名字段',
  `password_field` VARCHAR(50) NULL COMMENT '密码字段',
  `default_password` VARCHAR(50) NULL COMMENT '默认密码',
  `extra_params` TEXT NULL COMMENT '额外参数JSON',
  `encryption_type` VARCHAR(50) NULL COMMENT '加密类型',
  `executor_id` BIGINT(20) NULL COMMENT '关联执行器ID',
  `executor_script` LONGTEXT NULL COMMENT '自定义执行脚本',
  `rsa_key_api` VARCHAR(255) NULL COMMENT 'RSA密钥接口',
  `rsa_ts_param` VARCHAR(50) NULL COMMENT 'RSA时间戳参数名',
  `encryption_header` VARCHAR(50) NULL COMMENT '加密头名称',
  `value_field_name` VARCHAR(50) NULL COMMENT '加密数据字段名',
  `test_result` TINYINT(1) NULL DEFAULT 0 COMMENT '测试结果: 0未测试, 1成功, 2失败',
  `test_token` TEXT NULL COMMENT '测试获取的token',
  `test_error` TEXT NULL COMMENT '测试失败原因',
  `auto_notes` TEXT NULL COMMENT '自动分析生成的备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_website_url` (`website_url`, `deleted`),
  KEY `idx_test_result` (`test_result`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册模板草稿箱';

-- ============================================================================
-- 3. 扩展注册模板表 (增加执行器关联)
-- ============================================================================
ALTER TABLE `t_register_template` 
  ADD COLUMN `executor_id` BIGINT(20) NULL COMMENT '关联执行器ID' AFTER `encryption_type`,
  ADD COLUMN `executor_script_path` VARCHAR(500) NULL COMMENT '执行脚本路径' AFTER `executor_id`,
  ADD COLUMN `encryption_config` JSON NULL COMMENT '加密配置JSON' AFTER `executor_script_path`,
  ADD KEY `idx_executor_id` (`executor_id`);

-- ============================================================================
-- 4. 网站分析表 (重构为支持两种分析类型)
-- ============================================================================
DROP TABLE IF EXISTS `t_website_analysis`;
CREATE TABLE `t_website_analysis` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '分析ID',
  `analysis_type` VARCHAR(20) NOT NULL COMMENT '分析类型: AUTO_REGISTER(自动化注册), NUMBER_CHECK(号码检测)',
  `website_url` VARCHAR(255) NOT NULL COMMENT '目标网站',
  `website_name` VARCHAR(100) NULL COMMENT '网站名称',
  `analysis_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '分析状态: 1分析中, 2成功, 3失败',
  
  -- 自动化注册分析结果
  `register_api` VARCHAR(255) NULL COMMENT '注册接口',
  `register_method` VARCHAR(10) NULL COMMENT '注册方法',
  `encryption_type` VARCHAR(50) NULL COMMENT '检测到的加密类型',
  `rsa_key_api` VARCHAR(255) NULL COMMENT 'RSA密钥接口',
  `request_headers` JSON NULL COMMENT '分析得到的请求头',
  `required_fields` JSON NULL COMMENT '必填字段列表',
  `suggested_executor_id` BIGINT(20) NULL COMMENT '推荐的执行器ID',
  `auto_generated_script` LONGTEXT NULL COMMENT '自动生成的执行脚本',
  
  -- 号码检测分析结果 (预留)
  `check_api` VARCHAR(255) NULL COMMENT '检测接口',
  `check_method` VARCHAR(10) NULL COMMENT '检测方法',
  
  -- 通用字段
  `analysis_result` JSON NULL COMMENT '完整分析结果JSON',
  `error_message` TEXT NULL COMMENT '分析错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `complete_time` DATETIME NULL COMMENT '完成时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_website_type` (`website_url`, `analysis_type`, `deleted`),
  KEY `idx_status` (`analysis_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站分析表';

-- ============================================================================
-- 5. 预置常见执行器数据
-- ============================================================================

-- DES+RSA 老式JS-RSA (wwwtk666.com专用)
INSERT INTO `t_encryption_executor` 
  (`executor_name`, `executor_type`, `script_language`, `script_path`, `encryption_config`, `is_builtin`, `description`) 
VALUES 
  ('DES+RSA老式JS-RSA', 'DES_RSA_OLD_JS', 'PYTHON', '/root/jc-test/template_scripts/des_rsa_old_js_executor.py', 
   JSON_OBJECT(
     'des', JSON_OBJECT('mode', 'ECB', 'padding', 'PKCS5', 'keySource', 'rnd_reversed', 'keyLength', 8),
     'rsa', JSON_OBJECT('library', 'old_js_rsa', 'encryptTarget', 'original_rnd', 'outputFormat', 'hex'),
     'requestFormat', JSON_OBJECT('headerKey', 'encryption', 'bodyWrapper', 'value', 'contentType', 'application/json; charset=utf-8')
   ), 
   1, '适用于使用老式JS-RSA库的网站，如wwwtk666.com');

-- DES+RSA 标准PKCS1
INSERT INTO `t_encryption_executor` 
  (`executor_name`, `executor_type`, `script_language`, `script_path`, `encryption_config`, `is_builtin`, `description`) 
VALUES 
  ('DES+RSA标准', 'DES_RSA_STANDARD', 'PYTHON', '/root/jc-test/template_scripts/des_rsa_standard_executor.py', 
   JSON_OBJECT(
     'des', JSON_OBJECT('mode', 'ECB', 'padding', 'PKCS5', 'keyLength', 8),
     'rsa', JSON_OBJECT('library', 'standard', 'padding', 'PKCS1', 'outputFormat', 'base64')
   ), 
   1, '使用标准DES+RSA加密的网站');

-- AES+RSA
INSERT INTO `t_encryption_executor` 
  (`executor_name`, `executor_type`, `script_language`, `script_path`, `encryption_config`, `is_builtin`, `description`) 
VALUES 
  ('AES+RSA', 'AES_RSA', 'PYTHON', '/root/jc-test/template_scripts/aes_rsa_executor.py', 
   JSON_OBJECT(
     'aes', JSON_OBJECT('mode', 'CBC', 'padding', 'PKCS7', 'keyLength', 16),
     'rsa', JSON_OBJECT('library', 'standard', 'padding', 'PKCS1', 'outputFormat', 'base64')
   ), 
   1, '使用AES+RSA加密的网站');

-- MD5 (简单hash)
INSERT INTO `t_encryption_executor` 
  (`executor_name`, `executor_type`, `script_language`, `script_path`, `encryption_config`, `is_builtin`, `description`) 
VALUES 
  ('MD5哈希', 'MD5', 'PYTHON', '/root/jc-test/template_scripts/md5_executor.py', 
   JSON_OBJECT('hashAlgorithm', 'MD5', 'salt', ''), 
   1, '使用MD5哈希的网站');

-- 无加密
INSERT INTO `t_encryption_executor` 
  (`executor_name`, `executor_type`, `script_language`, `script_path`, `encryption_config`, `is_builtin`, `description`) 
VALUES 
  ('无加密', 'NONE', 'PYTHON', '/root/jc-test/template_scripts/none_executor.py', 
   JSON_OBJECT('type', 'NONE'), 
   1, '不使用加密的网站，明文传输');

COMMIT;

