/**
 * 微信订阅消息授权工具
 * 用于在小程序中请求用户授权接收订阅消息
 */

// 取餐提醒模板ID，需要和后端保持一致
const PICKUP_REMINDER_TEMPLATE_ID = 'D06pjzeY0sEHt1E0uR0ECI6KvHoqXZ-JY1UKAE6SC3U'

// 超时二次提醒模板ID，需要和后端保持一致
const PICKUP_OVERTIME_TEMPLATE_ID = 'D06pjzeY0sEHt1E0uR0ECOIr5khGwkn6HAOY47sTgYQ'

// 抽奖中奖通知模板ID，需要和后端保持一致
const LUCKY_DRAW_WINNER_TEMPLATE_ID = 'vp723a9EocUwYIMpKXnO4tthQUEJsqWH8eCPQqCKrTY'

const TEMPLATE_ID_MAP = {
  pickup: PICKUP_REMINDER_TEMPLATE_ID,
  pickupOvertime: PICKUP_OVERTIME_TEMPLATE_ID,
  luckyDraw: LUCKY_DRAW_WINNER_TEMPLATE_ID
} as const

type SubscribeTemplateType = keyof typeof TEMPLATE_ID_MAP

// 本地存储键名
const SUBSCRIBE_TEMPLATE_STATUS_KEY = 'subscribe_template_status_map'
const SUBSCRIBE_TEMPLATE_LAST_REJECT_TIME_KEY = 'subscribe_template_last_reject_time_map'
const SUBSCRIBE_TEMPLATE_LAST_ACCEPT_TIME_KEY = 'subscribe_template_last_accept_time_map'

// 授权状态枚举
enum AuthStatus {
  PENDING = 'pending',      // 从未请求过或状态未知
  ACCEPTED = 'accepted',    // 用户已接受授权
  REJECTED = 'rejected'     // 用户已拒绝授权
}

// 拒绝后重新请求的最小间隔（毫秒）：7天
const REJECT_RETRY_INTERVAL = 7 * 24 * 60 * 60 * 1000
const IS_SUBSCRIBE_DEBUG = import.meta.env.DEV

const debugLog = (...args: any[]) => {
  if (IS_SUBSCRIBE_DEBUG) {
    console.log(...args)
  }
}

const debugWarn = (...args: any[]) => {
  if (IS_SUBSCRIBE_DEBUG) {
    console.warn(...args)
  }
}
const getStorageMap = <T extends string | number>(key: string): Record<string, T> => {
  return uni.getStorageSync(key) || {}
}

const setStorageMap = <T extends string | number>(key: string, value: Record<string, T>) => {
  uni.setStorageSync(key, value)
}

const normalizeTemplateIds = (templateTypes: Array<SubscribeTemplateType | string> = ['pickup']): string[] => {
  const normalizedIds = templateTypes.map((templateType) => {
    if (templateType in TEMPLATE_ID_MAP) {
      return TEMPLATE_ID_MAP[templateType as SubscribeTemplateType]
    }
    return templateType
  })
  return Array.from(new Set(normalizedIds))
}

const getTemplateStatus = (templateId: string): AuthStatus => {
  const statusMap = getStorageMap<string>(SUBSCRIBE_TEMPLATE_STATUS_KEY)
  return (statusMap[templateId] as AuthStatus) || AuthStatus.PENDING
}

const getTemplateAcceptTime = (templateId: string): number | null => {
  const acceptTimeMap = getStorageMap<number>(SUBSCRIBE_TEMPLATE_LAST_ACCEPT_TIME_KEY)
  return acceptTimeMap[templateId] || null
}

const getTemplateRejectTime = (templateId: string): number | null => {
  const rejectTimeMap = getStorageMap<number>(SUBSCRIBE_TEMPLATE_LAST_REJECT_TIME_KEY)
  return rejectTimeMap[templateId] || null
}

const canRequestTemplateSubscribeAuth = (templateId: string): boolean => {
  const status = getTemplateStatus(templateId)
  const lastRejectTime = getTemplateRejectTime(templateId)

  if (status === AuthStatus.REJECTED && lastRejectTime) {
    const timeSinceReject = Date.now() - lastRejectTime
    return timeSinceReject >= REJECT_RETRY_INTERVAL
  }

  return true
}

const getRequestableTemplateIds = (
  templateTypes: Array<SubscribeTemplateType | string> = ['pickup'],
  force: boolean = false
): string[] => {
  const templateIds = normalizeTemplateIds(templateTypes)
  if (force) {
    return templateIds
  }
  return templateIds.filter(templateId => canRequestTemplateSubscribeAuth(templateId))
}

/**
 * 检查用户是否已接受订阅消息授权
 * @returns 是否已接受授权
 */
export function hasAcceptedSubscribeAuth(templateType: SubscribeTemplateType = 'pickup'): boolean {
  return getTemplateStatus(TEMPLATE_ID_MAP[templateType]) === AuthStatus.ACCEPTED
}

/**
 * 检查是否可以重新请求订阅消息授权
 * 如果用户之前拒绝了，需要等待一段时间后才能再次请求
 * @returns 是否可以请求授权
 */
export function canRequestSubscribeAuth(templateTypes: Array<SubscribeTemplateType | string> = ['pickup']): boolean {
  return getRequestableTemplateIds(templateTypes).length > 0
}

/**
 * 标记订阅消息授权状态
 * @param status 授权状态
 * @param rejectTime 拒绝时间（仅当status为REJECTED时需要）
 */
export function markSubscribeAuthStatus(
  status: AuthStatus,
  rejectTime?: number,
  templateTypes: Array<SubscribeTemplateType | string> = ['pickup']
): void {
  const templateIds = normalizeTemplateIds(templateTypes)
  const statusMap = getStorageMap<string>(SUBSCRIBE_TEMPLATE_STATUS_KEY)
  const rejectTimeMap = getStorageMap<number>(SUBSCRIBE_TEMPLATE_LAST_REJECT_TIME_KEY)
  const acceptTimeMap = getStorageMap<number>(SUBSCRIBE_TEMPLATE_LAST_ACCEPT_TIME_KEY)

  templateIds.forEach((templateId) => {
    statusMap[templateId] = status

    if (status === AuthStatus.REJECTED) {
      rejectTimeMap[templateId] = rejectTime || Date.now()
      delete acceptTimeMap[templateId]
    } else if (status === AuthStatus.ACCEPTED) {
      delete rejectTimeMap[templateId]
      acceptTimeMap[templateId] = Date.now()
    } else {
      delete rejectTimeMap[templateId]
      delete acceptTimeMap[templateId]
    }
  })

  setStorageMap(SUBSCRIBE_TEMPLATE_STATUS_KEY, statusMap)
  setStorageMap(SUBSCRIBE_TEMPLATE_LAST_REJECT_TIME_KEY, rejectTimeMap)
  setStorageMap(SUBSCRIBE_TEMPLATE_LAST_ACCEPT_TIME_KEY, acceptTimeMap)
}

const requestSubscribeMessages = (templateIds: string[]): Promise<boolean> => {
  debugLog('===== 开始请求微信订阅消息授权 =====')
  debugLog('模板ID列表:', templateIds)
  debugLog('当前环境:', process.env.NODE_ENV)

  return new Promise((resolve) => {
    if (templateIds.length === 0) {
      debugLog('ℹ️ 当前没有需要请求授权的模板')
      resolve(false)
      return
    }

    if (typeof uni.requestSubscribeMessage !== 'function') {
      debugWarn('❌ 当前环境不支持 uni.requestSubscribeMessage，可能原因：')
      debugWarn('1. 非微信小程序环境')
      debugWarn('2. 基础库版本过低（需要2.8.2以上）')
      debugWarn('3. 未在app.json中声明订阅消息权限')
      resolve(false)
      return
    }

    debugLog('✅ uni.requestSubscribeMessage API 可用')
    debugLog('正在调用微信授权弹窗...')

    uni.requestSubscribeMessage({
      tmplIds: templateIds,
      success: (res) => {
        debugLog('✅ 微信订阅消息授权API调用成功')
        debugLog('授权响应结果:', res)

        const acceptedTemplateIds = templateIds.filter(templateId => res[templateId] === 'accept')
        const rejectedTemplateIds = templateIds.filter(templateId => res[templateId] === 'reject')
        const bannedTemplateIds = templateIds.filter(templateId => res[templateId] === 'ban')
        const filteredTemplateIds = templateIds.filter(templateId => res[templateId] === 'filter')
        const missingTemplateIds = templateIds.filter(templateId => !(templateId in res))

        debugLog('----- 模板逐项授权诊断开始 -----')
        templateIds.forEach((templateId, index) => {
          const status = res[templateId]
          debugLog(`模板[${index + 1}] ${templateId} -> ${status || '未返回'}`)
        })
        debugLog('已接受模板数量:', acceptedTemplateIds.length)
        debugLog('已拒绝模板数量:', rejectedTemplateIds.length)
        debugLog('已封禁模板数量:', bannedTemplateIds.length)
        debugLog('被过滤模板数量:', filteredTemplateIds.length)
        debugLog('未返回结果模板数量:', missingTemplateIds.length)
        if (acceptedTemplateIds.length > 0) {
          debugLog('已接受模板列表:', acceptedTemplateIds)
        }
        if (rejectedTemplateIds.length > 0) {
          debugLog('已拒绝模板列表:', rejectedTemplateIds)
        }
        if (bannedTemplateIds.length > 0) {
          debugWarn('已封禁模板列表:', bannedTemplateIds)
        }
        if (filteredTemplateIds.length > 0) {
          debugWarn('被过滤模板列表:', filteredTemplateIds)
        }
        if (missingTemplateIds.length > 0) {
          debugWarn('未返回结果模板列表:', missingTemplateIds)
          debugWarn('提示: 这些模板本次没有出现在微信授权返回结果里，通常表示未被成功纳入本次授权流程')
        }
        debugLog('本次是否两个取餐模板都授权成功:', acceptedTemplateIds.includes(PICKUP_REMINDER_TEMPLATE_ID) && acceptedTemplateIds.includes(PICKUP_OVERTIME_TEMPLATE_ID))
        debugLog('----- 模板逐项授权诊断结束 -----')

        acceptedTemplateIds.forEach((templateId) => {
          debugLog(`🎉 模板已接受授权: ${templateId}`)
          markSubscribeAuthStatus(AuthStatus.ACCEPTED, undefined, [templateId])
        })

        rejectedTemplateIds.forEach((templateId) => {
          debugLog(`❌ 模板已拒绝授权: ${templateId}`)
          markSubscribeAuthStatus(AuthStatus.REJECTED, Date.now(), [templateId])
        })

        bannedTemplateIds.forEach((templateId) => {
          debugWarn(`🚫 模板已被封禁: ${templateId}`)
        })

        filteredTemplateIds.forEach((templateId) => {
          debugWarn(`⚠️ 模板被微信过滤: ${templateId}`)
        })

        resolve(acceptedTemplateIds.length > 0)
      },
      fail: (err) => {
        if (IS_SUBSCRIBE_DEBUG || err?.errCode) {
          console.error('❌ 微信订阅消息授权API调用失败')
          console.error('错误详情:', err)
          console.error('错误码:', err.errCode || '未知')
          console.error('错误信息:', err.errMsg || '未知')
        }

        if (err.errCode) {
          switch (err.errCode) {
            case 10001:
              debugLog('提示: 10001 - 用户取消授权')
              break
            case 10002:
              debugLog('提示: 10002 - 用户拒绝授权')
              break
            case 10003:
              debugLog('提示: 10003 - 用户已订阅，无需重复订阅')
              break
            case 10004:
              debugWarn('提示: 10004 - 参数错误')
              break
            case 10005:
              debugWarn('提示: 10005 - 模板ID无效')
              break
          }
        }

        if (err.errCode === 10002) {
          markSubscribeAuthStatus(AuthStatus.REJECTED, Date.now(), templateIds)
        } else if (err.errCode === 10003) {
          markSubscribeAuthStatus(AuthStatus.ACCEPTED, undefined, templateIds)
        }

        resolve(false)
      },
      complete: () => {
        debugLog('微信订阅消息授权流程完成')
      }
    })
  })
}

/**
 * 请求订阅消息授权
 * @param templateId 模板ID，默认为取餐提醒模板
 * @returns Promise<boolean> 是否授权成功
 */
export function requestSubscribeMessage(templateId: string = PICKUP_REMINDER_TEMPLATE_ID): Promise<boolean> {
  return requestSubscribeMessages([templateId])
}

/**
 * 智能请求订阅消息授权
 * 会检查是否已请求过授权，避免频繁弹窗
 * @param force 是否强制请求（忽略已请求过的标记）
 * @returns Promise<boolean> 是否授权成功
 */
export function smartRequestSubscribeMessage(force: boolean = false): Promise<boolean> {
  debugLog('===== 智能请求订阅消息授权 =====')
  debugLog('强制请求:', force)
  
  // 检查是否可以请求授权（考虑用户拒绝后的冷却时间）
  const targetTemplateId = PICKUP_REMINDER_TEMPLATE_ID
  if (!force && !canRequestSubscribeAuth(['pickup'])) {
    const status = getTemplateStatus(targetTemplateId)
    const lastRejectTime = getTemplateRejectTime(targetTemplateId)
    const lastAcceptTime = getTemplateAcceptTime(targetTemplateId)

    debugLog('ℹ️ 当前不适合请求订阅消息授权')
    debugLog('授权状态:', status)
    debugLog('上次拒绝时间:', lastRejectTime ? new Date(lastRejectTime).toLocaleString() : '无')
    debugLog('上次接受时间:', lastAcceptTime ? new Date(lastAcceptTime).toLocaleString() : '无')
    
    if (status === AuthStatus.REJECTED && lastRejectTime) {
      const timeSinceReject = Date.now() - lastRejectTime
      const daysLeft = Math.ceil((REJECT_RETRY_INTERVAL - timeSinceReject) / (24 * 60 * 60 * 1000))
      debugLog(`提示: 用户已拒绝授权，还需等待约${daysLeft}天才能重新请求`)
    } else if (status === AuthStatus.ACCEPTED) {
      debugLog('提示: 订阅消息是一次性的，已接受授权的模板在新订单场景下仍可再次请求')
    }
    
    debugLog('提示: 如需强制请求，请设置force=true参数')
    return Promise.resolve(false)
  }

  debugLog('可以请求授权，继续请求授权流程')
  return requestSubscribeMessages(getRequestableTemplateIds(['pickup'], force))
}

/**
 * 在合适的时机请求订阅消息授权
 * 推荐在以下场景调用：
 * 1. 用户提交订单前
 * 2. 用户查看订单列表时（有进行中的订单）
 * 3. 用户进入"我的"页面时
 * @param context 调用场景描述，用于日志
 * @returns Promise<boolean> 是否授权成功
 */
export function requestSubscribeMessageOnOpportunity(
  context: string,
  templateTypes: SubscribeTemplateType[] = ['pickup']
): Promise<boolean> {
  debugLog(`===== 在合适时机请求订阅消息授权 =====`)
  debugLog(`场景: ${context}`)
  debugLog(`时间: ${new Date().toLocaleString()}`)
  
  // 这里可以添加更复杂的逻辑，比如：
  // 1. 检查用户是否登录
  // 2. 检查用户是否有进行中的订单
  // 3. 检查是否在合适的时间段（避免深夜打扰）
  
  debugLog('调用智能请求所有订阅消息授权函数...')
  return smartRequestAllSubscribeMessages(false, templateTypes).then(result => {
    debugLog(`授权结果: ${result ? '成功' : '失败'}`)
    debugLog('===== 授权流程结束 =====')
    return result
  }).catch(err => {
    console.error('授权过程异常:', err)
    debugLog('===== 授权流程异常结束 =====')
    return false
  })
}

/**
 * 检查订阅消息授权状态（调试用）
 * 返回当前的授权状态信息
 */
export function checkSubscribeAuthStatus(): {
  authStatus: string
  lastRejectTime: number | null
  lastAcceptTime: number | null
  canRequest: boolean
  templateId: string
  timeUntilRetry: string | null
  acceptTimeInfo: string | null
  allTemplateStatus: Record<string, string>
} {
  const templateId = PICKUP_REMINDER_TEMPLATE_ID
  const authStatus = getTemplateStatus(templateId)
  const lastRejectTime = getTemplateRejectTime(templateId)
  const lastAcceptTime = getTemplateAcceptTime(templateId)
  const canRequest = canRequestSubscribeAuth(['pickup'])
  const allTemplateStatus = getStorageMap<string>(SUBSCRIBE_TEMPLATE_STATUS_KEY)
  
  let timeUntilRetry: string | null = null
  let acceptTimeInfo: string | null = null
  
  if (authStatus === AuthStatus.REJECTED && lastRejectTime) {
    const timeSinceReject = Date.now() - lastRejectTime
    if (timeSinceReject < REJECT_RETRY_INTERVAL) {
      const daysLeft = Math.ceil((REJECT_RETRY_INTERVAL - timeSinceReject) / (24 * 60 * 60 * 1000))
      timeUntilRetry = `${daysLeft}天`
    }
  } else if (authStatus === AuthStatus.ACCEPTED && lastAcceptTime) {
    const timeSinceAccept = Date.now() - lastAcceptTime
    const hoursSinceAccept = Math.floor(timeSinceAccept / (60 * 60 * 1000))
    acceptTimeInfo = `${hoursSinceAccept}小时前接受；订阅消息为一次性，新订单仍可再次请求`
  }
  
  debugLog('===== 订阅消息授权状态检查 =====')
  debugLog('授权状态:', authStatus)
  debugLog('上次拒绝时间:', lastRejectTime ? new Date(lastRejectTime).toLocaleString() : '无')
  debugLog('上次接受时间:', lastAcceptTime ? new Date(lastAcceptTime).toLocaleString() : '无')
  debugLog('是否可以请求授权:', canRequest)
  debugLog('距离可重新请求:', timeUntilRetry || (acceptTimeInfo ? '接受状态' : '可以立即请求'))
  if (acceptTimeInfo) {
    debugLog('接受时间信息:', acceptTimeInfo)
  }
  debugLog('模板ID:', PICKUP_REMINDER_TEMPLATE_ID)
  debugLog('全部模板状态:', allTemplateStatus)
  debugLog('当前时间:', new Date().toLocaleString())
  debugLog('===== 状态检查结束 =====')
  
  return {
    authStatus,
    lastRejectTime,
    lastAcceptTime,
    canRequest,
    templateId,
    timeUntilRetry,
    acceptTimeInfo,
    allTemplateStatus
  }
}

/**
 * 请求所有订阅消息模板授权（取餐提醒 + 抽奖中奖通知）
 * @returns Promise<boolean> 是否至少有一个模板授权成功
 */
export function requestAllSubscribeMessages(): Promise<boolean> {
  return requestSubscribeMessages([PICKUP_REMINDER_TEMPLATE_ID, LUCKY_DRAW_WINNER_TEMPLATE_ID])
}

/**
 * 智能请求所有订阅消息授权
 * 会检查是否已请求过授权，避免频繁弹窗
 * @param force 是否强制请求（忽略已请求过的标记）
 * @returns Promise<boolean> 是否至少有一个模板授权成功
 */
export function smartRequestAllSubscribeMessages(
  force: boolean = false,
  templateTypes: SubscribeTemplateType[] = ['pickup', 'pickupOvertime', 'luckyDraw']
): Promise<boolean> {
  debugLog('===== 智能请求所有订阅消息授权 =====')
  debugLog('强制请求:', force)
  debugLog('目标模板:', templateTypes)
  
  // 检查是否可以请求授权（考虑用户拒绝后的冷却时间）
  const requestableTemplateIds = getRequestableTemplateIds(templateTypes, force)
  if (requestableTemplateIds.length === 0) {
    debugLog('ℹ️ 当前不适合请求订阅消息授权')
    normalizeTemplateIds(templateTypes).forEach((templateId) => {
      const status = getTemplateStatus(templateId)
      const lastRejectTime = getTemplateRejectTime(templateId)
      const lastAcceptTime = getTemplateAcceptTime(templateId)
      debugLog('模板ID:', templateId)
      debugLog('授权状态:', status)
      debugLog('上次拒绝时间:', lastRejectTime ? new Date(lastRejectTime).toLocaleString() : '无')
      debugLog('上次接受时间:', lastAcceptTime ? new Date(lastAcceptTime).toLocaleString() : '无')
    })
    debugLog('提示: 当前模板都处于冷却期内，如需强制请求，请设置force=true参数')
    return Promise.resolve(false)
  }

  debugLog('可以请求授权，继续请求授权流程')
  return requestSubscribeMessages(requestableTemplateIds)
}

/**
 * 手动触发订阅消息授权（调试用）
 * 强制显示授权弹窗，忽略本地存储状态
 */
export function manualTriggerSubscribeAuth(): Promise<boolean> {
  debugLog('===== 手动触发订阅消息授权（调试用） =====')
  debugLog('注意：这将强制显示授权弹窗，忽略本地存储状态')
  
  return smartRequestSubscribeMessage(true)
}
