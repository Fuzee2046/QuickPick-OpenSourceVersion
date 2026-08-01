<template>
  <view class="login-page">
    <view class="nav-bar">
      <view class="nav-back" hover-class="nav-back--active" @tap="goBack">
        <uni-icons type="left" size="20" color="#1d4ed8"></uni-icons>
      </view>
      <text class="nav-title">食刻快取</text>
      <view class="nav-placeholder"></view>
    </view>

    <view class="hero-card">
      <view class="avatar-shell">
        <image class="avatar" src="/static/images/default-avatar.png" mode="aspectFill" />
      </view>
      <text class="hero-title">登录账号，立享校园预约点餐</text>
      <text class="hero-desc">提前下单，到店即取，减少排队等待时间</text>
    </view>

    <view class="form-card">
      <view class="field-label">你的姓名</view>
      <input
        v-model.trim="name"
        class="name-input"
        type="text"
        maxlength="20"
        placeholder="请输入真实姓名，用于订单取餐核验"
        placeholder-class="name-input__placeholder"
      />

      <button
        class="login-btn"
        :class="{ 'login-btn--disabled': !canQuickLogin }"
        :disabled="!canQuickLogin || loginLoading"
        :loading="loginLoading"
        open-type="getPhoneNumber"
        @getphonenumber="onGetPhoneNumber"
      >
        一键登录
      </button>

      <view class="agreement-row" @tap="toggleAgreement">
        <view class="agreement-check" :class="{ 'agreement-check--active': agreementChecked }">
          <uni-icons
            v-if="agreementChecked"
            type="checkmarkempty"
            size="14"
            color="#ffffff"
          ></uni-icons>
        </view>
        <view class="agreement-copy">
          <text class="agreement-prefix">我已阅读并同意</text>
          <text class="agreement-link" @tap.stop="navigateToAgreement('service')"
            >《用户服务协议》</text
          >
          <text class="agreement-prefix">和</text>
          <text class="agreement-link" @tap.stop="navigateToAgreement('privacy')"
            >《隐私政策》</text
          >
        </view>
      </view>

      <view class="skip-btn" hover-class="skip-btn--active" @tap="skipLogin"> 暂不登录 </view>
    </view>

    <BrandLoadingOverlay
      :visible="pageLoadingVisible"
      :title="pageLoadingTitle"
      :description="pageLoadingDescription"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { http } from '@/utils/http'
import { safeNavigateTo, safeSwitchTab, smartNavigateBack } from '@/utils/navigation'
import BrandLoadingOverlay from '@/components/BrandLoadingOverlay.vue'
import { useBrandLoading } from '@/composables/useBrandLoading'

const userStore = useUserStore()
const name = ref('')
const loginCode = ref('')
const agreementChecked = ref(false)
const loginLoading = ref(false)
const {
  visible: pageLoadingVisible,
  title: pageLoadingTitle,
  description: pageLoadingDescription,
  show: showPageLoading,
  hide: hidePageLoading
} = useBrandLoading({
  title: '登录信息正在核验',
  description: '请稍候，正在为你接入校园点餐服务'
})

const canQuickLogin = computed(() => agreementChecked.value && !!name.value)

const getLoginCode = (retryCount = 0, maxRetries = 2): Promise<string> => {
  return new Promise((resolve, reject) => {
    const timeoutId = setTimeout(() => {
      if (retryCount < maxRetries) {
        getLoginCode(retryCount + 1, maxRetries)
          .then(resolve)
          .catch(reject)
      } else {
        reject(new Error('获取登录凭证超时，请检查网络后重试'))
      }
    }, 15000)

    uni.login({
      provider: 'weixin',
      success: (res) => {
        clearTimeout(timeoutId)
        if (!res.code) {
          if (retryCount < maxRetries) {
            getLoginCode(retryCount + 1, maxRetries)
              .then(resolve)
              .catch(reject)
            return
          }
          reject(new Error('微信登录未返回有效 code'))
          return
        }
        loginCode.value = res.code
        resolve(res.code)
      },
      fail: (err) => {
        clearTimeout(timeoutId)
        if (retryCount < maxRetries) {
          getLoginCode(retryCount + 1, maxRetries)
            .then(resolve)
            .catch(reject)
          return
        }
        reject(err)
      },
      complete: () => {
        clearTimeout(timeoutId)
      },
    })
  })
}

const ensureLoginCode = async () => {
  if (loginCode.value) {
    return loginCode.value
  }
  return await getLoginCode()
}

const refreshLoginCode = () => {
  void getLoginCode().catch((err) => {
    console.error('refresh login code failed', err)
  })
}

const goBack = () => {
  smartNavigateBack({ fallbackTab: 'pages/my/my' })
}

const skipLogin = () => {
  safeSwitchTab({ url: '/pages/my/my' })
}

const toggleAgreement = () => {
  agreementChecked.value = !agreementChecked.value
}

const navigateToAgreement = (type: 'service' | 'privacy') => {
  safeNavigateTo({
    url:
      type === 'service' ? '/pages/agreement/service-agreement' : '/pages/agreement/privacy-policy',
  })
}

const onGetPhoneNumber = async (e: any) => {
  if (!name.value) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }

  if (!agreementChecked.value) {
    uni.showToast({ title: '请先勾选相关协议', icon: 'none' })
    return
  }

  if (!(e?.detail?.errMsg || '').includes('ok') || !e?.detail?.code) {
    uni.showToast({ title: '你已取消授权，可稍后再登录', icon: 'none' })
    return
  }

  try {
    loginLoading.value = true
    showPageLoading({ delay: 180 })

    const currentLoginCode = await ensureLoginCode()
    const res = await http<any>({
      url: '/api/auth/login',
      method: 'POST',
      data: {
        code: currentLoginCode,
        phoneCode: e.detail.code,
        name: name.value,
      },
    })

    hidePageLoading(true)

    if (res.code !== 200) {
      uni.showToast({ title: res.msg || '登录失败，请重试', icon: 'none' })
      refreshLoginCode()
      return
    }

    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)
    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      smartNavigateBack({ fallbackTab: 'pages/my/my' })
    }, 1200)
  } catch (err) {
    hidePageLoading(true)
    console.error('login failed', err)
    uni.showToast({ title: '登录请求失败，请重试', icon: 'none' })
    refreshLoginCode()
  } finally {
    loginLoading.value = false
  }
}

onLoad(() => {
  refreshLoginCode()
})
</script>

<style lang="scss">
page {
  background: linear-gradient(180deg, #edf5ff 0%, #f7fbff 42%, #ffffff 100%);
}

.login-page {
  min-height: 100vh;
  padding: calc(24rpx + var(--status-bar-height)) 28rpx 48rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40rpx;
}

.nav-back,
.nav-placeholder {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-back {
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8rpx 24rpx rgba(29, 78, 216, 0.08);
}

.nav-back--active {
  transform: scale(0.96);
}

.nav-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1e3a8a;
  letter-spacing: 2rpx;
}

.hero-card {
  padding: 108rpx 24rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-shell {
  width: 144rpx;
  height: 144rpx;
  border-radius: 50%;
  padding: 8rpx;
  background: #dbeafe;
  box-sizing: border-box;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: block;
  background: #ffffff;
}

.hero-title {
  margin-top: 56rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #1e3a8a;
  text-align: center;
}

.hero-desc {
  margin-top: 28rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #64748b;
  text-align: center;
}

.form-card {
  flex: 1;
  margin-top: 56rpx;
  padding: 28rpx 10rpx 0;
  display: flex;
  flex-direction: column;
}

.field-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #334155;
  margin-bottom: 18rpx;
}

.name-input {
  width: 100%;
  height: 96rpx;
  padding: 0 28rpx;
  border-radius: 20rpx;
  background: #f8fbff;
  border: 2rpx solid #dbeafe;
  font-size: 30rpx;
  color: #0f172a;
  box-sizing: border-box;
}

.name-input__placeholder {
  color: #94a3b8;
}

.login-btn {
  width: 100%;
  margin-top: 32rpx;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 999rpx;
  border: none;
  font-size: 32rpx;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #2a8bff 0%, #1d4ed8 100%);
  box-shadow: 0 14rpx 28rpx rgba(42, 139, 255, 0.22);
}

.login-btn::after {
  border: none;
}

.login-btn--disabled {
  background: #bfdbfe;
  box-shadow: none;
  color: rgba(255, 255, 255, 0.92);
}

.agreement-row {
  display: flex;
  align-items: flex-start;
  margin-top: 28rpx;
}

.agreement-check {
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  margin-top: 4rpx;
  margin-right: 14rpx;
  border: 2rpx solid #cbd5e1;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.agreement-check--active {
  border-color: #2a8bff;
  background: #2a8bff;
}

.agreement-copy {
  flex: 1;
  font-size: 23rpx;
  line-height: 1.8;
}

.agreement-prefix {
  color: #64748b;
}

.agreement-link {
  color: #2563eb;
}

.skip-btn {
  margin-top: auto;
  padding-top: 140rpx;
  text-align: center;
  font-size: 26rpx;
  color: #94a3b8;
  line-height: 1.6;
}

.skip-btn--active {
  opacity: 0.7;
}
</style>
