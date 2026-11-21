import React from 'react'
import { useTheme } from '../context/ThemeContext'
import { Moon, Sun, Globe, Server } from 'lucide-react'

export default function SettingsPage() {
  const { theme, toggle } = useTheme()
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="text-sm text-slate-500 mt-1">Application preferences and environment information.</p>
      </div>

      <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5 space-y-5">
        <h2 className="font-medium">Appearance</h2>
        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm font-medium">Theme</div>
            <div className="text-sm text-slate-500">Switch between light and dark mode.</div>
          </div>
          <button onClick={toggle} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg border text-sm">
            {theme === 'dark' ? <Sun size={16}/> : <Moon size={16}/>} {theme === 'dark' ? 'Light mode' : 'Dark mode'}
          </button>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5 space-y-4">
        <h2 className="font-medium">Environment</h2>
        <div className="flex items-start gap-3 text-sm">
          <Server size={16} className="mt-0.5 text-slate-500" />
          <div>
            <div className="text-slate-500">API base URL</div>
            <div className="font-mono text-xs mt-1 break-all">{apiBase}</div>
            <div className="text-xs text-slate-500 mt-1">Configured via <code>VITE_API_BASE_URL</code> at build time for the frontend.</div>
          </div>
        </div>
        <div className="flex items-start gap-3 text-sm">
          <Globe size={16} className="mt-0.5 text-slate-500" />
          <div>
            <div className="text-slate-500">WebSocket endpoint</div>
            <div className="font-mono text-xs mt-1 break-all">{apiBase.replace(/^http/, 'ws')}/ws</div>
            <div className="text-xs text-slate-500 mt-1">Used for real-time device and operation events (requires authenticated connection).</div>
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5">
        <h2 className="font-medium mb-2">Security & privacy notes</h2>
        <ul className="text-sm text-slate-600 dark:text-slate-300 space-y-2 list-disc pl-5">
          <li>Remote access to device media requires explicit consent and appropriate device permissions.</li>
          <li>Media preview/edit/delete requests are authorized and tracked by the backend, but actual execution is performed by the Android application (pending integration).</li>
          <li>No hidden access or permission bypass is implemented. Sensitive actions are audit logged.</li>
        </ul>
      </div>
    </div>
  )
}
