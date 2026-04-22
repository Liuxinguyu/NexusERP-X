import { describe, expect, it, vi } from 'vitest'
import { performTwoStepLogin } from './auth-flow'

describe('performTwoStepLogin', () => {
  it('completes pre-auth and confirm-shop then persists session', async () => {
    const deps = {
      loginPreAuth: vi.fn().mockResolvedValue({
        preAuthToken: 'pre-token',
        tenantId: 1,
        shops: [{ shopId: 100, shopName: '旗舰店' }],
      }),
      confirmShop: vi.fn().mockResolvedValue({
        accessToken: 'access-token',
        tenantId: 1,
        currentShopId: 100,
      }),
      setAuthSession: vi.fn(),
    }

    const result = await performTwoStepLogin(
      { username: 'admin', password: '123456' },
      deps,
    )

    expect(deps.loginPreAuth).toHaveBeenCalledWith({
      username: 'admin',
      password: '123456',
      captcha: undefined,
      captchaKey: undefined,
    })
    expect(deps.confirmShop).toHaveBeenCalledWith({
      preAuthToken: 'pre-token',
      shopId: 100,
    })
    expect(deps.setAuthSession).toHaveBeenCalledWith({
      accessToken: 'access-token',
      currentShopId: 100,
      tenantId: 1,
    })
    expect(result.accessToken).toBe('access-token')
  })

  it('throws when pre-auth does not return shops', async () => {
    const deps = {
      loginPreAuth: vi.fn().mockResolvedValue({
        preAuthToken: 'pre-token',
        tenantId: 1,
        shops: [],
      }),
      confirmShop: vi.fn(),
      setAuthSession: vi.fn(),
    }

    await expect(
      performTwoStepLogin({ username: 'admin', password: '123456' }, deps),
    ).rejects.toThrow('未获取到可用店铺')
  })
})
