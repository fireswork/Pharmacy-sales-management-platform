<template>
  <div class="reviews-container">
    <a-card>
      <!-- 筛选区域 -->
      <div class="filter-section">
        <a-space>
          <a-select
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="评价状态"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="pending">待评价</a-select-option>
            <a-select-option value="completed">已评价</a-select-option>
          </a-select>

          <a-range-picker
            v-model:value="searchForm.dateRange"
            :placeholder="['开始日期', '结束日期']"
          />

          <a-button type="primary" @click="handleSearch" :loading="loading">
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
        :data-source="processedReviewList"
        :pagination="pagination"
        :row-key="record => record.id || record.tempId"
        :loading="loading"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 商品信息列 -->
          <template v-if="column.key === 'product'">
            <div class="product-item">
              <img :src="record.productImage" :alt="record.productName" class="product-image"/>
              <div class="product-info">
                <div class="product-name">{{ record.productName }}</div>
                <div class="product-price" v-if="record.price">¥{{ record.price.toFixed(2) }}</div>
              </div>
            </div>
          </template>

          <!-- 评价状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record)">
              {{ getStatusText(record) }}
            </a-tag>
          </template>

          <!-- 评价内容列 -->
          <template v-if="column.key === 'review'">
            <template v-if="isReviewed(record)">
              <div class="review-content">
                <a-rate :value="record.rating" disabled />
                <div class="review-text">{{ record.content }}</div>
                <div class="review-time">{{ formatDate(record.createTime) }}</div>
              </div>
            </template>
            <template v-else>
              <span class="no-review">暂无评价</span>
            </template>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button 
                v-if="!isReviewed(record)"
                type="primary" 
                size="small"
                @click="showReviewModal(record)"
              >
                立即评价
              </a-button>
              <a-button 
                v-if="isReviewed(record) && canDeleteReview(record)"
                type="link" 
                size="small"
                @click="confirmDeleteReview(record)"
              >
                删除
              </a-button>
            </a-space>
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
      :maskClosable="false"
    >
      <div class="review-form">
        <div class="selected-product">
          <div class="product-image">
            <img :src="currentReview.productImage" :alt="currentReview.productName" />
          </div>
          <div class="product-info">
            <h4>{{ currentReview.productName }}</h4>
            <p v-if="currentReview.price">¥{{ currentReview.price.toFixed(2) }}</p>
          </div>
        </div>
        
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
import { ref, reactive, computed, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import request from "@/utils/axios";

// 状态管理
const loading = ref(false);
const submitLoading = ref(false);
const reviewList = ref([]);
const pendingList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const reviewModalVisible = ref(false);

// 搜索表单
const searchForm = reactive({
  status: '',
  dateRange: [],
  page: 0,
  size: 10
});

// 当前评价信息
const currentReview = reactive({
  id: null,
  rating: 5,
  content: '',
  orderId: null,
  productId: null,
  productName: '',
  productImage: '',
  price: null
});

// 表格列定义
const columns = [
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
    width: 120,
    align: 'center',
  },
];

// 合并已评价和待评价列表
const processedReviewList = computed(() => {
  let result = [];
  
  // 添加已评价的商品
  if (reviewList.value && reviewList.value.length > 0) {
    result = [...reviewList.value];
  }
  
  // 添加待评价的商品
  if (pendingList.value && pendingList.value.length > 0 && 
      (searchForm.status === '' || searchForm.status === 'pending')) {
    // 确保不重复添加
    const existingProductIds = new Set(result.map(r => r.productId?.toString()));
    const filteredPending = pendingList.value.filter(item => 
      !existingProductIds.has(item.productId?.toString())
    );
    
    result = [...result, ...filteredPending];
  }
  
  // 根据状态筛选
  if (searchForm.status === 'completed') {
    result = result.filter(item => isReviewed(item));
  } else if (searchForm.status === 'pending') {
    result = result.filter(item => !isReviewed(item));
  }
  
  // 为没有ID的项目添加临时ID
  result.forEach((item, index) => {
    if (!item.id) {
      item.tempId = `temp-${index}`;
    }
  });
  
  return result;
});

// 判断是否已评价
const isReviewed = (record) => {
  return record.rating !== undefined && record.content !== undefined;
};

// 获取状态颜色
const getStatusColor = (record) => {
  return isReviewed(record) ? 'green' : 'orange';
};

// 获取状态文本
const getStatusText = (record) => {
  return isReviewed(record) ? '已评价' : '待评价';
};

// 分页配置
const pagination = computed(() => ({
  total: processedReviewList.value.length,
  current: currentPage.value,
  pageSize: pageSize.value,
  showSizeChanger: true,
  pageSizeOptions: ["10", "20", "50"],
  showTotal: (total) => `共 ${total} 条记录`,
  onChange: (page, size) => {
    currentPage.value = page;
    pageSize.value = size;
  },
  onShowSizeChange: (current, size) => {
    currentPage.value = 1;
    pageSize.value = size;
  },
}));

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return dayjs(dateStr).format("YYYY-MM-DD HH:mm");
};

// 获取用户已评价列表
const fetchReviews = async () => {
  loading.value = true;
  try {
    const res = await request({
      url: "/reviews/user",
      method: "get",
    });

    if (res.code === 200) {
      reviewList.value = res.data || [];
      
      // 为每条评价添加评价状态
      reviewList.value.forEach(item => {
        // 从后端响应中提取订单号，如果没有，可能需要从order对象中获取
        if (item.order && item.order.orderNumber) {
          item.orderNumber = item.order.orderNumber;
        }
        
        // 提取商品价格，如果没有，可能需要从product对象中获取
        if (item.product && item.product.price) {
          item.price = item.product.price;
        }
      });
      
      total.value = reviewList.value.length;
    } else {
      message.error(res.message || "获取评价列表失败");
    }
  } catch (error) {
    console.error("获取评价列表失败:", error);
    message.error("获取评价列表失败");
  } finally {
    loading.value = false;
  }
};

// 显示评价弹窗
const showReviewModal = (record) => {
  // 重置评价表单
  Object.assign(currentReview, {
    id: record.id,
    rating: 5,
    content: '',
    orderId: record.orderId,
    productId: record.productId,
    productName: record.productName,
    productImage: record.productImage,
    price: record.price,
    orderNumber: record.orderNumber
  });
  
  reviewModalVisible.value = true;
};

// 提交评价
const handleReviewSubmit = async () => {
  if (!currentReview.content.trim()) {
    message.warning("请输入评价内容");
    return;
  }

  submitLoading.value = true;
  try {
    const res = await request({
      url: "/reviews",
      method: "post",
      data: {
        orderId: currentReview.orderId,
        productId: currentReview.productId,
        rating: currentReview.rating,
        content: currentReview.content,
        images: ""
      },
    });

    if (res.code === 200) {
      message.success("评价提交成功");
      reviewModalVisible.value = false;
      
    } else {
      message.error(res.message || "评价提交失败");
    }
  } catch (error) {
    console.error("评价提交失败:", error);
    message.error("评价提交失败");
  } finally {
    submitLoading.value = false;
  }
};

// 判断是否可以删除评价（通常评价发布后24小时内可以删除）
const canDeleteReview = (review) => {
  if (!review.createTime) return false;
  const reviewTime = dayjs(review.createTime);
  const now = dayjs();
  return now.diff(reviewTime, 'hour') <= 24;
};

// 确认删除评价
const confirmDeleteReview = (record) => {
  Modal.confirm({
    title: '确认删除',
    icon: () => h(ExclamationCircleOutlined),
    content: '确定要删除这条评价吗？删除后将无法恢复。',
    onOk: () => deleteReview(record),
    okText: '确认',
    cancelText: '取消',
  });
};

// 删除评价
const deleteReview = async (record) => {
  try {
    const res = await request({
      url: `/reviews/${record.id}`,
      method: "delete",
    });

    if (res.code === 200) {
      message.success("删除评价成功");
      await fetchReviews();
    } else {
      message.error(res.message || "删除评价失败");
    }
  } catch (error) {
    console.error("删除评价失败:", error);
    message.error("删除评价失败");
  }
};

// 搜索
const handleSearch = () => {
  currentPage.value = 1;
};

// 重置
const handleReset = () => {
  searchForm.status = '';
  searchForm.dateRange = [];
  currentPage.value = 1;
};

// 页面初始化
onMounted(() => {
  Promise.all([fetchReviews()]);
});
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
    
    .no-review {
      color: #999;
      font-style: italic;
    }
  }

  .review-form {
    .selected-product {
      display: flex;
      margin-bottom: 20px;
      padding: 16px;
      background: #f9f9f9;
      border-radius: 4px;
      
      .product-image {
        width: 80px;
        height: 80px;
        margin-right: 16px;
        
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          border-radius: 4px;
        }
      }
      
      .product-info {
        h4 {
          margin: 0 0 8px;
        }
        
        p {
          color: #f5222d;
          margin: 0;
        }
      }
    }
    
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