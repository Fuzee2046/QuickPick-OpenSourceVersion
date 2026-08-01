<template>
  <view class="menu-container">
    <view class="header-section">
      <view class="header-bg-wrapper">
        <image
          v-if="getImageUrl(shopDetail.coverImage)"
          :src="getImageUrl(shopDetail.coverImage)"
          class="header-bg"
          mode="aspectFill"
          lazy-load
        />
        <view v-else class="header-bg default-header-bg"></view>
        <view class="header-mask"></view>
      </view>

      <view class="header-content">
        <uni-icons type="back" size="24" color="#fff" @tap="goBack"></uni-icons>
      </view>

      <view class="shop-card-wrapper">
        <view class="shop-card">
          <image
            v-if="getImageUrl(shopDetail.logoImage)"
            :src="getImageUrl(shopDetail.logoImage)"
            class="shop-logo"
            lazy-load
          />
          <view v-else class="shop-logo default-logo">
            <uni-icons type="shop-filled" size="30" color="#ccc"></uni-icons>
          </view>
          <view class="shop-info">
            <view class="shop-title-row">
              <text class="shop-name">{{ shopDetail.name }}</text>
              <text
                v-if="shopDetail.displayStatusText"
                class="shop-status-tip"
                :class="`shop-status-tip--${shopDetail.displayStatus || 'open'}`"
              >
                {{ getShopStatusTip(shopDetail) }}
              </text>
              <text v-if="isWeightSelection" class="shop-mode-badge">自选称重</text>
            </view>
            <view class="shop-detail-row">
              <uni-icons type="location" size="14" color="#666"></uni-icons>
              <text class="shop-address">{{ shopDetail.address }}</text>
            </view>
            <view v-if="shopDetail.openTime1" class="shop-detail-row">
              <uni-icons type="calendar" size="14" color="#666"></uni-icons>
              <text class="shop-address">{{ formatHours(shopDetail) }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="search-section">
      <view class="search-box">
        <uni-icons type="search" size="18" color="#8a94a6"></uni-icons>
        <input
          v-model.trim="searchInputKeyword"
          class="search-input"
          type="text"
          confirm-type="search"
          :placeholder="searchPlaceholder"
          placeholder-class="search-input-placeholder"
        />
        <view v-if="searchInputKeyword" class="search-clear" @tap="clearSearch">
          <uni-icons type="clear" size="16" color="#8a94a6"></uni-icons>
        </view>
      </view>
    </view>

    <view class="menu-content">
      <scroll-view
        scroll-y
        class="category-sidebar"
        :scroll-into-view="'tab-' + activeIndex"
        scroll-with-animation
      >
        <view
          v-for="(item, index) in displaySections"
          :key="`${item.sectionKey || index}`"
          :id="'tab-' + index"
          class="category-item"
          :class="{ active: activeIndex === index }"
          @tap="selectCategory(index)"
        >
          <text class="category-name">{{ item.categoryName }}</text>
        </view>
        <view class="sidebar-placeholder"></view>
      </scroll-view>

      <scroll-view
        scroll-y
        class="dish-scroll"
        :scroll-into-view="dishScrollIntoView"
        scroll-with-animation
        @scroll="handleDishScroll"
      >
        <view class="dish-content">
          <view v-if="isWeightSelection" class="weight-banner">
            <view class="weight-banner-main">
              <text class="weight-banner-desc">按参考重量估算金额，最终以商户称重确认结果为准</text>
              <text class="weight-banner-tip">
                {{ hasBrothExtraPrice ? '部分汤底会额外加价' : '汤底默认不额外加价' }}
              </text>
            </view>
            <view class="weight-banner-price">
              <text class="weight-banner-price-label">每500g</text>
              <text class="weight-banner-price-value">￥{{ weightPricePer500g.toFixed(2) }}</text>
            </view>
          </view>

          <view v-if="!displaySections.length" class="search-empty">
            <uni-icons type="search" size="28" color="#c3cad5"></uni-icons>
            <text class="search-empty-title">{{ emptyTitle }}</text>
            <text class="search-empty-desc">{{ emptyDesc }}</text>
          </view>

          <template v-else>
            <view
              v-for="(section, catIdx) in displaySections"
              :key="`${section.sectionKey || catIdx}`"
              :id="'cat-' + catIdx"
              class="cat-section"
            >
              <view class="cat-title">{{ section.categoryName }}</view>

              <template v-if="!isWeightSelection">
                <view v-for="dish in section.dishes" :key="dish.id">
                  <DishItem
                    :dish="dish"
                    :quantity="getCartQuantity(dish.id)"
                    :has-options="hasDishOptions(dish)"
                    @add="addToCart"
                    @remove="removeFromCart"
                    @choose="openDishOptionPopup"
                  />
                </view>
              </template>

              <template v-else-if="section.sectionType === 'broth'">
                <view v-if="brothOptions.length === 0" class="broth-empty-tip">
                  暂未配置汤底，请稍后再试
                </view>
                <view
                  v-for="item in brothOptions"
                  v-else
                  :key="item.id"
                  class="broth-list-card"
                  :class="{ active: selectedBrothId === item.id }"
                  @tap="selectBroth(item.id)"
                >
                  <view class="broth-list-card__image-wrapper" :class="getBrothThemeClass(item.name)">
                    <image
                      v-if="getImageUrl(item.image)"
                      :src="getImageUrl(item.image)"
                      class="broth-list-card__image"
                      mode="aspectFill"
                      lazy-load
                    />
                    <view class="broth-list-card__caption">{{ getBrothCoverCaption(item.name) }}</view>
                  </view>

                  <view class="broth-list-card__content">
                    <view class="broth-list-card__name-row">
                      <text class="broth-list-card__name">{{ item.name }}</text>
                      <text v-if="selectedBrothId === item.id" class="broth-list-card__selected">已选</text>
                    </view>
                    <text class="broth-list-card__price">{{ formatBrothPrice(item.extraPrice) }}</text>
                  </view>

                  <view class="broth-list-card__check">
                    <uni-icons
                      :type="selectedBrothId === item.id ? 'checkbox-filled' : 'circle'"
                      size="20"
                      :color="selectedBrothId === item.id ? '#2a8bff' : '#cbd5e1'"
                    ></uni-icons>
                  </view>
                </view>
              </template>

              <template v-else>
                <view
                  v-for="ingredient in section.ingredients"
                  :key="ingredient.id"
                  class="ingredient-card"
                >
                  <view class="ingredient-image-wrapper">
                    <image
                      v-if="getImageUrl(ingredient.image)"
                      :src="getImageUrl(ingredient.image)"
                      class="ingredient-thumb"
                      mode="aspectFill"
                      lazy-load
                    />
                    <view v-else class="ingredient-thumb ingredient-thumb--fallback">
                      <uni-icons type="fire" size="18" color="#b7c5d9"></uni-icons>
                    </view>
                  </view>

                  <view class="ingredient-main">
                    <view class="ingredient-meta">
                      <view class="ingredient-name-row">
                        <text class="ingredient-name">{{ ingredient.name }}</text>
                        <text
                          v-if="getWeightQuantity(ingredient.id) > 0"
                          class="ingredient-selected-tag"
                        >
                          已选
                        </text>
                      </view>
                      <text class="ingredient-desc">
                        {{ ingredient.unitLabel || '份' }} / 约{{ ingredient.referenceWeightG }}g
                      </text>
                    </view>

                    <view class="ingredient-bottom">
                      <text class="ingredient-price">约{{ ingredient.referenceWeightG }}g/{{ ingredient.unitLabel || '份' }}</text>
                      <view class="ingredient-stepper">
                        <view
                          v-if="getWeightQuantity(ingredient.id) > 0"
                          class="stepper-btn stepper-btn--minus"
                          @tap="changeWeightQuantity(ingredient, -1)"
                        ></view>
                        <text v-if="getWeightQuantity(ingredient.id) > 0" class="stepper-count">{{ getWeightQuantity(ingredient.id) }}</text>
                        <view
                          class="stepper-btn stepper-btn--plus"
                          @tap="changeWeightQuantity(ingredient, 1)"
                        ></view>
                      </view>
                    </view>
                  </view>
                </view>
              </template>
            </view>

            <view
              v-if="isWeightSelection && !filteredIngredientCategories.length"
              class="search-empty search-empty--compact"
            >
              <uni-icons type="search" size="24" color="#c3cad5"></uni-icons>
              <text class="search-empty-title">{{ emptyTitle }}</text>
              <text class="search-empty-desc">{{ emptyDesc }}</text>
            </view>
          </template>

          <view class="dish-bottom-space" :class="{ 'dish-bottom-space--weight': isWeightSelection }"></view>
        </view>
      </scroll-view>
    </view>

    <view
      class="cart-mask"
      :class="{ visible: cartPopupVisible && hasPopupSelection }"
      @tap="closeCartPopup"
    ></view>

    <template v-if="!isWeightSelection">
      <view
        v-if="cartStore.totalQuantity > 0"
        class="cart-popup"
        :class="{ open: cartPopupVisible }"
      >
        <view class="cart-popup-header">
          <text class="cart-popup-title">已选菜品</text>
          <view class="cart-clear" @tap="clearCurrentCart">
            <uni-icons type="trash" size="16" color="#8893a7"></uni-icons>
            <text class="cart-clear-text">一键清空</text>
          </view>
        </view>

        <scroll-view scroll-y class="cart-popup-list">
          <view
            v-for="item in cartStore.items"
            :key="item.cartItemId"
            class="cart-popup-item"
          >
            <view class="cart-popup-main">
              <image
                v-if="getImageUrl(item.image)"
                :src="getImageUrl(item.image)"
                class="cart-popup-thumb"
                mode="aspectFill"
              />
              <view v-else class="cart-popup-thumb cart-popup-thumb--fallback">
                <uni-icons type="fire" size="18" color="#c8d6e5"></uni-icons>
              </view>

              <view class="cart-popup-meta">
                <text class="cart-popup-name">{{ item.name }}</text>
                <text v-if="item.optionSummary" class="cart-popup-option">{{ item.optionSummary }}</text>
                <text class="cart-popup-unit">单价 ￥{{ Number(item.price).toFixed(2) }}</text>
              </view>
            </view>

            <view class="cart-popup-action">
              <text class="cart-popup-subtotal">￥{{ (Number(item.price) * item.quantity).toFixed(2) }}</text>
              <view class="cart-popup-stepper">
                <view class="stepper-btn stepper-btn--minus" @tap="removeCartItem(item)"></view>
                <text class="stepper-count">{{ item.quantity }}</text>
                <view class="stepper-btn stepper-btn--plus" @tap="increaseCartItem(item)"></view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <view v-if="cartStore.totalQuantity > 0" class="cart-bar">
        <view class="cart-trigger" @tap="toggleCartPopup">
          <view class="cart-info">
            <view class="cart-icon-wrapper" :class="{ active: cartPopupVisible }">
              <uni-icons type="cart-filled" size="24" color="#2a8bff"></uni-icons>
              <text class="badge">{{ cartStore.totalQuantity }}</text>
            </view>

            <view class="cart-summary">
              <view class="cart-summary-row">
                <text class="total-label">合计</text>
                <text class="total-price">￥{{ cartStore.totalAmount.toFixed(2) }}</text>
              </view>
              <text class="cart-hint">
                {{ cartPopupVisible ? '点击收起购物车' : '点击查看已选菜品' }}
              </text>
            </view>
          </view>

          <uni-icons
            type="bottom"
            size="16"
            color="#8a94a6"
            class="cart-arrow"
            :class="{ open: cartPopupVisible }"
          ></uni-icons>
        </view>

        <view class="btn-submit" @tap.stop="goToConfirm">去结算</view>
      </view>
    </template>

    <template v-else>
      <view
        v-if="selectedWeightItems.length > 0"
        class="cart-popup"
        :class="{ open: cartPopupVisible }"
      >
        <view class="cart-popup-header">
          <text class="cart-popup-title">已选食材</text>
          <view class="cart-clear" @tap="clearWeightSelection">
            <uni-icons type="trash" size="16" color="#8893a7"></uni-icons>
            <text class="cart-clear-text">一键清空</text>
          </view>
        </view>

        <view class="weight-popup-summary">
          <view class="weight-popup-summary-row">
            <text class="weight-popup-summary-label">汤底</text>
            <text class="weight-popup-summary-value">
              {{ selectedBroth ? selectedBroth.name : '未选择' }}
            </text>
          </view>
          <view class="weight-popup-summary-row">
            <text class="weight-popup-summary-label">预计</text>
            <text class="weight-popup-summary-value">{{ displayWeightPriceText }} / 约{{ estimatedWeightG }}g</text>
          </view>
        </view>

        <scroll-view scroll-y class="cart-popup-list">
          <view
            v-for="item in selectedWeightItems"
            :key="item.ingredientId"
            class="cart-popup-item"
          >
            <view class="cart-popup-main">
              <image
                v-if="getImageUrl(item.image)"
                :src="getImageUrl(item.image)"
                class="cart-popup-thumb"
                mode="aspectFill"
              />
              <view v-else class="cart-popup-thumb cart-popup-thumb--fallback">
                <uni-icons type="fire" size="18" color="#c8d6e5"></uni-icons>
              </view>

              <view class="cart-popup-meta">
                <text class="cart-popup-name">{{ item.name }}</text>
                <text class="cart-popup-unit">
                  {{ item.unitLabel }} / 约{{ item.referenceWeightG }}g
                </text>
              </view>
            </view>

            <view class="cart-popup-action">
              <text class="cart-popup-subtotal">约{{ item.estimatedWeightG }}g</text>
              <view class="cart-popup-stepper">
                <view
                  class="stepper-btn stepper-btn--minus"
                  @tap="removeWeightPopupItem(item)"
                ></view>
                <text class="stepper-count">{{ item.quantity }}</text>
                <view
                  class="stepper-btn stepper-btn--plus"
                  @tap="increaseWeightPopupItem(item)"
                ></view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </template>

    <view v-if="isWeightSelection" class="weight-bar">
      <view class="weight-bar-main">
        <view class="cart-trigger" @tap="toggleCartPopup">
          <view class="cart-info">
            <view class="cart-icon-wrapper" :class="{ active: cartPopupVisible }">
              <uni-icons type="cart-filled" size="24" color="#2a8bff"></uni-icons>
              <text v-if="selectedWeightTotalQuantity > 0" class="badge">
                {{ selectedWeightTotalQuantity }}
              </text>
            </view>

            <view class="cart-summary">
              <view class="cart-summary-row">
                <text class="total-price">{{ displayWeightPriceText }}</text>
                <text class="weight-total-text">约{{ estimatedWeightG }}g</text>
              </view>
              <text class="cart-hint">
                {{
                  selectedWeightItems.length > 0
                    ? (cartPopupVisible ? '点击收起已选食材' : '点击查看已选食材')
                    : '请选择汤底和食材'
                }}
              </text>
            </view>
          </view>

          <uni-icons
            type="bottom"
            size="16"
            color="#8a94a6"
            class="cart-arrow"
            :class="{ open: cartPopupVisible }"
          ></uni-icons>
        </view>

        <view class="weight-bar-actions">
          <view
            class="btn-submit"
            :class="{ 'btn-submit--disabled': !canSubmitWeight }"
            @tap="goToConfirm"
          >
            {{ weightConfirmButtonText }}
          </view>
        </view>
      </view>
    </view>

    <view
      v-if="showDishOptionPopup"
      class="option-popup-mask"
      @tap="closeDishOptionPopup"
    ></view>
    <view v-if="showDishOptionPopup && currentOptionDish" class="option-popup-wrapper">
      <view class="option-popup">
        <view class="option-popup__header">
          <view>
            <text class="option-popup__title">{{ currentOptionDish.name }}</text>
            <text class="option-popup__subtitle">请选择需要的规格后加入购物车</text>
          </view>
        </view>

        <scroll-view scroll-y class="option-popup__body">
          <view
            v-for="group in currentOptionGroups"
            :key="group.optionGroupId"
            class="option-group-card"
          >
            <view class="option-group-card__header">
              <text class="option-group-card__title">{{ group.name }}</text>
              <text class="option-group-card__tag">
                {{ group.required === 1 ? '必选' : '可不选' }}
              </text>
            </view>
            <view class="option-group-card__values">
              <view
                v-for="value in group.values || []"
                :key="value.optionValueId"
                class="option-value-chip"
                :class="{ active: currentOptionSelections[group.optionGroupId] === value.optionValueId }"
                @tap="selectDishOptionValue(group, value)"
              >
                <text class="option-value-chip__name">{{ value.name }}</text>
                <text class="option-value-chip__price">
                  {{ Number(value.extraPrice || 0) > 0 ? `+￥${Number(value.extraPrice).toFixed(2)}` : '不加价' }}
                </text>
              </view>
            </view>
          </view>
        </scroll-view>

        <view class="option-popup__footer">
          <view class="option-popup__price-box">
            <text class="option-popup__price-label">当前单价</text>
            <text class="option-popup__price-value">￥{{ currentOptionDishPrice.toFixed(2) }}</text>
            <text v-if="currentOptionSummary" class="option-popup__summary">{{ currentOptionSummary }}</text>
          </view>
          <view class="option-popup__confirm" @tap="confirmDishOptionSelection">加入购物车</view>
        </view>
      </view>

      <view class="option-popup__bottom-close" @tap="closeDishOptionPopup">
        <uni-icons type="closeempty" size="22" color="#ffffff"></uni-icons>
      </view>
    </view>

    <BrandLoadingOverlay
      :visible="pageLoadingVisible"
      :title="pageLoadingTitle"
      :description="pageLoadingDescription"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onUnmounted, ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { http, baseURL } from '@/utils/http'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { usePageShare } from '@/utils/share'
import { safeNavigateTo, smartNavigateBack } from '@/utils/navigation'
import DishItem from '@/components/DishItem.vue'
import BrandLoadingOverlay from '@/components/BrandLoadingOverlay.vue'
import { useBrandLoading } from '@/composables/useBrandLoading'

const instance = getCurrentInstance()
const cartStore = useCartStore()
const userStore = useUserStore()
const SEARCH_DEBOUNCE_MS = 180
const SECTION_MEASURE_DEBOUNCE_MS = 60
const SCROLL_THROTTLE_MS = 80
const {
  visible: pageLoadingVisible,
  title: pageLoadingTitle,
  description: pageLoadingDescription,
  show: showPageLoading,
  hide: hidePageLoading
} = useBrandLoading({
  title: '正在打开店铺菜单',
  description: '菜品和营业信息正在同步，请稍候'
})

const shopId = ref<number>(0)
const shopDetail = ref<any>({})
const menuData = ref<any[]>([])
const weightMenuData = ref<any[]>([])
const brothOptions = ref<any[]>([])
const selectedBrothId = ref<number | null>(null)
const weightQuantities = ref<Record<number, number>>({})
const activeIndex = ref(0)
const dishScrollIntoView = ref('')
const categorySectionTops = ref<number[]>([])
const cartPopupVisible = ref(false)
const showDishOptionPopup = ref(false)
const currentOptionDish = ref<any | null>(null)
const currentOptionSelections = ref<Record<number, number | null>>({})
const searchInputKeyword = ref('')
const searchKeyword = ref('')
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null
let sectionMeasureTimer: ReturnType<typeof setTimeout> | null = null
let dishScrollThrottleTimer: ReturnType<typeof setTimeout> | null = null
let latestDishScrollTop = 0

const isWeightSelection = computed(() => shopDetail.value?.shopMode === 'weight_selection')
const minimumOrderWeightG = computed(() => Number(shopDetail.value?.minimumOrderWeightG || 0))
const weightPricePer500g = computed(() => Number(shopDetail.value?.weightPricePer500g || 0))

usePageShare(() => ({
  title: shopDetail.value?.name
    ? `${shopDetail.value.name}｜下课前先点好，到店直接取`
    : 'QuickPick 点餐，提前预订免排队',
  path: '/pages/menu/menu',
  query: shopId.value ? `shopId=${shopId.value}` : '',
  imageUrl: getImageUrl(shopDetail.value?.logoImage) || '/static/images/logo1.png',
}))

const getImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return baseURL + url
}

const formatHours = (detail: any) => {
  if (!detail.openTime1) return ''
  const fmt = (time: string) => (time ? time.substring(0, 5) : '')
  let text = `${fmt(detail.openTime1)}-${fmt(detail.closeTime1)}`
  if (detail.openTime2 && detail.closeTime2) {
    text += ` ${fmt(detail.openTime2)}-${fmt(detail.closeTime2)}`
  }
  return text
}

const searchPlaceholder = computed(() =>
  isWeightSelection.value ? '搜索这家店想选的食材' : '搜索这家店想吃的菜'
)

const selectedBroth = computed(() =>
  brothOptions.value.find((item) => item.id === selectedBrothId.value) || null
)
const hasBrothExtraPrice = computed(() =>
  brothOptions.value.some((item) => Number(item.extraPrice || 0) > 0)
)

const flatIngredients = computed(() =>
  weightMenuData.value.flatMap((category: any) => category.ingredients || [])
)

const filteredIngredientCategories = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  if (isWeightSelection.value) {
    if (!keyword) return weightMenuData.value
    return weightMenuData.value
      .map((category: any) => {
        const ingredients = Array.isArray(category.ingredients)
          ? category.ingredients.filter((ingredient: any) =>
              String(ingredient.name || '').toLowerCase().includes(keyword)
            )
          : []

        return {
          ...category,
          ingredients,
        }
      })
      .filter((category: any) => category.ingredients.length > 0)
  }

  if (!keyword) return menuData.value
  return menuData.value
    .map((category) => {
      const dishes = Array.isArray(category.dishes)
        ? category.dishes.filter((dish: any) =>
            String(dish.name || '').toLowerCase().includes(keyword)
          )
        : []

      return {
        ...category,
        dishes,
      }
    })
    .filter((category) => category.dishes.length > 0)
})

const displaySections = computed(() => {
  if (isWeightSelection.value) {
    const hasKeyword = !!searchKeyword.value.trim()
    const ingredientSections = filteredIngredientCategories.value.map((category: any) => ({
      ...category,
      sectionKey: `ingredient-${category.categoryId}`,
      sectionType: 'ingredient',
    }))

    if (hasKeyword) {
      return ingredientSections
    }

    return [
      {
        sectionKey: 'broth',
        sectionType: 'broth',
        categoryId: 'broth',
        categoryName: '汤底',
      },
      ...ingredientSections,
    ]
  }

  return filteredIngredientCategories.value.map((category: any) => ({
    ...category,
    sectionKey: `dish-${category.categoryId}`,
    sectionType: 'dish',
  }))
})

const cartQuantityMap = computed<Record<number, number>>(() =>
  cartStore.items.reduce((result, item) => {
    result[item.dishId] = (result[item.dishId] || 0) + item.quantity
    return result
  }, {} as Record<number, number>)
)

const normalizeDishOptionGroups = (dish: any) => {
  const groups = Array.isArray(dish?.optionGroups) ? dish.optionGroups : []
  return groups
    .map((group: any, groupIndex: number) => {
      const optionGroupId = Number(group?.optionGroupId ?? group?.id ?? 0)
      const values = Array.isArray(group?.values) ? group.values : []
      return {
        optionGroupId,
        name: group?.name || group?.groupName || '',
        required: Number(group?.required ?? 1) === 0 ? 0 : 1,
        sort: Number(group?.sort ?? groupIndex + 1),
        values: values
          .map((value: any, valueIndex: number) => ({
            optionValueId: Number(value?.optionValueId ?? value?.id ?? 0),
            name: value?.name || '',
            extraPrice: Number(value?.extraPrice || 0),
            isDefault: Number(value?.isDefault || 0) === 1 ? 1 : 0,
            sort: Number(value?.sort ?? valueIndex + 1),
          }))
          .filter((value: any) => value.optionValueId > 0),
      }
    })
    .filter((group: any) => group.optionGroupId > 0 && group.values.length > 0)
}

const hasDishOptions = (dish: any) => {
  const optionGroups = normalizeDishOptionGroups(dish)
  const optionEnabled = Number(dish?.optionEnabled ?? (optionGroups.length > 0 ? 1 : 0))
  return optionEnabled === 1 && optionGroups.length > 0
}

const currentOptionGroups = computed(() => {
  const groups = currentOptionDish.value?.optionGroups
  return Array.isArray(groups) ? groups : []
})

const currentSelectedOptionList = computed(() =>
  currentOptionGroups.value
    .map((group: any) => {
      const selectedValueId = currentOptionSelections.value[group.optionGroupId]
      const values = Array.isArray(group.values) ? group.values : []
      const selectedValue = values.find((value: any) => value.optionValueId === selectedValueId)
      if (!selectedValue) return null
      return {
        optionGroupId: Number(group.optionGroupId),
        optionValueId: Number(selectedValue.optionValueId),
        groupName: group.name,
        valueName: selectedValue.name,
        extraPrice: Number(selectedValue.extraPrice || 0),
      }
    })
    .filter(Boolean) as Array<{
      optionGroupId: number
      optionValueId: number
      groupName: string
      valueName: string
      extraPrice: number
    }>
)

const currentOptionDishPrice = computed(() => {
  const basePrice = Number(currentOptionDish.value?.price || 0)
  const extraPrice = currentSelectedOptionList.value.reduce((sum, item) => sum + Number(item.extraPrice || 0), 0)
  return Number((basePrice + extraPrice).toFixed(2))
})

const currentOptionSummary = computed(() =>
  currentSelectedOptionList.value.map((item) => item.valueName).join(' / ')
)

const selectedWeightItems = computed(() =>
  flatIngredients.value
    .filter((ingredient: any) => (weightQuantities.value[ingredient.id] || 0) > 0)
    .map((ingredient: any) => {
      const quantity = weightQuantities.value[ingredient.id] || 0
      const referenceWeightG = Number(ingredient.referenceWeightG || 0)
      return {
        ingredientId: ingredient.id,
        name: ingredient.name,
        image: ingredient.image || '',
        unitLabel: ingredient.unitLabel || '份',
        quantity,
        referenceWeightG,
        estimatedWeightG: quantity * referenceWeightG,
      }
    })
)

const selectedWeightTotalQuantity = computed(() =>
  selectedWeightItems.value.reduce((sum, item) => sum + item.quantity, 0)
)
const hasPopupSelection = computed(() =>
  isWeightSelection.value ? selectedWeightItems.value.length > 0 : cartStore.totalQuantity > 0
)

const estimatedWeightG = computed(() =>
  selectedWeightItems.value.reduce((sum, item) => sum + item.estimatedWeightG, 0)
)

const estimatedAmount = computed(() => {
  if (!weightPricePer500g.value || estimatedWeightG.value <= 0) return 0
  const brothExtra = Number(selectedBroth.value?.extraPrice || 0)
  return Number(((estimatedWeightG.value / 500) * weightPricePer500g.value + brothExtra).toFixed(2))
})

const amountRangeText = computed(() => {
  if (!estimatedAmount.value) return '0-0元'
  const spread = estimatedAmount.value < 15
    ? 2
    : estimatedAmount.value < 25
      ? 3
      : Math.max(3, estimatedAmount.value * 0.12)
  const min = Math.max(0, Math.floor(estimatedAmount.value - spread))
  const max = Math.ceil(estimatedAmount.value + spread)
  return `${min}-${max}元`
})

const displayWeightPriceText = computed(() => {
  if (!estimatedAmount.value) return '￥0'
  return `￥${amountRangeText.value.replace(/元/g, '')}`
})

const canSubmitWeight = computed(() =>
  !!selectedBrothId.value &&
  selectedWeightItems.value.length > 0 &&
  (minimumOrderWeightG.value <= 0 || estimatedWeightG.value >= minimumOrderWeightG.value)
)
const remainingWeightG = computed(() =>
  minimumOrderWeightG.value > estimatedWeightG.value
    ? minimumOrderWeightG.value - estimatedWeightG.value
    : 0
)
const weightConfirmButtonText = computed(() => {
  if (minimumOrderWeightG.value > 0 && selectedWeightItems.value.length > 0 && remainingWeightG.value > 0) {
    return `还差${remainingWeightG.value}g`
  }
  return '去确认'
})

const emptyTitle = computed(() => {
  if (searchKeyword.value) {
    return isWeightSelection.value ? '没有找到相关食材' : '没有找到相关菜品'
  }
  return isWeightSelection.value ? '当前还没有上架食材' : '当前还没有上架菜品'
})

const emptyDesc = computed(() => {
  if (searchKeyword.value) {
    return '换个关键词试试，比如名称或口味'
  }
  return isWeightSelection.value ? '商户配置完成后会在这里展示食材' : '商户配置完成后会在这里展示菜品'
})

const updateCategorySectionTops = async () => {
  await nextTick()

  if (!instance?.proxy || !displaySections.value.length) {
    categorySectionTops.value = []
    return
  }

  const query = uni.createSelectorQuery().in(instance.proxy)
  query.selectAll('.cat-section').boundingClientRect((rects) => {
    if (!Array.isArray(rects) || !rects.length) {
      categorySectionTops.value = []
      return
    }

    const firstTop = rects[0].top
    categorySectionTops.value = rects.map((rect: any) => rect.top - firstTop)
  }).exec()
}

const scheduleUpdateCategorySectionTops = (delay = SECTION_MEASURE_DEBOUNCE_MS) => {
  if (sectionMeasureTimer) {
    clearTimeout(sectionMeasureTimer)
  }

  sectionMeasureTimer = setTimeout(() => {
    sectionMeasureTimer = null
    updateCategorySectionTops()
  }, delay)
}

const fetchShopDetail = async () => {
  const res = await http<any>({
    url: `/api/client/shops/${shopId.value}`,
  })
  shopDetail.value = res.data
}

const fetchMenu = async () => {
  const res = await http<any[]>({
    url: `/api/client/shops/${shopId.value}/dishes`,
  })
  menuData.value = res.data || []
  weightMenuData.value = []
  brothOptions.value = []
  selectedBrothId.value = null
  weightQuantities.value = {}
  scheduleUpdateCategorySectionTops(0)
}

const fetchWeightSelectionConfig = async () => {
  const res = await http<any>({
    url: `/api/client/shops/${shopId.value}/weight-selection-config`,
  })
  const config = res.data || {}
  brothOptions.value = config.brothOptions || []
  weightMenuData.value = config.categories || []
  menuData.value = []
  if (brothOptions.value.length > 0 && !selectedBroth.value) {
    selectedBrothId.value = brothOptions.value[0].id
  }
  scheduleUpdateCategorySectionTops(0)
}

const fetchPageData = async () => {
  await fetchShopDetail()
  if (isWeightSelection.value) {
    await fetchWeightSelectionConfig()
  } else {
    await fetchMenu()
  }
}

const getCartQuantity = (dishId: number) => {
  return cartQuantityMap.value[dishId] || 0
}

const getWeightQuantity = (ingredientId: number) => weightQuantities.value[ingredientId] || 0

const goBack = () => {
  smartNavigateBack({ fallbackTab: 'pages/index/index' })
}

const getShopStatusTip = (shop: any) => {
  if (!shop) return ''
  if (shop.displayStatus === 'service_paused') return '商户服务暂停，暂时无法下单'
  if (shop.displayStatus === 'reservable') return '当前可预约营业时段内订单'
  if (shop.displayStatus === 'paused') return '店铺已暂停接单'
  if (shop.displayStatus === 'closed') return shop.displayStatusText || '休息中'
  return shop.displayStatusText || ''
}

const clearSearch = () => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
    searchDebounceTimer = null
  }
  searchInputKeyword.value = ''
  searchKeyword.value = ''
}

const formatBrothPrice = (extraPrice: number | string) => {
  const amount = Number(extraPrice || 0)
  if (amount > 0) {
    return `+￥${amount.toFixed(amount % 1 === 0 ? 0 : 2)}`
  }
  return '不加价'
}

const getBrothThemeClass = (name: string) => {
  const text = String(name || '')
  if (text.includes('番茄') || text.includes('酸甜')) return 'broth-card-cover--tomato'
  if (text.includes('麻辣') || text.includes('辣')) return 'broth-card-cover--spicy'
  if (text.includes('骨汤') || text.includes('原汤') || text.includes('清汤')) return 'broth-card-cover--bone'
  return 'broth-card-cover--default'
}

const getBrothCoverCaption = (name: string) => {
  const text = String(name || '')
  if (text.includes('番茄') || text.includes('酸甜')) return '酸甜开胃'
  if (text.includes('麻辣') || text.includes('辣')) return '香辣过瘾'
  if (text.includes('骨汤') || text.includes('原汤') || text.includes('清汤')) return '浓香鲜汤'
  return '风味汤底'
}

const selectCategory = async (index: number) => {
  activeIndex.value = index
  dishScrollIntoView.value = ''
  await nextTick()
  dishScrollIntoView.value = `cat-${index}`
}

const syncActiveIndexByScrollTop = (scrollTop: number) => {
  if (!categorySectionTops.value.length) return

  const threshold = 24
  let currentIndex = 0

  for (let i = 0; i < categorySectionTops.value.length; i += 1) {
    if (scrollTop + threshold >= categorySectionTops.value[i]) {
      currentIndex = i
    } else {
      break
    }
  }

  if (currentIndex !== activeIndex.value) {
    activeIndex.value = currentIndex
  }
}

const handleDishScroll = (event: any) => {
  latestDishScrollTop = event?.detail?.scrollTop ?? 0

  if (dishScrollThrottleTimer) {
    return
  }

  dishScrollThrottleTimer = setTimeout(() => {
    dishScrollThrottleTimer = null
    syncActiveIndexByScrollTop(latestDishScrollTop)
  }, SCROLL_THROTTLE_MS)
}

const ensureShopAvailable = () => {
  if (shopDetail.value.billingServiceAvailable === false) {
    uni.showToast({ title: '商户服务暂停，暂时无法下单', icon: 'none' })
    return false
  }
  if (shopDetail.value.status !== 0) return true

  uni.showToast({
    title: '店铺已暂停接单',
    icon: 'none'
  })
  return false
}

const appendCartItem = (dish: any) => {
  const dishId = Number(dish.id ?? dish.dishId)
  if (!dishId) return

  const result = cartStore.addItem(dishId, {
    cartItemId: `dish-${dishId}`,
    dishId,
    name: dish.name,
    price: Number(dish.price),
    image: dish.image || '',
    quantity: 1,
    optionSummary: '',
    selectedOptions: []
  }, shopId.value)

  if (!result.success) {
    uni.showToast({
      title: result.message || '已达到数量上限',
      icon: 'none'
    })
  }
}

const addToCart = (dish: any) => {
  if (!ensureShopAvailable()) return
  if (hasDishOptions(dish)) {
    openDishOptionPopup(dish)
    return
  }
  appendCartItem(dish)
}

const removeFromCart = (dish: any) => {
  cartStore.removeItem(`dish-${Number(dish.id ?? dish.dishId)}`)
}

const increaseCartItem = (item: any) => {
  if (!ensureShopAvailable()) return
  const result = cartStore.addItem(Number(item.dishId), {
    cartItemId: item.cartItemId,
    dishId: Number(item.dishId),
    name: item.name,
    price: Number(item.price),
    image: item.image || '',
    quantity: 1,
    optionSummary: item.optionSummary || '',
    selectedOptions: item.selectedOptions || []
  }, shopId.value)
  if (!result.success) {
    uni.showToast({
      title: result.message || '已达到数量上限',
      icon: 'none'
    })
  }
}

const removeCartItem = (item: any) => {
  cartStore.removeItem(item.cartItemId)
}

const toggleCartPopup = () => {
  if (!hasPopupSelection.value) return
  cartPopupVisible.value = !cartPopupVisible.value
}

const closeCartPopup = () => {
  cartPopupVisible.value = false
}

const clearCurrentCart = () => {
  cartStore.clearCart()
  cartPopupVisible.value = false
  uni.showToast({
    title: '购物车已清空',
    icon: 'none'
  })
}

const buildDefaultOptionSelections = (dish: any) => {
  const groups = normalizeDishOptionGroups(dish)
  const nextSelections: Record<number, number | null> = {}
  groups.forEach((group: any) => {
    const values = Array.isArray(group.values) ? group.values : []
    const defaultValue = values.find((value: any) => Number(value.isDefault || 0) === 1)
    if (defaultValue) {
      nextSelections[group.optionGroupId] = defaultValue.optionValueId
      return
    }
    if (group.required === 1 && values.length > 0) {
      nextSelections[group.optionGroupId] = values[0].optionValueId
      return
    }
    nextSelections[group.optionGroupId] = null
  })
  return nextSelections
}

const openDishOptionPopup = (dish: any) => {
  if (!ensureShopAvailable()) return
  const optionGroups = normalizeDishOptionGroups(dish)
  if (optionGroups.length === 0) {
    uni.showToast({
      title: '规格数据加载失败，请稍后重试',
      icon: 'none'
    })
    return
  }
  currentOptionDish.value = {
    ...dish,
    optionGroups,
  }
  currentOptionSelections.value = buildDefaultOptionSelections(dish)
  showDishOptionPopup.value = true
}

const closeDishOptionPopup = () => {
  showDishOptionPopup.value = false
  currentOptionDish.value = null
  currentOptionSelections.value = {}
}

const selectDishOptionValue = (group: any, value: any) => {
  const groupId = Number(group.optionGroupId)
  const valueId = Number(value.optionValueId)
  const currentValueId = currentOptionSelections.value[groupId]
  const isOptional = Number(group.required || 0) === 0
  currentOptionSelections.value = {
    ...currentOptionSelections.value,
    [groupId]: isOptional && currentValueId === valueId ? null : valueId,
  }
}

const confirmDishOptionSelection = () => {
  if (!currentOptionDish.value) return

  const missingGroup = currentOptionGroups.value.find((group: any) =>
    Number(group.required || 0) === 1 && !currentOptionSelections.value[group.optionGroupId]
  )
  if (missingGroup) {
    uni.showToast({
      title: `请选择${missingGroup.name}`,
      icon: 'none'
    })
    return
  }

  const dishId = Number(currentOptionDish.value.id)
  const selectedOptions = currentSelectedOptionList.value
  const optionKey = selectedOptions.map((item) => item.optionValueId).join('-') || 'default'
  const result = cartStore.addItem(dishId, {
    cartItemId: `dish-${dishId}-${optionKey}`,
    dishId,
    name: currentOptionDish.value.name,
    price: currentOptionDishPrice.value,
    image: currentOptionDish.value.image || '',
    quantity: 1,
    optionSummary: currentOptionSummary.value,
    selectedOptions,
  }, shopId.value)

  if (!result.success) {
    uni.showToast({
      title: result.message || '已达到数量上限',
      icon: 'none'
    })
    return
  }

  closeDishOptionPopup()
}

const selectBroth = (brothId: number) => {
  selectedBrothId.value = brothId
}

const changeWeightQuantity = (ingredient: any, delta: number) => {
  if (!ensureShopAvailable()) return
  const ingredientId = Number(ingredient.id)
  if (!ingredientId) return
  const current = weightQuantities.value[ingredientId] || 0
  const next = Math.max(0, current + delta)
  weightQuantities.value = {
    ...weightQuantities.value,
    [ingredientId]: next,
  }
}

const clearWeightSelection = () => {
  weightQuantities.value = {}
  cartPopupVisible.value = false
  if (brothOptions.value.length > 0) {
    selectedBrothId.value = brothOptions.value[0].id
  }
  uni.showToast({
    title: '已清空所选食材',
    icon: 'none'
  })
}

const increaseWeightPopupItem = (item: any) => {
  changeWeightQuantity({ id: item.ingredientId }, 1)
}

const removeWeightPopupItem = (item: any) => {
  changeWeightQuantity({ id: item.ingredientId }, -1)
}

const goToFixedDishConfirm = () => {
  cartPopupVisible.value = false
  safeNavigateTo({
    url: '/pages/confirm/confirm'
  })
}

const goToWeightConfirm = () => {
  if (!selectedBrothId.value) {
    uni.showToast({
      title: '请先选择汤底',
      icon: 'none'
    })
    return
  }

  if (!selectedWeightItems.value.length) {
    uni.showToast({
      title: '请先选择食材',
      icon: 'none'
    })
    return
  }

  if (minimumOrderWeightG.value > 0 && estimatedWeightG.value < minimumOrderWeightG.value) {
    uni.showToast({
      title: `至少选择${minimumOrderWeightG.value}g`,
      icon: 'none'
    })
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
    items: selectedWeightItems.value,
  })

  safeNavigateTo({
    url: `/pages/confirm/confirm?mode=weight_selection&shopId=${shopId.value}`
  })
}

const goToConfirm = () => {
  if (!ensureShopAvailable()) return

  if (!userStore.isLogin) {
    uni.showToast({
      title: '请先登录',
      icon: 'none'
    })
    return
  }

  if (isWeightSelection.value) {
    goToWeightConfirm()
    return
  }

  goToFixedDishConfirm()
}

onLoad(async (options) => {
  shopId.value = Number(options.shopId || 0)
  cartStore.setShopId(shopId.value)

  showPageLoading()
  try {
    await fetchPageData()
  } catch (error) {
    console.error('页面数据加载失败:', error)
    uni.showToast({
      title: '数据加载失败，请重试',
      icon: 'none'
    })
  } finally {
    hidePageLoading(true)
  }
})

onUnmounted(() => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  if (sectionMeasureTimer) {
    clearTimeout(sectionMeasureTimer)
  }
  if (dishScrollThrottleTimer) {
    clearTimeout(dishScrollThrottleTimer)
  }
})

watch(
  () => cartStore.totalQuantity,
  (quantity) => {
    if (!quantity && !isWeightSelection.value) {
      cartPopupVisible.value = false
    }
  }
)

watch(
  () => selectedWeightItems.value.length,
  (length) => {
    if (!length && isWeightSelection.value) {
      cartPopupVisible.value = false
    }
  }
)

watch(
  () => isWeightSelection.value,
  (value) => {
    if (value) {
      cartPopupVisible.value = false
    }
  }
)

watch(
  () => displaySections.value.length,
  (length) => {
    if (length) {
      scheduleUpdateCategorySectionTops()
      if (activeIndex.value >= length) {
        activeIndex.value = 0
      }
    } else {
      categorySectionTops.value = []
      activeIndex.value = 0
    }
  }
)

watch(
  () => searchInputKeyword.value,
  (value) => {
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer)
    }

    searchDebounceTimer = setTimeout(() => {
      searchDebounceTimer = null
      searchKeyword.value = value
    }, SEARCH_DEBOUNCE_MS)
  }
)

watch(
  () => searchKeyword.value,
  () => {
    activeIndex.value = 0
    dishScrollIntoView.value = ''
    scheduleUpdateCategorySectionTops()
  }
)
</script>

<style lang="scss">
.menu-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f8f9fa;
}

.header-section {
  position: relative;
  width: 100%;
  height: calc(380rpx + var(--status-bar-height));
  background-color: #fff;
}

.header-bg-wrapper {
  position: relative;
  width: 100%;
  height: calc(300rpx + var(--status-bar-height));

  .header-bg {
    width: 100%;
    height: 100%;
    background-color: #ddd;

    &.default-header-bg {
      background: linear-gradient(135deg, #e0e0e0 0%, #f5f5f5 100%);
    }
  }

  .header-mask {
    position: absolute;
    inset: 0;
    background: linear-gradient(to bottom, rgba(42, 139, 255, 0.82), rgba(42, 139, 255, 0));
  }
}

.header-content {
  position: absolute;
  top: calc(var(--status-bar-height) + 10rpx);
  left: 20rpx;
  z-index: 20;
}

.shop-card-wrapper {
  position: absolute;
  left: 30rpx;
  right: 30rpx;
  bottom: 20rpx;
  z-index: 10;
}

.shop-card {
  min-height: 160rpx;
  background-color: #fff;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);

  .shop-logo {
    width: 100rpx;
    height: 100rpx;
    margin-right: 24rpx;
    border-radius: 50%;
    background-color: #f5f5f5;
    border: 2rpx solid #fff;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);

    &.default-logo {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .shop-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .shop-title-row {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 10rpx;
    margin-bottom: 8rpx;
  }

  .shop-name {
    flex: 0 1 auto;
    min-width: 0;
    font-size: 34rpx;
    font-weight: 700;
    color: #333;
  }

  .shop-mode-badge {
    padding: 6rpx 14rpx;
    border-radius: 999rpx;
    font-size: 20rpx;
    font-weight: 600;
    color: #2a8bff;
    background: rgba(42, 139, 255, 0.1);
  }

  .shop-detail-row {
    display: flex;
    align-items: center;
    gap: 4rpx;
  }

  .shop-address {
    margin-left: 4rpx;
    font-size: 24rpx;
    color: #666;
  }

  .shop-status-tip {
    flex-shrink: 0;
    margin-top: 2rpx;
    padding: 6rpx 16rpx;
    border-radius: 999rpx;
    font-size: 22rpx;
    font-weight: 500;
    line-height: 1.2;
  }

  .shop-status-tip--open {
    background: #e6f7ff;
    color: #1890ff;
  }

  .shop-status-tip--reservable {
    background: #fff4e5;
    color: #f08c2e;
  }

  .shop-status-tip--closed,
  .shop-status-tip--paused {
    background: #f5f5f5;
    color: #999;
  }
}

.menu-content {
  position: relative;
  flex: 1;
  display: flex;
  overflow: hidden;
  background-color: #fff;
}

.search-section {
  padding: 8rpx 24rpx;
  background: #fff;
}

.search-box {
  height: 76rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 0 22rpx;
  border-radius: 38rpx;
  background: #f4f7fb;
  border: 2rpx solid rgba(42, 139, 255, 0.06);
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: #334155;
}

.search-input-placeholder {
  color: #9aa5b1;
}

.search-clear {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.category-sidebar {
  width: 150rpx;
  flex: 0 0 150rpx;
  min-width: 150rpx;
  max-width: 150rpx;
  height: 100%;
  box-sizing: border-box;
  background-color: #f5f7fa;
}

.category-item {
  width: 100%;
  padding: 30rpx 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  position: relative;
  transition: all 0.2s;
  box-sizing: border-box;

  .category-name {
    font-size: 26rpx;
    color: #666;
    line-height: 1.4;
    word-break: break-all;
  }

  &.active {
    background-color: #fff;
    font-weight: 700;

    .category-name {
      font-size: 28rpx;
      color: #333;
    }

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 30rpx;
      bottom: 30rpx;
      width: 6rpx;
      background-color: #2a8bff;
      border-top-right-radius: 4rpx;
      border-bottom-right-radius: 4rpx;
    }
  }
}

.sidebar-placeholder {
  height: 120rpx;
}

.dish-scroll {
  flex: 1;
  height: 100%;
  background-color: #fff;
}

.dish-content {
  padding: 20rpx;
  padding-bottom: 0;
}

.weight-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 24rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #eef7ff 0%, #fff9f2 100%);
  box-shadow: 0 12rpx 26rpx rgba(42, 139, 255, 0.08);
}

.weight-banner-main {
  flex: 1;
  min-width: 0;
}

.weight-banner-desc {
  display: block;
  font-size: 26rpx;
  line-height: 1.6;
  color: #334155;
}

.weight-banner-tip {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #7c8ba1;
}

.weight-banner-price {
  min-width: 156rpx;
  padding: 18rpx 20rpx;
  border-radius: 20rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.72);
}

.weight-banner-price-label {
  display: block;
  font-size: 20rpx;
  color: #5f7aa3;
}

.weight-banner-price-value {
  display: block;
  margin-top: 6rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #2a8bff;
}

.broth-empty-tip {
  padding: 28rpx 24rpx;
  border-radius: 20rpx;
  background: #f8fafc;
  font-size: 24rpx;
  color: #94a3b8;
}

.broth-list-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
  border: 2rpx solid transparent;

  &.active {
    border-color: rgba(42, 139, 255, 0.22);
    box-shadow: 0 8rpx 18rpx rgba(42, 139, 255, 0.1);
  }
}

.broth-list-card__image-wrapper {
  position: relative;
  width: 180rpx;
  height: 180rpx;
  overflow: hidden;
  flex-shrink: 0;
  border-radius: 12rpx;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.06), rgba(15, 23, 42, 0.3));
  }
}

.broth-list-card__image {
  width: 100%;
  height: 100%;
}

.broth-card-cover--bone {
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
}

.broth-card-cover--tomato {
  background: linear-gradient(135deg, #f85032 0%, #e73827 100%);
}

.broth-card-cover--spicy {
  background: linear-gradient(135deg, #f12711 0%, #f5af19 100%);
}

.broth-card-cover--default {
  background: linear-gradient(135deg, #5b86e5 0%, #36d1dc 100%);
}

.broth-list-card__caption {
  position: absolute;
  left: 16rpx;
  right: 16rpx;
  bottom: 16rpx;
  z-index: 1;
  font-size: 24rpx;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.94);
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.22);
}

.broth-list-card__content {
  flex: 1;
  min-width: 0;
}

.broth-list-card__name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.broth-list-card__name {
  overflow: hidden;
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.broth-list-card__selected {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  color: #2a8bff;
  background: rgba(42, 139, 255, 0.1);
}

.broth-list-card__price {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #64748b;
}

.broth-list-card__check {
  flex-shrink: 0;
}

.search-empty {
  min-height: 420rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  color: #9aa5b1;
}

.search-empty--compact {
  min-height: 280rpx;
}

.search-empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #64748b;
}

.search-empty-desc {
  font-size: 24rpx;
  color: #9aa5b1;
}

.cat-section {
  margin-bottom: 30rpx;
}

.cat-title {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 10rpx 0;
  margin-bottom: 20rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #333;
  background-color: #fff;
}

.ingredient-card {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}

.ingredient-image-wrapper {
  width: 180rpx;
  height: 180rpx;
  flex-shrink: 0;
  border-radius: 12rpx;
  overflow: hidden;
}

.ingredient-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.ingredient-thumb {
  width: 100%;
  height: 100%;
  background: #eef4fb;

  &.ingredient-thumb--fallback {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.ingredient-meta {
  flex: 1;
  min-width: 0;
}

.ingredient-name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.ingredient-name {
  overflow: hidden;
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ingredient-selected-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  color: #2a8bff;
  background: rgba(42, 139, 255, 0.1);
}

.ingredient-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.ingredient-bottom {
  margin-top: 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.ingredient-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ff7b2c;
}

.ingredient-stepper {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.dish-bottom-space {
  height: 160rpx;

  &.dish-bottom-space--weight {
    height: 220rpx;
  }
}

.cart-mask {
  position: fixed;
  inset: 0;
  z-index: 88;
  background: rgba(9, 18, 32, 0.22);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;

  &.visible {
    opacity: 1;
    pointer-events: auto;
  }
}

.cart-popup {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(126rpx + env(safe-area-inset-bottom));
  z-index: 90;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 28rpx 28rpx 20rpx 20rpx;
  box-shadow: 0 20rpx 50rpx rgba(16, 35, 63, 0.14);
  transform: translateY(24rpx);
  opacity: 0;
  pointer-events: none;
  transition: transform 0.22s ease, opacity 0.22s ease;

  &.open {
    transform: translateY(0);
    opacity: 1;
    pointer-events: auto;
  }

  .cart-popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 28rpx 20rpx;
    border-bottom: 1rpx solid rgba(42, 139, 255, 0.08);
  }

  .cart-popup-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1f2a37;
  }

  .cart-clear {
    display: flex;
    align-items: center;
    gap: 8rpx;
    padding: 10rpx 16rpx;
    border-radius: 999rpx;
    background: #f4f6f8;
  }

  .cart-clear-text {
    font-size: 22rpx;
    color: #7b8794;
  }

  .cart-popup-list {
    height: 40vh;
    max-height: 680rpx;
  }

  .cart-popup-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
    padding: 28rpx 32rpx;

    &:not(:last-child) {
      border-bottom: 1rpx solid #edf2f7;
    }
  }

  .cart-popup-main {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 22rpx;
  }

  .cart-popup-thumb {
    width: 104rpx;
    height: 104rpx;
    flex-shrink: 0;
    border-radius: 20rpx;
    background: #eef4fb;

    &.cart-popup-thumb--fallback {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .cart-popup-meta {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 10rpx;
  }

  .cart-popup-name {
    overflow: hidden;
    font-size: 32rpx;
    font-weight: 600;
    color: #1f2a37;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .cart-popup-unit {
    font-size: 26rpx;
    color: #97a1af;
  }

  .cart-popup-option {
    font-size: 22rpx;
    color: #2a8bff;
  }

  .cart-popup-action {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 18rpx;
  }

  .cart-popup-subtotal {
    font-size: 32rpx;
    font-weight: 600;
    color: #ff7b2c;
  }

  .cart-popup-stepper {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }
}

.weight-popup-summary {
  margin: 0 24rpx 8rpx;
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f7ff 100%);
}

.weight-popup-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &:not(:last-child) {
    margin-bottom: 10rpx;
  }
}

.weight-popup-summary-label {
  font-size: 24rpx;
  color: #7b8794;
}

.weight-popup-summary-value {
  font-size: 26rpx;
  font-weight: 600;
  color: #334155;
}

.stepper-btn {
  width: 52rpx;
  height: 52rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 26rpx;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    border-radius: 999rpx;
    transform: translate(-50%, -50%);
  }

  &.stepper-btn--minus {
    background: #eef5ff;
    border: 1rpx solid rgba(42, 139, 255, 0.22);

    &::before {
      width: 20rpx;
      height: 4rpx;
      background: #2a8bff;
    }
  }

  &.stepper-btn--plus {
    background: linear-gradient(135deg, #2a8bff, #56a6ff);
    box-shadow: 0 8rpx 16rpx rgba(42, 139, 255, 0.18);

    &::before,
    &::after {
      background: #fff;
    }

    &::before {
      width: 20rpx;
      height: 4rpx;
    }

    &::after {
      width: 4rpx;
      height: 20rpx;
    }
  }
}

.stepper-count {
  min-width: 36rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #334155;
}

.cart-bar,
.weight-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  background-color: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.cart-bar {
  height: calc(110rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  padding: 0 30rpx env(safe-area-inset-bottom);
}

.cart-trigger {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.cart-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.cart-icon-wrapper {
  position: relative;
  width: 60rpx;
  height: 60rpx;
  margin-right: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 30rpx;
  background: linear-gradient(180deg, rgba(42, 139, 255, 0.14), rgba(42, 139, 255, 0.04));
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &.active {
    transform: translateY(-4rpx);
    box-shadow: 0 10rpx 20rpx rgba(42, 139, 255, 0.16);
  }

  .badge {
    position: absolute;
    top: -6rpx;
    right: -6rpx;
    height: 32rpx;
    line-height: 32rpx;
    padding: 0 10rpx;
    border-radius: 16rpx;
    z-index: 1;
    font-size: 20rpx;
    color: #fff;
    background-color: #ff4d4f;
  }
}

.cart-summary {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.cart-summary-row {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 28rpx;
  color: #333;
}

.total-price {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff7b2c;
}

.weight-total-text {
  margin-left: 14rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #64748b;
}

.cart-hint {
  font-size: 22rpx;
  color: #8a94a6;
}

.cart-arrow {
  margin-right: 24rpx;
  transition: transform 0.2s ease;

  &.open {
    transform: rotate(180deg);
  }
}

.weight-bar {
  padding: 20rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
}

.weight-bar-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.weight-bar-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.weight-bar-actions .btn-submit {
  width: 220rpx;
}

.btn-submit {
  width: 200rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #fff;
  background-color: #ff7b2c;
  box-shadow: 0 4rpx 12rpx rgba(255, 123, 44, 0.3);

  &:active {
    transform: scale(0.98);
  }

  &.btn-submit--disabled {
    background: #cbd5e1;
    box-shadow: none;
  }
}

.option-popup-mask {
  position: fixed;
  inset: 0;
  z-index: 110;
  background: rgba(15, 23, 42, 0.42);
}

.option-popup-wrapper {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 28rpx;
  padding: 40rpx;
  box-sizing: border-box;
}

.option-popup {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 670rpx;
  max-height: 72vh;
  border-radius: 28rpx;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 20rpx 50rpx rgba(15, 23, 42, 0.2);
}

.option-popup__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  padding: 28rpx 28rpx 20rpx;
  border-bottom: 1rpx solid #eef2f7;
}

.option-popup__title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.option-popup__subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.option-popup__body {
  max-height: 50vh;
  padding: 24rpx 24rpx 0;
  box-sizing: border-box;
}

.option-group-card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  background: #f8fbff;
}

.option-group-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.option-group-card__title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1e293b;
}

.option-group-card__tag {
  flex-shrink: 0;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(42, 139, 255, 0.08);
  color: #2a8bff;
  font-size: 20rpx;
}

.option-group-card__values {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.option-value-chip {
  min-width: 180rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #fff;
  border: 2rpx solid transparent;
  box-sizing: border-box;
}

.option-value-chip.active {
  border-color: #2a8bff;
  background: #eef6ff;
}

.option-value-chip__name {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #334155;
}

.option-value-chip__price {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.option-popup__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx 24rpx calc(22rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eef2f7;
  background: #fff;
}

.option-popup__price-box {
  flex: 1;
  min-width: 0;
}

.option-popup__price-label {
  display: block;
  font-size: 22rpx;
  color: #94a3b8;
}

.option-popup__price-value {
  display: block;
  margin-top: 8rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #ff7b2c;
}

.option-popup__summary {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #64748b;
}

.option-popup__confirm {
  width: 220rpx;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  border-radius: 42rpx;
  background: linear-gradient(135deg, #ff8a3d, #ff6b1a);
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
  box-shadow: 0 10rpx 18rpx rgba(255, 123, 44, 0.24);
}

.option-popup__bottom-close {
  width: 76rpx;
  height: 76rpx;
  border-radius: 38rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.62);
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.2);
}
</style>
