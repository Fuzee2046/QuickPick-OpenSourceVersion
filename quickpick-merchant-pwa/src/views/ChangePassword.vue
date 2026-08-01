<template>
  <div class="change-password-container">
    <van-nav-bar
      title="修改密码"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
    />
    
    <div class="content">
      <div class="tips" v-if="auth.requirePasswordChange">
        <van-icon name="info-o" /> 为了您的账号安全，请修改初始密码
      </div>

      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="oldPassword"
            type="password"
            name="oldPassword"
            label="原密码"
            placeholder="请输入原密码"
            :rules="[{ required: true, message: '请输入原密码' }]"
          />
          <van-field
            v-model="newPassword"
            type="password"
            name="newPassword"
            label="新密码"
            :placeholder="auth.isAdmin ? '请输入新密码(至少10位)' : '请输入新密码(至少6位)'"
            :rules="[{ required: true, message: '请输入新密码' }, { validator: validatePassword, message: auth.isAdmin ? '密码长度不能少于10位' : '密码长度不能少于6位' }]"
          />
          <van-field
            v-model="confirmPassword"
            type="password"
            name="confirmPassword"
            label="确认密码"
            placeholder="请再次输入新密码"
            :rules="[{ required: true, message: '请确认新密码' }, { validator: validateConfirm, message: '两次密码不一致' }]"
          />
        </van-cell-group>
        
        <div style="margin: 32px 16px;">
          <van-button round block type="primary" native-type="submit" :loading="loading">
            确认修改
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import request from '@/utils/request';
import { showSuccessToast, showFailToast } from 'vant';

const router = useRouter();
const auth = useAuthStore();

const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const loading = ref(false);

const onClickLeft = () => {
  if (auth.requirePasswordChange) {
    showFailToast('首次登录必须修改密码');
    return;
  }
  router.back();
};

const validatePassword = (val: string) => val.length >= (auth.isAdmin ? 10 : 6);
const validateConfirm = (val: string) => val === newPassword.value;

const onSubmit = async () => {
  loading.value = true;
  try {
    await request.request({
      url: auth.isAdmin ? '/api/admin/auth/password' : '/api/merchant/auth/password',
      method: auth.isAdmin ? 'put' : 'post',
      data: {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
      },
    });
    
    showSuccessToast('密码修改成功，请重新登录');
    auth.logout();
    router.replace('/login');
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.change-password-container {
  min-height: 100vh;
  background-color: #f7f8fa;
}
.content {
  padding-top: 20px;
}
.tips {
  margin: 0 16px 16px;
  padding: 10px;
  background-color: #e6f7ff;
  color: #1989fa;
  font-size: 14px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
