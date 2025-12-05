-- 检测任务明细表（支持断点续跑）
CREATE TABLE IF NOT EXISTS `t_detection_task_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `phone` VARCHAR(32) NOT NULL COMMENT '手机号',
  `token_used` VARCHAR(200) COMMENT '使用的Token(脱敏)',
  `response_code` INT COMMENT '响应状态码',
  `response_body` TEXT COMMENT '响应内容(可选保存)',
  `is_duplicate` TINYINT DEFAULT 0 COMMENT '是否已注册:0=未注册,1=已注册',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING,SUCCESS,ERROR',
  `error_message` VARCHAR(500) COMMENT '错误信息',
  `is_rate_limited` TINYINT DEFAULT 0 COMMENT '是否触发限流',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测任务明细表';

-- POST模板表增加限流配置字段
ALTER TABLE `t_post_template` 
ADD COLUMN `rate_limit_keyword` VARCHAR(100) COMMENT '限流关键字(如TOO_MANY_REQUEST)' AFTER `response_code`,
ADD COLUMN `max_consecutive_rate_limit` INT DEFAULT 5 COMMENT '连续限流触发次数阈值' AFTER `rate_limit_keyword`,
ADD COLUMN `backoff_seconds` INT DEFAULT 2 COMMENT '触发限流后暂停秒数' AFTER `max_consecutive_rate_limit`,
ADD COLUMN `min_concurrency` INT DEFAULT 1 COMMENT '最小并发数' AFTER `backoff_seconds`,
ADD COLUMN `max_concurrency` INT DEFAULT 50 COMMENT '最大并发数' AFTER `min_concurrency`;
