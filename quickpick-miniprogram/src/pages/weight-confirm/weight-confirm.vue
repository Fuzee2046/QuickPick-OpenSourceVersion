<template>
  <view class="compat-page"></view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { safeRedirectTo } from '@/utils/navigation'

onLoad((options: any) => {
  const query = options?.shopId ? `&shopId=${options.shopId}` : ''
  safeRedirectTo({
    url: `/pages/confirm/confirm?mode=weight_selection${query}`
  })
})
</script>

<style lang="scss">
.compat-page {
  min-height: 100vh;
  background: #f5f7fa;
}
</style>
