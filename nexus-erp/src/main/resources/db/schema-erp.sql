DROP TABLE IF EXISTS erp_customer;
DROP TABLE IF EXISTS erp_product;

CREATE TABLE erp_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64),
  unit VARCHAR(32),
  price DECIMAL(18, 2) NOT NULL DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

CREATE TABLE erp_customer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64),
  contact_phone VARCHAR(32),
  level VARCHAR(32),
  credit_limit DECIMAL(18, 2) DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT DEFAULT 0
);

-- 主店 shop_id=1：2 商品 + 2 客户
INSERT INTO erp_product(id, tenant_id, shop_id, org_id, name, category, unit, price, status)
VALUES (1, 1, 1, 1, '主店商品A', '默认分类', '件', 99.00, 1);
INSERT INTO erp_product(id, tenant_id, shop_id, org_id, name, category, unit, price, status)
VALUES (2, 1, 1, 1, '主店商品B', '默认分类', '箱', 199.50, 1);

INSERT INTO erp_customer(id, tenant_id, shop_id, org_id, name, contact_name, contact_phone, level, credit_limit)
VALUES (1, 1, 1, 1, '主店客户甲', '张三', '13800000001', 'A', 50000.00);
INSERT INTO erp_customer(id, tenant_id, shop_id, org_id, name, contact_name, contact_phone, level, credit_limit)
VALUES (2, 1, 1, 1, '主店客户乙', '李四', '13800000002', 'B', 10000.00);

-- 分店 shop_id=2（组织华东）：1 商品 + 1 客户
INSERT INTO erp_product(id, tenant_id, shop_id, org_id, name, category, unit, price, status)
VALUES (3, 1, 2, 2, '分店商品C', '默认分类', '件', 59.00, 1);

INSERT INTO erp_customer(id, tenant_id, shop_id, org_id, name, contact_name, contact_phone, level, credit_limit)
VALUES (3, 1, 2, 2, '分店客户丙', '王五', '13900000003', 'A', 20000.00);
