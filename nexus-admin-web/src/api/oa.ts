import { get, post, put, del } from './request'

function withPageParams(current: number, size: number, extra?: Record<string, unknown>) {
  return { current, size, page: current, pageNum: current, pageSize: size, ...(extra || {}) }
}

export interface OaEmployee {
  id: number
  empNo: string
  name: string
  dept: string
  position: string
  hireDate: string
  phone: string
  status: number
  userId?: number
  directLeaderUserId?: number
}

export interface OaLeaveRequest {
  id: number
  applicantUserId: number
  leaveType: string
  startTime: string
  endTime: string
  leaveDays: number
  reason: string
  status: number
  createTime?: string
}

export interface OaAttendanceRule {
  id: number
  ruleName: string
  checkInStart: string
  checkInEnd: string
  checkOutStart: string
  checkOutEnd: string
  isEnable: number
}

export interface OaAttendanceRecord {
  id: number
  userId: number
  userName?: string
  checkDate: string
  checkInTime?: string
  checkOutTime?: string
  workMinutes: number
  status: number
}

export interface OaTask {
  id: number
  taskNo: string
  title: string
  description: string
  priority: number
  status: number
  assigneeUserId?: number
  assigneeUserName?: string
  creatorUserId?: number
  dueDate?: string
  progress: number
}

export interface OaSchedule {
  id: number
  title: string
  startTime: string
  endTime: string
  isAllDay: number
  visibility: number
}

export interface OaApprovalTask {
  id: number
  bizType: string
  bizId: number
  title: string
  applicantUserId: number
  applicantUserName: string
  approverUserId?: number
  status: number
}

export interface PageResult<T> {
  records?: T[]
  list?: T[]
  total: number
  current: number
  size: number
}

export const oaApi = {
  // Employee
  getEmployeePage: (current: number, size: number, name?: string, empNo?: string) =>
    get<PageResult<OaEmployee>>('/oa/employees/page', withPageParams(current, size, { name, empNo })),
  getEmployee: (id: number) => get<OaEmployee>(`/oa/employees/${id}`),
  createEmployee: (data: any) => post<number>('/oa/employees', data),
  updateEmployee: (id: number, data: any) => put(`/oa/employees/${id}`, data),
  deleteEmployee: (id: number) => del(`/oa/employees/${id}`),

  // Leave Request
  getLeaveRequestPage: (current: number, size: number, status?: number) =>
    get<PageResult<any>>('/oa/leave-requests/page', withPageParams(current, size, { status })),
  createLeaveRequest: (data: any) => post<number>('/oa/leave-requests', data),
  updateLeaveRequest: (id: number, data: any) => put(`/oa/leave-requests/${id}`, data),
  deleteLeaveRequest: (id: number) => del(`/oa/leave-requests/${id}`),
  submitLeaveRequest: (id: number) => put(`/oa/leave-requests/${id}/submit`),
  approveLeaveRequest: (id: number, approved: boolean, opinion?: string) =>
    put(`/oa/leave-requests/${id}/approve`, { approved, opinion }),

  // Attendance
  getAttendanceRules: () => get<OaAttendanceRule[]>('/oa/attendance/rules'),
  createAttendanceRule: (data: any) => post<number>('/oa/attendance/rules', data),
  deleteAttendanceRule: (id: number) => del(`/oa/attendance/rules/${id}`),
  checkIn: (type: 'in' | 'out', isOuter?: number, outerAddress?: string) =>
    post('/oa/attendance/check-in', { type, isOuter, outerAddress }),
  getMyTodayStatus: () => get<any>('/oa/attendance/my-today'),
  getAttendanceRecords: (current: number, size: number, params?: any) =>
    get<any>('/oa/attendance/records/page', withPageParams(current, size, params)),
  getAttendanceLeaves: (current: number, size: number, params?: any) =>
    get<any>('/oa/attendance/leave/page', withPageParams(current, size, params)),
  createAttendanceLeave: (data: any) => post<number>('/oa/attendance/leave', data),
  submitAttendanceLeave: (id: number) => post(`/oa/attendance/leave/${id}/submit`),
  approveAttendanceLeave: (id: number, data: any) =>
    post(`/oa/attendance/leave/${id}/approve`, data),
  getAttendanceOvertimes: (current: number, size: number, params?: any) =>
    get<any>('/oa/attendance/overtime/page', withPageParams(current, size, params)),
  createAttendanceOvertime: (data: any) => post<number>('/oa/attendance/overtime', data),
  deleteAttendanceOvertime: (id: number) => del(`/oa/attendance/overtime/${id}`),
  submitAttendanceOvertime: (id: number) => post(`/oa/attendance/overtime/${id}/submit`),
  approveAttendanceOvertime: (id: number, data: any) =>
    post(`/oa/attendance/overtime/${id}/approve`, data),
  getAttendanceStatistics: (userId: number, year: number, month: number) =>
    get<any>('/oa/attendance/statistics/monthly', { userId, year, month }),

  // Approval Center
  getMyApply: (current: number, size: number, status?: number) =>
    get<any>('/oa/approval/tasks/my-apply', withPageParams(current, size, { status })),
  getMyApprove: (current: number, size: number, status?: number) =>
    get<any>('/oa/approval/tasks/my-approve', withPageParams(current, size, { status })),
  approveTask: (id: number, opinion?: string) =>
    post(`/oa/approval/tasks/${id}/approve`, { opinion }),
  rejectTask: (id: number, opinion?: string) =>
    post(`/oa/approval/tasks/${id}/reject`, { opinion }),

  // Tasks
  getTaskPage: (current: number, size: number, status?: number, assigneeId?: number) =>
    get<any>('/oa/tasks/page', withPageParams(current, size, { status, assigneeId })),
  getTask: (id: number) => get<OaTask>(`/oa/tasks/${id}`),
  createTask: (data: any) => post<number>('/oa/tasks', data),
  updateTask: (id: number, data: any) => put(`/oa/tasks/${id}`, data),
  deleteTask: (id: number) => del(`/oa/tasks/${id}`),
  acceptTask: (id: number) => put(`/oa/tasks/${id}/accept`),
  updateTaskProgress: (id: number, progress: number) =>
    put(`/oa/tasks/${id}/progress?progress=${progress}`),
  completeTask: (id: number) => put(`/oa/tasks/${id}/complete`),
  cancelTask: (id: number) => put(`/oa/tasks/${id}/cancel`),
  getTaskComments: (id: number) => get<any[]>(`/oa/tasks/${id}/comments`),
  addTaskComment: (id: number, content: string) =>
    post(`/oa/tasks/${id}/comment`, { content }),

  // Schedule
  getSchedule: (startDate: string, endDate: string) =>
    get<OaSchedule[]>('/oa/schedules', { startDate, endDate }),
  createSchedule: (data: any) => post<number>('/oa/schedules', data),
  updateSchedule: (id: number, data: any) => put(`/oa/schedules/${id}`, data),
  deleteSchedule: (id: number) => del(`/oa/schedules/${id}`),

  // Files
  getFolders: (parentId?: number) => get<any[]>('/oa/files/folders', { parentId }),
  createFolder: (data: any) => post<number>('/oa/files/folders', data),
  deleteFolder: (id: number) => del(`/oa/files/folders/${id}`),
  getFiles: (folderId?: number) => get<any[]>('/oa/files', { folderId }),
  uploadFile: (formData: FormData) =>
    post('/oa/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  deleteFile: (id: number) => del(`/oa/files/${id}`),
}
