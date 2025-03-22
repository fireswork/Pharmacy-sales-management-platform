<template>
  <div class="checkout-container">
    <a-card class="checkout-card" :loading="loading">
      <h2 class="page-title">订单结算</h2>

      <!-- 订单金额 -->
      <div class="section">
        <div class="section-title">支付金额：</div>
        <div class="amount">¥{{ amount.toFixed(2) }}</div>
      </div>
      
      <!-- 收货地址 -->
      <div class="section">
        <div class="section-header">
          <div class="section-title">收货地址</div>
          <a-button type="link" @click="goToAddressManagement">
            <PlusOutlined /> 添加新地址
          </a-button>
        </div>
        
        <div class="address-list" v-if="addresses.length > 0">
          <a-radio-group v-model:value="selectedAddressId" class="address-radio-group">
            <div v-for="address in addresses" :key="address.id" class="address-item">
              <a-radio :value="address.id">
                <div class="address-content">
                  <div class="address-info">
                    <UserOutlined class="address-icon" />
                    <span class="address-name">{{ address.receiver }}</span>
                  </div>
                  <div class="address-info">
                    <PhoneOutlined class="address-icon" />
                    <span class="address-phone">{{ address.phoneNumber }}</span>
                  </div>
                  <div class="address-info address-detail">
                    <HomeOutlined class="address-icon" />
                    <span>{{ address.address }}</span>
                  </div>
                </div>
              </a-radio>
            </div>
          </a-radio-group>
        </div>
        
        <a-empty v-else description="暂无收货地址">
          <template #extra>
            <a-button type="primary" @click="goToAddressManagement">添加地址</a-button>
          </template>
        </a-empty>
      </div>

      <!-- 配送方式 -->
      <div class="section">
        <div class="section-title">配送方式：</div>
        <a-select v-model:value="deliveryMethod" style="width: 200px">
          <a-select-option value="express">快递配送</a-select-option>
          <a-select-option value="self">到店自取</a-select-option>
        </a-select>
      </div>

      <!-- 支付方式 -->
      <div class="section">
        <div class="section-title">支付方式</div>
        <div class="payment-methods">
          <div 
            class="payment-method-item" 
            :class="{ active: paymentMethod === 'wechat' }"
            @click="paymentMethod = 'wechat'"
          >
            <WechatOutlined />
            <span>微信支付</span>
          </div>
          <div 
            class="payment-method-item" 
            :class="{ active: paymentMethod === 'alipay' }"
            @click="paymentMethod = 'alipay'"
          >
            <AlipayCircleOutlined />
            <span>支付宝</span>
          </div>
          <div 
            class="payment-method-item" 
            :class="{ active: paymentMethod === 'bank' }"
            @click="paymentMethod = 'bank'"
          >
            <BankOutlined />
            <span>建设银行</span>
          </div>
          <div 
            class="payment-method-item" 
            :class="{ active: paymentMethod === 'cbank' }"
            @click="paymentMethod = 'cbank'"
          >
            <BankOutlined />
            <span>中国银行</span>
          </div>
        </div>
      </div>

      <!-- 提交订单 -->
      <div class="checkout-footer">
        <a-button type="primary" size="large" @click="submitOrder" :loading="submitting">
          确认支付
        </a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  WechatOutlined, 
  AlipayCircleOutlined, 
  BankOutlined, 
  PlusOutlined,
  UserOutlined,
  PhoneOutlined,
  HomeOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/axios'

const router = useRouter()
const route = useRoute()

// 状态
const loading = ref(false)
const submitting = ref(false)
const amount = ref(Number(route.query.amount) || 0)
const addresses = ref([])
const selectedAddressId = ref(null)
const deliveryMethod = ref('express')
const paymentMethod = ref('wechat')

// 获取地址列表
const fetchAddresses = async () => {
  loading.value = true
  try {
    const res = await request({
      url: '/address',
      method: 'get'
    })
    addresses.value = res.data || []
    
    // 如果有默认地址，选中它
    const defaultAddress = addresses.value.find(addr => addr.default)
    if (defaultAddress) {
      selectedAddressId.value = defaultAddress.id
    } else if (addresses.value.length > 0) {
      selectedAddressId.value = addresses.value[0].id
    }
    
    console.log('地址列表:', addresses.value)
    console.log('选中的地址ID:', selectedAddressId.value)
  } catch (error) {
    message.error('获取地址列表失败')
    console.error('获取地址列表错误:', error)
  } finally {
    loading.value = false
  }
}

// 跳转到地址管理页面
const goToAddressManagement = () => {
  // 保存当前结算页面状态
  localStorage.setItem('checkoutAmount', amount.value)
  localStorage.setItem('checkoutDeliveryMethod', deliveryMethod.value)
  localStorage.setItem('checkoutPaymentMethod', paymentMethod.value)
  
  // 跳转到地址管理页面
  router.push('/home/user/address')
}

// 提交订单
const submitOrder = async () => {
  if (!selectedAddressId.value) {
    message.warning('请选择收货地址')
    return
  }

  submitting.value = true
  try {
    const selectedAddress = addresses.value.find(addr => addr.id === selectedAddressId.value)
    if (!selectedAddress) {
      throw new Error('未找到选中的地址信息')
    }
    
    console.log('提交订单的地址ID:', selectedAddressId.value)
    
    // 创建订单
    const orderRes = await request({
      url: '/orders',
      method: 'post',
      data: {
        totalAmount: amount.value,
        deliveryMethod: deliveryMethod.value,
        paymentMethod: paymentMethod.value,
        addressId: selectedAddressId.value // 只传递地址ID
      }
    })
    
    message.success('订单已支付，请等待发货')
    
    // 跳转到支付页面
    router.push({
      path: '/home/order',
    })
  } catch (error) {
    console.error('创建订单错误:', error)
    message.error(error.response?.data?.message || '创建订单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 页面加载时获取地址列表
onMounted(() => {
  fetchAddresses()
  
  // 恢复之前保存的结算页面状态
  const savedAmount = localStorage.getItem('checkoutAmount')
  const savedDeliveryMethod = localStorage.getItem('checkoutDeliveryMethod')
  const savedPaymentMethod = localStorage.getItem('checkoutPaymentMethod')
  
  if (savedAmount && !route.query.amount) {
    amount.value = Number(savedAmount)
  }
  
  if (savedDeliveryMethod) {
    deliveryMethod.value = savedDeliveryMethod
  }
  
  if (savedPaymentMethod) {
    paymentMethod.value = savedPaymentMethod
  }
})
</script>

<style lang="less" scoped>
.checkout-container {
  padding: 20px;
  
  .checkout-card {
    max-width: 800px;
    margin: 0 auto;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    
    .page-title {
      margin-bottom: 24px;
      color: #262626;
      text-align: center;
    }
    
    .section {
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
      }
      
      .section-title {
        font-size: 16px;
        font-weight: 500;
        color: #262626;
      }
      
      .amount {
        font-size: 24px;
        font-weight: bold;
        color: #ff4d4f;
      }
      
      .address-list {
        .address-radio-group {
          width: 100%;
          
          .address-item {
            margin-bottom: 12px;
            padding: 16px;
            border: 1px solid #d9d9d9;
            border-radius: 4px;
            transition: all 0.3s;
            background-color: #fafafa;
            
            &:hover {
              border-color: #1890ff;
              background-color: #f0f8ff;
            }
            
            .address-content {
              display: flex;
              justify-content: space-between;
              align-items: center;
              
              .address-info {
                display: flex;
                align-items: center;
                
                .address-icon {
                  margin-right: 6px;
                  color: #666;
                  font-size: 16px;
                }
              }
              
              .address-name {
                font-weight: 500;
                font-size: 14px;
                min-width: 60px;
              }
              
              .address-phone {
                font-size: 14px;
                color: #666;
                min-width: 100px;
              }
              
              .address-detail {
                color: #666;
                flex: 1;
                text-align: right;
                font-size: 14px;
                max-width: 45%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                
                span {
                  overflow: hidden;
                  text-overflow: ellipsis;
                  white-space: nowrap;
                }
              }
            }
          }
        }
      }
      
      .payment-methods {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
        
        .payment-method-item {
          flex: 1;
          min-width: 160px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          padding: 16px;
          border: 1px solid #d9d9d9;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.3s;
          
          &:hover {
            border-color: #40a9ff;
          }
          
          &.active {
            border-color: #1890ff;
            background-color: #e6f7ff;
          }
          
          .anticon {
            font-size: 24px;
          }
        }
      }
    }
    
    .checkout-footer {
      display: flex;
      justify-content: center;
      margin-top: 24px;
      
      .ant-btn {
        min-width: 200px;
      }
    }
  }
}

@media (max-width: 576px) {
  .checkout-container {
    padding: 12px;
    
    .payment-methods {
      .payment-method-item {
        min-width: 120px;
      }
    }
  }
}
</style> 