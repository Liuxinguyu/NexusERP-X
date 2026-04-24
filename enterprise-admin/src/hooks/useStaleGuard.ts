import { useCallback, useRef } from 'react'

/**
 * 防止分页/快速切换条件下「后发先至」：仅最后一次发起的请求会更新 state。
 * 可配合 `useCallback` 中的 load 使用；下轮可逐页替换现有 `loadData` 模式。
 */
export function useStaleGuard() {
  const genRef = useRef(0)

  const isCurrent = useCallback((id: number) => id === genRef.current, [])

  const nextId = useCallback((): number => {
    const id = ++genRef.current
    return id
  }, [])

  return { nextId, isCurrent, currentGen: () => genRef.current }
}
