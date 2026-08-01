<template>
  <van-popup
    v-model:show="visible"
    class="stale-dialog"
    :close-on-click-overlay="false"
    :close-on-popstate="false"
    :lock-scroll="true"
    round
  >
    <div v-if="loading" class="dialog-state">
      <van-loading size="26" vertical>正在检查遗留订单</van-loading>
    </div>

    <div v-else-if="loadError" class="dialog-state error-state">
      <van-icon name="warning-o" size="34" />
      <strong>订单检查失败</strong>
      <p>请检查网络后重试</p>
      <van-button type="primary" block @click="loadOrders">重新检查</van-button>
    </div>

    <div v-else-if="currentOrder" class="dialog-content">
      <div class="dialog-heading">
        <div class="heading-icon"><van-icon name="todo-list-o" /></div>
        <div>
          <h2>请先处理昨日订单</h2>
          <p>处理完后即可继续今日营业</p>
        </div>
        <span class="remaining-count">{{ total }} 笔</span>
      </div>

      <div class="order-summary">
        <div class="order-main">
          <strong>#{{ currentOrder.pickupCode || '--' }}</strong>
          <span>{{ currentOrder.status === 'making' ? '制作中' : '待取餐' }}</span>
        </div>
        <div class="order-meta">
          <span>{{ formatDate(currentOrder.bizDate) }}</span>
          <span>取餐 {{ currentOrder.pickupTime || '--' }}</span>
        </div>
        <p class="item-summary">{{ itemSummary }}</p>
        <p v-if="currentOrder.remark" class="order-remark">备注：{{ currentOrder.remark }}</p>
      </div>

      <div class="result-hint">请选择这笔订单的实际结果</div>
      <van-button
        class="complete-action"
        block
        :loading="submitting === 'completed'"
        :disabled="!!submitting"
        @click="confirmCompleted"
      >
        <van-icon name="passed" /> 已完成取餐
      </van-button>
      <div class="secondary-actions">
        <van-button
          class="no-show-action"
          :loading="submitting === 'no_show'"
          :disabled="!!submitting"
          @click="confirmNoShow"
        >
          用户逃单
        </van-button>
        <van-button :disabled="!!submitting" @click="openCancelDialog">其他取消</van-button>
      </div>
    </div>

    <van-dialog
      v-model:show="showCancelDialog"
      title="填写取消原因"
      show-cancel-button
      :before-close="submitCancellation"
    >
      <div class="cancel-reason-field">
        <van-field
          v-model="cancelReason"
          type="textarea"
          maxlength="200"
          show-word-limit
          autosize
          placeholder="例如：商户临时缺货"
        />
      </div>
    </van-dialog>
  </van-popup>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { showConfirmDialog, showFailToast, showSuccessToast, showToast } from 'vant';
import request from '@/utils/request';
import { fetchStaleOrders } from '@/utils/staleOrders';

const emit = defineEmits<{ resolved: [] }>();
const orders = ref<any[]>([]);
const total = ref(0);
const loading = ref(true);
const loadError = ref(false);
const submitting = ref('');
const showCancelDialog = ref(false);
const cancelReason = ref('');
const currentOrder = computed(() => orders.value[0] || null);
const visible = computed({
  get: () => total.value > 0,
  set: () => undefined,
});
const itemSummary = computed(() => {
  const items = currentOrder.value?.items || [];
  if (!items.length) return '订单商品信息暂缺';
  return items
    .slice(0, 3)
    .map((item: any) => `${item.dishName || item.name || '商品'} ×${item.quantity || 0}`)
    .join('、') + (items.length > 3 ? ` 等${items.length}项` : '');
});
let timer: number | undefined;

const loadOrders = async (silent = false) => {
  if (!silent) loading.value = true;
  loadError.value = false;
  try {
    const result = await fetchStaleOrders(1, 20);
    orders.value = result.records;
    total.value = result.total;
    if (result.total === 0 && !silent) emit('resolved');
  } catch (error) {
    console.error(error);
    if (!silent || total.value > 0) loadError.value = true;
  } finally {
    loading.value = false;
  }
};

const resolveOrder = async (result: 'completed' | 'no_show' | 'cancelled', reason?: string) => {
  if (!currentOrder.value || submitting.value) return false;
  submitting.value = result;
  try {
    await request.put(`/api/merchant/orders/${currentOrder.value.id}/resolve-stale`, {
      result,
      cancelReason: reason,
    });
    showSuccessToast(result === 'completed' ? '已确认完成' : result === 'no_show' ? '已记录逃单' : '订单已取消');
    await loadOrders(true);
    if (total.value === 0) {
      showSuccessToast('遗留订单已处理完');
      emit('resolved');
    }
    return true;
  } catch (error) {
    console.error(error);
    showFailToast('处理失败，请重试');
    await loadOrders(true);
    return false;
  } finally {
    submitting.value = '';
  }
};

const confirmCompleted = async () => {
  try {
    await showConfirmDialog({ title: '确认已完成取餐？', message: '确认学生已经取走餐品。', confirmButtonText: '确认完成' });
    await resolveOrder('completed');
  } catch { /* cancelled */ }
};

const confirmNoShow = async () => {
  try {
    await showConfirmDialog({
      title: '确认用户逃单？',
      message: '该操作会记录逃单并触发对应处罚。',
      confirmButtonText: '确认逃单',
      confirmButtonColor: '#ee5a52',
    });
    await resolveOrder('no_show');
  } catch { /* cancelled */ }
};

const openCancelDialog = () => {
  cancelReason.value = '';
  showCancelDialog.value = true;
};

const submitCancellation = async (action: string) => {
  if (action !== 'confirm') return true;
  const reason = cancelReason.value.trim();
  if (!reason) {
    showToast('请填写取消原因');
    return false;
  }
  return await resolveOrder('cancelled', reason);
};

const formatDate = (value: string) => value ? value.replace(/-/g, '.') : '--';

onMounted(() => {
  void loadOrders();
  timer = window.setInterval(() => void loadOrders(true), 10000);
});
onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer);
});
</script>

<style scoped>
.stale-dialog { width: min(88vw, 390px); overflow: visible; border-radius: 8px; color: #323233; }
.dialog-state { min-height: 210px; box-sizing: border-box; padding: 28px 24px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.error-state { color: #646566; }
.error-state strong { margin-top: 10px; color: #323233; font-size: 17px; }
.error-state p { margin: 6px 0 20px; font-size: 13px; }
.dialog-content { padding: 22px 20px 20px; }
.dialog-heading { display: grid; grid-template-columns: 38px minmax(0, 1fr) auto; align-items: center; gap: 10px; }
.heading-icon { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 8px; background: #eaf5ff; color: #1989fa; font-size: 21px; }
.dialog-heading h2 { margin: 0; font-size: 18px; letter-spacing: 0; }
.dialog-heading p { margin: 4px 0 0; color: #969799; font-size: 12px; }
.remaining-count { color: #1989fa; background: #edf7ff; border-radius: 4px; padding: 4px 7px; font-size: 12px; font-weight: 700; white-space: nowrap; }
.order-summary { margin: 18px 0 14px; padding: 14px; border-radius: 6px; background: #f7f8fa; border: 1px solid #ebedf0; }
.order-main, .order-meta { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.order-main strong { color: #1989fa; font-size: 22px; }
.order-main span { color: #ed6a0c; background: #fff3e8; border-radius: 4px; padding: 3px 7px; font-size: 12px; }
.order-meta { margin-top: 7px; color: #969799; font-size: 12px; }
.item-summary { margin: 12px 0 0; color: #323233; font-size: 14px; line-height: 1.55; overflow-wrap: anywhere; }
.order-remark { margin: 7px 0 0; color: #ee0a24; font-size: 12px; line-height: 1.45; overflow-wrap: anywhere; }
.result-hint { margin-bottom: 9px; color: #969799; font-size: 12px; text-align: center; }
.complete-action { height: 44px; border: 0; background: #1989fa; color: #fff; font-weight: 700; }
.secondary-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 10px; }
.secondary-actions .van-button { height: 40px; border-radius: 4px; }
.no-show-action { color: #ee5a52; border-color: #f1b3af; }
.cancel-reason-field { padding: 12px 16px 4px; }
</style>
