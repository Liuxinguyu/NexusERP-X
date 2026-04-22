export interface MonthlySlipRow {
  id?: number
  belongMonth?: string
  employeeId?: number
  baseSalary?: number
  subsidyTotal?: number
  deductionTotal?: number
  netPay?: number
  status?: number
  [key: string]: unknown
}

export interface ItemConfigRow {
  id?: number
  itemName?: string
  calcType?: number
  defaultAmount?: number
  itemKind?: number
  [key: string]: unknown
}
