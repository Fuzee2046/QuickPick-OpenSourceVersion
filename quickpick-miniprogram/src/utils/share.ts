import { onShareAppMessage, onShareTimeline, onShow } from '@dcloudio/uni-app'

const DEFAULT_SHARE_PATH = '/pages/index/index'
const DEFAULT_SHARE_IMAGE = '/static/images/logo1.png'

export type PageShareOptions = {
  title: string
  path?: string
  query?: string
  imageUrl?: string
}

const normalizeQuery = (query?: string) => String(query || '').replace(/^\?/, '')

const appendQueryToPath = (path: string, query?: string) => {
  const normalizedQuery = normalizeQuery(query)
  if (!normalizedQuery) {
    return path
  }

  return `${path}${path.includes('?') ? '&' : '?'}${normalizedQuery}`
}

export const usePageShare = (
  optionsOrGetter: PageShareOptions | (() => PageShareOptions),
) => {
  const getOptions = () =>
    typeof optionsOrGetter === 'function' ? optionsOrGetter() : optionsOrGetter
  let hasShownShareMenu = false

  onShow(() => {
    if (hasShownShareMenu) {
      return
    }
    // #ifdef MP-WEIXIN
    uni.showShareMenu({
      menus: ['shareAppMessage', 'shareTimeline'],
    })
    // #endif
    hasShownShareMenu = true
  })

  onShareAppMessage(() => {
    const options = getOptions()
    return {
      title: options.title,
      path: appendQueryToPath(options.path || DEFAULT_SHARE_PATH, options.query),
      imageUrl: options.imageUrl || DEFAULT_SHARE_IMAGE,
    }
  })

  onShareTimeline(() => {
    const options = getOptions()
    return {
      title: options.title,
      query: normalizeQuery(options.query),
      imageUrl: options.imageUrl || DEFAULT_SHARE_IMAGE,
    }
  })
}
