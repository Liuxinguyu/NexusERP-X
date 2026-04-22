import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

interface ConfirmOptions {
  title?: string
  message: string
  danger?: boolean
  confirmText?: string
  cancelText?: string
}

type ConfirmFn = (opts: ConfirmOptions) => Promise<boolean>

const ConfirmContext = createContext<ConfirmFn | null>(null)

export function useConfirm(): ConfirmFn {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider')
  return ctx
}

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<(ConfirmOptions & { resolve: (v: boolean) => void }) | null>(null)
  const resolveRef = useRef<((v: boolean) => void) | null>(null)

  const confirm = useCallback<ConfirmFn>((opts) => {
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve
      setState({ ...opts, resolve })
    })
  }, [])

  const close = useCallback((result: boolean) => {
    resolveRef.current?.(result)
    resolveRef.current = null
    setState(null)
  }, [])

  useEffect(() => {
    if (!state) return
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') close(false) }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [state, close])

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {state && createPortal(
        <div
          className="fixed inset-0 z-[140] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4 animate-in fade-in duration-200"
          onClick={() => close(false)}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="w-full max-w-sm bg-white rounded-[2rem] p-8 shadow-2xl space-y-5 animate-in zoom-in-95 duration-200"
            onClick={e => e.stopPropagation()}
          >
            {state.title && <h3 className="font-black text-lg text-slate-800">{state.title}</h3>}
            <p className="text-sm text-slate-600">{state.message}</p>
            <div className="flex gap-3 pt-2">
              <button
                onClick={() => close(false)}
                className="flex-1 py-2.5 bg-white rounded-xl ring-1 ring-slate-200 text-sm font-black text-slate-500 hover:bg-slate-50 transition"
              >
                {state.cancelText ?? '取消'}
              </button>
              <button
                autoFocus
                onClick={() => close(true)}
                className={`flex-[2] py-2.5 rounded-xl text-sm font-black text-white shadow-md transition ${
                  state.danger ? 'bg-rose-600 hover:bg-rose-500' : 'bg-indigo-600 hover:bg-indigo-500'
                }`}
              >
                {state.confirmText ?? '确认'}
              </button>
            </div>
          </div>
        </div>,
        document.body,
      )}
    </ConfirmContext.Provider>
  )
}
