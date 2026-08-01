<template>
  <div class="order-detail-container">
    <van-nav-bar
      title="订单详情"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
    />

    <div v-if="loading" class="loading-state">
       <van-loading type="spinner" vertical>加载中...</van-loading>
    </div>

    <div v-else-if="!order" class="empty-state">
       <van-empty description="订单不存在" />
    </div>

    <div v-else class="content">
      <div class="page-stack">
        <div class="status-card" :class="getStatusClass(order.status)">
          <div class="status-row">
            <div class="status-main">
              <span class="status-text">{{ getStatusText(order.status) }}</span>
              <div class="status-desc">
                 <span v-if="order.status === 'making'">预计 {{ formatTime(order.pickupTime) || '尽快' }} 取餐</span>
                 <span v-else-if="order.status === 'pending'">请提示用户及时取餐</span>
                 <span v-else>订单已结束</span>
              </div>
            </div>
            <span class="pickup-code">#{{ order.pickupCode }}</span>
          </div>
          <div class="status-tags">
            <span class="status-pill">{{ order.needPack === 1 ? '打包' : '堂食' }}</span>
            <span v-if="order.brothName" class="status-pill">{{ order.brothName }}</span>
          </div>
        </div>

        <div
          v-if="weightInfoRows.length > 0"
          class="section-card detail-card weight-card"
        >
          <div class="section-header section-header--compact">
            <div class="section-badge section-badge--warm">称重信息</div>
          </div>
          <div class="weight-list">
            <div
              v-for="item in weightInfoRows"
              :key="item.label"
              class="weight-row"
            >
              <span class="weight-label">{{ item.label }}</span>
              <span class="weight-value" :class="item.tone ? `weight-value--${item.tone}` : ''">{{ item.value }}</span>
            </div>
          </div>
        </div>

        <div class="section-card detail-card">
          <div class="section-header section-header--compact">
            <div class="section-badge">订单信息</div>
          </div>
          <div class="info-list">
            <div
              v-for="item in orderInfoRows"
              :key="item.label"
              class="info-row"
              :class="{ 'info-row--span-2': item.wide }"
            >
              <span class="info-label">{{ item.label }}</span>
              <span class="info-value">{{ item.value }}</span>
            </div>
            <div class="info-row info-row--span-2">
              <span class="info-label">联系电话</span>
              <div class="phone-cell">
                <span class="phone-number">{{ order.userPhone || '未提供' }}</span>
                <van-button
                  v-if="order.userPhone"
                  type="primary"
                  size="mini"
                  round
                  @click="copyPhone"
                  class="copy-btn"
                >
                  复制
                </van-button>
              </div>
            </div>
            <div class="info-row info-row--remark info-row--span-2">
              <span class="info-label">备注信息</span>
              <span class="remark-card" :class="{ 'remark-card--empty': !order.remark }">
                {{ order.remark || '无' }}
              </span>
            </div>
          </div>
        </div>

        <div
          v-if="lifecycleRows.length > 0"
          class="section-card detail-card record-card"
        >
          <div class="section-header section-header--compact">
            <div class="section-badge">履约记录</div>
          </div>
          <div class="record-list">
            <div
              v-for="item in lifecycleRows"
              :key="item.label"
              class="record-item"
              :class="{ 'record-item--alert': item.alert }"
            >
              <div class="record-label">{{ item.label }}</div>
              <div class="record-value">{{ item.value }}</div>
            </div>
          </div>
        </div>

        <div class="section-card items-card">
           <div class="section-header section-header--items">
             <div class="items-summary">商品明细 {{ order.items?.length || 0 }}项</div>
           </div>
           <div class="items-list">
             <div v-for="(item, index) in order.items" :key="index" class="item-row">
                <img :src="getImageUrl(item.image)" class="item-img" />
                <div class="item-main">
                   <div class="item-name">{{ item.dishName }}</div>
                   <div class="item-opts" v-if="item.options">{{ item.options }}</div>
                </div>
                <div class="item-side">
                  <div class="item-qty">x{{ item.quantity }}</div>
                  <div class="item-price">{{ getOrderItemSideValue(item) }}</div>
                </div>
             </div>
           </div>
           <div class="divider"></div>
           <div class="total-row">
              <div>
                <div class="total-label">订单合计</div>
                <div class="total-subtitle">请核对商品与金额信息</div>
              </div>
              <span class="total-price">¥{{ order.totalAmount }}</span>
           </div>
        </div>
      </div>

      <!-- 底部操作栏占位 -->
      <div class="action-bar-placeholder"></div>
      
      <!-- 底部操作栏 -->
      <div class="action-bar" v-if="['making', 'pending'].includes(order.status)">
        <div class="action-bar__actions action-bar__actions--triple" v-if="order.status === 'pending'">
          <van-button
            type="warning"
            plain
            block
            round
            @click="cancelOrder"
            class="cancel-button"
          >
            取消订单
          </van-button>
          <van-button
            type="danger"
            block
            round
            @click="confirmNoShow"
            class="no-show-button"
          >
            确认逃单
          </van-button>
          <van-button
            type="success"
            block
            round
            @click="confirmComplete"
          >
            确认取餐
          </van-button>
        </div>
        <div class="action-bar__actions" v-else-if="order.status === 'making' && order.pricingStatus === 'pending_confirm'">
          <van-button
            type="warning"
            plain
            block
            round
            @click="cancelOrder"
            class="cancel-button"
          >
            取消订单
          </van-button>
          <van-button
            type="warning"
            block
            round
            @click="openConfirmPricePopup"
          >
            确认金额
          </van-button>
        </div>
        <div class="action-bar__actions" v-else-if="order.status === 'making'">
          <van-button
            type="warning"
            plain
            block
            round
            @click="cancelOrder"
            class="cancel-button"
          >
            取消订单
          </van-button>
          <van-button
            type="primary"
            block
            round
            @click="confirmPrepare"
          >
            制作完成
          </van-button>
        </div>
      </div>
    </div>

    <!-- 取消订单弹窗 -->
    <van-popup
      v-model:show="showCancelDialog"
      position="center"
      round
      :style="{ width: '85%', maxWidth: '400px' }"
    >
      <div class="cancel-dialog">
        <div class="dialog-header">
          <div class="dialog-title">取消订单</div>
          <div class="dialog-subtitle">请选择或填写取消原因，便于后续记录订单情况</div>
        </div>
        
        <div class="dialog-body">
          <van-field
            v-model="cancelReasonInput"
            type="textarea"
            rows="3"
            placeholder="请输入取消原因..."
            maxlength="200"
            show-word-limit
            autofocus
            class="reason-input"
          />
        </div>
        
        <!-- 常用取消原因 -->
        <div class="common-reasons-section">
          <div class="common-reasons-title">常用语：</div>
          <div class="common-reasons-list">
            <van-tag
              v-for="(reason, index) in commonReasons"
              :key="index"
              class="reason-tag"
              type="primary"
              size="medium"
              round
              @click="selectCommonReason(reason)"
            >
              {{ reason }}
            </van-tag>
          </div>
        </div>
        
        <div class="dialog-footer">
          <van-button
            type="default"
            size="large"
            @click="closeCancelDialog"
            class="cancel-btn"
          >
            取消
          </van-button>
          <van-button
            type="warning"
            size="large"
            @click="confirmCancelOrder"
            class="confirm-btn"
            :disabled="!cancelReasonInput.trim()"
          >
            确认取消
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-popup
      v-model:show="showConfirmPricePopup"
      class="confirm-price-popup"
      position="bottom"
      round
      :lock-scroll="false"
      :close-on-popstate="false"
      :style="{ height: '85vh', maxHeight: '85vh' }"
    >
      <div class="confirm-price-sheet">
        <div class="sheet-header">
          <div class="sheet-close" @click="closeConfirmPricePopup">
            <van-icon name="cross" />
          </div>
        </div>

        <div v-if="order" class="sheet-body">
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
                <span class="sheet-compare-row__value">{{ formatEstimatedWeight(order.estimatedWeightG) }}</span>
                <span class="sheet-compare-row__value sheet-compare-row__value--highlight">{{ finalWeightDisplay }}</span>
              </div>
              <div class="sheet-compare-row">
                <span class="sheet-compare-row__label">金额</span>
                <span class="sheet-compare-row__value">{{ formatEstimatedAmount(order.estimatedAmount) }}</span>
                <span class="sheet-compare-row__value sheet-compare-row__value--amount">{{ confirmPriceFinalAmountText }}</span>
              </div>
            </div>

            <div v-if="evidenceRequired" class="sheet-evidence-card">
              <div class="sheet-evidence-title">请上传称重凭证</div>
              <div class="sheet-evidence-desc">
                当前最终重量超出预计容差范围。为避免学生质疑虚假增重，请上传电子秤称重照片留证。
              </div>
              <div class="sheet-evidence-tip">{{ evidenceRequirementText }}</div>
              <div v-if="confirmPriceForm.priceEvidenceImage" class="sheet-evidence-preview">
                <img :src="confirmPriceForm.priceEvidenceImage" alt="称重凭证" class="sheet-evidence-preview__image" />
              </div>
              <div class="sheet-evidence-actions">
                <button
                  class="sheet-evidence-btn sheet-evidence-btn--primary"
                  :disabled="priceEvidenceUploading"
                  @click="triggerPriceEvidencePicker"
                >
                  {{ priceEvidenceUploading ? '上传中...' : confirmPriceForm.priceEvidenceImage ? '重新上传' : '拍照上传' }}
                </button>
                <div class="sheet-evidence-status">
                  {{ confirmPriceForm.priceEvidenceImage ? '已上传称重凭证' : '点击后将直接弹出系统相机/相册选择' }}
                </div>
              </div>
            </div>
          </div>

          <div class="sheet-items">
            <div
              v-if="order.brothName"
              class="sheet-item sheet-item--broth"
            >
              <div class="sheet-item-main">
                <span class="sheet-item-name">{{ order.brothName }}</span>
              </div>
              <div class="sheet-item-side">
                <span class="sheet-item-price">{{ formatBrothExtraPrice(order.brothExtraPrice) }}</span>
              </div>
            </div>
            <div
              v-for="(item, index) in order.items || []"
              :key="`${order.id}-${index}`"
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
      ref="priceEvidenceInputRef"
      class="sheet-evidence-input"
      type="file"
      accept="image/*"
      @change="handlePriceEvidenceFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onBeforeUnmount, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { showToast, showConfirmDialog, showSuccessToast } from 'vant';
import { buildUploadedPreviewFile, uploadPriceEvidenceImage } from '@/utils/priceEvidence';

const route = useRoute();
const router = useRouter();
const orderId = route.params.id as string;

const loading = ref(true);
const order = ref<any>(null);
const showCancelDialog = ref(false);
const cancelReasonInput = ref('');
const showConfirmPricePopup = ref(false);
const confirmPriceSubmitting = ref(false);
const priceEvidenceFileList = ref<any[]>([]);
const priceEvidenceUploading = ref(false);
const priceEvidenceInputRef = ref<HTMLInputElement | null>(null);
const confirmPriceForm = ref({
  finalWeightG: '',
  priceEvidenceImage: ''
});

// 常用取消原因列表
const commonReasons = ref([
  '菜品售罄',
  '超出营业时间',
  '用户要求取消'
])

const pushExitGuardState = () => {
  window.history.pushState({
    __quickpickOrderExitGuard: true,
    orderId
  }, '', window.location.href);
};

const goBackToDashboard = () => {
  router.replace('/dashboard');
};

const handleBrowserBack = () => {
  goBackToDashboard();
};

const onClickLeft = () => {
  goBackToDashboard();
};

const fetchOrder = async () => {
  try {
    loading.value = true;
    // 获取订单详情
    const res: any = await request.get(`/api/merchant/orders/${orderId}`);
    // 由于后端接口直接返回订单详情对象，这里直接赋值
    // 注意：如果是之前的列表接口，items可能在order对象内。如果是详情接口，可能也是如此。
    // 假设 /api/merchant/orders/{id} 返回的是单个 Order 对象（包含items）
    order.value = res;
  } catch (e) {
    showToast('获取订单详情失败');
  } finally {
    loading.value = false;
  }
};

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    making: '制作中',
    pending: '待取餐',
    completed: '已完成',
    cancelled: '已取消'
  };
  return map[status] || status;
};

const getStatusClass = (status: string) => {
  if (status === 'making') return 'bg-blue';
  if (status === 'pending') return 'bg-orange';
  if (status === 'completed') return 'bg-green';
  return 'bg-gray';
};

const formatTime = (timeStr: string) => {
  if (!timeStr) return '';
  // 如果是 HH:mm 格式直接返回
  if (timeStr.length === 5 && timeStr.indexOf(':') === 2) return timeStr;
  try {
    const date = new Date(timeStr);
    return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0');
  } catch {
    return timeStr;
  }
};

const formatDateTime = (timeStr: string) => {
  if (!timeStr) return '';
  try {
    return new Date(timeStr).toLocaleString('zh-CN', { hour12: false });
  } catch {
    return timeStr;
  }
};

const openConfirmPricePopup = () => {
  confirmPriceForm.value = {
    finalWeightG: order.value?.estimatedWeightG || '',
    priceEvidenceImage: order.value?.priceEvidenceImage || ''
  };
  priceEvidenceFileList.value = order.value?.priceEvidenceImage
    ? [buildUploadedPreviewFile(order.value.priceEvidenceImage)]
    : [];
  showConfirmPricePopup.value = true;
};

const triggerPriceEvidencePicker = () => {
  if (priceEvidenceUploading.value) return;
  if (priceEvidenceInputRef.value) {
    priceEvidenceInputRef.value.value = '';
    priceEvidenceInputRef.value.click();
  }
};

const closeConfirmPricePopup = () => {
  if (confirmPriceSubmitting.value) return;
  showConfirmPricePopup.value = false;
  priceEvidenceFileList.value = [];
};

const forceCloseConfirmPricePopup = () => {
  showConfirmPricePopup.value = false;
  priceEvidenceFileList.value = [];
};

const calculateEvidenceTolerance = (estimatedWeight: number) => {
  if (estimatedWeight <= 0) return 0;
  return Math.max(50, Math.ceil(estimatedWeight * 0.12));
};

const evidenceRequired = computed(() => {
  if (!order.value) return false;
  const estimatedWeight = normalizeWeight(order.value.estimatedWeightG);
  const finalWeight = normalizeWeight(confirmPriceForm.value.finalWeightG);
  if (estimatedWeight <= 0 || finalWeight <= 0) return false;
  return finalWeight > estimatedWeight + calculateEvidenceTolerance(estimatedWeight);
});

const evidenceRequirementText = computed(() => {
  if (!order.value) return '';
  const estimatedWeight = normalizeWeight(order.value.estimatedWeightG);
  if (estimatedWeight <= 0) return '';
  const tolerance = calculateEvidenceTolerance(estimatedWeight);
  return `预计重量 ${estimatedWeight}g，超过 ${tolerance}g 以上需上传凭证`;
});

const handlePriceEvidenceCaptured = async (file: File | null) => {
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
    priceEvidenceUploading.value = true;
    const res: any = await uploadPriceEvidenceImage(file);
    confirmPriceForm.value.priceEvidenceImage = res.url;
    priceEvidenceFileList.value = [buildUploadedPreviewFile(res.url)];
    showSuccessToast('凭证图片上传成功');
  } catch (e) {
    confirmPriceForm.value.priceEvidenceImage = '';
    priceEvidenceFileList.value = [];
    showToast((e as any)?.message || '凭证图片上传失败');
  } finally {
    priceEvidenceUploading.value = false;
  }
};

const handlePriceEvidenceFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement | null;
  const file = target?.files?.[0] || null;
  await handlePriceEvidenceCaptured(file);
  if (target) {
    target.value = '';
  }
};

const submitConfirmPrice = async () => {
  const finalWeightG = Number(confirmPriceForm.value.finalWeightG);

  if (!finalWeightG || finalWeightG <= 0) {
    showToast('请输入有效的最终重量');
    return;
  }

  const finalAmount = calculateFinalAmount(order.value, finalWeightG);
  if (!finalAmount || finalAmount <= 0) {
    showToast('暂时无法计算最终金额');
    return;
  }

  if (evidenceRequired.value && !confirmPriceForm.value.priceEvidenceImage) {
    showToast('最终重量超出预计范围，请先上传称重凭证图片');
    return;
  }

  try {
    confirmPriceSubmitting.value = true;
    await request.put(`/api/merchant/orders/${orderId}/confirm-price`, {
      finalWeightG,
      finalAmount,
      priceEvidenceImage: confirmPriceForm.value.priceEvidenceImage || null
    });
    showSuccessToast('金额已确认');
    forceCloseConfirmPricePopup();
    fetchOrder();
  } catch (e) {
    showToast('操作失败');
  } finally {
    confirmPriceSubmitting.value = false;
  }
};

const confirmPrepare = () => {
  showConfirmDialog({
    title: '确认制作完成',
    message: '确认该订单已制作完成？'
  }).then(async () => {
    try {
      await request.put(`/api/merchant/orders/${orderId}/prepare`);
      showSuccessToast('操作成功');
      fetchOrder(); // 刷新状态
    } catch (e) {
      showToast('操作失败');
    }
  });
};

const confirmComplete = () => {
  showConfirmDialog({
    title: '确认取餐',
    message: '确认用户已取餐？'
  }).then(async () => {
    try {
      await request.put(`/api/merchant/orders/${orderId}/complete`);
      showSuccessToast('操作成功');
      fetchOrder(); // 刷新状态
    } catch (e) {
      showToast('操作失败');
    }
  });
};

const submitCancelOrder = async (reason: string) => {
  await request.put(`/api/merchant/orders/${orderId}/cancel`, {
    cancelReason: reason
  });
  showSuccessToast('订单已取消');
  showCancelDialog.value = false;
  cancelReasonInput.value = '';
  fetchOrder();
};

const confirmNoShow = () => {
  showConfirmDialog({
    title: '确认逃单',
    message: '确认将该订单标记为“用户超时未取”吗？系统会记录本次逃单并触发对应处罚。',
    confirmButtonText: '确认逃单',
    cancelButtonText: '再想想',
  }).then(async () => {
    try {
      await submitCancelOrder('用户超时未取');
    } catch (e: any) {
      showToast(e?.message || '操作失败');
    }
  }).catch(() => {
    // 用户取消操作
  });
};

// 选择常用原因
const selectCommonReason = (reason: string) => {
  cancelReasonInput.value = reason;
};

const cancelOrder = () => {
  cancelReasonInput.value = '';
  showCancelDialog.value = true;
};

const closeCancelDialog = () => {
  showCancelDialog.value = false;
  cancelReasonInput.value = '';
};

const confirmCancelOrder = () => {
  const reason = cancelReasonInput.value.trim();
  if (!reason) {
    showToast('请输入取消原因');
    return;
  }
  
  showConfirmDialog({
    title: '确认取消',
    message: `确认取消该订单？\n原因：${reason}`,
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想',
  }).then(async () => {
    try {
      await submitCancelOrder(reason);
    } catch (e: any) {
      showToast(e?.message || '取消失败');
    }
  }).catch(() => {
    // 用户取消操作
  });
};

const copyPhone = async () => {
  if (!order.value?.userPhone) {
    showToast('暂无电话号码');
    return;
  }
  try {
    await navigator.clipboard.writeText(order.value.userPhone);
    showToast('电话号码已复制');
  } catch (err) {
    console.error('复制失败:', err);
    showToast('复制失败');
  }
};

const getOrderItemName = (item: any) => item?.dishName || item?.name || item?.ingredientName || '未命名';
const getOrderItemSideValue = (item: any) => {
  const price = Number(item?.price || 0);
  if (price > 0) {
    return `¥${price % 1 === 0 ? price.toFixed(0) : price.toFixed(2)}`;
  }
  const weight = Number(item?.estimatedWeightG || item?.referenceWeightG || 0);
  if (weight > 0) {
    return `${weight}g`;
  }
  return '-';
};
const normalizeAmount = (value: any) => Number(value || 0);
const normalizeWeight = (value: any) => Number(value || 0);
const formatEstimatedWeight = (value: any) => `${normalizeWeight(value)}g`;
const formatEstimatedAmount = (value: any) => `¥${normalizeAmount(value).toFixed(2)}`;
const formatBrothExtraPrice = (value: any) => {
  const amount = normalizeAmount(value);
  return `¥${amount % 1 === 0 ? amount.toFixed(0) : amount.toFixed(2)}`;
};
const calculateFinalAmount = (currentOrder: any, finalWeight: number) => {
  const estimatedWeight = normalizeWeight(currentOrder?.estimatedWeightG);
  const estimatedAmount = normalizeAmount(currentOrder?.estimatedAmount);
  const brothExtraPrice = normalizeAmount(currentOrder?.brothExtraPrice);
  if (estimatedWeight <= 0 || finalWeight <= 0) return 0;
  const ingredientAmount = Math.max(estimatedAmount - brothExtraPrice, 0);
  const pricePerGram = ingredientAmount / estimatedWeight;
  return Number((pricePerGram * finalWeight + brothExtraPrice).toFixed(2));
};
const confirmPriceFinalAmountText = computed(() => {
  if (!order.value) return '¥0.00';
  const finalWeight = normalizeWeight(confirmPriceForm.value.finalWeightG);
  if (finalWeight <= 0) return '¥0.00';
  return `¥${calculateFinalAmount(order.value, finalWeight).toFixed(2)}`;
});
const finalWeightDisplay = computed(() => {
  const finalWeight = normalizeWeight(confirmPriceForm.value.finalWeightG);
  return finalWeight > 0 ? `${finalWeight}g` : '--';
});
const weightInfoRows = computed(() => {
  if (!order.value || order.value.orderMode !== 'weight_selection') return [];
  const rows = [];

  if (order.value.brothName) {
    rows.push({ label: '汤底口味', value: order.value.brothName });
  }
  rows.push({ label: '预计重量', value: order.value.estimatedWeightG ? `${order.value.estimatedWeightG}g` : '未计算', tone: 'estimated' });
  rows.push({ label: '预计金额', value: order.value.estimatedAmount ? `¥${order.value.estimatedAmount}` : '未计算', tone: 'estimated' });
  if (order.value.finalWeightG) {
    rows.push({ label: '最终重量', value: `${order.value.finalWeightG}g`, tone: 'final' });
  }
  if (order.value.finalAmount) {
    rows.push({ label: '最终金额', value: `¥${order.value.finalAmount}`, tone: 'final' });
  }

  return rows;
});
const orderInfoRows = computed(() => {
  if (!order.value) return [];
  const rows = [
    { label: '订单编号', value: String(order.value.id || '-'), wide: true },
    { label: '下单时间', value: formatDateTime(order.value.createTime) || '-' },
    { label: '取餐时间', value: order.value.pickupTime || '-' },
    { label: '取餐方式', value: order.value.needPack === 1 ? '打包' : '堂食' },
    { label: '学生姓名', value: order.value.userName || '未提供' }
  ];

  return rows;
});
const lifecycleRows = computed(() => {
  if (!order.value) return [];
  const rows: Array<{ label: string; value: string; alert?: boolean }> = [];
  if (order.value.readyTime) {
    rows.push({ label: '实际出餐时间', value: formatDateTime(order.value.readyTime) || '未记录' });
  }
  if (order.value.completedTime) {
    rows.push({ label: '实际取餐时间', value: formatDateTime(order.value.completedTime) || '未记录' });
  }
  if (order.value.secondPickupReminderTime) {
    rows.push({ label: '二次提醒时间', value: formatDateTime(order.value.secondPickupReminderTime) || '未记录' });
  }
  if (order.value.pickupOvertimeMinutes > 0) {
    rows.push({ label: '超时取餐', value: `${order.value.pickupOvertimeMinutes} 分钟`, alert: true });
  }
  if (order.value.pickupOvertimeNote) {
    rows.push({ label: '凭证摘要', value: order.value.pickupOvertimeNote, alert: true });
  }
  return rows;
});

onMounted(() => {
  pushExitGuardState();
  window.addEventListener('popstate', handleBrowserBack);
  fetchOrder();
});

onBeforeUnmount(() => {
  window.removeEventListener('popstate', handleBrowserBack);
});
const getImageUrl = (url: string) => {
  if (!url) return 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
  if (url.startsWith('http')) return url;
  // Adjust base URL as needed, assuming same host for now or relative
  return `/api/common/download?name=${url}`; 
};
</script>

<style scoped>
.order-detail-container {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.content {
  padding: 12px 12px 76px;
}

.page-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.loading-state {
  padding: 40px;
  display: flex;
  justify-content: center;
}

.section-card {
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.status-card {
  color: #fff;
  border-radius: 18px;
  padding: 14px 14px 12px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
}

.bg-blue {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 50%, #0f172a 100%);
}

.bg-orange {
  background: linear-gradient(135deg, #ff8a3d 0%, #ea580c 55%, #7c2d12 100%);
}

.bg-green {
  background: linear-gradient(135deg, #10b981 0%, #059669 55%, #064e3b 100%);
}

.bg-gray {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 45%, #334155 100%);
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.status-main {
  min-width: 0;
  flex: 1;
}

.status-text {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.2;
}

.pickup-code {
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.04em;
}

.status-desc {
  margin-top: 3px;
  font-size: 12px;
  line-height: 1.45;
  opacity: 0.88;
}

.status-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.status-pill {
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
  font-size: 11px;
  font-weight: 600;
}

.detail-card {
  padding: 12px;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.section-header--compact,
.section-header--items {
  justify-content: flex-start;
}

.section-badge,
.items-summary {
  flex-shrink: 0;
  padding: 4px 9px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
}

.section-badge--warm {
  background: #fff7ed;
  color: #9a3412;
}

.weight-card {
  background: linear-gradient(180deg, #fffaf5 0%, #fff7ed 100%);
}

.weight-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.weight-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(251, 191, 36, 0.14);
}

.weight-label {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  flex-shrink: 0;
}

.weight-value {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  word-break: break-all;
  text-align: right;
}

.weight-value--estimated {
  color: #334155;
}

.weight-value--final {
  color: #ea580c;
}

.info-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #edf2f7;
}

.info-row--span-2 {
  grid-column: 1 / -1;
}

.info-row--remark {
  align-items: flex-start;
}

.info-label {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
}

.info-value {
  min-width: 0;
  flex: 1;
  text-align: right;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  word-break: break-all;
}

.remark-card {
  display: block;
  flex: 1;
  width: auto;
  max-width: calc(100% - 72px);
  padding: 8px 10px;
  border-radius: 10px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.45;
  text-align: right;
  word-break: break-word;
  box-sizing: border-box;
}

.remark-card--empty {
  background: #f8fafc;
  color: #94a3b8;
}

.record-card {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.record-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.record-item {
  padding: 9px 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.record-item--alert {
  background: #fff7ed;
  border-color: #fed7aa;
}

.record-label {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 3px;
}

.record-value {
  font-size: 12px;
  line-height: 1.45;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.items-card {
  padding: 12px;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #edf2f7;
}

.item-img {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  object-fit: cover;
  background-color: #f5f5f5;
  box-shadow: 0 6px 12px rgba(148, 163, 184, 0.14);
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  color: #0f172a;
  margin-bottom: 2px;
  line-height: 1.4;
  font-weight: 700;
}

.item-opts {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.35;
}

.item-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.item-qty {
  min-width: 34px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  text-align: center;
  font-size: 11px;
  font-weight: 700;
}

.item-price {
  min-width: 56px;
  text-align: right;
  font-size: 14px;
  font-weight: 800;
  color: #ea580c;
}

.divider {
  height: 1px;
  background: linear-gradient(90deg, rgba(226, 232, 240, 0) 0%, rgba(226, 232, 240, 1) 18%, rgba(226, 232, 240, 1) 82%, rgba(226, 232, 240, 0) 100%);
  margin: 10px 0 8px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.total-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.total-subtitle {
  margin-top: 2px;
  font-size: 10px;
  color: #94a3b8;
}

.total-price {
  color: #ea580c;
  font-size: 22px;
  font-weight: 800;
}

.phone-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  flex: 1;
  width: auto;
  max-width: none;
}

.phone-number {
  font-size: 13px;
  color: #0f172a;
  flex: 1;
  text-align: right;
  font-weight: 700;
  word-break: break-all;
}

.copy-btn {
  flex-shrink: 0;
  min-width: 48px;
  font-size: 11px;
  padding: 0 10px;
  height: 24px;
  line-height: 22px;
  box-shadow: none;
}

.action-bar-placeholder {
  height: 58px;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  padding: 8px 10px calc(8px + env(safe-area-inset-bottom));
  box-shadow: 0 -10px 24px rgba(15, 23, 42, 0.06);
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  z-index: 99;
}

.action-bar__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.action-bar__actions--triple {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.action-bar :deep(.van-button) {
  height: 38px;
  font-size: 13px;
  font-weight: 700;
  border: none;
  box-shadow: none;
}

.no-show-button {
  box-shadow: none;
}

.cancel-button {
  border-color: rgba(249, 115, 22, 0.32) !important;
}

.order-detail-container :deep(.van-nav-bar) {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 101;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
}

.order-detail-container :deep(.van-nav-bar__title) {
  font-size: 16px;
  font-weight: 700;
}

.order-detail-container :deep(.van-nav-bar__text) {
  font-size: 13px;
}

@media (max-width: 420px) {
  .record-list,
  .info-list,
  .weight-list {
    grid-template-columns: 1fr;
  }

  .info-row--span-2 {
    grid-column: auto;
  }
}

/* 取消订单弹窗样式 */
.cancel-dialog {
  padding: 20px;
  background: #fff;
}

.dialog-header {
  text-align: center;
  margin-bottom: 20px;
}

.dialog-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.dialog-subtitle {
  font-size: 14px;
  color: #666;
  line-height: 1.4;
}

.dialog-body {
  margin-bottom: 24px;
}

.reason-input {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e8e8e8;
}

.reason-input:focus-within {
  border-color: #ff976a;
  background: #fff;
}

/* 常用取消原因样式 */
.common-reasons-section {
  margin-bottom: 20px;
}

.common-reasons-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
  font-weight: 500;
}

.common-reasons-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 120px;
  overflow-y: auto;
  padding: 4px;
}

.reason-tag {
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reason-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.dialog-footer {
  display: flex;
  gap: 12px;
}

.dialog-footer .cancel-btn {
  flex: 1;
  border-radius: 8px;
  border: 1px solid #dcdee0;
  font-weight: 500;
}

.dialog-footer .confirm-btn {
  flex: 1;
  border-radius: 8px;
  font-weight: 500;
}

.dialog-footer .confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.confirm-price-sheet {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
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
  display: block;
  height: 100%;
  max-height: 100%;
  min-height: 0;
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
