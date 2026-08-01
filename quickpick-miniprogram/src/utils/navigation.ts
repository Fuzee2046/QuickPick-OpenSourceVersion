const TAB_BAR_ROUTES = new Set([
  'pages/index/index',
  'pages/free-meal/free-meal',
  'pages/order-list/order-list',
  'pages/my/my',
])

const NAVIGATION_GUARD_MS = 500
let lastNavigationSignature = ''
let lastNavigationTime = 0
let hideNativeTabBarRetryTimer: ReturnType<typeof setTimeout> | null = null

const normalizePagePath = (path: string = '') => String(path).replace(/^\//, '').split('?')[0]

const toPageUrl = (path: string) => `/${normalizePagePath(path)}`

export const isTabBarPage = (path: string) => TAB_BAR_ROUTES.has(normalizePagePath(path))

const canStartNavigation = (type: string, target: string) => {
  const signature = `${type}:${target}`
  const now = Date.now()
  if (lastNavigationSignature === signature && now - lastNavigationTime < NAVIGATION_GUARD_MS) {
    return false
  }

  lastNavigationSignature = signature
  lastNavigationTime = now
  return true
}

const getCurrentRoute = () => {
  const pages = getCurrentPages()
  return normalizePagePath(pages[pages.length - 1]?.route || '')
}

export const syncNativeTabBarHidden = (retryCount: number = 0) => {
  // #ifdef MP-WEIXIN
  if (hideNativeTabBarRetryTimer) {
    clearTimeout(hideNativeTabBarRetryTimer)
    hideNativeTabBarRetryTimer = null
  }

  const currentRoute = getCurrentRoute()
  if (!currentRoute || !isTabBarPage(currentRoute)) {
    return
  }

  uni.hideTabBar({
    animation: false,
    fail: () => {}
  })

  // 页面切换到 tab 页时，系统 tabBar 可能会在下一帧重新出现，这里做一次短暂重试兜底
  if (retryCount < 5) {
    hideNativeTabBarRetryTimer = setTimeout(() => {
      syncNativeTabBarHidden(retryCount + 1)
    }, 60)
  }
  // #endif
}

export const safeNavigateTo = (options: UniNamespace.NavigateToOptions) => {
  const targetPath = normalizePagePath(options.url)
  if (!targetPath || getCurrentRoute() === targetPath) {
    return
  }
  if (!canStartNavigation('navigateTo', targetPath)) {
    return
  }
  uni.navigateTo(options)
}

export const safeSwitchTab = (options: UniNamespace.SwitchTabOptions) => {
  const targetPath = normalizePagePath(options.url)
  if (!targetPath || getCurrentRoute() === targetPath) {
    return
  }
  if (!canStartNavigation('switchTab', targetPath)) {
    return
  }
  uni.switchTab(options)
}

export const safeRedirectTo = (options: UniNamespace.RedirectToOptions) => {
  const targetPath = normalizePagePath(options.url)
  if (!targetPath) {
    return
  }
  if (!canStartNavigation('redirectTo', targetPath)) {
    return
  }
  uni.redirectTo(options)
}

export const safeReLaunch = (options: UniNamespace.ReLaunchOptions) => {
  const targetPath = normalizePagePath(options.url)
  if (!targetPath) {
    return
  }
  if (!canStartNavigation('reLaunch', targetPath)) {
    return
  }
  uni.reLaunch(options)
}

type SmartNavigateBackOptions = {
  fallbackTab?: string
}

export const smartNavigateBack = (options: SmartNavigateBackOptions = {}) => {
  const pages = getCurrentPages()

  if (pages.length > 1) {
    const previousRoute = normalizePagePath(pages[pages.length - 2]?.route || '')
    if (previousRoute && isTabBarPage(previousRoute)) {
      safeSwitchTab({ url: toPageUrl(previousRoute) })
      return
    }

    if (!canStartNavigation('navigateBack', previousRoute || 'default')) {
      return
    }
    uni.navigateBack()
    return
  }

  if (options.fallbackTab && isTabBarPage(options.fallbackTab)) {
    safeSwitchTab({ url: toPageUrl(options.fallbackTab) })
  }
}
