<template>
  <view class="order-list-container">
    <!-- Top Navigation Tabs -->
    <view class="tabs-wrapper">
      <view class="tabs">
        <view 
          v-for="(tab, index) in tabs" 
          :key="index" 
          class="tab-item"
          :class="{ active: currentTab === tab.value }"
          @tap="switchTab(tab.value)"
        >
          <text>{{ tab.label }}</text>
        </view>
      </view>
    </view>

    <!-- 诚信提示 -->
    <view class="integrity-banner">
      <uni-icons type="info-circle-filled" size="18" color="#2a8bff"></uni-icons>
      <text class="banner-text">下单即承诺，取餐显诚信。请按时取餐，避免浪费。</text>
    </view>

    <!-- Empty State -->
    <view v-if="filteredOrders.length === 0" class="empty">
      <uni-icons type="cart" size="100" color="#ddd" class="empty-icon"></uni-icons>
      <text class="empty-text">暂无订单</text>
      <text class="empty-sub">快去点一份美味吧</text>
      <button class="go-btn" @tap="goToMenu">去点单</button>
    </view>
    
    <!-- Order List -->
    <view v-else class="list">
      <OrderCard 
        v-for="order in filteredOrders" 
        :key="order.id" 
        :order="order"
        @click="goToDetail"
        @detail="goToDetail"
        @contact="onContact"
        @reorder="onReorder"
      />
    </view>
    <CustomTabBar :selected="2" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { http } from '@/utils/http'
import { safeNavigateTo, safeSwitchTab, syncNativeTabBarHidden } from '@/utils/navigation'
import { useUserStore } from '@/stores/user'
import { usePageShare } from '@/utils/share'
import OrderCard from '@/components/OrderCard.vue'
import CustomTabBar from '@/components/CustomTabBar.vue'

const userStore = useUserStore()
const orders = ref<any[]>([])
const currentTab = ref('processing')
const ORDER_LIST_CACHE_MS = 20000
const lastFetchTime = ref(0)

usePageShare({
  title: '食刻快取｜校园食堂提前点单小程序',
  path: '/pages/order-list/order-list',
})

const tabs = [
  { label: '进行中', value: 'processing' },
  { label: '全部订单', value: 'all' }
]

const buildAmountRangeText = (amount: any) => {
  const estimatedAmount = Number(amount || 0)
  if (!estimatedAmount) return '¥0-0'
  const spread = estimatedAmount < 15
    ? 2
    : estimatedAmount < 25
      ? 3
      : Math.max(3, estimatedAmount * 0.12)
  const min = Math.max(0, Math.floor(estimatedAmount - spread))
  const max = Math.ceil(estimatedAmount + spread)
  return `¥${min}-${max}`
}

const formatFinalAmountText = (amount: any) => {
  const value = Number(amount || 0)
  return `¥${value % 1 === 0 ? value.toFixed(0) : value.toFixed(2)}`
}

const formatOrderTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const normalizeOrder = (order: any) => {
  const items = Array.isArray(order?.items) ? order.items : []
  const isPendingConfirmWeightOrder =
    order?.orderMode === 'weight_selection' && order?.pricingStatus === 'pending_confirm'
  const displayAmountText = isPendingConfirmWeightOrder
    ? buildAmountRangeText(order?.estimatedAmount || order?.totalAmount)
    : formatFinalAmountText(order?.finalAmount ?? order?.totalAmount)

  return {
    ...order,
    items,
    listStatusText: isPendingConfirmWeightOrder
      ? '待确认金额'
      : ({
          making: '制作中',
          pending: '待取餐',
          completed: '已完成',
          cancelled: '已取消',
        } as Record<string, string>)[order?.status] || order?.status,
    listStatusClass: ({
      making: 'status-making',
      pending: 'status-pending',
      completed: 'status-completed',
      cancelled: 'status-cancelled',
    } as Record<string, string>)[order?.status] || '',
    listTotalQuantity: items.reduce((sum: number, item: any) => sum + Number(item.quantity || 0), 0),
    listDisplayAmountText: displayAmountText,
    listAmountLabel: isPendingConfirmWeightOrder ? '预估' : '实付',
    listShowPriceEvidenceNotice:
      order?.orderMode === 'weight_selection' && Boolean(order?.priceEvidenceImage),
    listFormattedCreateTime: formatOrderTime(order?.createTime),
  }
}

const filteredOrders = computed(() => {
  if (currentTab.value === 'all') return orders.value
  if (currentTab.value === 'processing') {
    // 制作中 + 待取餐
    return orders.value.filter(order => order.status === 'making' || order.status === 'pending')
  }
  return orders.value.filter(order => order.status === currentTab.value)
})

const switchTab = (tabValue: string) => {
  if (currentTab.value === tabValue) {
    return
  }

  currentTab.value = tabValue
}

const fetchOrders = async (forceRefresh: boolean = false) => {
  if (!userStore.isLogin) {
    orders.value = []
    lastFetchTime.value = 0
    uni.stopPullDownRefresh()
    return
  }

  if (!forceRefresh && orders.value.length > 0 && Date.now() - lastFetchTime.value < ORDER_LIST_CACHE_MS) {
    uni.stopPullDownRefresh()
    return
  }
  
  const res = await http<any[]>({
    url: '/api/client/orders',
  })
  orders.value = Array.isArray(res.data) ? res.data.map(normalizeOrder) : []
  lastFetchTime.value = Date.now()
  uni.stopPullDownRefresh()
}

const goToDetail = (order: any) => {
  safeNavigateTo({
    url: `/pages/order-detail/order-detail?orderId=${order.id}`
  })
}

const onContact = (order: any) => {
  const contactPhone = String(order?.shopContactPhone || '').trim()
  if (!contactPhone) {
    uni.showToast({ title: '商户暂未配置联系电话', icon: 'none' })
    return
  }

  uni.showModal({
    title: '联系商户',
    content: `是否拨打 ${order?.shopName || '商户'} 电话：${contactPhone}`,
    confirmText: '立即拨打',
    success: (res) => {
      if (!res.confirm) {
        return
      }

      uni.makePhoneCall({
        phoneNumber: contactPhone,
        fail: () => {
          uni.showToast({ title: '拨号失败，请稍后重试', icon: 'none' })
        }
      })
    }
  })
}

const onReorder = (order: any) => {
  safeNavigateTo({
    url: `/pages/menu/menu?shopId=${order.shopId}`
  })
}

const goToMenu = () => {
    safeSwitchTab({ url: '/pages/index/index' })
}

onShow(() => {
  syncNativeTabBarHidden()
  fetchOrders()
})

onPullDownRefresh(() => {
  fetchOrders(true)
})
</script>

<style lang="scss">
.order-list-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-top: calc(200rpx + var(--status-bar-height)); /* Increased to match tabs-wrapper height */
  padding-bottom: 120rpx; /* Space for tabbar */
}

.tabs-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 200rpx; /* Increased height to provide more white background */
  background-color: #fff;
  z-index: 100;
  padding: 20rpx 40rpx;
  padding-top: calc(30rpx + var(--status-bar-height)); /* Adjusted top padding to position tabs higher */
  display: flex;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}

.tabs {
  display: flex;
  width: 100%;
  background-color: #f2f3f5;
  border-radius: 40rpx;
  padding: 6rpx;
  
  .tab-item {
    flex: 1;
    height: 68rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #666;
    font-size: 32rpx;
    border-radius: 34rpx;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    font-weight: 500;

    &.active {
      background-color: #fff;
      color: #2a8bff;
      font-weight: bold;
      box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.08);
    }
  }
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 140rpx; /* Further reduced padding to decrease gray-white background space */
  
  .empty-icon {
    margin-bottom: 24rpx;
    opacity: 0.8;
  }
  
  .empty-text {
      font-size: 36rpx; /* Increased font size */
      color: #333;
      font-weight: bold;
      margin-bottom: 12rpx;
  }
  
  .empty-sub {
      font-size: 28rpx; /* Increased font size */
      color: #999;
      margin-bottom: 40rpx;
  }
  
  .go-btn {
      width: 240rpx;
      height: 80rpx;
      line-height: 80rpx;
      background-color: #2a8bff;
      color: #fff;
      font-size: 32rpx; /* Increased font size */
      border-radius: 40rpx;
      font-weight: bold;
      box-shadow: 0 8rpx 20rpx rgba(42, 139, 255, 0.3);
  }
}

.list {
  padding: 16rpx 20rpx; /* Further reduced padding to decrease gray-white background space */
}

.integrity-banner {
  margin: 8rpx 30rpx 16rpx;
  padding: 24rpx 30rpx;
  background-color: #e6f7ff;
  border-radius: 16rpx;
  border: 2rpx solid #91d5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .banner-text {
    font-size: 32rpx;
    color: #1890ff;
    margin-left: 16rpx;
    font-weight: 600;
    line-height: 1.4;
  }
}
</style>
