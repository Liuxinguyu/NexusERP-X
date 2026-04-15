-- 考勤规则表
CREATE TABLE IF NOT EXISTS oa_attendance_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  rule_name VARCHAR(64) NOT NULL COMMENT '规则名称，如标准工时制',
  check_in_start TIME NOT NULL COMMENT '上班签到开始时间',
  check_in_end TIME NOT NULL COMMENT '上班签到截止时间',
  check_out_start TIME NOT NULL COMMENT '下班签退开始时间',
  check_out_end TIME NOT NULL COMMENT '下班签退截止时间',
  is_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
  remark TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 考勤记录表（每人每天一条）
CREATE TABLE IF NOT EXISTS oa_attendance_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL COMMENT '用户ID（关联 sys_user）',
  check_date DATE NOT NULL COMMENT '考勤日期',
  check_in_time DATETIME COMMENT '上班打卡时间',
  check_out_time DATETIME COMMENT '下班打卡时间',
  work_minutes INT DEFAULT 0 COMMENT '工作时长（分钟）',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1迟到 2早退 3缺卡 4旷工 5加班',
  is_outer TINYINT NOT NULL DEFAULT 0 COMMENT '是否外勤：0否 1是',
  outer_address VARCHAR(256) COMMENT '外勤地址',
  outer_reason VARCHAR(256) COMMENT '外勤原因',
  remark TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_user_date (tenant_id, user_id, check_date),
  KEY idx_date (tenant_id, check_date),
  KEY idx_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 请假明细表
CREATE TABLE IF NOT EXISTS oa_leave_detail (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  leave_no VARCHAR(64) NOT NULL COMMENT '请假编号',
  user_id BIGINT NOT NULL COMMENT '申请人用户ID',
  user_name VARCHAR(64) COMMENT '申请人姓名',
  leave_type VARCHAR(32) NOT NULL COMMENT '请假类型：年假/病假/事假/婚假/产假/其他',
  start_date DATE NOT NULL COMMENT '请假开始日期',
  end_date DATE NOT NULL COMMENT '请假结束日期',
  leave_days DECIMAL(5,1) NOT NULL COMMENT '请假天数',
  reason TEXT COMMENT '请假原因',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1待审批 2已通过 3已拒绝',
  approver_user_id BIGINT COMMENT '审批人用户ID',
  approver_opinion TEXT COMMENT '审批意见',
  approver_time DATETIME COMMENT '审批时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_no (tenant_id, leave_no),
  KEY idx_user (tenant_id, user_id),
  KEY idx_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 加班记录表
CREATE TABLE IF NOT EXISTS oa_overtime (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  overtime_no VARCHAR(64) NOT NULL COMMENT '加班单号',
  user_id BIGINT NOT NULL COMMENT '申请人用户ID',
  user_name VARCHAR(64) COMMENT '申请人姓名',
  start_time DATETIME NOT NULL COMMENT '加班开始时间',
  end_time DATETIME NOT NULL COMMENT '加班结束时间',
  hours DECIMAL(5,1) NOT NULL COMMENT '加班时长（小时）',
  reason TEXT COMMENT '加班原因',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1待审批 2已通过 3已拒绝',
  approver_user_id BIGINT COMMENT '审批人用户ID',
  approver_opinion TEXT COMMENT '审批意见',
  approver_time DATETIME COMMENT '审批时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_no (tenant_id, overtime_no),
  KEY idx_user (tenant_id, user_id),
  KEY idx_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
