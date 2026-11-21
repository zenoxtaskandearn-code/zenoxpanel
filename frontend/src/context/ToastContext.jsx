import React, { createContext, useCallback, useContext, useMemo, useState } from 'react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const remove = useCallback((id) => setToasts(t => t.filter(x => x.id !== id)), [])
  const push = useCallback((toast) => {
    const id = Date.now() + Math.random()
    const item = { id, type: 'info', ...toast }
    setToasts(t => [...t, item])
    setTimeout(() => remove(id), toast.duration || 3500)
  }, [remove])

  const api = useMemo(() => ({
    push,
    success: (message, opts = {}) => push({ type: 'success', message, ...opts }),
    error: (message, opts = {}) => push({ type: 'error', message, ...opts }),
    info: (message, opts = {}) => push({ type: 'info', message, ...opts }),
  }), [push])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="fixed top-4 right-4 z-50 space-y-2 w-[320px] max-w-[calc(100vw-2rem)]">
        {toasts.map(t => (
          <div key={t.id} className={`rounded-lg border px-4 py-3 shadow-soft backdrop-blur bg-white/95 dark:bg-slate-900/95 ${
            t.type === 'success' ? 'border-emerald-200 dark:border-emerald-900 text-emerald-800 dark:text-emerald-200' :
            t.type === 'error' ? 'border-rose-200 dark:border-rose-900 text-rose-800 dark:text-rose-200' :
            'border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200'
          }`}>
            <div className="text-sm font-medium">{t.message}</div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
