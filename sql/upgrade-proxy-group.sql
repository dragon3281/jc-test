-- =============================================
-- 代理池分组功能升级脚本
-- 功能：为t_proxy_pool表添加国家和分组字段
-- 日期：2025-12-10
-- =============================================

USE detection_platform;

-- 1. 检查并添加country字段
SET @column_exists_country = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'detection_platform' 
    AND TABLE_NAME = 't_proxy_pool' 
    AND COLUMN_NAME = 'country'
);

SET @sql_country = IF(@column_exists_country = 0,
    'ALTER TABLE t_proxy_pool ADD COLUMN `country` VARCHAR(50) NULL DEFAULT NULL COMMENT ''国家'' AFTER `description`;',
    'SELECT ''字段country已存在'' AS message;'
);

PREPARE stmt_country FROM @sql_country;
EXECUTE stmt_country;
DEALLOCATE PREPARE stmt_country;

-- 2. 检查并添加group_name字段
SET @column_exists_group = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'detection_platform' 
    AND TABLE_NAME = 't_proxy_pool' 
    AND COLUMN_NAME = 'group_name'
);

SET @sql_group = IF(@column_exists_group = 0,
    'ALTER TABLE t_proxy_pool ADD COLUMN `group_name` VARCHAR(100) NULL DEFAULT NULL COMMENT ''分组名称'' AFTER `country`;',
    'SELECT ''字段group_name已存在'' AS message;'
);

PREPARE stmt_group FROM @sql_group;
EXECUTE stmt_group;
DEALLOCATE PREPARE stmt_group;

-- 3. 创建索引以提升查询性能
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'detection_platform' 
    AND TABLE_NAME = 't_proxy_pool' 
    AND INDEX_NAME = 'idx_country_group'
);

SET @sql_index = IF(@index_exists = 0,
    'ALTER TABLE t_proxy_pool ADD INDEX `idx_country_group` (`country`, `group_name`);',
    'SELECT ''索引idx_country_group已存在'' AS message;'
);

PREPARE stmt_index FROM @sql_index;
EXECUTE stmt_index;
DEALLOCATE PREPARE stmt_index;

-- 4. 显示最终表结构
DESC t_proxy_pool;

-- 5. 显示升级完成信息
SELECT 
    '代理池分组功能升级完成！' AS status,
    'country字段已添加' AS field1,
    'group_name字段已添加' AS field2,
    '索引idx_country_group已创建' AS field3;
