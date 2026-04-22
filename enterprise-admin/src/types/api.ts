export interface PageResult<T> {
  records?: T[]
  list?: T[]
  total: number
  current: number
  size: number
  pages?: number
}
