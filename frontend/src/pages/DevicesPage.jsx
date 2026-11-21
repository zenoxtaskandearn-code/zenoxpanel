import React, { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Plus, Search, Wifi, WifiOff, ShieldOff, ShieldCheck, Eye } from 'lucide-react'
import api from '../api/client'
import { useToast } from '../context/ToastContext'

export default function DevicesPage() {
  const qc = useQueryClient()
  const toast = useToast()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [revoked, setRevoked] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({ deviceUuid: '', name: '', deviceType: '' })

  const params = useMemo(() => {
    const p = {}
    if (search) p.search = search
    if (status) p.status = status
    if (revoked !== '') p.revoked = revoked === 'true'
    return p
  }, [search, status, revoked])

  const { data, isLoading, error } = useQuery({
    queryKey: ['devices', params],
    queryFn: async () => (await api.get('/api/admin/devices', { params })).data,
  })

  const createMut = useMutation({
    mutationFn: async (payload) => (await api.post('/api/admin/devices', payload)).data,
    onSuccess: () => { toast.success('Device created'); setShowCreate(false); setForm({ deviceUuid: '', name: '', deviceType: '' }); qc.invalidateQueries({ queryKey: ['devices'] }) },
    onError: (e) => toast.error(e?.response?.data?.message || 'Failed to create device'),
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Devices</h1>
          <p className="text-sm text-slate-500 mt-1">Manage registered devices, permissions, consent, and access status.</p>
        </div>
        <button onClick={() => setShowCreate(v=>!v)} className="inline-flex items-center justify-center gap-2 rounded-lg bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 px-4 py-2 text-sm font-medium">
          <Plus size={16} /> {showCreate ? 'Close' : 'Add device'}
        </button>
      </div>

      {showCreate && (
        <form onSubmit={(e)=>{e.preventDefault(); createMut.mutate(form)}} className="bg-white dark:bg-slate-900 border rounded-xl p-5 shadow-soft grid grid-cols-1 md:grid-cols-4 gap-3">
          <input required value={form.deviceUuid} onChange={e=>setForm({...form, deviceUuid:e.target.value})} placeholder="Device UUID" className="rounded-lg border bg-transparent px-3 py-2 text-sm" />
          <input required value={form.name} onChange={e=>setForm({...form, name:e.target.value})} placeholder="Device name" className="rounded-lg border bg-transparent px-3 py-2 text-sm" />
          <input value={form.deviceType} onChange={e=>setForm({...form, deviceType:e.target.value})} placeholder="Type (e.g. Android)" className="rounded-lg border bg-transparent px-3 py-2 text-sm" />
          <button disabled={createMut.isPending} className="rounded-lg bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 px-4 py-2 text-sm font-medium disabled:opacity-50">{createMut.isPending?'Creating...':'Create'}</button>
          <p className="md:col-span-4 text-xs text-slate-500">This creates a device record owned by you. The Android device must authenticate/register with matching credentials to connect and sync metadata.</p>
        </form>
      )}

      <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-4 flex flex-col lg:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search by name or UUID" className="w-full rounded-lg border bg-transparent pl-9 pr-3 py-2 text-sm" />
        </div>
        <select value={status} onChange={e=>setStatus(e.target.value)} className="rounded-lg border bg-transparent px-3 py-2 text-sm">
          <option value="">All statuses</option>
          <option value="ONLINE">Online</option>
          <option value="OFFLINE">Offline</option>
          <option value="REVOKED">Revoked</option>
        </select>
        <select value={revoked} onChange={e=>setRevoked(e.target.value)} className="rounded-lg border bg-transparent px-3 py-2 text-sm">
          <option value="">All access states</option>
          <option value="false">Active</option>
          <option value="true">Revoked</option>
        </select>
      </div>

      <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-3">{Array.from({length:5}).map((_,i)=><div key={i} className="h-12 bg-slate-100 dark:bg-slate-800 rounded animate-pulse" />)}</div>
        ) : error ? (
          <div className="p-6 text-sm text-rose-600">Failed to load devices.</div>
        ) : !data || data.length === 0 ? (
          <div className="p-10 text-center">
            <div className="text-sm font-medium">No devices found</div>
            <div className="text-sm text-slate-500 mt-1">Register a device record or adjust filters. Devices appear when added by admin or enrolled by the Android app.</div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500">
                <tr>
                  <th className="text-left font-medium px-4 py-3">Device</th>
                  <th className="text-left font-medium px-4 py-3">Status</th>
                  <th className="text-left font-medium px-4 py-3">Permissions</th>
                  <th className="text-left font-medium px-4 py-3">Consent</th>
                  <th className="text-left font-medium px-4 py-3">Last seen</th>
                  <th className="text-right font-medium px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {data.map(d => (
                  <tr key={d.id} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/30">
                    <td className="px-4 py-3">
                      <div className="font-medium">{d.name}</div>
                      <div className="text-xs text-slate-500">{d.deviceUuid}{d.deviceType ? ` · ${d.deviceType}` : ''}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="inline-flex items-center gap-2">
                        {d.status === 'ONLINE' ? <Wifi size={14} className="text-emerald-600" /> : <WifiOff size={14} className="text-slate-400" />}
                        <span className={`text-xs px-2 py-0.5 rounded-full border ${d.revoked ? 'border-rose-200 text-rose-700 dark:border-rose-900 dark:text-rose-300' : d.status==='ONLINE' ? 'border-emerald-200 text-emerald-700 dark:border-emerald-900 dark:text-emerald-300' : 'border-slate-200 text-slate-600 dark:text-slate-300'}`}>
                          {d.revoked ? 'REVOKED' : d.status}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600 dark:text-slate-300">
                      <div className="flex flex-wrap gap-1">
                        {d.permissions?.canView && <span className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800">View</span>}
                        {d.permissions?.canStream && <span className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800">Stream</span>}
                        {d.permissions?.canEdit && <span className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800">Edit</span>}
                        {d.permissions?.canDelete && <span className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800">Delete</span>}
                        {d.permissions?.canSyncMetadata && <span className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800">Sync</span>}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {d.latestConsent?.granted ? (
                        <span className="inline-flex items-center gap-1 text-xs text-emerald-700 dark:text-emerald-300"><ShieldCheck size={14}/> Granted</span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs text-slate-500"><ShieldOff size={14}/> Not granted</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-500 text-xs">{d.lastSeenAt ? new Date(d.lastSeenAt).toLocaleString() : 'Never'}</td>
                    <td className="px-4 py-3 text-right">
                      <Link to={`/devices/${d.id}`} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md border hover:bg-slate-50 dark:hover:bg-slate-800 text-xs font-medium">
                        <Eye size={14} /> Details
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
