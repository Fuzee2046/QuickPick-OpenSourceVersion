<template>
  <view class="dish-item" :class="{ disabled: dish.status === 0 }">
    <view class="dish-img-wrapper">
      <image 
        v-if="getImageUrl(dish.image)"
        :src="getImageUrl(dish.image)" 
        class="dish-img" 
        mode="aspectFill" 
        lazy-load
      />
      <view v-else class="dish-img default-dish-img">
        <uni-icons type="fire" size="40" color="#e0e0e0"></uni-icons>
      </view>
      <view v-if="dish.status === 0" class="sold-out-mask">
        <text class="sold-out-text">已售完</text>
      </view>
    </view>
    <view class="dish-info">
      <text class="dish-name">{{ dish.name }}</text>
      <!-- Optional: Sales/Rating placeholder -->
      <!-- <text class="dish-sales">月售 100+ 好评度 98%</text> -->
      
      <view class="dish-bottom">
        <view class="dish-price-box">
          <text class="dish-price">￥{{ dish.price }}</text>
          <text v-if="hasOptions" class="dish-price-tip">可选规格</text>
        </view>
        <view class="action-area">
          <view
            v-if="dish.status === 1 && hasOptions"
            class="choose-option-btn"
            @tap.stop="onChoose"
          >
            {{ quantity > 0 ? `已选${quantity}` : '+ 选规格' }}
          </view>
          <NumberCounter 
            v-else-if="dish.status === 1"
            :quantity="quantity"
            @increase="onAdd"
            @decrease="onRemove"
          />
          <text v-else class="sold-out-tip">已售完</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { baseURL } from '@/utils/http'
import NumberCounter from './NumberCounter.vue'

const props = defineProps<{
  dish: any
  quantity: number
  hasOptions?: boolean
}>()

const emit = defineEmits(['add', 'remove', 'choose'])

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return baseURL + url
}

const onAdd = () => {
  emit('add', props.dish)
}

const onRemove = () => {
  emit('remove', props.dish)
}

const onChoose = () => {
  emit('choose', props.dish)
}
</script>

<style lang="scss" scoped>
.dish-item {
  display: flex;
  background-color: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);

  &.disabled {
    .dish-name, .dish-price {
      color: #999;
    }
  }

  .dish-img-wrapper {
    position: relative;
    width: 180rpx;
    height: 180rpx;
    margin-right: 20rpx;
    border-radius: 12rpx;
    overflow: hidden;

    .dish-img {
      width: 100%;
      height: 100%;
      background-color: #f5f5f5;
      
      &.default-dish-img {
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .sold-out-mask {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(255, 255, 255, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;

      .sold-out-text {
        font-size: 24rpx;
        color: #666;
        font-weight: bold;
        background-color: rgba(255, 255, 255, 0.9);
        padding: 4rpx 10rpx;
        border-radius: 4rpx;
      }
    }
  }

  .dish-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .dish-name {
      font-size: 28rpx;
      color: #333;
      margin-top: 0;
      line-height: 1.4;
    }

    .dish-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .dish-price-box {
        display: flex;
        flex-direction: column;
        gap: 6rpx;
      }

      .dish-price {
        font-size: 32rpx;
        color: #ff7b2c;
        font-weight: bold;
      }

      .dish-price-tip {
        font-size: 22rpx;
        color: #94a3b8;
      }

      .action-area {
        .choose-option-btn {
          min-width: 120rpx;
          height: 56rpx;
          padding: 0 20rpx;
          border-radius: 28rpx;
          background: linear-gradient(135deg, #2a8bff, #56a6ff);
          color: #fff;
          font-size: 24rpx;
          font-weight: 600;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 6rpx 12rpx rgba(42, 139, 255, 0.22);
        }

        .sold-out-tip {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }
}
</style>
