<template>
  <div ref="chartElement" class="admin-chart"></div>
</template>

<script setup lang="ts">
import { BarChart, HeatmapChart, LineChart, PieChart } from 'echarts/charts';
import {
  CalendarComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components';
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

use([
  BarChart,
  LineChart,
  PieChart,
  HeatmapChart,
  CalendarComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
  CanvasRenderer,
]);

const props = defineProps<{ option: EChartsCoreOption }>();
const chartElement = ref<HTMLElement>();
let chart: ECharts | undefined;
let resizeObserver: ResizeObserver | undefined;

const render = async () => {
  await nextTick();
  if (!chartElement.value) return;
  chart ??= init(chartElement.value, undefined, { renderer: 'canvas' });
  chart.setOption(props.option, { notMerge: true });
};

watch(() => props.option, render, { deep: true });

onMounted(() => {
  render();
  resizeObserver = new ResizeObserver(() => chart?.resize());
  if (chartElement.value) resizeObserver.observe(chartElement.value);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chart?.dispose();
});
</script>

<style scoped>
.admin-chart {
  width: 100%;
  height: 100%;
  min-height: 0;
}
</style>
