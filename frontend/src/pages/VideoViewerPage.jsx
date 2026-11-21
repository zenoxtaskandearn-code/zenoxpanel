import React from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { ArrowLeft, Film, Trash2, Wand2, RefreshCw } from 'lucide-react'
import api from '../api/client'
import { useToast } from '../context/ToastContext'

export default function VideoViewerPage() {
  const { deviceId, mediaId } = useParams()
  const toast = useToast()
  const { data: media, isLoading, error } = useQuery({
    queryKey: ['media', deviceId, mediaId],
    queryFn: async () => (await api.get(`/api/admin/devices/${deviceId}/media/${mediaId}`)).data,
  })
  const { data: stream } = useQuery({
    queryKey: ['media-stream', deviceId, mediaId],
    queryFn: async () => (await api.get(`/api/admin/devices/${deviceId}/media/${mediaId}/stream`)).data,
  })
  const opMut = useMutation({
    mutationFn: async (operationType) => (await api.post('/api/admin/operations', { deviceId: Number(deviceId), mediaId: Number(mediaId), operationType })).data,
    onSuccess: (d) => toast.success(`Operation requested (#${d.id}, ${d.status})`),
    onError: (e) => toast.error(e?.response?.data?.message || 'Failed to request operation'),
  })

  if (isLoading) return <div className="text-sm text-slate-500">Loading video...</div>
  if (error || !media) return <div className="text-sm text-rose-600">Unable to load video. {error?.response?.data?.message || ''}</div>

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div className="flex items-center gap-3">
          <Link to={`/gallery?deviceId=${deviceId}`} className="p-2 rounded-md border hover:bg-slate-50 dark:hover:bg-slate-800"><ArrowLeft size={16} /></Link>
          <div>
            <h1 className="text-xl font-semibold">Video Viewer</h1>
            <p className="text-sm text-slate-500">{media.fileName || media.externalId}</p>
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={()=>opMut.mutate('PREVIEW')} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border text-sm"><RefreshCw size={15}/> Request preview</button>
          <button onClick={()=>opMut.mutate('EDIT')} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border text-sm"><Wand2 size={15}/> Request edit</button>
          <button onClick={()=>opMut.mutate('DELETE')} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-rose-200 text-rose-700 dark:border-rose-900 dark:text-rose-300 text-sm"><Trash2 size={15}/> Request delete</button>
        </div>
      </div>
      <div className="bg-black rounded-xl border aspect-video flex items-center justify-center p-6">
        <div className="text-center max-w-xl">
          <div className="mx-auto h-14 w-14 rounded-xl bg-white/10 flex items-center justify-center text-white mb-4"><Film /></div>
          <div className="text-white font-medium">Video player (on-demand stream contract)</div>
          <div className="text-sm text-slate-300 mt-2">{stream?.message || 'Stream not available.'}</div>
          <div className="mt-4 text-xs text-slate-400">When the Android device provides an authorized stream reference, this contract endpoint can be used by the integration to deliver playback. No placeholder video is embedded.</div>
        </div>
      </div>
      <div className="bg-white dark:bg-slate-900 border rounded-xl p-5 text-sm">
        <h2 className="font-medium mb-3">Video metadata</h2>
        <dl className="grid grid-cols-2 md:grid-cols-4 gap-x-4 gap-y-2">
          <dt className="text-slate-500">Duration</dt><dd>{media.durationMs != null ? `${(media.durationMs/1000).toFixed(1)} s` : '—'}</dd>
          <dt className="text-slate-500">Dimensions</dt><dd>{media.width && media.height ? `${media.width}×${media.height}` : '—'}</dd>
          <dt className="text-slate-500">MIME</dt><dd>{media.mimeType || '—'}</dd>
          <dt className="text-slate-500">Stream contract</dt><dd>{stream?.available ? 'Available' : 'Unavailable'}</dd>
        </dl>
      </div>
    </div>
  )
}
