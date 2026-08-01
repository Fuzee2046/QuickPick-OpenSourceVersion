import { useUserStore } from '@/stores/user'
import { safeNavigateTo } from '@/utils/navigation'

export const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
const LOGIN_EXPIRED_MESSAGE = '未登录或登录已过期'
let isRedirectingToLogin = false

const httpInterceptor = {
  invoke(options: UniApp.RequestOptions) {
    if (!options.url.startsWith('http')) {
      options.url = baseURL + options.url
    }
    options.timeout = 20000
    options.header = {
      ...options.header,
    }

    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      options.header['Authorization'] = `Bearer ${token}`
    }
  },
}

uni.addInterceptor('request', httpInterceptor)
uni.addInterceptor('uploadFile', httpInterceptor)

type Data<T> = {
  code: number
  msg: string
  data: T
}

type HttpRequestOptions = UniApp.RequestOptions & {
  hideErrorToast?: boolean
  cacheTtlMs?: number
  cacheKey?: string
}

const HTTP_CACHE_PREFIX = 'http_cache:'

type HttpCachePayload<T> = {
  expireAt: number
  data: Data<T>
}

const buildCacheKey = (options: UniApp.RequestOptions, customKey?: string) => {
  if (customKey) {
    return `${HTTP_CACHE_PREFIX}${customKey}`
  }

  const requestUrl = typeof options.url === 'string' ? options.url : ''
  const dataText = options.data ? JSON.stringify(options.data) : ''
  return `${HTTP_CACHE_PREFIX}${requestUrl}::${dataText}`
}

const getCachedResponse = <T>(cacheKey: string): Data<T> | null => {
  const cache = uni.getStorageSync(cacheKey) as HttpCachePayload<T> | undefined
  if (!cache || typeof cache !== 'object') return null
  if (Number(cache.expireAt || 0) <= Date.now()) {
    uni.removeStorageSync(cacheKey)
    return null
  }
  return cache.data || null
}

const setCachedResponse = <T>(cacheKey: string, data: Data<T>, cacheTtlMs: number) => {
  uni.setStorageSync(cacheKey, {
    expireAt: Date.now() + cacheTtlMs,
    data,
  } as HttpCachePayload<T>)
}

const handleLoginExpired = () => {
  const userStore = useUserStore()
  userStore.clearUserInfo()

  if (isRedirectingToLogin) {
    return
  }

  isRedirectingToLogin = true
  uni.showToast({
    icon: 'none',
    title: '登录已过期，请重新登录',
    duration: 2000,
  })

  setTimeout(() => {
    const currentPages = getCurrentPages()
    const currentRoute = currentPages[currentPages.length - 1]?.route

    if (currentRoute !== 'pages/login/login') {
      safeNavigateTo({ url: '/pages/login/login' })
    }

    isRedirectingToLogin = false
  }, 1000)
}

export const http = <T>(options: HttpRequestOptions) => {
  const { hideErrorToast = false, cacheTtlMs = 0, cacheKey, ...requestOptions } = options
  const method = String(requestOptions.method || 'GET').toUpperCase()
  const shouldUseCache = method === 'GET' && cacheTtlMs > 0
  const resolvedCacheKey = shouldUseCache ? buildCacheKey(requestOptions, cacheKey) : ''

  if (shouldUseCache) {
    const cachedData = getCachedResponse<T>(resolvedCacheKey)
    if (cachedData) {
      return Promise.resolve(cachedData)
    }
  }

  return new Promise<Data<T>>((resolve, reject) => {
    uni.request({
      ...requestOptions,
      success(res) {
        const resData = res.data as Data<T>
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (resData.code === 200) {
            if (shouldUseCache) {
              setCachedResponse(resolvedCacheKey, resData, cacheTtlMs)
            }
            resolve(resData)
          } else if (resData.code === 401 || resData.msg === LOGIN_EXPIRED_MESSAGE) {
            handleLoginExpired()
            reject(resData)
          } else {
            if (!hideErrorToast) {
              uni.showToast({
                icon: 'none',
                title: resData.msg || '请求错误',
              })
            }
            reject(resData)
          }
        } else {
          if (res.statusCode === 401 || res.statusCode === 403) {
            handleLoginExpired()
          } else if (!hideErrorToast) {
            uni.showToast({
              icon: 'none',
              title: '服务器异常',
            })
          }
          reject(res)
        }
      },
      fail(err) {
        if (err.errMsg && err.errMsg.includes('timeout')) {
          if (!hideErrorToast) {
            uni.showToast({
              icon: 'none',
              title: '请求超时，请检查网络',
            })
          }
        } else {
          if (!hideErrorToast) {
            uni.showToast({
              icon: 'none',
              title: '网络错误',
            })
          }
        }
        reject(err)
      },
    })
  })
}
