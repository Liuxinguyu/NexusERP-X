-- 通用审批任务表
CREATE TABLE IF NOT EXISTS oa_approval_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_no VARCHAR(64) NOT NULL COMMENT '审批任务编号',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：leave/overtime/purchase',
  biz_id BIGINT NOT NULL COMMENT '业务数据ID',
  title VARCHAR(256) NOT NULL COMMENT '审批标题',
  content_summary VARCHAR(512) COMMENT '内容摘要',
  applicant_user_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_user_name VARCHAR(64) COMMENT '申请人姓名',
  approver_user_id BIGINT NOT NULL COMMENT '审批人ID',
  approver_user_name VARCHAR(64) COMMENT '审批人姓名',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已拒绝',
  opinion TEXT COMMENT '审批意见',
  approve_time DATETIME COMMENT '审批时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_biz (tenant_id, biz_type, biz_id),
  KEY idx_approver (tenant_id, approver_user_id, status),
  KEY idx_applicant (tenant_id, applicant_user_id),
  KEY idx_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 任务看板
CREATE TABLE IF NOT EXISTS oa_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
  title VARCHAR(256) NOT NULL COMMENT '任务标题',
  description TEXT COMMENT '任务描述',
  priority TINYINT NOT NULL DEFAULT 2 COMMENT '1紧急 2高 3中 4低',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待接受 1进行中 2已完成 3已取消',
  assignee_user_id BIGINT COMMENT '负责人ID',
  assignee_user_name VARCHAR(64) COMMENT '负责人姓名',
  creator_user_id BIGINT NOT NULL COMMENT '创建人ID',
  due_date DATE COMMENT '截止日期',
  start_date DATE COMMENT '开始日期',
  completed_time DATETIME COMMENT '完成时间',
  progress INT DEFAULT 0 COMMENT '进度 0-100',
  tags VARCHAR(256) COMMENT '标签，逗号分隔',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_no (tenant_id, task_no),
  KEY idx_assignee (tenant_id, assignee_user_id),
  KEY idx_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 任务评论表
CREATE TABLE IF NOT EXISTS oa_task_comment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL COMMENT '评论人ID',
  user_name VARCHAR(64) COMMENT '评论人姓名',
  content TEXT NOT NULL COMMENT '评论内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  del_flag TINYINT NOT NULL DEFAULT 0,
  KEY idx_task (tenant_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 日程管理
CREATE TABLE IF NOT EXISTS oa_schedule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL COMMENT '日程标题',
  content TEXT COMMENT '日程内容',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  is_all_day TINYINT DEFAULT 0 COMMENT '是否全天',
  reminder_minutes INT COMMENT '提前提醒分钟数',
  location VARCHAR(256) COMMENT '地点',
  color VARCHAR(16) DEFAULT '#409EFF' COMMENT '日历颜色',
  visibility TINYINT DEFAULT 0 COMMENT '0私有 1公开',
  creator_user_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  KEY idx_creator (tenant_id, creator_user_id),
  KEY idx_time_range (tenant_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 云空间-文件夹
CREATE TABLE IF NOT EXISTS oa_file_folder (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0 COMMENT '父文件夹ID，0为根目录',
  folder_name VARCHAR(128) NOT NULL COMMENT '文件夹名称',
  visibility TINYINT DEFAULT 0 COMMENT '0私有 1公开',
  owner_user_id BIGINT NOT NULL COMMENT '所有者ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  KEY idx_parent (tenant_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 云空间-文件
CREATE TABLE IF NOT EXISTS oa_file (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  folder_id BIGINT DEFAULT 0 COMMENT '所属文件夹，0为根目录',
  file_name VARCHAR(256) NOT NULL COMMENT '文件名称',
  file_key VARCHAR(256) NOT NULL COMMENT '存储KEY',
  file_size BIGINT COMMENT '文件大小（字节）',
  file_type VARCHAR(64) COMMENT '文件扩展名',
  download_count INT DEFAULT 0 COMMENT '下载次数',
  visibility TINYINT DEFAULT 0 COMMENT '0私有 1公开',
  owner_user_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by BIGINT,
  update_by BIGINT,
  del_flag TINYINT NOT NULL DEFAULT 0,
  KEY idx_folder (tenant_id, folder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
