<template>
  <view 
    class="shop-card" 
    :class="{ 'shop-closed': ['closed', 'paused', 'service_paused'].includes(shop.displayStatus) }"
    hover-class="shop-card-hover"
    @tap="onClick"
  >
    <image 
      v-if="getImageUrl(shop.coverImage)"
      :src="getImageUrl(shop.coverImage)" 
      class="shop-cover" 
      mode="aspectFill"
      lazy-load
    />
    <view v-else class="shop-cover default-cover">
      <uni-icons type="shop" size="40" color="#ccc"></uni-icons>
    </view>
    <view class="shop-info">
      <view class="shop-header">
        <text class="shop-name">{{ shop.name }}</text>
      </view>
      <view class="shop-address-box">
        <uni-icons type="location-filled" size="16" color="#999"></uni-icons>
        <text class="shop-address">{{ shop.address }}</text>
      </view>
      <view class="shop-bottom">
         <view class="shop-status-tag" :class="getStatusClass(shop.displayStatus)">
            <text>{{ shop.displayStatusText || '营业中' }}</text>
         </view>
         <!-- Optional: Add time info or rating if available -->
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { baseURL } from '@/utils/http'

const props = defineProps<{
  shop: any
}>()

const emit = defineEmits(['click'])

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return baseURL + url
}

const onClick = () => {
  emit('click', props.shop)
}

const getStatusClass = (displayStatus?: string) => {
  if (displayStatus === 'reservable') return 'status-reservable'
  if (displayStatus === 'closed' || displayStatus === 'paused' || displayStatus === 'service_paused') return 'status-closed'
  return 'status-open'
}
</script>

<style lang="scss" scoped>
.shop-card {
  display: flex;
  background-color: #fff;
  border-radius: 24rpx; /* Rounded corners */
  padding: 24rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
  position: relative;
  overflow: hidden;

  &.shop-card-hover {
    transform: scale(0.98);
    background-color: #fafafa;
  }

  .shop-cover {
    width: 180rpx;
    height: 180rpx;
    border-radius: 16rpx;
    margin-right: 24rpx;
    background-color: #f0f0f0;
    flex-shrink: 0;
    
    &.default-cover {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .shop-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: 4rpx 0;

    .shop-header {
      .shop-name {
        font-size: 34rpx; /* Larger font */
        color: #333;
        font-weight: bold;
        line-height: 1.4;
        margin-bottom: 8rpx;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 1;
        overflow: hidden;
      }
    }

    .shop-address-box {
      display: flex;
      align-items: center;
      margin-bottom: auto;
      
      .shop-address {
        font-size: 26rpx;
        color: #999;
        margin-left: 6rpx;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 380rpx;
      }
    }

    .shop-bottom {
      display: flex;
      align-items: center;
      
      .shop-status-tag {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 6rpx 20rpx;
        border-radius: 30rpx; /* Capsule shape */
        font-size: 24rpx;
        font-weight: 500;
        
        &.status-open {
          background-color: #e6f7ff;
          color: #1890ff;
        }

        &.status-reservable {
          background-color: #fff4e5;
          color: #f08c2e;
        }
        
        &.status-closed {
          background-color: #f5f5f5;
          color: #999;
        }
      }
    }
  }
}
</style>
