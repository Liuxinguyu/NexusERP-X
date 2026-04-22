import type { ConfirmShopPayload, ConfirmShopResult, PreAuthPayload } from '../api/auth'
import { authApi } from '../api/auth'
import { setAuthSession } from './storage'

type PreAuthResult = {
  preAuthToken: string
  tenantId: number
  shops: Array<{ shopId: number; shopName: string }>
}

interface AuthFlowDeps {
  loginPreAuth: (payload: PreAuthPayload) => Promise<PreAuthResult>
  confirmShop: (payload: ConfirmShopPayload) => Promise<ConfirmShopResult>
  setAuthSession: typeof setAuthSession
}

const defaultDeps: AuthFlowDeps = {
  loginPreAuth: authApi.loginPreAuth,
  confirmShop: authApi.confirmShop,
  setAuthSession,
}

export async function beginPreAuthLogin(
  payload: PreAuthPayload,
  deps: AuthFlowDeps = defaultDeps,
): Promise<PreAuthResult> {
  const preAuth = await deps.loginPreAuth({
    username: payload.username,
    password: payload.password,
    captcha: payload.captcha,
    captchaKey: payload.captchaKey,
    tenantId: payload.tenantId,
  })
  if (!preAuth.shops?.length) {
    throw new Error('未获取到可用店铺')
  }
  return preAuth
}

export async function completeShopLogin(
  preAuthToken: string,
  shopId: number,
  deps: AuthFlowDeps = defaultDeps,
): Promise<ConfirmShopResult> {
  const confirmed = await deps.confirmShop({
    preAuthToken,
    shopId,
  })

  deps.setAuthSession({
    accessToken: confirmed.accessToken,
    currentShopId: confirmed.currentShopId,
    tenantId: confirmed.tenantId,
    currentOrgId: confirmed.currentOrgId,
    dataScope: confirmed.dataScope,
  })
  return confirmed
}

export async function performTwoStepLogin(
  payload: PreAuthPayload,
  deps: AuthFlowDeps = defaultDeps,
): Promise<ConfirmShopResult> {
  const preAuth = await beginPreAuthLogin(payload, deps)
  return completeShopLogin(preAuth.preAuthToken, preAuth.shops[0].shopId, deps)
}
