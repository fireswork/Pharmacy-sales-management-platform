<template>
  <div class="cart-container">
    <a-card class="cart-card">
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
                <a-checkbox v-model:checked="item.selected" @change="updateSelection"/>
                <img :src="item.image" :alt="item.name" class="product-image"/>
                <div class="product-info">
                  <div class="product-name">{{ item.name }}</div>
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
                    :disabled="item.quantity >= item.stock"
                  >
                    <PlusOutlined />
                  </a-button>
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
import { ref, computed } from 'vue'
import { DeleteOutlined, MinusOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 购物车数据
const cartItems = ref([
  {
    id: 1,
    name: '布洛芬缓释胶囊',
    price: 35.8,
    quantity: 1,
    stock: 100,
    image: 'https://example.com/medicine1.jpg',
    selected: true
  },
  // 可以添加更多测试数据
])

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

// 方法
const updateQuantity = (item, delta) => {
  const newQuantity = item.quantity + delta
  if (newQuantity >= 1 && newQuantity <= item.stock) {
    item.quantity = newQuantity
  }
}

const removeItem = (item) => {
  cartItems.value = cartItems.value.filter(i => i.id !== item.id)
  message.success('商品已移除')
}

const clearCart = () => {
  cartItems.value = []
  message.success('购物车已清空')
}

const toggleSelectAll = (e) => {
  const checked = e.target.checked
  cartItems.value.forEach(item => item.selected = checked)
}

const updateSelection = () => {
  // 更新选择状态时可能需要做一些额外操作
}

const checkout = () => {
  if (selectedCount.value === 0) {
    message.warning('请选择要结算的商品')
    return
  }
  
  // 跳转到结算页面，并传递总金额
  router.push({
    name: 'checkout',
    query: {
      amount: totalAmount.value
    }
  })
}
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