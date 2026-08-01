<template>
  <view class="my-container">
    <!-- Top Section: Background & User Info -->
    <view class="top-section">
      <view class="top-visual-layer">
        <view class="top-decoration decoration-left"></view>
        <view class="top-decoration decoration-right"></view>
        <view class="top-decoration decoration-bottom"></view>
        <view class="top-grid"></view>
      </view>
      <view class="user-card">
        <view class="avatar">
          <image class="avatar-image" :src="displayAvatar" mode="aspectFill" @error="handleAvatarError" />
        </view>
        <view class="user-meta">
          <text class="nickname">{{ userStore.isLogin ? (userStore.userInfo?.name || '微信用户') : '点击登录' }}</text>
          <text class="welcome-text">{{ userStore.isLogin ? (userStore.userInfo?.phone || '欢迎回来，随时准备开吃！') : '登录后体验更多功能' }}</text>
        </view>
        <view v-if="!userStore.isLogin" class="login-mask" @tap="handleLogin"></view>
      </view>
    </view>
    <!-- Middle Section: Function List -->
    <view class="menu-list">
      <view
        class="menu-item"
        hover-class="menu-item-hover"
        @tap="goToOrders"
      >
        <uni-icons type="list" size="20" color="#2a8bff"></uni-icons>
        <text class="label">我的订单</text>
        <uni-icons type="right" size="14" color="#ccc"></uni-icons>
      </view>

      <view
        class="menu-item"
        hover-class="menu-item-hover"
        @tap="onAddressManage"
      >
        <uni-icons type="location" size="20" color="#2a8bff"></uni-icons>
        <text class="label">地址管理</text>
        <uni-icons type="right" size="14" color="#ccc"></uni-icons>
      </view>

      <view
        class="menu-item"
        hover-class="menu-item-hover"
        @tap="onFeedback"
      >
        <uni-icons type="chat" size="20" color="#ff7b2c"></uni-icons>
        <text class="label">提交建议</text>
        <uni-icons type="right" size="14" color="#ccc"></uni-icons>
      </view>

      <view
        class="menu-item"
        hover-class="menu-item-hover"
        @tap="onAbout"
      >
        <uni-icons type="info" size="20" color="#2a8bff"></uni-icons>
        <text class="label">关于我们</text>
        <uni-icons type="right" size="14" color="#ccc"></uni-icons>
      </view>

      <view
        class="menu-item"
        hover-class="menu-item-hover"
        @tap="onSettings"
      >
        <uni-icons type="gear" size="20" color="#2a8bff"></uni-icons>
        <text class="label">设置</text>
        <uni-icons type="right" size="14" color="#ccc"></uni-icons>
      </view>
    </view>
    <!-- Bottom Section: Brand & Logout -->
    <view class="bottom-section">
      <view class="brand-info">
        <text class="app-name">食刻快取</text>
      </view>

      <view
        v-if="userStore.isLogin"
        class="btn-logout"
        hover-class="btn-logout-hover"
        @tap="handleLogout"
      >
        退出登录
      </view>
    </view>
    <CustomTabBar :selected="3" />
    <!-- 提交建议弹窗 -->
    <uni-popup ref="feedbackPopup" type="center" :is-mask-click="true" mask-background-color="rgba(0,0,0,0.4)">
      <view class="feedback-popup-content">
        <view class="popup-header">
          <text class="popup-title">提交建议</text>
          <uni-icons type="closeempty" size="24" color="#999" @tap="closeFeedback"></uni-icons>
        </view>
        <view class="popup-body">
          <textarea
            class="feedback-textarea"
            v-model="feedbackContent"
            placeholder="请留下您宝贵的建议或遇到的问题..."
            :maxlength="500"
          ></textarea>
          <input
            class="feedback-input"
            v-model="feedbackContact"
            placeholder="您的联系方式（选填，手机号或微信）"
          />
        </view>
        <view class="popup-footer">
          <button class="btn-cancel" @tap="closeFeedback">取消</button>
          <button class="btn-submit" @tap="submitFeedback">提交</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { http } from '@/utils/http'
import { safeNavigateTo, safeSwitchTab, syncNativeTabBarHidden } from '@/utils/navigation'
import { usePageShare } from '@/utils/share'
import CustomTabBar from '@/components/CustomTabBar.vue'

const userStore = useUserStore()
const feedbackPopup = ref<any>(null)
const feedbackContent = ref('')
const feedbackContact = ref('')
const DEFAULT_AVATAR = '/static/images/default-avatar.png'
const avatarLoadFailed = ref(false)

usePageShare({
  title: '食刻快取｜校园食堂提前点单小程序',
  path: '/pages/my/my',
})

const normalizeAvatarUrl = (url: string) => {
  if (!url) {
    return ''
  }

  const trimmedUrl = String(url).trim()
  if (!trimmedUrl) {
    return ''
  }

  // 只接受明确可识别的本地或网络头像地址，异常值直接回退默认头像
  if (
    trimmedUrl.startsWith('/') ||
    trimmedUrl.startsWith('http://') ||
    trimmedUrl.startsWith('https://') ||
    trimmedUrl.startsWith('wxfile://') ||
    trimmedUrl.startsWith('cloud://')
  ) {
    return trimmedUrl
  }

  return ''
}

const customAvatar = computed(() => {
  const userInfo = userStore.userInfo || {}
  return normalizeAvatarUrl(
    userInfo.avatarUrl || userInfo.avatar || userInfo.headimgurl || userInfo.headImgUrl || ''
  )
})

const displayAvatar = computed(() => {
  if (customAvatar.value && !avatarLoadFailed.value) {
    return customAvatar.value
  }
  return DEFAULT_AVATAR
})

watch(customAvatar, () => {
  avatarLoadFailed.value = false
})

onShow(() => {
  syncNativeTabBarHidden()
})

const handleAvatarError = () => {
  if (!customAvatar.value || avatarLoadFailed.value) {
    return
  }
  avatarLoadFailed.value = true
}

const handleLogin = () => {
  safeNavigateTo({ url: '/pages/login/login' })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.clearUserInfo()
        // Optional: Redirect to home
        safeSwitchTab({ url: '/pages/index/index' })
      }
    }
  })
}


const goToOrders = () => {
  safeSwitchTab({ url: '/pages/order-list/order-list' })
}

const onAddressManage = () => {
  uni.showToast({ title: '地址管理功能开发中', icon: 'none' })
}

const onFeedback = () => {
  if (!userStore.isLogin) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  feedbackContent.value = ''
  feedbackContact.value = ''
  feedbackPopup.value?.open()
}

const closeFeedback = () => {
  feedbackPopup.value?.close()
}

const submitFeedback = async () => {
  if (!feedbackContent.value.trim()) {
    uni.showToast({ title: '请输入建议内容', icon: 'none' })
    return
  }

  try {
    uni.showLoading({ title: '提交中...' })
    await http({
      url: '/api/client/feedbacks',
      method: 'POST',
      data: {
        content: feedbackContent.value.trim(),
        contact: feedbackContact.value.trim()
      }
    })
    uni.hideLoading()
    uni.showToast({ title: '提交成功，感谢您的建议', icon: 'none' })
    closeFeedback()
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: error.message || '提交失败', icon: 'none' })
  }
}

const onAbout = () => {
  uni.showModal({
    title: '关于食刻快取',
    content: '版本号：6.0.0\n食刻快取 - 预约校园美食',
    showCancel: false
  })
}

const onSettings = () => {
  uni.showToast({ title: '设置功能开发中', icon: 'none' })
}
</script>

<style lang="scss">
.my-container {
  min-height: 100vh;
  background-color: #f8f9fa;
  display: flex;
  flex-direction: column;
}

/* Top Section */
.top-section {
  position: relative;
  height: 480rpx;
  background:
    radial-gradient(circle at top left, rgba(255,255,255,0.22) 0%, rgba(255,255,255,0) 36%),
    linear-gradient(135deg, #2a8bff 0%, #1a73e8 55%, #1363d1 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: calc(100rpx + var(--status-bar-height));
  border-bottom-left-radius: 60rpx;
  border-bottom-right-radius: 60rpx;
  margin-bottom: 60rpx;

  .brand-info {
    position: absolute;
    top: calc(30rpx + var(--status-bar-height));
    left: 40rpx;
    display: flex;
    align-items: center;

    .app-name {
      color: rgba(255,255,255,0.9);
      font-size: 32rpx;
      font-weight: bold;
      letter-spacing: 2rpx;
    }
  }
}

.top-visual-layer {
  position: absolute;
  inset: 0;
  border-bottom-left-radius: 60rpx;
  border-bottom-right-radius: 60rpx;
  overflow: hidden;
  pointer-events: none;
}

.top-decoration {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.decoration-left {
  width: 240rpx;
  height: 240rpx;
  top: 40rpx;
  left: -80rpx;
  background: rgba(255, 255, 255, 0.14);
  box-shadow: 0 0 80rpx rgba(255, 255, 255, 0.08);
}

.decoration-right {
  width: 180rpx;
  height: 180rpx;
  top: 120rpx;
  right: -40rpx;
  background: rgba(255, 255, 255, 0.12);
}

.decoration-bottom {
  width: 320rpx;
  height: 320rpx;
  right: 60rpx;
  bottom: 20rpx;
  background: radial-gradient(circle, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0) 70%);
}

.top-grid {
  position: absolute;
  top: 36rpx;
  right: 36rpx;
  width: 180rpx;
  height: 120rpx;
  opacity: 0.18;
  background-image:
    linear-gradient(rgba(255,255,255,0.7) 2rpx, transparent 2rpx),
    linear-gradient(90deg, rgba(255,255,255,0.7) 2rpx, transparent 2rpx);
  background-size: 30rpx 30rpx;
  transform: rotate(-12deg);
  pointer-events: none;
}

.user-card {
  position: absolute;
  bottom: -60rpx; /* Overlap effect */
  width: 690rpx;
  background-color: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
  z-index: 2;

  .avatar {
    width: 152rpx;
    height: 152rpx;
    border-radius: 50%;
    border: 6rpx solid rgba(255, 255, 255, 0.98);
    margin-top: -76rpx; /* Half overlapping top */
    background-color: #f5f5f5;
    box-shadow: 0 10rpx 24rpx rgba(18, 99, 209, 0.18);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  .avatar-image {
    width: 100%;
    height: 100%;
    display: block;
  }

  .user-meta {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 16rpx;

    .nickname {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 8rpx;
    }

    .welcome-text {
      font-size: 24rpx;
      color: #666;
    }
  }

  /* Invisible click area for login */
  .login-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
  }
}

/* Middle Section: Menu */
.menu-list {
  background-color: #fff;
  margin: 0 30rpx;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);

  .menu-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1rpx solid #f0f0f0;
    transition: background-color 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &.menu-item-hover {
      background-color: #f5f5f5;
    }

    .label {
      flex: 1;
      margin-left: 20rpx;
      font-size: 28rpx;
      color: #333;
    }
  }
}

/* Bottom Section */
.bottom-section {
  margin-top: 60rpx;
  padding-bottom: 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  .brand-info {
    display: flex;
    align-items: center;
    margin-bottom: 40rpx;

    .app-name {
      font-size: 24rpx;
      color: #999;
      font-weight: 500;
    }
  }

  .btn-logout {
    width: 300rpx;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    border: 2rpx solid #ff7b2c;
    color: #ff7b2c;
    border-radius: 40rpx;
    font-size: 28rpx;
    background-color: transparent;
    transition: all 0.2s;

    &.btn-logout-hover {
      background-color: rgba(255, 123, 44, 0.05);
    }
  }
}

/* Feedback Popup */
.feedback-popup-content {
  width: 600rpx;
  background-color: #fff;
  border-radius: 24rpx;
  overflow: hidden;

  .popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1rpx solid #eee;

    .popup-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
    }
  }

  .popup-body {
    padding: 30rpx;

    .feedback-textarea {
      width: 100%;
      height: 240rpx;
      background-color: #f8f9fa;
      border-radius: 12rpx;
      padding: 20rpx;
      font-size: 28rpx;
      color: #333;
      box-sizing: border-box;
      margin-bottom: 20rpx;
    }

    .feedback-input {
      width: 100%;
      height: 80rpx;
      background-color: #f8f9fa;
      border-radius: 12rpx;
      padding: 0 20rpx;
      font-size: 28rpx;
      color: #333;
      box-sizing: border-box;
    }
  }

  .popup-footer {
    display: flex;
    border-top: 1rpx solid #eee;

    button {
      flex: 1;
      height: 100rpx;
      line-height: 100rpx;
      text-align: center;
      font-size: 30rpx;
      background-color: transparent;
      border: none;
      border-radius: 0;
      margin: 0;
      padding: 0;

      &::after {
        display: none;
      }
    }

    .btn-cancel {
      color: #666;
      border-right: 1rpx solid #eee;
    }

    .btn-submit {
      color: #2a8bff;
      font-weight: bold;
    }
  }
}
</style>
