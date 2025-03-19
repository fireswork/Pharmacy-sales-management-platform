<template>
  <div class="orders-container">
    <a-card>
      <!-- 筛选区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="orderNo"
            placeholder="请输入订单号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="orderStatus"
            style="width: 120px"
            placeholder="订单状态"
          >
            <a-select-option value="all">全部订单</a-select-option>
            <a-select-option value="pending">待付款</a-select-option>
            <a-select-option value="processing">待发货</a-select-option>
            <a-select-option value="shipped">已发货</a-select-option>
            <a-select-option value="completed">已完成</a-select-option>
          </a-select>

          <a-date-picker
            v-model:value="orderDate"
            placeholder="下单日期"
            style="width: 180px"
          />

          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </a-space>
      </div>

      <!-- 订单表格 -->
      <a-table 
        :columns="columns" 
        :data-source="filteredOrders"
        :pagination="pagination"
        :row-key="record => record.orderId"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'product'">
            <div class="product-item">
              <img :src="record.product.image" :alt="record.product.name" class="product-image"/>
              <div class="product-info">
                <div class="product-name">{{ record.product.name }}</div>
                <div class="product-price">¥{{ record.product.price }} × {{ record.product.quantity }}</div>
              </div>
            </div>
          </template>

          <template v-if="column.key === 'total'">
            <span class="total-amount">¥{{ getTotalAmount(record.product) }}</span>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a-button 
                v-if="record.status === 'pending'" 
                type="primary"
                size="small"
                @click="handlePayment(record)"
              >
                立即付款
              </a-button>
              <a-button 
                v-if="record.status === 'shipped'" 
                type="primary"
                size="small"
                @click="confirmReceived(record)"
              >
                确认收货
              </a-button>
              <a-button 
                v-if="record.status === 'completed'" 
                size="small"
                @click="writeReview(record)"
              >
                评价
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 添加评价弹窗 -->
    <a-modal
      v-model:visible="reviewModalVisible"
      title="商品评价"
      @ok="handleReviewSubmit"
      :confirmLoading="submitLoading"
      okText="提交评价"
      cancelText="取消"
      width="500px"
    >
      <div class="review-form">
        <div class="product-info">
          <img :src="currentOrder?.product.image" :alt="currentOrder?.product.name" class="product-image"/>
          <div class="product-detail">
            <div class="product-name">{{ currentOrder?.product.name }}</div>
            <div class="product-price">¥{{ currentOrder?.product.price }}</div>
          </div>
        </div>
        <div class="rating-section">
          <span class="label">商品评分：</span>
          <a-rate v-model:value="reviewForm.rating" />
        </div>
        <div class="content-section">
          <span class="label">评价内容：</span>
          <a-textarea
            v-model:value="reviewForm.content"
            :rows="4"
            placeholder="请输入您的评价内容，最多200字"
            :maxLength="200"
            :show-count="false"
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const orderStatus = ref('all')
const orderDate = ref(null)
const orderNo = ref('')

// 表格列定义
const columns = [
  {
    title: '订单号',
    dataIndex: 'orderId',
    key: 'orderId',
    width: 180,
  },
  {
    title: '下单时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
  },
  {
    title: '商品信息',
    key: 'product',
  },
  {
    title: '订单状态',
    key: 'status',
    dataIndex: 'status',
    width: 100,
    align: 'center',
  },
  {
    title: '实付金额',
    key: 'total',
    width: 120,
    align: 'right',
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    align: 'center',
    fixed: 'right',
  },
]

// 修改模拟订单数据格式
const orders = [
  {
    orderId: 'ORDER202401010001',
    createTime: '2024-01-01 12:00:00',
    status: 'pending',
    product: {
      id: 1,
      name: '布洛芬缓释胶囊',
      price: 25.8,
      quantity: 2,
      image: '/images/product1.jpg'
    }
  },
  {
    orderId: 'ORDER202401010002',
    createTime: '2024-01-01 14:30:00',
    status: 'completed',
    product: {
      id: 3,
      name: '维生素C片',
      price: 45.0,
      quantity: 1,
      image: '/images/product3.jpg'
    }
  },
  {
    orderId: 'ORDER202401020001',
    createTime: '2024-01-02 09:15:00',
    status: 'shipped',
    product: {
      id: 4,
      name: '复方板蓝根颗粒',
      price: 28.5,
      quantity: 3,
      image: '/images/product4.jpg'
    }
  },
  {
    orderId: 'ORDER202401020002',
    createTime: '2024-01-02 15:45:00',
    status: 'processing',
    product: {
      id: 5,
      name: '阿莫西林胶囊',
      price: 35.6,
      quantity: 1,
      image: '/images/product5.jpg'
    }
  },
  {
    orderId: 'ORDER202401030001',
    createTime: '2024-01-03 10:20:00',
    status: 'completed',
    product: {
      id: 7,
      name: '银翘解毒片',
      price: 22.5,
      quantity: 2,
      image: '/images/product7.jpg'
    }
  },
  {
    orderId: 'ORDER202401030002',
    createTime: '2024-01-03 16:40:00',
    status: 'pending',
    product: {
      id: 9,
      name: '小柴胡颗粒',
      price: 38.5,
      quantity: 1,
      image: '/images/product9.jpg'
    }
  },
  {
    orderId: 'ORDER202401040001',
    createTime: '2024-01-04 11:30:00',
    status: 'shipped',
    product: {
      id: 10,
      name: '金嗓子喉片',
      price: 29.9,
      quantity: 2,
      image: '/images/product10.jpg'
    }
  },
  {
    orderId: 'ORDER202401040002',
    createTime: '2024-01-04 14:20:00',
    status: 'processing',
    product: {
      id: 12,
      name: '999感冒灵',
      price: 32.8,
      quantity: 1,
      image: '/images/product12.jpg'
    }
  }
]

// 更新筛选逻辑
const filteredOrders = computed(() => {
  let result = [...orders]
  
  // 订单号筛选
  if (orderNo.value) {
    result = result.filter(order => 
      order.orderId.toLowerCase().includes(orderNo.value.toLowerCase())
    )
  }
  
  // 状态筛选
  if (orderStatus.value !== 'all') {
    result = result.filter(order => order.status === orderStatus.value)
  }
  
  // 日期筛选
  if (orderDate.value) {
    const selectedDate = dayjs(orderDate.value).format('YYYY-MM-DD')
    result = result.filter(order => 
      dayjs(order.createTime).format('YYYY-MM-DD') === selectedDate
    )
  }
  
  return result
})

// 分页配置
const pagination = {
  total: orders.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`,
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    pending: '待付款',
    processing: '待发货',
    shipped: '已发货',
    completed: '已完成'
  }
  return statusMap[status]
}

// 获取状态颜色
const getStatusColor = (status) => {
  const colorMap = {
    pending: 'orange',
    processing: 'blue',
    shipped: 'green',
    completed: 'gray'
  }
  return colorMap[status]
}

// 修改计算总金额的方法
const getTotalAmount = (product) => {
  return (product.price * product.quantity).toFixed(2)
}

// 处理付款
const handlePayment = (order) => {
  router.push({
    path: '/home/checkout',
    query: { 
      orderId: order.orderId,
      amount: getTotalAmount(order.product)
    }
  })
}

// 确认收货
const confirmReceived = (order) => {
  message.success('确认收货成功')
}

// 评价相关的状态
const reviewModalVisible = ref(false)
const submitLoading = ref(false)
const currentOrder = ref(null)
const reviewForm = ref({
  rating: 5,
  content: ''
})

// 显示评价弹窗
const writeReview = (order) => {
  currentOrder.value = order
  reviewForm.value = {
    rating: 5,
    content: ''
  }
  reviewModalVisible.value = true
}

// 提交评价
const handleReviewSubmit = async () => {
  if (!reviewForm.value.content.trim()) {
    message.warning('请输入评价内容')
    return
  }

  submitLoading.value = true
  try {
    // 模拟提交评价
    await new Promise(resolve => setTimeout(resolve, 1000))
    message.success('评价提交成功')
    reviewModalVisible.value = false
    
    // 更新订单状态
    const orderIndex = filteredOrders.value.findIndex(o => o.orderId === currentOrder.value.orderId)
    if (orderIndex !== -1) {
      filteredOrders.value[orderIndex].status = 'reviewed'
    }
  } finally {
    submitLoading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  // 实际项目中这里会调用API
  message.success('搜索成功')
}

// 处理重置
const handleReset = () => {
  orderStatus.value = 'all'
  orderDate.value = null
  orderNo.value = ''
  handleSearch()
}
</script>

<style lang="less" scoped>
.orders-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
  }

  :deep(.ant-table) {
    .ant-table-thead > tr > th {
      background: #fafafa;
      font-weight: 500;
    }

    // 优化表格边框样式，避免重叠
    .ant-table-thead > tr > th,
    .ant-table-tbody > tr > td {
      border-right: 1px solid #f0f0f0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-right: none;
      }
    }

    // 其他样式保持不变
    .product-item {
      display: flex;
      align-items: center;
      padding: 8px 0;

      .product-image {
        width: 60px;
        height: 60px;
        object-fit: cover;
        border-radius: 4px;
        margin-right: 12px;
      }

      .product-info {
        flex: 1;

        .product-name {
          margin-bottom: 4px;
          font-weight: 500;
          color: #262626;
        }

        .product-price {
          color: #8c8c8c;
          font-size: 13px;
        }
      }
    }

    .total-amount {
      font-weight: bold;
      color: #ff4d4f;
      font-size: 15px;
    }
  }

  .review-form {
    padding: 0 12px;

    .product-info {
      display: flex;
      align-items: center;
      padding: 16px 0 24px;
      margin-bottom: 24px;
      border-bottom: 1px solid #f0f0f0;

      .product-image {
        width: 80px;
        height: 80px;
        object-fit: cover;
        border-radius: 4px;
        margin-right: 16px;
      }

      .product-detail {
        flex: 1;

        .product-name {
          margin-bottom: 8px;
          font-weight: 500;
          font-size: 16px;
          color: #262626;
        }

        .product-price {
          color: #ff4d4f;
          font-size: 15px;
          font-weight: 500;
        }
      }
    }

    .rating-section {
      margin-bottom: 24px;

      .label {
        display: inline-block;
        margin-right: 12px;
        color: #262626;
        font-size: 14px;
      }

      :deep(.ant-rate) {
        font-size: 20px;
      }
    }

    .content-section {
      .label {
        display: block;
        margin-bottom: 12px;
        color: #262626;
        font-size: 14px;
      }

      :deep(.ant-input) {
        padding: 12px;
        font-size: 14px;
        resize: none;
        border-radius: 4px;

        &:hover, &:focus {
          border-color: #40a9ff;
        }
      }
    }
  }
}

// 优化弹窗样式
:deep(.ant-modal-content) {
  .ant-modal-header {
    padding: 16px 24px;
    border-bottom: 1px solid #f0f0f0;

    .ant-modal-title {
      font-size: 16px;
      font-weight: 500;
      color: #262626;
    }
  }

  .ant-modal-body {
    padding: 24px;
  }

  .ant-modal-footer {
    padding: 16px 24px;
    border-top: 1px solid #f0f0f0;

    .ant-btn {
      height: 32px;
      padding: 0 16px;
      font-size: 14px;
    }
  }
}
</style> 