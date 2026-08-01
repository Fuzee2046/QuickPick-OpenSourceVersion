<template>
  <view class="free-meal-page">
    <view class="hero-card">
      <view class="hero-title-row">
        <uni-icons type="fire-filled" size="24" color="#2a8bff"></uni-icons>
        <text class="hero-title">{{ info.activityName || '请你吃餐饭' }}</text>
      </view>
      <text class="hero-subtitle">每日预约抽奖 · 准点开奖 · 红包免单</text>
      <view class="meta-grid">
        <view class="meta-item">
          <text class="meta-label">预约时间</text>
          <text class="meta-value"
            >{{ info.reserveStartTime || '--:--:--' }} -
            {{ info.reserveEndTime || '--:--:--' }}</text
          >
        </view>
        <view class="meta-item">
          <text class="meta-label">开奖时间</text>
          <text class="meta-value">{{ info.drawTime || '--:--:--' }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">今日奖品</text>
          <text class="meta-value">免单</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">中奖名额</text>
          <text class="meta-value">{{ info.maxWinnersPerDay || 0 }}人</text>
        </view>
      </view>
    </view>

    <view
      v-if="!info.isWinner && info.isYesterdayWinner && info.yesterdayWinnerInfo"
      class="yesterday-winner-card"
    >
      <view class="yesterday-badge">昨日中奖</view>
      <text class="yesterday-title">你昨日中奖啦</text>
      <text class="yesterday-tip">你昨日抽中免单，请尽快添加账号领取</text>
      <view class="yesterday-wechat-row">
        <text class="wechat-id">{{ info.yesterdayWinnerInfo.redeemWechat || '暂未配置' }}</text>
        <view class="copy-btn" @tap="copyYesterdayWechat">复制</view>
      </view>
    </view>

    <view class="reserve-card">
      <view class="dot-wall-wrapper">
        <view class="dot-wall">
          <view
            v-for="(dot, idx) in dotList"
            :key="dot.id || idx"
            class="dot-item"
          >
            <view
              class="dot"
              :class="{ 'dot-winner': dot.isWinner, 'dot-mine': dot.isMine }"
              :style="{ backgroundColor: dot.color }"
            >
              <text v-if="dot.isWinner" class="dot-crown">👑</text>
            </view>
            <view v-if="winnerBubbleVisible && dot.showBubble" class="winner-bubble">我中奖了！</view>
          </view>
          <view v-if="dotList.length === 0" class="dot-empty">今天还没人预约，快来抢第一位</view>
        </view>
        <text class="reserve-count">已预约 {{ info.reservationCount || 0 }} 人</text>
        <text v-if="hiddenDotCount > 0" class="reserve-count-note">
          已简化展示前 {{ MAX_VISIBLE_DOTS }} 位，剩余 {{ hiddenDotCount }} 人以数字统计
        </text>
      </view>

      <view class="reserve-btn-wrapper">
        <view
          class="reserve-btn-circle"
          :class="{ disabled: !info.canReserve || info.userReserved || reserving }"
          @tap="reserveNow"
        >
          <text class="reserve-btn-text">{{ reserveButtonText }}</text>
        </view>
      </view>

      <text class="phase-text">{{ phaseText }}</text>
    </view>

    <view v-if="info.isWinner && info.myWinnerInfo" class="winner-card">
      <view class="winner-header">
        <uni-icons type="gift-filled" size="24" color="#FFD700"></uni-icons>
        <text class="winner-title">恭喜你中奖了！</text>
      </view>
      <text class="winner-prize">获得红包免单</text>
      <view class="winner-wechat-box">
        <text class="winner-tip">请添加兑奖账号：</text>
        <view class="wechat-copy-row">
          <text class="wechat-id">{{ info.myWinnerInfo.redeemWechat || '暂未配置' }}</text>
          <view class="copy-btn" @tap="copyWechat">复制</view>
        </view>
      </view>
    </view>

    <view v-else-if="info.drawn && info.userReserved" class="notice-card">
      <uni-icons
        type="clock-filled"
        size="28"
        color="#1890ff"
        style="margin-bottom: 12rpx"
      ></uni-icons>
      <text class="notice-title">今日开奖已完成</text>
      <text class="notice-tip">很遗憾这次未中奖，明天继续预约试试手气吧</text>
      <text class="notice-encourage">每天都有新机会，坚持参与好运自然来！</text>
    </view>

    <!-- 占位，让内容不被底部说明和TabBar遮挡 -->
    <view class="content-spacer"></view>

    <view class="rule-section">
      <text class="rule-title">—— 活动说明 ——</text>
      <text class="rule-content">{{ info.description || defaultRule }}</text>
    </view>

    <CustomTabBar :selected="1" />

  </view>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import CustomTabBar from '@/components/CustomTabBar.vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { http } from '@/utils/http'
import { syncNativeTabBarHidden } from '@/utils/navigation'
import { usePageShare } from '@/utils/share'
import { smartRequestAllSubscribeMessages } from '@/utils/subscribe'

const userStore = useUserStore()
const MAX_VISIBLE_DOTS = 80
const reserving = ref(false)
const info = ref<any>({})
const winnerBubbleVisible = ref(false)
let winnerBubbleTimer: ReturnType<typeof setTimeout> | null = null

usePageShare(() => ({
  title: info.value?.activityName || 'QuickPick 免单活动，预约抽奖赢免单',
  path: '/pages/free-meal/free-meal',
}))

const colorPool = ['#ff6b35', '#f59e0b', '#10b981', '#2a8bff', '#8b5cf6', '#ef4444', '#06b6d4']

const defaultRule =
  '用户在预约时段点击预约参与当天抽奖，系统在开奖时间随机抽取中奖用户发放现金红包。'

const dotList = computed(() => {
  const reservationDots = Array.isArray(info.value?.reservationDots) ? info.value.reservationDots : []

  if (reservationDots.length > 0) {
    return reservationDots.slice(0, MAX_VISIBLE_DOTS).map((dot: any, idx: number) => ({
      id: dot.id,
      color: colorPool[idx % colorPool.length],
      isWinner: Boolean(dot.isWinner),
      isMine: Boolean(dot.isMine),
      showBubble: Boolean(dot.showBubble),
    }))
  }

  const count = Number(info.value?.reservationCount || 0)
  return Array.from({ length: Math.min(count, MAX_VISIBLE_DOTS) }).map((_, idx) => ({
    id: `mock-${idx}`,
    color: colorPool[idx % colorPool.length],
    isWinner: false,
    isMine: false,
    showBubble: false,
  }))
})

const hiddenDotCount = computed(() => {
  const reservationDotsLength = Array.isArray(info.value?.reservationDots)
    ? info.value.reservationDots.length
    : 0
  if (reservationDotsLength > 0) {
    return Math.max(0, reservationDotsLength - MAX_VISIBLE_DOTS)
  }
  return Math.max(0, Number(info.value?.reservationCount || 0) - MAX_VISIBLE_DOTS)
})

const updateWinnerBubbleState = () => {
  if (winnerBubbleTimer) {
    clearTimeout(winnerBubbleTimer)
    winnerBubbleTimer = null
  }

  winnerBubbleVisible.value = false
  const hasBubbleWinner = dotList.value.some((dot: any) => dot.showBubble)
  if (!hasBubbleWinner) return

  winnerBubbleVisible.value = true
  winnerBubbleTimer = setTimeout(() => {
    winnerBubbleVisible.value = false
  }, 2600)
}

const reserveButtonText = computed(() => {
  if (reserving.value) return '预约中...'
  if (info.value?.userReserved) return '今日已预约'
  if (!info.value?.canReserve) return '当前不可预约'
  return '预约抽奖'
})

const phaseText = computed(() => {
  const phase = info.value?.phase
  if (phase === 'not_started') return '活动尚未开始，请在预约时间内参与'
  if (phase === 'reserving') return '预约进行中，点击按钮参与今日抽奖'
  if (phase === 'waiting_draw') return '预约已截止，等待开奖中'
  if (phase === 'drawn') return '今日已开奖，可继续关注明日活动'
  return '活动状态同步中'
})

const formatPrice = (value: any) => {
  if (value === null || value === undefined || value === '') return '0.00'
  return Number(value).toFixed(2)
}

const fetchInfo = async () => {
  try {
    const res = await http<any>({
      url: '/api/client/lucky-draw/info',
      method: 'GET',
      hideErrorToast: true,
      cacheTtlMs: 15 * 1000,
      cacheKey: 'lucky-draw-info',
    })
    info.value = res.data || {}
    updateWinnerBubbleState()
  } catch (e: any) {
    info.value = {}
  }
}

const reserveNow = async () => {
  if (!userStore.isLogin) {
    safeNavigateTo({ url: '/pages/login/login' })
    return
  }
  if (!info.value?.canReserve || info.value?.userReserved || reserving.value) {
    return
  }
  reserving.value = true
  try {
    const res = await http<string>({
      url: '/api/client/lucky-draw/reserve',
      method: 'POST',
    })
    uni.showToast({ icon: 'success', title: res.data || '预约成功' })
    await fetchInfo()

    // 预约成功后，仅请求中奖通知模板，避免和订单提醒互相影响
    try {
      const authResult = await smartRequestAllSubscribeMessages(false, ['luckyDraw'])
      void authResult
    } catch (authErr) {
      console.error('订阅消息授权过程异常:', authErr)
      // 授权失败不影响预约结果，继续执行
    }
  } finally {
    reserving.value = false
  }
}

const copyWechat = () => {
  if (info.value?.myWinnerInfo?.redeemWechat) {
    uni.setClipboardData({
      data: info.value.myWinnerInfo.redeemWechat,
      success: () => {
        uni.showToast({ title: '已复制', icon: 'success' })
      },
    })
  }
}

const copyYesterdayWechat = () => {
  if (info.value?.yesterdayWinnerInfo?.redeemWechat) {
    uni.setClipboardData({
      data: info.value.yesterdayWinnerInfo.redeemWechat,
      success: () => {
        uni.showToast({ title: '已复制', icon: 'success' })
      },
    })
  }
}

onShow(() => {
  syncNativeTabBarHidden()
  fetchInfo()
})

onBeforeUnmount(() => {
  if (winnerBubbleTimer) {
    clearTimeout(winnerBubbleTimer)
  }
})
</script>

<style lang="scss">
.free-meal-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e6f7ff 0%, #f5f7fa 45%);
  padding: 60rpx 24rpx 240rpx 24rpx;
  box-sizing: border-box;
  position: relative;
}

.hero-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(42, 139, 255, 0.08);
}

.hero-title-row {
  display: flex;
  align-items: center;
}

.hero-title {
  margin-left: 10rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #1f2937;
}

.hero-subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
}

.meta-grid {
  margin-top: 22rpx;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.meta-item {
  background: #f0f7ff;
  border-radius: 16rpx;
  padding: 16rpx;
}

.meta-label {
  display: block;
  font-size: 22rpx;
  color: #6b7280;
}

.meta-value {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #2a8bff;
  font-weight: 600;
}

.reserve-card {
  margin-top: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(42, 139, 255, 0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.yesterday-winner-card {
  margin-top: 20rpx;
  background: linear-gradient(135deg, #fff8e1 0%, #ffefbf 100%);
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 6rpx 18rpx rgba(255, 184, 0, 0.18);
}

.yesterday-badge {
  display: inline-block;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: #ffb800;
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
}

.yesterday-title {
  display: block;
  margin-top: 10rpx;
  font-size: 32rpx;
  color: #8b5a00;
  font-weight: 800;
}

.yesterday-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8b5a00;
}

.yesterday-wechat-row {
  margin-top: 14rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff7dc;
  border-radius: 12rpx;
  padding: 12rpx 18rpx;
}

.dot-wall-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
}

.dot-wall {
  min-height: 86rpx;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 18rpx 16rpx;
  margin-bottom: 16rpx;
}

.dot-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34rpx;
  height: 34rpx;
}

.dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
  position: relative;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border 0.25s ease;
}

.dot-winner {
  transform: scale(1.32);
  box-shadow:
    0 0 0 8rpx rgba(255, 215, 0, 0.34),
    0 0 0 14rpx rgba(255, 184, 0, 0.18),
    0 10rpx 22rpx rgba(255, 184, 0, 0.42);
  border: 3rpx solid rgba(255, 255, 255, 0.98);
  animation: winnerPulse 1.8s ease-in-out infinite;
}

.dot-mine {
  box-shadow:
    0 0 0 6rpx rgba(42, 139, 255, 0.3),
    0 0 0 11rpx rgba(42, 139, 255, 0.12),
    0 6rpx 16rpx rgba(42, 139, 255, 0.24);
  border: 3rpx solid rgba(255, 255, 255, 0.98);
  transform: scale(1.18);
}

.dot-mine.dot-winner {
  box-shadow:
    0 0 0 8rpx rgba(255, 215, 0, 0.34),
    0 0 0 14rpx rgba(255, 184, 0, 0.18),
    0 10rpx 22rpx rgba(255, 184, 0, 0.42);
  transform: scale(1.32);
}

.dot-crown {
  position: absolute;
  top: -22rpx;
  left: 50%;
  transform: translateX(-50%);
  font-size: 18rpx;
  line-height: 1;
}

.winner-bubble {
  position: absolute;
  left: 50%;
  top: -62rpx;
  transform: translateX(-50%);
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #f59e0b;
  font-size: 18rpx;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 8rpx 20rpx rgba(245, 158, 11, 0.18);
  border: 2rpx solid rgba(255, 215, 0, 0.45);
  animation: bubblePop 2.6s ease forwards;
}

.winner-bubble::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -10rpx;
  width: 16rpx;
  height: 16rpx;
  background: #ffffff;
  border-right: 2rpx solid rgba(255, 215, 0, 0.45);
  border-bottom: 2rpx solid rgba(255, 215, 0, 0.45);
  transform: translateX(-50%) rotate(45deg);
}

.dot-empty {
  font-size: 24rpx;
  color: #9ca3af;
  margin-top: 20rpx;
}

.reserve-count {
  font-size: 24rpx;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4rpx 20rpx;
  border-radius: 20rpx;
}

.reserve-count-note {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #94a3b8;
  text-align: center;
}

.reserve-btn-wrapper {
  margin-bottom: 30rpx;
}

.reserve-btn-circle {
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 16rpx 32rpx rgba(42, 139, 255, 0.3);
  transition: all 0.2s ease;
}

.reserve-btn-circle:active {
  transform: scale(0.95);
  box-shadow: 0 8rpx 16rpx rgba(42, 139, 255, 0.3);
}

.reserve-btn-circle.disabled {
  background: #d1d5db;
  box-shadow: none;
  pointer-events: none;
}

.reserve-btn-text {
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
  text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.phase-text {
  font-size: 24rpx;
  color: #6b7280;
  text-align: center;
}

.winner-card {
  margin-top: 20rpx;
  background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 165, 0, 0.3);
}

.winner-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 12rpx;
}

.winner-title {
  margin-left: 10rpx;
  font-size: 34rpx;
  color: #8b4513;
  font-weight: 800;
}

.winner-prize {
  display: block;
  font-size: 30rpx;
  color: #8b4513;
  font-weight: 700;
  margin-bottom: 20rpx;
}

.winner-wechat-box {
  background: rgba(255, 251, 235, 0.9);
  border-radius: 16rpx;
  padding: 20rpx;
}

.winner-tip {
  display: block;
  font-size: 24rpx;
  color: #8b4513;
  margin-bottom: 10rpx;
}

.wechat-copy-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff8dc;
  padding: 12rpx 24rpx;
  border-radius: 12rpx;
}

.wechat-id {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}

.copy-btn {
  font-size: 24rpx;
  color: #8b4513;
  background: #ffebcd;
  padding: 6rpx 24rpx;
  border-radius: 30rpx;
  font-weight: 500;
}

.notice-card {
  margin-top: 20rpx;
  background: #f0f7ff;
  border-radius: 24rpx;
  padding: 30rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 2rpx dashed #bae0ff;
}

.notice-title {
  display: block;
  font-size: 30rpx;
  color: #1890ff;
  font-weight: 700;
}

.notice-tip {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #69c0ff;
  text-align: center;
}

.notice-encourage {
  display: block;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: #91d5ff;
  font-style: italic;
  text-align: center;
}

.content-spacer {
  flex: 1;
  min-height: 40rpx;
}

.rule-section {
  position: absolute;
  bottom: 200rpx;
  left: 0;
  right: 0;
  padding: 0 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rule-title {
  display: block;
  font-size: 24rpx;
  color: #9ca3af;
  font-weight: 500;
  margin-bottom: 12rpx;
}

.rule-content {
  display: block;
  font-size: 22rpx;
  line-height: 1.6;
  color: #9ca3af;
  text-align: center;
}

@keyframes winnerPulse {
  0%,
  100% {
    transform: scale(1.16);
  }
  50% {
    transform: scale(1.28);
  }
}

@keyframes bubblePop {
  0% {
    opacity: 0;
    transform: translateX(-50%) translateY(10rpx) scale(0.88);
  }
  15%,
  80% {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
  100% {
    opacity: 0;
    transform: translateX(-50%) translateY(-10rpx) scale(0.94);
  }
}
</style>
