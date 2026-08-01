import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref('')
  const userInfo = ref<UserInfo | null>(null)
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  
  // 方法
  const setToken = (newToken: string) => {
    token.value = newToken
  }
  
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
  }
  
  const updateUserInfo = (partialInfo: Partial<UserInfo>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...partialInfo }
    }
  }
  
  const clearUser = () => {
    token.value = ''
    userInfo.value = null
  }
  
  return {
    // 状态
    token,
    userInfo,
    
    // 计算属性
    isLoggedIn,
    username,
    
    // 方法
    setToken,
    setUserInfo,
    updateUserInfo,
    clearUser
  }
}, {
  // 持久化配置 - 改为数组形式
  persist: [
    {
      key: 'pinia-user',
      storage: localStorage
    }
  ]
})