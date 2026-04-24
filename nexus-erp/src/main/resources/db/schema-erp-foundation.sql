-- H2（MODE=MySQL）自动建表：与 V1.0 / V1.1 逻辑一致，随 nexus-erp 启动执行

DROP TABLE IF EXISTS erp_sale_order_item;
DROP TABLE IF EXISTS erp_sale_order;
DROP TABLE IF EXISTS erp_purchase_order_item;
DROP TABLE IF EXISTS erp_purchase_order;
DROP TABLE IF EXISTS erp_product_info;
DROP TABLE IF EXISTS erp_product_category;
DROP TABLE IF EXISTS erp_warehouse;
DROP TABLE IF EXISTS erp_supplier;
DROP TABLE IF EXISTS sys_oper_log;

CREATE TABLE erp_product_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_epc_tenant ON erp_product_category(tenant_id);

CREATE TABLE erp_product_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  category_id BIGINT,
  spec VARCHAR(200),
  unit VARCHAR(20),
  price DECIMAL(10,2) DEFAULT 0.00,
  stock INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_epi_tenant ON erp_product_info(tenant_id);
CREATE INDEX idx_epi_cat ON erp_product_info(category_id);

CREATE TABLE erp_warehouse (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  manager VARCHAR(50),
  phone VARCHAR(20),
  address VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 1,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ew_tenant ON erp_warehouse(tenant_id);

CREATE TABLE erp_supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  contact_person VARCHAR(50),
  phone VARCHAR(20),
  email VARCHAR(100),
  bank_name VARCHAR(100),
  bank_account VARCHAR(50),
  status TINYINT NOT NULL DEFAULT 1,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_es_tenant ON erp_supplier(tenant_id);

CREATE TABLE erp_purchase_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  supplier_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  status TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(512),
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, order_no)
);
CREATE INDEX idx_epo_supplier ON erp_purchase_order(supplier_id);
CREATE INDEX idx_epo_wh ON erp_purchase_order(warehouse_id);

CREATE TABLE erp_purchase_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(18,2) NOT NULL,
  subtotal DECIMAL(18,2) NOT NULL,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_epoi_order ON erp_purchase_order_item(order_id);

CREATE TABLE erp_sale_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  customer_id BIGINT,
  customer_name VARCHAR(128) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  status TINYINT NOT NULL DEFAULT 0,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, order_no)
);
CREATE INDEX idx_eso_customer ON erp_sale_order(customer_id);
CREATE INDEX idx_eso_wh ON erp_sale_order(warehouse_id);

CREATE TABLE erp_sale_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(18,2) NOT NULL,
  subtotal DECIMAL(18,2) NOT NULL,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_esoi_order ON erp_sale_order_item(order_id);

CREATE TABLE erp_stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  qty INT NOT NULL DEFAULT 0,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, product_id, warehouse_id)
);
CREATE INDEX idx_est_tenant ON erp_stock(tenant_id);
CREATE INDEX idx_est_product ON erp_stock(product_id);
CREATE INDEX idx_est_wh ON erp_stock(warehouse_id);

-- 测试库存数据：商品ID=1，仓库ID=1，库存=100，租户ID=1
INSERT INTO erp_stock(id, tenant_id, product_id, warehouse_id, qty)
VALUES (1, 1, 1, 1, 100);

CREATE TABLE sys_oper_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT,
  username VARCHAR(64),
  module VARCHAR(128),
  oper_type VARCHAR(64),
  oper_url VARCHAR(512),
  oper_method VARCHAR(16),
  oper_ip VARCHAR(64),
  request_param CLOB,
  response_data CLOB,
  status INT,
  error_msg VARCHAR(1024),
  cost_time BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sol_tenant ON sys_oper_log(tenant_id);
