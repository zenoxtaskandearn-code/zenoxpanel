import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Activity } from 'lucide-react'
import api from '../api/client'

export default function ActivityLogsPage() {
  const [page, setPage] = useState(0)
  const [deviceId, setDeviceId] = useState('')
  const { data, isLoading, error } = useQuery({
    queryKey: ['audit', page, deviceId],
    queryFn: async () => (await api.get('/api/admin/audit-logs', { params: { page, size: 20, deviceId: deviceId || undefined } })).data,
  })
  const items = data?.content || []

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Activity Logs</h1>
          <p className="text-sm text-slate-500 mt-1">Audit trail of admin, device, and system actions.</p>
        </div>
        <div className="flex gap-2">
          <input value={deviceId} onChange={e=>{setDeviceId(e.target.value.replace(/[^0-9]/g,'')); setPage(0)}} placeholder="Filter by device ID" className="rounded-lg border bg-transparent px-3 py-2 text-sm w-44" />
        </div>
      </div>
      <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft overflow-hidden">
        <div className="px-5 py-4 border-b flex items-center gap-2"><Activity size={16} className="text-slate-500"/><h2 className="font-medium">Audit events</h2></div>
        {isLoading ? (
          <div className="p-6 space-y-3">{Array.from({length:6}).map((_,i)=><div key={i} className="h-10 rounded bg-slate-100 dark:bg-slate-800 animate-pulse" />)}</div>
        ) : error ? (
          <div className="p-6 text-sm text-rose-600">Failed to load activity logs.</div>
        ) : items.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-500">No audit log entries found.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500">
                <tr>
                  <th className="text-left font-medium px-4 py-3">Time</th>
                  <th className="text-left font-medium px-4 py-3">Actor</th>
                  <th className="text-left font-medium px-4 py-3">Action</th>
                  <th className="text-left font-medium px-4 py-3">Resource</th>
                  <th className="text-left font-medium px-4 py-3">Details</th>
                  <th className="text-left font-medium px-4 py-3">Result</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {items.map(l => (
                  <tr key={l.id}>
                    <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">{l.createdAt ? new Date(l.createdAt).toLocaleString() : ''}</td>
                    <td className="px-4 py-3 text-xs">{l.actorType}{l.actorId ? `:${l.actorId}` : ''}</td>
                    <td className="px-4 py-3 font-medium">{l.action}</td>
                    <td className="px-4 py-3 text-xs text-slate-500">{[l.resourceType, l.resourceId].filter(Boolean).join(' / ') || '—'}{l.deviceId ? ` (device ${l.deviceId})` : ''}</td>
                    <td className="px-4 py-3 text-xs text-slate-600 dark:text-slate-300 max-w-md truncate" title={l.details || ''}>{l.details || '—'}</td>
                    <td className="px-4 py-3"><span className={`text-xs px-2 py-0.5 rounded-full border ${l.success ? 'border-emerald-200 text-emerald-700 dark:border-emerald-900 dark:text-emerald-300' : 'border-rose-200 text-rose-700 dark:border-rose-900 dark:text-rose-300'}`}>{l.success ? 'Success' : 'Failed'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="px-5 py-3 border-t flex items-center justify-between text-xs text-slate-500">
          <div>Page {(data?.number ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)} · {data?.totalElements ?? 0} events</div>
          <div className="flex gap-2">
            <button disabled={page<=0} onClick={()=>setPage(p=>p-1)} className="px-3 py-1.5 rounded-md border disabled:opacity-40">Previous</button>
            <button disabled={page >= ((data?.totalPages ?? 1) - 1)} onClick={()=>setPage(p=>p+1)} className="px-3 py-1.5 rounded-md border disabled:opacity-40">Next</button>
          </div>
        </div>
      </div>
    </div>
  )
}
