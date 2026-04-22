export interface TaskRow {
  id?: number
  title?: string
  description?: string
  status?: number | string
  priority?: number
  assigneeId?: number
  assigneeName?: string
  deadline?: string
  progress?: number
  createTime?: string
  [key: string]: unknown
}

export interface TaskComment {
  id?: number
  taskId?: number
  content?: string
  userId?: number
  userName?: string
  createTime?: string
  [key: string]: unknown
}

export interface LeaveRequestRow {
  id?: number
  leaveType?: string
  startDate?: string
  endDate?: string
  reason?: string
  status?: number | string
  applicantName?: string
  createTime?: string
  [key: string]: unknown
}

export interface ApprovalTaskRow {
  id?: number
  title?: string
  applicantUserName?: string
  status?: number | string
  contentSummary?: string
  createTime?: string
  [key: string]: unknown
}

export interface AttendanceRule {
  id?: number
  ruleName?: string
  checkInTime?: string
  checkOutTime?: string
  [key: string]: unknown
}

export interface TodayStatus {
  checkInTime?: string
  checkOutTime?: string
  status?: string
  [key: string]: unknown
}

export interface AttendanceRecord {
  id?: number
  userId?: number
  userName?: string
  checkInTime?: string
  checkOutTime?: string
  status?: string
  date?: string
  [key: string]: unknown
}

export interface AttendanceStatistics {
  normalDays?: number
  lateDays?: number
  earlyDays?: number
  absentDays?: number
  leaveDays?: number
  overtimeHours?: number
  [key: string]: unknown
}

export interface LeaveRecord {
  id?: number
  leaveType?: string
  startDate?: string
  endDate?: string
  reason?: string
  status?: number | string
  [key: string]: unknown
}

export interface OvertimeRecord {
  id?: number
  date?: string
  hours?: number
  reason?: string
  status?: number | string
  [key: string]: unknown
}

export interface ScheduleRow {
  id?: number
  title?: string
  description?: string
  startTime?: string
  endTime?: string
  [key: string]: unknown
}

export interface EmployeeRow {
  id?: number
  employeeName?: string
  employeeNo?: string
  department?: string
  position?: string
  phone?: string
  email?: string
  entryDate?: string
  status?: number
  [key: string]: unknown
}

export interface OaFileFolder {
  id?: number
  folderName?: string
  parentId?: number
  [key: string]: unknown
}

export interface OaFileRow {
  id?: number
  fileName?: string
  fileSize?: number
  fileType?: string
  folderId?: number
  uploadTime?: string
  uploaderName?: string
  downloadCount?: number
  [key: string]: unknown
}

export interface NoticeRow {
  id?: number
  title?: string
  content?: string
  noticeType?: string
  status?: number
  createTime?: string
  [key: string]: unknown
}
