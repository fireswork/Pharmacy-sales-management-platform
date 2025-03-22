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
            <a-select-option value="PENDING">待付款</a-select-option>
            <a-select-option value="PAID">已付款</a-select-option>
            <a-select-option value="DELIVERING">已发货</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
            <a-select-option value="CANCELLED">已取消</a-select-option>
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
              <a-button type="link" size="small" @click="handleView(record)"
                >查看</a-button
              >
              <template v-if="record.status === 'PAID'">
                <a-button
                  type="link"
                  size="small"
                  @click="handleDelivery(record)"
                  >发货</a-button
                >
              </template>
              <template v-if="record.status === 'DELIVERING'">
                <a-button
                  type="link"
                  size="small"
                  @click="handleComplete(record)"
                  >完成订单</a-button
                >
              </template>
              <!-- <template v-if="record.status !== 'COMPLETED' && record.status !== 'CANCELLED'">
                <a-button type="link" size="small" danger @click="handleCancel(record)">取消订单</a-button>
              </template> -->
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 订单详情弹窗 -->
    <a-modal
      v-model:visible="detailVisible"
      title="订单详情"
      :footer="null"
      width="900px"
    >
      <a-descriptions bordered :column="2">
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
      </a-descriptions>

      <div class="order-items">
        <h3>订单商品</h3>
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
          <span>商品总额：</span>
          <span>¥{{ currentOrder.totalAmount?.toFixed(2) }}</span>
        </div>
        <div class="summary-item" v-if="currentOrder.discount">
          <span>优惠金额：</span>
          <span>-¥{{ currentOrder.discount?.toFixed(2) }}</span>
        </div>
        <div class="summary-item">
          <span>实付金额：</span>
          <span class="amount"
            >¥{{ currentOrder.totalAmount?.toFixed(2) }}</span
          >
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { message } from "ant-design-vue";
import { SearchOutlined, ReloadOutlined } from "@ant-design/icons-vue";
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

// 表格列定义
const columns = [
  {
    title: "订单编号",
    dataIndex: "orderNumber",
    width: 180,
  },
  {
    title: "会员姓名",
    dataIndex: ["user", "name"],
    width: 120,
    customRender: ({ record }) => record.user?.name || record.receiverName,
  },
  {
    title: "会员等级",
    dataIndex: ["user", "memberLevel"],
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
    title: "所属店铺",
    dataIndex: ["store", "name"],
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
    width: 200,
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
      params.startDate = dayjs(searchForm.value.dateRange[0]).format(
        "YYYY-MM-DD"
      );
      params.endDate = dayjs(searchForm.value.dateRange[1]).format(
        "YYYY-MM-DD"
      );
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
      storeOptions.value = res.data || [];
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

const handleCancel = (record) => {
  updateOrderStatus(record.id, "CANCELLED");
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

    .summary-item {
      line-height: 32px;

      .amount {
        color: #f5222d;
        font-size: 20px;
        font-weight: bold;
      }
    }
  }
}
</style>
