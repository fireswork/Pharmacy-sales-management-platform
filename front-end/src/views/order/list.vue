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
            <a-select-option value="pending">待付款</a-select-option>
            <a-select-option value="paid">已付款</a-select-option>
            <a-select-option value="completed">已完成</a-select-option>
            <a-select-option value="cancelled">已取消</a-select-option>
          </a-select>
          <a-select
            v-model:value="searchForm.storeId"
            style="width: 160px"
            placeholder="所属店铺"
            allow-clear
          >
            <a-select-option v-for="store in storeOptions" :key="store.id" :value="store.id">
              {{ store.name }}
            </a-select-option>
          </a-select>
          <a-range-picker
            v-model:value="searchForm.dateRange"
            style="width: 240px"
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

      <!-- 订单列表 -->
      <a-table
        :columns="columns"
        :data-source="orderList"
        :pagination="pagination"
        :row-key="record => record.id"
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
            <a-tag :color="getMemberLevelColor(record.memberLevel)">
              {{ getMemberLevelText(record.memberLevel) }}
            </a-tag>
          </template>
          
          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <template v-if="record.status === 'pending'">
                <a-button type="link" size="small" @click="handleConfirmPayment(record)">确认付款</a-button>
                <a-button type="link" size="small" danger @click="handleCancel(record)">取消订单</a-button>
              </template>
              <template v-if="record.status === 'paid'">
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
      title="订单详情"
      :footer="null"
      width="900px"
    >
      <a-descriptions bordered :column="2">
        <a-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="下单时间">{{ currentOrder.createTime }}</a-descriptions-item>
        <a-descriptions-item label="会员姓名">{{ currentOrder.memberName }}</a-descriptions-item>
        <a-descriptions-item label="会员手机">{{ currentOrder.memberPhone }}</a-descriptions-item>
        <a-descriptions-item label="订单状态">
          <a-tag :color="getOrderStatusColor(currentOrder.status)">
            {{ getOrderStatusText(currentOrder.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="支付方式">
          <a-tag>{{ getPaymentMethodText(currentOrder.paymentMethod) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="所属店铺">{{ currentOrder.storeName }}</a-descriptions-item>
        <a-descriptions-item label="操作员">{{ currentOrder.operatorName }}</a-descriptions-item>
      </a-descriptions>

      <div class="order-items">
        <h3>订单商品</h3>
        <a-table
          :columns="itemColumns"
          :data-source="currentOrder.items"
          :pagination="false"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">
              ¥{{ record.amount.toFixed(2) }}
            </template>
          </template>
        </a-table>
      </div>

      <div class="order-summary">
        <div class="summary-item">
          <span>商品总额：</span>
          <span>¥{{ currentOrder.totalAmount }}</span>
        </div>
        <div class="summary-item">
          <span>会员折扣：</span>
          <span>-¥{{ currentOrder.discount }}</span>
        </div>
        <div class="summary-item">
          <span>实付金额：</span>
          <span class="amount">¥{{ currentOrder.actualAmount }}</span>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

// 搜索表单
const searchForm = ref({
  keyword: '',
  status: undefined,
  storeId: undefined,
  dateRange: []
})

// 表格列定义
const columns = [
  {
    title: '订单编号',
    dataIndex: 'orderNo',
    width: 180
  },
  {
    title: '会员姓名',
    dataIndex: 'memberName',
    width: 120
  },
  {
    title: '会员等级',
    dataIndex: 'memberLevel',
    key: 'memberLevel',
    width: 100
  },
  {
    title: '订单金额',
    dataIndex: 'totalAmount',
    width: 120,
    customRender: ({ text }) => `¥${text.toFixed(2)}`
  },
  {
    title: '实付金额',
    dataIndex: 'actualAmount',
    width: 120,
    customRender: ({ text }) => `¥${text.toFixed(2)}`
  },
  {
    title: '支付方式',
    dataIndex: 'paymentMethod',
    key: 'paymentMethod',
    width: 100
  },
  {
    title: '订单状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '所属店铺',
    dataIndex: 'storeName',
    width: 150
  },
  {
    title: '下单时间',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 200
  }
]

// 订单商品列定义
const itemColumns = [
  {
    title: '商品编号',
    dataIndex: 'code',
    width: 120
  },
  {
    title: '商品名称',
    dataIndex: 'name'
  },
  {
    title: '单价',
    dataIndex: 'price',
    width: 120,
    customRender: ({ text }) => `¥${text.toFixed(2)}`
  },
  {
    title: '数量',
    dataIndex: 'quantity',
    width: 100
  },
  {
    title: '金额',
    dataIndex: 'amount',
    key: 'amount',
    width: 120
  }
]

// 店铺选项
const storeOptions = [
  { id: 1, name: '总店' },
  { id: 2, name: '分店1' },
  { id: 3, name: '分店2' }
]

// 会员等级配置
const memberLevels = [
  { value: 'bronze', label: '普通会员', color: '' },
  { value: 'silver', label: '白银会员', color: 'cyan' },
  { value: 'gold', label: '黄金会员', color: 'gold' },
  { value: 'platinum', label: '铂金会员', color: 'purple' }
]

// 订单列表数据
const orderList = ref([
  {
    id: 1,
    orderNo: 'O202403150001',
    memberName: '张三',
    memberLevel: 'gold',
    totalAmount: 500,
    actualAmount: 450,
    paymentMethod: 'wechat',
    status: 'completed',
    storeName: '总店',
    createTime: '2024-03-15 10:20:00'
  },
  // ... 其他订单数据
])

// 当前查看的订单
const currentOrder = ref({})

// 弹窗控制
const detailVisible = ref(false)

// 分页配置
const pagination = {
  total: orderList.value.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`
}

// 获取订单状态颜色
const getOrderStatusColor = (status) => {
  const colors = {
    pending: 'warning',
    paid: 'processing',
    completed: 'success',
    cancelled: 'default'
  }
  return colors[status]
}

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const texts = {
    pending: '待付款',
    paid: '已付款',
    completed: '已完成',
    cancelled: '已取消'
  }
  return texts[status]
}

// 获取支付方式文本
const getPaymentMethodText = (method) => {
  const texts = {
    cash: '现金',
    wechat: '微信',
    alipay: '支付宝',
    card: '银行卡'
  }
  return texts[method]
}

// 获取会员等级颜色
const getMemberLevelColor = (level) => {
  const levelConfig = memberLevels.find(item => item.value === level)
  return levelConfig ? levelConfig.color : ''
}

// 获取会员等级文本
const getMemberLevelText = (level) => {
  const levelConfig = memberLevels.find(item => item.value === level)
  return levelConfig ? levelConfig.label : ''
}

// 处理函数
const handleSearch = () => {
  message.success('搜索成功')
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: undefined,
    storeId: undefined,
    dateRange: []
  }
  handleSearch()
}

const handleView = (record) => {
  currentOrder.value = {
    ...record,
    memberPhone: '13800138000',
    operatorName: '李四',
    discount: 50,
    items: [
      {
        id: 1,
        code: 'MED001',
        name: '布洛芬缓释胶囊',
        price: 25.00,
        quantity: 2,
        amount: 50.00
      },
      {
        id: 2,
        code: 'MED002',
        name: '感冒灵颗粒',
        price: 15.00,
        quantity: 3,
        amount: 45.00
      }
    ]
  }
  detailVisible.value = true
}

const handleConfirmPayment = (record) => {
  record.status = 'paid'
  message.success('已确认付款')
}

const handleComplete = (record) => {
  record.status = 'completed'
  message.success('订单已完成')
}

const handleCancel = (record) => {
  record.status = 'cancelled'
  message.success('订单已取消')
}
</script>

<style lang="less" scoped>
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