<template>
  <div class="admin-page admin-orders">
    <div class="admin-page__header">
      <div><h2>订单管理</h2><p class="admin-page__description">查询全平台订单并处理异常状态</p></div>
      <el-button :icon="Download" @click="download">导出 CSV</el-button>
    </div>

    <div class="admin-summary-strip">
      <div v-for="item in summaryItems" :key="item.label" class="admin-summary-item" :class="`admin-summary-item--${item.tone}`">
        <span class="admin-summary-item__icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <div><span>{{ item.label }}</span><strong>{{ item.prefix }}{{ item.value }}</strong></div>
      </div>
    </div>

    <section class="admin-filter-panel">
      <div class="admin-filter-panel__top">
        <div class="admin-filter-panel__title"><el-icon><Filter /></el-icon>筛选订单</div>
        <el-button link :icon="RefreshLeft" @click="resetFilters">重置筛选</el-button>
      </div>
      <div class="admin-filter-grid admin-filter-grid--orders">
        <label><span>订单搜索</span><el-input v-model="filters.keyword" placeholder="订单号或取餐码" clearable @keyup.enter="search" /></label>
        <label><span>所属商户</span><el-select v-model="filters.shopId" placeholder="全部商户" clearable filterable><el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" /></el-select></label>
        <label><span>订单状态</span><el-select v-model="filters.status" placeholder="全部状态" clearable><el-option v-for="status in statuses" :key="status.value" :label="status.label" :value="status.value" /></el-select></label>
        <label><span>订单类型</span><el-select v-model="filters.orderMode" placeholder="全部类型" clearable><el-option label="固定菜品" value="fixed_dish"/><el-option label="自选称重" value="weight_selection"/></el-select></label>
        <label><span>用户 ID</span><el-input v-model="filters.userId" placeholder="精确查询用户 ID" clearable /></label>
        <label class="admin-filter-grid__date"><span>下单时间</span><div class="admin-inline-date"><el-select v-model="datePreset" style="width:112px" @change="setDatePreset"><el-option label="今天" value="today"/><el-option label="近 7 天" value="7d"/><el-option label="近 30 天" value="30d"/><el-option label="自定义" value="custom"/></el-select><el-date-picker v-if="datePreset==='custom'" v-model="filters.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期"/><span v-if="datePreset==='custom'">至</span><el-date-picker v-if="datePreset==='custom'" v-model="filters.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期"/><span v-else class="admin-date-display">{{ dateRangeText }}</span></div></label>
      </div>
      <div class="admin-filter-panel__actions"><el-button type="primary" :icon="Search" @click="search">查询订单</el-button></div>
    </section>

    <div class="admin-panel admin-table-panel">
      <el-table :data="records" v-loading="loading" @row-dblclick="openDetail">
        <el-table-column prop="id" label="订单号" min-width="180"><template #default="scope"><span class="admin-order-id">{{ scope.row.id }}</span></template></el-table-column>
        <el-table-column prop="pickup_code" label="取餐码" width="90"><template #default="scope"><strong class="admin-pickup-code">{{ scope.row.pickup_code }}</strong></template></el-table-column>
        <el-table-column prop="shop_name" label="商户" min-width="130" show-overflow-tooltip/>
        <el-table-column label="用户" min-width="120"><template #default="scope"><div>{{ scope.row.user_name || '未实名用户' }}</div><small class="admin-cell-muted">{{ scope.row.phone || `ID ${scope.row.user_id}` }}</small></template></el-table-column>
        <el-table-column label="类型" width="100"><template #default="scope">{{ modeLabel(scope.row.order_mode) }}</template></el-table-column>
        <el-table-column label="金额" width="105"><template #default="scope"><span class="admin-money">¥{{ money(scope.row.total_amount) }}</span></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="tagType(scope.row.status)" effect="light">{{ statusLabel(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="create_time" label="下单时间" width="170"/>
        <el-table-column label="操作" width="150" fixed="right"><template #default="scope"><el-button link type="primary" :icon="View" @click="openDetail(scope.row)">详情</el-button><el-button link type="danger" :icon="EditPen" @click="openCorrection(scope.row)">纠正</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="load" class="admin-pagination" />
    </div>

    <el-drawer v-model="detailDrawer" size="760px" class="admin-detail-drawer" destroy-on-close>
      <template #header><div class="admin-drawer-title"><span class="admin-drawer-title__icon"><el-icon><Tickets /></el-icon></span><div><strong>订单详情</strong><small>{{ current?.id }}</small></div></div></template>
      <div v-if="current" class="admin-order-detail">
        <div class="admin-order-hero">
          <div><span>当前状态</span><strong>{{ statusLabel(current.status) }}</strong></div>
          <div><span>取餐码</span><strong class="admin-order-hero__code">{{ current.pickup_code }}</strong></div>
          <div><span>实付金额</span><strong class="admin-order-hero__money">¥{{ money(current.total_amount) }}</strong></div>
        </div>
        <section class="admin-detail-section"><h4>基础信息</h4><el-descriptions :column="2" border><el-descriptions-item label="商户">{{ current.shop_name }}</el-descriptions-item><el-descriptions-item label="用户">{{ current.user_name || '-' }}（{{ current.user_phone || '-' }}）</el-descriptions-item><el-descriptions-item label="订单类型">{{ modeLabel(current.order_mode) }}</el-descriptions-item><el-descriptions-item label="用餐方式">{{ current.need_pack ? '打包带走' : '堂食' }}</el-descriptions-item><el-descriptions-item label="预约取餐">{{ current.pickup_time || '-' }}</el-descriptions-item><el-descriptions-item label="业务日期">{{ current.biz_date || '-' }}</el-descriptions-item><el-descriptions-item label="用户备注" :span="2">{{ current.remark || '无备注' }}</el-descriptions-item></el-descriptions></section>
        <section class="admin-detail-section"><h4>商品明细</h4><el-table v-if="current.items?.length" :data="current.items"><el-table-column prop="dish_name" label="商品"><template #default="scope"><div>{{ scope.row.dish_name }}</div><small v-if="scope.row.options" class="admin-cell-muted">{{ scope.row.options }}</small></template></el-table-column><el-table-column prop="quantity" label="数量" width="70"/><el-table-column label="单价" width="95"><template #default="scope">¥{{ money(scope.row.price) }}</template></el-table-column><el-table-column label="小计" width="100"><template #default="scope"><strong>¥{{ money(Number(scope.row.price) * Number(scope.row.quantity)) }}</strong></template></el-table-column></el-table>
          <el-table v-if="current.weightItems?.length" :data="current.weightItems" class="admin-sub-table"><el-table-column prop="ingredient_name" label="自选食材"/><el-table-column prop="estimated_weight_g" label="预估重量(g)"/><el-table-column prop="final_weight_g" label="最终重量(g)"/></el-table>
        </section>
        <section v-if="current.order_mode==='weight_selection'" class="admin-detail-section"><h4>称重与定价</h4><div class="admin-info-grid"><span>预估重量<strong>{{ current.estimated_weight_g || '-' }} g</strong></span><span>最终重量<strong>{{ current.final_weight_g || '-' }} g</strong></span><span>预估金额<strong>¥{{ money(current.estimated_amount) }}</strong></span><span>最终金额<strong>¥{{ money(current.final_amount) }}</strong></span></div><el-image v-if="current.price_evidence_image" :src="current.price_evidence_image" fit="cover" class="admin-evidence-image" :preview-src-list="[current.price_evidence_image]"/></section>
        <section class="admin-detail-section"><h4>订单时间线</h4><el-timeline><el-timeline-item :timestamp="current.create_time">用户提交订单</el-timeline-item><el-timeline-item v-if="current.pay_time" :timestamp="current.pay_time" type="primary">订单支付</el-timeline-item><el-timeline-item v-if="current.ready_time" :timestamp="current.ready_time" type="warning">制作完成，等待取餐</el-timeline-item><el-timeline-item v-if="current.completed_time" :timestamp="current.completed_time" type="success">用户完成取餐</el-timeline-item><el-timeline-item v-if="current.cancel_time" :timestamp="current.cancel_time" type="danger">订单取消：{{ current.cancel_reason }}</el-timeline-item></el-timeline></section>
      </div>
    </el-drawer>

    <el-dialog v-model="correctionDialog" title="纠正订单状态" width="500px" destroy-on-close>
      <el-alert title="该操作会修改订单状态并写入管理员操作日志，请确认业务事实后再提交。" type="warning" :closable="false" show-icon />
      <el-form label-position="top" class="admin-dialog-form"><el-form-item label="订单"><el-input :model-value="correctionOrder?.id" disabled/></el-form-item><el-form-item label="当前状态"><el-tag :type="tagType(correctionOrder?.status)">{{ statusLabel(correctionOrder?.status) }}</el-tag></el-form-item><el-form-item label="纠正为"><el-select v-model="correction.targetStatus" style="width:100%"><el-option v-for="status in statuses" :key="status.value" :label="status.label" :value="status.value"/></el-select></el-form-item><el-form-item label="操作原因"><el-input v-model="correction.reason" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="请填写可追溯的纠正原因"/></el-form-item></el-form>
      <template #footer><el-button @click="correctionDialog=false">取消</el-button><el-button type="danger" @click="correct">确认纠正</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CircleCheck, Download, EditPen, Filter, Finished, Money, RefreshLeft, Search, Tickets, Timer, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import './admin.css';

const statuses = [{ value:'making', label:'制作中' }, { value:'pending', label:'待取餐' }, { value:'completed', label:'已完成' }, { value:'cancelled', label:'已取消' }];
const filters = reactive<any>({ keyword:'', shopId:null, status:'', orderMode:'', userId:'', startDate:'', endDate:'' });
const records = ref<any[]>([]), shops = ref<any[]>([]), total = ref(0), page = ref(1), loading = ref(false);
const summary = ref<any>({}), detailDrawer = ref(false), correctionDialog = ref(false), current = ref<any>(), correctionOrder = ref<any>();
const correction = reactive({ targetStatus:'', reason:'' });
const datePreset = ref('7d');

const formatDate = (date:Date) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const before = (days:number) => { const date=new Date(); date.setDate(date.getDate()-days); return formatDate(date); };
const setDatePreset = (value:string) => { const today=formatDate(new Date()); if(value==='today') filters.startDate=filters.endDate=today; else if(value==='7d'){filters.startDate=before(6);filters.endDate=today}else if(value==='30d'){filters.startDate=before(29);filters.endDate=today}else{filters.startDate='';filters.endDate=''} };
const dateRangeText = computed(() => filters.startDate && filters.endDate ? `${filters.startDate} 至 ${filters.endDate}` : '请选择自定义日期');
const money = (value:any) => Number(value || 0).toFixed(2);
const statusLabel = (value:string) => statuses.find(status => status.value===value)?.label || value || '-';
const tagType = (value:string) => value==='completed'?'success':value==='cancelled'?'danger':value==='pending'?'warning':'primary';
const modeLabel = (value:string) => value==='weight_selection'?'自选称重':'固定菜品';
const summaryItems = computed(() => [
  { label:'今日订单', value:summary.value.todayOrders||0, prefix:'', tone:'blue', icon:Tickets },
  { label:'制作中', value:summary.value.makingCount||0, prefix:'', tone:'cyan', icon:Timer },
  { label:'待取餐', value:summary.value.pendingCount||0, prefix:'', tone:'orange', icon:Finished },
  { label:'今日完成', value:summary.value.completedCount||0, prefix:'', tone:'green', icon:CircleCheck },
  { label:'今日成交', value:money(summary.value.todayRevenue), prefix:'¥', tone:'violet', icon:Money },
]);

const load = async () => { loading.value=true; try { const response:any=await request.get('/api/admin/orders',{params:{page:page.value,pageSize:20,keyword:filters.keyword||undefined,shopId:filters.shopId||undefined,userId:filters.userId||undefined,status:filters.status||undefined,orderMode:filters.orderMode||undefined,startDate:filters.startDate||undefined,endDate:filters.endDate||undefined}}); records.value=response.records;total.value=response.total } finally { loading.value=false } };
const loadBase = async () => { const [shopRows, summaryData]:any = await Promise.all([request.get('/api/admin/shops/options'), request.get('/api/admin/orders/summary')]); shops.value=shopRows; summary.value=summaryData; };
const search = () => { page.value=1; load(); };
const resetFilters = () => { Object.assign(filters,{keyword:'',shopId:null,status:'',orderMode:'',userId:'',startDate:'',endDate:''});datePreset.value='7d';setDatePreset('7d');search(); };
const openDetail = async (row:any) => { current.value=await request.get(`/api/admin/orders/${row.id}`);detailDrawer.value=true; };
const openCorrection = (row:any) => { correctionOrder.value=row;correction.targetStatus=row.status;correction.reason='';correctionDialog.value=true; };
const correct = async () => { if(!correction.reason.trim()) return ElMessage.warning('请填写操作原因');await ElMessageBox.confirm(`确认将订单改为“${statusLabel(correction.targetStatus)}”？`,'确认高风险操作',{type:'warning'});await request.put(`/api/admin/orders/${correctionOrder.value.id}/status`,correction);ElMessage.success('订单状态已纠正');correctionDialog.value=false;await Promise.all([load(),loadBase()]); };
const download = () => fetch('/api/admin/export/orders',{headers:{Authorization:`Bearer ${localStorage.getItem('token')}`}}).then(response=>response.blob()).then(blob=>{const link=document.createElement('a');link.href=URL.createObjectURL(blob);link.download='orders.csv';link.click();URL.revokeObjectURL(link.href)});

setDatePreset('7d');
onMounted(() => { loadBase(); load(); });
</script>
