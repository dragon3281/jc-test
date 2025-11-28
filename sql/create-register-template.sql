-- 注册模板表
CREATE TABLE IF NOT EXISTS `t_register_template` (
  `id` BIGINT AUTO_INCREMENT COMMENT '模板ID',
  `template_name` VARCHAR(100) COMMENT '模板名称',
  `website_url` VARCHAR(255) COMMENT '网站URL',
  `register_api` VARCHAR(255) COMMENT '注册接口',
  `method` VARCHAR(10) DEFAULT 'PUT' COMMENT '请求方法',
  `username_field` VARCHAR(50) COMMENT '用户名字段',
  `password_field` VARCHAR(50) COMMENT '密码字段',
  `default_password` VARCHAR(50) COMMENT '默认密码',
  `extra_params` TEXT COMMENT '额外参数JSON',
  `encryption_type` VARCHAR(50) COMMENT '加密类型(如DES+RSA)',
  `rsa_key_api` VARCHAR(255) COMMENT 'RSA密钥接口',
  `rsa_ts_param` VARCHAR(50) COMMENT 'RSA密钥接口时间戳参数名',
  `encryption_header` VARCHAR(50) COMMENT '加密头名称',
  `value_field_name` VARCHAR(50) COMMENT '加密数据字段名',
  `notes` TEXT COMMENT '备注(记录关键逻辑如老RSA库)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册模板表';
