/** 将机构树拍平为下拉/多选选项（缩进展示） */
export function flattenOrgTree(
  nodes: unknown[],
  depth = 0,
): { id: number; label: string }[] {
  const out: { id: number; label: string }[] = []
  for (const n of nodes) {
    if (!n || typeof n !== 'object') continue
    const o = n as Record<string, unknown>
    const id = Number(o.id)
    if (!Number.isFinite(id)) continue
    const name = String(o.orgName ?? o.name ?? id)
    const pad = '\u3000'.repeat(depth)
    out.push({ id, label: pad + name })
    const ch = o.children
    if (Array.isArray(ch) && ch.length) {
      out.push(...flattenOrgTree(ch as unknown[], depth + 1))
    }
  }
  return out
}
