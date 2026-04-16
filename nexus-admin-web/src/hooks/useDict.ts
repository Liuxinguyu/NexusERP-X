import { onMounted, reactive, ref } from 'vue'
import { systemApi, type DictItemVO } from '@/api/system'

export type DictValue = string | number
export type DictOption = { label: string; value: DictValue }

const dictCache = new Map<string, DictOption[]>()
const pendingRequests = new Map<string, Promise<DictOption[]>>()

function normalizeDictItem(item: DictItemVO): DictOption {
  const label = String(
    item.itemLabel ??
      item.label ??
      item.dictLabel ??
      item.name ??
      item.itemValue ??
      item.value ??
      item.dictValue ??
      item.code ??
      ''
  )

  const rawValue = item.itemValue ?? item.value ?? item.dictValue ?? item.code ?? ''
  const value: DictValue = typeof rawValue === 'number' ? rawValue : String(rawValue)
  return { label, value }
}

async function fetchDictOptions(typeCode: string): Promise<DictOption[]> {
  if (dictCache.has(typeCode)) {
    return dictCache.get(typeCode) || []
  }
  if (pendingRequests.has(typeCode)) {
    return pendingRequests.get(typeCode)!
  }

  const req = systemApi
    .getDictItemsByTypeCode(typeCode)
    .then((list) => (list || []).map(normalizeDictItem))
    .then((options) => {
      dictCache.set(typeCode, options)
      pendingRequests.delete(typeCode)
      return options
    })
    .catch((error) => {
      pendingRequests.delete(typeCode)
      throw error
    })

  pendingRequests.set(typeCode, req)
  return req
}

export async function preloadDict(...typeCodes: string[]) {
  const uniqueCodes = Array.from(new Set(typeCodes.filter(Boolean)))
  await Promise.all(uniqueCodes.map((code) => fetchDictOptions(code)))
}

export function clearDictCache(typeCode?: string) {
  if (typeCode) {
    dictCache.delete(typeCode)
    pendingRequests.delete(typeCode)
    return
  }
  dictCache.clear()
  pendingRequests.clear()
}

export function useDict<T extends readonly string[]>(...typeCodes: T) {
  const dictMap = reactive<Record<string, DictOption[]>>({})
  const loading = ref(false)
  const error = ref<unknown>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      const uniqueCodes = Array.from(new Set(typeCodes.filter(Boolean))) as T[number][]
      const result = await Promise.all(uniqueCodes.map((code) => fetchDictOptions(code)))
      uniqueCodes.forEach((code, index) => {
        dictMap[String(code)] = result[index]
      })
    } catch (e) {
      error.value = e
      throw e
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    load()
  })

  return {
    dictMap: dictMap as Record<T[number], DictOption[]>,
    loading,
    error,
    refresh: load,
  }
}
