import { useCallback, useEffect, useRef, useState } from 'react'
import { fileApi } from '../../api/oa-file'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/ConfirmDialog'
import Modal from '../../components/Modal'
import type { OaFileFolder, OaFileRow } from '../../types/oa-crud'

export default function CloudDisk() {
  const toast = useToast()
  const confirm = useConfirm()
  const [folders, setFolders] = useState<OaFileFolder[]>([])
  const [files, setFiles] = useState<OaFileRow[]>([])
  const [currentFolder, setCurrentFolder] = useState<number | undefined>()
  const [breadcrumb, setBreadcrumb] = useState<Array<{ id?: number; name: string }>>([{ name: '根目录' }])
  const [loading, setLoading] = useState(false)
  const [newFolderName, setNewFolderName] = useState('')
  const [folderModal, setFolderModal] = useState(false)
  const uploadRef = useRef<HTMLInputElement>(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [f, fi] = await Promise.all([
        fileApi.getFolders().catch(() => []),
        fileApi.listFiles({ folderId: currentFolder }).catch(() => []),
      ])
      setFolders(f); setFiles(fi)
    } finally { setLoading(false) }
  }, [currentFolder])

  useEffect(() => { void loadData() }, [loadData])

  const navigateToFolder = (folder: OaFileFolder) => {
    setCurrentFolder(folder.id)
    setBreadcrumb(prev => [...prev, { id: folder.id, name: folder.folderName ?? '未命名' }])
  }

  const navigateToBreadcrumb = (idx: number) => {
    const item = breadcrumb[idx]
    setCurrentFolder(item.id)
    setBreadcrumb(prev => prev.slice(0, idx + 1))
  }

  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return
    try {
      await fileApi.createFolder({ folderName: newFolderName.trim(), parentId: currentFolder })
      toast.success('文件夹已创建')
      setFolderModal(false); setNewFolderName(''); void loadData()
    } catch (e) { toast.error(e instanceof Error ? e.message : '创建失败') }
  }

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      await fileApi.upload(currentFolder ?? 0, file)
      toast.success('上传成功')
      void loadData()
    } catch (err) { toast.error(err instanceof Error ? err.message : '上传失败') }
    finally { if (uploadRef.current) uploadRef.current.value = '' }
  }

  const handleDeleteFile = async (id: number) => {
    const ok = await confirm({ title: '删除文件', message: '确认删除此文件？', danger: true })
    if (!ok) return
    try { await fileApi.remove(id); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '删除失败') }
  }

  const handleDeleteFolder = async (id: number) => {
    const ok = await confirm({ title: '删除文件夹', message: '确认删除此文件夹？', danger: true })
    if (!ok) return
    try { await fileApi.deleteFolder(id); void loadData() }
    catch (e) { toast.error(e instanceof Error ? e.message : '删除失败') }
  }

  const fileIcon = (name: string) => {
    if (name.endsWith('.pdf')) return '📕'
    if (name.endsWith('.xlsx') || name.endsWith('.xls')) return '📊'
    if (name.endsWith('.docx') || name.endsWith('.doc')) return '📝'
    if (name.endsWith('.zip') || name.endsWith('.rar')) return '📦'
    if (name.endsWith('.png') || name.endsWith('.jpg')) return '🖼️'
    return '📄'
  }

  const formatSize = (bytes?: number) => {
    if (!bytes) return '-'
    if (bytes < 1024) return bytes + 'B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
    return (bytes / 1024 / 1024).toFixed(1) + 'MB'
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex justify-between items-center">
        <div className="flex gap-2 items-center text-xs font-black">
          {breadcrumb.map((b, i) => (
            <span key={i} className="flex items-center gap-2">
              {i > 0 && <span className="text-slate-300">/</span>}
              <button onClick={() => navigateToBreadcrumb(i)} className="text-indigo-600 hover:underline">{b.name}</button>
            </span>
          ))}
        </div>
        <div className="flex gap-3">
          <button onClick={() => setFolderModal(true)} className="px-6 py-2 bg-white rounded-2xl ring-1 ring-slate-200 text-xs font-black text-slate-600 hover:bg-slate-50 transition-all">新建文件夹</button>
          <button onClick={() => uploadRef.current?.click()} className="px-6 py-2 bg-indigo-600 text-white rounded-2xl text-xs font-black shadow-lg shadow-indigo-200 hover:bg-indigo-500 transition-all">上传文件</button>
          <input ref={uploadRef} type="file" className="hidden" onChange={handleUpload} />
        </div>
      </div>

      {loading ? (
        <div className="text-center text-slate-300 font-black py-16">加载中...</div>
      ) : (
        <div className="grid grid-cols-5 gap-5">
          {folders.map(f => (
            <div key={f.id} onDoubleClick={() => navigateToFolder(f)}
              className="bg-white p-6 rounded-[2rem] shadow-sm ring-1 ring-slate-100 flex flex-col items-center group cursor-pointer hover:bg-indigo-50 hover:ring-indigo-200 transition-all duration-300">
              <div className="text-4xl mb-4 group-hover:scale-110 transition-transform">📁</div>
              <div className="text-xs font-black text-slate-900 truncate w-full text-center">{f.folderName}</div>
              <button onClick={e => { e.stopPropagation(); handleDeleteFolder(f.id!) }}
                className="mt-3 text-[10px] text-rose-400 font-black opacity-0 group-hover:opacity-100 transition-opacity hover:underline">删除</button>
            </div>
          ))}
          {files.map(f => (
            <div key={f.id}
              className="bg-white p-6 rounded-[2rem] shadow-sm ring-1 ring-slate-100 flex flex-col items-center group cursor-pointer hover:bg-slate-900 transition-all duration-500">
              <div className="text-4xl mb-4 group-hover:scale-110 transition-transform">{fileIcon(f.fileName ?? '')}</div>
              <div className="text-xs font-black text-slate-900 truncate w-full text-center group-hover:text-white">{f.fileName}</div>
              <div className="text-[9px] font-bold text-slate-300 mt-2 flex gap-3 uppercase group-hover:text-slate-500">
                <span>{formatSize(f.fileSize)}</span>
                {f.downloadCount != null && <span className="text-emerald-500">下载: {f.downloadCount}</span>}
              </div>
              <div className="flex gap-3 mt-3 opacity-0 group-hover:opacity-100 transition-opacity">
                <a href={`/api/v1/oa/files/${f.id}/download`} target="_blank" rel="noopener noreferrer"
                  className="text-[10px] font-black text-indigo-400 hover:underline">下载</a>
                <button onClick={() => handleDeleteFile(f.id!)} className="text-[10px] font-black text-rose-400 hover:underline">删除</button>
              </div>
            </div>
          ))}
          {folders.length === 0 && files.length === 0 && (
            <div className="col-span-5 text-center text-slate-300 font-black py-16">此目录为空</div>
          )}
        </div>
      )}

      <Modal open={folderModal} onClose={() => setFolderModal(false)} title="新建文件夹" maxWidth="sm">
        <div className="space-y-6">
          <input value={newFolderName} onChange={e => setNewFolderName(e.target.value)} placeholder="文件夹名称"
            className="w-full px-4 py-3 rounded-xl bg-slate-50 ring-1 ring-slate-200 text-sm font-bold focus:ring-indigo-500" />
          <div className="flex gap-4">
            <button onClick={() => setFolderModal(false)} className="flex-1 py-3 bg-white rounded-2xl ring-1 ring-slate-200 font-black text-xs text-slate-400">取消</button>
            <button onClick={handleCreateFolder} className="flex-[2] py-3 bg-indigo-600 text-white rounded-2xl font-black text-xs hover:bg-indigo-500 transition-all">创建</button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
