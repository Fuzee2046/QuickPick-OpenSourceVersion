<template>
  <div class="billing-page">
    <header class="page-header">
      <h1>运营成本费</h1>
    </header>

    <section class="estimate-card">
      <div class="estimate-head">
        <div><span>本月预估费用</span><small>根据本月已完成订单实时计算</small></div>
        <strong>¥{{ money(overview.estimatedAmount) }}</strong>
      </div>
      <div class="summary-grid">
        <div><b>{{ overview.completedOrderCount || 0 }}<small>单</small></b><span>已完成订单</span></div>
        <div><b>{{ overview.freeOrderCount ?? '--' }}<small>单</small></b><span>每月免费额度</span></div>
        <div><b>¥{{ overview.unitPrice == null ? '--' : money(overview.unitPrice) }}<small>/单</small></b><span>超出部分单价</span></div>
      </div>
    </section>

    <section v-if="overview.overdueBills?.length" class="overdue-card">
      <div class="overdue-icon"><van-icon name="lock" /></div>
      <div><b>接单服务已暂停</b><p>存在逾期未支付账单。学生端将无法向本店提交新订单，支付到账后系统会自动恢复接单。</p></div>
    </section>

    <section v-else-if="overview.billingEnabled === false" class="billing-paused-card">
      <van-icon name="info-o" /><span>{{ billingPausedText }}</span>
    </section>

    <section class="billing-rules">
      <van-collapse v-model="rulesOpen">
        <van-collapse-item title="计费规则" name="rules">
          每月前 {{ overview.freeOrderCount || 0 }} 单免费，超出部分按 ¥{{ money(overview.unitPrice) }}/单计费；账单以完成订单为准。
        </van-collapse-item>
      </van-collapse>
    </section>

    <section class="bill-list">
      <div class="section-heading"><h2>月度账单</h2><span>{{ total }} 张</span></div>
      <van-loading v-if="loading" vertical>加载中...</van-loading>
      <van-empty v-else-if="!bills.length" description="暂无账单" />
      <article v-for="bill in bills" v-else :key="bill.id" class="bill-card">
        <div class="bill-top"><div><b>{{ monthLabel(bill.billing_month) }}</b><small>{{ bill.bill_no }}</small></div><van-tag round :type="tagType(bill.status)">{{ statusLabel(bill.status) }}</van-tag></div>
        <div class="bill-middle"><div><span>完成订单</span><b>{{ bill.completed_order_count }} 单</b></div><div><span>计费订单</span><b>{{ bill.billable_order_count }} 单</b></div><div class="bill-amount"><small v-if="hasAdjustment(bill)">原 ¥{{ money(bill.original_amount) }}</small><strong>¥{{ money(bill.payable_amount) }}</strong></div></div>
        <div v-if="hasAdjustment(bill)" class="bill-adjustment-note"><van-icon name="coupon-o" /><span>{{ bill.status === 'waived' ? '平台已为本账单全部减免，您无需支付。' : `平台已为本账单减免 ¥${adjustmentAmount(bill)}，当前应付金额已更新。` }}</span></div>
        <div class="bill-bottom"><span>{{ billTimeText(bill) }}</span><van-button v-if="bill.status === 'unpaid'" round size="small" type="primary" :disabled="overview.alipayWapEnabled === false" @click="openPayment(bill)">{{ overview.alipayWapEnabled === false ? '支付暂未开放' : '立即支付' }}</van-button></div>
      </article>
    </section>

    <van-popup v-model:show="showPayDrawer" position="bottom" round closeable safe-area-inset-bottom class="payment-drawer">
      <div v-if="selectedBill" class="payment-content">
        <div class="drawer-title">确认支付</div>
        <div class="payment-brand"><img src="/favicon.ico" alt="食刻快取" /><div><b>食刻快取</b><small>商户运营成本费</small></div></div>
        <div class="payment-amount"><span class="bill-period">{{ monthLabel(selectedBill.billing_month) }}账单</span><span>本次应付</span><strong>¥{{ money(selectedBill.payable_amount) }}</strong><small v-if="hasAdjustment(selectedBill)">平台已减免 ¥{{ adjustmentAmount(selectedBill) }}</small></div>
        <div class="payment-details"><div><span>计费订单</span><b>{{ selectedBill.billable_order_count }} 单</b></div><div><span>支付要求</span><b>{{ paymentDueText(selectedBill.due_time) }}</b></div></div>
        <div class="payment-method"><div class="alipay-logo">支</div><div><b>支付宝</b><small>安全快捷的手机网站支付</small></div><van-icon name="checked" color="#1677ff" size="22" /></div>
        <p class="payment-tip"><van-icon name="shield-o" /> 支付结果以支付宝通知和系统查单为准</p>
        <van-button block round type="primary" color="#1677ff" :loading="paying" :disabled="overview.alipayWapEnabled === false" @click="pay">{{ overview.alipayWapEnabled === false ? '支付宝支付暂未开放' : `支付宝支付 ¥${money(selectedBill.payable_amount)}` }}</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showFailToast, showSuccessToast, showToast } from 'vant';
import request from '@/utils/request';

const route = useRoute();
const router = useRouter();
const overview = ref<any>({});
const bills = ref<any[]>([]);
const total = ref(0);
const loading = ref(false);
const rulesOpen = ref<string[]>([]);
const showPayDrawer = ref(false);
const selectedBill = ref<any>(null);
const paying = ref(false);
let stopped = false;

const money = (value: any) => Number(value || 0).toFixed(2);
const monthLabel = (value: string) => String(value || '').slice(0, 7).replace('-', '年') + '月';
const formatTime = (value: string) => String(value || '').replace('T', ' ').slice(0, 16);
const formatDate = (value: string) => {
  const date = new Date(String(value || '').replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return '';
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
};
const hasPlaceholderDueDate = (value: string) => Number(String(value || '').slice(0, 4)) >= 2090;
const paymentDueText = (value: string) => hasPlaceholderDueDate(value) ? '请及时完成支付' : `请于${formatDate(value)}前支付`;
const adjustmentAmount = (bill: any) => money(Math.abs(Number(bill?.adjustment_amount || 0)));
const hasAdjustment = (bill: any) => Math.abs(Number(bill?.adjustment_amount || 0)) > 0;
const billTimeText = (bill: any) => {
  if (bill.status === 'paid') return bill.paid_time ? `支付时间 ${formatTime(bill.paid_time)}` : '已完成支付';
  if (bill.status === 'unpaid') return hasPlaceholderDueDate(bill.due_time)
    ? '请及时完成支付，逾期将限制接单'
    : `请于${formatDate(bill.due_time)}前完成支付，逾期将限制接单`;
  return bill.create_time ? `账单生成于 ${formatTime(bill.create_time)}` : '';
};
const statusLabel = (value: string) => ({ unpaid: '待支付', paid: '已支付', waived: '已减免', void: '已作废' } as Record<string, string>)[value] || value;
const tagType = (value: string): any => value === 'paid' ? 'success' : value === 'unpaid' ? 'danger' : 'primary';
const billingPausedText = computed(() => {
  if (overview.value.billingConfiguredEnabled === false) return '管理员当前已暂停运营成本费，新订单不会继续产生运营成本费用。';
  if (overview.value.planEffectiveMonth) return `运营成本费方案将于 ${monthLabel(overview.value.planEffectiveMonth)} 生效，生效前不会产生新的计费金额。`;
  return '当前没有生效中的运营成本费方案。';
});

const load = async () => {
  loading.value = true;
  try {
    overview.value = await request.get('/api/merchant/billing/overview');
    const data: any = await request.get('/api/merchant/billing/bills');
    bills.value = data.records || [];
    total.value = data.total || 0;
  } finally { loading.value = false; }
};

const openPayment = (bill: any) => { selectedBill.value = bill; showPayDrawer.value = true; };

const openQueryBill = () => {
  const billId = String(route.query.payBillId || '');
  if (!billId) return;
  const bill = bills.value.find((item) => String(item.id) === billId && item.status === 'unpaid');
  if (bill) openPayment(bill);
};

const pay = async () => {
  if (!selectedBill.value || paying.value) return;
  paying.value = true;
  try {
    const data: any = await request.post(`/api/merchant/billing/bills/${selectedBill.value.id}/pay`);
    document.open(); document.write(data.formHtml); document.close();
  } catch (error: any) {
    showFailToast(error?.message || '发起支付宝支付失败');
  } finally { paying.value = false; }
};

const waitForPaymentResult = async () => {
  const outTradeNo = String(route.query.outTradeNo || '');
  if (!outTradeNo) return;
  showToast({ message: '正在确认支付结果...', duration: 2000 });
  for (let attempt = 0; attempt < 15 && !stopped; attempt += 1) {
    if (attempt > 0) await new Promise((resolve) => window.setTimeout(resolve, 2000));
    if (attempt % 3 === 0) { try { await request.post(`/api/merchant/billing/payments/${encodeURIComponent(outTradeNo)}/sync`); } catch (_) {} }
    const payment: any = await request.get(`/api/merchant/billing/payments/${encodeURIComponent(outTradeNo)}`);
    if (payment.status === 'success' || payment.bill_status === 'paid') {
      await load(); showSuccessToast('支付成功，账单已结清'); await router.replace('/billing'); return;
    }
  }
  if (!stopped) { showToast({ message: '支付结果仍在确认中，请稍后刷新', duration: 4000 }); await router.replace('/billing'); }
};

onMounted(async () => { await load(); openQueryBill(); await waitForPaymentResult(); });
watch(() => route.query.payBillId, openQueryBill);
onBeforeUnmount(() => { stopped = true; });
</script>

<style scoped>
.billing-page{min-height:100%;box-sizing:border-box;padding:18px 16px 32px;background:#f5f7fa;color:#263243}.page-header,.bill-top,.bill-bottom,.section-heading,.estimate-head{display:flex;align-items:center;justify-content:space-between}.page-header{margin-bottom:16px}.page-header h1{margin:0;font-size:22px;line-height:1.3}.estimate-card,.bill-card{background:#fff;border:1px solid #e8edf4;border-radius:14px}.estimate-card{padding:18px}.estimate-head>div{display:flex;flex-direction:column;gap:5px}.estimate-head span{color:#475467;font-size:14px;font-weight:600}.estimate-head small{color:#98a2b3;font-size:11px;font-weight:400}.estimate-head strong{color:#167a62;font-size:28px}.summary-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:0;margin-top:18px;padding-top:15px;border-top:1px solid #edf1f5}.summary-grid div{display:flex;align-items:center;flex-direction:column;gap:5px;border-right:1px solid #edf1f5}.summary-grid div:last-child{border-right:0}.summary-grid b{color:#344054;font-size:17px}.summary-grid b small{margin-left:2px;color:#667085;font-size:11px;font-weight:400}.summary-grid span,.bill-middle span,.payment-amount>span{color:#98a2b3;font-size:11px}.overdue-card,.billing-paused-card{margin:12px 0;padding:14px;display:flex;align-items:flex-start;gap:11px;border-radius:11px}.overdue-card{border:1px solid #ffd5cf;background:#fff4f2}.overdue-icon{width:34px;height:34px;flex:0 0 auto;display:grid;place-items:center;border-radius:9px;background:#fee4e2;color:#d92d20;font-size:18px}.overdue-card b{color:#b42318;font-size:14px}.overdue-card p{margin:4px 0 0;color:#c04438;font-size:12px;line-height:1.55}.billing-paused-card{border:1px solid #dbe8f7;background:#f5f9ff;color:#526b86;font-size:12px;line-height:1.55}.billing-paused-card .van-icon{margin-top:1px;color:#1a8cff;font-size:16px}.billing-rules{margin:12px 0}.billing-rules :deep(.van-cell){border-radius:10px}.section-heading{margin:20px 0 12px}.section-heading h2{margin:0;font-size:17px}.section-heading span{color:#98a2b3;font-size:12px}.bill-card{padding:16px;margin-bottom:10px}.bill-top>div,.payment-brand div,.payment-method div,.bill-middle div{display:flex;flex-direction:column;gap:4px}.bill-top b{font-size:16px}.bill-top small{color:#98a2b3;font-size:11px}.bill-middle{display:grid;grid-template-columns:1fr 1fr auto;align-items:end;gap:8px;margin:17px 0}.bill-middle b{font-size:13px}.bill-amount{align-items:flex-end}.bill-amount small{color:#98a2b3;font-size:10px;text-decoration:line-through}.bill-middle strong{color:#202b3c;font-size:21px}.bill-adjustment-note{margin:-4px 0 12px;padding:9px 10px;display:flex;align-items:flex-start;gap:7px;border-radius:8px;background:#fff8e8;color:#9a6700;font-size:11px;line-height:1.5}.bill-adjustment-note .van-icon{margin-top:2px;font-size:14px}.bill-bottom{gap:12px;padding-top:12px;border-top:1px solid #f0f2f5;color:#667085;font-size:12px;line-height:1.5}.bill-bottom span{flex:1}.payment-drawer{overflow:hidden}.payment-content{padding:20px 18px 28px}.drawer-title{text-align:center;font-size:17px;font-weight:600;margin-bottom:15px}.payment-brand,.payment-method{display:flex;align-items:center;gap:10px;padding:12px 14px;border:1px solid #e8edf4;border-radius:12px}.payment-brand img{width:30px;height:30px;border-radius:7px}.payment-brand b{font-size:14px;font-weight:600}.payment-brand div,.payment-method div{flex:1}.payment-brand small,.payment-method small{color:#98a2b3;font-size:11px}.payment-amount{text-align:center;padding:15px 0 16px}.payment-amount .bill-period{display:block;margin-bottom:4px;color:#475467;font-size:15px;font-weight:600}.payment-amount strong{display:block;margin-top:3px;font-size:34px}.payment-amount>small{display:block;margin-top:4px;color:#b7791f;font-size:11px}.payment-details{padding:4px 14px;margin-bottom:12px;border:1px solid #e8edf4;border-radius:12px}.payment-details div{display:flex;align-items:flex-start;justify-content:space-between;gap:20px;padding:12px 0;border-bottom:1px solid #f0f2f5;font-size:12px}.payment-details div:last-child{border:0}.payment-details span{flex:0 0 auto;color:#8491a5}.payment-details b{text-align:right;font-size:12px;font-weight:500}.alipay-logo{flex:0 0 34px!important;width:34px;height:34px;border-radius:8px;background:#1677ff;color:#fff;display:grid!important;place-items:center;font-size:20px;font-weight:700}.payment-tip{margin:14px 0;color:#98a2b3;text-align:center;font-size:12px}.payment-tip .van-icon{vertical-align:-2px;margin-right:3px}
</style>
