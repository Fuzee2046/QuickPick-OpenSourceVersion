<template>
  <div class="profile-container">
    <!-- 1. 顶部身份卡 -->
    <div class="profile-header">
      <div class="header-content">
        <div class="shop-avatar">
          <van-image 
            round 
            width="72" 
            height="72" 
            :src="shopInfo.logo || 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'" 
            fit="cover"
          />
          <div class="status-badge" :class="shopInfo.status === 1 ? 'open' : 'closed'">
            {{ shopInfo.status === 1 ? '营业中' : '已打烊' }}
          </div>
        </div>
        
        <div class="shop-meta">
          <h2 class="shop-name">{{ shopInfo.name || auth.shopName }}</h2>
          <div class="meta-row">
            <van-icon name="location-o" />
            <span class="text">{{ shopInfo.address || '未设置地址' }}</span>
          </div>
          <div class="meta-row">
            <van-icon name="phone-o" />
            <span class="text">{{ shopInfo.phone || '未设置电话' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="section-card billing-entry" @click="router.push('/billing')">
      <div class="billing-entry-main">
        <div class="icon-box billing-icon">
          <img src="/favicon.ico" alt="食刻快取" />
        </div>
        <div class="billing-copy">
          <div class="billing-title-row">
            <span>运营成本费</span>
            <van-tag v-if="billingStatus.type" :type="billingStatus.type" plain>
              {{ billingStatus.label }}
            </van-tag>
          </div>
          <small>{{ billingStatus.description }}</small>
        </div>
      </div>
      <van-icon name="arrow" class="arrow-icon" />
    </div>

    <!-- 2. 核心信息管理区 -->
    <div class="section-card info-management">
      <div class="section-title">店铺信息</div>
      
      <div class="info-item" @click="openHoursEdit">
        <div class="label">营业时间</div>
        <div class="value-row">
          <span class="value text-ellipsis">{{ formatBusinessHours }}</span>
          <van-icon name="arrow" class="arrow-icon" />
        </div>
      </div>

      <div class="info-item">
        <div class="label">联系电话</div>
        <div class="value-row">
          <span class="value">{{ shopInfo.phone || '未设置' }}</span>
        </div>
      </div>
      
      <div class="info-item">
        <div class="label">店铺地址</div>
        <div class="value-row">
          <span class="value text-ellipsis">{{ shopInfo.address || '未设置' }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 账户设置与安全区 -->
    <div class="section-card account-settings">
      <div class="section-title">账户与设置</div>
      
      <div class="setting-item">
        <div class="left">
          <div class="icon-box status-icon"><van-icon name="shop-o" /></div>
          <span>营业状态</span>
        </div>
        <van-switch 
          v-model="isOpen" 
          :loading="statusLoading"
          size="24px" 
          active-color="#00b894" 
          inactive-color="#e5e7eb"
          @change="toggleShopStatus"
        />
      </div>

      <div class="setting-item">
        <div class="left">
          <div class="icon-box info-icon"><van-icon name="underway-o" /></div>
          <span>高峰预约限制</span>
        </div>
        <van-switch
          v-model="peakLimitEnabled"
          :loading="peakLimitLoading"
          size="24px"
          active-color="#ff8a3d"
          inactive-color="#e5e7eb"
          @change="togglePeakLimit"
        />
      </div>

      <div class="setting-item" @click="router.push('/change-password')">
        <div class="left">
          <div class="icon-box pwd-icon"><van-icon name="lock" /></div>
          <span>修改密码</span>
        </div>
        <van-icon name="arrow" class="arrow-icon" />
      </div>
      
      <div class="setting-item">
        <div class="left">
          <div class="icon-box info-icon"><van-icon name="info-o" /></div>
          <span>关于我们</span>
        </div>
        <span class="version-text">v6.0.0</span>
      </div>
    </div>

    <!-- 4. 安全退出区 -->
    <div class="logout-section">
      <button class="logout-btn" @click="onLogout">退出登录</button>
    </div>

    <!-- Business Hours Edit Popup -->
    <van-popup 
      v-model:show="showHoursEdit" 
      position="bottom" 
      round 
      safe-area-inset-bottom
      :style="{ height: 'auto', maxHeight: '80%' }"
    >
      <div class="popup-header">
        <div class="cancel-btn" @click="showHoursEdit = false">取消</div>
        <div class="title">设置营业时间</div>
        <div class="confirm-btn" @click="saveHours">保存</div>
      </div>
      
      <div class="popup-content">
        <van-cell-group inset>
          <van-cell center title="中途休息">
            <template #right-icon>
              <van-switch v-model="hasBreak" size="24" active-color="#00b894" />
            </template>
          </van-cell>
          
          <div class="time-section">
            <div class="section-label">第一时段 (必填)</div>
            <van-cell title="开始时间" is-link :value="tempHours.openTime1 || '请选择'" @click="pickTime('openTime1')" />
            <van-cell title="结束时间" is-link :value="tempHours.closeTime1 || '请选择'" @click="pickTime('closeTime1')" />
          </div>

          <div v-if="hasBreak" class="time-section">
            <div class="section-label">第二时段</div>
            <van-cell title="开始时间" is-link :value="tempHours.openTime2 || '请选择'" @click="pickTime('openTime2')" />
            <van-cell title="结束时间" is-link :value="tempHours.closeTime2 || '请选择'" @click="pickTime('closeTime2')" />
          </div>
        </van-cell-group>
      </div>
    </van-popup>

    <!-- Time Picker Popup -->
    <van-popup v-model:show="showTimePicker" position="bottom" round safe-area-inset-bottom>
      <van-time-picker
        v-model="pickerTimeValue"
        title="选择时间"
        format="HH:mm"
        @confirm="onTimeConfirm"
        @cancel="showTimePicker = false"
      />
    </van-popup>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { showConfirmDialog, showSuccessToast, showFailToast } from 'vant';

const auth = useAuthStore();
const router = useRouter();

// State
const shopInfo = ref<any>({
  status: 0,
  name: '',
  address: '',
  phone: '',
  logo: '',
  peakLimitEnabled: 0,
  openTime1: '08:00',
  closeTime1: '20:00',
  openTime2: null,
  closeTime2: null
});
const statusLoading = ref(false);
const peakLimitLoading = ref(false);
const billingOverview = ref<any>({});

// Dialog Controls
const showChangePwd = ref(false);

// Business Hours Logic
const showHoursEdit = ref(false);
const hasBreak = ref(false);
const tempHours = ref<any>({});
const showTimePicker = ref(false);
const currentPickerField = ref('');
const pickerTimeValue = ref<string[]>(['08', '00']);

const formatBusinessHours = computed(() => {
  const { openTime1, closeTime1, openTime2, closeTime2 } = shopInfo.value;
  if (!openTime1 || !closeTime1) return '未设置';
  
  // Format times to HH:mm (remove seconds if present)
  const fmt = (t: string) => t ? t.substring(0, 5) : '';
  
  let text = `${fmt(openTime1)}-${fmt(closeTime1)}`;
  if (openTime2 && closeTime2) {
    text += `, ${fmt(openTime2)}-${fmt(closeTime2)}`;
  }
  return text;
});

const billingStatus = computed(() => {
  const reminder = billingOverview.value.paymentReminder;
  if (reminder?.overdue) {
    return { type: 'danger' as const, label: '已逾期', description: `待支付 ¥${Number(reminder.amount || 0).toFixed(2)}，接单服务已暂停` };
  }
  if (reminder) {
    return { type: 'warning' as const, label: '待支付', description: `待支付 ¥${Number(reminder.amount || 0).toFixed(2)}，请及时完成支付` };
  }
  return { type: 'success' as const, label: '正常', description: '查看本月预估费用与历史账单' };
});

// Computed Switch State
const isOpen = computed({
  get: () => shopInfo.value.status === 1,
  set: (val) => {
    // Optimistic update handled in toggle
  }
});

const peakLimitEnabled = computed({
  get: () => shopInfo.value.peakLimitEnabled === 1,
  set: () => {
    // Optimistic update handled in toggle
  }
});

// Actions
const fetchShopProfile = async () => {
  try {
    const res: any = await request.get('/api/merchant/auth/profile');
    
    // Check if status needs to be updated based on time
    // Client-side time check for display consistency
    let displayStatus = res.status;
    const now = new Date();
    // Get HH:mm string manually to avoid timezone issues with toTimeString()
    const timeStr = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0');
    
    const isTimeInRange = (start: string, end: string) => {
      if (!start || !end) return false;
      // Simple string comparison works for HH:mm
      if (start > end) { // Cross midnight e.g. 22:00 - 02:00
        return timeStr >= start || timeStr <= end;
      } else {
        return timeStr >= start && timeStr <= end;
      }
    };
    
    let isOpenTime = false;
    // Check first slot
    if (res.openTime1 && res.closeTime1) {
        if (isTimeInRange(res.openTime1.substring(0, 5), res.closeTime1.substring(0, 5))) {
            isOpenTime = true;
        }
    }
    // Check second slot
    if (!isOpenTime && res.openTime2 && res.closeTime2) {
        if (isTimeInRange(res.openTime2.substring(0, 5), res.closeTime2.substring(0, 5))) {
            isOpenTime = true;
        }
    }
    
    // If current time is NOT in any open slot, force status to closed (0) for display
    if (!isOpenTime) {
       displayStatus = 0;
    }

    shopInfo.value = {
      name: res.shopName,
      status: displayStatus,
      address: res.address,
      phone: res.contactPhone,
      logo: res.logoImage,
      peakLimitEnabled: res.peakLimitEnabled ?? 0,
      openTime1: res.openTime1,
      closeTime1: res.closeTime1,
      openTime2: res.openTime2,
      closeTime2: res.closeTime2
    };
  } catch (e) {
    console.error(e);
  }
};

const fetchBillingOverview = async () => {
  try {
    billingOverview.value = await request.get('/api/merchant/billing/overview');
  } catch (error) {
    console.error('Failed to load billing overview', error);
  }
};

const openHoursEdit = () => {
  // Init temp state
  tempHours.value = {
    openTime1: shopInfo.value.openTime1 ? shopInfo.value.openTime1.substring(0, 5) : '08:00',
    closeTime1: shopInfo.value.closeTime1 ? shopInfo.value.closeTime1.substring(0, 5) : '20:00',
    openTime2: shopInfo.value.openTime2 ? shopInfo.value.openTime2.substring(0, 5) : null,
    closeTime2: shopInfo.value.closeTime2 ? shopInfo.value.closeTime2.substring(0, 5) : null
  };
  hasBreak.value = !!(tempHours.value.openTime2 && tempHours.value.closeTime2);
  showHoursEdit.value = true;
};

const pickTime = (field: string) => {
  currentPickerField.value = field;
  const val = tempHours.value[field] || '08:00';
  pickerTimeValue.value = val.split(':');
  showTimePicker.value = true;
};

const onTimeConfirm = ({ selectedValues }: any) => {
  tempHours.value[currentPickerField.value] = selectedValues.join(':');
  showTimePicker.value = false;
};

watch(hasBreak, (value) => {
  if (!value) {
    tempHours.value.openTime2 = null;
    tempHours.value.closeTime2 = null;
  }
});

const saveHours = async () => {
  // Validate
  if (!tempHours.value.openTime1 || !tempHours.value.closeTime1) {
    showFailToast('请完善第一段营业时间');
    return;
  }
  
  if (hasBreak.value) {
    if (!tempHours.value.openTime2 || !tempHours.value.closeTime2) {
      showFailToast('请完善第二段营业时间');
      return;
    }
  } else {
    tempHours.value.openTime2 = null;
    tempHours.value.closeTime2 = null;
  }

  try {
    await request.put('/api/merchant/shop/business-hours', tempHours.value);
    
    // Update local state
    shopInfo.value.openTime1 = tempHours.value.openTime1;
    shopInfo.value.closeTime1 = tempHours.value.closeTime1;
    shopInfo.value.openTime2 = tempHours.value.openTime2;
    shopInfo.value.closeTime2 = tempHours.value.closeTime2;
    
    showSuccessToast('营业时间已更新');
    showHoursEdit.value = false;
  } catch (e) {
    showFailToast('更新失败');
  }
};

const toggleShopStatus = async (checked: boolean) => {
  statusLoading.value = true;
  const newStatus = checked ? 1 : 0;
  try {
    // 修正后的API地址
    await request.put('/api/merchant/shop/status', { status: newStatus });
    shopInfo.value.status = newStatus;
    showSuccessToast(newStatus === 1 ? '店铺已营业' : '店铺已打烊');
  } catch (e) {
    // Revert switch on error
    showFailToast('状态切换失败');
    fetchShopProfile(); 
  } finally {
    statusLoading.value = false;
  }
};

const togglePeakLimit = async (checked: boolean) => {
  peakLimitLoading.value = true;
  const newValue = checked ? 1 : 0;
  try {
    await request.put('/api/merchant/shop/peak-limit', { peakLimitEnabled: newValue });
    shopInfo.value.peakLimitEnabled = newValue;
    showSuccessToast(newValue === 1 ? '已开启高峰预约限制' : '已关闭高峰预约限制');
  } catch (e) {
    showFailToast('高峰限制切换失败');
    fetchShopProfile();
  } finally {
    peakLimitLoading.value = false;
  }
};

const onLogout = async () => {
  showConfirmDialog({
    title: '退出登录',
    message: '确定要退出登录吗？您将无法接收新订单通知。',
    confirmButtonText: '确定退出',
    confirmButtonColor: '#ef4444'
  })
    .then(() => {
      auth.logout();
      router.replace('/login');
    })
    .catch(() => {});
};

onMounted(() => {
  fetchShopProfile();
  fetchBillingOverview();
});
</script>

<style scoped>
.profile-container {
  min-height: 100%;
  background-color: #f8fafc;
  padding-bottom: 40px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* 1. Header Card */
.profile-header {
  background-color: #fff;
  padding: 24px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.02);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.shop-avatar {
  position: relative;
  flex-shrink: 0;
}

.status-badge {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  color: white;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  transition: all 0.3s;
}

.status-badge.open { background-color: #00b894; }
.status-badge.closed { background-color: #95a5a6; }

.shop-meta {
  flex: 1;
  overflow: hidden;
}

.shop-name {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.meta-row .text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 2. Section Cards */
.section-card {
  background-color: #fff;
  margin: 0 16px 16px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
  padding: 16px 16px 8px;
}

.billing-entry{padding:16px;display:flex;align-items:center;justify-content:space-between;cursor:pointer}.billing-entry:active{background:#f8fafc}.billing-entry-main{min-width:0;display:flex;align-items:center;gap:12px}.billing-icon{flex:0 0 auto;background:#eef4ff}.billing-icon img{width:24px;height:24px;border-radius:6px}.billing-copy{min-width:0;display:flex;flex-direction:column;gap:5px}.billing-title-row{display:flex;align-items:center;gap:8px;color:#2c3e50;font-size:15px;font-weight:600}.billing-copy small{overflow:hidden;color:#94a3b8;font-size:12px;white-space:nowrap;text-overflow:ellipsis}

/* Info Items */
.info-item {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}

.info-item:last-child { border-bottom: none; }

.info-item .label {
  font-size: 15px;
  color: #2c3e50;
  font-weight: 500;
}

.value-row {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 60%;
}

.value-row .value {
  font-size: 15px;
  color: #64748b;
}

.value-row .text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Setting Items */
.setting-item {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}

.setting-item:last-child { border-bottom: none; }
.setting-item:active { background-color: #f8fafc; }

.setting-item .left {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #2c3e50;
  font-weight: 500;
}

.icon-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.status-icon { background-color: #e0f2fe; color: #1a8cff; }
.pwd-icon { background-color: #fef3c7; color: #f59e0b; }
.info-icon { background-color: #f3f4f6; color: #6b7280; }

.arrow-icon { color: #cbd5e1; }
.version-text { font-size: 13px; color: #94a3b8; }

/* 4. Logout */
.logout-section {
  padding: 20px 16px;
}

.logout-btn {
  width: 100%;
  padding: 14px;
  background-color: #fee2e2;
  color: #ef4444;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:active {
  background-color: #fecaca;
  transform: scale(0.98);
}

/* Popup Styles */
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.popup-header .title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.popup-header .cancel-btn {
  color: #94a3b8;
  font-size: 14px;
  padding: 8px;
}

.popup-header .confirm-btn {
  color: #00b894;
  font-size: 14px;
  font-weight: 600;
  padding: 8px;
}

.popup-content {
  padding: 16px 0 32px;
  background-color: #f8fafc;
}

.time-section {
  margin-top: 16px;
}

.section-label {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 8px 16px;
}
</style>
