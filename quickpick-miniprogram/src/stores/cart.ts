import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const SINGLE_DISH_QUANTITY_LIMIT = 5
export const TOTAL_DISH_QUANTITY_LIMIT = 20

export interface CartItem {
  cartItemId: string
  dishId: number
  name: string
  price: number
  image: string
  quantity: number
  remark?: string
  optionSummary?: string
  selectedOptions?: Array<{
    optionGroupId: number
    optionValueId: number
    groupName: string
    valueName: string
    extraPrice: number
  }>
}

export interface AddCartResult {
  success: boolean
  message?: string
}

export const useCartStore = defineStore('cart', () => {
  // Multi-cart support: Map shopId -> CartItem[]
  const carts = ref<Record<number, CartItem[]>>({})
  
  // Current active shop context (set by page)
  const currentShopId = ref<number | null>(null)

  // Get items for current shop context
  const items = computed(() => {
    if (!currentShopId.value) return []
    return carts.value[currentShopId.value] || []
  })
  
  // Get shopId for compatibility (returns current active shop if has items)
  const shopId = computed(() => currentShopId.value)

  const totalQuantity = computed(() => {
    return items.value.reduce((total, item) => total + item.quantity, 0)
  })

  const totalAmount = computed(() => {
    return items.value.reduce((total, item) => total + item.price * item.quantity, 0)
  })

  // Action to set current shop context (called by page on load)
  const setShopId = (id: number) => {
    currentShopId.value = id
    // Initialize cart if not exists
    if (!carts.value[id]) {
      carts.value[id] = []
    }
  }

  const addItem = (id: number, item: CartItem, sId: number): AddCartResult => {
    // Ensure we are adding to the correct shop cart
    if (!carts.value[sId]) {
      carts.value[sId] = []
    }
    
    // If current context is not this shop, switch context?
    // Usually addItem is called from within the shop page, so context should match.
    // But to be safe:
    if (currentShopId.value !== sId) {
       currentShopId.value = sId
    }

    const shopItems = carts.value[sId]
    const existingItem = shopItems.find((i) => i.cartItemId === item.cartItemId)
    const currentTotalQuantity = shopItems.reduce((total, cartItem) => total + cartItem.quantity, 0)

    if (currentTotalQuantity >= TOTAL_DISH_QUANTITY_LIMIT) {
      return {
        success: false,
        message: `最多只能点 ${TOTAL_DISH_QUANTITY_LIMIT} 份菜品`
      }
    }
    
    if (existingItem) {
      if (existingItem.quantity >= SINGLE_DISH_QUANTITY_LIMIT) {
        return {
          success: false,
          message: `同一个菜品最多点 ${SINGLE_DISH_QUANTITY_LIMIT} 份`
        }
      }
      existingItem.quantity += 1
    } else {
      shopItems.push({ ...item, quantity: 1 })
    }

    return { success: true }
  }

  const removeItem = (cartItemId: string) => {
    if (!currentShopId.value) return
    const sId = currentShopId.value
    const shopItems = carts.value[sId]
    if (!shopItems) return

    const index = shopItems.findIndex((i) => i.cartItemId === cartItemId)
    if (index > -1) {
      if (shopItems[index].quantity > 1) {
        shopItems[index].quantity -= 1
      } else {
        shopItems.splice(index, 1)
      }
    }
  }

  const clearCart = () => {
    if (currentShopId.value) {
      carts.value[currentShopId.value] = []
    }
  }

  return {
    shopId,
    items,
    totalQuantity,
    totalAmount,
    addItem,
    removeItem,
    clearCart,
    setShopId
  }
})
