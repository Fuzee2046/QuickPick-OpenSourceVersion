<template>
  <view class="detail-container">
    <!-- Top Navigation Bar with Back Button -->
    <view class="nav-bar">
      <uni-icons type="back" size="24" color="#fff" @tap="onBack" class="nav-back"></uni-icons>
      <text class="nav-title">订单详情</text>
      <view class="nav-right-placeholder"></view>
    </view>
    
    <!-- Header Section: Background & Shop Info -->
    <view class="header-section">
      <view class="header-bg-wrapper">
        <image 
          v-if="getImageUrl(shop.coverImage)"
          :src="getImageUrl(shop.coverImage)" 
          class="header-bg" 
          mode="aspectFill" 
        />
        <view v-else class="header-bg default-header-bg"></view>
        <view class="header-mask"></view>
      </view>
      
      <view class="header-content" @tap="goToShop">
        <view class="shop-title-row">
          <text class="shop-name">{{ shop.name }}</text>
        </view>
        <view class="shop-address-row">
           <uni-icons type="location-filled" size="16" color="#fff" style="opacity: 0.8;"></uni-icons>
           <text class="shop-address">{{ shop.address }}</text>
        </view>
      </view>
    </view>

    <!-- Info Overview Card (Status & Pickup) -->
    <view class="info-card-wrapper">
      <view class="info-overview-card">
        <!-- Status Header -->
        <view class="status-header">
           <text class="status-title" :class="statusClass">{{ statusText }}</text>
           <text class="status-desc">{{ statusDesc }}</text>
        </view>

        <!-- Pickup Code (Prominent) -->
        <view class="pickup-area" v-if="['making', 'pending'].includes(order.status)">
          <text class="pickup-label">取餐号</text>
          <text class="pickup-code">{{ order.pickupCode }}</text>
          <view class="pickup-time-badge" v-if="order.pickupTime">
            <uni-icons type="time-filled" size="14" color="#1890ff"></uni-icons>
            <text class="time-val">预计 {{ formatTime(order.pickupTime) }} 取餐</text>
          </view>
          <!-- 诚信提示 -->
          <view class="integrity-tip-small">
            <uni-icons type="heart-filled" size="14" color="#ff7b2c"></uni-icons>
            <text class="tip-text">下单即承诺，取餐显诚信</text>
          </view>
        </view>
        
        <!-- Action Buttons (Contextual) -->
        <view class="card-actions">
           <view class="action-btn outline" @tap="onContact">联系商户</view>
           <view class="action-btn outline" v-if="order.status === 'completed'" @tap="onReorder">再来一单</view>
        </view>
      </view>
    </view>

    <view class="section evidence-section evidence-section--highlight" v-if="showPriceEvidenceSection">
      <view class="evidence-section__badge">称重凭证</view>
      <view class="evidence-tip">
        商户已上传电子称重照片，方便你核对订单金额与现场称重情况。
      </view>
      <image
        :src="getImageUrl(order.priceEvidenceImage)"
        class="evidence-image"
        mode="aspectFill"
        @tap="previewEvidenceImage"
      />
      <view class="evidence-action" @tap="previewEvidenceImage">点击查看大图</view>
    </view>

    <!-- Order Items List -->
    <view class="section order-items">
      <view class="section-header">
        <text class="section-title">订单详情</text>
        <text class="dining-mode">{{ order.needPack === 1 ? '打包带走' : '堂食' }}</text>
      </view>
      
      <view v-for="item in items" :key="item.id" class="dish-item">
        <image 
          v-if="getImageUrl(item.image || item.dish?.image || item.dishImage)"
          :src="getImageUrl(item.image || item.dish?.image || item.dishImage)" 
          class="dish-thumb" 
          mode="aspectFill" 
        />
        <view v-else class="dish-thumb default-dish-thumb">
             <uni-icons type="fire" size="24" color="#ddd"></uni-icons>
        </view>
        <view class="dish-info">
          <view class="dish-main">
             <text class="dish-name">{{ item.dishName }}</text>
             <text class="dish-price">￥{{ item.price }}</text>
          </view>
          <view class="dish-sub">
            <view class="dish-sub-main">
              <text class="dish-qty">x{{ item.quantity }}</text>
              <text v-if="getOrderItemOptionsText(item)" class="dish-options">{{ getOrderItemOptionsText(item) }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- Price Summary -->
      <view class="price-summary">
        <view class="summary-row">
          <text class="label">{{ isPendingConfirmWeightOrder ? '预估金额' : '商品总额' }}</text>
          <text class="val">{{ displayAmountText }}</text>
        </view>
        <view class="summary-row" v-if="order.needPack === 1">
          <text class="label">打包费</text>
          <text class="val">￥1.00</text>
        </view>
        <view class="divider"></view>
        <view class="summary-row total-row">
          <text class="label">{{ isPendingConfirmWeightOrder ? '预估区间' : '实付金额' }}</text>
          <text class="val highlight">{{ displayAmountText }}</text>
        </view>
      </view>
    </view>

    <!-- Order Info Details -->
    <view class="section info-section">
      <view class="section-title">订单信息</view>
      <view class="info-row">
        <text class="label">订单编号</text>
        <view class="val-box">
          <text class="val">{{ order.id }}</text>
          <text class="copy-btn" @tap="copyText(order.id)">复制</text>
        </view>
      </view>
      <view class="info-row">
        <text class="label">下单时间</text>
        <text class="val">{{ formatDate(order.createTime) }}</text>
      </view>
      <view class="info-row" v-if="order.pickupTime">
        <text class="label">取餐时间</text>
        <text class="val">{{ formatTime(order.pickupTime) }}</text>
      </view>
      <view class="info-row">
        <text class="label">用餐方式</text>
        <text class="val">{{ order.needPack === 1 ? '打包' : '堂食' }}</text>
      </view>
      <view class="info-row" v-if="order.remark">
        <text class="label">订单备注</text>
        <text class="val">{{ order.remark }}</text>
      </view>
    </view>

    <!-- Bottom Action Bar (Fixed) -->
    <view class="bottom-placeholder"></view>
    <view class="bottom-bar">
      <view class="home-btn" @tap="onHome">
        <uni-icons type="home" size="24" color="#666"></uni-icons>
        <text>首页</text>
      </view>
      <view class="main-actions">
        <button class="action-btn primary" @tap="onReorder">再来一单</button>
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
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { http, baseURL } from '@/utils/http'
import { safeNavigateTo, safeSwitchTab, smartNavigateBack } from '@/utils/navigation'
import BrandLoadingOverlay from '@/components/BrandLoadingOverlay.vue'
import { useBrandLoading } from '@/composables/useBrandLoading'

const order = ref<any>({})
const items = ref<any[]>([])
const shop = ref<any>({})
const {
  visible: pageLoadingVisible,
  title: pageLoadingTitle,
  description: pageLoadingDescription,
  show: showPageLoading,
  hide: hidePageLoading
} = useBrandLoading({
  title: '订单信息正在展开',
  description: '正在同步店铺与订单详情，请稍候'
})

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return baseURL + url
}

const statusClass = computed(() => {
  switch (order.value.status) {
    case 'making': return 'text-making'
    case 'pending': return 'text-pending'
    case 'completed': return 'text-completed'
    case 'cancelled': return 'text-cancelled'
    default: return ''
  }
})

const statusText = computed(() => {
  switch (order.value.status) {
    case 'making': return '制作中'
    case 'pending': return '待取餐'
    case 'completed': return '已完成'
    case 'cancelled': return '已取消'
    default: return order.value.status
  }
})

const statusDesc = computed(() => {
  switch (order.value.status) {
    case 'making': return '商家正在加紧制作中，请耐心等待'
    case 'pending': return '您的餐品已准备好，请凭取餐号取餐'
    case 'completed': return '感谢您的光临，期待再次相遇'
    case 'cancelled': return '订单已取消'
    default: return ''
  }
})
const isPendingConfirmWeightOrder = computed(() =>
  order.value?.orderMode === 'weight_selection' && order.value?.pricingStatus === 'pending_confirm'
)
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
const displayAmountText = computed(() => {
  if (isPendingConfirmWeightOrder.value) {
    return buildAmountRangeText(order.value?.estimatedAmount || order.value?.totalAmount)
  }
  return formatFinalAmountText(order.value?.finalAmount ?? order.value?.totalAmount)
})
const showPriceEvidenceSection = computed(() => {
  if (order.value?.orderMode !== 'weight_selection') return false
  if (!order.value?.priceEvidenceImage) return false
  const estimatedAmount = Number(order.value?.estimatedAmount || 0)
  const finalAmount = Number((order.value?.finalAmount ?? order.value?.totalAmount) || 0)
  return estimatedAmount > 0 && finalAmount > estimatedAmount
})

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  // If it's full datetime string, extract time
  if (timeStr.includes('T') || timeStr.includes(' ')) {
      const date = new Date(timeStr)
      return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  // If it's HH:mm:ss
  return timeStr.substring(0, 5)
}

const copyText = (text: string) => {
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}

const previewEvidenceImage = () => {
  const imageUrl = getImageUrl(order.value?.priceEvidenceImage)
  if (!imageUrl) return
  uni.previewImage({
    urls: [imageUrl],
    current: imageUrl
  })
}

const getOrderItemOptionsText = (item: any) => item?.options || item?.optionSummary || ''

onLoad((options) => {
  if (options && options.orderId) {
    fetchOrderDetail(options.orderId)
  }
})

const fetchOrderDetail = async (orderId: string) => {
  showPageLoading()
  try {
    const res = await http<any>({
      url: `/api/client/orders/${orderId}`
    })

    order.value = res.data.order
    shop.value = res.data.shop || {}

    items.value = Array.isArray(res.data.items) ? res.data.items : []
  } catch (err) {
    console.error(err)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    hidePageLoading(true)
  }
}

const onContact = () => {
  const contactPhone = String(shop.value?.contactPhone || '').trim()
  if (!contactPhone) {
    uni.showToast({ title: '商户暂未配置联系电话', icon: 'none' })
    return
  }

  uni.showModal({
    title: '联系商户',
    content: `是否拨打 ${shop.value?.name || '商户'} 电话：${contactPhone}`,
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

const onHome = () => {
  safeSwitchTab({ url: '/pages/index/index' })
}

const onReorder = () => {
  safeNavigateTo({
    url: `/pages/menu/menu?shopId=${shop.value.id}`
  })
}

const goToShop = () => {
    // Optional: go to shop page
}

const onBack = () => {
  smartNavigateBack({ fallbackTab: 'pages/order-list/order-list' })
}
</script>

<style lang="scss">
.detail-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 40rpx;
  box-sizing: border-box;
}

/* Navigation Bar */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 108rpx; /* Increased height for better spacing */
  background: linear-gradient(to bottom, rgba(0,0,0,0.4), transparent);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx;
  padding-top: calc(var(--status-bar-height) + 10rpx); /* Added top padding to move arrow down */
  
  .nav-back {
    flex: 0 0 auto;
  }
  
  .nav-title {
    flex: 1;
    text-align: center;
    font-size: 32rpx;
    font-weight: bold;
    color: #fff;
  }
  
  .nav-right-placeholder {
    flex: 0 0 24rpx; /* Same width as back icon for balance */
  }
}

/* Header */
.header-section {
  position: relative;
  height: calc(380rpx + var(--status-bar-height));
  background-color: #333;
  overflow: hidden;
}

.header-bg-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  
  .header-bg {
    width: 100%;
    height: 100%;
    filter: blur(10px);
    opacity: 0.6;
    transform: scale(1.1);
    
    &.default-header-bg {
        background: linear-gradient(135deg, #444 0%, #666 100%);
    }
  }
  
  .header-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(to bottom, rgba(0,0,0,0.3), rgba(0,0,0,0.6));
  }
}

.header-content {
  position: relative;
  z-index: 10;
  padding: 60rpx 40rpx;
  padding-top: calc(120rpx + var(--status-bar-height)); /* Increased for taller nav bar */
  color: #fff;
  display: flex;
  flex-direction: column;

  
  .shop-title-row {
    display: flex;
    align-items: center;
    margin-bottom: 16rpx;
    
    .shop-name {
      font-size: 40rpx;
      font-weight: bold;
      margin-right: 12rpx;
      letter-spacing: 1rpx;
    }
    

  }
  
  .shop-address-row {
    display: flex;
    align-items: center;
    opacity: 0.9;
    
    .shop-address {
      font-size: 26rpx;
      margin-left: 8rpx;
      max-width: 80%;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

/* Info Overview Card */
.info-card-wrapper {
  padding: 0 30rpx;
  margin-top: -120rpx;
  position: relative;
  z-index: 20;
  margin-bottom: 30rpx;
}

.info-overview-card {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .status-header {
      text-align: center;
      margin-bottom: 30rpx;
      
      .status-title {
          font-size: 44rpx;
          font-weight: bold;
          display: block;
          margin-bottom: 10rpx;
          
          &.text-making { color: #1890ff; }
          &.text-pending { color: #ff7b2c; }
          &.text-completed { color: #333; }
          &.text-cancelled { color: #999; }
      }
      
      .status-desc {
          font-size: 26rpx;
          color: #999;
      }
  }
  
  .pickup-area {
      width: 100%;
      background-color: #f9fbff;
      border: 1rpx solid #e6f0ff;
      border-radius: 16rpx;
      padding: 30rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 30rpx;
      
      .pickup-label {
          font-size: 28rpx;
          color: #666;
          margin-bottom: 10rpx;
      }
      
      .pickup-code {
          font-size: 80rpx;
          font-weight: bold;
          color: #1890ff;
          line-height: 1;
          margin-bottom: 20rpx;
          font-family: 'DIN', sans-serif; /* If available */
      }
      
      .pickup-time-badge {
          display: flex;
          align-items: center;
          background-color: #e6f7ff;
          padding: 6rpx 20rpx;
          border-radius: 24rpx;
          
          .time-val {
              font-size: 24rpx;
              color: #1890ff;
              margin-left: 8rpx;
          }
      }
  }
  
  .card-actions {
      display: flex;
      gap: 30rpx;
      
      .action-btn {
          padding: 12rpx 40rpx;
          border-radius: 40rpx;
          font-size: 28rpx;
          
          &.outline {
              border: 1rpx solid #ddd;
              color: #666;
          }
      }
  }
}

/* Sections */
.section {
  margin: 0 30rpx 30rpx;
  background-color: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.02);
  
  .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 30rpx;
      padding-bottom: 20rpx;
      border-bottom: 1rpx solid #f5f5f5;
      
      .section-title {
          font-size: 32rpx;
          font-weight: bold;
          color: #333;
      }
      
      .dining-mode {
          font-size: 24rpx;
          background-color: #333;
          color: #fff;
          padding: 4rpx 12rpx;
          border-radius: 8rpx;
      }
  }
}

.order-items {
  .dish-item {
    display: flex;
    margin-bottom: 30rpx;
    
    .dish-thumb {
      width: 120rpx;
      height: 120rpx;
      border-radius: 12rpx;
      margin-right: 24rpx;
      background-color: #f5f5f5;
      
      &.default-dish-thumb {
          display: flex;
          align-items: center;
          justify-content: center;
      }
    }
    
    .dish-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      
      .dish-main {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8rpx;
          
          .dish-name {
              font-size: 30rpx;
              color: #333;
              font-weight: 500;
          }
          .dish-price {
              font-size: 30rpx;
              color: #333;
              font-weight: bold;
          }
      }
      
      .dish-sub {
          .dish-sub-main {
              display: flex;
              flex-direction: column;
              gap: 8rpx;
          }

          .dish-qty {
              font-size: 26rpx;
              color: #999;
          }

          .dish-options {
              font-size: 22rpx;
              color: #2a8bff;
              line-height: 1.5;
          }
      }
      
      .dish-remark {
          margin-top: 10rpx;
          background-color: #f5f5f5;
          color: #666;
          font-size: 24rpx;
          padding: 8rpx 16rpx;
          border-radius: 8rpx;
          align-self: flex-start;
      }
    }
  }
  
  .price-summary {
    margin-top: 30rpx;
    padding-top: 20rpx;
    border-top: 1rpx dashed #eee;
    
    .summary-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 16rpx;
      font-size: 28rpx;
      
      .label { color: #666; }
      .val { color: #333; }
      
      &.total-row {
        margin-top: 20rpx;
        align-items: center;
        
        .label { font-size: 30rpx; font-weight: bold; color: #333; }
        .val.highlight { font-size: 40rpx; font-weight: bold; color: #ff7b2c; }
      }
    }
    
    .divider { height: 1rpx; background-color: #f5f5f5; margin: 20rpx 0; }
  }
}

.info-section {
    .section-title {
        font-size: 32rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 30rpx;
    }
    
    .info-row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 24rpx;
        font-size: 28rpx;
        
        &:last-child { margin-bottom: 0; }
        
        .label { color: #999; }
        
        .val-box {
            display: flex;
            align-items: center;
            
            .copy-btn {
                font-size: 22rpx;
                color: #2a8bff;
                border: 1rpx solid #2a8bff;
                padding: 2rpx 10rpx;
                border-radius: 20rpx;
                margin-left: 12rpx;
            }
        }
        
        .val { color: #333; }
    }
}

.evidence-section {
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 20rpx;
  }

  .evidence-section__badge {
    display: inline-flex;
    align-items: center;
    padding: 12rpx 26rpx;
    border-radius: 999rpx;
    background: rgba(255, 255, 255, 0.72);
    color: #9a3412;
    font-size: 28rpx;
    font-weight: 700;
    margin-bottom: 20rpx;
  }

  .evidence-tip {
    font-size: 24rpx;
    line-height: 1.6;
    color: #666;
    margin-bottom: 20rpx;
  }

  .evidence-image {
    width: 100%;
    height: 420rpx;
    border-radius: 18rpx;
    background: #f5f5f5;
  }

  .evidence-action {
    margin-top: 16rpx;
    font-size: 24rpx;
    color: #2a8bff;
    text-align: center;
  }
}

.evidence-section--highlight {
  background: linear-gradient(135deg, #fff7ed 0%, #fffbeb 100%);
  border: 1rpx solid rgba(249, 115, 22, 0.14);
  box-shadow: 0 10rpx 24rpx rgba(249, 115, 22, 0.08);
}

/* Bottom Bar */
.bottom-placeholder {
    height: 120rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  padding: 20rpx 40rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  z-index: 100;
  
  .home-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-right: 40rpx;
      
      text {
          font-size: 22rpx;
          color: #666;
          margin-top: 4rpx;
      }
  }
  
  .main-actions {
      flex: 1;
      
      .action-btn {
          width: 100%;
          border-radius: 44rpx;
          font-size: 30rpx;
          font-weight: bold;
          
          &.primary {
              background-color: #2a8bff;
              color: #fff;
              border: none;
              box-shadow: 0 4rpx 12rpx rgba(42, 139, 255, 0.3);
              
              &:active { opacity: 0.9; }
          }
    }
  }
}

/* 诚信提示 */
.integrity-tip-small {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20rpx;
  padding: 12rpx 20rpx;
  background-color: #fff8e6;
  border-radius: 12rpx;
  border: 1rpx solid #ffd166;
  
  .tip-text {
    font-size: 24rpx;
    color: #ff7b2c;
    margin-left: 8rpx;
  }
}
</style>
