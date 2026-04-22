import { httpGet, httpPost, httpPut, httpDelete } from '../lib/request'
import type { PageResult } from '../types/api'
import type {
  TaskRow, TaskComment, LeaveRequestRow, ApprovalTaskRow,
  AttendanceRule, TodayStatus, AttendanceRecord, AttendanceStatistics,
  LeaveRecord, OvertimeRecord, ScheduleRow, EmployeeRow,
} from '../types/oa-crud'

export const taskApi = {
  page: (params: { current: number; size: number; status?: string; assigneeId?: number }) =>
    httpGet<PageResult<TaskRow>>('/oa/tasks/page', { params }),
  get: (id: number) => httpGet<TaskRow>(`/oa/tasks/${id}`),
  create: (body: Partial<TaskRow>) => httpPost<unknown>('/oa/tasks', body),
  update: (id: number, body: Partial<TaskRow>) => httpPut<unknown>(`/oa/tasks/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/oa/tasks/${id}`),
  accept: (id: number) => httpPut<unknown>(`/oa/tasks/${id}/accept`),
  progress: (id: number, body: { progress: number }) => httpPut<unknown>(`/oa/tasks/${id}/progress`, body),
  complete: (id: number) => httpPut<unknown>(`/oa/tasks/${id}/complete`),
  cancel: (id: number) => httpPut<unknown>(`/oa/tasks/${id}/cancel`),
  getComments: (id: number) => httpGet<TaskComment[]>(`/oa/tasks/${id}/comments`),
  addComment: (id: number, body: { content: string }) => httpPost<unknown>(`/oa/tasks/${id}/comment`, body),
}

export const leaveRequestApi = {
  page: (params: { current: number; size: number }) =>
    httpGet<PageResult<LeaveRequestRow>>('/oa/leave-requests/page', { params }),
  get: (id: number) => httpGet<LeaveRequestRow>(`/oa/leave-requests/${id}`),
  create: (body: Partial<LeaveRequestRow>) => httpPost<unknown>('/oa/leave-requests', body),
  update: (id: number, body: Partial<LeaveRequestRow>) => httpPut<unknown>(`/oa/leave-requests/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/oa/leave-requests/${id}`),
  submit: (id: number) => httpPut<unknown>(`/oa/leave-requests/${id}/submit`),
  approve: (id: number) => httpPut<unknown>(`/oa/leave-requests/${id}/approve`),
}

export const approvalApi = {
  myInitiated: (params: { current: number; size: number }) =>
    httpGet<PageResult<ApprovalTaskRow>>('/oa/approval/tasks/my-apply', { params }),
  myApprove: (params: { current: number; size: number }) =>
    httpGet<PageResult<ApprovalTaskRow>>('/oa/approval/tasks/my-approve', { params }),
  getTask: (id: number) => httpGet<ApprovalTaskRow>(`/oa/approval/tasks/${id}`),
  approve: (id: number, body?: { remark?: string }) =>
    httpPost<unknown>(`/oa/approval/tasks/${id}/approve`, body),
  reject: (id: number, body?: { remark?: string }) =>
    httpPost<unknown>(`/oa/approval/tasks/${id}/reject`, body),
}

export const attendanceApi = {
  getRules: () => httpGet<AttendanceRule[]>('/oa/attendance/rules'),
  createRule: (body: Partial<AttendanceRule>) => httpPost<unknown>('/oa/attendance/rules', body),
  deleteRule: (id: number) => httpDelete<unknown>(`/oa/attendance/rules/${id}`),
  checkIn: () => httpPost<unknown>('/oa/attendance/check-in'),
  myToday: () => httpGet<TodayStatus>('/oa/attendance/my-today'),
  recordsPage: (params: { current: number; size: number; month?: string }) =>
    httpGet<PageResult<AttendanceRecord>>('/oa/attendance/records/page', { params }),
  leavePage: (params: { current: number; size: number }) =>
    httpGet<PageResult<LeaveRecord>>('/oa/attendance/leave/page', { params }),
  createLeave: (body: Partial<LeaveRecord>) => httpPost<unknown>('/oa/attendance/leave', body),
  deleteLeave: (id: number) => httpDelete<unknown>(`/oa/attendance/leave/${id}`),
  submitLeave: (id: number) => httpPost<unknown>(`/oa/attendance/leave/${id}/submit`),
  approveLeave: (id: number) => httpPost<unknown>(`/oa/attendance/leave/${id}/approve`),
  overtimePage: (params: { current: number; size: number }) =>
    httpGet<PageResult<OvertimeRecord>>('/oa/attendance/overtime/page', { params }),
  createOvertime: (body: Partial<OvertimeRecord>) => httpPost<unknown>('/oa/attendance/overtime', body),
  deleteOvertime: (id: number) => httpDelete<unknown>(`/oa/attendance/overtime/${id}`),
  submitOvertime: (id: number) => httpPost<unknown>(`/oa/attendance/overtime/${id}/submit`),
  approveOvertime: (id: number) => httpPost<unknown>(`/oa/attendance/overtime/${id}/approve`),
  monthlyStatistics: (params: { year: number; month: number }) =>
    httpGet<AttendanceStatistics>('/oa/attendance/statistics/monthly', { params }),
}

export const scheduleApi = {
  list: (params?: { startDate?: string; endDate?: string }) =>
    httpGet<ScheduleRow[]>('/oa/schedules', { params }),
  create: (body: Partial<ScheduleRow>) => httpPost<unknown>('/oa/schedules', body),
  update: (id: number, body: Partial<ScheduleRow>) => httpPut<unknown>(`/oa/schedules/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/oa/schedules/${id}`),
}

export const employeeApi = {
  page: (params: { current: number; size: number }) =>
    httpGet<PageResult<EmployeeRow>>('/oa/employees/page', { params }),
  get: (id: number) => httpGet<EmployeeRow>(`/oa/employees/${id}`),
  create: (body: Partial<EmployeeRow>) => httpPost<unknown>('/oa/employees', body),
  update: (id: number, body: Partial<EmployeeRow>) => httpPut<unknown>(`/oa/employees/${id}`, body),
  remove: (id: number) => httpDelete<unknown>(`/oa/employees/${id}`),
}
