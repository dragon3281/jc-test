-- 升级自动化注册任务表，添加加密配置字段
-- 执行时间: 2024-11-20

USE `detection_platform`;

-- 添加缺失字段(如果字段已存在会报错，忽略即可)
ALTER TABLE `t_register_task` ADD COLUMN `rsa_key_api` VARCHAR(500) NULL DEFAULT NULL COMMENT 'RSA密钥接口' AFTER `encryption_type`;
ALTER TABLE `t_register_task` ADD COLUMN `rsa_ts_param` VARCHAR(50) NULL DEFAULT 't' COMMENT 'RSA时间戳参数名' AFTER `rsa_key_api`;
ALTER TABLE `t_register_task` ADD COLUMN `value_field_name` VARCHAR(100) NULL DEFAULT 'value' COMMENT '数据包装字段名' AFTER `encryption_header`;
ALTER TABLE `t_register_task` ADD COLUMN `dup_msg_substring` VARCHAR(500) NULL DEFAULT NULL COMMENT '重复用户名提示(用于验证成功)' AFTER `value_field_name`;

SELECT '升级完成!' AS status;
