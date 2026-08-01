<template>
  <view class="page">
    <view class="header">
      <view class="title">{{ shopDetail.name || '自选称重' }}</view>
      <view class="tip">先选汤底，再选食材，系统会按参考重量给出预计价格区间</view>
    </view>

    <view class="section">
      <view class="section-title">汤底口味</view>
      <view class="chip-list">
        <view
          v-for="item in brothOptions"
          :key="item.id"
          class="chip"
          :class="{ active: selectedBrothId === item.id }"
          @tap="selectedBrothId = item.id"
        >
          <text>{{ item.name }}</text>
          <text v-if="Number(item.extraPrice || 0) > 0" class="chip-price">+{{ item.extraPrice }}元</text>
        </view>
      </view>
    </view>

    <view v-for="category in categories" :key="category.categoryId" class="section">
      <view class="section-title">{{ category.categoryName }}</view>
      <view v-for="ingredient in category.ingredients" :key="ingredient.id" class="ingredient-row">
        <view class="ingredient-main">
          <text class="ingredient-name">{{ ingredient.name }}</text>
          <text class="ingredient-meta">{{ ingredient.unitLabel }} / 约{{ ingredient.referenceWeightG }}g</text>
        </view>
        <view class="stepper">
          <view class="stepper-btn" @tap="changeQuantity(ingredient.id, -1)">-</view>
          <text class="stepper-value">{{ quantities[ingredient.id] || 0 }}</text>
          <view class="stepper-btn active" @tap="changeQuantity(ingredient.id, 1)">+</view>
        </view>
      </view>
    </view>

    <view class="footer-placeholder"></view>
    <view class="footer">
      <view class="summary">
        <view>已选 {{ selectedItems.length }} 种，约 {{ estimatedWeightG }}g</view>
        <view>预计 {{ amountRangeText }}</view>
      </view>
      <view class="summary-tip" v-if="minimumOrderWeightG > 0 && estimatedWeightG < minimumOrderWeightG">
        还差约 {{ minimumOrderWeightG - estimatedWeightG }}g 达到最低下单重量
      </view>
      <view class="submit-btn" :class="{ disabled: !canSubmit }" @tap="goConfirm">去确认</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { http } from '@/utils/http'
import { safeNavigateTo } from '@/utils/navigation'

const shopId = ref<number | null>(null)
const shopDetail = ref<any>({})
const brothOptions = ref<any[]>([])
const categories = ref<any[]>([])
const selectedBrothId = ref<number | null>(null)
const quantities = ref<Record<number, number>>({})

const minimumOrderWeightG = computed(() => Number(shopDetail.value.minimumOrderWeightG || 0))

const flatIngredients = computed(() =>
  categories.value.flatMap((category: any) => category.ingredients || [])
)

const selectedBroth = computed(() =>
  brothOptions.value.find(item => item.id === selectedBrothId.value) || null
)

const selectedItems = computed(() =>
  flatIngredients.value
    .filter((ingredient: any) => (quantities.value[ingredient.id] || 0) > 0)
    .map((ingredient: any) => {
      const quantity = quantities.value[ingredient.id]
      const estimatedWeightG = quantity * Number(ingredient.referenceWeightG || 0)
      return {
        ingredientId: ingredient.id,
        name: ingredient.name,
        unitLabel: ingredient.unitLabel,
        quantity,
        referenceWeightG: Number(ingredient.referenceWeightG || 0),
        estimatedWeightG,
        image: ingredient.image || ''
      }
    })
)

const estimatedWeightG = computed(() =>
  selectedItems.value.reduce((sum: number, item: any) => sum + item.estimatedWeightG, 0)
)

const estimatedAmount = computed(() => {
  const pricePer500g = Number(shopDetail.value.weightPricePer500g || 0)
  if (!pricePer500g || estimatedWeightG.value <= 0) return 0
  const brothExtra = Number(selectedBroth.value?.extraPrice || 0)
  return Number(((estimatedWeightG.value / 500) * pricePer500g + brothExtra).toFixed(2))
})

const amountRangeText = computed(() => {
  if (!estimatedAmount.value) return '0-0元'
  const spread = estimatedAmount.value < 15 ? 2 : estimatedAmount.value < 25 ? 3 : Math.max(3, estimatedAmount.value * 0.12)
  const min = Math.max(0, Math.floor(estimatedAmount.value - spread))
  const max = Math.ceil(estimatedAmount.value + spread)
  return `${min}-${max}元`
})

const canSubmit = computed(() =>
  !!selectedBrothId.value &&
  selectedItems.value.length > 0 &&
  (minimumOrderWeightG.value <= 0 || estimatedWeightG.value >= minimumOrderWeightG.value)
)

const fetchData = async () => {
  const shopRes: any = await http({ url: `/api/client/shops/${shopId.value}` })
  shopDetail.value = shopRes.data
  const configRes: any = await http({ url: `/api/client/shops/${shopId.value}/weight-selection-config` })
  brothOptions.value = configRes.data.brothOptions || []
  categories.value = configRes.data.categories || []
  if (brothOptions.value.length > 0) {
    selectedBrothId.value = brothOptions.value[0].id
  }
}

const changeQuantity = (ingredientId: number, delta: number) => {
  const current = quantities.value[ingredientId] || 0
  const next = Math.max(0, current + delta)
  quantities.value = {
    ...quantities.value,
    [ingredientId]: next
  }
}

const goConfirm = () => {
  if (!selectedBrothId.value) {
    uni.showToast({ title: '请先选择汤底', icon: 'none' })
    return
  }
  if (selectedItems.value.length === 0) {
    uni.showToast({ title: '请先选择食材', icon: 'none' })
    return
  }
  if (minimumOrderWeightG.value > 0 && estimatedWeightG.value < minimumOrderWeightG.value) {
    uni.showToast({ title: `至少选择${minimumOrderWeightG.value}g`, icon: 'none' })
    return
  }

  uni.setStorageSync('weightSelectionDraft', {
    shopId: shopId.value,
    shopName: shopDetail.value.name,
    brothOptionId: selectedBrothId.value,
    brothName: selectedBroth.value?.name || '',
    brothImage: selectedBroth.value?.image || '',
    brothExtraPrice: Number(selectedBroth.value?.extraPrice || 0),
    estimatedWeightG: estimatedWeightG.value,
    estimatedAmount: estimatedAmount.value,
    amountRangeText: amountRangeText.value,
    items: selectedItems.value
  })

  safeNavigateTo({
    url: `/pages/confirm/confirm?mode=weight_selection&shopId=${shopId.value}`
  })
}

onLoad((options: any) => {
  shopId.value = Number(options.shopId)
  fetchData()
})
</script>

<style lang="scss">
.page { min-height: 100vh; background: #f5f7fa; }
.header, .section { background: #fff; margin: 20rpx; border-radius: 20rpx; padding: 24rpx; }
.title { font-size: 36rpx; font-weight: bold; color: #333; }
.tip { margin-top: 12rpx; font-size: 24rpx; color: #888; }
.section-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; }
.chip-list { display: flex; flex-wrap: wrap; gap: 16rpx; }
.chip { padding: 16rpx 24rpx; border-radius: 999rpx; background: #f1f5f9; color: #475569; display: flex; gap: 10rpx; }
.chip.active { background: #e0f2ff; color: #2a8bff; }
.chip-price { font-size: 22rpx; }
.ingredient-row { display: flex; align-items: center; justify-content: space-between; padding: 18rpx 0; border-bottom: 1rpx solid #f1f5f9; }
.ingredient-row:last-child { border-bottom: none; }
.ingredient-main { display: flex; flex-direction: column; gap: 8rpx; }
.ingredient-name { font-size: 28rpx; color: #333; }
.ingredient-meta { font-size: 24rpx; color: #888; }
.stepper { display: flex; align-items: center; gap: 16rpx; }
.stepper-btn { width: 56rpx; height: 56rpx; border-radius: 28rpx; background: #eef2f7; display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: #64748b; }
.stepper-btn.active { background: #2a8bff; color: #fff; }
.stepper-value { min-width: 30rpx; text-align: center; font-size: 28rpx; color: #333; }
.footer-placeholder { height: 220rpx; }
.footer { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 24rpx 24rpx 36rpx; box-shadow: 0 -8rpx 30rpx rgba(0,0,0,0.08); }
.summary { display: flex; justify-content: space-between; font-size: 26rpx; color: #333; }
.summary-tip { margin-top: 10rpx; font-size: 24rpx; color: #ff8a3d; }
.submit-btn { margin-top: 20rpx; height: 88rpx; border-radius: 44rpx; background: #2a8bff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 30rpx; font-weight: 600; }
.submit-btn.disabled { background: #cbd5e1; }
</style>
