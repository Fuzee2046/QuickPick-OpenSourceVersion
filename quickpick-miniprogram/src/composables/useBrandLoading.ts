import { onUnmounted, ref } from 'vue'

type BrandLoadingOptions = {
  title?: string
  description?: string
  delay?: number
}

export function useBrandLoading(defaultOptions?: BrandLoadingOptions) {
  const visible = ref(false)
  const title = ref(defaultOptions?.title || '正在为你准备中')
  const description = ref(defaultOptions?.description || '网络稍慢时，请稍候片刻')

  let timer: ReturnType<typeof setTimeout> | null = null
  let loadingCount = 0

  const clearTimer = () => {
    if (!timer) return
    clearTimeout(timer)
    timer = null
  }

  const show = (options?: BrandLoadingOptions) => {
    loadingCount += 1
    clearTimer()

    title.value = options?.title || defaultOptions?.title || '正在为你准备中'
    description.value = options?.description || defaultOptions?.description || '网络稍慢时，请稍候片刻'

    const delay = options?.delay ?? defaultOptions?.delay ?? 260
    timer = setTimeout(() => {
      visible.value = true
      timer = null
    }, delay)
  }

  const hide = (force = false) => {
    if (force) {
      loadingCount = 0
    } else if (loadingCount > 0) {
      loadingCount -= 1
    }

    if (loadingCount > 0) {
      return
    }

    clearTimer()
    visible.value = false
  }

  onUnmounted(() => {
    clearTimer()
    loadingCount = 0
  })

  return {
    visible,
    title,
    description,
    show,
    hide
  }
}
