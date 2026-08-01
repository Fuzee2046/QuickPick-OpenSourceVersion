<template>
  <view class="container">
    <!-- Header with Search -->
    <view class="header-container">
      <view class="header-bg">
        <view class="bg-gradient"></view>
        <view class="bg-mask"></view>
        <view class="bg-orb orb-left"></view>
        <view class="bg-orb orb-right"></view>
        <view class="bg-orb orb-bottom"></view>
        <view class="bg-grid"></view>
      </view>
      <view class="header-content">
        <view class="location-row">
          <uni-icons type="location-filled" size="20" color="#fff"></uni-icons>
          <text class="location-text">南昌航空大学</text>
          <uni-icons
            type="right"
            size="14"
            color="#fff"
            style="opacity: 0.8; margin-left: 4rpx"
          ></uni-icons>
        </view>
        <view class="search-bar" hover-class="search-bar-hover" @tap="handleSearchComingSoon">
          <uni-icons type="search" size="18" color="#999"></uni-icons>
          <text class="search-placeholder">搜索想吃的店铺或美食...</text>
        </view>
      </view>
    </view>

    <!-- Promotional Banner -->
    <view class="banner-section">
      <swiper
        class="banner-swiper"
        circular
        autoplay
        interval="4000"
        indicator-dots
        indicator-active-color="#fff"
        indicator-color="rgba(255,255,255,0.5)"
      >
        <swiper-item>
          <view class="banner-item item-1">
            <view class="banner-text">
              <text class="banner-title">校园店铺陆续入驻中</text>
              <text class="banner-desc">项目为起步阶段，更多校园店铺将陆续加入</text>
            </view>
            <uni-icons
              type="sound-filled"
              size="80"
              color="rgba(255,255,255,0.8)"
              class="banner-icon"
            ></uni-icons>
            <view class="banner-decoration"></view>
          </view>
        </swiper-item>
        <swiper-item>
          <view class="banner-item item-2">
            <view class="banner-text">
              <text class="banner-title">免单活动开放中</text>
              <text class="banner-desc">预约参与赢免单，试试今天的好运气</text>
            </view>
            <uni-icons
              type="gift-filled"
              size="80"
              color="rgba(255,255,255,0.8)"
              class="banner-icon"
            ></uni-icons>
            <view class="banner-decoration"></view>
          </view>
        </swiper-item>
        <swiper-item>
          <view class="banner-item item-3">
            <view class="banner-text">
              <text class="banner-title">下课先点餐，到店直接取</text>
              <text class="banner-desc">减少排队等待，按时取餐、诚信下单</text>
            </view>
            <uni-icons
              type="calendar-filled"
              size="80"
              color="rgba(255,255,255,0.8)"
              class="banner-icon"
            ></uni-icons>
            <view class="banner-decoration"></view>
          </view>
        </swiper-item>
      </swiper>
    </view>

    <!-- Canteen List -->

    <!-- Canteen Filter (Sticky) -->
    <view class="filter-sticky-wrapper">
      <scroll-view
        class="canteen-scroll"
        scroll-x
        enable-flex
        show-scrollbar="false"
        :scroll-left="scrollLeft"
        scroll-with-animation
      >
        <view class="canteen-list">
          <!-- Skeleton Loading -->
          <template v-if="loading">
            <view v-for="i in 4" :key="i" class="canteen-item skeleton"></view>
          </template>

          <template v-else>
            <view
              class="canteen-item"
              :class="{ active: currentCanteenId === null }"
              @tap="onCanteenChange(null, 0)"
              id="canteen-0"
            >
              <text class="canteen-name">全部食堂</text>
            </view>
            <view
              v-for="(canteen, index) in canteens"
              :key="canteen.id"
              class="canteen-item"
              :class="{ active: currentCanteenId === canteen.id }"
              @tap="onCanteenChange(canteen.id, index + 1)"
              :id="`canteen-${index + 1}`"
            >
              <text class="canteen-name">{{ canteen.name }}</text>
            </view>
          </template>
        </view>
      </scroll-view>
    </view>

    <!-- Shop List -->
    <view class="shop-list-section">
      <view class="section-header">
        <text class="section-title">优选店铺</text>
        <text class="section-subtitle">品质好店 放心点餐</text>
      </view>

      <view class="shop-list">
        <ShopCard v-for="shop in displayedShops" :key="shop.id" :shop="shop" @click="goToMenu" />

        <view v-if="hasMoreShops" class="shop-list-loading-more">
          <text>更多店铺正在加载...</text>
        </view>

        <!-- Empty State for Shops -->
        <view v-if="shops.length === 0 && !loading" class="empty-shop">
        <uni-icons type="shop" size="80" color="#ccc" class="empty-icon"></uni-icons>
        <text>该食堂暂无店铺入驻</text>
        <text class="sub-text">去看看其他食堂吧</text>
        </view>
      </view>
    </view>

    <CustomTabBar :selected="0" />
    <BrandLoadingOverlay
      :visible="pageLoadingVisible"
      :title="pageLoadingTitle"
      :description="pageLoadingDescription"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { http } from '@/utils/http'
import { safeNavigateTo, syncNativeTabBarHidden } from '@/utils/navigation'
import { usePageShare } from '@/utils/share'
import ShopCard from '@/components/ShopCard.vue'
import CustomTabBar from '@/components/CustomTabBar.vue'
import BrandLoadingOverlay from '@/components/BrandLoadingOverlay.vue'
import { useBrandLoading } from '@/composables/useBrandLoading'

const shops = ref<any[]>([])
const canteens = ref<any[]>([])
const currentCanteenId = ref<number | null>(null)
const loading = ref(true)
const scrollLeft = ref(0)
const lastFetchTime = ref(0)
const hasLoadedInitialData = ref(false)
const isFetchingShops = ref(false)
const INITIAL_VISIBLE_SHOP_COUNT = 6
const displayedShopCount = ref(INITIAL_VISIBLE_SHOP_COUNT)
let shopRenderTimer: ReturnType<typeof setTimeout> | null = null
const {
  visible: pageLoadingVisible,
  title: pageLoadingTitle,
  description: pageLoadingDescription,
  show: showPageLoading,
  hide: hidePageLoading
} = useBrandLoading({
  title: '校园店铺正在赶来',
  description: '正在同步食堂与店铺信息，请稍候',
})

usePageShare({
  title: '食刻快取｜下课先点餐，到店直接取',
  path: '/pages/index/index',
})

const displayedShops = computed(() => shops.value.slice(0, displayedShopCount.value))
const hasMoreShops = computed(() => shops.value.length > displayedShopCount.value)

const scheduleRenderRemainingShops = () => {
  if (shopRenderTimer) {
    clearTimeout(shopRenderTimer)
  }

  displayedShopCount.value = Math.min(INITIAL_VISIBLE_SHOP_COUNT, shops.value.length)
  if (shops.value.length <= INITIAL_VISIBLE_SHOP_COUNT) {
    return
  }

  shopRenderTimer = setTimeout(() => {
    displayedShopCount.value = shops.value.length
    shopRenderTimer = null
  }, 120)
}

const fetchCanteens = async () => {
  try {
    const res = await http<any[]>({
      url: '/api/client/canteens',
      method: 'GET',
      hideErrorToast: true,
      cacheTtlMs: 5 * 60 * 1000,
      cacheKey: 'client-canteens',
    })
    canteens.value = res.data
    loading.value = false
  } catch (e) {
    console.error('Fetch canteens failed', e)
    canteens.value = []
    uni.showToast({
      title: '服务暂不可用，请稍后重试',
      icon: 'none',
    })
    loading.value = false
  }
}

const fetchShops = async () => {
  if (isFetchingShops.value) {
    return
  }

  const data: any = {}
  if (currentCanteenId.value !== null) {
    data.canteenId = currentCanteenId.value
  }

  try {
    isFetchingShops.value = true
    const res = await http<any[]>({
      url: '/api/client/shops',
      method: 'GET',
      data,
      hideErrorToast: true,
    })
    shops.value = Array.isArray(res.data) ? res.data : []
    scheduleRenderRemainingShops()
    lastFetchTime.value = Date.now()
  } catch (e) {
    console.error('Fetch shops failed', e)
    shops.value = []
    displayedShopCount.value = 0
    uni.showToast({
      title: '店铺加载失败，请稍后重试',
      icon: 'none',
    })
  } finally {
    isFetchingShops.value = false
  }
}

const onCanteenChange = (id: number | null, index: number) => {
  if (currentCanteenId.value === id) return
  currentCanteenId.value = id
  fetchShops()
  scrollToCenter(index)
}

const scrollToCenter = (index: number) => {
  // Simple heuristic for scrolling
  const itemWidth = 100 // approx width in px
  const screenWidth = uni.getSystemInfoSync().windowWidth
  scrollLeft.value = index * itemWidth - screenWidth / 2 + itemWidth / 2
}

const goToMenu = (shop: any) => {
  if (shop.billingServiceAvailable === false) {
    uni.showToast({ title: '商户服务暂停，暂时无法下单', icon: 'none' })
    return
  }
  safeNavigateTo({
    url: `/pages/menu/menu?shopId=${shop.id}`,
  })
}

const handleSearchComingSoon = () => {
  uni.showToast({
    title: '搜索功能正在开发中，敬请期待',
    icon: 'none',
  })
}

onMounted(async () => {
  showPageLoading()
  try {
    await Promise.all([fetchCanteens(), fetchShops()])
  } finally {
    hidePageLoading(true)
    hasLoadedInitialData.value = true
  }
})

// 页面显示时刷新店铺列表，确保营业状态及时更新
onShow(() => {
  syncNativeTabBarHidden()

  if (!hasLoadedInitialData.value) {
    return
  }

  // 如果距离上次获取数据超过30秒，则重新获取
  if (lastFetchTime.value === 0 || Date.now() - lastFetchTime.value > 30000) {
    fetchShops()
  }
})

// 下拉刷新
onPullDownRefresh(() => {
  fetchShops().finally(() => {
    uni.stopPullDownRefresh()
  })
})

onUnmounted(() => {
  if (shopRenderTimer) {
    clearTimeout(shopRenderTimer)
  }
})
</script>

<style lang="scss">
page {
  --primary-color: #2a8bff;
  --accent-color: #ff6b35;
  --bg-color: #f5f7fa;
}

.container {
  min-height: 100vh;
  background-color: var(--bg-color);
  padding-bottom: 120rpx;
}

/* Header Section */
.header-container {
  position: relative;
  height: calc(340rpx + var(--status-bar-height));
  background-color: var(--primary-color);
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
  overflow: hidden;
  z-index: 1;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;

  .bg-gradient {
    width: 100%;
    height: 100%;
    background: radial-gradient(
        circle at top left,
        rgba(255, 255, 255, 0.2) 0%,
        rgba(255, 255, 255, 0) 32%
      ),
      linear-gradient(135deg, #2a8bff 0%, #1a73e8 70%, #1363d1 100%);
  }

  .bg-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(to bottom, rgba(42, 139, 255, 0.8), rgba(42, 139, 255, 1));
  }

  .bg-orb {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
  }

  .orb-left {
    width: 240rpx;
    height: 240rpx;
    top: 30rpx;
    left: -70rpx;
    background: rgba(255, 255, 255, 0.12);
  }

  .orb-right {
    width: 180rpx;
    height: 180rpx;
    top: 110rpx;
    right: -30rpx;
    background: rgba(255, 255, 255, 0.1);
  }

  .orb-bottom {
    width: 320rpx;
    height: 320rpx;
    right: 80rpx;
    bottom: -110rpx;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0) 70%);
  }

  .bg-grid {
    position: absolute;
    top: 46rpx;
    right: 34rpx;
    width: 180rpx;
    height: 120rpx;
    opacity: 0.16;
    background-image: linear-gradient(rgba(255, 255, 255, 0.7) 2rpx, transparent 2rpx),
      linear-gradient(90deg, rgba(255, 255, 255, 0.7) 2rpx, transparent 2rpx);
    background-size: 30rpx 30rpx;
    transform: rotate(-12deg);
    pointer-events: none;
  }
}

.header-content {
  position: relative;
  z-index: 2;
  padding: 20rpx 30rpx;
  padding-top: calc(102rpx + var(--status-bar-height));

  .location-row {
    display: flex;
    align-items: center;
    margin-bottom: 30rpx;

    .location-text {
      font-size: 34rpx;
      font-weight: bold;
      color: #fff;
      margin-left: 8rpx;
      margin-right: 4rpx;
      letter-spacing: 1rpx;
    }
  }

  .search-bar {
    height: 72rpx;
    background-color: #fff;
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    padding: 0 24rpx;
    box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    .search-placeholder {
      font-size: 28rpx;
      color: #999;
      margin-left: 12rpx;
    }
  }

  .search-bar-hover {
    transform: scale(0.99);
    box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.12);
  }
}

/* Banner Section */
.banner-section {
  position: relative;
  margin-top: -60rpx;
  padding: 0 30rpx;
  z-index: 2;
  margin-bottom: 20rpx;
}

.banner-swiper {
  height: 220rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);

  .banner-item {
    width: 100%;
    height: 100%;
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 40rpx;
    box-sizing: border-box;
    overflow: hidden;

    &.item-1 {
      background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
      .banner-title {
        color: #0284c7;
      }
      .banner-desc {
        color: #0ea5e9;
        background: rgba(255, 255, 255, 0.6);
      }
    }

    &.item-2 {
      background: linear-gradient(135deg, #ffedd5 0%, #fed7aa 100%);
      .banner-title {
        color: #c2410c;
      }
      .banner-desc {
        color: #ea580c;
        background: rgba(255, 255, 255, 0.6);
      }
    }

    &.item-3 {
      background: linear-gradient(135deg, #efe7ff 0%, #d9ccff 100%);
      .banner-title {
        color: #6d28d9;
      }
      .banner-desc {
        color: #7c3aed;
        background: rgba(255, 255, 255, 0.62);
      }
    }

    .banner-text {
      display: flex;
      flex-direction: column;
      z-index: 2;

      .banner-title {
        font-size: 40rpx;
        font-weight: 800;
        margin-bottom: 12rpx;
      }

      .banner-desc {
        font-size: 24rpx;
        padding: 4rpx 12rpx;
        border-radius: 20rpx;
        align-self: flex-start;
        font-weight: 500;
      }
    }

    .banner-icon {
      margin-right: 20rpx;
      z-index: 2;
    }

    .banner-decoration {
      position: absolute;
      right: -20rpx;
      bottom: -40rpx;
      width: 200rpx;
      height: 200rpx;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      z-index: 1;
    }
  }
}

/* Filter Sticky */
.filter-sticky-wrapper {
  position: sticky;
  top: 0; /* Adjust if header is fixed, but here header scrolls away partially? No, let's keep it simple */
  /* If header scrolls, sticky top needs to be adjusted or placed after scrollable area. */
  /* For simple sticky effect in page flow: */
  z-index: 99;
  background-color: var(--bg-color);
  padding: 14rpx 0 16rpx;
}

.canteen-scroll {
  width: 100%;
  white-space: nowrap;

  .canteen-list {
    display: flex;
    padding: 4rpx 30rpx;
    min-height: 88rpx;

    .canteen-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 18rpx 34rpx;
      background-color: #fff;
      border-radius: 36rpx;
      margin-right: 20rpx;
      transition: all 0.3s;
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.02);
      border: 2rpx solid transparent;

      .canteen-name {
        font-size: 28rpx;
        color: #666;
        font-weight: 500;
      }

      &.active {
        background-color: var(--primary-color);
        box-shadow: 0 4rpx 12rpx rgba(42, 139, 255, 0.3);
        transform: translateY(-2rpx);

        .canteen-name {
          color: #fff;
          font-weight: 600;
        }
      }

      &:last-child {
        margin-right: 0;
      }

      &.skeleton {
        width: 140rpx;
        height: 68rpx;
        background-color: #e2e8f0;
      }
    }
  }
}

/* Shop List Section */
.shop-list-section {
  padding: 20rpx 30rpx;

  .section-header {
    display: flex;
    align-items: baseline;
    margin-bottom: 24rpx;
    margin-left: 8rpx;

    .section-title {
      font-size: 36rpx;
      font-weight: 800;
      color: #333;
      margin-right: 12rpx;
    }

    .section-subtitle {
      font-size: 24rpx;
      color: #999;
    }
  }

  .shop-list {
    .shop-list-loading-more {
      display: flex;
      justify-content: center;
      padding: 10rpx 0 24rpx;

      text {
        font-size: 24rpx;
        color: #94a3b8;
      }
    }

    .empty-shop {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding-top: 80rpx;

      .empty-icon {
        margin-bottom: 20rpx;
        opacity: 0.6;
      }

      text {
        font-size: 30rpx;
        color: #333;
        font-weight: 600;
        margin-bottom: 8rpx;
      }

      .sub-text {
        font-size: 24rpx;
        color: #999;
        font-weight: normal;
      }
    }
  }
}
</style>
