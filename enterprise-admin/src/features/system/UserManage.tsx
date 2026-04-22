import { useCallback, useEffect, useState } from 'react'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import {
  orgApi,
  roleApi,
  shopApi,
  unwrapPage,
  userAdminApi,
} from '../../api/system-crud'
import { flattenOrgTree } from '../../lib/org-flat'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import type { RoleRow, ShopRoleItem, ShopRow, UserRow } from '../../types/system-crud'
import OrgSidebar from './components/OrgSidebar'

function parseShopRoles(raw: unknown): ShopRoleItem[] {
  if (Array.isArray(raw)) return raw as ShopRoleItem[]
  if (raw && typeof raw === 'object') {
    const items = (raw as { items?: unknown }).items
    if (Array.isArray(items)) return items as ShopRoleItem[]
  }
  return []
}

type FormErrors = Record<string, string>

function validateUserForm(
  form: {
    username: string
    realName: string
    phone: string
    password: string
    mainShopId: number
    mainOrgId: number
  },
  isNew: boolean,
): FormErrors {
  const errors: FormErrors = {}
  if (!form.username.trim()) errors.username = '用户名不能为空'
  if (!form.realName.trim()) errors.realName = '姓名不能为空'
  if (isNew && !form.password.trim()) errors.password = '新建用户密码不能为空'
  if (!form.mainShopId || form.mainShopId <= 0)
    errors.mainShopId = '⚠️ 主店铺不可为空，否则后端 DataScope 会拦截无权限'
  if (!form.mainOrgId || form.mainOrgId <= 0)
    errors.mainOrgId = '⚠️ 所属部门不可为空，否则后端 DataScope 会拦截无权限'
  return errors
}

export default function UserManage() {
  const { can } = usePermissions()
  const toast = useToast()
  const confirm = useConfirm()
  const [list, setList] = useState<UserRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [size] = useState(10)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // --- search filters ---
  const [usernameQ, setUsernameQ] = useState('')
  const [phoneQ, setPhoneQ] = useState('')
  const [statusQ, setStatusQ] = useState<number | undefined>(undefined)
  const [deptIdQ, setDeptIdQ] = useState<number | undefined>(undefined)
  const [searchNonce, setSearchNonce] = useState(0)

  const [shops, setShops] = useState<ShopRow[]>([])
  const [roles, setRoles] = useState<RoleRow[]>([])
  const [orgOptions, setOrgOptions] = useState<{ id: number; label: string }[]>([])

  const [modal, setModal] = useState<{ open: boolean; row: UserRow | null }>({
    open: false,
    row: null,
  })
  const [form, setForm] = useState({
    username: '',
    realName: '',
    phone: '',
    status: 1,
    password: '',
    mainShopId: 0 as number,
    mainOrgId: 0 as number,
  })
  const [formErrors, setFormErrors] = useState<FormErrors>({})

  const [srModal, setSrModal] = useState<{
    open: boolean
    userId: number | null
    username: string
  }>({ open: false, userId: null, username: '' })
  const [srRows, setSrRows] = useState<ShopRoleItem[]>([])

  const [resetPwdModal, setResetPwdModal] = useState<{
    open: boolean
    userId: number | null
    username: string
  }>({ open: false, userId: null, username: '' })
  const [resetPwdValue, setResetPwdValue] = useState('')

  const loadRefs = useCallback(async () => {
    try {
      const s = await shopApi.getShopOptions()
      setShops(Array.isArray(s) ? s : [])
    } catch {
      try {
        const { rows } = await unwrapPage(shopApi.getShopPage({ current: 1, size: 200 }))
        setShops(rows)
      } catch {
        setShops([])
      }
    }
    try {
      const r = await roleApi.options()
      setRoles(Array.isArray(r) ? r : [])
    } catch {
      try {
        const { rows } = await unwrapPage(roleApi.page({ current: 1, size: 200 }))
        setRoles(rows)
      } catch {
        setRoles([])
      }
    }
    try {
      const tree = await orgApi.tree()
      setOrgOptions(flattenOrgTree(Array.isArray(tree) ? tree : []))
    } catch {
      setOrgOptions([])
    }
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { rows, total: t } = await unwrapPage(
        userAdminApi.page({
          current,
          size,
          username: usernameQ.trim() || undefined,
          orgId: deptIdQ,
        }),
      )
      setList(rows)
      setTotal(t)
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载用户失败')
    } finally {
      setLoading(false)
    }
  }, [current, size, usernameQ, phoneQ, statusQ, deptIdQ, searchNonce])

  useEffect(() => {
    void loadRefs()
  }, [loadRefs])

  useEffect(() => {
    void load()
  }, [load])

  const openEdit = (row: UserRow | null) => {
    setFormErrors({})
    if (row) {
      setForm({
        username: String(row.username ?? ''),
        realName: String(row.realName ?? ''),
        phone: String(row.phone ?? ''),
        status: Number(row.status ?? 1),
        password: '',
        mainShopId: Number(row.mainShopId ?? 0),
        mainOrgId: Number(row.mainOrgId ?? 0),
      })
    } else {
      setForm({
        username: '',
        realName: '',
        phone: '',
        status: 1,
        password: '',
        mainShopId: 0,
        mainOrgId: deptIdQ ?? (orgOptions[0]?.id ?? 0),
      })
    }
    setModal({ open: true, row })
  }

  const save = async () => {
    const isNew = modal.row?.id == null
    const errors = validateUserForm(form, isNew)
    setFormErrors(errors)
    if (Object.keys(errors).length > 0) return

    const body: Partial<UserRow> = {
      username: form.username,
      realName: form.realName,
      phone: form.phone,
      status: form.status,
      mainShopId: form.mainShopId || undefined,
      mainOrgId: form.mainOrgId || undefined,
    }
    if (form.password.trim()) {
      body.password = form.password.trim()
    }
    try {
      if (modal.row?.id != null) {
        await userAdminApi.update(modal.row.id, body)
      } else {
        await userAdminApi.create(body)
      }
      setModal({ open: false, row: null })
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存失败')
    }
  }

  const remove = async (row: UserRow) => {
    if (row.id == null) return
    const name = row.username ?? String(row.id)
    const ok = await confirm({ title: '删除用户', message: `确定删除用户「${name}」？`, danger: true })
    if (!ok) return
    try {
      await userAdminApi.remove(row.id)
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const toggleUserStatus = async (row: UserRow) => {
    if (row.id == null) return
    const next = Number(row.status) === 1 ? 0 : 1
    const action = next === 1 ? '启用' : '停用'
    const ok = await confirm({ title: action, message: `确定要${action}用户「${row.username ?? row.id}」？` })
    if (!ok) return
    try {
      await userAdminApi.setStatus(row.id, next)
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '更新状态失败')
    }
  }

  const openShopRoles = async (row: UserRow) => {
    if (row.id == null) return
    setError('')
    try {
      const raw = await userAdminApi.getShopRoles(row.id)
      setSrRows(parseShopRoles(raw))
      setSrModal({
        open: true,
        userId: row.id,
        username: String(row.username ?? ''),
      })
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载店铺角色失败')
    }
  }

  const addSrRow = () => {
    setSrRows((r) => [
      ...r,
      {
        shopId: Number(shops[0]?.id ?? 0),
        roleId: Number(roles[0]?.id ?? 0),
      },
    ])
  }

  const updateSr = (idx: number, patch: Partial<ShopRoleItem>) => {
    setSrRows((rows) =>
      rows.map((row, i) => (i === idx ? { ...row, ...patch } : row)),
    )
  }

  const removeSr = (idx: number) => {
    setSrRows((rows) => rows.filter((_, i) => i !== idx))
  }

  const saveShopRoles = async () => {
    if (srModal.userId == null) return
    try {
      await userAdminApi.setShopRoles(srModal.userId, srRows)
      setSrModal({ open: false, userId: null, username: '' })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存店铺角色失败')
    }
  }

  const openResetPwd = async (row: UserRow) => {
    if (row.id == null) return
    const ok = await confirm({
      title: '重置密码',
      message: `确定为用户「${row.username ?? row.id}」重置密码？`,
      danger: true,
      confirmText: '继续',
    })
    if (!ok) return
    setResetPwdValue('')
    setResetPwdModal({
      open: true,
      userId: row.id,
      username: String(row.username ?? ''),
    })
  }

  const submitResetPwd = async () => {
    if (resetPwdModal.userId == null) return
    const pwd = resetPwdValue.trim()
    if (!pwd) {
      toast.error('新密码不能为空')
      return
    }
    try {
      await userAdminApi.resetPwd(resetPwdModal.userId, pwd)
      toast.success(`已重置「${resetPwdModal.username || resetPwdModal.userId}」的密码`)
      setResetPwdModal({ open: false, userId: null, username: '' })
      setResetPwdValue('')
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '重置密码失败')
    }
  }

  const shopNameById = (id: number | undefined) => {
    if (!id) return '—'
    const s = shops.find((s) => s.id === id)
    return s?.shopName ?? String(id)
  }

  const handleSearch = () => {
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  const handleReset = () => {
    setUsernameQ('')
    setPhoneQ('')
    setStatusQ(undefined)
    setDeptIdQ(undefined)
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  return (
    <div className="flex gap-6 animate-in fade-in duration-500">
      <OrgSidebar 
        selectedOrgId={deptIdQ} 
        onSelectOrg={(id) => { 
          setDeptIdQ(id)
          setCurrent(1)
          setSearchNonce((n) => n + 1)
        }}
        onOrgOptionsLoaded={setOrgOptions}
      />
      <div className="flex-1 min-w-0 space-y-4">
        {/* Header */}
        <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
          <div>
            <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
            <div className="flex items-center gap-3">
              <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">
                {deptIdQ ? `[${orgOptions.find(o=>o.id===deptIdQ)?.label?.replace(/—+ /g, '') || deptIdQ}] 用户管理` : '全部用户'}
              </h4>
              <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">系统操作员档案</span>
            </div>
          </div>
          <PermGate perms={[SYSTEM_PERMS.user.add]}>
            <button
              type="button"
              onClick={() => openEdit(null)}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-2"
            >
              + 挂靠新增用户
            </button>
          </PermGate>
        </div>

      {/* Search bar */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">用户名</label>
          <input
            placeholder="搜索用户名"
            value={usernameQ}
            onChange={(e) => setUsernameQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">手机号</label>
          <input
            placeholder="搜索手机号"
            value={phoneQ}
            onChange={(e) => setPhoneQ(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">状态</label>
          <select
            value={statusQ ?? ''}
            onChange={(e) =>
              setStatusQ(e.target.value === '' ? undefined : Number(e.target.value))
            }
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-28"
          >
            <option value="">全部</option>
            <option value="1">正常</option>
            <option value="0">停用</option>
          </select>
        </div>

        <PermGate perms={[SYSTEM_PERMS.user.query]}>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleSearch}
              className="px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold"
            >
              查询
            </button>
            <button
              type="button"
              onClick={handleReset}
              className="px-4 py-2 bg-slate-100 rounded-xl text-xs font-bold text-slate-600"
            >
              重置
            </button>
          </div>
        </PermGate>
      </div>

      {error ? (
        <div className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">
          {error}
        </div>
      ) : null}

      {/* Table */}
      <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden">
        <table className="w-full text-left text-sm font-bold">
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black">
            <tr>
              <th className="px-6 py-3">用户名</th>
              <th className="px-6 py-3">姓名</th>
              <th className="px-6 py-3">手机</th>
              <th className="px-6 py-3">所属部门</th>
              <th className="px-6 py-3">所属店铺</th>
              <th className="px-6 py-3">状态</th>
              <th className="px-6 py-3">创建时间</th>
              <th className="px-6 py-3 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {loading ? (
              <tr>
                <td colSpan={8} className="px-6 py-8 text-center text-slate-400">
                  加载中...
                </td>
              </tr>
            ) : list.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-6 py-8 text-center text-slate-300">
                  暂无数据
                </td>
              </tr>
            ) : (
              list.map((u) => (
                <tr key={u.id} className="hover:bg-slate-50/80">
                  <td className="px-6 py-3">{u.username}</td>
                  <td className="px-6 py-3">{u.realName}</td>
                  <td className="px-6 py-3 text-slate-500">{u.phone}</td>
                  <td className="px-6 py-3 text-slate-500 text-xs">
                    {u.deptName ?? (u.mainOrgId ? `Org#${u.mainOrgId}` : '—')}
                  </td>
                  <td className="px-6 py-3 text-slate-500 text-xs">
                    {u.shopName ?? shopNameById(u.mainShopId)}
                  </td>
                  <td className="px-6 py-3">
                    {can(SYSTEM_PERMS.user.edit) ? (
                      <button
                        type="button"
                        onClick={() => void toggleUserStatus(u)}
                        className={`text-xs font-bold px-2 py-1 rounded-lg ${
                          Number(u.status) === 1
                            ? 'bg-emerald-50 text-emerald-600'
                            : 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        {Number(u.status) === 1 ? '正常' : '停用'}
                      </button>
                    ) : (
                      <span
                        className={`inline-flex rounded-full px-2.5 py-0.5 text-[10px] font-black ${
                          Number(u.status) === 1
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        {Number(u.status) === 1 ? '正常' : '停用'}
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-3 text-slate-400 text-xs">{u.createTime ?? '—'}</td>
                  <td className="px-6 py-3 text-right space-x-2">
                    <PermGate perms={[SYSTEM_PERMS.user.edit]}>
                      <button
                        type="button"
                        onClick={() => void openShopRoles(u)}
                        className="text-indigo-600 text-xs font-bold"
                      >
                        店铺角色
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.user.resetPwd]}>
                      <button
                        type="button"
                        onClick={() => void openResetPwd(u)}
                        className="text-amber-600 text-xs font-bold"
                      >
                        重置密码
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.user.edit]}>
                      <button
                        type="button"
                        onClick={() => openEdit(u)}
                        className="text-slate-700 text-xs font-bold"
                      >
                        编辑
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.user.remove]}>
                      <button
                        type="button"
                        onClick={() => void remove(u)}
                        className="text-rose-600 text-xs font-bold"
                      >
                        删除
                      </button>
                    </PermGate>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <div className="p-4 flex justify-between items-center border-t border-slate-50 text-xs text-slate-500">
          <span>共 {total} 条</span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={current <= 1}
              onClick={() => setCurrent((c) => Math.max(1, c - 1))}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              上一页
            </button>
            <button
              type="button"
              disabled={current * size >= total}
              onClick={() => setCurrent((c) => c + 1)}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      {/* Edit/Create Modal */}
      <Modal
        open={modal.open}
        onClose={() => setModal({ open: false, row: null })}
        title={modal.row ? '编辑用户' : '新增用户'}
        maxWidth="max-w-md"
      >
        <div className="space-y-2">
            {/* username */}
            <label className="text-[10px] font-black text-slate-500 block">
              用户名 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.username}
              onChange={(e) =>
                setForm((f) => ({ ...f, username: e.target.value }))
              }
              disabled={!!modal.row}
              className={`w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 text-sm disabled:opacity-60 ${
                formErrors.username ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.username && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.username}</p>
            )}

            {/* realName */}
            <label className="text-[10px] font-black text-slate-500 block">
              姓名 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.realName}
              onChange={(e) =>
                setForm((f) => ({ ...f, realName: e.target.value }))
              }
              className={`w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 text-sm ${
                formErrors.realName ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.realName && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.realName}</p>
            )}

            {/* phone */}
            <label className="text-[10px] font-black text-slate-500 block">
              手机
            </label>
            <input
              value={form.phone}
              onChange={(e) =>
                setForm((f) => ({ ...f, phone: e.target.value }))
              }
              className="w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm"
            />

            {/* status */}
            <label className="text-[10px] font-black text-slate-500 block">
              状态
            </label>
            <select
              value={form.status}
              onChange={(e) =>
                setForm((f) => ({ ...f, status: Number(e.target.value) }))
              }
              className="w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm"
            >
              <option value={1}>正常</option>
              <option value={0}>停用</option>
            </select>

            {/* password */}
            <label className="text-[10px] font-black text-slate-500 block">
              密码 {!modal.row && <span className="text-rose-500">*</span>}
              {modal.row && '（留空则不改）'}
            </label>
            <input
              type="password"
              value={form.password}
              onChange={(e) =>
                setForm((f) => ({ ...f, password: e.target.value }))
              }
              className={`w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 text-sm ${
                formErrors.password ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.password && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.password}</p>
            )}

            {/* mainShopId — required for DataScope */}
            <label className="text-[10px] font-black text-slate-500 block">
              主店铺 <span className="text-rose-500">*</span>
            </label>
            <select
              value={form.mainShopId}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  mainShopId: Number(e.target.value),
                }))
              }
              className={`w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 text-sm ${
                formErrors.mainShopId ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            >
              <option value={0}>— 请选择店铺 —</option>
              {shops.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.shopName ?? s.id}
                </option>
              ))}
            </select>
            {formErrors.mainShopId && (
              <p className="text-[10px] text-rose-600 font-bold bg-rose-50 rounded-lg px-2 py-1">
                {formErrors.mainShopId}
              </p>
            )}

            {/* mainOrgId — required for DataScope */}
            <label className="text-[10px] font-black text-slate-500 block">
              所属部门 <span className="text-rose-500">*</span>
            </label>
            <select
              value={form.mainOrgId}
              onChange={(e) =>
                setForm((f) => ({
                  ...f,
                  mainOrgId: Number(e.target.value) || 0,
                }))
              }
              className={`w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 text-sm ${
                formErrors.mainOrgId ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            >
              <option value={0}>— 请选择部门 —</option>
              {orgOptions.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.label}
                </option>
              ))}
            </select>
            {formErrors.mainOrgId && (
              <p className="text-[10px] text-rose-600 font-bold bg-rose-50 rounded-lg px-2 py-1">
                {formErrors.mainOrgId}
              </p>
            )}

            <div className="flex justify-end gap-2 pt-3">
              <button
                type="button"
                onClick={() => setModal({ open: false, row: null })}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                取消
              </button>
              <button
                type="button"
                onClick={() => void save()}
                className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
              >
                保存
              </button>
            </div>
        </div>
      </Modal>

      {/* Shop-Role Modal */}
      <Modal
        open={srModal.open}
        onClose={() => setSrModal({ open: false, userId: null, username: '' })}
        title={`店铺角色分配 — ${srModal.username}`}
        maxWidth="max-w-lg"
      >
        <div className="flex flex-col">
            <p className="text-[10px] text-slate-400 mb-2">
              每行一条：店铺 + 角色，保存时整体提交 items
            </p>
            <div className="flex-1 overflow-y-auto space-y-2 mb-3">
              {srRows.map((row, idx) => (
                <div
                  key={idx}
                  className="flex gap-2 items-center flex-wrap bg-slate-50 p-2 rounded-xl"
                >
                  <select
                    value={row.shopId}
                    onChange={(e) =>
                      updateSr(idx, { shopId: Number(e.target.value) })
                    }
                    className="flex-1 min-w-[120px] px-2 py-1 rounded-lg text-xs font-bold"
                  >
                    {shops.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.shopName ?? s.id}
                      </option>
                    ))}
                  </select>
                  <select
                    value={row.roleId}
                    onChange={(e) =>
                      updateSr(idx, { roleId: Number(e.target.value) })
                    }
                    className="flex-1 min-w-[120px] px-2 py-1 rounded-lg text-xs font-bold"
                  >
                    {roles.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.roleName ?? r.roleCode ?? r.id}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={() => removeSr(idx)}
                    className="text-rose-600 text-xs font-bold px-2"
                  >
                    删行
                  </button>
                </div>
              ))}
            </div>
            <button
              type="button"
              onClick={addSrRow}
              className="mb-3 px-3 py-2 bg-slate-100 rounded-xl text-xs font-bold self-start"
            >
              + 添加一行
            </button>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() =>
                  setSrModal({ open: false, userId: null, username: '' })
                }
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                取消
              </button>
              <button
                type="button"
                onClick={() => void saveShopRoles()}
                className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
              >
                保存分配
              </button>
            </div>
        </div>
      </Modal>

      {/* Reset Password Modal */}
      <Modal
        open={resetPwdModal.open}
        onClose={() => {
          setResetPwdModal({ open: false, userId: null, username: '' })
          setResetPwdValue('')
        }}
        title={`重置密码 — ${resetPwdModal.username || '用户'}`}
        maxWidth="max-w-md"
      >
        <div className="space-y-3">
          <div className="text-[10px] text-slate-400 font-bold">
            请输入新密码并提交。提交后该用户现有会话将被强制失效（需重新登录）。
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-black text-slate-500 block">
              新密码 <span className="text-rose-500">*</span>
            </label>
            <input
              type="password"
              value={resetPwdValue}
              onChange={(e) => setResetPwdValue(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm"
              placeholder="请输入新密码"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => {
                setResetPwdModal({ open: false, userId: null, username: '' })
                setResetPwdValue('')
              }}
              className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
            >
              取消
            </button>
            <button
              type="button"
              onClick={() => void submitResetPwd()}
              className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
            >
              提交
            </button>
          </div>
        </div>
      </Modal>
      </div>
    </div>
  )
}
