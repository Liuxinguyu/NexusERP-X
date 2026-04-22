import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

type ToastType = 'success' | 'error' | 'info'
interface ToastItem { id: number; type: ToastType; message: string }

interface ToastApi {
  success: (msg: string) => void
  error: (msg: string) => void
  info: (msg: string) => void
}

const ToastContext = createContext<ToastApi | null>(null)

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}

const TYPE_STYLES: Record<ToastType, string> = {
  success: 'bg-emerald-600 text-white',
  error: 'bg-rose-600 text-white',
  info: 'bg-slate-800 text-white',
}

const MAX_TOASTS = 5

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const idRef = useRef(0)

  const remove = useCallback((id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }, [])

  const add = useCallback((type: ToastType, message: string) => {
    const id = ++idRef.current
    setToasts(prev => [...prev.slice(-(MAX_TOASTS - 1)), { id, type, message }])
    setTimeout(() => remove(id), 3000)
  }, [remove])

  const api = useRef<ToastApi>({
    success: (msg) => add('success', msg),
    error: (msg) => add('error', msg),
    info: (msg) => add('info', msg),
  })
  api.current.success = (msg) => add('success', msg)
  api.current.error = (msg) => add('error', msg)
  api.current.info = (msg) => add('info', msg)

  return (
    <ToastContext.Provider value={api.current}>
      {children}
      {createPortal(
        <div className="fixed top-4 right-4 z-[200] flex flex-col gap-2 pointer-events-none">
          {toasts.map(t => (
            <div
              key={t.id}
              className={`pointer-events-auto px-5 py-3 rounded-xl text-sm font-bold shadow-lg animate-in slide-in-from-right fade-in duration-200 flex items-center gap-3 max-w-sm ${TYPE_STYLES[t.type]}`}
            >
              <span className="flex-1 break-words">{t.message}</span>
              <button onClick={() => remove(t.id)} className="opacity-60 hover:opacity-100 text-xs shrink-0">✕</button>
            </div>
          ))}
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  )
}
