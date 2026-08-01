<template>
  <view class="confirm-container">
    <view class="nav-bar">
      <uni-icons type="back" size="24" color="#333" @tap="goBack"></uni-icons>
      <text class="nav-title">确认订单</text>
    </view>

    <view class="section shop-header">
      <image v-if="getImageUrl(shopDetail.logoImage)" :src="getImageUrl(shopDetail.logoImage)" class="shop-logo" />
      <view v-else class="shop-logo default-logo">
        <uni-icons type="shop-filled" size="30" color="#ccc"></uni-icons>
      </view>
      <view class="shop-info">
        <text class="shop-name">{{ shopDetail.name }}</text>
        <view class="shop-address-row">
          <uni-icons type="location" size="14" color="#666"></uni-icons>
          <text class="shop-address">{{ shopDetail.address }}</text>
        </view>
      </view>
    </view>

    <view class="section integrity-tip">
      <view class="tip-header">
        <uni-icons type="heart-filled" size="20" color="#ff7b2c"></uni-icons>
        <text class="tip-title">诚信提示</text>
      </view>
      <view class="tip-content">
        <text class="tip-text">下单即承诺，取餐显诚信。请按时取餐，避免浪费资源。</text>
      </view>
    </view>

    <view class="section remark">
      <view class="section-title">取餐方式</view>
      <view class="pack-selector">
        <view class="pack-item" :class="{ active: needPack === 0 }" @tap="needPack = 0">
          <text>店内就餐</text>
          <uni-icons v-if="needPack === 0" type="checkmarkempty" size="16" color="#2a8bff"></uni-icons>
        </view>
        <view class="pack-item" :class="{ active: needPack === 1 }" @tap="needPack = 1">
          <text>打包带走 (￥1)</text>
          <uni-icons v-if="needPack === 1" type="checkmarkempty" size="16" color="#2a8bff"></uni-icons>
        </view>
      </view>
    </view>

    <view class="section remark">
      <view class="section-title">取餐时间 (必选)</view>
      <picker mode="selector" :range="availableTimes" @change="onTimeChange">
        <view class="picker-row">
          <text :class="['time-val', { placeholder: !selectedTime }]">
            {{ selectedTime || '请选择取餐时间' }}
          </text>
          <uni-icons type="arrowright" size="16" color="#999"></uni-icons>
        </view>
      </picker>
      <view v-if="showTasteSensitiveTip" class="taste-sensitive-tip">
        <uni-icons type="info-filled" size="15" color="#f08c2e"></uni-icons>
        <text class="taste-sensitive-tip__text">该类型菜品建议尽快取餐，超时取餐可能会影响口感。</text>
      </view>
    </view>

    <view class="section remark">
      <view class="section-title">订单备注 (可选)</view>
      <view class="remark-row">
        <view
          class="remark-field"
          :class="{ 'remark-field--focused': remarkInputFocus }"
          @tap="focusRemarkInput"
        >
          <input
            v-model.trim="remark"
            type="text"
            maxlength="30"
            placeholder="口味、忌口等要求"
            placeholder-class="remark-input__placeholder"
            class="remark-input"
            :adjust-position="true"
            :focus="remarkInputFocus"
            :cursor="remarkCursor"
            @focus="remarkInputFocus = true"
            @blur="remarkInputFocus = false"
          />
          <view
            class="remark-quick-list"
            @tap.stop
          >
            <view
              v-for="option in REMARK_QUICK_OPTIONS"
              :key="option"
              class="remark-quick-item"
              :class="{ active: isQuickRemarkActive(option) }"
              @tap.stop="applyQuickRemarkOption(option)"
            >
              {{ option }}
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="section order-items">
      <view class="order-items__header">
        <view class="section-title order-items__title">订单详情</view>
        <text class="order-items__summary">{{ orderSummaryText }}</text>
      </view>

      <template v-if="isWeightSelectionMode">
        <view class="broth-summary-card">
          <view class="broth-summary-card__content">
            <image
              v-if="getImageUrl(weightDraft.brothImage)"
              :src="getImageUrl(weightDraft.brothImage)"
              class="broth-summary-card__image"
              mode="aspectFill"
            />
            <view v-else class="broth-summary-card__image broth-summary-card__image--fallback">
              <uni-icons type="fire" size="18" color="#ff9f43"></uni-icons>
            </view>
            <view class="broth-summary-card__meta">
              <text class="broth-summary-card__name">{{ weightDraft.brothName || '未选择' }}</text>
              <text class="broth-summary-card__desc">
                {{ weightDraft.brothExtraPrice > 0 ? `附加￥${Number(weightDraft.brothExtraPrice).toFixed(2)}` : '默认不加价' }}
              </text>
            </view>
          </view>
        </view>

        <view v-for="item in displayedWeightItems" :key="item.ingredientId" class="item">
          <image v-if="getImageUrl(item.image)" :src="getImageUrl(item.image)" class="item-img" mode="aspectFill" />
          <view v-else class="item-img default-item-img">
            <uni-icons type="fire" size="24" color="#ddd"></uni-icons>
          </view>
          <view class="item-content">
            <view class="item-top">
              <text class="item-name">{{ item.name }}</text>
              <text class="item-price">x{{ item.quantity }}</text>
            </view>
            <view class="item-bottom">
              <text class="item-qty">{{ item.unitLabel || '份' }} / 约{{ item.referenceWeightG }}g</text>
              <text class="item-subtotal">预计 {{ item.estimatedWeightG }}g</text>
            </view>
          </view>
        </view>

        <view class="weight-order-tip">
          <uni-icons type="info-filled" size="15" color="#2a8bff"></uni-icons>
          <text class="weight-order-tip__text">最终金额以商户称重确认为准，到店后扫码支付取餐。</text>
        </view>

        <view v-if="hasMoreOrderItems" class="order-items__toggle" @tap="toggleOrderItems">
          <text class="order-items__toggle-text">{{ orderToggleText }}</text>
          <uni-icons
            type="bottom"
            size="14"
            color="#2a8bff"
            class="order-items__toggle-icon"
            :class="{ open: orderItemsExpanded }"
          ></uni-icons>
        </view>

        <view class="total">
          <text>预计合计</text>
          <text class="price">{{ weightFooterAmount }}</text>
        </view>
      </template>

      <template v-else>
        <view
          v-for="(item, index) in displayedFixedDishItems"
          :key="item.cartItemId || `${item.dishId}-${item.optionSummary || 'default'}-${index}`"
          class="item"
        >
          <image v-if="getImageUrl(item.image)" :src="getImageUrl(item.image)" class="item-img" mode="aspectFill" />
          <view v-else class="item-img default-item-img">
            <uni-icons type="fire" size="24" color="#ddd"></uni-icons>
          </view>
          <view class="item-content">
            <view class="item-top">
              <text class="item-name">{{ item.name }}</text>
              <text class="item-price">￥{{ item.price }}</text>
            </view>
            <view class="item-bottom">
              <view class="item-qty-block">
                <text class="item-qty">x{{ item.quantity }}</text>
                <text v-if="item.optionSummary" class="item-options">{{ item.optionSummary }}</text>
              </view>
              <text class="item-subtotal">小计: ￥{{ (item.price * item.quantity).toFixed(2) }}</text>
            </view>
          </view>
        </view>

        <view v-if="hasMoreOrderItems" class="order-items__toggle" @tap="toggleOrderItems">
          <text class="order-items__toggle-text">{{ orderToggleText }}</text>
          <uni-icons
            type="bottom"
            size="14"
            color="#2a8bff"
            class="order-items__toggle-icon"
            :class="{ open: orderItemsExpanded }"
          ></uni-icons>
        </view>

        <view class="total">
          <text>合计</text>
          <text class="price">{{ fixedDishFooterAmount }}</text>
        </view>
      </template>
    </view>

    <view class="footer">
      <view class="price-box">
        <text class="label">{{ isWeightSelectionMode ? '预计:' : '合计:' }}</text>
        <text class="amount">{{ footerAmountText }}</text>
        <text v-if="footerWeightText" class="weight-text">{{ footerWeightText }}</text>
      </view>
      <view class="submit-box">
        <text class="submit-tip">{{ isWeightSelectionMode ? '最终金额以商户称重确认结果为准' : '到店扫码支付取餐' }}</text>
        <view class="btn-pay" :class="{ 'btn-pay--disabled': submitInProgress }" @tap="submitOrder">
          {{ submitInProgress ? '提交中...' : '提交订单' }}
        </view>
      </view>
    </view>

    <view v-if="showPenaltyDialog" class="penalty-dialog-mask" @tap="hidePenaltyDialog">
      <view class="penalty-dialog" @tap.stop>
        <view class="penalty-dialog__title">当前无法下单</view>
        <view class="penalty-dialog__content">{{ penaltyDialogMessage }}</view>
        <view class="penalty-dialog__tip">如需申诉或恢复，可添加下方账号联系处理。</view>
        <view class="penalty-dialog__actions">
          <view class="penalty-dialog__button" @tap="handlePenaltyDialogConfirm">
            {{ penaltyDialogButtonText }}
          </view>
        </view>
      </view>
    </view>

    <BrandLoadingOverlay
      :visible="pageLoadingVisible"
      :title="pageLoadingTitle"
      :description="pageLoadingDescription"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  SINGLE_DISH_QUANTITY_LIMIT,
  TOTAL_DISH_QUANTITY_LIMIT,
  useCartStore,
} from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { http, baseURL } from '@/utils/http'
import { safeNavigateTo, safeRedirectTo, smartNavigateBack } from '@/utils/navigation'
import { requestSubscribeMessageOnOpportunity } from '@/utils/subscribe'
import BrandLoadingOverlay from '@/components/BrandLoadingOverlay.vue'
import { useBrandLoading } from '@/composables/useBrandLoading'

type ReservationRuleConfig = {
  offPeakMinMinutes: number
  peakMinMinutes: number
  lunchPeakStart: string
  lunchPeakEnd: string
  dinnerPeakStart: string
  dinnerPeakEnd: string
  workdayOnly: number
}

type OrderRiskStatus = {
  noShowCount: number
  currentActiveOrderCount: number
  penaltyStatus: 'normal' | 'blocked_3d' | 'blocked_7d' | 'blocked_30d' | 'frozen'
  restrictionLevel: 'normal' | 'blocked_3d' | 'blocked_7d' | 'blocked_30d' | 'frozen' | 'active_order_limit'
  restrictionMessage?: string
  penaltyReason?: string
  penaltyEndTime?: string | null
  frozenContactNote?: string
}

const cartStore = useCartStore()
const userStore = useUserStore()

const shopDetail = ref<any>({})
const isWeightSelectionMode = ref(false)
const weightDraft = ref<any>({ items: [] })
const remark = ref('')
const selectedTime = ref('')
const availableTimes = ref<string[]>([])
const needPack = ref(0)
const showPenaltyDialog = ref(false)
const penaltyDialogMessage = ref('')
const orderItemsExpanded = ref(false)
const remarkInputFocus = ref(false)
const remarkCursor = ref(-1)
const submitInProgress = ref(false)
const currentOrderRequestId = ref('')
const reservationRuleConfig = ref<ReservationRuleConfig | null>(null)
const {
  visible: pageLoadingVisible,
  title: pageLoadingTitle,
  description: pageLoadingDescription,
  show: showPageLoading,
  hide: hidePageLoading
} = useBrandLoading({
  title: '订单页面正在准备',
  description: '正在同步店铺与取餐信息，请稍候'
})

const OFFICIAL_WECHAT_ID = 'skkq-8888'
const penaltyDialogButtonText = `联系方式：${OFFICIAL_WECHAT_ID}`
const RESERVATION_RULE_CACHE_KEY = 'reservation_rule_config_cache'
const RESERVATION_RULE_CACHE_TTL = 5 * 60 * 1000
const RESERVATION_OPEN_MINUTES = 7 * 60 + 30
const TIME_SLOT_STEP_MINUTES = 5
const ORDER_PREVIEW_COUNT = 3
const REMARK_QUICK_OPTIONS = ['不辣', '微辣', '中辣'] as const

const currentShopId = computed(() =>
  isWeightSelectionMode.value ? Number(weightDraft.value.shopId || 0) : Number(cartStore.shopId || 0)
)

const showTasteSensitiveTip = computed(() => shopDetail.value?.tasteSensitiveEnabled === 1)

const fixedDishFooterAmount = computed(() =>
  `￥${(cartStore.totalAmount + (needPack.value === 1 ? 1 : 0)).toFixed(2)}`
)

const weightFooterAmount = computed(() => {
  const amountRangeText = String(weightDraft.value.amountRangeText || '0-0元').replace(/元/g, '')
  if (needPack.value !== 1) {
    return `￥${amountRangeText}`
  }
  const parts = amountRangeText.split('-').map((num: string) => Number(num || 0) + 1)
  if (parts.length === 2) {
    return `￥${parts[0]}-${parts[1]}`
  }
  return `￥${(Number(amountRangeText || 0) + 1).toFixed(0)}`
})

const footerAmountText = computed(() =>
  isWeightSelectionMode.value ? weightFooterAmount.value : fixedDishFooterAmount.value
)
const footerWeightText = computed(() =>
  isWeightSelectionMode.value ? `约${Number(weightDraft.value.estimatedWeightG || 0)}g` : ''
)
const fixedDishItems = computed(() => cartStore.items || [])
const weightOrderItems = computed(() => weightDraft.value.items || [])
const hasMoreOrderItems = computed(() =>
  isWeightSelectionMode.value
    ? weightOrderItems.value.length > ORDER_PREVIEW_COUNT
    : fixedDishItems.value.length > ORDER_PREVIEW_COUNT
)
const displayedFixedDishItems = computed(() =>
  orderItemsExpanded.value ? fixedDishItems.value : fixedDishItems.value.slice(0, ORDER_PREVIEW_COUNT)
)
const displayedWeightItems = computed(() =>
  orderItemsExpanded.value ? weightOrderItems.value : weightOrderItems.value.slice(0, ORDER_PREVIEW_COUNT)
)
const hiddenOrderItemCount = computed(() => {
  const total = isWeightSelectionMode.value ? weightOrderItems.value.length : fixedDishItems.value.length
  return Math.max(0, total - ORDER_PREVIEW_COUNT)
})
const orderSummaryText = computed(() => {
  if (isWeightSelectionMode.value) {
    return `共 ${weightOrderItems.value.length} 种食材`
  }
  const totalDishCount = fixedDishItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  return `共 ${fixedDishItems.value.length} 道菜 / ${totalDishCount} 份`
})
const orderToggleText = computed(() => {
  if (orderItemsExpanded.value) return '收起详情'
  return hiddenOrderItemCount.value > 0 ? `展开其余 ${hiddenOrderItemCount.value} 项` : '展开详情'
})

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return baseURL + url
}

const buildOrderRequestId = () =>
  `ord_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`

const ensureOrderRequestId = () => {
  if (!currentOrderRequestId.value) {
    currentOrderRequestId.value = buildOrderRequestId()
  }
  return currentOrderRequestId.value
}

const goBack = () => {
  smartNavigateBack({ fallbackTab: 'pages/index/index' })
}

const toggleOrderItems = () => {
  orderItemsExpanded.value = !orderItemsExpanded.value
}

const toMinutes = (time: string) => {
  const [hour, minute] = time.split(':').map(Number)
  return hour * 60 + minute
}

const isWorkday = (date: Date) => {
  const day = date.getDay()
  return day >= 1 && day <= 5
}

const isInRange = (target: number, start: number, end: number) => {
  return target >= start && target <= end
}

const getCachedReservationRuleConfig = (): ReservationRuleConfig | null => {
  const cache = uni.getStorageSync(RESERVATION_RULE_CACHE_KEY)
  if (!cache || typeof cache !== 'object') return null
  if (Number(cache.expireAt || 0) <= Date.now()) return null
  return cache.data || null
}

const setCachedReservationRuleConfig = (config: ReservationRuleConfig) => {
  uni.setStorageSync(RESERVATION_RULE_CACHE_KEY, {
    data: config,
    expireAt: Date.now() + RESERVATION_RULE_CACHE_TTL
  })
}

const fetchReservationRuleConfig = async (): Promise<ReservationRuleConfig | null> => {
  const cachedConfig = getCachedReservationRuleConfig()
  if (cachedConfig) {
    reservationRuleConfig.value = cachedConfig
    return cachedConfig
  }

  try {
    const res = await http<ReservationRuleConfig>({
      url: '/api/client/shops/reservation-rule-config',
      hideErrorToast: true
    })
    reservationRuleConfig.value = res.data
    setCachedReservationRuleConfig(res.data)
    return res.data
  } catch {
    reservationRuleConfig.value = null
    return null
  }
}

const ceilToStepMinutes = (minutes: number, step: number) => {
  return Math.ceil(minutes / step) * step
}

const fetchShopDetail = async () => {
  if (!currentShopId.value) return
  const res = await http<any>({
    url: `/api/client/shops/${currentShopId.value}`,
  })
  shopDetail.value = res.data
}

const generateTimeSlots = () => {
  if (!shopDetail.value) return

  const times: string[] = []
  const now = new Date()
  const ruleConfig = reservationRuleConfig.value
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  if (currentMinutes < RESERVATION_OPEN_MINUTES) {
    availableTimes.value = ['07:30后开放预约']
    return
  }

  const resolveMinMinutes = (candidateMinutes: number) => {
    if (!ruleConfig) return 15
    const peakLimitEnabled = shopDetail.value?.peakLimitEnabled === 1
    if (!peakLimitEnabled) return ruleConfig.offPeakMinMinutes || 15
    if (ruleConfig.workdayOnly === 1 && !isWorkday(now)) {
      return ruleConfig.offPeakMinMinutes || 15
    }

    const inLunchPeak = isInRange(
      candidateMinutes,
      toMinutes(ruleConfig.lunchPeakStart),
      toMinutes(ruleConfig.lunchPeakEnd)
    )
    const inDinnerPeak = isInRange(
      candidateMinutes,
      toMinutes(ruleConfig.dinnerPeakStart),
      toMinutes(ruleConfig.dinnerPeakEnd)
    )

    return (inLunchPeak || inDinnerPeak)
      ? (ruleConfig.peakMinMinutes || 25)
      : (ruleConfig.offPeakMinMinutes || 15)
  }

  const addSlots = (openStr: string, closeStr: string) => {
    if (!openStr || !closeStr) return
    const [openH, openM] = openStr.split(':').map(Number)
    const [closeH, closeM] = closeStr.split(':').map(Number)
    const openMinutes = openH * 60 + openM
    const closeMinutes = closeH * 60 + closeM

    let current = ceilToStepMinutes(
      Math.max(openMinutes, currentMinutes),
      TIME_SLOT_STEP_MINUTES
    )
    while (current <= closeMinutes) {
      const minMinutes = resolveMinMinutes(current)
      if (current >= currentMinutes + minMinutes) {
        break
      }
      current += TIME_SLOT_STEP_MINUTES
    }

    if (current > closeMinutes) return

    while (current <= closeMinutes) {
      const h = Math.floor(current / 60)
      const m = current % 60
      if (h >= 24) break
      const timeStr = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`
      times.push(timeStr)
      current += TIME_SLOT_STEP_MINUTES
    }
  }

  addSlots(shopDetail.value.openTime1, shopDetail.value.closeTime1)
  addSlots(shopDetail.value.openTime2, shopDetail.value.closeTime2)

  if (times.length === 0) {
    times.push('当前无可预约时间')
  }

  availableTimes.value = times
}

const initConfirmPageData = async () => {
  await Promise.all([fetchShopDetail(), fetchReservationRuleConfig()])
  generateTimeSlots()
}

const onTimeChange = (e: any) => {
  const val = availableTimes.value[e.detail.value]
  if (val === '当前无可预约时间' || val === '07:30后开放预约') {
    uni.showToast({ title: val, icon: 'none' })
    selectedTime.value = ''
    return
  }
  selectedTime.value = val
}

const fetchOrderRiskStatus = async () => {
  const res = await http<OrderRiskStatus>({
    url: '/api/client/orders/risk-status',
    hideErrorToast: true
  })
  return res.data
}

const copyOfficialWechatId = () => {
  return new Promise<boolean>((resolve) => {
    uni.setClipboardData({
      data: OFFICIAL_WECHAT_ID,
      success: () => resolve(true),
      fail: () => resolve(false)
    })
  })
}

const hidePenaltyDialog = () => {
  showPenaltyDialog.value = false
}

const showPenaltyBlockDialog = (message: string) => {
  return new Promise<void>((resolve) => {
    penaltyDialogMessage.value = message
    showPenaltyDialog.value = true

    const stopWatch = watch(showPenaltyDialog, (visible) => {
      if (!visible) {
        stopWatch()
        resolve()
      }
    })
  })
}

const handlePenaltyDialogConfirm = async () => {
  const copied = await copyOfficialWechatId()
  showPenaltyDialog.value = false
  uni.showToast({
    title: copied ? '联系方式已复制' : `请手动添加：${OFFICIAL_WECHAT_ID}`,
    icon: 'none'
  })
}

const extractErrorMessage = (err: any) => {
  if (!err) return ''
  if (typeof err === 'string') return err
  if (typeof err.msg === 'string') return err.msg
  if (typeof err.errMsg === 'string') return err.errMsg
  return ''
}

const isPenaltyRestrictionMessage = (message: string) => {
  return message.includes('限制下单')
    || message.includes('无法下单')
    || message.includes('冻结')
    || message.includes('最多只能同时保留2个进行中订单')
}

const normalizeRemarkText = (value: string) => value
  .replace(/[，,、]/g, ' ')
  .replace(/\s+/g, ' ')
  .trim()

const getRemarkParts = (value: string) => normalizeRemarkText(value)
  .split(' ')
  .filter(Boolean)

const isQuickRemarkActive = (option: typeof REMARK_QUICK_OPTIONS[number]) => {
  return getRemarkParts(remark.value).includes(option)
}

const focusRemarkInput = async () => {
  remarkCursor.value = remark.value.length
  remarkInputFocus.value = false
  await nextTick()
  remarkInputFocus.value = true
}

const applyQuickRemarkOption = async (option: typeof REMARK_QUICK_OPTIONS[number]) => {
  const currentParts = getRemarkParts(remark.value)
  const hasCurrentOption = currentParts.includes(option)
  const customParts = currentParts.filter(
    part => !REMARK_QUICK_OPTIONS.includes(part as typeof REMARK_QUICK_OPTIONS[number])
  )

  remark.value = hasCurrentOption
    ? customParts.join(' ')
    : [option, ...customParts].join(' ')

  await focusRemarkInput()
}

const validateBeforeSubmit = () => {
  if (!userStore.isLogin) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      safeNavigateTo({ url: '/pages/login/login' })
    }, 1500)
    return false
  }

  if (!selectedTime.value) {
    uni.showToast({ title: '请选择取餐时间', icon: 'none' })
    return false
  }

  if (isWeightSelectionMode.value) {
    if (!weightDraft.value.brothOptionId) {
      uni.showToast({ title: '请先选择汤底', icon: 'none' })
      return false
    }
    if (!Array.isArray(weightDraft.value.items) || weightDraft.value.items.length === 0) {
      uni.showToast({ title: '请先选择食材', icon: 'none' })
      return false
    }
    return true
  }

  const totalQuantity = cartStore.items.reduce((sum, item) => sum + item.quantity, 0)
  if (totalQuantity > TOTAL_DISH_QUANTITY_LIMIT) {
    uni.showToast({
      title: `最多只能点 ${TOTAL_DISH_QUANTITY_LIMIT} 份菜品`,
      icon: 'none'
    })
    return false
  }

  const overLimitItem = cartStore.items.find((item) => item.quantity > SINGLE_DISH_QUANTITY_LIMIT)
  if (overLimitItem) {
    uni.showToast({
      title: `${overLimitItem.name} 最多点 ${SINGLE_DISH_QUANTITY_LIMIT} 份`,
      icon: 'none'
    })
    return false
  }

  return true
}

const buildOrderPayload = () => {
  const clientRequestId = ensureOrderRequestId()

  if (isWeightSelectionMode.value) {
    return {
      shopId: currentShopId.value,
      clientRequestId,
      pickupTime: selectedTime.value,
      needPack: needPack.value,
      remark: remark.value,
      brothOptionId: weightDraft.value.brothOptionId,
      weightItems: weightDraft.value.items.map((item: any) => ({
        ingredientId: item.ingredientId,
        quantity: item.quantity
      }))
    }
  }

  return {
    shopId: currentShopId.value,
    clientRequestId,
    pickupTime: selectedTime.value,
    needPack: needPack.value,
    remark: remark.value,
    items: cartStore.items.map((item) => ({
      dishId: item.dishId,
      quantity: item.quantity,
      selectedOptions: (item.selectedOptions || []).map((option) => ({
        optionGroupId: option.optionGroupId,
        optionValueId: option.optionValueId
      }))
    }))
  }
}

const submitOrderRequest = async (clientRequestId: string, allowRetry = true): Promise<any> => {
  try {
    return await http<any>({
      url: '/api/client/orders',
      method: 'POST',
      hideErrorToast: true,
      header: {
        'content-type': 'application/json',
        'X-Client-Request-Id': clientRequestId
      },
      data: buildOrderPayload()
    })
  } catch (err) {
    if (allowRetry && extractErrorMessage(err) === '订单正在处理中，请稍后重试') {
      await new Promise((resolve) => setTimeout(resolve, 800))
      return submitOrderRequest(clientRequestId, false)
    }
    throw err
  }
}

const submitOrder = async () => {
  if (submitInProgress.value) return
  if (!validateBeforeSubmit()) return
  submitInProgress.value = true
  const riskStatusPromise = fetchOrderRiskStatus().catch(() => null)

  try {
    await requestSubscribeMessageOnOpportunity('结算页-提交订单按钮', ['pickup', 'pickupOvertime'])
  } catch (err) {
    console.error('订阅消息授权失败:', err)
  }

  try {
    const riskStatus = await riskStatusPromise

    if (riskStatus && riskStatus.restrictionLevel !== 'normal') {
      await showPenaltyBlockDialog(
        riskStatus.restrictionMessage || '当前暂时无法下单'
      )
      submitInProgress.value = false
      return
    }
  } catch {
    // 风险状态接口只做辅助校验，失败时继续走后端兜底校验
  }

  showPageLoading({
    title: '订单正在提交',
    description: '正在为你锁定取餐信息，请稍候',
    delay: 180
  })

  try {
    const clientRequestId = ensureOrderRequestId()
    const res = await submitOrderRequest(clientRequestId)

    hidePageLoading(true)
    if (isWeightSelectionMode.value) {
      uni.removeStorageSync('weightSelectionDraft')
    } else {
      cartStore.clearCart()
    }

    safeRedirectTo({
      url: `/pages/success/success?orderId=${res.data.id}&pickupCode=${res.data.pickupCode}&shopName=${encodeURIComponent(shopDetail.value?.name || '')}&tasteSensitiveEnabled=${shopDetail.value?.tasteSensitiveEnabled === 1 ? 1 : 0}`
    })
  } catch (err) {
    hidePageLoading(true)
    const errorMessage = extractErrorMessage(err)

    if (isPenaltyRestrictionMessage(errorMessage)) {
      await showPenaltyBlockDialog(errorMessage)
      return
    }

    uni.showToast({
      title: errorMessage || '下单失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    submitInProgress.value = false
  }
}

onLoad(async (options: any) => {
  isWeightSelectionMode.value = options?.mode === 'weight_selection'
  orderItemsExpanded.value = false
  currentOrderRequestId.value = ''

  if (isWeightSelectionMode.value) {
    weightDraft.value = uni.getStorageSync('weightSelectionDraft') || { items: [] }
    if (!weightDraft.value.shopId) {
      uni.showToast({ title: '选料信息已失效', icon: 'none' })
      setTimeout(() => smartNavigateBack({ fallbackTab: 'pages/index/index' }), 500)
      return
    }
  }

  showPageLoading()
  try {
    await initConfirmPageData()
  } finally {
    hidePageLoading(true)
  }
})
</script>

<style lang="scss">
.confirm-container {
  padding: 20rpx;
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 160rpx;
  padding-top: calc(88rpx + var(--status-bar-height));
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 88rpx;
  background-color: #fff;
  z-index: 100;
  display: flex;
  align-items: center;
  padding: 0 20rpx;
  padding-top: var(--status-bar-height);

  .nav-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }
}

.section {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03);

  .section-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 24rpx;
    border-left: 8rpx solid #2a8bff;
    padding-left: 16rpx;
    line-height: 1;
  }
}

.shop-header {
  display: flex;
  align-items: center;

  .shop-logo {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    margin-right: 24rpx;
    background-color: #f5f5f5;

    &.default-logo {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .shop-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .shop-name {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 8rpx;
    }

    .shop-address-row {
      display: flex;
      align-items: center;

      .shop-address {
        font-size: 24rpx;
        color: #666;
        margin-left: 4rpx;
      }
    }
  }
}

.order-items__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.order-items__title {
  margin-bottom: 0 !important;
}

.order-items__summary {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #94a3b8;
}

.broth-summary-card {
  margin-top: 10rpx;
  margin-bottom: 12rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #fff8ef 0%, #fffdf8 100%);

  &__content {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__image {
    width: 88rpx;
    height: 88rpx;
    border-radius: 16rpx;
    flex-shrink: 0;
    background: #fff1df;

    &.broth-summary-card__image--fallback {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  &__meta {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: block;
    font-size: 28rpx;
    font-weight: 700;
    color: #1f2937;
  }

  &__desc {
    display: block;
    margin-top: 8rpx;
    font-size: 22rpx;
    color: #7c8ba1;
  }
}

.order-items {
  .item {
    display: flex;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    .item-img {
      width: 100rpx;
      height: 100rpx;
      border-radius: 12rpx;
      margin-right: 20rpx;
      background-color: #f5f5f5;

      &.default-item-img {
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .item-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .item-top {
        display: flex;
        justify-content: space-between;
        margin-bottom: 10rpx;

        .item-name {
          font-size: 28rpx;
          color: #333;
          font-weight: 500;
        }

        .item-price {
          font-size: 28rpx;
          font-weight: bold;
          color: #333;
        }
      }

      .item-bottom {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .item-qty-block {
          display: flex;
          flex-direction: column;
          gap: 8rpx;
          min-width: 0;
        }

        .item-qty {
          font-size: 24rpx;
          color: #999;
        }

        .item-options {
          font-size: 22rpx;
          color: #2a8bff;
        }

        .item-subtotal {
          font-size: 24rpx;
          color: #666;
        }
      }
    }
  }

  .total {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 30rpx;
    font-size: 32rpx;
    font-weight: bold;

    .price {
      color: #ff7b2c;
      font-size: 40rpx;
    }
  }
}

.order-items__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 12rpx;
  padding: 18rpx 0 6rpx;
}

.order-items__toggle-text {
  font-size: 24rpx;
  color: #2a8bff;
  font-weight: 500;
}

.order-items__toggle-icon {
  transition: transform 0.2s ease;

  &.open {
    transform: rotate(180deg);
  }
}

.weight-order-tip {
  margin-top: 20rpx;
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  padding: 16rpx 18rpx;
  border-radius: 14rpx;
  background: #eef7ff;
}

.weight-order-tip__text {
  flex: 1;
  font-size: 24rpx;
  line-height: 1.5;
  color: #2f5f9e;
}

.remark {
  .remark-row {
    width: 100%;
  }

  .remark-field {
    position: relative;
    width: 100%;
    height: 72rpx;
    background-color: #fff;
    border: 2rpx solid #eee;
    border-radius: 12rpx;
    box-sizing: border-box;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &.remark-field--focused {
      border-color: #2a8bff;
      box-shadow: 0 0 0 4rpx rgba(42, 139, 255, 0.08);
    }
  }

  .taste-sensitive-tip {
    margin-top: 16rpx;
    display: flex;
    align-items: flex-start;
    gap: 10rpx;
    padding: 14rpx 16rpx;
    border-radius: 14rpx;
    background: #fff7ed;
  }

  .taste-sensitive-tip__text {
    flex: 1;
    font-size: 24rpx;
    line-height: 1.5;
    color: #c46a1d;
  }

  .remark-input {
    width: 100%;
    height: 72rpx;
    background-color: transparent;
    padding: 0 236rpx 0 20rpx;
    font-size: 26rpx;
    line-height: 72rpx;
    box-sizing: border-box;
    border: none;
  }

  .remark-input__placeholder {
    color: #999;
  }

  .remark-quick-list {
    position: absolute;
    top: 50%;
    right: 12rpx;
    transform: translateY(-50%);
    display: flex;
    align-items: center;
    gap: 10rpx;
  }

  .remark-quick-item {
    min-width: 62rpx;
    height: 52rpx;
    padding: 0 14rpx;
    border-radius: 26rpx;
    border: 2rpx solid #dbeafe;
    background: #f8fbff;
    color: #5b6b82;
    font-size: 22rpx;
    line-height: 48rpx;
    text-align: center;
    box-sizing: border-box;
    transition: all 0.2s ease;

    &.active {
      background: #e6f2ff;
      border-color: #2a8bff;
      color: #2a8bff;
      font-weight: 600;
    }
  }

  .picker-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 0;

    .time-val {
      font-size: 28rpx;
      color: #333;

      &.placeholder {
        color: #999;
      }
    }
  }

  .pack-selector {
    display: flex;
    justify-content: space-between;
    gap: 20rpx;
    padding: 10rpx 0;

    .pack-item {
      flex: 1;
      height: 80rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f8f9fa;
      border-radius: 12rpx;
      border: 2rpx solid transparent;
      font-size: 28rpx;
      color: #666;
      gap: 10rpx;
      transition: all 0.2s;

      &.active {
        background-color: #e6f7ff;
        color: #2a8bff;
        border-color: #2a8bff;
        font-weight: bold;
      }
    }
  }
}

.integrity-tip {
  background-color: #f9fbff;
  border: 1rpx solid #e6f0ff;
  border-radius: 16rpx;
  padding: 30rpx;

  .tip-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .tip-title {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
      margin-left: 12rpx;
    }
  }

  .tip-content {
    .tip-text {
      font-size: 26rpx;
      color: #666;
      line-height: 1.5;
    }
  }
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 140rpx;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  z-index: 100;

  .price-box {
    display: flex;
    align-items: baseline;

    .label {
      font-size: 28rpx;
      color: #333;
      margin-right: 10rpx;
    }

    .amount {
      font-size: 44rpx;
      color: #ff7b2c;
      font-weight: bold;
    }

    .weight-text {
      margin-left: 14rpx;
      font-size: 24rpx;
      color: #64748b;
      font-weight: 500;
    }
  }

  .submit-box {
    display: flex;
    flex-direction: column;
    align-items: center;

    .submit-tip {
      font-size: 20rpx;
      color: #999;
      margin-bottom: 6rpx;
    }

    .btn-pay {
      width: 280rpx;
      height: 88rpx;
      line-height: 88rpx;
      text-align: center;
      background-color: #ff7b2c;
      color: #fff;
      border-radius: 44rpx;
      font-size: 32rpx;
      font-weight: bold;
      box-shadow: 0 4rpx 12rpx rgba(255, 123, 44, 0.3);

      &.btn-pay--disabled {
        opacity: 0.72;
      }

      &:active {
        transform: scale(0.98);
      }
    }
  }
}

.penalty-dialog-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  background: rgba(0, 0, 0, 0.45);
  z-index: 999;
}

.penalty-dialog {
  width: 100%;
  max-width: 620rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 32rpx 28rpx;
  box-sizing: border-box;

  &__title {
    font-size: 34rpx;
    font-weight: bold;
    color: #333;
    text-align: center;
  }

  &__content {
    margin-top: 24rpx;
    font-size: 28rpx;
    line-height: 42rpx;
    color: #555;
    white-space: pre-wrap;
  }

  &__tip {
    margin-top: 18rpx;
    font-size: 24rpx;
    line-height: 36rpx;
    color: #999;
  }

  &__actions {
    margin-top: 32rpx;
  }

  &__button {
    min-height: 88rpx;
    padding: 0 24rpx;
    border-radius: 44rpx;
    background: #2a8bff;
    color: #fff;
    font-size: 28rpx;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    word-break: break-all;
  }
}
</style>
