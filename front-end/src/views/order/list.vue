<template>
  <div class="order-container">
    <a-card>
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入订单号/会员名称"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="订单状态"
            allow-clear
          >
            <a-select-option value="PENDING">待发货</a-select-option>
            <a-select-option value="DELIVERING">已发货</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
          </a-select>
          <a-select
            v-model:value="searchForm.storeId"
            style="width: 160px"
            placeholder="所属店铺"
            allow-clear
          >
            <a-select-option
              v-for="store in storeOptions"
              :key="store.id"
              :value="store.id"
            >
              {{ store.name }}
            </a-select-option>
          </a-select>
          <a-range-picker
            v-model:value="searchForm.dateRange"
            style="width: 240px"
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

      <!-- 订单列表 -->
      <a-table
        :columns="columns"
        :data-source="orderList"
        :pagination="pagination"
        :row-key="(record) => record.id"
        :loading="loading"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 订单状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="getOrderStatusColor(record.status)">
              {{ getOrderStatusText(record.status) }}
            </a-tag>
          </template>

          <!-- 支付方式列 -->
          <template v-if="column.key === 'paymentMethod'">
            <a-tag>{{ getPaymentMethodText(record.paymentMethod) }}</a-tag>
          </template>

          <!-- 会员等级列 -->
          <template v-if="column.key === 'memberLevel'">
            <a-tag :color="getMemberLevelColor(record.user?.memberLevel)">
              {{ getMemberLevelText(record.user?.memberLevel) }}
            </a-tag>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <template v-if="record.status === 'PAID' && userRole === 'ADMIN'">
                <a-button type="link" size="small" @click="handleDelivery(record)">发货</a-button>
              </template>
              <template v-if="record.status === 'DELIVERING' && userRole === 'USER'">
                <a-button type="link" size="small" @click="handleViewReviews(record)">查看评价</a-button>
                <a-button type="link" size="small" @click="handleReview(record)">评价</a-button>
                <a-button type="link" size="small" @click="handleComplete(record)">完成订单</a-button>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 订单详情弹窗 -->
    <a-modal
      v-model:visible="detailVisible"
      title="订单商品"
      :footer="null"
      width="900px"
    >
      <!-- <a-descriptions bordered :column="2">
        <a-descriptions-item label="订单编号">{{
          currentOrder.orderNumber
        }}</a-descriptions-item>
        <a-descriptions-item label="下单时间">{{
          formatDate(currentOrder.createTime)
        }}</a-descriptions-item>
        <a-descriptions-item label="会员姓名">{{
          currentOrder.user?.name || currentOrder.receiverName
        }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{
          currentOrder.user?.phone || currentOrder.receiverPhone
        }}</a-descriptions-item>
        <a-descriptions-item label="订单状态">
          <a-tag :color="getOrderStatusColor(currentOrder.status)">
            {{ getOrderStatusText(currentOrder.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="支付方式">
          <a-tag>{{ getPaymentMethodText(currentOrder.paymentMethod) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="所属店铺">{{
          currentOrder.store?.name
        }}</a-descriptions-item>
        <a-descriptions-item label="收货地址">{{
          currentOrder.deliveryAddress
        }}</a-descriptions-item>
      </a-descriptions> -->

      <div class="order-items">
        <!-- <h3>订单商品</h3> -->
        <a-table
          :columns="itemColumns"
          :data-source="currentOrder.items || []"
          :pagination="false"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'image'">
              <img
                :src="record.image"
                :alt="record.productName"
                class="product-image"
              />
            </template>
            <template v-if="column.key === 'amount'">
              ¥{{ calculateItemAmount(record).toFixed(2) }}
            </template>
          </template>
        </a-table>
      </div>

      <div class="order-summary">
        <div class="summary-item">
          <span class="summary-label">商品总额：</span>
          <span class="summary-value">¥{{ currentOrder.totalAmount?.toFixed(2) }}</span>
        </div>
        <div class="summary-item" v-if="currentOrder.discount">
          <span class="summary-label">优惠金额：</span>
          <span class="summary-value discount">-¥{{ currentOrder.discount?.toFixed(2) }}</span>
        </div>
        <div class="summary-item total">
          <span class="summary-label">实付金额：</span>
          <span class="summary-value amount">¥{{ currentOrder.totalAmount?.toFixed(2) }}</span>
        </div>
      </div>
    </a-modal>

    <!-- 评价弹窗 -->
    <a-modal
      v-model:visible="reviewVisible"
      title="商品评价"
      :width="700"
      @ok="submitReview"
      :okText="reviewableItems.length ? '提交评价' : '确定'"
      :cancelText="'取消'"
      :confirmLoading="reviewSubmitting"
    >
      <div v-if="reviewableItems.length > 0">
        <div class="review-product-selector">
          <span class="label">选择评价商品：</span>
          <a-select
            v-model:value="currentReviewItem.productId"
            style="width: 300px"
            @change="handleReviewItemChange"
          >
            <a-select-option
              v-for="item in reviewableItems"
              :key="item.productId"
              :value="item.productId"
            >
              {{ item.productName }}
            </a-select-option>
          </a-select>
        </div>

        <div class="review-form" v-if="currentReviewItem.productId">
          <div class="selected-product">
            <div class="product-image">
              <img :src="currentReviewItem.image" :alt="currentReviewItem.productName" />
            </div>
            <div class="product-info">
              <h4>{{ currentReviewItem.productName }}</h4>
              <p>¥{{ currentReviewItem.price?.toFixed(2) }}</p>
            </div>
          </div>

          <div class="rating-section">
            <span class="label">评分：</span>
            <a-rate v-model:value="reviewForm.rating" />
          </div>

          <div class="content-section">
            <a-textarea
              v-model:value="reviewForm.content"
              placeholder="请输入您对商品的评价"
              :rows="4"
              :maxLength="500"
              show-count
            />
          </div>
        </div>

        <div class="no-reviewable-items" v-else>
          <a-empty description="请选择需要评价的商品" />
        </div>
      </div>
      <div v-else class="no-reviewable-items">
        <a-empty description="该订单所有商品已完成评价" />
      </div>
    </a-modal>

    <!-- 图片预览 -->
    <a-modal
      :visible="previewVisible"
      :footer="null"
      @cancel="previewVisible = false"
    >
      <img alt="预览图片" style="width: 100%" :src="previewImage" />
    </a-modal>

    <!-- 新增查看评价弹窗 -->
    <a-modal
      v-model:visible="reviewListVisible"
      title="订单评价"
      :footer="null"
      width="800px"
    >
      <div class="review-list-header">
        <h3>订单号: {{ currentReviewOrder.orderNumber }}</h3>
        <p>下单时间: {{ formatDate(currentReviewOrder.createTime) }}</p>
      </div>

      <a-empty v-if="orderReviews.length === 0" description="该订单暂无评价" />

      <div v-else class="review-list">
        <a-list
          :data-source="orderReviews"
          :item-layout="'vertical'"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <div class="review-product-title">
                    <span>{{ item.productName }}</span>
                    <a-button 
                      v-if="canEditReview(item)" 
                      type="link" 
                      @click="handleEditReview(item)"
                    >
                      修改评价
                    </a-button>
                  </div>
                </template>
                <template #description>
                  <a-rate :value="item.rating" disabled />
                  <span class="review-date">{{ formatDate(item.createTime) }}</span>
                </template>
                <template #avatar>
                  <a-avatar :src="item.productImage" shape="square" :size="64" />
                </template>
              </a-list-item-meta>
              
              <div class="review-content">{{ item.content }}</div>
              
              <div v-if="item.images" class="review-images">
                <a-image-preview-group>
                  <a-image
                    v-for="(img, index) in item.images.split(',')"
                    :key="index"
                    :width="80"
                    :src="img"
                  />
                </a-image-preview-group>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </a-modal>

    <!-- 新增修改评价弹窗 -->
    <a-modal
      v-model:visible="editReviewVisible"
      title="修改评价"
      @ok="submitEditReview"
      :okText="'提交修改'"
      :cancelText="'取消'"
      :confirmLoading="reviewSubmitting"
      width="700px"
    >
      <div class="edit-review-form" v-if="currentEditReview.id">
        <div class="selected-product">
          <div class="product-image">
            <img :src="currentEditReview.productImage" :alt="currentEditReview.productName" />
          </div>
          <div class="product-info">
            <h4>{{ currentEditReview.productName }}</h4>
          </div>
        </div>

        <div class="rating-section">
          <span class="label">评分：</span>
          <a-rate v-model:value="editReviewForm.rating" />
        </div>

        <div class="content-section">
          <a-textarea
            v-model:value="editReviewForm.content"
            placeholder="请输入您对商品的评价"
            :rows="4"
            :maxLength="500"
            show-count
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { message } from "ant-design-vue";
import { SearchOutlined, ReloadOutlined, PlusOutlined } from "@ant-design/icons-vue";
import dayjs from "dayjs";
import request from "@/utils/axios";

// 搜索表单
const searchForm = ref({
  keyword: "",
  status: undefined,
  storeId: undefined,
  dateRange: [],
});

// 状态管理
const loading = ref(false);
const orderList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const storeOptions = ref([]);
const detailVisible = ref(false);
const currentOrder = ref({});

// 评价相关
const reviewVisible = ref(false);
const reviewListVisible = ref(false);
const editReviewVisible = ref(false);
const reviewSubmitting = ref(false);
const reviewableItems = ref([]);
const orderReviews = ref([]);
const currentReviewItem = ref({});
const currentReviewOrder = ref({});
const currentEditReview = ref({});
const reviewForm = reactive({
  productId: null,
  orderId: null,
  rating: 5,
  content: "",
  images: "",
});
const editReviewForm = reactive({
  id: null,
  rating: 5,
  content: "",
  images: "",
});
const fileList = ref([]);
const editFileList = ref([]);
const previewVisible = ref(false);
const previewImage = ref("");
const userRole = ref(localStorage.getItem('userRole'))

// 表格列定义
const columns = [
  {
    title: "订单编号",
    dataIndex: "orderNumber",
    width: 180,
  },
  {
    title: "会员等级",
    dataIndex: "memberLevel",
    key: "memberLevel",
    width: 100,
  },
  {
    title: "订单金额",
    dataIndex: "totalAmount",
    width: 120,
    customRender: ({ text }) => `¥${(text || 0).toFixed(2)}`,
  },
  {
    title: "支付方式",
    dataIndex: "paymentMethod",
    key: "paymentMethod",
    width: 100,
  },
  {
    title: "订单状态",
    dataIndex: "status",
    key: "status",
    width: 100,
  },
  {
    title: "下单店铺",
    dataIndex: "storeName",
    width: 150,
  },
  {
    title: "下单时间",
    dataIndex: "createTime",
    width: 180,
    customRender: ({ text }) => formatDate(text),
  },
  {
    title: "操作",
    key: "action",
    fixed: "right",
    width: 100,
  },
];

// 订单商品列定义
const itemColumns = [
  {
    title: "商品图片",
    dataIndex: "image",
    key: "image",
    width: 100,
  },
  {
    title: "商品名称",
    dataIndex: "productName",
  },
  {
    title: "单价",
    dataIndex: "price",
    width: 120,
    customRender: ({ text }) => `¥${(text || 0).toFixed(2)}`,
  },
  {
    title: "数量",
    dataIndex: "quantity",
    width: 100,
  },
  {
    title: "金额",
    key: "amount",
    width: 120,
  },
];

// 会员等级配置
const memberLevels = [
  { value: "REGULAR", label: "普通会员", color: "" },
  { value: "SILVER", label: "白银会员", color: "cyan" },
  { value: "GOLD", label: "黄金会员", color: "gold" },
  { value: "PLATINUM", label: "铂金会员", color: "purple" },
];

// 分页配置
const pagination = computed(() => ({
  total: total.value,
  current: currentPage.value,
  pageSize: pageSize.value,
  showSizeChanger: true,
  pageSizeOptions: ["10", "20", "50"],
  showTotal: (total) => `共 ${total} 条记录`,
  onChange: (page, size) => {
    currentPage.value = page;
    pageSize.value = size;
    fetchOrders();
  },
  onShowSizeChange: (current, size) => {
    currentPage.value = 1;
    pageSize.value = size;
    fetchOrders();
  },
}));

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return dayjs(dateStr).format("YYYY-MM-DD HH:mm");
};

// 获取订单状态颜色
const getOrderStatusColor = (status) => {
  const colors = {
    PENDING: "orange",
    PAID: "blue",
    DELIVERING: "green",
    COMPLETED: "gray",
    CANCELLED: "red",
  };
  return colors[status] || "default";
};

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const texts = {
    PENDING: "待付款",
    PAID: "已付款",
    DELIVERING: "已发货",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
  };
  return texts[status] || status;
};

// 获取支付方式文本
const getPaymentMethodText = (method) => {
  const texts = {
    cash: "现金",
    wechat: "微信",
    alipay: "支付宝",
    bank: "银行卡",
    cbank: "银行卡",
  };
  return texts[method] || method;
};

// 获取会员等级颜色
const getMemberLevelColor = (level) => {
  const levelConfig = memberLevels.find((item) => item.value === level);
  return levelConfig ? levelConfig.color : "";
};

// 获取会员等级文本
const getMemberLevelText = (level) => {
  const levelConfig = memberLevels.find((item) => item.value === level);
  return levelConfig ? levelConfig.label : "普通用户";
};

// 计算订单项金额
const calculateItemAmount = (item) => {
  return (item.price || 0) * (item.quantity || 0);
};

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
    };

    if (searchForm.value.keyword) {
      params.keyword = searchForm.value.keyword;
    }

    if (searchForm.value.status) {
      params.status = searchForm.value.status;
    }

    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId;
    }

    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.value.dateRange[0]).format("YYYY-MM-DD");
      params.endDate = dayjs(searchForm.value.dateRange[1]).format("YYYY-MM-DD");
    }

    const res = await request({
      url: "/orders",
      method: "get",
      params,
    });

    if (res.code === 200) {
      orderList.value = res.data || [];
      total.value = res.total || orderList.value.length;
    } else {
      message.error(res.message || "获取订单列表失败");
    }
  } catch (error) {
    console.error("获取订单列表失败:", error);
    message.error("获取订单列表失败");
  } finally {
    loading.value = false;
  }
};

// 获取店铺列表
const fetchStores = async () => {
  try {
    const res = await request({
      url: "/store",
      method: "get",
    });

    if (res.code === 200) {
      storeOptions.value = res.data.content || [];
    } else {
      console.error("获取店铺列表失败:", res.message);
    }
  } catch (error) {
    console.error("获取店铺列表失败:", error);
  }
};

// 获取订单详情
const fetchOrderDetail = async (id) => {
  try {
    const res = await request({
      url: `/orders/${id}`,
      method: "get",
    });

    if (res.code === 200) {
      currentOrder.value = res.data || {};
      detailVisible.value = true;
    } else {
      message.error(res.message || "获取订单详情失败");
    }
  } catch (error) {
    console.error("获取订单详情失败:", error);
    message.error("获取订单详情失败");
  }
};

// 更新订单状态
const updateOrderStatus = async (id, status) => {
  try {
    const res = await request({
      url: `/orders/${id}/status`,
      method: "put",
      params: {
        status,
      },
    });

    if (res.code === 200) {
      message.success("操作成功");
      fetchOrders();
    } else {
      message.error(res.message || "操作失败");
    }
  } catch (error) {
    console.error("更新订单状态失败:", error);
    message.error("操作失败");
  }
};

// 选择评价商品
const handleReviewItemChange = (productId) => {
  const item = reviewableItems.value.find(item => item.productId === productId);
  if (item) {
    currentReviewItem.value = { ...item };
    reviewForm.productId = productId;
    
    // 重置评价内容和评分
    reviewForm.rating = 5;
    reviewForm.content = "";
  }
};

// 处理评价提交
const submitReview = async () => {
  if (reviewableItems.value.length === 0) {
    reviewVisible.value = false;
    return;
  }

  if (!reviewForm.productId) {
    message.error("请选择要评价的商品");
    return;
  }

  if (!reviewForm.rating) {
    message.error("请为商品打分");
    return;
  }

  reviewSubmitting.value = true;
  
  try {
    const res = await request({
      url: "/reviews",
      method: "post",
      data: {
        orderId: reviewForm.orderId,
        productId: reviewForm.productId,
        rating: reviewForm.rating,
        content: reviewForm.content,
        images: "" // 将图片设为空字符串，不再上传图片
      },
    });

    if (res.code === 200) {
      message.success("评价成功");
      reviewVisible.value = false;
      resetReviewForm();
    } else {
      message.error(res.message || "评价失败");
    }
  } catch (error) {
    console.error("评价失败:", error);
    message.error("评价失败");
  } finally {
    reviewSubmitting.value = false;
  }
};

// 重置评价表单
const resetReviewForm = () => {
  reviewForm.productId = null;
  reviewForm.orderId = null;
  reviewForm.rating = 5;
  reviewForm.content = "";
};

// 处理函数
const handleSearch = () => {
  currentPage.value = 1;
  fetchOrders();
};

const handleReset = () => {
  searchForm.value = {
    keyword: "",
    status: undefined,
    storeId: undefined,
    dateRange: [],
  };
  currentPage.value = 1;
  fetchOrders();
};

const handleView = (record) => {
  fetchOrderDetail(record.id);
};

const handleDelivery = (record) => {
  updateOrderStatus(record.id, "DELIVERING");
};

const handleComplete = (record) => {
  updateOrderStatus(record.id, "COMPLETED");
};

// 获取可评价商品列表
const fetchReviewableItems = async (orderId) => {
  try {
    const res = await request({
      url: `/orders/${orderId}/reviewable-items`,
      method: "get",
    });

    if (res.code === 200) {
      reviewableItems.value = res.data || [];
      
      // 如果有可评价商品，默认选中第一个
      if (reviewableItems.value.length > 0) {
        const firstItem = reviewableItems.value[0];
        currentReviewItem.value = { ...firstItem };
        reviewForm.productId = firstItem.productId;
      } else {
        // 如果没有可评价商品，清空当前选中项
        currentReviewItem.value = {};
        reviewForm.productId = null;
      }
    } else {
      message.error(res.message || "获取可评价商品失败");
    }
  } catch (error) {
    console.error("获取可评价商品失败:", error);
    message.error("获取可评价商品失败");
    reviewableItems.value = [];
  }
};

const handleReview = (record) => {
  resetReviewForm();
  reviewForm.orderId = record.id;
  fetchReviewableItems(record.id); // 获取可评价商品列表
  reviewVisible.value = true;
};

// 获取订单评价列表
const fetchOrderReviews = async (orderId) => {
  try {
    const res = await request({
      url: `/reviews/order/${orderId}`,
      method: "get",
    });

    if (res.code === 200) {
      orderReviews.value = res.data || [];
    } else {
      message.error(res.message || "获取订单评价失败");
    }
  } catch (error) {
    console.error("获取订单评价失败:", error);
    message.error("获取订单评价失败");
  }
};

// 判断是否可以编辑评价（通常评价发布后24小时内可以编辑）
const canEditReview = (review) => {
  // 创建时间在24小时内的评价可以编辑
  const reviewTime = dayjs(review.createTime);
  const now = dayjs();
  return now.diff(reviewTime, 'hour') <= 24;
};

// 处理查看评价
const handleViewReviews = (record) => {
  currentReviewOrder.value = record;
  fetchOrderReviews(record.id);
  reviewListVisible.value = true;
};

// 处理修改评价
const handleEditReview = (review) => {
  currentEditReview.value = review;
  editReviewForm.id = review.id;
  editReviewForm.rating = review.rating;
  editReviewForm.content = review.content;
  editReviewForm.images = review.images;
  
  editReviewVisible.value = true;
};

// 提交修改评价
const submitEditReview = async () => {
  if (!editReviewForm.rating) {
    message.error("请为商品打分");
    return;
  }

  reviewSubmitting.value = true;
  
  try {
    const res = await request({
      url: `/reviews/${editReviewForm.id}`,
      method: "put",
      data: {
        rating: editReviewForm.rating,
        content: editReviewForm.content,
        images: "" // 将图片设为空字符串，不再上传图片
      },
    });

    if (res.code === 200) {
      message.success("修改评价成功");
      editReviewVisible.value = false;
      
      // 刷新评价列表
      fetchOrderReviews(currentReviewOrder.value.id);
    } else {
      message.error(res.message || "修改评价失败");
    }
  } catch (error) {
    console.error("修改评价失败:", error);
    message.error("修改评价失败");
  } finally {
    reviewSubmitting.value = false;
  }
};

// 页面初始化
onMounted(() => {
  fetchStores();
  fetchOrders();
});
</script>

<style lang="less" scoped>
:deep(.product-image) {
  width: 100px;
  object-fit: cover;
  border-radius: 4px;
}
.rating-section {
  margin-bottom: 16px;
}
.selected-product {
  img {
    width: 200px;
    margin: 20px 0;
  }
}
.order-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
  }

  .order-items {
    margin-top: 24px;

    h3 {
      margin-bottom: 16px;
    }
  }

  .order-summary {
    margin-top: 24px;
    text-align: right;
    padding-right: 24px;
    background: #f5f5f5;
    padding: 16px 24px;
    border-radius: 4px;

    .summary-item {
      line-height: 32px;
      display: flex;
      justify-content: flex-end;
      align-items: center;
      
      .summary-label {
        font-size: 14px;
        margin-right: 16px;
        min-width: 80px;
        text-align: right;
      }
      
      .summary-value {
        font-size: 14px;
        min-width: 80px;
        text-align: right;
        
        &.discount {
          color: #52c41a;
        }
      }
      
      &.total {
        margin-top: 8px;
        border-top: 1px dashed #d9d9d9;
        padding-top: 8px;
        
        .summary-label {
          font-weight: bold;
        }
        
        .amount {
          color: #f5222d;
          font-size: 20px;
          font-weight: bold;
        }
      }
    }
  }
  
  .review-product-selector {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    
    .label {
      margin-right: 10px;
      min-width: 100px;
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
      display: flex;
      align-items: center;
      
      .label {
        margin-right: 10px;
        min-width: 40px;
      }
    }
    
    .content-section {
      margin-bottom: 16px;
    }
  }
  
  .no-reviewable-items {
    padding: 40px 0;
    text-align: center;
  }

  .review-list-header {
    margin-bottom: 20px;
    
    h3 {
      margin-bottom: 8px;
    }
    
    p {
      color: #999;
    }
  }

  .review-list {
    :deep(.ant-list-item-meta) {
      align-items: flex-start;
    }
    
    .review-product-title {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .review-date {
      margin-left: 10px;
      color: #999;
      font-size: 12px;
    }
    
    .review-content {
      margin: 16px 0;
      white-space: pre-wrap;
    }
    
    .review-images {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 10px;
    }
  }

  .edit-review-form {
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
      }
    }
    
    .rating-section, .content-section {
      margin-bottom: 16px;
    }
    
    .rating-section {
      display: flex;
      align-items: center;
      margin-bottom: 16px;
      .label {
        margin-right: 10px;
        min-width: 40px;
      }
    }
  }
}
</style>
