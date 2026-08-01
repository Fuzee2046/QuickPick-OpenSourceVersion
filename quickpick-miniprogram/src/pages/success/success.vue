<template>
  <view class="success-container">
    <view class="status-icon">
      <uni-icons type="checkbox-filled" size="100" color="#07c160"></uni-icons>
    </view>
    <text class="success-tip">下单成功</text>
    
    <view class="pickup-card">
      <text class="label">您的取餐号是</text>
      <text class="code">{{ pickupCode }}</text>
      <text class="instruction">请前往【{{ shopName || '店铺' }}】凭取餐号取餐</text>
    </view>

    <view v-if="tasteSensitiveEnabled" class="taste-sensitive-tip">
      <view class="taste-sensitive-tip__icon">
        <uni-icons type="notification-filled" size="17" color="#f08c2e"></uni-icons>
      </view>
      <view class="taste-sensitive-tip__content">
        <text class="taste-sensitive-tip__title">即时口感提醒</text>
        <text class="taste-sensitive-tip__text">该类型菜品建议尽快取餐，超时取餐可能会影响口感。</text>
      </view>
    </view>

    <view class="integrity-battle-card">
      <view v-if="showBattleHeader" class="battle-header">
        <view class="battle-badge battle-badge-win">
          小准完胜
        </view>
      </view>

      <view class="battle-camp-row">
        <view class="battle-camp battle-camp--integrity">
          <view class="battle-avatar battle-avatar--integrity">
            <view class="mini-character mini-character--integrity">
              <view class="mini-character-head"></view>
              <view class="mini-character-body"></view>
            </view>
          </view>
          <view class="battle-camp-meta">
            <text class="battle-camp-name">小准</text>
          </view>
        </view>

        <view class="battle-vs">
          <text class="battle-vs-text">VS</text>
        </view>

        <view class="battle-camp battle-camp--noshow">
          <view class="battle-camp-meta battle-camp-meta--right">
            <view v-if="showPenaltyTag" class="battle-penalty-tag">
              已处理
            </view>
            <text class="battle-camp-name">小鸽</text>
            <text class="battle-camp-count">{{ integrityStats.noShowCount }} 单</text>
          </view>
          <view class="battle-avatar battle-avatar--noshow">
            <view class="mini-character mini-character--noshow">
              <view class="mini-character-head"></view>
              <view class="mini-character-body"></view>
            </view>
          </view>
        </view>
      </view>

      <view class="battle-track">
        <view class="battle-side battle-side--integrity" :style="{ width: `${animatedIntegrityRate}%` }"></view>
        <view
          v-if="integrityStats.noShowRate > 0"
          class="battle-side battle-side--noshow"
          :style="{ width: `${animatedNoShowRate}%` }"
        ></view>
      </view>
    </view>
    
    <!-- 诚信提示 -->
    <view class="integrity-tip-success">
      <uni-icons type="heart-filled" size="20" color="#ff7b2c"></uni-icons>
      <text class="tip-text">下单即承诺，取餐显诚信。请按时取餐，谢谢！</text>
    </view>
    
    <!-- <view class="order-info">
      <text>订单编号：{{ orderId }}</text>
    </view> -->
    
    <view class="btn-group">
      <view class="btn btn-primary" @tap="goToOrders">查看订单</view>
      <view class="btn btn-outline" @tap="goToHome">返回首页</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { http } from '@/utils/http' // If needed for shop name
import { safeSwitchTab } from '@/utils/navigation'

const orderId = ref('')
const pickupCode = ref('')
const shopName = ref('') // We might need to fetch order details to get shop name, or pass it. 
const tasteSensitiveEnabled = ref(false)
const animatedIntegrityRate = ref(0)
const animatedNoShowRate = ref(0)
let statsLoadTimer: ReturnType<typeof setTimeout> | null = null
let statsAnimationTimer: ReturnType<typeof setTimeout> | null = null

type DailyIntegrityStats = {
  integrityCount: number
  noShowCount: number
  totalCount: number
  integrityRate: number
  noShowRate: number
  highlightText: string
}

const integrityStats = ref<DailyIntegrityStats>({
  integrityCount: 0,
  noShowCount: 0,
  totalCount: 0,
  integrityRate: 100,
  noShowRate: 0,
  highlightText: '今日诚信完胜，继续保持'
})

const showBattleHeader = computed(() => integrityStats.value.noShowCount === 0)
const showPenaltyTag = computed(() => integrityStats.value.noShowCount > 0)
// For now, let's just leave it generic or fetch if possible. 
// Since we don't have an endpoint handy in the prompt to get shop name by order ID easily without looking at backend, I'll assume generic or user context.
// Actually, let's try to fetch order details if we have the ID.

onLoad(async (options) => {
  orderId.value = options.orderId
  pickupCode.value = options.pickupCode
  shopName.value = decodeURIComponent(options.shopName || '')
  tasteSensitiveEnabled.value = String(options.tasteSensitiveEnabled || '0') === '1'

  if (orderId.value && !shopName.value) {
    fetchOrderDetail()
  }
  statsLoadTimer = setTimeout(() => {
    fetchDailyIntegrityStats()
    statsLoadTimer = null
  }, 120)
})

const fetchOrderDetail = async () => {
    try {
        const res = await http<any>({
            url: `/api/client/orders/${orderId.value}`,
            method: 'GET'
        })
        shopName.value = res.data.shopName || res.data.shop?.name || ''
        tasteSensitiveEnabled.value = !!(res.data.tasteSensitiveEnabled || res.data.shop?.tasteSensitiveEnabled)
    } catch (e) {
        console.error(e)
    }
}

const fetchDailyIntegrityStats = async () => {
  try {
    const res = await http<DailyIntegrityStats>({
      url: '/api/client/orders/daily-integrity-stats',
      method: 'GET',
      cacheTtlMs: 30 * 1000,
      cacheKey: 'daily-integrity-stats'
    })
    integrityStats.value = res.data

    // 轻量过渡动画，营造对抗条从 0 增长到目标值的效果
    if (statsAnimationTimer) {
      clearTimeout(statsAnimationTimer)
    }
    statsAnimationTimer = setTimeout(() => {
      animatedIntegrityRate.value = res.data.integrityRate
      animatedNoShowRate.value = res.data.noShowRate
      statsAnimationTimer = null
    }, 80)
  } catch (e) {
    console.error(e)
  }
}

const goToOrders = () => {
  safeSwitchTab({ url: '/pages/order-list/order-list' })
}

const goToHome = () => {
  safeSwitchTab({ url: '/pages/index/index' })
}

onUnmounted(() => {
  if (statsLoadTimer) {
    clearTimeout(statsLoadTimer)
  }
  if (statsAnimationTimer) {
    clearTimeout(statsAnimationTimer)
  }
})
</script>

<style lang="scss">
.success-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;
  background-color: #fff;
  min-height: 100vh;
}

.status-icon {
  margin-bottom: 20rpx;
}

.success-tip {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 60rpx;
}

.pickup-card {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 60rpx 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 80rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.08);
  width: 80%;
  
  .label {
    font-size: 28rpx;
    color: #666;
    margin-bottom: 24rpx;
  }
  
  .code {
    font-size: 100rpx;
    font-weight: bold;
    color: #2a8bff; // 主色
    line-height: 1;
    margin-bottom: 30rpx;
    text-shadow: 2rpx 2rpx 0rpx rgba(42, 139, 255, 0.1);
  }
  
  .instruction {
    font-size: 24rpx;
    color: #999;
    text-align: center;
  }
}

.integrity-battle-card {
  width: 80%;
  margin: -12rpx 0 34rpx;
}

.taste-sensitive-tip {
  width: 80%;
  margin-bottom: 28rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 22rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #fff7ed 0%, #fff1dd 100%);
  border: 1rpx solid rgba(240, 140, 46, 0.14);
  box-shadow: 0 10rpx 26rpx rgba(240, 140, 46, 0.08);
}

.taste-sensitive-tip__icon {
  flex-shrink: 0;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  background: rgba(240, 140, 46, 0.12);
}

.taste-sensitive-tip__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.taste-sensitive-tip__title {
  font-size: 24rpx;
  font-weight: 700;
  color: #b76114;
}

.taste-sensitive-tip__text {
  flex: 1;
  font-size: 24rpx;
  line-height: 1.5;
  color: #c46a1d;
}

.battle-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 14rpx;
}

.battle-subtitle {
  flex: 1;
  font-size: 23rpx;
  color: #718097;
}

.battle-badge {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 123, 123, 0.12);
  color: #df6a6a;
  font-size: 22rpx;
  font-weight: 700;
}

.battle-badge-win {
  background: rgba(18, 184, 134, 0.14);
  color: #11926c;
}

.battle-camp-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 10rpx;
}

.battle-camp {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.battle-camp--noshow {
  justify-content: flex-end;
}

.battle-avatar {
  width: 42rpx;
  height: 42rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 10rpx rgba(15, 23, 42, 0.06);
}

.battle-avatar--integrity {
  background: linear-gradient(135deg, #7cdb68 0%, #52c7ff 48%, #7a8cff 100%);
}

.battle-avatar--noshow {
  background: linear-gradient(135deg, #ffca7a 0%, #ff8d8d 100%);
}

.mini-character {
  position: relative;
  width: 22rpx;
  height: 24rpx;
}

.mini-character-head {
  position: absolute;
  top: 0;
  left: 50%;
  width: 11rpx;
  height: 11rpx;
  margin-left: -5.5rpx;
  border-radius: 50%;
  background: #fff;
}

.mini-character-body {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 16rpx;
  height: 12rpx;
  margin-left: -8rpx;
  border-radius: 9rpx 9rpx 6rpx 6rpx;
  background: rgba(255, 255, 255, 0.92);
}

.mini-character--integrity {
  transform: rotate(-6deg);
}

.mini-character--noshow {
  transform: rotate(6deg);
}

.battle-camp-meta {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.battle-camp-meta--right {
  align-items: flex-end;
  gap: 6rpx;
}

.battle-camp-name {
  font-size: 24rpx;
  font-weight: 700;
  color: #314056;
}

.battle-camp-count {
  font-size: 22rpx;
  color: #748195;
}

.battle-penalty-tag {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, rgba(255, 143, 143, 0.14) 0%, rgba(255, 181, 124, 0.18) 100%);
  border: 1rpx solid rgba(231, 120, 96, 0.16);
  color: #cc6752;
  font-size: 19rpx;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: 0.5rpx;
  box-shadow: 0 2rpx 8rpx rgba(231, 120, 96, 0.08);
}

.battle-vs {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 34rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #fff4ca 0%, #ffe1ee 100%);
  border: 2rpx solid rgba(255, 171, 91, 0.35);
  box-shadow: 0 4rpx 10rpx rgba(255, 171, 91, 0.14);
  transform: rotate(-6deg);
}

.battle-vs-text {
  font-size: 20rpx;
  font-weight: 900;
  color: #ff8b61;
  letter-spacing: 1rpx;
}

.battle-track {
  position: relative;
  display: flex;
  width: 100%;
  height: 14rpx;
  overflow: hidden;
  border-radius: 999rpx;
  background:
    linear-gradient(180deg, rgba(255,255,255,0.75) 0%, rgba(255,255,255,0) 100%),
    #eff3f8;
  box-shadow: inset 0 1rpx 4rpx rgba(15, 23, 42, 0.05);
}

.battle-side {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0;
  box-sizing: border-box;
  transition: width 0.8s ease;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    width: 48rpx;
    background: linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.42) 50%, rgba(255,255,255,0) 100%);
    animation: battleSweep 2.8s ease-in-out infinite;
  }
}

.battle-side--integrity {
  min-width: 120rpx;
  background: linear-gradient(
    90deg,
    #90ea5b 0%,
    #52df93 18%,
    #4fdfe3 36%,
    #49b8ff 54%,
    #6f8cff 72%,
    #b06dff 88%,
    #ff79c6 100%
  );
}

.battle-side--noshow {
  min-width: 0;
  background: linear-gradient(90deg, #ffd18b 0%, #ffb27d 34%, #ff907e 68%, #ff7da6 100%);

  &::after {
    animation-delay: 0.5s;
  }
}

.order-info {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 60rpx;
}

.btn-group {
  width: 100%;
  padding: 0 60rpx;
  display: flex;
  justify-content: space-between;
  
  .btn {
    flex: 1;
    height: 88rpx;
    line-height: 88rpx;
    text-align: center;
    border-radius: 44rpx;
    font-size: 30rpx;
    font-weight: bold;
    
    &.btn-primary {
      background-color: #2a8bff;
      color: #fff;
      margin-right: 30rpx;
      box-shadow: 0 4rpx 12rpx rgba(42, 139, 255, 0.3);
    }
    
    &.btn-outline {
      background-color: #fff;
      color: #2a8bff;
      border: 2rpx solid #2a8bff;
      margin-left: 0; // Reset
    }
    
    &:active {
      transform: scale(0.98);
    }
  }
}

.integrity-tip-success {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 30rpx 0 40rpx;
  padding: 24rpx 30rpx;
  background-color: #fff8e6;
  border-radius: 16rpx;
  border: 1rpx solid #ffd166;
  width: 80%;
  
  .tip-text {
    font-size: 26rpx;
    color: #ff7b2c;
    margin-left: 12rpx;
  }
}

@keyframes battleSweep {
  0% {
    transform: translateX(-120rpx);
    opacity: 0;
  }
  25% {
    opacity: 1;
  }
  75% {
    opacity: 1;
  }
  100% {
    transform: translateX(420rpx);
    opacity: 0;
  }
}
</style>
