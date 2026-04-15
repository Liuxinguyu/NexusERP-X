-- H2（MODE=MySQL）最小 oa_employee，供薪酬模块本地联调与一键生成工资时查询员工

DROP TABLE IF EXISTS oa_employee;

CREATE TABLE oa_employee (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  emp_no VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  dept VARCHAR(128),
  position VARCHAR(64),
  hire_date DATE,
  phone VARCHAR(32),
  status TINYINT NOT NULL DEFAULT 1,
  user_id BIGINT,
  direct_leader_user_id BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, emp_no)
);
CREATE INDEX idx_oe_tenant ON oa_employee(tenant_id);
CREATE INDEX idx_oe_user ON oa_employee(user_id);
