-- 1. 更新 sys_role 表补充 shop_scope
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE add_column_if_not_exists(
    IN table_name VARCHAR(100),
    IN column_name VARCHAR(100),
    IN column_definition VARCHAR(255)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    SET @ddl = CONCAT('ALTER TABLE ', table_name, ' ADD COLUMN ', column_name, ' ', column_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END $$
DELIMITER ;

CALL add_column_if_not_exists('sys_role', 'shop_scope', 'INT DEFAULT 1 COMMENT ''1全部门店 2自定门店 3本门店'' AFTER data_scope');
CALL add_column_if_not_exists('sys_org', 'ancestors', 'VARCHAR(500) DEFAULT '''' COMMENT ''祖级列表'' AFTER parent_id');

-- 3. 创建 sys_role_org 表 (用于 data_scope = 2)
CREATE TABLE IF NOT EXISTS sys_role_org (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    PRIMARY KEY (role_id, org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和组织关联表';

-- 4. 创建 sys_role_shop 表 (用于 shop_scope = 2)
CREATE TABLE IF NOT EXISTS sys_role_shop (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    shop_id BIGINT NOT NULL COMMENT '门店ID',
    PRIMARY KEY (role_id, shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和门店关联表';

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
