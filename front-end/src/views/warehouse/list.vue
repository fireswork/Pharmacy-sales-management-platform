<template>
  <div class="warehouse-container">
    <a-card>
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入药品名称/编号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.category"
            style="width: 160px"
            placeholder="药品分类"
            allow-clear
          >
            <a-select-option v-for="cat in categories" :key="cat.value" :value="cat.value">
              {{ cat.label }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="searchForm.stockStatus"
            style="width: 120px"
            placeholder="库存状态"
            allow-clear
          >
            <a-select-option value="normal">正常</a-select-option>
            <a-select-option value="low">偏低</a-select-option>
            <a-select-option value="warning">警告</a-select-option>
          </a-select>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
          <a-button type="primary" @click="handleInbound">
            <template #icon><ImportOutlined /></template>
            入库登记
          </a-button>
          <a-button type="primary" danger @click="handleOutbound">
            <template #icon><ExportOutlined /></template>
            出库登记
          </a-button>
        </a-space>
      </div>

      <!-- 库存列表 -->
      <a-table
        :columns="columns"
        :data-source="stockList"
        :pagination="pagination"
        :row-key="record => record.id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 库存状态列 -->
          <template v-if="column.key === 'stockStatus'">
            <a-tag :color="getStockStatusColor(record)">
              {{ getStockStatusText(record) }}
            </a-tag>
          </template>
          
          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleStockRecord(record)">
                出入库记录
              </a-button>
              <a-button type="link" size="small" @click="handleAdjust(record)">
                库存调整
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 入库登记弹窗 -->
    <a-modal
      v-model:visible="inboundVisible"
      title="入库登记"
      @ok="handleInboundSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="inboundFormRef"
        :model="inboundForm"
        :rules="inboundRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="选择药品" name="productId" required>
          <a-select
            v-model:value="inboundForm.productId"
            placeholder="请选择药品"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="item in productOptions" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="入库数量" name="quantity" required>
          <a-input-number
            v-model:value="inboundForm.quantity"
            :min="1"
            style="width: 100%"
            placeholder="请输入入库数量"
          />
        </a-form-item>

        <a-form-item label="生产批号" name="batchNo" required>
          <a-input v-model:value="inboundForm.batchNo" placeholder="请输入生产批号" />
        </a-form-item>

        <a-form-item label="生产日期" name="productionDate" required>
          <a-date-picker
            v-model:value="inboundForm.productionDate"
            style="width: 100%"
            :disabledDate="disabledProductionDate"
          />
        </a-form-item>

        <a-form-item label="有效期至" name="expirationDate" required>
          <a-date-picker
            v-model:value="inboundForm.expirationDate"
            style="width: 100%"
            :disabledDate="disabledExpirationDate"
          />
        </a-form-item>

        <a-form-item label="供应商" name="supplierId" required>
          <a-select
            v-model:value="inboundForm.supplierId"
            placeholder="请选择供应商"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="item in supplierOptions" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="inboundForm.remark"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 出库登记弹窗 -->
    <a-modal
      v-model:visible="outboundVisible"
      title="出库登记"
      @ok="handleOutboundSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="outboundFormRef"
        :model="outboundForm"
        :rules="outboundRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="选择药品" name="productId" required>
          <a-select
            v-model:value="outboundForm.productId"
            placeholder="请选择药品"
            show-search
            :filter-option="filterOption"
            @change="handleProductSelect"
          >
            <a-select-option v-for="item in productOptions" :key="item.id" :value="item.id">
              {{ item.name }} (库存: {{ item.stock }})
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="出库数量" name="quantity" required>
          <a-input-number
            v-model:value="outboundForm.quantity"
            :min="1"
            :max="selectedProductStock"
            style="width: 100%"
            placeholder="请输入出库数量"
          />
        </a-form-item>

        <a-form-item label="出库类型" name="type" required>
          <a-select v-model:value="outboundForm.type" placeholder="请选择出库类型">
            <a-select-option value="sale">销售出库</a-select-option>
            <a-select-option value="transfer">调拨出库</a-select-option>
            <a-select-option value="damage">损坏出库</a-select-option>
            <a-select-option value="expired">过期出库</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item 
          label="目标分店" 
          name="targetStoreId" 
          v-if="outboundForm.type === 'transfer'"
          required
        >
          <a-select
            v-model:value="outboundForm.targetStoreId"
            placeholder="请选择目标分店"
          >
            <a-select-option v-for="store in storeOptions" :key="store.id" :value="store.id">
              {{ store.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="outboundForm.remark"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 库存记录弹窗 -->
    <a-modal
      v-model:visible="recordVisible"
      title="出入库记录"
      :footer="null"
      width="800px"
      :maskClosable="false"
    >
      <a-table
        :columns="recordColumns"
        :data-source="stockRecords"
        :pagination="{ pageSize: 5 }"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 'inbound' ? 'success' : 'error'">
              {{ record.type === 'inbound' ? '入库' : '出库' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { 
  SearchOutlined, 
  ReloadOutlined, 
  ImportOutlined, 
  ExportOutlined 
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

// 搜索表单
const searchForm = ref({
  keyword: '',
  category: undefined,
  stockStatus: undefined
})

// 药品分类
const categories = [
  { value: 'OTC', label: 'OTC药品' },
  { value: 'RX', label: '处方药' },
  { value: 'TCM', label: '中药' }
]

// 表格列定义
const columns = [
  {
    title: '药品编号',
    dataIndex: 'code',
    width: 120,
  },
  {
    title: '药品名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '分类',
    dataIndex: 'category',
    width: 100,
  },
  {
    title: '当前库存',
    dataIndex: 'stock',
    width: 100,
  },
  {
    title: '库存状态',
    key: 'stockStatus',
    width: 100,
  },
  {
    title: '预警阈值',
    dataIndex: 'warningThreshold',
    width: 100,
  },
  {
    title: '最近入库',
    dataIndex: 'lastInbound',
    width: 180,
  },
  {
    title: '最近出库',
    dataIndex: 'lastOutbound',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right'
  }
]

// 记录表格列定义
const recordColumns = [
  {
    title: '时间',
    dataIndex: 'time',
    width: 180,
  },
  {
    title: '类型',
    key: 'type',
    width: 100,
  },
  {
    title: '数量',
    dataIndex: 'quantity',
    width: 100,
  },
  {
    title: '操作人',
    dataIndex: 'operator',
    width: 120,
  },
  {
    title: '备注',
    dataIndex: 'remark',
  }
]

// 获取库存状态颜色
const getStockStatusColor = (record) => {
  if (record.stock === 0) return 'error'
  if (record.stock <= record.warningThreshold) return 'warning'
  return 'success'
}

// 获取库存状态文本
const getStockStatusText = (record) => {
  if (record.stock === 0) return '无库存'
  if (record.stock <= record.warningThreshold) return '偏低'
  return '正常'
}

// 模拟数据
const stockList = ref([
  {
    id: 1,
    code: 'MED001',
    name: '布洛芬缓释胶囊',
    category: 'OTC',
    stock: 1000,
    warningThreshold: 100,
    lastInbound: '2024-03-10 14:30:00',
    lastOutbound: '2024-03-15 09:20:00'
  },
  {
    id: 2,
    code: 'MED002',
    name: '感冒灵颗粒',
    category: 'OTC',
    stock: 50,
    warningThreshold: 100,
    lastInbound: '2024-03-08 16:45:00',
    lastOutbound: '2024-03-14 11:30:00'
  }
])

// 分页配置
const pagination = {
  total: stockList.value.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`,
}

// 入库表单
const inboundVisible = ref(false)
const inboundFormRef = ref()
const inboundForm = ref({
  productId: undefined,
  quantity: 1,
  batchNo: '',
  productionDate: null,
  expirationDate: null,
  supplierId: undefined,
  remark: ''
})

// 出库表单
const outboundVisible = ref(false)
const outboundFormRef = ref()
const outboundForm = ref({
  productId: undefined,
  quantity: 1,
  type: undefined,
  targetStoreId: undefined,
  remark: ''
})

// 选中药品的库存
const selectedProductStock = ref(0)

// 提交loading
const submitLoading = ref(false)

// 记录弹窗
const recordVisible = ref(false)
const stockRecords = ref([])

// 供应商选项
const supplierOptions = [
  { id: 1, name: '广州医药有限公司' },
  { id: 2, name: '深圳医药集团' }
]

// 分店选项
const storeOptions = [
  { id: 1, name: '总店' },
  { id: 2, name: '分店1' },
  { id: 3, name: '分店2' }
]

// 药品选项
const productOptions = [
  { id: 1, name: '布洛芬缓释胶囊', stock: 1000 },
  { id: 2, name: '感冒灵颗粒', stock: 50 }
]

// 日期禁用
const disabledProductionDate = (current) => {
  return current && current > dayjs().endOf('day')
}

const disabledExpirationDate = (current) => {
  return current && current < dayjs().endOf('day')
}

// 搜索过滤
const filterOption = (input, option) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// 处理函数
const handleSearch = () => {
  message.success('搜索成功')
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    category: undefined,
    stockStatus: undefined
  }
  handleSearch()
}

const handleInbound = () => {
  inboundForm.value = {
    productId: undefined,
    quantity: 1,
    batchNo: '',
    productionDate: null,
    expirationDate: null,
    supplierId: undefined,
    remark: ''
  }
  inboundVisible.value = true
}

const handleOutbound = () => {
  outboundForm.value = {
    productId: undefined,
    quantity: 1,
    type: undefined,
    targetStoreId: undefined,
    remark: ''
  }
  outboundVisible.value = true
}

const handleProductSelect = (value) => {
  const product = productOptions.find(item => item.id === value)
  selectedProductStock.value = product ? product.stock : 0
}

const handleInboundSubmit = () => {
  inboundFormRef.value.validate().then(() => {
    submitLoading.value = true
    setTimeout(() => {
      message.success('入库成功')
      inboundVisible.value = false
      submitLoading.value = false
      handleSearch()
    }, 1000)
  })
}

const handleOutboundSubmit = () => {
  outboundFormRef.value.validate().then(() => {
    submitLoading.value = true
    setTimeout(() => {
      message.success('出库成功')
      outboundVisible.value = false
      submitLoading.value = false
      handleSearch()
    }, 1000)
  })
}

const handleStockRecord = (record) => {
  stockRecords.value = [
    {
      id: 1,
      time: '2024-03-15 09:20:00',
      type: 'outbound',
      quantity: 50,
      operator: '张三',
      remark: '销售出库'
    },
    {
      id: 2,
      time: '2024-03-10 14:30:00',
      type: 'inbound',
      quantity: 1000,
      operator: '李四',
      remark: '采购入库'
    }
  ]
  recordVisible.value = true
}

const handleAdjust = (record) => {
  message.success('库存调整成功')
}
</script>

<style lang="less" scoped>
.warehouse-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}
</style> 