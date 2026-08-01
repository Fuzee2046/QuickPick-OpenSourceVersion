<template>
  <view class="number-counter" :class="{ expanded: quantity > 0 }">
    <view class="add-btn" @tap.stop="onIncrease">
      <view class="icon-plus"></view>
    </view>

    <view class="counter-control">
      <view class="btn minus" @tap.stop="onDecrease"></view>
      <text class="count">{{ quantity }}</text>
      <view class="btn plus" @tap.stop="onIncrease"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  quantity: number
}>()

const emit = defineEmits(['increase', 'decrease'])

const onIncrease = () => {
  emit('increase')
}

const onDecrease = () => {
  emit('decrease')
}
</script>

<style lang="scss" scoped>
.number-counter {
  position: relative;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  overflow: visible;
  transition: width 0.24s cubic-bezier(0.22, 1, 0.36, 1);

  &.expanded {
    width: 164rpx;

    .add-btn {
      opacity: 0;
      transform: scale(0.7);
      pointer-events: none;
    }

    .counter-control {
      opacity: 1;
      transform: translateX(0) scale(1);
      pointer-events: auto;
    }
  }

  .add-btn,
  .counter-control {
    position: absolute;
    top: 0;
    right: 0;
    height: 48rpx;
    transition: opacity 0.2s ease, transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
  }

  .add-btn {
    width: 48rpx;
    background: linear-gradient(135deg, #2a8bff, #56a6ff);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4rpx 8rpx rgba(42, 139, 255, 0.3);

    .icon-plus {
      position: relative;
      width: 18rpx;
      height: 18rpx;

      &::before,
      &::after {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        border-radius: 999rpx;
        background: #fff;
        transform: translate(-50%, -50%);
      }

      &::before {
        width: 18rpx;
        height: 4rpx;
      }

      &::after {
        width: 4rpx;
        height: 18rpx;
      }
    }
  }

  .counter-control {
    width: 164rpx;
    padding: 0 4rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: #f0f8ff;
    border-radius: 24rpx;
    opacity: 0;
    transform: translateX(18rpx) scale(0.92);
    pointer-events: none;
    box-shadow: inset 0 0 0 1rpx rgba(42, 139, 255, 0.06);

    .btn {
      position: relative;
      width: 48rpx;
      height: 48rpx;
      flex-shrink: 0;
      border-radius: 50%;

      &::before,
      &::after {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        border-radius: 999rpx;
        transform: translate(-50%, -50%);
      }

      &.minus::before {
        width: 18rpx;
        height: 4rpx;
        background: #2a8bff;
      }

      &.plus::before,
      &.plus::after {
        background: #2a8bff;
      }

      &.plus::before {
        width: 18rpx;
        height: 4rpx;
      }

      &.plus::after {
        width: 4rpx;
        height: 18rpx;
      }
    }

    .count {
      min-width: 36rpx;
      text-align: center;
      font-size: 28rpx;
      color: #333;
      font-weight: 700;
    }
  }
}
</style>
