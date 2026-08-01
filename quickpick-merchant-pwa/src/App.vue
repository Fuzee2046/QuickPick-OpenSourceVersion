<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { hasStaleOrders } from '@/utils/staleOrders'

const auth = useAuthStore()

const checkStaleOrdersOnResume = async () => {
  if (document.visibilityState !== 'visible' || !auth.isLoggedIn || auth.isAdmin || auth.requirePasswordChange) return
  if (router.currentRoute.value.meta.public) return
  if (router.currentRoute.value.path === '/login' || router.currentRoute.value.path === '/dashboard') return
  try {
    if (await hasStaleOrders()) await router.replace('/dashboard')
  } catch (error) {
    console.error('Failed to check stale orders after resume', error)
    await router.replace('/dashboard')
  }
}

onMounted(() => document.addEventListener('visibilitychange', checkStaleOrdersOnResume))
onBeforeUnmount(() => document.removeEventListener('visibilitychange', checkStaleOrdersOnResume))
</script>

<template>
  <RouterView />
</template>

<style>
/* 全局样式重置 */
body {
  margin: 0;
  padding: 0;
  background-color: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica,
    Segoe UI, Arial, Roboto, 'PingFang SC', 'miui', 'Hiragino Sans GB', 'Microsoft Yahei',
    sans-serif;
}
</style>
