import { useCallback, useEffect, useState } from 'react'
import { configApi, unwrapPage } from '../../api/system-crud'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import { useToast } from '../../components/Toast'
import { PermGate, usePermissions } from '../../context/PermissionsContext'
import { formatDateTime } from '../../lib/format'
import { useStaleGuard } from '../../hooks/useStaleGuard'
import { SYSTEM_PERMS } from '../../lib/system-perms'
import type { ConfigRow } from '../../types/system-crud'

export default function ConfigManage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { can } = usePermissions()

  const [list, setList] = useState<ConfigRow[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [size, setSize] = useState(10)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // --- search ---
  const [configNameQ, setConfigNameQ] = useState('')
  const [configKeyQ, setConfigKeyQ] = useState('')
  const [draft, setDraft] = useState({ configName: '', configKey: '' })
  const [searchNonce, setSearchNonce] = useState(0)

  // --- modal ---
  const [modal, setModal] = useState<{ open: boolean; row: ConfigRow | null }>({
    open: false,
    row: null,
  })
  const [form, setForm] = useState({
    configName: '',
    configKey: '',
    configValue: '',
    configType: 'N' as string,
    remark: '',
  })
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const guard = useStaleGuard()

  const load = useCallback(async () => {
    const id = guard.nextId()
    setLoading(true)
    setError('')
    try {
      const { rows, total: t } = await unwrapPage(
        configApi.page({
          current,
          size,
          configName: configNameQ.trim() || undefined,
          configKey: configKeyQ.trim() || undefined,
        }),
      )
      if (!guard.isCurrent(id)) return
      setList(rows)
      setTotal(t)
    } catch (e) {
      if (!guard.isCurrent(id)) return
      setError(e instanceof Error ? e.message : '加载参数配置失败')
    } finally {
      if (!guard.isCurrent(id)) return
      setLoading(false)
    }
  }, [current, size, configNameQ, configKeyQ, searchNonce, guard])

  useEffect(() => {
    void load()
  }, [load])

  const openEdit = async (row: ConfigRow | null) => {
    setFormErrors({})
    if (row?.id != null) {
      try {
        const detail = await configApi.get(row.id)
        setForm({
          configName: String(detail.configName ?? ''),
          configKey: String(detail.configKey ?? ''),
          configValue: String(detail.configValue ?? ''),
          configType: String(detail.configType ?? 'N'),
          remark: String(detail.remark ?? ''),
        })
      } catch (e) {
        toast.error(e instanceof Error ? e.message : '获取配置详情失败')
        return
      }
      setModal({ open: true, row })
    } else {
      setForm({ configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
      setModal({ open: true, row: null })
    }
  }

  const save = async () => {
    const errors: Record<string, string> = {}
    if (!form.configName.trim()) errors.configName = '参数名称不能为空'
    if (!form.configKey.trim()) errors.configKey = '参数键名不能为空'
    setFormErrors(errors)
    if (Object.keys(errors).length > 0) return

    const body: Partial<ConfigRow> = {
      configName: form.configName,
      configKey: form.configKey,
      configValue: form.configValue,
      configType: form.configType,
      remark: form.remark,
    }
    if (submitting) return
    setSubmitting(true)
    try {
      if (modal.row?.id != null) {
        await configApi.update(modal.row.id, body)
      } else {
        await configApi.create(body)
      }
      setModal({ open: false, row: null })
      toast.success('配置已保存')
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  const remove = async (row: ConfigRow) => {
    if (row.id == null) return
    const name = row.configName ?? row.configKey ?? String(row.id)
    const ok = await confirm({
      title: '删除配置',
      message: `确定删除参数配置「${name}」？此操作不可撤销。`,
      danger: true,
    })
    if (!ok) return
    try {
      await configApi.remove(row.id)
      toast.success('配置已删除')
      await load()
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const handleSearch = () => {
    setConfigNameQ(draft.configName)
    setConfigKeyQ(draft.configKey)
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  const handleReset = () => {
    setDraft({ configName: '', configKey: '' })
    setConfigNameQ('')
    setConfigKeyQ('')
    setCurrent(1)
    setSearchNonce((n) => n + 1)
  }

  if (!can(SYSTEM_PERMS.config.query)) {
    return (
      <div className="bg-white p-10 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100 text-center text-slate-500 font-bold">
        无权限访问：参数配置
      </div>
    )
  }

  /** Whether the row being edited is a built-in config */
  const isBuiltIn = modal.row?.configType === 'Y'

  return (
    <div className="space-y-4 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-wrap gap-3 justify-between items-center bg-white p-6 rounded-[2.5rem] shadow-sm ring-1 ring-slate-100">
        <div>
          <div className="text-[10px] font-black text-slate-400 uppercase mb-1 tracking-wider">业务上下文</div>
          <div className="flex items-center gap-3">
            <h4 className="font-black text-slate-900 border-l-4 border-indigo-600 pl-3">参数配置</h4>
            <span className="bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded-md text-xs font-bold ring-1 ring-indigo-500/20">系统全局视图</span>
          </div>
        </div>
        <PermGate perms={[SYSTEM_PERMS.config.add]}>
          <button
            type="button"
            onClick={() => void openEdit(null)}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition flex items-center gap-2"
          >
            + 新增配置
          </button>
        </PermGate>
      </div>

      {/* Search bar */}
      <div className="bg-white rounded-2xl p-4 shadow-sm ring-1 ring-slate-100 flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">参数名称</label>
          <input
            placeholder="搜索名称"
            value={draft.configName}
            onChange={(e) => setDraft((prev) => ({ ...prev, configName: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <div className="space-y-1">
          <label className="text-[10px] font-black text-slate-400 uppercase">参数键名</label>
          <input
            placeholder="搜索键名"
            value={draft.configKey}
            onChange={(e) => setDraft((prev) => ({ ...prev, configKey: e.target.value }))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
            className="px-3 py-2 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm w-36"
          />
        </div>
        <PermGate perms={[SYSTEM_PERMS.config.query]}>
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
          <thead className="bg-slate-50 text-[10px] text-slate-400 uppercase font-black tracking-widest">
            <tr>
              <th className="px-8 py-5">参数名称</th>
              <th className="px-8 py-5">参数键名</th>
              <th className="px-8 py-5">参数值</th>
              <th className="px-8 py-5">类型</th>
              <th className="px-8 py-5">备注</th>
              <th className="px-8 py-5">创建时间</th>
              <th className="px-8 py-5 text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {loading ? (
              <tr>
                <td colSpan={7} className="px-8 py-8 text-center text-slate-400">
                  加载中...
                </td>
              </tr>
            ) : list.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-8 py-8 text-center text-slate-300">
                  暂无数据
                </td>
              </tr>
            ) : (
              list.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80">
                  <td className="px-8 py-5">{r.configName}</td>
                  <td className="px-8 py-5 text-slate-500 font-mono text-xs">{r.configKey}</td>
                  <td className="px-8 py-5 max-w-[180px] truncate text-xs text-slate-700" title={r.configValue ?? ''}>
                    {r.configValue || '—'}
                  </td>
                  <td className="px-8 py-5">
                    <span
                      className={`px-2 py-1 rounded-lg text-xs ${
                        r.configType === 'Y'
                          ? 'bg-indigo-50 text-indigo-700'
                          : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {r.configType === 'Y' ? '内置' : '自定义'}
                    </span>
                  </td>
                  <td className="px-8 py-5 text-slate-400 text-xs max-w-[120px] truncate">
                    {r.remark || '—'}
                  </td>
                  <td className="px-8 py-5 text-slate-400 text-xs whitespace-nowrap">{formatDateTime(r.createTime)}</td>
                  <td className="px-8 py-5 text-right space-x-2">
                    <PermGate perms={[SYSTEM_PERMS.config.edit]}>
                      <button
                        type="button"
                        onClick={() => void openEdit(r)}
                        className="text-amber-600 hover:text-amber-700 text-xs font-bold"
                      >
                        编辑
                      </button>
                    </PermGate>
                    <PermGate perms={[SYSTEM_PERMS.config.remove]}>
                      <button
                        type="button"
                        onClick={() => void remove(r)}
                        className="text-rose-600 hover:text-rose-700 text-xs font-bold"
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
              disabled={current <= 1}
              onClick={() => setCurrent((c) => Math.max(1, c - 1))}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              上一页
            </button>
            <button
              type="button"
              disabled={current >= Math.max(1, Math.ceil(total / size))}
              onClick={() => setCurrent((c) => c + 1)}
              className="px-3 py-1 rounded-lg bg-slate-100 font-bold disabled:opacity-40"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      {/* Edit / Create Modal */}
      <Modal
        open={modal.open}
        onClose={() => setModal({ open: false, row: null })}
        title={modal.row ? '编辑配置' : '新增配置'}
      >
        <div className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">
              参数名称 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.configName}
              onChange={(e) => setForm((f) => ({ ...f, configName: e.target.value }))}
              className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 text-sm font-bold focus:ring-indigo-500 outline-none transition ${
                formErrors.configName ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.configName && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.configName}</p>
            )}
          </div>

          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">
              参数键名 <span className="text-rose-500">*</span>
            </label>
            <input
              value={form.configKey}
              onChange={(e) => setForm((f) => ({ ...f, configKey: e.target.value }))}
              disabled={!!modal.row}
              className={`w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 text-sm font-bold focus:ring-indigo-500 outline-none transition disabled:opacity-60 ${
                formErrors.configKey ? 'ring-rose-400' : 'ring-slate-200'
              }`}
            />
            {formErrors.configKey && (
              <p className="text-[10px] text-rose-600 font-bold">{formErrors.configKey}</p>
            )}
          </div>

          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">参数值</label>
            <input
              value={form.configValue}
              onChange={(e) => setForm((f) => ({ ...f, configValue: e.target.value }))}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">类型</label>
            <select
              value={form.configType}
              onChange={(e) => setForm((f) => ({ ...f, configType: e.target.value }))}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            >
              <option value="Y">内置</option>
              <option value="N">自定义</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-black text-slate-500 block">备注</label>
            <input
              value={form.remark}
              onChange={(e) => setForm((f) => ({ ...f, remark: e.target.value }))}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500 outline-none transition"
            />
          </div>

          {isBuiltIn && (
            <p className="text-[10px] text-amber-600 font-bold bg-amber-50 rounded-lg px-2 py-1">
              内置参数的键名不可修改
            </p>
          )}

          <div className="flex justify-end gap-2 pt-2">
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
                disabled={submitting}
              className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
            >
                {submitting ? '保存中...' : '保存'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
