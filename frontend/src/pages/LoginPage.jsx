import React, { useState } from 'react'
import { Shield, Lock, Mail, User, KeyRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

export default function LoginPage() {
  const { login, register } = useAuth()
  const toast = useToast()
  const [mode, setMode] = useState('login')
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({ email: '', password: '', name: '', inviteCode: '' })

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      if (mode === 'login') {
        await login(form.email, form.password)
        toast.success('Signed in successfully')
      } else {
        await register({ email: form.email, password: form.password, name: form.name, inviteCode: form.inviteCode || undefined })
        toast.success('Admin account created')
      }
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Authentication failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto h-12 w-12 rounded-xl bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 flex items-center justify-center mb-4">
            <Shield />
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">Remote Gallery Admin</h1>
          <p className="text-sm text-slate-500 mt-1">Secure administration dashboard</p>
        </div>
        <div className="bg-white dark:bg-slate-900 border rounded-2xl shadow-soft p-6">
          <div className="flex gap-2 mb-6 p-1 bg-slate-100 dark:bg-slate-800 rounded-lg">
            <button onClick={() => setMode('login')} className={`flex-1 py-2 text-sm rounded-md transition ${mode==='login'?'bg-white dark:bg-slate-900 shadow-sm':'text-slate-500'}`}>Sign in</button>
            <button onClick={() => setMode('register')} className={`flex-1 py-2 text-sm rounded-md transition ${mode==='register'?'bg-white dark:bg-slate-900 shadow-sm':'text-slate-500'}`}>Register</button>
          </div>
          <form onSubmit={submit} className="space-y-4">
            {mode === 'register' && (
              <label className="block">
                <span className="text-sm font-medium text-slate-700 dark:text-slate-300">Full name</span>
                <div className="mt-1 relative">
                  <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input name="name" required value={form.name} onChange={onChange} className="w-full rounded-lg border bg-transparent pl-9 pr-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-slate-300 dark:focus:ring-slate-700" placeholder="Admin name" />
                </div>
              </label>
            )}
            <label className="block">
              <span className="text-sm font-medium text-slate-700 dark:text-slate-300">Email</span>
              <div className="mt-1 relative">
                <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input name="email" type="email" required value={form.email} onChange={onChange} className="w-full rounded-lg border bg-transparent pl-9 pr-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-slate-300 dark:focus:ring-slate-700" placeholder="admin@example.com" />
              </div>
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-700 dark:text-slate-300">Password</span>
              <div className="mt-1 relative">
                <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input name="password" type="password" required minLength={8} value={form.password} onChange={onChange} className="w-full rounded-lg border bg-transparent pl-9 pr-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-slate-300 dark:focus:ring-slate-700" placeholder="Minimum 8 characters" />
              </div>
            </label>
            {mode === 'register' && (
              <label className="block">
                <span className="text-sm font-medium text-slate-700 dark:text-slate-300">Invite code (if required)</span>
                <div className="mt-1 relative">
                  <KeyRound size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input name="inviteCode" value={form.inviteCode} onChange={onChange} className="w-full rounded-lg border bg-transparent pl-9 pr-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-slate-300 dark:focus:ring-slate-700" placeholder="Optional" />
                </div>
              </label>
            )}
            <button disabled={loading} type="submit" className="w-full rounded-lg bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 py-2.5 text-sm font-medium hover:opacity-90 disabled:opacity-50 transition">
              {loading ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Create admin account'}
            </button>
          </form>
          <p className="text-xs text-slate-500 mt-4">Registration may be restricted by server configuration. Use a valid invite code if required.</p>
        </div>
      </div>
    </div>
  )
}
