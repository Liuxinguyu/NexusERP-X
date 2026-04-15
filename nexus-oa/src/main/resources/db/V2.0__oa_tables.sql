-- H2（MODE=MySQL）：OA 员工与请假申请

DROP TABLE IF EXISTS oa_leave_request;
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

CREATE TABLE oa_leave_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  leave_type VARCHAR(32) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  leave_days DECIMAL(10,2) NOT NULL,
  reason VARCHAR(512),
  status TINYINT NOT NULL DEFAULT 0,
  approver_user_id BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_olr_tenant ON oa_leave_request(tenant_id);
CREATE INDEX idx_olr_app ON oa_leave_request(applicant_user_id);
CREATE INDEX idx_olr_status ON oa_leave_request(status);
