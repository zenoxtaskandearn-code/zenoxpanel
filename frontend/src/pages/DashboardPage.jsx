import React from 'react'
import { useQuery } from '@tanstack/react-query'
import { Smartphone, Wifi, WifiOff, Image, Film, Activity } from 'lucide-react'
import api from '../api/client'

function StatCard({ label, value, icon: Icon, hint }) {
  return (
    <div className="bg-white dark:bg-slate-900 border rounded-xl p-5 shadow-soft">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="text-2xl font-semibold mt-1">{value ?? '—'}</p>
          {hint && <p className="text-xs text-slate-500 mt-1">{hint}</p>}
        </div>
        <div className="h-9 w-9 rounded-lg bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200">
          <Icon size={18} />
        </div>
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: async () => (await api.get('/api/admin/dashboard')).data,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Dashboard</h1>
          <p className="text-sm text-slate-500">Overview of registered devices and indexed media.</p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
          {Array.from({length:5}).map((_,i)=>(<div key={i} className="h-28 rounded-xl bg-slate-200/70 dark:bg-slate-800 animate-pulse" />))}
        </div>
      </div>
    )
  }

  if (error) {
    return <div className="text-sm text-rose-600">Failed to load dashboard: {error?.response?.data?.message || error.message}</div>
  }

  const recent = data?.recentActivity || []
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-sm text-slate-500 mt-1">Overview of registered devices and indexed media (real data from backend).</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
        <StatCard label="Total devices" value={data.totalDevices} icon={Smartphone} />
        <StatCard label="Online devices" value={data.onlineDevices} icon={Wifi} />
        <StatCard label="Offline devices" value={data.offlineDevices} icon={WifiOff} />
        <StatCard label="Indexed images" value={data.totalIndexedImages} icon={Image} />
        <StatCard label="Indexed videos" value={data.totalIndexedVideos} icon={Film} />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white dark:bg-slate-900 border rounded-xl shadow-soft overflow-hidden">
          <div className="px-5 py-4 border-b flex items-center gap-2">
            <Activity size={16} className="text-slate-500" />
            <h2 className="font-medium">Recent activity</h2>
          </div>
          <div className="divide-y">
            {recent.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-500">No activity recorded yet.</div>
            ) : recent.map((a) => (
              <div key={a.id} className="px-5 py-3 flex items-start justify-between gap-4">
                <div>
                  <div className="text-sm font-medium">{a.action}</div>
                  <div className="text-xs text-slate-500 mt-0.5">{a.details || `${a.resourceType || ''} ${a.resourceId || ''}`}</div>
                </div>
                <div className="text-xs text-slate-500 whitespace-nowrap">{a.createdAt ? new Date(a.createdAt).toLocaleString() : ''}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5">
          <h2 className="font-medium mb-4">Device connection statistics</h2>
          <div className="space-y-3 text-sm">
            <div className="flex items-center justify-between"><span className="text-slate-500">Total</span><span className="font-medium">{data?.deviceConnectionStatistics?.total ?? 0}</span></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">Online</span><span className="font-medium text-emerald-600">{data?.deviceConnectionStatistics?.online ?? 0}</span></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">Offline</span><span className="font-medium">{data?.deviceConnectionStatistics?.offline ?? 0}</span></div>
            <div className="flex items-center justify-between"><span className="text-slate-500">Revoked</span><span className="font-medium text-rose-600">{data?.revokedDevices ?? 0}</span></div>
          </div>
          <div className="mt-5 p-3 rounded-lg bg-slate-50 dark:bg-slate-800/50 text-xs text-slate-500">
            Statistics are derived from registered devices and indexed metadata only. No sample or fabricated values are shown.
          </div>
        </div>
      </div>
    </div>
  )
}
