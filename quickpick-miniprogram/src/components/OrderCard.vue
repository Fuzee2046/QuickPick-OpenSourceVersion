<template>
  <view class="order-card" @tap="onClick">
    <!-- Header: Shop Name & Status -->
    <view class="card-header">
      <view class="shop-info">
        <text class="shop-name">{{ order.shopName }}</text>
        <uni-icons type="right" size="14" color="#999" class="arrow-icon"></uni-icons>
      </view>
      <view class="status-tag" :class="statusClass">{{ statusText }}</view>
    </view>

    <!-- Content: Dish List -->
    <view class="card-content">
      <view 
        v-for="(item, index) in displayedItems" 
        :key="index" 
        class="dish-item"
      >
        <view class="dish-image-wrap">
          <image 
            v-if="getImageUrl(item.image)"
            :src="getImageUrl(item.image)" 
            class="dish-image" 
            mode="aspectFill" 
          />
          <view v-else class="dish-image default-dish-image">
               <uni-icons type="fire" size="24" color="#ddd"></uni-icons>
          </view>
          <view v-if="showPriceEvidenceNotice && index === 0" class="dish-evidence-badge">保留凭证</view>
        </view>
        <view class="dish-info">
          <text class="dish-name">{{ getOrderItemName(item) }}</text>
          <view class="dish-meta">
            <text class="dish-qty">x{{ item.quantity }}</text>
          </view>
          <text v-if="getOrderItemOptionsText(item)" class="dish-options">{{ getOrderItemOptionsText(item) }}</text>
        </view>
        <view class="dish-price" v-if="item.price">
          <text>¥{{ item.price }}</text>
        </view>
      </view>

      <view
        v-if="hasMoreItems"
        class="items-toggle"
        @tap.stop="toggleItems"
      >
        <text class="items-toggle__text">{{ itemsToggleText }}</text>
        <uni-icons
          type="bottom"
          size="14"
          color="#2a8bff"
          class="items-toggle__icon"
          :class="{ open: itemsExpanded }"
        ></uni-icons>
      </view>
    </view>

    <!-- Info: Time & Total -->
    <view class="card-info">
      <text class="order-time">{{ formatDate(order.createTime) }}</text>
      <view class="total-box">
        <text class="total-label">共 {{ totalQuantity }} 件，{{ amountLabel }}</text>
        <text class="total-price">{{ displayAmountText }}</text>
      </view>
    </view>

    <view v-if="showPriceEvidenceNotice" class="evidence-notice">
      <uni-icons type="camera-filled" size="15" color="#c2410c"></uni-icons>
      <text class="evidence-notice__text">最终金额超出预估区间，商户已保留称重凭证</text>
    </view>
    
    <!-- Pickup Code (Only for Active Orders) -->
    <view class="pickup-bar" v-if="['making', 'pending'].includes(order.status)">
       <text class="pickup-label">取餐号</text>
       <text class="pickup-code">{{ order.pickupCode }}</text>
    </view>

    <!-- Footer: Actions -->
    <view class="card-footer" @tap.stop>
      <template v-if="['making', 'pending'].includes(order.status)">
        <view class="action-btn outline" @tap="onContact">联系商户</view>
        <view class="action-btn primary" @tap="onDetail">查看详情</view>
      </template>
      <template v-else-if="order.status === 'completed'">
        <view class="action-btn outline" @tap="onReorder">再来一单</view>
      </template>
      <template v-else>
         <view class="action-btn outline" @tap="onReorder">再来一单</view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { baseURL } from '@/utils/http'

const props = defineProps<{
  order: any
}>()

const emit = defineEmits(['click', 'contact', 'detail', 'reorder'])
const itemsExpanded = ref(false)
const PREVIEW_ITEM_COUNT = 2

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return baseURL + url
}

const orderItems = computed(() => props.order.items || [])
const hasMoreItems = computed(() => orderItems.value.length > PREVIEW_ITEM_COUNT)
const displayedItems = computed(() =>
  itemsExpanded.value ? orderItems.value : orderItems.value.slice(0, PREVIEW_ITEM_COUNT)
)
const hiddenItemsCount = computed(() => Math.max(0, orderItems.value.length - PREVIEW_ITEM_COUNT))
const itemsToggleText = computed(() =>
  itemsExpanded.value ? '收起明细' : `展开其余 ${hiddenItemsCount.value} 项`
)

const statusClass = computed(() => {
  if (props.order.listStatusClass) return props.order.listStatusClass
  switch (props.order.status) {
    case 'making': return 'status-making'
    case 'pending': return 'status-pending'
    case 'completed': return 'status-completed'
    case 'cancelled': return 'status-cancelled'
    default: return ''
  }
})

const statusText = computed(() => {
  if (props.order.listStatusText) return props.order.listStatusText
  if (props.order.status === 'making' && props.order.pricingStatus === 'pending_confirm') {
    return '待确认金额'
  }
  switch (props.order.status) {
    case 'making': return '制作中'
    case 'pending': return '待取餐'
    case 'completed': return '已完成'
    case 'cancelled': return '已取消'
    default: return props.order.status
  }
})

const totalQuantity = computed(() => {
  if (typeof props.order.listTotalQuantity === 'number') return props.order.listTotalQuantity
  if (!orderItems.value.length) return 0
  return orderItems.value.reduce((sum: number, item: any) => sum + item.quantity, 0)
})

const getOrderItemName = (item: any) => item?.dishName || item?.name || item?.ingredientName || '未命名'
const getOrderItemOptionsText = (item: any) => item?.options || item?.optionSummary || ''
const isPendingConfirmWeightOrder = computed(() =>
  props.order?.orderMode === 'weight_selection' && props.order?.pricingStatus === 'pending_confirm'
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
const amountLabel = computed(() => props.order.listAmountLabel || (isPendingConfirmWeightOrder.value ? '预估' : '实付'))
const displayAmountText = computed(() => {
  if (props.order.listDisplayAmountText) return props.order.listDisplayAmountText
  if (isPendingConfirmWeightOrder.value) {
    return buildAmountRangeText(props.order?.estimatedAmount || props.order?.totalAmount)
  }
  return formatFinalAmountText(props.order?.finalAmount ?? props.order?.totalAmount)
})
const showPriceEvidenceNotice = computed(() => {
  if (typeof props.order.listShowPriceEvidenceNotice === 'boolean') {
    return props.order.listShowPriceEvidenceNotice
  }
  if (props.order?.orderMode !== 'weight_selection') return false
  return Boolean(props.order?.priceEvidenceImage)
})

const formatDate = (dateStr: string) => {
  if (props.order.listFormattedCreateTime) return props.order.listFormattedCreateTime
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const onClick = () => emit('click', props.order)
const onContact = () => emit('contact', props.order)
const onDetail = () => emit('detail', props.order)
const onReorder = () => emit('reorder', props.order)
const toggleItems = () => {
  itemsExpanded.value = !itemsExpanded.value
}
</script>

<style lang="scss" scoped>
.order-card {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f0f0f0;
}

/* Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .shop-info {
    display: flex;
    align-items: center;
    
    .shop-name {
      font-size: 36rpx; /* Increased from 34rpx */
      font-weight: bold;
      color: #333;
      margin-right: 12rpx;
    }
    
    .arrow-icon {
      margin-top: 4rpx;
    }
  }

  .status-tag {
    font-size: 28rpx; /* Increased from 24rpx */
    font-weight: 600;
    padding: 8rpx 20rpx;
    border-radius: 10rpx;
    
    &.status-making { 
      color: #1890ff; 
      background-color: rgba(24, 144, 255, 0.12);
    }
    &.status-pending { 
      color: #ff7b2c; 
      background-color: rgba(255, 123, 44, 0.12);
    }
    &.status-completed { 
      color: #666; 
      background-color: #f8f8f8;
    }
    &.status-cancelled { 
      color: #999; 
      background-color: #f8f8f8;
    }
  }
}

/* Content */
.card-content {
  margin-bottom: 16rpx;

  .dish-item {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;
    
    &:last-child {
      margin-bottom: 0;
    }

    .dish-image-wrap {
      position: relative;
      width: 100rpx;
      height: 100rpx;
      margin-right: 20rpx;
      flex-shrink: 0;
    }

    .dish-image {
      width: 100%;
      height: 100%;
      border-radius: 12rpx;
      background-color: #f8f8f8;
      
      &.default-dish-image {
          display: flex;
          align-items: center;
          justify-content: center;
      }
    }

    .dish-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;
      
      .dish-name {
        font-size: 30rpx; /* Increased from 28rpx */
        color: #333;
        margin-bottom: 6rpx;
        font-weight: 600;
      }
      
      .dish-meta {
        display: flex;
        align-items: center;
        
        .dish-qty {
          font-size: 26rpx; /* Increased from 24rpx */
          color: #666;
        }
      }

      .dish-options {
        margin-top: 6rpx;
        font-size: 22rpx;
        color: #2a8bff;
        line-height: 1.4;
      }
    }
    
    .dish-price {
        font-size: 30rpx; /* Increased from 28rpx */
        color: #333;
        font-weight: 600;
    }
  }
}

.dish-evidence-badge {
  position: absolute;
  left: 8rpx;
  bottom: 8rpx;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: rgba(124, 45, 18, 0.88);
  color: #fff7ed;
  font-size: 18rpx;
  line-height: 1.2;
  font-weight: 600;
  box-shadow: 0 4rpx 10rpx rgba(124, 45, 18, 0.18);
}

.items-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding-top: 12rpx;
}

.items-toggle__text {
  font-size: 24rpx;
  color: #2a8bff;
  font-weight: 500;
}

.items-toggle__icon {
  transition: transform 0.2s ease;

  &.open {
    transform: rotate(180deg);
  }
}

/* Info & Pickup */
.card-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  
  .order-time {
    font-size: 26rpx; /* Increased from 24rpx */
    color: #666;
  }
  
  .total-box {
    display: flex;
    align-items: baseline;
    
    .total-label {
      font-size: 26rpx; /* Increased from 24rpx */
      color: #666;
      margin-right: 10rpx;
    }
    
    .total-price {
      font-size: 36rpx; /* Increased from 32rpx */
      font-weight: bold;
      color: #ff7b2c;
    }
  }
}

.evidence-notice {
  margin-top: 16rpx;
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  border: 1rpx solid rgba(249, 115, 22, 0.18);
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.evidence-notice__text {
  flex: 1;
  font-size: 24rpx;
  line-height: 1.5;
  color: #9a3412;
  font-weight: 500;
}

.pickup-bar {
  margin-top: 16rpx;
  background-color: #f0f8ff;
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1rpx solid #e6f0ff;
  
  .pickup-label {
    font-size: 28rpx; /* Increased from 26rpx */
    color: #666;
    font-weight: 500;
  }
  
  .pickup-code {
    font-size: 40rpx; /* Increased from 36rpx */
    font-weight: bold;
    color: #2a8bff;
    letter-spacing: 2rpx;
  }
}

/* Footer */
.card-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 24rpx;
  
  .action-btn {
    padding: 14rpx 36rpx;
    border-radius: 36rpx;
    font-size: 28rpx; /* Increased from 26rpx */
    margin-left: 20rpx;
    transition: all 0.2s;
    font-weight: 600;
    
    &.outline {
      border: 1rpx solid #ddd;
      color: #666;
      background: transparent;
    }
    
    &.primary {
      background: #2a8bff;
      color: #fff;
      border: 1rpx solid #2a8bff;
      box-shadow: 0 4rpx 12rpx rgba(42, 139, 255, 0.25);
    }
    
    &:active {
      opacity: 0.8;
      transform: scale(0.98);
    }
  }
}
</style>
