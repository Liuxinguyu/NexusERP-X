import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearAuthSession,
  getAccessToken,
  getCurrentShopId,
  setAuthSession,
} from './storage'

function createMemoryStorage() {
  const map = new Map<string, string>()
  return {
    getItem: (key: string) => map.get(key) ?? null,
    setItem: (key: string, value: string) => {
      map.set(key, value)
    },
    removeItem: (key: string) => {
      map.delete(key)
    },
  }
}

describe('storage helpers', () => {
  beforeEach(() => {
    Object.defineProperty(globalThis, 'localStorage', {
      value: createMemoryStorage(),
      writable: true,
    })
  })

  it('stores and reads auth session', () => {
    setAuthSession({ accessToken: 'token-1', currentShopId: 7, tenantId: 100 })
    expect(getAccessToken()).toBe('token-1')
    expect(getCurrentShopId()).toBe(7)
  })

  it('clears auth session keys', () => {
    setAuthSession({ accessToken: 'token-2', currentShopId: 8, tenantId: 200 })
    clearAuthSession()
    expect(getAccessToken()).toBeNull()
    expect(getCurrentShopId()).toBeNull()
  })
})
