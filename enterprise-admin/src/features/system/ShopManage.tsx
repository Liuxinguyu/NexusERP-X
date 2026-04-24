import { useCallback, useEffect, useState } from 'react'
import { shopApi, unwrapPage } from '../../api/system-crud'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { formatDateTime } from '../../lib/format'
import { useStaleGuard } from '../../hooks/useStaleGuard'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import type { ShopItem } from '../../types/system-crud'
import OrgSidebar from './components/OrgSidebar'

// --- 枚举映射集中的管理 ---
const SHOP_STATUS_MAP: Record<number, { label: string; bg: string; text: string }> = {
  1: { label: '正常', bg: 'bg-emerald-50', text: 'text-emerald-600' },
  0: { label: '停用', bg: 'bg-slate-100', text: 'text-slate-500' },
}

const SHOP_TYPE_MAP: Record<number, string> = {
  1: '直营店',
  2: '加盟店',
  3: '联营店',
}

function getStatusInfo(status?: number) {
  return SHOP_STATUS_MAP[status ?? 1] ?? SHOP_STATUS_MAP[0]
}

function getShopTypeLabel(type?: number) {
  return SHOP_TYPE_MAP[type ?? 1] ?? String(type)
}

type FormErrors = Record<string, string>

function validateShopForm(form: { orgId: number; shopName: string }): FormErrors {
  const errors: FormErrors = {}
  if (!form.shopName.trim()) errors.shopName = '店铺名称不能为空'
  if (!form.orgId || form.orgId <= 0)
    errors.orgId = '⚠️ 所属机构不可为空'
  return errors
}

export default function ShopManage() {
  const { can } = usePermissions()
  const toast = useToast()
  const confirm = useConfirm()
  
  // -- 店铺相关的 State --
  const [list, setList] = useState<ShopItem[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [size, setSize] = useState(10)
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const [queryShopName, setQueryShopName] = useState('')
  const [draftShopName, setDraftShopName] = useState('')
  const [searchNonce, setSearchNonce] = useState(0)
  const [selectedOrgId, setSelectedOrgId] = useState<number | undefined>(undefined)

  const [shopModal, setShopModal] = useState<{ open: boolean; isEdit: boolean; id?: number }>({
    open: false,
    isEdit: false,
  })
  const [shopForm, setShopForm] = useState({ orgId: 0, shopName: '', shopType: 1, remark: '' })
  const [shopFormErrors, setShopFormErrors] = useState<FormErrors>({})
  const [submitting, setSubmitting] = useState(false)

  // -- 来自 OrgSidebar 的引用数据 --
  const [orgOptions, setOrgOptions] = useState<{ id: number; label: string }[]>([])
  const guard = useStaleGuard()

  // 1. 加载店铺表格数据
  const loadShops = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    setError('')
    try {
      const { rows, total: t } = await unwrapPage(
        shopApi.getShopPage({
          current,
          size,
          shopName: queryShopName.trim() || undefined,
          orgId: selectedOrgId,
        })
      )
      if (!guard.isCurrent(id)) return
      setList(rows)
      setTotal(t)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      setError(e instanceof Error ? e.message : '加载店铺失败')
    } finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [current, size, queryShopName, searchNonce, selectedOrgId, guard])

  useEffect(() => {
    void loadShops()
  }, [loadShops])

  // ================= 店铺 CRUD =================

  const handleSearch = () => {
    setQueryShopName(draftShopName)
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  const handleReset = () => {
    setDraftShopName('')
    setQueryShopName('')
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  const openCreateShop = () => {
    setShopFormErrors({})
    setShopForm({
      orgId: selectedOrgId ?? (orgOptions[0]?.id ?? 0),
      shopName: '',
      shopType: 1,
      remark: '',
    })
    setShopModal({ open: true, isEdit: false })
  }

  const openEditShop = async (id: number) => {
    setShopFormErrors({})
    setDetailLoading(true)
    try {
      const detail = await shopApi.getShopDetail(id)
      setShopForm({
        orgId: Number(detail.orgId ?? 0),
        shopName: String(detail.shopName ?? ''),
        shopType: Number(detail.shopType ?? 1),
        remark: String(detail.remark ?? ''),
      })
      setShopModal({ open: true, isEdit: true, id })
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '拉取详情失败')
    } finally {
      setDetailLoading(false)
    }
  }

  const submitShopForm = async () => {
    const errors = validateShopForm(shopForm)
    setShopFormErrors(errors)
    if (Object.keys(errors).length > 0) return

    if (submitting) return
    setSubmitting(true)
    try {
      if (shopModal.isEdit && shopModal.id != null) {
        await shopApi.updateShop(shopModal.id, shopForm)
      } else {
        await shopApi.createShop(shopForm)
      }
      setShopModal({ open: false, isEdit: false })
      await loadShops()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  const removeShop = async (row: ShopItem) => {
    if (row.id == null) return
    const ok = await confirm({ title: '删除店铺', message: `确定删除店铺「${row.shopName ?? String(row.id)}」？操作不可恢复。`, danger: true })
    if (!ok) return
    setLoading(true)
    try {
      await shopApi.deleteShop(row.id)
      await loadShops()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    } finally {
      setLoading(false)
    }
  }

  const toggleShopStatus = async (row: ShopItem) => {
    if (row.id == null) return
    const nextStatus = Number(row.status) === 1 ? 0 : 1
    const name = row.shopName ?? String(row.id)
    const ok = await confirm({ title: `${nextStatus === 1 ? '启用' : '停用'}店铺`, message: `确定要${nextStatus === 1 ? '启用' : '停用'}店铺「${name}」吗？` })
    if (!ok) return

    setLoading(true)
    try {
      await shopApi.updateShopStatus(row.id, { status: nextStatus })
      await loadShops()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '更新状态失败')
    } finally {
      setLoading(false)
    }
  }

  const getOrgName = (orgId?: number) => {
    if (!orgId) return '未知（' + String(orgId) + '）'
    const match = orgOptions.find((o) => o.id === orgId)
    return match ? match.label.replace(/—+ /g, '') : `机构 ${orgId}`
  }

  if (!can(SYSTEM_PERMS.shop.query)) {
    return (
      <div className="bg-white p-10 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 text-center text-slate-500 font-bold">
        无权限访问：店铺管理
      </div>
    )
  }

  return (
    <div className="flex gap-6 animate-in fade-in duration-500">
      
      {/* 左侧：组织机构树管理，已被解耦到独立公共组件中 */}
      <OrgSidebar 
        selectedOrgId={selectedOrgId} 
        onSelectOrg={(id) => { setSelectedOrgId(id); setCurrent(1); }}
        onOrgOptionsLoaded={setOrgOptions}
      />

      {/* 右侧：店铺实体列表 */}
      <div className="flex-1 min-w-0 space-y-4">
        <div className="flex justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
          <div>
            <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
            <div className="text-xl font-black text-slate-800 flex items-center gap-3">
              {selectedOrgId ? getOrgName(selectedOrgId) : '全局视角'}
              {selectedOrgId && (
                <span className="text-[10px] bg-slate-100 text-slate-500 px-2 py-0.5 rounded-md font-bold self-center">
                  OrgID: {selectedOrgId}
                </span>
              )}
            </div>
          </div>
          <PermGate perms={[SYSTEM_PERMS.shop.add]}>
            <button
              type="button"
              onClick={openCreateShop}
              className="px-5 py-2.5 bg-indigo-600 text-white rounded-xl text-xs font-black shadow-lg shadow-indigo-200 hover:bg-indigo-700 transition"
            >
              ⊕ 挂靠新增店铺
            </button>
          </PermGate>
        </div>
        
        {/* Search bar */}
        <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
          <div className="space-y-1">
            <label className="text-[10px] font-black text-slate-400 uppercase">店铺名称检索</label>
            <input
              placeholder="搜索当前组织下的店铺名"
              value={draftShopName}
              onChange={(e) => setDraftShopName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleSearch()
              }}
              className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-56 focus:ring-indigo-500 outline-none transition"
            />
          </div>
          <PermGate perms={[SYSTEM_PERMS.shop.query]}>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleSearch}
                className="px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold shadow-sm hover:shadow-md transition"
              >
                查询
              </button>
              <button
                type="button"
                onClick={handleReset}
                className="px-4 py-2 bg-slate-50 ring-1 ring-slate-200 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100 transition"
              >
                重置
              </button>
            </div>
          </PermGate>
        </div>
        
        {error && (
          <div className="text-sm text-red-600 bg-red-50 rounded-xl px-4 py-3 font-bold block animate-in fade-in">
            ⚠️ {error}
          </div>
        )}

        {/* Table */}
        <div className="bg-white rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 overflow-hidden relative">
          {loading && (
            <div className="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-10 flex items-center justify-center">
              <div className="h-8 w-8 rounded-full border-[3px] border-indigo-600 border-t-transparent animate-spin"></div>
            </div>
          )}
          <table className="w-full text-left text-sm font-bold">
            <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black">
              <tr>
                <th className="px-6 py-4 border-b border-slate-100">底层商业单元 (店铺名称)</th>
                <th className="px-6 py-4 border-b border-slate-100">单元类型</th>
                <th className="px-6 py-4 border-b border-slate-100">直属挂靠节点</th>
                <th className="px-6 py-4 border-b border-slate-100">营运状态</th>
                <th className="px-6 py-4 border-b border-slate-100">创建时间</th>
                <th className="px-6 py-4 border-b border-slate-100 text-right">管理动作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {list.length === 0 && !loading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-16 text-center">
                    <div className="text-slate-300 font-black text-lg">暂无关联店铺</div>
                    <div className="text-slate-400 text-xs font-normal mt-1">此组织机构域下尚未分配店铺实体</div>
                  </td>
                </tr>
              ) : (
                list.map((r) => {
                  const statusInfo = getStatusInfo(Number(r.status))
                  return (
                   <tr key={r.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-5 flex items-center gap-3">
                      <div className="h-8 w-8 rounded-xl bg-indigo-50 flex items-center justify-center text-indigo-400 text-lg">🏪</div>
                      <span>{r.shopName}</span>
                    </td>
                    <td className="px-6 py-5 text-slate-500">
                      <span className="bg-slate-50 ring-1 ring-slate-100 px-2.5 py-1 rounded-lg text-xs tracking-wide">
                        {getShopTypeLabel(Number(r.shopType))}
                      </span>
                    </td>
                    <td className="px-6 py-5 text-slate-600 text-xs flex items-center gap-2">
                       <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 hidden sm:inline-block"></span>
                       {getOrgName(Number(r.orgId))}
                    </td>
                    <td className="px-6 py-5">
                      {can(SYSTEM_PERMS.shop.edit) ? (
                        <button
                          type="button"
                          onClick={() => void toggleShopStatus(r)}
                          className={`text-[10px] font-black px-2.5 py-1.5 rounded-lg transition active:scale-95 ${statusInfo.bg} ${statusInfo.text} ring-1 ring-black/5 hover:brightness-95`}
                        >
                          {statusInfo.label}
                        </button>
                      ) : (
                        <span className={`text-[10px] font-black px-2.5 py-1.5 rounded-lg inline-block ${statusInfo.bg} ${statusInfo.text} ring-1 ring-black/5`}>
                          {statusInfo.label}
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-5 text-slate-500 text-xs">{formatDateTime(r.createTime)}</td>
                    <td className="px-6 py-5 text-right space-x-3">
                      <PermGate perms={[SYSTEM_PERMS.shop.edit]}>
                        <button
                          type="button"
                          onClick={() => {
                            if (r.id) void openEditShop(r.id)
                          }}
                          className="text-indigo-600 text-xs font-bold hover:text-indigo-800 transition"
                        >
                          编辑
                        </button>
                      </PermGate>
                      <PermGate perms={[SYSTEM_PERMS.shop.remove]}>
                        <button
                          type="button"
                          onClick={() => void removeShop(r)}
                          className="text-rose-600 text-xs font-bold hover:text-rose-800 transition"
                        >
                          删除
                        </button>
                      </PermGate>
                    </td>
                   </tr>
                  )
                })
              )}
            </tbody>
          </table>
          <div className="p-5 flex justify-between items-center bg-slate-50 text-xs text-slate-500 border-t border-slate-100">
            <span className="font-bold">查询命中 <span className="text-slate-900">{total}</span> 个商业单元</span>
            <div className="flex items-center gap-2">
              <select
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value))
                  setCurrent(1)
                }}
                className="px-2 py-1 bg-white rounded-lg ring-1 ring-slate-200"
              >
                <option value={10}>10条/页</option>
                <option value={20}>20条/页</option>
                <option value={50}>50条/页</option>
              </select>
              <span>第 {current} / {Math.max(1, Math.ceil(total / size))} 页</span>
              <button
                type="button"
                disabled={current <= 1 || loading}
                onClick={() => setCurrent((c) => Math.max(1, c - 1))}
                className="px-3 py-1.5 rounded-lg bg-white shadow-sm ring-1 ring-slate-200 font-black text-slate-600 disabled:opacity-40 transition hover:bg-slate-50 disabled:hover:bg-white"
              >
                ← 上一页
              </button>
              <button
                type="button"
                disabled={current >= Math.max(1, Math.ceil(total / size)) || loading}
                onClick={() => setCurrent((c) => c + 1)}
                className="px-3 py-1.5 rounded-lg bg-white shadow-sm ring-1 ring-slate-200 font-black text-slate-600 disabled:opacity-40 transition hover:bg-slate-50 disabled:hover:bg-white"
              >
                下一页 →
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* ================= MODALS ================= */}



      {/* 店铺新增/编辑弹窗 */}
      <Modal open={shopModal.open} onClose={() => setShopModal({ open: false, isEdit: false })} title={shopModal.isEdit ? '编辑关联实体店铺' : '新建关联实体店铺'}>
            <div className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <label className="text-xs font-black text-slate-500 block">
                  直属机构节点选定 <span className="text-rose-500">*</span>
                </label>
                <select
                  value={shopForm.orgId}
                  onChange={(e) =>
                    setShopForm((f) => ({ ...f, orgId: Number(e.target.value) }))
                  }
                  className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 text-sm font-bold focus:ring-indigo-500 outline-none transition ${
                    shopFormErrors.orgId ? 'ring-rose-400 bg-rose-50 text-rose-900' : 'ring-slate-200 text-slate-700'
                  }`}
                >
                  <option value={0}>— 此处挂载必需依托某一级真实节点 —</option>
                  {orgOptions.map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.label}
                    </option>
                  ))}
                </select>
                {shopFormErrors.orgId && (
                  <p className="text-[10px] text-rose-600 font-bold mt-1">
                    {shopFormErrors.orgId}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-black text-slate-500 block">
                  店铺名称 <span className="text-rose-500">*</span>
                </label>
                <input
                  value={shopForm.shopName}
                  onChange={(e) =>
                    setShopForm((f) => ({ ...f, shopName: e.target.value }))
                  }
                  className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 text-sm font-bold focus:ring-indigo-500 outline-none transition ${
                    shopFormErrors.shopName ? 'ring-rose-400 bg-rose-50' : 'ring-slate-200'
                  }`}
                  placeholder="请输入店铺名称"
                />
                {shopFormErrors.shopName && (
                  <p className="text-[10px] text-rose-600 font-bold mt-1">
                    {shopFormErrors.shopName}
                  </p>
                )}
              </div>

              <div className="flex gap-4">
                 <div className="space-y-1.5 flex-1">
                    <label className="text-xs font-black text-slate-500 block">店铺类型</label>
                    <select
                      value={shopForm.shopType}
                      onChange={(e) =>
                        setShopForm((f) => ({ ...f, shopType: Number(e.target.value) }))
                      }
                      className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
                    >
                      {Object.entries(SHOP_TYPE_MAP).map(([val, label]) => (
                        <option key={val} value={Number(val)}>{label}</option>
                      ))}
                    </select>
                 </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-black text-slate-500 block">其他备注信息</label>
                <textarea
                  value={shopForm.remark}
                  onChange={(e) =>
                    setShopForm((f) => ({ ...f, remark: e.target.value }))
                  }
                  rows={2}
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm focus:ring-indigo-500 outline-none transition resize-none"
                  placeholder="可选填..."
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-4">
              <button
                type="button"
                onClick={() => setShopModal({ open: false, isEdit: false })}
                disabled={submitting}
                className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-700 text-sm font-black hover:bg-slate-200 transition"
              >
                关闭
              </button>
              <button
                type="button"
                onClick={() => void submitShopForm()}
                disabled={submitting}
                className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-black shadow-md hover:bg-indigo-700 transition flex items-center gap-2"
              >
                {submitting ? '提交封存...' : detailLoading ? '加载中...' : '落库提交'}
              </button>
            </div>
      </Modal>
    </div>
  )
}
