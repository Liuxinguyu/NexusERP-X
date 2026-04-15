import { get, post } from './request'

export interface CaptchaImage {
  uuid: string
  img: string
}

export const captchaApi = {
  getCaptcha: () => get<CaptchaImage>('/system/captcha/image'),
  validateCaptcha: (uuid: string, code: string) => post<boolean>('/system/captcha/validate', null, { params: { uuid, code } }),
}
