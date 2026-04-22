import { describe, expect, it } from 'vitest'
import {
  buildPermissionSet,
  extractPermissionStrings,
  hasAllPerm,
  hasAnyPerm,
  hasPermi,
} from './permissions'

describe('extractPermissionStrings', () => {
  it('reads permissions and perms arrays', () => {
    expect(
      extractPermissionStrings({
        permissions: ['system:user:add'],
        perms: ['system:role:edit'],
      }),
    ).toEqual(expect.arrayContaining(['system:user:add', 'system:role:edit']))
  })
})

describe('hasPermi', () => {
  it('respects super perm and exact match', () => {
    const s = buildPermissionSet(['*:*:*'])
    expect(hasPermi(s, 'system:user:add')).toBe(true)
    const t = buildPermissionSet(['system:user:add'])
    expect(hasPermi(t, 'system:user:add')).toBe(true)
    expect(hasPermi(t, 'system:user:edit')).toBe(false)
  })
})

describe('hasAnyPerm / hasAllPerm', () => {
  it('works', () => {
    const s = buildPermissionSet(['a', 'b'])
    expect(hasAnyPerm(s, ['x', 'a'])).toBe(true)
    expect(hasAllPerm(s, ['a', 'b'])).toBe(true)
    expect(hasAllPerm(s, ['a', 'c'])).toBe(false)
  })
})
