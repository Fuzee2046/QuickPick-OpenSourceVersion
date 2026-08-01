<template>
  <div class="admin-page billing-admin" v-loading="loading">
    <div class="admin-page__header billing-header">
      <div><h2>商户计费</h2><p>查看平台收入、商户账单、支付流水与减免记录</p></div>
      <el-button :icon="Refresh" @click="loadAll">刷新数据</el-button>
    </div>

    <div class="billing-metrics">
      <div class="billing-metric blue"><span><el-icon><Money /></el-icon></span><div><small>累计应收</small><strong>¥{{ money(totals.total_billed) }}</strong><em>{{ totals.bill_count || 0 }} 张账单</em></div></div>
      <div class="billing-metric green"><span><el-icon><CircleCheck /></el-icon></span><div><small>实际到账</small><strong>¥{{ money(totals.total_received) }}</strong><em>{{ totals.paid_count || 0 }} 张已结清</em></div></div>
      <div class="billing-metric orange"><span><el-icon><Clock /></el-icon></span><div><small>待收金额</small><strong>¥{{ money(totals.pending_amount) }}</strong><em>{{ totals.unpaid_count || 0 }} 张待支付</em></div></div>
      <div class="billing-metric violet"><span><el-icon><Discount /></el-icon></span><div><small>累计减免</small><strong>¥{{ money(totals.discounted_amount) }}</strong><em>含部分及全部减免</em></div></div>
      <div class="billing-metric red"><span><el-icon><Warning /></el-icon></span><div><small>逾期账单</small><strong>{{ totals.overdue_count || 0 }}</strong><em>逾期后限制接单</em></div></div>
      <div class="billing-metric cyan"><span><el-icon><DataLine /></el-icon></span><div><small>账单回收率</small><strong>{{ collectionRate }}%</strong><em>按账单数量计算</em></div></div>
    </div>

    <div class="billing-top-grid">
      <section class="admin-panel plan-card">
        <div class="panel-heading"><div><h3>当前计费方案</h3><p>修改后影响后续统计与新生成账单，历史账单金额不会改变</p></div><el-tag :type="plan.billingEnabled ? 'success' : 'info'">{{ plan.billingEnabled ? '计费启用' : '计费停用' }}</el-tag></div>
        <el-form label-position="top" class="plan-form">
          <div class="plan-grid">
            <el-form-item label="每月免费订单"><el-input-number v-model="plan.freeOrderCount" :min="0" /></el-form-item>
            <el-form-item label="超出部分单价"><el-input-number v-model="plan.unitPrice" :min="0" :step="0.01" :precision="2" /><small>元/单</small></el-form-item>
            <el-form-item label="支付期限"><el-input-number v-model="plan.graceDays" :min="1" :max="31" /><small>天</small></el-form-item>
          </div>
          <div class="plan-switches"><label><el-switch v-model="plan.billingEnabled" />启用运营成本费</label><label><el-switch v-model="plan.alipayWapEnabled" />启用支付宝支付</label><span>生效月份：{{ formatMonth(plan.effectiveMonth) || '未配置' }}</span></div>
          <div class="plan-actions"><el-button type="primary" :loading="savingPlan" @click="savePlan">保存当前方案</el-button></div>
        </el-form>
      </section>

      <section class="admin-panel income-card">
        <div class="panel-heading"><div><h3>商户收入概览</h3><p>{{ filters.billingMonth ? formatMonth(filters.billingMonth) : '全部账期' }}各商户应收、实收与减免情况</p></div></div>
        <el-table :data="shopIncome" height="250" empty-text="暂无商户账单">
          <el-table-column prop="shop_name" label="商户" min-width="120" />
          <el-table-column label="应收" width="100"><template #default="s">¥{{ money(s.row.billed_amount) }}</template></el-table-column>
          <el-table-column label="实收" width="100"><template #default="s"><b class="income-value">¥{{ money(s.row.received_amount) }}</b></template></el-table-column>
          <el-table-column label="待收" width="100"><template #default="s">¥{{ money(s.row.pending_amount) }}</template></el-table-column>
          <el-table-column label="减免" width="100"><template #default="s">¥{{ money(s.row.discounted_amount) }}</template></el-table-column>
        </el-table>
      </section>
    </div>

    <section class="admin-panel bill-section">
      <div class="panel-heading"><div><h3>月度账单</h3><p>支持按账期、商户和状态查询，查看支付及调整记录</p></div></div>
      <div class="billing-toolbar">
        <el-select v-model="filters.billingMonth" clearable placeholder="全部账期" @change="applyFilters"><el-option v-for="item in months" :key="String(item.billing_month)" :label="formatMonth(item.billing_month)" :value="String(item.billing_month).slice(0,10)" /></el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态" @change="applyFilters"><el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-input v-model="filters.keyword" clearable placeholder="搜索商户或账单号" :prefix-icon="Search" @keyup.enter="applyFilters" />
        <el-button type="primary" @click="applyFilters">查询</el-button><el-button @click="resetFilters">重置</el-button>
      </div>
      <el-table :data="bills" empty-text="暂无账单">
        <el-table-column prop="bill_no" label="账单号" min-width="165" />
        <el-table-column prop="shop_name" label="商户" min-width="130" />
        <el-table-column label="账期" width="110"><template #default="s">{{ formatMonth(s.row.billing_month) }}</template></el-table-column>
        <el-table-column label="订单" width="120"><template #default="s"><span class="order-count">{{ s.row.completed_order_count }} 完成 / {{ s.row.billable_order_count }} 计费</span></template></el-table-column>
        <el-table-column label="原金额" width="100"><template #default="s">¥{{ money(s.row.original_amount) }}</template></el-table-column>
        <el-table-column label="减免" width="90"><template #default="s"><span class="discount-value">-¥{{ money(Math.abs(Number(s.row.adjustment_amount || 0))) }}</span></template></el-table-column>
        <el-table-column label="应付" width="105"><template #default="s"><b>¥{{ money(s.row.payable_amount) }}</b></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="s"><el-tag round :type="tagType(s.row.status)">{{ statusLabel(s.row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="时间" min-width="155"><template #default="s"><span class="time-text">{{ billTime(s.row) }}</span></template></el-table-column>
        <el-table-column label="操作" width="145" fixed="right"><template #default="s"><el-button link type="primary" @click="showDetail(s.row)">详情</el-button><el-button v-if="s.row.status === 'unpaid'" link type="warning" @click="openAdjust(s.row)">减免/作废</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :total="total" :page-size="20" layout="total, prev, pager, next" @current-change="loadBills" />
    </section>

    <el-dialog v-model="adjustDialog" width="520px" class="billing-dialog">
      <template #header><div class="dialog-heading"><span>调整账单</span><small>{{ currentBill.shop_name }} · {{ formatMonth(currentBill.billing_month) }}</small></div></template>
      <div class="adjust-summary"><span>当前应付</span><strong>¥{{ money(currentBill.payable_amount) }}</strong></div>
      <el-form label-position="top">
        <el-form-item label="调整方式"><el-radio-group v-model="adjust.type"><el-radio-button value="reduce">部分减免</el-radio-button><el-radio-button value="waive">全部减免</el-radio-button><el-radio-button value="void">作废账单</el-radio-button></el-radio-group></el-form-item>
        <el-form-item v-if="adjust.type === 'reduce'" label="本次减免金额"><el-input-number v-model="adjust.amount" :min="0.01" :max="Number(currentBill.payable_amount || 0)" :precision="2" /></el-form-item>
        <el-form-item label="调整原因"><el-input v-model="adjust.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请填写可供后续审计的具体原因" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="adjustDialog=false">取消</el-button><el-button type="primary" :loading="savingAdjust" @click="saveAdjust">确认调整</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" size="760px" class="billing-detail-drawer">
      <template #header><div class="drawer-heading"><span>账单详情</span><small>{{ detail.shop_name }} · {{ formatMonth(detail.billing_month) }}</small></div></template>
      <div v-if="detail.id" class="detail-content">
        <div class="detail-hero"><div><span>最终应付</span><strong>¥{{ money(detail.payable_amount) }}</strong></div><el-tag round size="large" :type="tagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></div>
        <div class="detail-grid"><div><span>账单号</span><b>{{ detail.bill_no }}</b></div><div><span>商户</span><b>{{ detail.shop_name }}</b></div><div><span>完成订单</span><b>{{ detail.completed_order_count }} 单</b></div><div><span>计费订单</span><b>{{ detail.billable_order_count }} 单</b></div><div><span>原始金额</span><b>¥{{ money(detail.original_amount) }}</b></div><div><span>累计减免</span><b class="discount-value">-¥{{ money(Math.abs(Number(detail.adjustment_amount || 0))) }}</b></div><div><span>支付期限</span><b>{{ formatTime(detail.due_time) }}</b></div><div><span>支付时间</span><b>{{ detail.paid_time ? formatTime(detail.paid_time) : '尚未支付' }}</b></div></div>
        <div class="detail-section"><div class="detail-title"><h4>支付流水</h4><span>{{ detail.payments?.length || 0 }} 条</span></div><el-table :data="detail.payments || []" empty-text="暂无支付流水"><el-table-column prop="out_trade_no" label="平台交易号" min-width="190" /><el-table-column label="支付方式" width="100"><template #default="s">{{ channelLabel(s.row.channel) }}</template></el-table-column><el-table-column label="金额" width="95"><template #default="s">¥{{ money(s.row.amount) }}</template></el-table-column><el-table-column label="状态" width="95"><template #default="s"><el-tag :type="paymentTagType(s.row.status)">{{ paymentStatusLabel(s.row.status) }}</el-tag></template></el-table-column><el-table-column label="创建时间" width="150"><template #default="s">{{ formatTime(s.row.create_time) }}</template></el-table-column><el-table-column label="操作" width="75"><template #default="s"><el-button v-if="s.row.status !== 'success'" link type="primary" @click="syncPayment(s.row.out_trade_no)">核对</el-button></template></el-table-column></el-table></div>
        <div class="detail-section"><div class="detail-title"><h4>减免与调整记录</h4><span>{{ detail.adjustments?.length || 0 }} 条</span></div><el-table :data="detail.adjustments || []" empty-text="暂无调整记录"><el-table-column label="类型" width="100"><template #default="s">{{ adjustmentLabel(s.row.adjustment_type) }}</template></el-table-column><el-table-column label="金额" width="100"><template #default="s">¥{{ money(s.row.amount) }}</template></el-table-column><el-table-column prop="reason" label="调整原因" min-width="180" /><el-table-column prop="admin_name" label="操作人" width="100" /><el-table-column label="操作时间" width="150"><template #default="s">{{ formatTime(s.row.create_time) }}</template></el-table-column></el-table></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CircleCheck, Clock, DataLine, Discount, Money, Refresh, Search, Warning } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import './admin.css';

const bills=ref<any[]>([]),shopIncome=ref<any[]>([]),months=ref<any[]>([]),loading=ref(false),savingPlan=ref(false),savingAdjust=ref(false),page=ref(1),total=ref(0),adjustDialog=ref(false),detailDrawer=ref(false),currentBill=ref<any>({}),detail=ref<any>({}),totals=ref<any>({});
const filters=reactive<any>({status:'',billingMonth:'',keyword:''});
const plan=reactive<any>({id:null,effectiveMonth:'',billingEnabled:true,freeOrderCount:10,unitPrice:.3,graceDays:7,alipayWapEnabled:true});
const adjust=reactive<any>({type:'reduce',amount:.01,reason:''});
const statuses=[{value:'unpaid',label:'待支付'},{value:'paid',label:'已支付'},{value:'waived',label:'已全部减免'},{value:'void',label:'已作废'}];
const collectionRate=computed(()=>Number(totals.value.bill_count||0)?((Number(totals.value.paid_count||0)/Number(totals.value.bill_count))*100).toFixed(1):'0.0');
const money=(v:any)=>Number(v||0).toFixed(2),formatTime=(v:any)=>String(v||'').replace('T',' ').slice(0,16),formatMonth=(v:any)=>String(v||'').slice(0,7).replace('-','年')+(v?'月':'');
const statusLabel=(v:string)=>({unpaid:'待支付',paid:'已支付',waived:'已全部减免',void:'已作废'} as any)[v]||'未知状态';
const tagType=(v:string):any=>v==='paid'?'success':v==='unpaid'?'danger':v==='waived'?'warning':'info';
const paymentStatusLabel=(v:string)=>({created:'已创建',paying:'等待支付',success:'支付成功',closed:'已关闭',failed:'支付失败'} as any)[v]||'未知状态';
const paymentTagType=(v:string):any=>v==='success'?'success':v==='failed'?'danger':v==='paying'?'warning':'info';
const adjustmentLabel=(v:string)=>({reduce:'部分减免',waive:'全部减免',void:'作废账单'} as any)[v]||'其他调整';
const channelLabel=(v:string)=>v==='ALIPAY_WAP'?'支付宝':'其他方式';
const billTime=(row:any)=>row.status==='paid'&&row.paid_time?`支付于 ${formatTime(row.paid_time)}`:row.status==='unpaid'?`截止 ${formatTime(row.due_time)}`:`生成于 ${formatTime(row.create_time)}`;

const loadPlan=async()=>{const rows:any[]=await request.get('/api/admin/billing/plans');const current=rows[0];if(current)Object.assign(plan,{id:current.id,effectiveMonth:current.effective_month,billingEnabled:Boolean(current.billing_enabled),freeOrderCount:current.free_order_count,unitPrice:Number(current.unit_price),graceDays:current.grace_days,alipayWapEnabled:Boolean(current.alipay_wap_enabled)});};
const loadSummary=async()=>{const data:any=await request.get('/api/admin/billing/summary',{params:{billingMonth:filters.billingMonth||undefined}});totals.value=data.totals||{};shopIncome.value=data.shopIncome||[];months.value=data.months||[];};
const loadBills=async()=>{const data:any=await request.get('/api/admin/billing/bills',{params:{page:page.value,pageSize:20,status:filters.status||undefined,billingMonth:filters.billingMonth||undefined,keyword:filters.keyword||undefined}});bills.value=data.records||[];total.value=data.total||0;};
const loadAll=async()=>{loading.value=true;try{await Promise.all([loadPlan(),loadSummary()]);if(!filters.billingMonth&&months.value.length)filters.billingMonth=String(months.value[0].billing_month).slice(0,10);await Promise.all([loadSummary(),loadBills()]);}finally{loading.value=false;}};
const applyFilters=async()=>{page.value=1;loading.value=true;try{await Promise.all([loadSummary(),loadBills()]);}finally{loading.value=false;}};
const resetFilters=()=>{filters.status='';filters.billingMonth='';filters.keyword='';applyFilters();};
const savePlan=async()=>{if(!plan.id)return ElMessage.warning('当前没有可编辑的计费方案');savingPlan.value=true;try{await request.put(`/api/admin/billing/plans/${plan.id}`,plan);ElMessage.success('当前计费方案已更新');await loadPlan();}finally{savingPlan.value=false;}};
const openAdjust=(row:any)=>{currentBill.value=row;adjust.type='reduce';adjust.amount=Math.min(.01,Number(row.payable_amount||0));adjust.reason='';adjustDialog.value=true;};
const saveAdjust=async()=>{if(!adjust.reason.trim())return ElMessage.warning('请填写调整原因');savingAdjust.value=true;try{await request.post(`/api/admin/billing/bills/${currentBill.value.id}/adjust`,adjust);ElMessage.success('账单调整已保存');adjustDialog.value=false;await Promise.all([loadSummary(),loadBills()]);}finally{savingAdjust.value=false;}};
const showDetail=async(row:any)=>{detail.value=await request.get(`/api/admin/billing/bills/${row.id}`);detailDrawer.value=true;};
const syncPayment=async(outTradeNo:string)=>{const data:any=await request.post(`/api/admin/billing/payments/${outTradeNo}/sync`);ElMessage.success(`支付宝状态：${data.tradeState||'已核对'}`);detail.value=await request.get(`/api/admin/billing/bills/${detail.value.id}`);await Promise.all([loadSummary(),loadBills()]);};
onMounted(loadAll);
</script>

<style scoped>
.billing-admin{padding:20px 24px 28px;line-height:1.4}.billing-header{min-height:34px;margin-bottom:14px;display:flex;align-items:center;justify-content:space-between}.billing-header h2{margin:0;color:#1e293b;font-size:20px;line-height:28px}.billing-header p{margin:3px 0 0;color:#8a96a8;font-size:12px}.billing-metrics{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:10px;margin:14px 0}.billing-metric{min-width:0;min-height:72px;padding:12px;display:flex;align-items:center;gap:9px;border:1px solid #e7ecf3;border-radius:7px;background:#fff;transition:.2s}.billing-metric:hover{transform:translateY(-1px);box-shadow:0 5px 14px rgba(15,23,42,.06)}.billing-metric>span{width:32px;height:32px;flex:0 0 auto;display:grid;place-items:center;border-radius:7px;font-size:16px}.billing-metric div{min-width:0;display:flex;flex-direction:column}.billing-metric small{color:#7b8798;font-size:10px;line-height:15px}.billing-metric strong{margin:1px 0;color:#26364d;font-size:17px;line-height:22px}.billing-metric em{overflow:hidden;color:#a0aabd;font-size:9px;line-height:13px;font-style:normal;white-space:nowrap;text-overflow:ellipsis}.blue>span{color:#1a8cff;background:#eaf4ff}.green>span{color:#0f9f79;background:#e9f8f3}.orange>span{color:#f0782b;background:#fff2e9}.violet>span{color:#7c6bd4;background:#f0edff}.red>span{color:#e45b65;background:#fff0f1}.cyan>span{color:#168ca6;background:#e8f7fa}.billing-top-grid{display:grid;grid-template-columns:minmax(470px,.9fr) minmax(520px,1.1fr);gap:14px}.admin-panel{padding:16px;border:1px solid #e8edf5;border-radius:7px;background:#fff;margin-bottom:14px}.panel-heading{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:12px}.panel-heading h3{margin:0;color:#334155;font-size:14px;line-height:20px}.panel-heading p{margin:3px 0 0;color:#94a3b8;font-size:10px;line-height:15px}.plan-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.plan-grid .el-input-number{width:100%}.plan-grid .el-form-item{position:relative;margin-bottom:10px}.plan-grid .el-form-item small{position:absolute;right:9px;bottom:8px;color:#98a2b3;font-size:10px}.plan-switches{display:flex;align-items:center;gap:18px;padding:11px 0;border-top:1px solid #edf0f5;color:#64748b;font-size:11px}.plan-switches label{display:flex;align-items:center;gap:7px}.plan-switches span{margin-left:auto;color:#94a3b8}.plan-actions{display:flex;justify-content:flex-end}.income-value{color:#0f9f79}.bill-section{padding:16px}.billing-toolbar{display:grid;grid-template-columns:145px 145px minmax(220px,1fr) auto auto;gap:8px;margin-bottom:12px}.order-count,.time-text{color:#667085;font-size:11px}.discount-value{color:#d97706}.el-pagination{justify-content:flex-end;margin-top:12px}.dialog-heading,.drawer-heading{display:flex;flex-direction:column;gap:3px}.dialog-heading span,.drawer-heading span{color:#1e293b;font-size:15px;font-weight:700}.dialog-heading small,.drawer-heading small{color:#94a3b8;font-size:10px;font-weight:400}.adjust-summary{margin-bottom:14px;padding:11px 13px;display:flex;align-items:center;justify-content:space-between;border-radius:7px;background:#f7f9fc}.adjust-summary span{color:#64748b;font-size:11px}.adjust-summary strong{font-size:21px}.detail-content{padding:0 2px 20px}.detail-hero{padding:16px;display:flex;align-items:center;justify-content:space-between;border-radius:8px;background:linear-gradient(135deg,#f2f8ff,#fbfdff)}.detail-hero div{display:flex;flex-direction:column;gap:4px}.detail-hero span{color:#64748b;font-size:11px}.detail-hero strong{font-size:25px}.detail-grid{display:grid;grid-template-columns:repeat(2,1fr);margin:13px 0;border-top:1px solid #e8edf5;border-left:1px solid #e8edf5}.detail-grid div{padding:10px 12px;display:flex;flex-direction:column;gap:3px;border-right:1px solid #e8edf5;border-bottom:1px solid #e8edf5}.detail-grid span{color:#8a96a8;font-size:10px}.detail-grid b{color:#3f4d61;font-size:12px}.detail-section{margin-top:18px}.detail-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px}.detail-title h4{margin:0;font-size:13px}.detail-title span{color:#98a2b3;font-size:10px}.billing-admin :deep(.el-form-item){margin-bottom:12px}.billing-admin :deep(.el-form-item__label){padding-bottom:4px;font-size:11px;line-height:16px}.billing-admin :deep(.el-input__wrapper),.billing-admin :deep(.el-select__wrapper),.billing-admin :deep(.el-input-number){min-height:32px}.billing-admin :deep(.el-button){height:32px;font-size:12px}.billing-admin :deep(.el-table){font-size:12px}.billing-admin :deep(.el-table th.el-table__cell){height:38px;font-size:11px}.billing-admin :deep(.el-table td.el-table__cell){height:42px}.billing-admin :deep(.el-table .cell){line-height:18px;padding:0 8px}.billing-admin :deep(.el-tag){font-size:11px}.billing-admin :deep(.el-drawer__body){padding:16px 20px}.billing-admin :deep(.el-dialog__body){padding-top:14px;padding-bottom:4px}@media(max-width:1400px){.billing-metrics{grid-template-columns:repeat(3,1fr)}.billing-top-grid{grid-template-columns:1fr}}
.billing-admin :deep(.el-form-item__label){font-size:13px}.billing-admin :deep(.el-button){font-size:13px}.billing-admin :deep(.el-table){font-size:13px}.billing-admin :deep(.el-table th.el-table__cell){font-size:13px}.billing-admin :deep(.el-tag){font-size:12px}
</style>
