import React, { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import api from '../api/client'

export default function ProfilePage() {
  const { user, refreshProfile } = useAuth()
  const toast = useToast()
  const [name, setName] = useState(user?.name || '')
  const [saving, setSaving] = useState(false)
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' })
  const [pwSaving, setPwSaving] = useState(false)

  const saveProfile = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await api.put('/api/auth/profile', { name })
      await refreshProfile()
      toast.success('Profile updated')
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to update profile')
    } finally { setSaving(false) }
  }

  const changePassword = async (e) => {
    e.preventDefault()
    setPwSaving(true)
    try {
      await api.post('/api/auth/change-password', pw)
      setPw({ currentPassword: '', newPassword: '' })
      toast.success('Password changed')
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to change password')
    } finally { setPwSaving(false) }
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Profile</h1>
        <p className="text-sm text-slate-500 mt-1">Manage your administrator account details.</p>
      </div>

      <form onSubmit={saveProfile} className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5 space-y-4">
        <h2 className="font-medium">Account information</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label className="block">
            <span className="text-sm text-slate-600 dark:text-slate-300">Name</span>
            <input value={name} onChange={e=>setName(e.target.value)} required className="mt-1 w-full rounded-lg border bg-transparent px-3 py-2 text-sm" />
          </label>
          <label className="block">
            <span className="text-sm text-slate-600 dark:text-slate-300">Email</span>
            <input value={user?.email || ''} disabled className="mt-1 w-full rounded-lg border bg-slate-50 dark:bg-slate-800 px-3 py-2 text-sm text-slate-500" />
          </label>
          <label className="block">
            <span className="text-sm text-slate-600 dark:text-slate-300">Role</span>
            <input value={user?.role || 'ADMIN'} disabled className="mt-1 w-full rounded-lg border bg-slate-50 dark:bg-slate-800 px-3 py-2 text-sm text-slate-500" />
          </label>
        </div>
        <button disabled={saving} className="px-4 py-2 rounded-lg bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 text-sm font-medium disabled:opacity-50">{saving ? 'Saving...' : 'Save changes'}</button>
      </form>

      <form onSubmit={changePassword} className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5 space-y-4">
        <h2 className="font-medium">Change password</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label className="block">
            <span className="text-sm text-slate-600 dark:text-slate-300">Current password</span>
            <input type="password" required value={pw.currentPassword} onChange={e=>setPw({...pw,currentPassword:e.target.value})} className="mt-1 w-full rounded-lg border bg-transparent px-3 py-2 text-sm" />
          </label>
          <label className="block">
            <span className="text-sm text-slate-600 dark:text-slate-300">New password</span>
            <input type="password" required minLength={8} value={pw.newPassword} onChange={e=>setPw({...pw,newPassword:e.target.value})} className="mt-1 w-full rounded-lg border bg-transparent px-3 py-2 text-sm" />
          </label>
        </div>
        <button disabled={pwSaving} className="px-4 py-2 rounded-lg border text-sm font-medium disabled:opacity-50">{pwSaving ? 'Updating...' : 'Update password'}</button>
      </form>
    </div>
  )
}
