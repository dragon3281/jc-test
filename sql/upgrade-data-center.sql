-- 数据中心升级脚本
-- 功能：支持TXT上传（每行纯数字）、添加国家/数据类型字段、创建历史表

USE detection_platform;

-- 1. 修改基础数据表
ALTER TABLE t_base_data 
MODIFY COLUMN `data_value` VARCHAR(255) NOT NULL COMMENT '数据值（纯数字号码）',
MODIFY COLUMN `data_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '数据类型（用户手动输入：账号/密码/token/手机号等）',
MODIFY COLUMN `country` VARCHAR(50) NULL DEFAULT NULL COMMENT '国家（下拉选择）';

-- 2. 修改检测结果表
ALTER TABLE t_detection_result
ADD COLUMN `seq_no` INT(11) NOT NULL COMMENT '序号' AFTER `id`,
ADD COLUMN `data_type` TINYINT(4) NULL DEFAULT NULL COMMENT '数据类型:1邮箱,2手机,3用户名' AFTER `account_identifier`,
ADD COLUMN `country` VARCHAR(50) NULL DEFAULT NULL COMMENT '国家' AFTER `data_type`,
ADD COLUMN `is_archived` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已归档:0否,1是' AFTER `detect_time`,
ADD INDEX `idx_archived` (`is_archived`, `detect_time`);

-- 3. 创建检测结果历史表
CREATE TABLE IF NOT EXISTS `t_detection_result_history` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `seq_no` INT(11) NOT NULL COMMENT '序号',
  `task_id` BIGINT(20) NOT NULL COMMENT '任务ID',
  `account_identifier` VARCHAR(255) NOT NULL COMMENT '账号标识',
  `data_type` TINYINT(4) NULL DEFAULT NULL COMMENT '数据类型:1邮箱,2手机,3用户名',
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

SELECT '数据库升级完成！' AS message;
