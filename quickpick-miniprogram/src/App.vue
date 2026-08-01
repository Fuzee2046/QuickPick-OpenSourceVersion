<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { syncNativeTabBarHidden } from '@/utils/navigation'

onLaunch(() => {
  syncNativeTabBarHidden()
})

onShow(() => {
  syncNativeTabBarHidden()
})
</script>

<style lang="scss">
// 字体图标

view,
navigator,
input,
scroll-view {
  box-sizing: border-box;
}

button::after {
  border: none;
}

swiper,
scroll-view {
  flex: 1;
  height: 100%;
  overflow: hidden;
}

image {
  width: 100%;
  height: 100%;
  vertical-align: middle;
}

// 两行省略
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
