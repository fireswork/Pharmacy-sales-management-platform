<template>
  <div class="cart-container">
    <a-card class="cart-card" :loading="loading">
      <!-- 购物车头部 -->
      <div class="cart-header">
        <h2>我的购物车</h2>
        <a-button type="link" @click="clearCart" v-if="cartItems.length">
          <DeleteOutlined /> 清空购物车
        </a-button>
      </div>

      <!-- 购物车列表 -->
      <div class="cart-list" v-if="cartItems.length">
        <a-list item-layout="horizontal" :data-source="cartItems">
          <template #renderItem="{ item }">
            <a-list-item>
              <div class="cart-item">
                <a-checkbox 
                  v-model:checked="item.selected" 
                  @change="() => updateSelection(item)"
                />
                <img 
                  :src="item.image || 'https://via.placeholder.com/80x80?text=No+Image'" 
                  :alt="item.productName" 
                  class="product-image"
                />
                <div class="product-info">
                  <div class="product-name">{{ item.productName }}</div>
                  <div class="product-price">¥{{ item.price }}</div>
                </div>
                <div class="quantity-control">
                  <a-button 
                    shape="circle" 
                    size="small"
                    @click="updateQuantity(item, -1)"
                    :disabled="item.quantity <= 1"
                  >
                    <MinusOutlined />
                  </a-button>
                  <span class="quantity">{{ item.quantity }}</span>
                  <a-button 
                    shape="circle" 
                    size="small"
                    @click="updateQuantity(item, 1)"
                    :disabled="!item.inStock || !item.available || item.quantity >= item.stockQuantity"
                  >
                    <PlusOutlined />
                  </a-button>
                  <span class="stock-info" v-if="item.stockQuantity">
                    (库存: {{ item.stockQuantity }})
                  </span>
                </div>
                <div class="item-total">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
                <a-button type="link" danger @click="removeItem(item)">
                  <DeleteOutlined />
                </a-button>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </div>

      <!-- 空购物车提示 -->
      <a-empty v-else description="购物车是空的">
        <template #extra>
          <a-button type="primary" @click="$router.push('/products')">
            去购物
          </a-button>
        </template>
      </a-empty>

      <!-- 购物车底部 -->
      <div class="cart-footer" v-if="cartItems.length">
        <div class="footer-left">
          <a-checkbox 
            :checked="isAllSelected" 
            :indeterminate="isIndeterminate"
            @change="toggleSelectAll"
          >
            全选
          </a-checkbox>
          <span class="selected-count">已选择 {{ selectedCount }} 件商品</span>
        </div>
        <div class="footer-right">
          <div class="total-amount">
            合计：<span class="amount">¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <a-button type="primary" size="large" @click="checkout" :disabled="!selectedCount">
            结算 ({{ selectedCount }})
          </a-button>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { DeleteOutlined, MinusOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import request from '@/utils/axios'

const router = useRouter()
const loading = ref(false)
const cartItems = ref([])

// 计算属性
const selectedCount = computed(() => {
  return cartItems.value.filter(item => item.selected).length
})

const totalAmount = computed(() => {
  return cartItems.value
    .filter(item => item.selected)
    .reduce((total, item) => total + item.price * item.quantity, 0)
})

const isAllSelected = computed(() => {
  return cartItems.value.length > 0 && cartItems.value.every(item => item.selected)
})

const isIndeterminate = computed(() => {
  const selectedCount = cartItems.value.filter(item => item.selected).length
  return selectedCount > 0 && selectedCount < cartItems.value.length
})

// 获取购物车列表
const fetchCartItems = async () => {
  loading.value = true
  try {
    const res = await request({
      url: `/cart/store/${localStorage.getItem('currentStoreId')}`,
      method: 'get'
    })
    cartItems.value = res.data
  } catch (error) {
    message.error('获取购物车失败')
  } finally {
    loading.value = false
  }
}

// 更新商品数量
const updateQuantity = async (item, delta) => {
  const newQuantity = item.quantity + delta
  if (newQuantity > item.stockQuantity) {
    message.warning('超出库存数量')
    return
  }

  try {
    await request({
      url: `/cart/${item.id}`,
      method: 'put',
      data: {
        quantity: newQuantity,
        storeId: localStorage.getItem('currentStoreId')
      }
    })
    item.quantity = newQuantity
  } catch (error) {
    if (error.response?.status === 400) {
      message.warning(error.response.data.message)
    } else {
      message.error('更新数量失败')
    }
  }
}

// 删除商品
const removeItem = async (item) => {
  try {
    await request({
      url: `/cart/${item.id}`,
      method: 'delete'
    })
    cartItems.value = cartItems.value.filter(i => i.id !== item.id)
    message.success('商品已移除')
  } catch (error) {
    message.error('删除失败')
  }
}

// 清空购物车
const clearCart = () => {
  Modal.confirm({
    title: '确认清空购物车？',
    content: '此操作将清空购物车中的所有商品',
    async onOk() {
      try {
        await request({
          url: `/cart/clear`,
          method: 'delete',
          params: {
            storeId: localStorage.getItem('currentStoreId')
          }
        })
        cartItems.value = []
        message.success('购物车已清空')
      } catch (error) {
        message.error('清空购物车失败')
      }
    }
  })
}

// 更新选择状态
const updateSelection = async (item) => {
  try {
    await request({
      url: `/cart/${item.id}/select`,
      method: 'put',
      params: {
        selected: item.selected
      }
    })
  } catch (error) {
    item.selected = !item.selected // 恢复状态
    message.error('更新选择状态失败')
  }
}

// 全选/取消全选
const toggleSelectAll = async (e) => {
  const checked = e.target.checked
  try {
    await Promise.all(
      cartItems.value.map(item => 
        request({
          url: `/cart/${item.id}/select`,
          method: 'put',
          params: {
            selected: checked
          }
        })
      )
    )
    cartItems.value.forEach(item => item.selected = checked)
  } catch (error) {
    message.error('更新选择状态失败')
  }
}

// 结算
const checkout = () => {
  if (selectedCount.value === 0) {
    message.warning('请选择要结算的商品')
    return
  }
  
  router.push({
    name: 'checkout',
    query: {
      amount: totalAmount.value
    }
  })
}

// 页面加载时获取购物车数据
onMounted(() => {
  fetchCartItems()
})
</script>

<style lang="less" scoped>
.cart-container {
  padding: 16px;

  .cart-card {
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);

    .cart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;

      h2 {
        margin: 0;
      }
    }

    .cart-list {
      .cart-item {
        display: flex;
        align-items: center;
        width: 100%;
        gap: 16px;

        .product-image {
          width: 80px;
          height: 80px;
          object-fit: cover;
          border-radius: 4px;
        }

        .product-info {
          flex: 1;

          .product-name {
            font-weight: 500;
            margin-bottom: 8px;
          }

          .product-price {
            color: #ff4d4f;
          }
        }

        .quantity-control {
          display: flex;
          align-items: center;
          gap: 8px;

          .quantity {
            min-width: 40px;
            text-align: center;
          }

          .stock-info {
            color: #8c8c8c;
            font-size: 12px;
          }
        }

        .item-total {
          font-weight: 500;
          color: #ff4d4f;
          min-width: 80px;
          text-align: right;
        }
      }
    }

    .cart-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 24px;
      padding-top: 24px;
      border-top: 1px solid rgba(0, 0, 0, 0.06);

      .footer-left {
        display: flex;
        align-items: center;
        gap: 16px;

        .selected-count {
          color: #8c8c8c;
        }
      }

      .footer-right {
        display: flex;
        align-items: center;
        gap: 24px;

        .total-amount {
          .amount {
            font-size: 20px;
            font-weight: bold;
            color: #ff4d4f;
          }
        }
      }
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .cart-item {
    flex-wrap: wrap;
    
    .product-info {
      width: 100%;
      order: -1;
    }
    
    .quantity-control {
      margin-left: auto;
    }
  }
}
</style> 