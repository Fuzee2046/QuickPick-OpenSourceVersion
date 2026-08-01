import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(uni.getStorageSync('token') || '')
  const userInfo = ref<any>(uni.getStorageSync('userInfo') || null)

  const setToken = (val: string) => {
    token.value = val
    uni.setStorageSync('token', val)
  }

  const setUserInfo = (val: any) => {
    userInfo.value = val
    uni.setStorageSync('userInfo', val)
  }

  const clearUserInfo = () => {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  // 使用 computed 确保响应式更新
  const isLogin = computed(() => !!token.value)

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    clearUserInfo,
    isLogin,
  }
})
