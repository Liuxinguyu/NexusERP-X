import { describe, expect, it } from 'vitest'
import { extractBizData, pickPageRecords } from './http-helpers'

describe('extractBizData', () => {
  it('returns data when code is 200', () => {
    const data = extractBizData({ code: 200, msg: 'ok', data: { id: 1 } })
    expect(data).toEqual({ id: 1 })
  })

  it('returns data when code is 0 and message field is used', () => {
    const data = extractBizData({
      code: 0,
      message: '操作成功',
      data: { ok: true },
    })
    expect(data).toEqual({ ok: true })
  })

  it('throws when code is not success', () => {
    expect(() =>
      extractBizData({ code: 500, msg: 'server error', data: null }),
    ).toThrow('server error')
  })

  it('throws using message when msg is absent', () => {
    expect(() =>
      extractBizData({ code: 400, message: 'bad request', data: null }),
    ).toThrow('bad request')
  })
})

describe('pickPageRecords', () => {
  it('prefers records field', () => {
    const data = pickPageRecords({ records: [1, 2], list: [3] })
    expect(data).toEqual([1, 2])
  })

  it('fallbacks to list field', () => {
    const data = pickPageRecords({ list: [3, 4] })
    expect(data).toEqual([3, 4])
  })

  it('returns empty array on invalid payload', () => {
    const data = pickPageRecords(undefined)
    expect(data).toEqual([])
  })
})
