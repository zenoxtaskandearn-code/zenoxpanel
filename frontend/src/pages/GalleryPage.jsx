import React, { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { Search, Image as ImageIcon, Film, Calendar, ArrowUpDown } from 'lucide-react'
import api from '../api/client'

export default function GalleryPage() {
  const [sp, setSp] = useSearchParams()
  const [deviceId, setDeviceId] = useState(sp.get('deviceId') || '')
  const [mediaType, setMediaType] = useState(sp.get('mediaType') || '')
  const [q, setQ] = useState(sp.get('search') || '')
  const [from, setFrom] = useState(sp.get('from') || '')
  const [to, setTo] = useState(sp.get('to') || '')
  const [sort, setSort] = useState(sp.get('sort') || 'capturedAt,desc')
  const [page, setPage] = useState(Number(sp.get('page') || 0))

  const { data: devices } = useQuery({
    queryKey: ['devices'],
    queryFn: async () => (await api.get('/api/admin/devices')).data,
  })

  useEffect(() => {
    if (!deviceId && devices && devices.length > 0) setDeviceId(String(devices[0].id))
  }, [devices, deviceId])

  useEffect(() => {
    const next = {}
    if (deviceId) next.deviceId = deviceId
    if (mediaType) next.mediaType = mediaType
    if (q) next.search = q
    if (from) next.from = from
    if (to) next.to = to
    if (sort) next.sort = sort
    if (page) next.page = String(page)
    setSp(next, { replace: true })
  }, [deviceId, mediaType, q, from, to, sort, page])

  const params = useMemo(() => ({ mediaType: mediaType || undefined, search: q || undefined, from: from || undefined, to: to || undefined, sort, page, size: 24 }), [mediaType, q, from, to, sort, page])
  const { data, isLoading, error } = useQuery({
    queryKey: ['gallery', deviceId, params],
    enabled: !!deviceId,
    queryFn: async () => (await api.get(`/api/admin/devices/${deviceId}/media`, { params })).data,
  })

  const items = data?.items || []
  const images = items.filter(i => i.mediaType === 'IMAGE')
  const videos = items.filter(i => i.mediaType === 'VIDEO')

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Gallery</h1>
        <p className="text-sm text-slate-500 mt-1">Browse indexed media metadata from authorized devices. Actual media bytes/previews are provided by the Android device when available.</p>
      </div>

      <div className="bg-white dark:bg-slate-900 border rounded-xl shadow-soft p-4 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-6 gap-3">
        <select value={deviceId} onChange={e=>{setDeviceId(e.target.value); setPage(0)}} className="rounded-lg border bg-transparent px-3 py-2 text-sm xl:col-span-2">
          <option value="">Select device</option>
          {(devices||[]).map(d => <option key={d.id} value={d.id}>{d.name} ({d.status})</option>)}
        </select>
        <div className="relative xl:col-span-2">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input value={q} onChange={e=>{setQ(e.target.value); setPage(0)}} placeholder="Search by file name" className="w-full rounded-lg border bg-transparent pl-9 pr-3 py-2 text-sm" />
        </div>
        <select value={mediaType} onChange={e=>{setMediaType(e.target.value); setPage(0)}} className="rounded-lg border bg-transparent px-3 py-2 text-sm">
          <option value="">All media</option>
          <option value="IMAGE">Images</option>
          <option value="VIDEO">Videos</option>
        </select>
        <select value={sort} onChange={e=>setSort(e.target.value)} className="rounded-lg border bg-transparent px-3 py-2 text-sm">
          <option value="capturedAt,desc">Captured (newest)</option>
          <option value="capturedAt,asc">Captured (oldest)</option>
          <option value="name,asc">Name (A-Z)</option>
          <option value="name,desc">Name (Z-A)</option>
          <option value="indexedAt,desc">Indexed (newest)</option>
        </select>
        <div className="flex items-center gap-2 md:col-span-2 xl:col-span-3">
          <Calendar size={16} className="text-slate-400" />
          <input type="date" value={from} onChange={e=>{setFrom(e.target.value); setPage(0)}} className="flex-1 rounded-lg border bg-transparent px-3 py-2 text-sm" />
          <span className="text-slate-400 text-sm">to</span>
          <input type="date" value={to} onChange={e=>{setTo(e.target.value); setPage(0)}} className="flex-1 rounded-lg border bg-transparent px-3 py-2 text-sm" />
        </div>
        <div className="text-xs text-slate-500 flex items-center gap-2 xl:col-span-3"><ArrowUpDown size={14}/> Sort and filter apply to indexed metadata only.</div>
      </div>

      {!deviceId ? (
        <div className="bg-white dark:bg-slate-900 border rounded-xl p-10 text-center text-sm text-slate-500">Select a device to view its indexed gallery metadata.</div>
      ) : isLoading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4">{Array.from({length:12}).map((_,i)=><div key={i} className="aspect-square rounded-xl bg-slate-200/70 dark:bg-slate-800 animate-pulse" />)}</div>
      ) : error ? (
        <div className="bg-white dark:bg-slate-900 border rounded-xl p-6 text-sm text-rose-600">Failed to load gallery. {error?.response?.data?.message || ''} (Consent and permissions may be required.)</div>
      ) : items.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 border rounded-xl p-10 text-center">
          <div className="text-sm font-medium">No indexed media found</div>
          <div className="text-sm text-slate-500 mt-1 max-w-xl mx-auto">This is expected when the Android device has not synced metadata yet, or when filters/consent/permissions exclude results. No placeholder media is shown.</div>
        </div>
      ) : (
        <>
          {(mediaType === '' || mediaType === 'IMAGE') && images.length > 0 && (
            <section className="space-y-3">
              <h2 className="text-sm font-medium text-slate-700 dark:text-slate-300 flex items-center gap-2"><ImageIcon size={16}/> Images ({images.length} on page)</h2>
              <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4">
                {images.map(item => (
                  <Link key={item.id} to={`/gallery/image/${deviceId}/${item.id}`} className="group bg-white dark:bg-slate-900 border rounded-xl overflow-hidden shadow-soft hover:shadow-md transition">
                    <div className="aspect-square bg-slate-100 dark:bg-slate-800 flex items-center justify-center relative">
                      <ImageIcon className="text-slate-400" />
                      <div className="absolute top-2 right-2 text-[10px] px-1.5 py-0.5 rounded bg-white/90 dark:bg-slate-900/90 border">{item.thumbnailAvailable ? 'Thumbnail contract' : 'No thumbnail'}</div>
                    </div>
                    <div className="p-3">
                      <div className="text-xs font-medium truncate" title={item.fileName || item.externalId}>{item.fileName || item.externalId}</div>
                      <div className="text-[11px] text-slate-500 mt-1">{item.capturedAt ? new Date(item.capturedAt).toLocaleDateString() : 'Unknown date'}</div>
                    </div>
                  </Link>
                ))}
              </div>
            </section>
          )}
          {(mediaType === '' || mediaType === 'VIDEO') && videos.length > 0 && (
            <section className="space-y-3">
              <h2 className="text-sm font-medium text-slate-700 dark:text-slate-300 flex items-center gap-2"><Film size={16}/> Videos ({videos.length} on page)</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                {videos.map(item => (
                  <Link key={item.id} to={`/gallery/video/${deviceId}/${item.id}`} className="group bg-white dark:bg-slate-900 border rounded-xl overflow-hidden shadow-soft hover:shadow-md transition">
                    <div className="aspect-video bg-slate-100 dark:bg-slate-800 flex items-center justify-center relative">
                      <Film className="text-slate-400" />
                      <div className="absolute bottom-2 right-2 text-[10px] px-1.5 py-0.5 rounded bg-white/90 dark:bg-slate-900/90 border">{item.durationMs ? `${Math.round(item.durationMs/1000)}s` : 'Video'}</div>
                    </div>
                    <div className="p-3">
                      <div className="text-xs font-medium truncate" title={item.fileName || item.externalId}>{item.fileName || item.externalId}</div>
                      <div className="text-[11px] text-slate-500 mt-1">{item.streamAvailable ? 'Stream contract available' : 'Stream unavailable'} · {item.capturedAt ? new Date(item.capturedAt).toLocaleDateString() : 'Unknown date'}</div>
                    </div>
                  </Link>
                ))}
              </div>
            </section>
          )}
          <div className="flex items-center justify-between pt-2">
            <div className="text-xs text-slate-500">Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} items</div>
            <div className="flex gap-2">
              <button disabled={page <= 0} onClick={()=>setPage(p=>p-1)} className="px-3 py-1.5 rounded-md border text-sm disabled:opacity-40">Previous</button>
              <button disabled={page >= (data.totalPages - 1)} onClick={()=>setPage(p=>p+1)} className="px-3 py-1.5 rounded-md border text-sm disabled:opacity-40">Next</button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
