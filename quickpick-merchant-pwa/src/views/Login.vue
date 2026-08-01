<template>
  <div class="login-container">
    <main class="login-panel">
      <div class="header">
        <img class="header-logo" src="/favicon.ico" alt="食刻快取" />
        <h1>食刻快取</h1>
        <p>商户与运营管理端</p>
      </div>
      <div class="login-mode" role="tablist" aria-label="登录身份">
        <button
          type="button"
          class="login-mode__option"
          :class="{ 'login-mode__option--active': loginMode === 'merchant' }"
          :aria-selected="loginMode === 'merchant'"
          role="tab"
          @click="loginMode = 'merchant'"
        >
          <van-icon name="shop-o" />
          <span>商户登录</span>
        </button>
        <button
          type="button"
          class="login-mode__option"
          :class="{ 'login-mode__option--active': loginMode === 'admin' }"
          :aria-selected="loginMode === 'admin'"
          role="tab"
          @click="loginMode = 'admin'"
        >
          <van-icon name="manager-o" />
          <span>管理员登录</span>
        </button>
      </div>

      <van-form class="login-form" @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="identifier"
            name="identifier"
            label="账号"
            :placeholder="loginMode === 'admin' ? '管理员账号' : '店铺ID或手机号'"
            :rules="[{ required: true, message: '请输入账号' }]"
          />
          <van-field
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            label="密码"
            placeholder="请输入密码"
            :right-icon="showPassword ? 'eye-o' : 'closed-eye'"
            :rules="[{ required: true, message: '请输入密码' }]"
            @click-right-icon="showPassword = !showPassword"
          />
        </van-cell-group>

        <div class="login-actions">
          <van-button round block type="primary" native-type="submit" :loading="loading">
            登录
          </van-button>
        </div>
      </van-form>

      <button
        class="portfolio-link"
        :class="{ 'portfolio-link--attention': portfolioEntryAttention }"
        type="button"
        @click="router.push('/portfolio')"
      >
        <van-icon name="description" />
        <span>面试官查看项目作品集</span>
        <van-icon name="arrow" />
      </button>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import request from '@/utils/request';
import { showSuccessToast } from 'vant';
import { enableNotificationSound } from '@/utils/notificationSound';

const identifier = ref('');
const password = ref('');
const showPassword = ref(false);
const loading = ref(false);
const loginMode = ref<'merchant' | 'admin'>('merchant');
const portfolioEntryAttention = ref(false);
const router = useRouter();
const auth = useAuthStore();

onMounted(() => {
  const storageKey = 'quickpick_portfolio_entry_seen_v1';
  try {
    if (!localStorage.getItem(storageKey)) {
      portfolioEntryAttention.value = true;
      localStorage.setItem(storageKey, '1');
    }
  } catch {
    portfolioEntryAttention.value = true;
  }
});

const onSubmit = async () => {
  void enableNotificationSound();
  loading.value = true;
  try {
    const endpoint = loginMode.value === 'admin' ? '/api/admin/auth/login' : '/api/merchant/auth/login';
    const payload = loginMode.value === 'admin'
      ? { username: identifier.value, password: password.value }
      : { identifier: identifier.value, password: password.value };
    const res: any = await request.post(endpoint, payload);
    auth.setAuth(res);
    showSuccessToast('登录成功');
    
    if (res.requirePasswordChange) {
      router.replace('/change-password');
    } else if (res.role === 'admin') {
      router.replace('/admin/dashboard');
    } else {
      router.replace('/dashboard');
    }
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  padding: 32px;
  display: grid;
  place-items: center;
  box-sizing: border-box;
  background: #f5f7fa;
}
.login-panel {
  width: min(460px, 100%);
  padding: 38px 38px 26px;
  box-sizing: border-box;
  border: 1px solid #e2e7ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(47, 66, 91, .1);
}
.header {
  text-align: center;
  margin-bottom: 30px;
}
.header h1 {
  margin: 16px 0 0;
  font-size: 24px;
  color: #323233;
  line-height: 1.35;
}
.header p {
  margin: 6px 0 0;
  color: #8b95a5;
  font-size: 12px;
}
.header-logo {
  width: 78px;
  height: 78px;
  display: block;
  margin: 0 auto;
  border-radius: 20px;
  object-fit: cover;
  box-shadow: 0 8px 22px rgba(31, 87, 174, .2);
}
.login-mode {
  width: 100%;
  margin: 0 0 22px;
  padding: 5px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px;
  box-sizing: border-box;
  border: 1px solid #e7ecf3;
  border-radius: 12px;
  background: #eef2f7;
}
.login-mode__option {
  height: 46px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  background: transparent;
  color: #7b8798;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: color .2s ease, background-color .2s ease, box-shadow .2s ease, transform .2s ease;
}
.login-mode__option .van-icon {
  font-size: 18px;
}
.login-mode__option:hover {
  color: #1989fa;
}
.login-mode__option:focus-visible {
  outline: 2px solid rgba(25, 137, 250, .32);
  outline-offset: 1px;
}
.login-mode__option--active {
  background: #fff;
  color: #1989fa;
  box-shadow: 0 3px 10px rgba(38, 72, 112, .1);
  transform: translateY(-1px);
}
.login-form :deep(.van-cell-group--inset) {
  margin: 0;
  overflow: hidden;
  border: 1px solid #e7ebf1;
  border-radius: 8px;
}
.login-form :deep(.van-cell) {
  padding: 14px 16px;
  font-size: 14px;
  line-height: 24px;
}
.login-form :deep(.van-field__label) {
  width: 66px;
  color: #4e5a6b;
}
.login-form :deep(.van-field__control) {
  font-size: 14px;
}
.login-form :deep(.van-field__right-icon) {
  font-size: 18px;
}
.login-actions {
  margin: 28px 0 0;
}
.login-actions :deep(.van-button) {
  height: 46px;
  font-size: 15px;
}
.portfolio-link {
  width: 100%;
  min-height: 46px;
  margin: 18px 0 0;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid #dce7f3;
  border-radius: 8px;
  background: #f8fbff;
  color: #64748b;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color .2s ease, border-color .2s ease, background-color .2s ease;
}
.portfolio-link:hover {
  border-color: #9acbfa;
  background: #eef7ff;
  color: #1989fa;
}
.portfolio-link:focus-visible {
  outline: 3px solid rgba(25, 137, 250, .26);
  outline-offset: 2px;
}
.portfolio-link--attention {
  border-color: #72b8fb;
  background: #edf7ff;
  color: #1989fa;
  animation: portfolio-entry-attention 1.35s ease-in-out 4;
}
.portfolio-link .van-icon:last-child { font-size: 14px; }

@keyframes portfolio-entry-attention {
  0%, 100% { box-shadow: 0 0 0 0 rgba(25, 137, 250, 0); transform: translateY(0); }
  50% { box-shadow: 0 0 0 8px rgba(25, 137, 250, .14); transform: translateY(-1px); }
}

@media (max-width: 767px) {
  .login-container {
    padding: 0;
    display: block;
    background: #f7f8fa;
  }
  .login-panel {
    width: 100%;
    min-height: 100vh;
    padding: 58px 16px 26px;
    border: 0;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
  }
  .header {
    margin-bottom: 34px;
  }
  .header-logo {
    width: 72px;
    height: 72px;
    border-radius: 18px;
  }
  .login-mode {
    margin-bottom: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .portfolio-link--attention {
    animation: none;
  }
}
</style>
