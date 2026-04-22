import { useEffect, useId, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

interface ModalProps {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  maxWidth?: string
}

export default function Modal({ open, onClose, title, children, maxWidth = 'max-w-md' }: ModalProps) {
  const titleId = useId()

  useEffect(() => {
    if (!open) return
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div
      className="fixed inset-0 z-[130] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4 animate-in fade-in duration-200"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby={title ? titleId : undefined}
    >
      <div
        className={`w-full ${maxWidth} bg-white rounded-[2rem] p-8 shadow-2xl space-y-5 animate-in zoom-in-95 duration-200`}
        onClick={e => e.stopPropagation()}
      >
        {title && <h3 id={titleId} className="font-black text-xl text-slate-800">{title}</h3>}
        {children}
      </div>
    </div>,
    document.body,
  )
}
