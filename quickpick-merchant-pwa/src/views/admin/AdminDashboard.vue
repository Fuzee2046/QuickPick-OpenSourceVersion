<template>
  <div class="admin-page admin-dashboard" v-loading="loading">
    <div class="admin-page__header admin-dashboard__header">
      <div>
        <h2>经营概览</h2>
        <p>统计区间：{{ rangeText }}</p>
      </div>
      <div class="admin-date-filter">
        <el-radio-group v-model="activeRange" @change="changeRange">
          <el-radio-button value="today">今天</el-radio-button>
          <el-radio-button value="7d">近 7 天</el-radio-button>
          <el-radio-button value="30d">近 30 天</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
        <el-popover v-model:visible="customVisible" placement="bottom-end" :width="350" trigger="click">
          <template #reference>
            <el-button :type="activeRange === 'custom' ? 'primary' : 'default'" :icon="Calendar">自定义</el-button>
          </template>
          <div class="admin-custom-date">
            <div class="admin-custom-date__title">自定义统计区间</div>
            <label>开始日期<el-date-picker v-model="customStart" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" /></label>
            <label>结束日期<el-date-picker v-model="customEnd" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期" /></label>
            <div class="admin-custom-date__actions">
              <el-button @click="customVisible=false">取消</el-button>
              <el-button type="primary" @click="applyCustomRange">应用</el-button>
            </div>
          </div>
        </el-popover>
        <el-button circle :icon="Refresh" title="刷新数据" @click="load" />
      </div>
    </div>

    <div class="admin-metrics">
      <div class="admin-metric admin-metric--blue"><span class="admin-metric__icon"><el-icon><Tickets /></el-icon></span><div><div class="admin-metric__label">订单总量</div><div class="admin-metric__value">{{ summary.orderCount || 0 }}</div></div></div>
      <div class="admin-metric admin-metric--green"><span class="admin-metric__icon"><el-icon><CircleCheck /></el-icon></span><div><div class="admin-metric__label">完成订单</div><div class="admin-metric__value">{{ summary.completedCount || 0 }}</div></div></div>
      <div class="admin-metric admin-metric--orange"><span class="admin-metric__icon"><el-icon><Wallet /></el-icon></span><div><div class="admin-metric__label">成交金额</div><div class="admin-metric__value">¥{{ money(summary.revenue) }}</div></div></div>
      <div class="admin-metric admin-metric--cyan"><span class="admin-metric__icon"><el-icon><TrendCharts /></el-icon></span><div><div class="admin-metric__label">客单价</div><div class="admin-metric__value">¥{{ money(summary.averageOrderValue) }}</div></div></div>
      <div class="admin-metric admin-metric--red"><span class="admin-metric__icon"><el-icon><CircleClose /></el-icon></span><div><div class="admin-metric__label">取消订单</div><div class="admin-metric__value">{{ summary.cancelledCount || 0 }}</div></div></div>
      <div class="admin-metric admin-metric--violet"><span class="admin-metric__icon"><el-icon><User /></el-icon></span><div><div class="admin-metric__label">活跃用户</div><div class="admin-metric__value">{{ summary.activeUsers || 0 }}</div></div></div>
      <div class="admin-metric admin-metric--teal"><span class="admin-metric__icon"><el-icon><Shop /></el-icon></span><div><div class="admin-metric__label">有单商户</div><div class="admin-metric__value">{{ summary.activeShops || 0 }}</div></div></div>
    </div>

    <div class="admin-dashboard__calendar-row">
      <section class="admin-panel admin-chart-panel admin-chart-panel--calendar">
        <div class="admin-chart-panel__header">
          <div><h3>年度订单日历</h3><p>{{ data.calendarYear || new Date().getFullYear() }} 年每日订单活跃度，颜色越深订单越多</p></div>
          <span class="admin-chart-panel__badge">日历热力图</span>
        </div>
        <div class="admin-chart-panel__body admin-chart-panel__body--calendar"><AdminChart :option="calendarOption" /></div>
      </section>
      <section class="admin-panel admin-chart-panel admin-chart-panel--status">
        <div class="admin-chart-panel__header"><div><h3>订单状态</h3><p>当前筛选区间的订单占比</p></div></div>
        <div class="admin-chart-panel__body admin-chart-panel__body--calendar"><AdminChart :option="statusOption" /></div>
      </section>
    </div>

    <section class="admin-panel admin-chart-panel admin-chart-panel--wide">
      <div class="admin-chart-panel__header">
        <div><h3>每日商户订单</h3><p>不同颜色代表不同商户，柱高为当天订单总量</p></div>
        <span class="admin-chart-panel__badge">按商户堆叠</span>
      </div>
      <div class="admin-chart-panel__body admin-chart-panel__body--large"><AdminChart :option="shopOrderOption" /></div>
    </section>

    <div class="admin-dashboard__charts">
      <section class="admin-panel admin-chart-panel admin-chart-panel--trend">
        <div class="admin-chart-panel__header"><div><h3>经营趋势</h3><p>订单量与成交金额变化</p></div></div>
        <div class="admin-chart-panel__body"><AdminChart :option="trendOption" /></div>
      </section>
      <section class="admin-panel admin-chart-panel">
        <div class="admin-chart-panel__header"><div><h3>商户排行</h3><p>按订单数量排名</p></div></div>
        <div class="admin-chart-panel__body"><AdminChart :option="shopRankOption" /></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { EChartsCoreOption } from 'echarts/core';
import { Calendar, CircleCheck, CircleClose, Refresh, Shop, Tickets, TrendCharts, User, Wallet } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import AdminChart from '@/components/admin/AdminChart.vue';
import request from '@/utils/request';
import './admin.css';

const chartColors = ['#1a8cff', '#ff8a3d', '#12a594', '#7c6bd4', '#e45b65', '#168ca6', '#e6ad32', '#5b7cda'];
const statusLabels: Record<string, string> = { making: '制作中', pending: '待取餐', completed: '已完成', cancelled: '已取消' };
const statusColors: Record<string, string> = { making: '#1a8cff', pending: '#ff8a3d', completed: '#12a594', cancelled: '#e45b65' };
const data = ref<any>({});
const loading = ref(false);
const activeRange = ref('today');
const customVisible = ref(false);
const customStart = ref('');
const customEnd = ref('');
const dates = ref<[string, string]>(['', '']);
const summary = computed(() => data.value.summary || {});

const formatDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};
const dateBefore = (days: number) => {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return formatDate(date);
};
const money = (value: unknown) => Number(value || 0).toFixed(2);
const rangeText = computed(() => `${dates.value[0]} 至 ${dates.value[1]}`);

const setPresetRange = (range: string) => {
  const today = formatDate(new Date());
  if (range === 'today') dates.value = [today, today];
  else if (range === '30d') dates.value = [dateBefore(29), today];
  else if (range === 'month') dates.value = [`${today.slice(0, 8)}01`, today];
  else dates.value = [dateBefore(6), today];
};

const changeRange = (range: string | number | boolean | undefined) => {
  setPresetRange(String(range));
  load();
};

const applyCustomRange = () => {
  if (!customStart.value || !customEnd.value) return ElMessage.warning('请选择完整的开始和结束日期');
  if (customStart.value > customEnd.value) return ElMessage.warning('开始日期不能晚于结束日期');
  dates.value = [customStart.value, customEnd.value];
  activeRange.value = 'custom';
  customVisible.value = false;
  load();
};

const load = async () => {
  loading.value = true;
  try {
    data.value = await request.get('/api/admin/dashboard', { params: { startDate: dates.value[0], endDate: dates.value[1] } });
  } finally {
    loading.value = false;
  }
};

const axisStyle = {
  axisLine: { lineStyle: { color: '#dfe5ec' } },
  axisTick: { show: false },
  axisLabel: { color: '#7b8798', fontSize: 11 },
};
const tooltipStyle = { backgroundColor: 'rgba(30,41,59,.94)', borderWidth: 0, textStyle: { color: '#fff', fontSize: 12 } };

const dateAxis = computed(() => {
  const values: string[] = [];
  const cursor = new Date(`${dates.value[0]}T00:00:00`);
  const end = new Date(`${dates.value[1]}T00:00:00`);
  while (cursor <= end) {
    values.push(formatDate(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return values;
});

const shopOrderOption = computed<EChartsCoreOption>(() => {
  const rows = data.value.dailyShopTrend || [];
  const shops = [...new Map(rows.map((row: any) => [row.shopId, row.shopName])).entries()];
  return {
    color: chartColors,
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { type: 'scroll', top: 0, right: 0, left: 0, textStyle: { color: '#64748b', fontSize: 11 }, itemWidth: 10, itemHeight: 7 },
    grid: { left: 14, right: 14, top: 48, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: dateAxis.value.map((date) => date.slice(5)), ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf1f5', type: 'dashed' } }, ...axisStyle },
    series: shops.map(([shopId, shopName], index) => ({
      name: String(shopName), type: 'bar', stack: 'orders', barMaxWidth: 42,
      itemStyle: { borderRadius: index === shops.length - 1 ? [3, 3, 0, 0] : 0 },
      emphasis: { focus: 'series' },
      data: dateAxis.value.map((date) => Number(rows.find((row: any) => String(row.date) === date && String(row.shopId) === String(shopId))?.orderCount || 0)),
    })),
  };
});

const calendarOption = computed<EChartsCoreOption>(() => {
  const rows = data.value.calendarTrend || [];
  const maxValue = Math.max(1, ...rows.map((row: any) => Number(row.orderCount || 0)));
  return {
    tooltip: { ...tooltipStyle, formatter: (params: any) => `${params.value[0]}<br/>订单量：${params.value[1]} 单` },
    visualMap: {
      min: 0, max: maxValue, calculable: false, orient: 'horizontal', right: 8, top: 0,
      itemWidth: 12, itemHeight: 90, text: ['多', '少'], textStyle: { color: '#7b8798', fontSize: 10 },
      inRange: { color: ['#edf6ff', '#b9dcff', '#6db4ff', '#1a8cff', '#0869c8'] },
    },
    calendar: {
      top: 58, left: 42, right: 12, bottom: 4, range: String(data.value.calendarYear || new Date().getFullYear()),
      cellSize: ['auto', 14], splitLine: { show: false },
      itemStyle: { color: '#f5f7fa', borderColor: '#fff', borderWidth: 2, borderRadius: 2 },
      yearLabel: { show: false }, monthLabel: { color: '#64748b', fontSize: 10 }, dayLabel: { color: '#94a3b8', fontSize: 10, firstDay: 1, nameMap: ['日', '一', '二', '三', '四', '五', '六'] },
    },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: rows.map((row: any) => [String(row.date), Number(row.orderCount || 0)]) }],
  };
});

const trendOption = computed<EChartsCoreOption>(() => {
  const rows = data.value.trend || [];
  const valueFor = (date: string, field: string) => Number(rows.find((row: any) => String(row.date) === date)?.[field] || 0);
  return {
    color: ['#1a8cff', '#ff8a3d'],
    tooltip: { ...tooltipStyle, trigger: 'axis' },
    legend: { top: 0, right: 0, textStyle: { color: '#64748b', fontSize: 11 }, itemWidth: 12, itemHeight: 7 },
    grid: { left: 10, right: 12, top: 42, bottom: 8, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dateAxis.value.map((date) => date.slice(5)), ...axisStyle },
    yAxis: [
      { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf1f5', type: 'dashed' } }, ...axisStyle },
      { type: 'value', splitLine: { show: false }, axisLabel: { color: '#7b8798', fontSize: 11, formatter: '¥{value}' }, axisLine: { show: false }, axisTick: { show: false } },
    ],
    series: [
      { name: '订单量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6, data: dateAxis.value.map((date) => valueFor(date, 'orderCount')), areaStyle: { color: 'rgba(26,140,255,.13)' }, lineStyle: { width: 2 } },
      { name: '成交额', type: 'line', smooth: true, yAxisIndex: 1, symbol: 'none', data: dateAxis.value.map((date) => valueFor(date, 'revenue')), lineStyle: { width: 2 } },
    ],
  };
});

const statusOption = computed<EChartsCoreOption>(() => ({
  color: ['#1a8cff', '#ff8a3d', '#12a594', '#e45b65'],
  tooltip: { ...tooltipStyle, trigger: 'item', formatter: '{b}<br/>{c} 单（{d}%）' },
  legend: { orient: 'vertical', right: 6, top: 'middle', textStyle: { color: '#64748b', fontSize: 11 }, itemWidth: 10, itemHeight: 7 },
  series: [{
    type: 'pie', radius: ['48%', '70%'], center: ['38%', '54%'], avoidLabelOverlap: true,
    itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
    label: { show: false }, emphasis: { label: { show: true, fontSize: 13, fontWeight: 700 } },
    data: (data.value.statusDistribution || []).map((row: any) => ({ name: statusLabels[row.status] || row.status, value: row.count, itemStyle: { color: statusColors[row.status] } })),
  }],
}));

const shopRankOption = computed<EChartsCoreOption>(() => {
  const rows = [...(data.value.topShops || [])].sort((a: any, b: any) => Number(a.orderCount) - Number(b.orderCount)).slice(-6);
  return {
    color: ['#1a8cff'],
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 18, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf1f5', type: 'dashed' } }, ...axisStyle },
    yAxis: { type: 'category', data: rows.map((row: any) => row.name), axisLabel: { color: '#64748b', fontSize: 11, width: 70, overflow: 'truncate' }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{ type: 'bar', barWidth: 14, data: rows.map((row: any, index: number) => ({ value: row.orderCount, itemStyle: { color: chartColors[index % chartColors.length], borderRadius: [0, 4, 4, 0] } })), label: { show: true, position: 'right', color: '#64748b', fontSize: 11 } }],
  };
});

setPresetRange('today');
onMounted(load);
</script>
