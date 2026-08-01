<template>
  <view class="tab-bar-placeholder"></view>
  <view class="custom-tab-bar">
    <view
      v-for="(item, index) in list"
      :key="index"
      class="tab-bar-item"
      @click="switchTab(item, index)"
    >
      <view class="icon-box">
        <uni-icons
          :type="selected === index ? item.selectedIcon : item.icon"
          size="30"
          :color="selected === index ? selectedColor : color"
        ></uni-icons>
      </view>
      <text
        class="tab-text"
        :style="{ color: selected === index ? selectedColor : color }"
      >
        {{ item.text }}
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { safeSwitchTab } from '@/utils/navigation'

const props = defineProps<{
  selected: number
}>()

const color = '#666666'
const selectedColor = '#2a8ffb'
const list = [
  {
    pagePath: '/pages/index/index',
    icon: 'home',
    selectedIcon: 'home-filled',
    text: '点单'
  },
  {
    pagePath: '/pages/free-meal/free-meal',
    icon: 'fire',
    selectedIcon: 'fire-filled',
    text: '免单'
  },
  {
    pagePath: '/pages/order-list/order-list',
    icon: 'list',
    selectedIcon: 'list',
    text: '订单'
  },
  {
    pagePath: '/pages/my/my',
    icon: 'person',
    selectedIcon: 'person-filled',
    text: '我的'
  }
]

const switchTab = (item: any, index: number) => {
  if (props.selected === index) {
    return
  }

  safeSwitchTab({
    url: item.pagePath
  })
}
</script>

<style lang="scss" scoped>
.tab-bar-placeholder {
  height: calc(76px + constant(safe-area-inset-bottom));
  height: calc(76px + env(safe-area-inset-bottom));
}

.custom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: calc(76px + constant(safe-area-inset-bottom));
  height: calc(76px + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background-color: #ffffff;
  display: flex;
  padding-top: 4px;
  padding-bottom: calc(4px + constant(safe-area-inset-bottom));
  padding-bottom: calc(4px + env(safe-area-inset-bottom));
  border-top: 1px solid #eeeeee;
  z-index: 999;

  .tab-bar-item {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;

    .icon-box {
      width: 32px;
      height: 32px;
      flex: 0 0 32px;
      display: flex;
      justify-content: center;
      align-items: center;
      margin-top: 0;
    }

    .tab-text {
      font-size: 12px;
      margin-top: 2px;
      line-height: 14px;
    }
  }
}
</style>
