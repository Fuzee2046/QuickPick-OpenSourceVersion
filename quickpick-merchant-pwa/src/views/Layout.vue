<template>
  <div class="layout-container">
    <div class="content-wrapper">
      <router-view v-slot="{ Component }">
        <keep-alive include="Dashboard">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </div>

    <van-tabbar route fixed safe-area-inset-bottom :z-index="999" :placeholder="false">
      <van-tabbar-item replace to="/dashboard" icon="orders-o">看板</van-tabbar-item>
      <van-tabbar-item replace to="/dishes" icon="apps-o">菜品/食材</van-tabbar-item>
      <van-tabbar-item replace to="/categories" icon="label-o">分类</van-tabbar-item>
      <van-tabbar-item replace to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>

    <van-dialog
      v-model:show="showReminder"
      :title="reminder?.overdue ? '接单服务已暂停' : '运营成本费待支付'"
      show-cancel-button
      cancel-button-text="稍后处理"
      confirm-button-text="立即支付"
      confirm-button-color="#1677ff"
      :close-on-click-overlay="false"
      @cancel="dismissReminder"
      @confirm="openReminderBill"
    >
      <div v-if="reminder" class="reminder-content">
        <img src="/favicon.ico" alt="食刻快取" class="brand-icon" />
        <p class="reminder-title">{{ monthLabel(reminder.billingMonth) }}运营成本费</p>
        <strong class="reminder-amount">¥{{ money(reminder.amount) }}</strong>
        <p :class="['reminder-message', { overdue: reminder.overdue }]">
          {{ reminder.overdue
            ? '账单已逾期，当前店铺接单服务已暂停，支付到账后将自动恢复。'
            : `请于 ${reminderDueDate(reminder)} 前完成支付，避免影响正常接单。` }}
        </p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';

const router = useRouter();
const reminder = ref<any>(null);
const showReminder = ref(false);
let checking = false;

const money = (value: any) => Number(value || 0).toFixed(2);
const monthLabel = (value: string) => String(value || '').slice(0, 7).replace('-', '年') + '月';
const formatDate = (value: string) => {
  const date = new Date(String(value || '').replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? '' : `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
};
const reminderDueDate = (item: any) => {
  if (Number(String(item?.dueTime || '').slice(0, 4)) < 2090) return formatDate(item.dueTime);
  const month = new Date(`${String(item?.billingMonth || '').slice(0, 7)}-01T00:00:00`);
  if (Number.isNaN(month.getTime())) return '规定期限内';
  month.setMonth(month.getMonth() + 1);
  month.setDate(7);
  return formatDate(month.toISOString());
};

const checkPaymentReminder = async () => {
  if (checking || document.visibilityState !== 'visible') return;
  checking = true;
  try {
    const overview: any = await request.get('/api/merchant/billing/overview');
    reminder.value = overview.paymentReminder || null;
    showReminder.value = router.currentRoute.value.path !== '/billing' && Boolean(reminder.value?.shouldShow);
  } catch (error) {
    console.error('Failed to check merchant billing reminder', error);
  } finally {
    checking = false;
  }
};

const dismissReminder = async () => {
  if (!reminder.value?.billId) return;
  try {
    await request.post(`/api/merchant/billing/reminders/${reminder.value.billId}/dismiss`);
    reminder.value.shouldShow = false;
  } catch (error) {
    console.error('Failed to dismiss merchant billing reminder', error);
  }
};

const openReminderBill = () => {
  if (!reminder.value?.billId) return;
  router.push({ path: '/billing', query: { payBillId: String(reminder.value.billId) } });
};

const onVisibilityChange = () => {
  if (document.visibilityState === 'visible') checkPaymentReminder();
};

onMounted(() => {
  checkPaymentReminder();
  document.addEventListener('visibilitychange', onVisibilityChange);
});
onBeforeUnmount(() => document.removeEventListener('visibilitychange', onVisibilityChange));
</script>

<style scoped>
.layout-container{height:100vh;width:100vw;overflow:hidden;display:flex;flex-direction:column;background:#f5f7fa}.content-wrapper{flex:1;overflow-y:auto;-webkit-overflow-scrolling:touch;width:100%;padding-bottom:calc(50px + env(safe-area-inset-bottom))}.reminder-content{text-align:center;padding:8px 24px 24px}.brand-icon{display:block;width:32px;height:32px;margin:2px auto 12px;border-radius:8px}.reminder-title{margin:0;color:#667085;font-size:14px}.reminder-amount{display:block;margin:8px 0 14px;color:#101828;font-size:32px;line-height:1.2}.reminder-message{margin:0;padding:11px 12px;border-radius:8px;background:#f5f8ff;color:#667085;font-size:13px;line-height:1.6;text-align:left}.reminder-message.overdue{background:#fff2f0;color:#b42318}
</style>
