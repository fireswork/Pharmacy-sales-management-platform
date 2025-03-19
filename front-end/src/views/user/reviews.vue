<template>
  <div class="reviews-container">
    <a-card>
      <!-- 筛选区域 -->
      <div class="filter-section">
        <a-space>
          <a-select
            v-model:value="reviewStatus"
            style="width: 120px"
            placeholder="评价状态"
          >
            <a-select-option value="all">全部</a-select-option>
            <a-select-option value="pending">待评价</a-select-option>
            <a-select-option value="completed">已评价</a-select-option>
          </a-select>

          <a-range-picker
            v-model:value="dateRange"
            :placeholder="['开始日期', '结束日期']"
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

      <!-- 评价列表 -->
      <a-table
        :columns="columns"
        :data-source="filteredReviews"
        :pagination="pagination"
        :row-key="record => record.orderId"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 商品信息列 -->
          <template v-if="column.key === 'product'">
            <div class="product-item">
              <img :src="record.product.image" :alt="record.product.name" class="product-image"/>
              <div class="product-info">
                <div class="product-name">{{ record.product.name }}</div>
                <div class="product-price">¥{{ record.product.price }}</div>
              </div>
            </div>
          </template>

          <!-- 评价状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'pending' ? 'orange' : 'green'">
              {{ record.status === 'pending' ? '待评价' : '已评价' }}
            </a-tag>
          </template>

          <!-- 评价内容列 -->
          <template v-if="column.key === 'review'">
            <template v-if="record.status === 'completed'">
              <div class="review-content">
                <a-rate :value="record.rating" disabled />
                <div class="review-text">{{ record.reviewContent }}</div>
                <div class="review-time">{{ record.reviewTime }}</div>
              </div>
            </template>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-button 
              v-if="record.status === 'pending'"
              type="primary" 
              size="small"
              @click="showReviewModal(record)"
            >
              立即评价
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 评价弹窗 -->
    <a-modal
      v-model:visible="reviewModalVisible"
      title="商品评价"
      @ok="handleReviewSubmit"
      :confirmLoading="submitLoading"
    >
      <div class="review-form">
        <div class="rating-section">
          <span class="label">商品评分：</span>
          <a-rate v-model:value="currentReview.rating" />
        </div>
        <div class="content-section">
          <span class="label">评价内容：</span>
          <a-textarea
            v-model:value="currentReview.content"
            :rows="4"
            placeholder="请输入您的评价内容"
            :maxLength="200"
            show-count
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'

// 评价状态
const reviewStatus = ref('all')
const dateRange = ref([])
const reviewModalVisible = ref(false)
const submitLoading = ref(false)

// 当前评价信息
const currentReview = ref({
  rating: 5,
  content: '',
  orderId: null
})

// 表格列定义
const columns = [
  {
    title: '订单号',
    dataIndex: 'orderId',
    key: 'orderId',
    width: 180,
  },
  {
    title: '商品信息',
    key: 'product',
    width: 300,
  },
  {
    title: '评价状态',
    key: 'status',
    width: 100,
    align: 'center',
  },
  {
    title: '评价内容',
    key: 'review',
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    align: 'center',
  },
]

// 模拟评价数据
const reviews = ref([
  {
    orderId: 'ORDER202401010001',
    status: 'completed',
    product: {
      name: '布洛芬缓释胶囊',
      price: 25.8,
      image: '/images/product1.jpg',
    },
    rating: 5,
    reviewContent: '药效很好，物流快，包装完整，很满意！',
    reviewTime: '2024-01-02 14:30:00'
  },
  {
    orderId: 'ORDER202401020001',
    status: 'pending',
    product: {
      name: '感冒灵颗粒',
      price: 32.5,
      image: '/images/product2.jpg',
    }
  },
  // 可以添加更多模拟数据
])

// 分页配置
const pagination = {
  total: reviews.value.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`,
}

// 筛选评价列表
const filteredReviews = computed(() => {
  let result = [...reviews.value]
  
  if (reviewStatus.value !== 'all') {
    result = result.filter(review => review.status === reviewStatus.value)
  }
  
  if (dateRange.value?.length === 2) {
    const startDate = dateRange.value[0]
    const endDate = dateRange.value[1]
    result = result.filter(review => {
      if (review.status === 'completed') {
        const reviewDate = dayjs(review.reviewTime)
        return reviewDate.isBetween(startDate, endDate, 'day', '[]')
      }
      return false
    })
  }
  
  return result
})

// 显示评价弹窗
const showReviewModal = (record) => {
  currentReview.value = {
    rating: 5,
    content: '',
    orderId: record.orderId
  }
  reviewModalVisible.value = true
}

// 提交评价
const handleReviewSubmit = async () => {
  if (!currentReview.value.content.trim()) {
    message.warning('请输入评价内容')
    return
  }

  submitLoading.value = true
  try {
    // 模拟提交评价
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 更新本地数据
    const reviewIndex = reviews.value.findIndex(r => r.orderId === currentReview.value.orderId)
    if (reviewIndex !== -1) {
      reviews.value[reviewIndex] = {
        ...reviews.value[reviewIndex],
        status: 'completed',
        rating: currentReview.value.rating,
        reviewContent: currentReview.value.content,
        reviewTime: dayjs().format('YYYY-MM-DD HH:mm:ss')
      }
    }

    message.success('评价提交成功')
    reviewModalVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

// 搜索
const handleSearch = () => {
  message.success('搜索成功')
}

// 重置
const handleReset = () => {
  reviewStatus.value = 'all'
  dateRange.value = []
  handleSearch()
}
</script>

<style lang="less" scoped>
.reviews-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
  }

  :deep(.ant-table) {
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
        .product-name {
          margin-bottom: 4px;
          font-weight: 500;
        }

        .product-price {
          color: #ff4d4f;
        }
      }
    }

    .review-content {
      .review-text {
        margin: 8px 0;
        color: #666;
      }

      .review-time {
        color: #999;
        font-size: 12px;
      }
    }
  }

  .review-form {
    .rating-section {
      margin-bottom: 16px;
    }

    .content-section {
      .label {
        display: block;
        margin-bottom: 8px;
      }
    }

    .label {
      color: #666;
    }
  }
}
</style> 