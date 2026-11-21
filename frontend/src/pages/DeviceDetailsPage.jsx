import React, { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Shield, Ban, CheckCircle, Save } from 'lucide-react'
import api from '../api/client'
import { useToast } from '../context/ToastContext'

export default function DeviceDetailsPage() {
  const { id } = useParams()
  const qc = useQueryClient()
  const toast = useToast()
  const { data: device, isLoading, error } = useQuery({
    queryKey: ['device', id],
    queryFn: async () => (await api.get(`/api/admin/devices/${id}`)).data,
  })
  const { data: ops } = useQuery({
    queryKey: ['device-ops', id],
    queryFn: async () => (await api.get('/api/admin/operations', { params: { deviceId: id, size: 10 } })).data,
  })
  const [perms, setPerms] = useState(null)

  useEffect(() => { if (device?.permissions) setPerms({ ...device.permissions }) }, [device])

  const revokeMut = useMutation({
    mutationFn: async (revoked) => (await api.post(`/api/admin/devices/${id}/revoke`, null, { params: { revoked } })).data,
    onSuccess: () => { toast.success('Device access updated'); qc.invalidateQueries({ queryKey: ['device', id] }); qc.invalidateQueries({ queryKey: ['devices'] }) },
    onError: (e) => toast.error(e?.response?.data?.message || 'Failed to update device'),
  })
  const permMut = useMutation({
    mutationFn: async (payload) => (await api.put(`/api/admin/devices/${id}/permissions`, payload)).data,
    onSuccess: () => { toast.success('Permissions updated'); qc.invalidateQueries({ queryKey: ['device', id] }) },
    onError: (e) => toast.error(e?.response?.data?.message || 'Failed to update permissions'),
  })
  const consentMut = useMutation({
    mutationFn: async (granted) => (await api.post(`/api/admin/devices/${id}/consent`, { granted, scope: 'view,stream,operations' })).data,
    onSuccess: () => { toast.success('Consent updated'); qc.invalidateQueries({ queryKey: ['device', id] }) },
    onError: (e) => toast.error(e?.response?.data?.message || 'Failed to update consent'),
  })

  if (isLoading) return <div className="text-sm text-slate-500">Loading device...</div>
  if (error || !device) return <div className="text-sm text-rose-600">Device not found or access denied.</div>

  const opItems = ops?.content || []

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/devices" className="p-2 rounded-md border hover:bg-slate-50 dark:hover:bg-slate-800"><ArrowLeft size={16} /></Link>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{device.name}</h1>
          <p className="text-sm text-slate-500">{device.deviceUuid} · {device.deviceType || 'Unknown type'}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5">
            <h2 className="font-medium mb-4">Device status</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
              <div><div className="text-slate-500">Status</div><div className="font-medium mt-0.5">{device.status}</div></div>
              <div><div className="text-slate-500">Revoked</div><div className="font-medium mt-0.5">{device.revoked ? 'Yes' : 'No'}</div></div>
              <div><div className="text-slate-500">Last seen</div><div className="font-medium mt-0.5">{device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : 'Never'}</div></div>
              <div><div className="text-slate-500">IP</div><div className="font-medium mt-0.5">{device.lastKnownIp || '—'}</div></div>
              <div><div className="text-slate-500">App version</div><div className="font-medium mt-0.5">{device.appVersion || '—'}</div></div>
              <div><div className="text-slate-500">Created</div><div className="font-medium mt-0.5">{device.createdAt ? new Date(device.createdAt).toLocaleString() : '—'}</div></div>
            </div>
            <div className="mt-5 flex flex-wrap gap-2">
              {device.revoked ? (
                <button onClick={()=>revokeMut.mutate(false)} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border text-sm hover:bg-slate-50 dark:hover:bg-slate-800"><CheckCircle size={16}/> Restore access</button>
              ) : (
                <button onClick={()=>revokeMut.mutate(true)} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-rose-200 text-rose-700 dark:border-rose-900 dark:text-rose-300 text-sm hover:bg-rose-50 dark:hover:bg-rose-950/20"><Ban size={16}/> Revoke device</button>
              )}
              <Link to={`/gallery?deviceId=${device.id}`} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border text-sm hover:bg-slate-50 dark:hover:bg-slate-800">Open gallery</Link>
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5">
            <h2 className="font-medium mb-4">Permissions</h2>
            {perms && (
              <div className="space-y-3">
                {[
                  ['canView','View metadata & previews'],
                  ['canStream','Stream media (on-demand contract)'],
                  ['canEdit','Request edit operations'],
                  ['canDelete','Request delete operations'],
                  ['canSyncMetadata','Sync metadata from device'],
                ].map(([key,label]) => (
                  <label key={key} className="flex items-center justify-between gap-4 py-2 border-b last:border-0">
                    <span className="text-sm">{label}</span>
                    <input type="checkbox" checked={!!perms[key]} onChange={e=>setPerms({...perms,[key]:e.target.checked})} className="h-4 w-4" />
                  </label>
                ))}
                <button onClick={()=>permMut.mutate(perms)} disabled={permMut.isPending} className="inline-flex items-center gap-2 mt-2 px-4 py-2 rounded-lg bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 text-sm font-medium disabled:opacity-50"><Save size={16}/> Save permissions</button>
              </div>
            )}
          </div>

          <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft overflow-hidden">
            <div className="px-5 py-4 border-b"><h2 className="font-medium">Recent remote operations</h2></div>
            {opItems.length === 0 ? (
              <div className="p-6 text-sm text-slate-500">No operations requested for this device yet.</div>
            ) : (
              <div className="divide-y">
                {opItems.map(op => (
                  <div key={op.id} className="px-5 py-3 flex items-center justify-between gap-4 text-sm">
                    <div>
                      <div className="font-medium">{op.operationType} <span className="text-slate-400">#{op.id}</span></div>
                      <div className="text-xs text-slate-500">Status: {op.status}{op.failureReason ? ` · ${op.failureReason}` : ''}</div>
                    </div>
                    <div className="text-xs text-slate-500">{op.requestedAt ? new Date(op.requestedAt).toLocaleString() : ''}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-5">
            <div className="flex items-center gap-2 mb-4"><Shield size={16} /><h2 className="font-medium">Remote access consent</h2></div>
            <p className="text-sm text-slate-500 mb-4">Remote access requires explicit owner/admin consent. Operations and media access are denied without active consent.</p>
            <div className="text-sm mb-4">
              <div className="text-slate-500">Current consent</div>
              <div className="font-medium mt-1">{device.latestConsent?.granted ? 'Granted' : 'Not granted'}</div>
              {device.latestConsent?.expiresAt && <div className="text-xs text-slate-500 mt-1">Expires: {new Date(device.latestConsent.expiresAt).toLocaleString()}</div>}
            </div>
            <div className="flex gap-2">
              <button onClick={()=>consentMut.mutate(true)} className="flex-1 px-3 py-2 rounded-lg bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 text-sm font-medium">Grant</button>
              <button onClick={()=>consentMut.mutate(false)} className="flex-1 px-3 py-2 rounded-lg border text-sm">Revoke</button>
            </div>
            <p className="text-xs text-slate-500 mt-3">In a production device-owner flow, consent should be confirmed on the Android device. This console records administrative consent state and enforces it server-side.</p>
          </div>
        </div>
      </div>
    </div>
  )
}
