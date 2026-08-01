<template>
  <div
    class="dashboard-container"
    :class="{ 'dashboard-container--with-history-pagination': showHistoryPagination }"
  >
    <StaleOrderDialog @resolved="refreshDashboardData" />
    <!-- 1. 顶部状态栏与数据看板 -->
    <div class="header-section">
      <div class="shop-info">
        <h1 class="shop-name">{{ auth.shopName || '我的店铺' }}</h1>
        <div class="current-time">{{ currentTime }}</div>
      </div>

      <div v-if="showSoundEnableCard" class="sound-enable-card">
        <div class="sound-enable-copy">
          <div class="sound-enable-title">开启新订单提醒音</div>
          <div class="sound-enable-desc">手机浏览器会拦截自动播放，请先手动开启一次</div>
        </div>
        <button class="sound-enable-btn" @click="enableSoundAlert">
          {{ enablingSound ? '开启中...' : '开启提醒音' }}
        </button>
      </div>

      <div class="stats-board">
        <div class="stat-item clickable" @click="activeTab = 'making'">
          <div class="stat-value highlight">{{ stats.makingOrders || 0 }}<span class="unit">单</span></div>
          <div class="stat-label">🔥 待制作</div>
        </div>
        <div class="divider"></div>
        <div class="stat-item clickable" @click="activeTab = 'pending'">
          <div class="stat-value warning">{{ stats.pendingOrders || 0 }}<span class="unit">单</span></div>
          <div class="stat-label">🥡 待取餐</div>
        </div>
        <div class="divider"></div>
        <div class="stat-item clickable" @click="activeTab = 'completed'">
          <div class="stat-value success">{{ stats.completedOrders || 0 }}<span class="unit">单</span></div>
          <div class="stat-label">✅ 已完成</div>
        </div>
        <div class="divider"></div>
        <div class="stat-item">
          <div class="stat-value primary">¥{{ formatMoney(stats.totalRevenue) }}</div>
          <div class="stat-label">💰 今日营收</div>
        </div>
      </div>
    </div>

    <!-- 3. 标签页/筛选栏 (Segmented Control Style) -->
    <div class="tab-control">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'making' }"
        @click="activeTab = 'making'"
      >
        制作中
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'pending' }"
        @click="activeTab = 'pending'"
      >
        待取餐
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'completed' }"
        @click="activeTab = 'completed'"
      >
        已完成
      </div>
    </div>

    <!-- 4. 固定菜品店显示待制作汇总，自选称重店不显示 -->
    <div v-if="!isWeightSelectionShop && dishSummary.length > 0" class="dish-summary-card">
      <div class="summary-header">
        <span class="title">{{ activeTab === 'making' ? '🍳 待制作汇总' : '🥡 待取餐汇总' }}</span>
        <span class="count">共 {{ dishSummary.length }} 种</span>
      </div>
      <div class="summary-list">
        <div 
          v-for="dish in dishSummary" 
          :key="dish.name"
          class="summary-item"
          :class="{ active: filterDishName === dish.name }"
          @click="toggleDishFilter(dish.name)"
        >
          <div class="dish-name">{{ dish.name }}</div>
          <div class="dish-count">{{ dish.count }}</div>
        </div>
      </div>
    </div>

    <!-- 2. 订单列表（核心交互区） -->
    <div class="order-list-container">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <div v-if="activeList.length === 0 && !loading" class="empty-state">
           <van-empty description="暂无新订单，休息一下吧" image="default" />
        </div>

        <div class="order-list">
          <div 
            v-for="order in activeList" 
            :key="order.id" 
            class="order-card"
            :class="{ 'new-arrival': order.isNew }"
            @click="router.push(`/order/${order.id}`)"
          >
            <!-- 左侧信息区 -->
            <div class="card-left">
              <div class="header-row">
                <span class="pickup-code">#{{ order.pickupCode }}</span>
                <div class="time-info">
                   <span v-if="activeTab === 'completed' && order.status === 'cancelled'" class="order-time cancelled-time">取消: {{ formatTime(order.cancelTime || order.updateTime || order.createTime) }}</span>
                   <span v-else-if="activeTab === 'completed'" class="order-time">完成: {{ formatTime(order.completedTime || order.updateTime || order.createTime) }}</span>
                   <span v-else class="order-time highlight">取餐: {{ order.pickupTime || formatTime(order.createTime) }}</span>
                </div>
              </div>
              
              <div class="dish-summary">
                <template v-if="isWeightSelectionShop">
                  <div v-if="order.brothName" class="dish-item">
                    <span class="bullet">•</span> {{ order.brothName }}
                  </div>
                  <div class="more-items">
                    等{{ getOrderIngredientCount(order) }}件食材
                  </div>
                </template>
                <template v-else>
                  <div v-for="(item, idx) in order.items.slice(0, 2)" :key="idx" class="dish-item">
                    <span class="bullet">•</span>
                    <span class="dish-item__text">
                      {{ item.dishName }} x{{ item.quantity }}
                      <span v-if="item.options" class="dish-item__options">（{{ item.options }}）</span>
                    </span>
                  </div>
                  <div v-if="order.items.length > 2" class="more-items">
                    等{{ order.items.length }}件商品...
                  </div>
                </template>
              </div>

              <div class="tags-row">
                <!-- Pack Tag -->
                <div v-if="order.needPack === 1" class="tag pack-tag">
                  <span class="icon">🥡</span> 打包
                </div>
                <div v-else class="tag dine-tag">
                  <span class="icon">🍽️</span> 堂食
                </div>
                
                <!-- Remark Tag -->
                <div v-if="order.remark" class="tag remark-tag">
                  <span class="icon">📝</span> {{ formatRemark(order.remark) }}
                </div>
                <div v-if="activeTab === 'completed' && order.status === 'cancelled' && order.cancelReason" class="tag cancel-tag">
                  <span class="icon">🚫</span> {{ order.cancelReason }}
                </div>
                
                <span class="price-tag">¥{{ order.totalAmount }}</span>
              </div>
            </div>

            <!-- 右侧操作区 -->
            <div class="card-right">
              <button 
                v-if="activeTab === 'making' && order.pricingStatus === 'pending_confirm'"
                class="action-btn complete-btn"
                style="background-color: #ff8a3d;"
                @click.stop="openConfirmPricePopup(order)"
              >
                确认<br>金额
              </button>
              <button 
                v-else-if="activeTab === 'making'"
                class="action-btn"
                @click.stop="confirmPrepareOrder(order.id)"
              >
                制作<br>完成
              </button>
              <button 
                v-else-if="activeTab === 'pending'"
                class="action-btn complete-btn"
                style="background-color: #00b894;"
                @click.stop="confirmCompleteOrder(order.id)"
              >
                确认<br>取餐
              </button>
              <div v-else-if="activeTab === 'completed' && order.status === 'cancelled'" class="status-text cancelled">
                已取消
              </div>
              <div v-else-if="activeTab === 'completed'" class="status-text completed">
                已完成
              </div>
              <div v-else class="status-text completed">已完成</div>
            </div>
          </div>
        </div>

      </van-pull-refresh>
    </div>

    <div v-if="showHistoryPagination" class="history-pagination" aria-label="历史订单分页">
      <button
        class="history-pagination__button"
        type="button"
        aria-label="上一页"
        :disabled="historyPage <= 1 || loading"
        @click="changeHistoryPage(historyPage - 1)"
      >
        <van-icon name="arrow-left" />
      </button>
      <div class="history-pagination__status">
        <span class="history-pagination__page">
          <template v-if="loading">加载中...</template>
          <template v-else>第 {{ historyPage }} / {{ historyTotalPages }} 页</template>
        </span>
        <span class="history-pagination__total">共 {{ historyTotal }} 单</span>
      </div>
      <button
        class="history-pagination__button"
        type="button"
        aria-label="下一页"
        :disabled="historyPage >= historyTotalPages || loading"
        @click="changeHistoryPage(historyPage + 1)"
      >
        <van-icon name="arrow" />
      </button>
    </div>

    <van-popup
      v-model:show="showConfirmPricePopup"
      class="confirm-price-popup"
      position="bottom"
      round
      :style="{ height: '85vh', maxHeight: '85vh' }"
    >
      <div class="confirm-price-sheet">
        <div class="sheet-header">
          <div class="sheet-close" @click="closeConfirmPricePopup">
            <van-icon name="cross" />
          </div>
        </div>

        <div v-if="confirmPriceOrder" class="sheet-body">
          <div class="sheet-form-card sheet-form-card--top">
            <van-field
              v-model="confirmPriceForm.finalWeightG"
              label="最终重量"
              type="number"
              placeholder="请输入最终重量"
              input-align="right"
              class="sheet-weight-field"
            >
              <template #button>
                <span class="field-suffix">g</span>
              </template>
            </van-field>

            <div class="sheet-compare-card">
              <div class="sheet-compare-header">
                <span class="sheet-compare-header__empty"></span>
                <span class="sheet-compare-header__value">预计</span>
                <span class="sheet-compare-header__value">最终</span>
              </div>
              <div class="sheet-compare-row">
                <span class="sheet-compare-row__label">重量</span>
                <span class="sheet-compare-row__value">{{ formatEstimatedWeight(confirmPriceOrder.estimatedWeightG) }}</span>
                <span class="sheet-compare-row__value sheet-compare-row__value--highlight">{{ finalWeightDisplay }}</span>
              </div>
              <div class="sheet-compare-row">
                <span class="sheet-compare-row__label">金额</span>
                <span class="sheet-compare-row__value">{{ formatEstimatedAmount(confirmPriceOrder.estimatedAmount) }}</span>
                <span class="sheet-compare-row__value sheet-compare-row__value--amount">{{ confirmPriceFinalAmountText }}</span>
              </div>
            </div>
          </div>

          <div v-if="confirmPriceEvidenceRequired" class="sheet-evidence-card">
            <div class="sheet-evidence-title">请上传称重凭证</div>
            <div class="sheet-evidence-desc">
              当前最终重量超出预计容差范围。为避免学生质疑虚假增重，请上传电子秤称重照片留证。
            </div>
            <div class="sheet-evidence-tip">{{ confirmPriceEvidenceRequirementText }}</div>
            <div v-if="confirmPriceForm.priceEvidenceImage" class="sheet-evidence-preview">
              <img :src="confirmPriceForm.priceEvidenceImage" alt="称重凭证" class="sheet-evidence-preview__image" />
            </div>
            <div class="sheet-evidence-actions">
              <button
                class="sheet-evidence-btn sheet-evidence-btn--primary"
                :disabled="confirmPriceEvidenceUploading"
                @click="triggerConfirmPriceEvidencePicker"
              >
                {{ confirmPriceEvidenceUploading ? '上传中...' : confirmPriceForm.priceEvidenceImage ? '重新上传' : '拍照上传' }}
              </button>
              <span class="sheet-evidence-status">
                {{ confirmPriceForm.priceEvidenceImage ? '已上传称重凭证' : '点击后将直接弹出系统相机/相册选择' }}
              </span>
            </div>
          </div>

          <div class="sheet-items">
            <div
              v-if="confirmPriceOrder.brothName"
              class="sheet-item sheet-item--broth"
            >
              <div class="sheet-item-main">
                <span class="sheet-item-name">{{ confirmPriceOrder.brothName }}</span>
              </div>
              <div class="sheet-item-side">
                <span class="sheet-item-price">{{ formatBrothExtraPrice(confirmPriceOrder.brothExtraPrice) }}</span>
              </div>
            </div>
            <div
              v-for="(item, index) in confirmPriceOrder.items || []"
              :key="`${confirmPriceOrder.id}-${index}`"
              class="sheet-item"
            >
              <div class="sheet-item-main">
                <span class="sheet-item-name">{{ getOrderItemName(item) }}</span>
                <span class="sheet-item-meta" v-if="item.options">{{ item.options }}</span>
              </div>
              <div class="sheet-item-side">
                <span class="sheet-item-qty">x{{ item.quantity }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="sheet-footer">
          <button class="sheet-cancel-btn" @click="closeConfirmPricePopup">取消</button>
          <button class="sheet-confirm-btn" :disabled="confirmPriceSubmitting" @click="submitConfirmPrice">
            {{ confirmPriceSubmitting ? '提交中...' : '确认金额' }}
          </button>
        </div>
      </div>
    </van-popup>
    <input
      ref="confirmPriceEvidenceInputRef"
      class="sheet-evidence-input"
      type="file"
      accept="image/*"
      @change="handleConfirmPriceEvidenceFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import StaleOrderDialog from '@/components/StaleOrderDialog.vue';
import request from '@/utils/request';
import { showSuccessToast, showToast, showConfirmDialog } from 'vant';
import { buildUploadedPreviewFile, uploadPriceEvidenceImage } from '@/utils/priceEvidence';
import {
  enableNotificationSound,
  isNotificationSoundEnabled,
  playNotificationSound,
  primeNotificationAudio,
} from '@/utils/notificationSound';

const auth = useAuthStore();
const router = useRouter();
const activeTab = ref('making');
const currentTime = ref('');
const stats = ref({
  makingOrders: 0,
  pendingOrders: 0,
  completedOrders: 0,
  totalRevenue: 0
});
const shopConfig = ref({
  shopMode: 'fixed_dish'
});

const orders = ref<any[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const HISTORY_PAGE_SIZE = 20;
const historyPage = ref(1);
const loadedHistoryPage = ref(1);
const historyTotal = ref(0);
const historyTotalPages = ref(0);
const showHistoryPagination = computed(() => activeTab.value === 'completed' && historyTotalPages.value > 1);
const filterDishName = ref('');
const enablingSound = ref(false);
const soundEnabled = ref(isNotificationSoundEnabled());
const makingOrderIds = ref<string[]>([]);
const makingOrdersInitialized = ref(false);
const showConfirmPricePopup = ref(false);
const confirmPriceOrder = ref<any>(null);
const confirmPriceSubmitting = ref(false);
const confirmPriceEvidenceFileList = ref<any[]>([]);
const confirmPriceEvidenceUploading = ref(false);
const confirmPriceEvidenceInputRef = ref<HTMLInputElement | null>(null);
const confirmPriceForm = ref({
  finalWeightG: '',
  priceEvidenceImage: ''
});
const isWeightSelectionShop = computed(() => shopConfig.value.shopMode === 'weight_selection');
let dashboardIsActive = false;
let ordersRequestSequence = 0;

const pushDashboardHistoryState = (mode: 'page' | 'confirm-price' = 'page') => {
  window.history.pushState({
    __quickpickDashboardGuard: true,
    mode
  }, '', window.location.href);
};

const handleDashboardPopState = () => {
  if (showConfirmPricePopup.value) {
    closeConfirmPricePopup();
  }
};

// Calculate dish summary for making orders
const dishSummary = computed(() => {
  // Allow both making and pending tabs to show summary
  if (activeTab.value !== 'making' && activeTab.value !== 'pending') return [];
  
  const summaryMap = new Map<string, number>();
  orders.value
    .filter(order => order.status === activeTab.value)
    .forEach(order => {
      (order.items || []).forEach((item: any) => {
        const quantity = Number(item.quantity);
        if (!item.dishName || !Number.isFinite(quantity) || quantity <= 0) return;
        const count = summaryMap.get(item.dishName) || 0;
        summaryMap.set(item.dishName, count + quantity);
      });
    });

  return Array.from(summaryMap.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count); // Default sort by count desc
});

// Filter orders based on active tab and selected dish filter
const activeList = computed(() => {
  const currentStatusOrders = activeTab.value === 'completed'
    ? orders.value.filter(order => order.status === 'completed' || order.status === 'cancelled')
    : orders.value.filter(order => order.status === activeTab.value);

  // Apply filter for both making and pending
  if (filterDishName.value && (activeTab.value === 'making' || activeTab.value === 'pending')) {
    return currentStatusOrders.filter(order =>
      (order.items || []).some((item: any) => item.dishName === filterDishName.value)
    );
  }
  return currentStatusOrders;
});

// Watch activeTab to refetch orders and clear filter
watch(activeTab, () => {
  if (activeTab.value === 'completed') {
    historyPage.value = 1;
    loadedHistoryPage.value = 1;
  }
  orders.value = []; 
  filterDishName.value = '';
  loading.value = true;
  refreshDashboardData();
});

const toggleDishFilter = (name: string) => {
  if (filterDishName.value === name) {
    filterDishName.value = ''; // Clear filter if clicked again
  } else {
    filterDishName.value = name;
  }
};

// Update time every second
const updateTime = () => {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

const fetchStats = async () => {
  try {
    const res: any = await request.get('/api/merchant/stats/today');
    stats.value = res;
  } catch (e) {
    console.error(e);
  }
};

const fetchCurrentShop = async () => {
  try {
    const res: any = await request.get('/api/merchant/shop');
    shopConfig.value = {
      shopMode: res.shopMode || 'fixed_dish'
    };
  } catch (e) {
    console.error(e);
  }
};

const clearNewArrivalFlag = () => {
  orders.value.forEach((order: any) => {
    order.isNew = false;
  });
};

const updateMakingOrderNotifications = (makingOrders: any[]) => {
  const oldIds = new Set(makingOrderIds.value);
  const newIds = new Set<string>();

  if (makingOrdersInitialized.value) {
    makingOrders.forEach((order: any) => {
      if (!oldIds.has(order.id)) {
        newIds.add(order.id);
      }
    });
  }

  makingOrderIds.value = makingOrders.map((order: any) => order.id);
  makingOrdersInitialized.value = true;

  return newIds;
};

const triggerNewOrderNotification = (newIds: Set<string>) => {
  if (newIds.size === 0) return;

  playNotification();

  if (activeTab.value === 'making') {
    setTimeout(() => {
      clearNewArrivalFlag();
    }, 3000);
  }
};

const fetchMakingOrdersForNotification = async () => {
  const makingOrders: any = await request.get('/api/merchant/orders', {
    params: { shopId: auth.shopId, status: 'making' }
  });

  const newIds = updateMakingOrderNotifications(makingOrders);
  triggerNewOrderNotification(newIds);
};

const fetchOrders = async () => {
  const requestId = ++ordersRequestSequence;
  const status = activeTab.value;
  const requestedHistoryPage = historyPage.value;
  const canApplyResponse = () => requestId === ordersRequestSequence
    && dashboardIsActive
    && activeTab.value === status
    && (status !== 'completed' || historyPage.value === requestedHistoryPage);

  try {
    if (status === 'completed') {
      const res: any = await request.get('/api/merchant/orders/history', {
        params: { page: requestedHistoryPage, pageSize: HISTORY_PAGE_SIZE }
      });
      if (!canApplyResponse()) return true;
      orders.value = res.records || [];
      historyTotal.value = Number(res.total || 0);
      historyTotalPages.value = Number(res.totalPages || 0);
      loadedHistoryPage.value = Number(res.page || requestedHistoryPage);
      await fetchMakingOrdersForNotification();
      return true;
    }

    // Get orders based on active tab
    const res: any = await request.get('/api/merchant/orders', {
      params: { shopId: auth.shopId, status }
    });
    
    // Process items for each order
    // Since backend now returns items and remark in /merchant/orders, we don't need to fetch detail individually
    // But we need to handle if items are missing (just in case)
    
    // Actually, backend now returns List<Map> with items and remark.
    // So `res` is already the list we need.
    // We just need to check for new orders if status is making.
    
    const ordersWithItems = res; // No need for Promise.all map anymore
    if (!canApplyResponse()) return true;

    // Only for making orders, check for new arrivals
    if (status === 'making') {
      const newIds = updateMakingOrderNotifications(ordersWithItems);
      const newOrders = ordersWithItems.map((o: any) => {
        return { ...o, isNew: newIds.has(o.id) }; 
      });

      orders.value = newOrders;
      triggerNewOrderNotification(newIds);
    } else {
      // For other orders, just show them
      orders.value = ordersWithItems;
      await fetchMakingOrdersForNotification();
    }

    return true;

  } catch (e) {
    if (!canApplyResponse()) return true;
    console.error(e);
    return false;
  } finally {
    if (requestId === ordersRequestSequence) {
      loading.value = false;
      refreshing.value = false;
    }
  }
};

const refreshDashboardData = async () => {
  await Promise.all([fetchStats(), fetchOrders()]);
};

const pollDashboardData = async () => {
  if (activeTab.value === 'completed') {
    await Promise.all([fetchStats(), fetchMakingOrdersForNotification()]);
    return;
  }
  await refreshDashboardData();
};

const onHistoryPageChange = async () => {
  if (historyPage.value === loadedHistoryPage.value) return;
  loading.value = true;
  const loaded = await fetchOrders();
  if (!loaded) {
    historyPage.value = loadedHistoryPage.value;
    showToast('历史订单加载失败，请重试');
    return;
  }
  document.querySelector('.order-list-container')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const changeHistoryPage = (page: number) => {
  if (loading.value || page < 1 || page > historyTotalPages.value || page === historyPage.value) return;
  historyPage.value = page;
  void onHistoryPageChange();
};

const openConfirmPricePopup = (order: any) => {
  confirmPriceOrder.value = order;
  confirmPriceForm.value = {
    finalWeightG: order?.estimatedWeightG || '',
    priceEvidenceImage: order?.priceEvidenceImage || ''
  };
  confirmPriceEvidenceFileList.value = order?.priceEvidenceImage
    ? [{ url: order.priceEvidenceImage, status: 'done', message: '上传成功' }]
    : [];
  showConfirmPricePopup.value = true;
  pushDashboardHistoryState('confirm-price');
};

const triggerConfirmPriceEvidencePicker = () => {
  if (confirmPriceEvidenceUploading.value) return;
  if (confirmPriceEvidenceInputRef.value) {
    confirmPriceEvidenceInputRef.value.value = '';
    confirmPriceEvidenceInputRef.value.click();
  }
};

const closeConfirmPricePopup = () => {
  if (confirmPriceSubmitting.value) return;
  showConfirmPricePopup.value = false;
  confirmPriceOrder.value = null;
  confirmPriceEvidenceFileList.value = [];
};

const forceCloseConfirmPricePopup = () => {
  showConfirmPricePopup.value = false;
  confirmPriceOrder.value = null;
  confirmPriceEvidenceFileList.value = [];
};

const calculateConfirmPriceEvidenceTolerance = (estimatedWeight: number) => {
  if (estimatedWeight <= 0) return 0;
  return Math.max(50, Math.ceil(estimatedWeight * 0.12));
};

const confirmPriceEvidenceRequired = computed(() => {
  if (!confirmPriceOrder.value) return false;
  const estimatedWeight = normalizeWeight(confirmPriceOrder.value.estimatedWeightG);
  const finalWeight = normalizeWeight(confirmPriceForm.value.finalWeightG);
  if (estimatedWeight <= 0 || finalWeight <= 0) return false;
  return finalWeight > estimatedWeight + calculateConfirmPriceEvidenceTolerance(estimatedWeight);
});

const confirmPriceEvidenceRequirementText = computed(() => {
  if (!confirmPriceOrder.value) return '';
  const estimatedWeight = normalizeWeight(confirmPriceOrder.value.estimatedWeightG);
  if (estimatedWeight <= 0) return '';
  const tolerance = calculateConfirmPriceEvidenceTolerance(estimatedWeight);
  return `预计重量 ${estimatedWeight}g，超过 ${tolerance}g 以上需上传凭证`;
});

const handleConfirmPriceEvidenceCaptured = async (file: File | null) => {
  if (!file) return;
  if (!file.type.startsWith('image/')) {
    showToast('只能上传图片文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    showToast('凭证图片大小不能超过5MB');
    return;
  }
  try {
    confirmPriceEvidenceUploading.value = true;
    const res: any = await uploadPriceEvidenceImage(file);
    confirmPriceForm.value.priceEvidenceImage = res.url;
    confirmPriceEvidenceFileList.value = [buildUploadedPreviewFile(res.url)];
    showSuccessToast('凭证图片上传成功');
  } catch (e) {
    confirmPriceForm.value.priceEvidenceImage = '';
    confirmPriceEvidenceFileList.value = [];
    showToast((e as any)?.message || '凭证图片上传失败');
  } finally {
    confirmPriceEvidenceUploading.value = false;
  }
};

const handleConfirmPriceEvidenceFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement | null;
  const file = target?.files?.[0] || null;
  await handleConfirmPriceEvidenceCaptured(file);
  if (target) {
    target.value = '';
  }
};

const submitConfirmPrice = async () => {
  if (!confirmPriceOrder.value || confirmPriceSubmitting.value) return;

  const finalWeightG = Number(confirmPriceForm.value.finalWeightG);

  if (!finalWeightG || finalWeightG <= 0) {
    showToast('请输入有效的最终重量');
    return;
  }

  const finalAmount = calculateFinalAmount(confirmPriceOrder.value, finalWeightG);
  if (!finalAmount || finalAmount <= 0) {
    showToast('暂时无法计算最终金额');
    return;
  }

  if (confirmPriceEvidenceRequired.value && !confirmPriceForm.value.priceEvidenceImage) {
    showToast('最终重量超出预计范围，请先上传称重凭证图片');
    return;
  }

  try {
    confirmPriceSubmitting.value = true;
    await request.put(`/api/merchant/orders/${confirmPriceOrder.value.id}/confirm-price`, {
      finalWeightG,
      finalAmount,
      priceEvidenceImage: confirmPriceForm.value.priceEvidenceImage || null
    });
    showSuccessToast('金额已确认');
    forceCloseConfirmPricePopup();
    await refreshDashboardData();
  } catch (e) {
    showToast((e as any)?.message || '确认失败');
  } finally {
    confirmPriceSubmitting.value = false;
  }
};

const getOrderItemName = (item: any) => item?.dishName || item?.name || item?.ingredientName || '未命名';
const getOrderIngredientCount = (order: any) => {
  if (!order?.items?.length) return 0;
  return order.items.reduce((total: number, item: any) => total + Number(item.quantity || 0), 0);
};
const normalizeAmount = (value: any) => Number(value || 0);
const normalizeWeight = (value: any) => Number(value || 0);
const formatEstimatedWeight = (value: any) => `${normalizeWeight(value)}g`;
const formatEstimatedAmount = (value: any) => `¥${normalizeAmount(value).toFixed(2)}`;
const formatBrothExtraPrice = (value: any) => {
  const amount = normalizeAmount(value);
  return `¥${amount % 1 === 0 ? amount.toFixed(0) : amount.toFixed(2)}`;
};
const calculateFinalAmount = (order: any, finalWeight: number) => {
  const estimatedWeight = normalizeWeight(order?.estimatedWeightG);
  const estimatedAmount = normalizeAmount(order?.estimatedAmount);
  const brothExtraPrice = normalizeAmount(order?.brothExtraPrice);
  if (estimatedWeight <= 0 || finalWeight <= 0) return 0;
  const ingredientAmount = Math.max(estimatedAmount - brothExtraPrice, 0);
  const pricePerGram = ingredientAmount / estimatedWeight;
  return Number((pricePerGram * finalWeight + brothExtraPrice).toFixed(2));
};
const confirmPriceFinalAmountText = computed(() => {
  if (!confirmPriceOrder.value) return '¥0.00';
  const finalWeight = normalizeWeight(confirmPriceForm.value.finalWeightG);
  if (finalWeight <= 0) return '¥0.00';
  return `¥${calculateFinalAmount(confirmPriceOrder.value, finalWeight).toFixed(2)}`;
});
const finalWeightDisplay = computed(() => {
  const finalWeight = normalizeWeight(confirmPriceForm.value.finalWeightG);
  return finalWeight > 0 ? `${finalWeight}g` : '--';
});

const confirmPrepareOrder = (orderId: string) => {
  showConfirmDialog({
    title: '确认制作完成',
    message: '确认该订单已制作完成并可通知用户取餐？',
    confirmButtonText: '制作完成',
    confirmButtonColor: '#1a8cff'
  })
    .then(() => {
      prepareOrder(orderId);
    })
    .catch(() => {
      // cancel
    });
};

const confirmCompleteOrder = (orderId: string) => {
  showConfirmDialog({
    title: '确认取餐',
    message: '确认用户已取餐？',
    confirmButtonText: '确认取餐',
    confirmButtonColor: '#00b894'
  })
    .then(() => {
      completeOrder(orderId);
    })
    .catch(() => {
      // cancel
    });
};

const prepareOrder = async (orderId: string) => {
  try {
    if (navigator.vibrate) navigator.vibrate(50);
    await request.put(`/api/merchant/orders/${orderId}/prepare`);
    showSuccessToast('已通知取餐');
    
    // Remove locally
    const index = orders.value.findIndex(o => o.id === orderId);
    if (index > -1) {
       orders.value.splice(index, 1);
       fetchStats();
    }
  } catch (e) {
    showToast('操作失败');
  }
};

const completeOrder = async (orderId: string) => {
  try {
    // Haptic feedback
    if (navigator.vibrate) navigator.vibrate(50);
    
    await request.put(`/api/merchant/orders/${orderId}/complete`);
    showSuccessToast('订单完成');
    
    // Remove locally to feel instant
    const index = orders.value.findIndex(o => o.id === orderId);
    if (index > -1) {
       orders.value.splice(index, 1);
       // Refresh stats
       fetchStats();
    }
  } catch (e) {
    showToast('操作失败');
  }
};

const onRefresh = async () => {
  refreshing.value = true;
  await refreshDashboardData();
};

const formatTime = (timeStr: string) => {
  if (!timeStr) return '';
  // 如果是 HH:mm 格式直接返回 (通常取餐时间是 HH:mm)
  if (timeStr.length === 5 && timeStr.indexOf(':') === 2) return timeStr;
  
  // 如果是 HH:mm:ss 格式，也只返回 HH:mm
  if (timeStr.length === 8 && timeStr.indexOf(':') === 2 && timeStr.lastIndexOf(':') === 5) {
      return timeStr.substring(0, 5);
  }
  
  // 制作中：如果是完整时间，只显示 HH:mm
  if (activeTab.value === 'making' || activeTab.value === 'pending') {
    try {
      const date = new Date(timeStr);
      return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0');
    } catch {
       if (timeStr.length > 16) return timeStr.substring(11, 16);
       return timeStr;
    }
  }

  // 已完成：需要显示日期 MM-DD HH:mm
  if (activeTab.value === 'completed') {
    try {
      const date = new Date(timeStr);
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hour = date.getHours().toString().padStart(2, '0');
      const minute = date.getMinutes().toString().padStart(2, '0');
      return `${month}-${day} ${hour}:${minute}`;
    } catch {
      return timeStr;
    }
  }

  // Fallback
  try {
    const date = new Date(timeStr);
    return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0');
  } catch {
    return timeStr.substring(11, 16);
  }
};

const formatRemark = (remark: string) => {
  if (!remark) return '';
  if (remark.length > 5) return remark.substring(0, 5) + '...';
  return remark;
};

const formatMoney = (val: number | string) => {
  if (!val) return '0.00';
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const isLikelyMobileDevice = () => {
  if (typeof window === 'undefined') return false;
  const coarsePointer = typeof window.matchMedia === 'function' && window.matchMedia('(pointer: coarse)').matches;
  return coarsePointer || /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent);
};

const showSoundEnableCard = computed(() => isLikelyMobileDevice() && !soundEnabled.value);

const speakNewOrder = () => {
  if (!('speechSynthesis' in window)) return;

  window.speechSynthesis.cancel();

  const utterance = new SpeechSynthesisUtterance('你有新的订单了');
  utterance.lang = 'zh-CN';
  utterance.rate = 1;
  utterance.pitch = 1;
  window.speechSynthesis.speak(utterance);
};

const playNotification = () => {
  void playNotificationSound().then((soundPlayed) => {
    if (!soundPlayed && navigator.vibrate) {
      navigator.vibrate([120, 80, 120]);
    }

    if (!isLikelyMobileDevice()) {
      speakNewOrder();
    }
  });
};

const syncNotificationAudioState = async () => {
  const enabled = await primeNotificationAudio();
  soundEnabled.value = enabled;
  return enabled;
};

const enableSoundAlert = async () => {
  if (enablingSound.value) return;

  enablingSound.value = true;
  try {
    const enabled = await enableNotificationSound();
    soundEnabled.value = enabled;
    if (enabled) {
      showSuccessToast('提醒音已开启');
    } else {
      showToast('当前浏览器仍然拦截音频，请再点击一次');
    }
  } finally {
    enablingSound.value = false;
  }
};

let timer: any = null;
let timeInterval: any = null;
let detachAudioUnlockListeners: (() => void) | null = null;
let detachVisibilityListeners: (() => void) | null = null;

const installAudioUnlockListeners = () => {
  const handler = () => {
    void syncNotificationAudioState();
  };

  const options: AddEventListenerOptions = { passive: true, once: true };
  window.addEventListener('pointerdown', handler, options);
  window.addEventListener('touchstart', handler, options);
  window.addEventListener('keydown', handler, options);

  detachAudioUnlockListeners = () => {
    window.removeEventListener('pointerdown', handler);
    window.removeEventListener('touchstart', handler);
    window.removeEventListener('keydown', handler);
  };
};

const installAudioResumeListeners = () => {
  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      void syncNotificationAudioState();
    }
  };

  const handleWindowFocus = () => {
    void syncNotificationAudioState();
  };

  const handlePageShow = () => {
    void syncNotificationAudioState();
  };

  document.addEventListener('visibilitychange', handleVisibilityChange);
  window.addEventListener('focus', handleWindowFocus);
  window.addEventListener('pageshow', handlePageShow);

  detachVisibilityListeners = () => {
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    window.removeEventListener('focus', handleWindowFocus);
    window.removeEventListener('pageshow', handlePageShow);
  };
};

const stopDashboardTimers = () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
  if (timeInterval) {
    clearInterval(timeInterval);
    timeInterval = null;
  }
};

const startDashboardTimers = () => {
  stopDashboardTimers();
  dashboardIsActive = true;
  updateTime();
  void refreshDashboardData();
  timer = setInterval(pollDashboardData, 10000);
  timeInterval = setInterval(updateTime, 1000);
};

onMounted(() => {
  soundEnabled.value = isNotificationSoundEnabled();
  void syncNotificationAudioState();
  installAudioUnlockListeners();
  installAudioResumeListeners();
  pushDashboardHistoryState('page');
  window.addEventListener('popstate', handleDashboardPopState);
  void fetchCurrentShop();
});

onActivated(() => {
  startDashboardTimers();
});

onDeactivated(() => {
  dashboardIsActive = false;
  ordersRequestSequence += 1;
  loading.value = false;
  refreshing.value = false;
  stopDashboardTimers();
  // 缓存期间仅保留新订单提醒，不再刷新或写入当前看板列表。
  timer = setInterval(() => void fetchMakingOrdersForNotification(), 10000);
});

onUnmounted(() => {
  dashboardIsActive = false;
  ordersRequestSequence += 1;
  stopDashboardTimers();
  if (detachAudioUnlockListeners) detachAudioUnlockListeners();
  if (detachVisibilityListeners) detachVisibilityListeners();
  window.removeEventListener('popstate', handleDashboardPopState);
});
</script>

<style scoped>
/* Design Variables */
.dashboard-container {
  --primary-color: #1a8cff;
  --success-color: #00b894;
  --warning-color: #ff9f43;
  --bg-color: #f8fafc;
  --card-bg: #ffffff;
  --text-main: #2c3e50;
  --text-light: #95a5a6;
  
  /* In the new layout, we don't need min-height 100vh because the container scrolls */
  min-height: 100%; 
  background-color: var(--bg-color);
  padding: 16px;
  box-sizing: border-box; 
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* 1. Header & Stats */
.header-section {
  margin-bottom: 24px;
}

.sound-enable-card {
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  border: 1px solid rgba(249, 115, 22, 0.18);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sound-enable-copy {
  min-width: 0;
}

.sound-enable-title {
  font-size: 15px;
  font-weight: 700;
  color: #9a3412;
}

.sound-enable-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #c2410c;
  line-height: 1.4;
}

.sound-enable-btn {
  flex-shrink: 0;
  border: none;
  border-radius: 999px;
  background: #f97316;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 10px 16px;
}

.sound-enable-btn:active {
  transform: scale(0.98);
  opacity: 0.92;
}

.shop-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.shop-name {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.current-time {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-light);
}

.stats-board {
  background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 100%);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(26, 140, 255, 0.08);
  border: 1px solid rgba(26, 140, 255, 0.1);
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-value .unit {
  font-size: 12px;
  font-weight: normal;
  margin-left: 2px;
  color: var(--text-light);
}

.stat-value.highlight { color: #ff7b2c; } /* making - orange */
.stat-value.warning { color: #1a8cff; } /* pending - blue (to match user side logic if needed, or keep consistent) */
/* Wait, user side making is blue, pending is orange. Let's align. */
/* Making -> Blue (#1a8cff), Pending -> Orange (#ff7b2c) */
.stat-value.highlight { color: #1a8cff; } /* making */
.stat-value.warning { color: #ff7b2c; } /* pending */
.stat-value.success { color: var(--success-color); }
.stat-value.primary { color: var(--text-main); } /* revenue - dark */

.stat-item.clickable {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-item.clickable:active {
  transform: scale(0.95);
  background-color: rgba(0,0,0,0.02);
  border-radius: 8px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-light);
  font-weight: 500;
}

.divider {
  width: 1px;
  height: 24px;
  background-color: #eee;
}

/* 4. Dish Summary Card */
.dish-summary-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.summary-header .title {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.summary-header .count {
  font-size: 13px;
  color: #999;
}

.summary-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.summary-item {
  display: flex;
  align-items: center;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 120px;
  justify-content: space-between;
}

.summary-item:active, .summary-item.active {
  background-color: #e6f7ff;
  border-color: #1a8cff;
}

.summary-item .dish-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-right: 8px;
}

.summary-item .dish-count {
  background-color: #ff7b2c;
  color: white;
  font-size: 14px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 12px;
  min-width: 20px;
  text-align: center;
}

.summary-item.active .dish-count {
  background-color: #1a8cff;
}

/* 2. Tabs */
.tab-control {
  display: flex;
  background-color: #e2e8f0;
  border-radius: 8px;
  padding: 4px;
  margin-bottom: 20px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  border-radius: 6px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.tab-item.active {
  background-color: #fff;
  color: var(--primary-color);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  font-weight: 600;
}

/* 3. Order List */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-container--with-history-pagination {
  padding-bottom: 92px;
}

.history-pagination {
  position: fixed;
  left: 50%;
  bottom: calc(58px + env(safe-area-inset-bottom));
  z-index: 900;
  width: calc(100% - 32px);
  max-width: 520px;
  min-height: 58px;
  padding: 8px;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.14);
  transform: translateX(-50%);
  backdrop-filter: blur(10px);
}

.history-pagination__button {
  width: 44px;
  height: 42px;
  padding: 0;
  border: none;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #1a8cff;
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  transition: background-color 0.2s ease, opacity 0.2s ease;
}

.history-pagination__button:active:not(:disabled) {
  background: #0876dc;
}

.history-pagination__button:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.history-pagination__status {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  line-height: 1.25;
}

.history-pagination__page {
  color: #1e293b;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.history-pagination__total {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.order-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 0; /* Layout handled by children */
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  display: flex;
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
}

/* New Order Animation */
.new-arrival::before {
  content: '';
  position: absolute;
  top: 0; left: 0; bottom: 0; width: 4px;
  background-color: var(--warning-color);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 0.6; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

.new-arrival {
  animation: slideIn 0.4s ease-out;
  border: 1px solid var(--primary-color);
}

@keyframes slideIn {
  from { transform: translateY(-20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* Card Left */
.card-left {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.pickup-code {
  font-size: 28px;
  font-weight: 800;
  color: var(--primary-color);
  letter-spacing: -0.5px;
}

.order-time {
  font-size: 14px;
  color: #94a3b8;
}

.order-time.highlight {
  color: #ff7b2c;
  font-weight: 600;
}

.order-time.cancelled-time {
  color: #ef4444;
  font-weight: 600;
}

.dish-summary {
  margin-bottom: 12px;
}

.dish-item {
  font-size: 15px;
  color: var(--text-main);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-item__text {
  min-width: 0;
}

.dish-item__options {
  color: #1a8cff;
}

.bullet {
  color: #cbd5e1;
  margin-right: 4px;
}

.more-items {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.tags-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap; /* Allow wrapping for multiple tags */
  margin-top: auto; /* Push to bottom */
}

.tag {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.pack-tag {
  background-color: #e6f7ff;
  color: #1a8cff;
  border: 1px solid rgba(26, 140, 255, 0.2);
}

.dine-tag {
  background-color: #f0fdf4;
  color: #16a34a;
  border: 1px solid rgba(22, 163, 74, 0.2);
}

.remark-tag {
  background-color: #fff7ed;
  color: #c2410c;
  border: 1px solid rgba(194, 65, 12, 0.2);
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cancel-tag {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid rgba(220, 38, 38, 0.18);
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.price-tag {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-main);
  margin-left: auto; /* Push price to right */
}

/* Card Right */
.card-right {
  width: 88px;
  border-left: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}

.action-btn {
  width: 100%;
  height: 100%;
  border: none;
  background: var(--primary-color);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;
}

.action-btn:active {
  background: #0077e6;
  transform: scale(0.98);
}

.status-text.completed {
  color: var(--success-color);
  font-weight: 600;
}

.status-text.cancelled {
  color: #ef4444;
  font-weight: 600;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.confirm-price-sheet {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #fff;
  overflow: hidden;
}

.confirm-price-popup {
  overflow: hidden;
}

.sheet-header {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px 0;
}

.sheet-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
}

.sheet-body {
  flex: 1;
  min-height: 0;
  height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 18px 100px;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  touch-action: pan-y;
}

.sheet-items {
  margin-top: 16px;
  background: #f8fafc;
  border-radius: 14px;
  padding: 6px 14px;
}

.sheet-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #e2e8f0;
}

.sheet-item:last-child {
  border-bottom: none;
}

.sheet-item--broth {
  background: #fff7ed;
  margin: -6px -14px 0;
  padding: 12px 14px;
  border-radius: 14px 14px 0 0;
}

.sheet-item-main {
  flex: 1;
  min-width: 0;
}

.sheet-item-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.sheet-item-meta {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.sheet-item-side {
  flex-shrink: 0;
}

.sheet-item-qty {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

.sheet-item-price {
  font-size: 14px;
  font-weight: 700;
  color: #c2410c;
}

.sheet-form-card {
  margin-top: 4px;
  padding: 8px 0 16px;
  border-radius: 14px;
  background: #fff7ed;
}

.sheet-form-card--top {
  margin-top: 0;
}

.sheet-weight-field {
  width: calc(100% - 32px);
  margin: 0 16px;
  border: 1px solid #fed7aa;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-sizing: border-box;
}

.sheet-weight-field :deep(.van-cell) {
  padding: 12px 14px;
  background: transparent;
}

.sheet-weight-field :deep(.van-field__label) {
  color: #475569;
  font-weight: 600;
}

.sheet-weight-field :deep(.van-field__control) {
  font-size: 16px;
}

.field-suffix {
  font-size: 14px;
  color: #94a3b8;
}

.sheet-compare-card {
  margin: 10px 16px 0;
  padding: 14px 16px;
  border-radius: 12px;
  background: #fff;
}

.sheet-evidence-card {
  margin: 12px 16px 0;
  padding: 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #fdba74;
}

.sheet-evidence-title {
  font-size: 14px;
  font-weight: 700;
  color: #9a3412;
}

.sheet-evidence-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #7c2d12;
}

.sheet-evidence-tip {
  margin: 8px 0 12px;
  font-size: 12px;
  color: #c2410c;
}

.sheet-evidence-preview {
  margin-bottom: 12px;
}

.sheet-evidence-preview__image {
  display: block;
  width: 100%;
  max-height: 220px;
  border-radius: 12px;
  object-fit: cover;
  background: #f8fafc;
}

.sheet-evidence-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sheet-evidence-btn {
  height: 42px;
  border: none;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
}

.sheet-evidence-btn--primary {
  background: #f97316;
  color: #fff;
}

.sheet-evidence-btn--primary:disabled {
  opacity: 0.7;
}

.sheet-evidence-status {
  font-size: 12px;
  line-height: 1.5;
  color: #9a3412;
}

.sheet-evidence-input {
  display: none;
}

.sheet-compare-header,
.sheet-compare-row {
  display: grid;
  grid-template-columns: 64px 1fr 1fr;
  align-items: center;
  column-gap: 12px;
}

.sheet-compare-header {
  margin-bottom: 10px;
}

.sheet-compare-header__empty {
  display: block;
}

.sheet-compare-header__value {
  font-size: 12px;
  color: #94a3b8;
  text-align: right;
}

.sheet-compare-row:not(:last-child) {
  margin-bottom: 12px;
}

.sheet-compare-row__label {
  font-size: 14px;
  color: #64748b;
}

.sheet-compare-row__value {
  text-align: right;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.sheet-compare-row__value--highlight {
  color: #2a8bff;
}

.sheet-compare-row__value--amount {
  color: #ff8a3d;
}

.sheet-footer {
  display: flex;
  gap: 12px;
  padding: 14px 18px calc(18px + env(safe-area-inset-bottom));
  border-top: 1px solid #eef2f7;
  background: #fff;
  position: sticky;
  bottom: 0;
  z-index: 2;
}

.sheet-cancel-btn,
.sheet-confirm-btn {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
}

.sheet-cancel-btn {
  background: #f1f5f9;
  color: #475569;
}

.sheet-confirm-btn {
  background: #ff8a3d;
  color: #fff;
  box-shadow: 0 8px 16px rgba(255, 138, 61, 0.2);
}

.sheet-confirm-btn:disabled {
  opacity: 0.7;
}
</style>
