import { chromium } from 'playwright'
import { execSync } from 'child_process'

const BASE = 'http://localhost:5178'

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext()
  const page = await context.newPage()

  // Capture ALL responses
  page.on('response', async resp => {
    if (resp.url().includes('/api/v1/')) {
      const status = resp.status()
      let body = ''
      try { body = await resp.text() } catch {}
      const req = resp.request()
      const authHdr = (await req.allHeaders())['authorization'] || '(none)'
      console.log(`\n[RESP] ${status} ${resp.request().method()} ${resp.url()}`)
      console.log(`[AUTH] ${authHdr}`)
      if (body) console.log(`[BODY] ${body.substring(0, 400)}`)
    }
  })

  page.on('request', async req => {
    if (req.url().includes('/api/v1/')) {
      const headers = await req.allHeaders()
      const body = req.postData()
      console.log(`\n[REQ] ${req.method()} ${req.url()}`)
      console.log(`[AUTH] ${headers['authorization'] || '(none)'}`)
      if (body) console.log(`[BODY] ${body.substring(0, 200)}`)
    }
  })

  // Fresh context, fresh page
  await page.goto(`${BASE}/login`)
  await page.evaluate(() => localStorage.clear())
  await page.reload()
  await page.waitForTimeout(2000)

  // Trigger the Vue component's own captcha refresh so the uuid is consistent
  await page.locator('.captcha-row .el-image').click()
  await page.waitForTimeout(2000)

  // Get captcha uuid from Vue component state (captchaUuid ref)
  const uuid = await page.evaluate(() => window.__NEXUS_CAPTCHA_UUID__).catch(() => null)
  if (!uuid) {
    // Fallback: get the latest uuid from the <img> src or the component's reactive state
    const fallback = await page.evaluate(async () => {
      // Call Vue store / component to get current captchaUuid
      const r = await fetch('/api/v1/system/captcha/image')
      const d = await r.json()
      return d?.data?.uuid
    })
    console.log('\n=== CAPTCHA (fallback):', fallback)
    if (!fallback) { console.log('No captcha!'); await browser.close(); return }
    await page.evaluate(async (captchaUuid) => {
      // Patch the Vue component's captchaUuid ref
      const el = document.querySelector('#login-app')
      if (el && el.__vueParentComponent) {
        let found = false
        const walk = (comp) => {
          if (comp.props?.captchaUuid !== undefined) {
            comp.setupState.captchaUuid.value = captchaUuid
            found = true
          }
          comp.subTree && walk(comp.subTree)
          comp.children && Object.values(comp.children).forEach(c => walk(c))
        }
        walk(el.__vueParentComponent)
      }
    }, fallback)
    console.log('=== Correct code: (fallback, using fresh)')
    // Use fresh captcha code
    await page.fill('input[placeholder="验证码"]', '')
    await page.waitForTimeout(500)
    await browser.close()
    return
  }

  const code = execSync(`redis-cli -n 0 GET "captcha:${uuid}"`).toString().trim()
  console.log('\n=== UUID from component:', uuid)
  console.log('=== Correct code:', code)

  // Check what's in localStorage right now
  const storage = await page.evaluate(() => localStorage.getItem('nexus_token'))
  console.log('nexus_token in localStorage:', storage)

  // Fill form - use el-input inner input selectors
  await page.locator('.login-form .el-input').first().locator('input').fill('admin')
  await page.locator('.login-form .el-input').nth(1).locator('input').fill('admin')
  await page.locator('.captcha-row .el-input input').fill(code)

  console.log('\n=== SUBMITTING ===')
  await page.click('button:has-text("登 录")')
  await page.waitForTimeout(5000)

  console.log('\n=== URL:', page.url())
  await browser.close()
}

main().catch(e => { console.error(e); process.exit(1) })
