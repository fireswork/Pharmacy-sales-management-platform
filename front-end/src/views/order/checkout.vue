<template>
  <div class="checkout-container">
    <a-card class="checkout-card">
      <div class="payment-info">
        <div class="amount-section">
          <span class="label">支付金额：</span>
          <span class="amount">¥{{ totalAmount }}</span>
        </div>
        
        <div class="delivery-section">
          <span class="label">配送方式：</span>
          <a-select v-model:value="deliveryMethod" style="width: 200px">
            <a-select-option value="express">快递配送</a-select-option>
            <a-select-option value="self">到店自取</a-select-option>
          </a-select>
        </div>

        <div class="payment-methods">
          <h3>支付方式</h3>
          <div class="methods-grid">
            <div 
              v-for="method in paymentMethods" 
              :key="method.id"
              class="method-item"
              :class="{ active: selectedMethod === method.id }"
              @click="selectPaymentMethod(method.id)"
            >
              <component :is="method.icon" class="payment-icon" />
              <span class="method-name">{{ method.name }}</span>
            </div>
          </div>
        </div>

        <div class="action-section">
          <a-button 
            type="primary" 
            size="large" 
            :disabled="!selectedMethod"
            :loading="paying"
            @click="handlePayment"
          >
            确认支付
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- 支付结果弹窗 -->
    <a-modal
      v-model:visible="paymentResultVisible"
      :title="null"
      :footer="null"
      :closable="false"
      width="400px"
    >
      <div class="payment-result">
        <CheckCircleFilled class="success-icon" />
        <h2>支付成功</h2>
        <p>订单支付完成，感谢您的购买！</p>
        <a-button type="primary" @click="goToOrders">查看订单</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  CheckCircleFilled,
  WechatOutlined,
  AlipayOutlined,
  BankOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

// 从路由参数中获取总金额
const totalAmount = ref(0)

onMounted(() => {
  // 获取传递的金额参数，如果没有则使用默认值 0
  const amount = Number(route.query.amount)
  totalAmount.value = amount || 0
  
  // 如果没有金额参数，提示错误并返回购物车页面
  if (!amount) {
    message.error('订单金额无效')
    router.push('/cart')
  }
})

const deliveryMethod = ref('express')
const selectedMethod = ref('')
const paying = ref(false)
const paymentResultVisible = ref(false)

// 支付方式数据
const paymentMethods = [
  {
    id: 'wechat',
    name: '微信支付',
    icon: WechatOutlined,
  },
  {
    id: 'alipay',
    name: '支付宝',
    icon: AlipayOutlined,
  },
  {
    id: 'ccb',
    name: '建设银行',
    icon: BankOutlined,
  },
  {
    id: 'boc',
    name: '中国银行',
    icon: BankOutlined,
  }
]

// 选择支付方式
const selectPaymentMethod = (methodId) => {
  selectedMethod.value = methodId
}

// 处理支付
const handlePayment = async () => {
  if (!selectedMethod.value) {
    message.warning('请选择支付方式')
    return
  }

  paying.value = true
  // 模拟支付过程
  try {
    await new Promise(resolve => setTimeout(resolve, 2000))
    paymentResultVisible.value = true
  } catch (error) {
    message.error('支付失败，请重试')
  } finally {
    paying.value = false
  }
}

// 跳转到订单列表
const goToOrders = () => {
  router.push('/user/orders')
}
</script>

<style lang="less" scoped>
.checkout-container {
  padding: 16px;

  .checkout-card {
    max-width: 800px;
    margin: 0 auto;
    background: #fff;

    .payment-info {
      .amount-section {
        margin-bottom: 24px;
        padding-bottom: 24px;
        border-bottom: 1px solid #f0f0f0;

        .label {
          font-size: 16px;
        }

        .amount {
          font-size: 24px;
          color: #ff4d4f;
          font-weight: bold;
          margin-left: 8px;
        }
      }

      .delivery-section {
        margin-bottom: 24px;
        
        .label {
          margin-right: 16px;
        }
      }

      .payment-methods {
        margin-bottom: 32px;

        h3 {
          margin-bottom: 16px;
        }

        .methods-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
          gap: 16px;

          .method-item {
            display: flex;
            align-items: center;
            padding: 16px;
            border: 1px solid #d9d9d9;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
              border-color: #1890ff;
            }

            &.active {
              border-color: #1890ff;
              background: #e6f7ff;
            }

            .payment-icon {
              font-size: 24px;
              margin-right: 12px;
            }

            &.active .payment-icon {
              color: #1890ff;
            }

            &[data-method="wechat"] .payment-icon {
              color: #07c160;
            }

            &[data-method="alipay"] .payment-icon {
              color: #1677ff;
            }

            &[data-method="ccb"] .payment-icon {
              color: #0066b3;
            }

            &[data-method="boc"] .payment-icon {
              color: #c7000b;
            }

            .method-name {
              font-size: 16px;
            }
          }
        }
      }

      .action-section {
        text-align: center;
        margin-top: 32px;
      }
    }
  }
}

.payment-result {
  text-align: center;
  padding: 32px 0;

  .success-icon {
    font-size: 48px;
    color: #52c41a;
    margin-bottom: 16px;
  }

  h2 {
    margin-bottom: 8px;
  }

  p {
    color: #8c8c8c;
    margin-bottom: 24px;
  }
}

@media (max-width: 768px) {
  .methods-grid {
    grid-template-columns: 1fr !important;
  }
}
</style> 