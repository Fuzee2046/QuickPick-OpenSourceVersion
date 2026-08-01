<template>
  <view v-if="visible" class="brand-loading">
    <view class="brand-loading__backdrop"></view>

    <view class="brand-loading__dots">
      <view class="brand-loading__dot"></view>
      <view class="brand-loading__dot"></view>
      <view class="brand-loading__dot"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    visible: boolean
    title?: string
    description?: string
  }>(),
  {
    title: '正在为你准备中',
    description: '网络稍慢时，请稍候片刻'
  }
)
</script>

<style lang="scss">
.brand-loading {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-loading__backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at center, rgba(98, 182, 255, 0.18), transparent 42%),
    linear-gradient(180deg, rgba(245, 250, 255, 0.9) 0%, rgba(234, 244, 255, 0.96) 100%);
  backdrop-filter: blur(8rpx);
}

.brand-loading__dots {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.brand-loading__dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, #7ec8ff 0%, #2a8bff 100%);
  box-shadow:
    0 4rpx 10rpx rgba(42, 139, 255, 0.16),
    0 0 0 3rpx rgba(126, 200, 255, 0.06);
  animation: dotBounce 1s ease-in-out infinite;
}

.brand-loading__dot:nth-child(2) {
  animation-delay: 0.14s;
}

.brand-loading__dot:nth-child(3) {
  animation-delay: 0.28s;
}

@keyframes dotBounce {
  0%,
  100% {
    transform: translateY(0) scale(0.92);
    opacity: 0.52;
  }
  50% {
    transform: translateY(-10rpx) scale(1.08);
    opacity: 1;
  }
}
</style>
