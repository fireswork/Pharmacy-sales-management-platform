<template>
  <div class="finance-container">
    <!-- 筛选条件 -->
    <a-card class="filter-section">
      <a-space>
        <a-select
          v-model:value="searchForm.storeId"
          style="width: 160px"
          placeholder="选择店铺"
          allow-clear
          @change="handleStoreChange"
        >
          <a-select-option v-for="store in storeOptions" :key="store.id" :value="store.id">
            {{ store.name }}
          </a-select-option>
        </a-select>
        <a-range-picker
          v-model:value="searchForm.dateRange"
          style="width: 240px"
          :disabled-date="disabledDate"
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
    </a-card>

    <!-- 数据概览 -->
    <div class="statistics-cards">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card>
            <statistic
              title="今日销售额"
              :value="statistics.todaySales"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#3f8600' }"
            >
              <template #suffix>
                <span v-if="statistics.salesGrowth > 0" class="growth up">
                  <ArrowUpOutlined /> {{ statistics.salesGrowth }}%
                </span>
                <span v-else class="growth down">
                  <ArrowDownOutlined /> {{ Math.abs(statistics.salesGrowth) }}%
                </span>
              </template>
            </statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <statistic
              title="今日订单数"
              :value="statistics.todayOrders"
              :value-style="{ color: '#1890ff' }"
            >
              <template #suffix>
                <span v-if="statistics.ordersGrowth > 0" class="growth up">
                  <ArrowUpOutlined /> {{ statistics.ordersGrowth }}%
                </span>
                <span v-else class="growth down">
                  <ArrowDownOutlined /> {{ Math.abs(statistics.ordersGrowth) }}%
                </span>
              </template>
            </statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <statistic
              title="今日客单价"
              :value="statistics.averageOrder"
              :precision="2"
              prefix="¥"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <statistic
              title="今日毛利率"
              :value="statistics.grossProfitRate"
              suffix="%"
              :precision="2"
              :value-style="{ color: '#cf1322' }"
            />
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 销售趋势图 -->
    <a-card class="chart-section" title="销售趋势">
      <div ref="salesChart" class="chart"></div>
    </a-card>

    <!-- 支付方式占比 -->
    <a-row :gutter="16" class="chart-section">
      <a-col :span="12">
        <a-card title="支付方式占比">
          <div ref="paymentChart" class="chart"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="热销商品TOP10">
          <div ref="productsChart" class="chart"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 销售明细表格 -->
    <a-card class="table-section" title="销售明细">
      <a-table
        :columns="columns"
        :data-source="salesList"
        :pagination="pagination"
        :loading="loading"
        :row-key="record => record.id"
        bordered
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'paymentMethod'">
            <a-tag>{{ getPaymentMethodText(record.paymentMethod) }}</a-tag>
          </template>
          <template v-if="column.key === 'profit'">
            <span :style="{ color: record.profit >= 0 ? '#3f8600' : '#cf1322' }">
              {{ record.profit >= 0 ? '+' : '' }}¥{{ record.profit.toFixed(2) }}
            </span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { message, Statistic } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import axios from '@/utils/axios'

// 门店列表
const storeOptions = ref([])
const loading = ref(false)

// 搜索表单
const searchForm = ref({
  storeId: undefined,
  dateRange: []
})

// 统计数据
const statistics = ref({
  todaySales: 0,
  salesGrowth: 0,
  todayOrders: 0,
  ordersGrowth: 0,
  averageOrder: 0,
  grossProfitRate: 0
})

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

// 图表实例
const salesChart = ref(null)
const paymentChart = ref(null)
const productsChart = ref(null)
let salesChartInstance = null
let paymentChartInstance = null
let productsChartInstance = null

// 销售明细数据
const salesList = ref([])

// 表格列定义
const columns = [
  {
    title: '订单编号',
    dataIndex: 'orderNo',
    width: 180
  },
  {
    title: '店铺',
    dataIndex: 'storeName',
    width: 150
  },
  {
    title: '销售金额',
    dataIndex: 'amount',
    width: 120,
    align: 'right',
    customRender: ({ text }) => `¥${text.toFixed(2)}`
  },
  {
    title: '支付方式',
    dataIndex: 'paymentMethod',
    width: 120,
    key: 'paymentMethod'
  },
  {
    title: '毛利',
    dataIndex: 'profit',
    width: 120,
    align: 'right',
    key: 'profit'
  },
  {
    title: '销售时间',
    dataIndex: 'createTime',
    width: 180
  }
]

// 获取门店列表
const fetchStores = async () => {
  try {
    const res = await axios.get('/store')
    storeOptions.value = res.data.content || []
  } catch (error) {
    message.error('获取门店列表失败')
  }
}

// 获取财务概览数据
const fetchStatistics = async () => {
  try {
    const params = {}
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId
    }
    
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.value.dateRange[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(searchForm.value.dateRange[1]).format('YYYY-MM-DD')
    }
    
    const res = await axios.get('/finance/statistics', { params })
    if (res.code === 200) {
      statistics.value = res.data
    }
  } catch (error) {
    message.error('获取财务统计数据失败')
  }
}

// 获取销售明细
const fetchSalesList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize
    }
    
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId
    }
    
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.value.dateRange[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(searchForm.value.dateRange[1]).format('YYYY-MM-DD')
    }
    
    const res = await axios.get('/finance/sales', { params })
    if (res.code === 200) {
      salesList.value = res.data.content || []
      pagination.total = res.data.totalElements || 0
    }
  } catch (error) {
    message.error('获取销售明细失败')
  } finally {
    loading.value = false
  }
}

// 获取销售趋势数据
const fetchSalesTrend = async () => {
  try {
    const params = {}
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId
    }
    
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.value.dateRange[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(searchForm.value.dateRange[1]).format('YYYY-MM-DD')
    }
    
    const res = await axios.get('/finance/trend', { params })
    if (res.code === 200) {
      updateSalesChart(res.data)
    }
  } catch (error) {
    message.error('获取销售趋势数据失败')
  }
}

// 获取支付方式占比
const fetchPaymentMethods = async () => {
  try {
    const params = {}
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId
    }
    
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.value.dateRange[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(searchForm.value.dateRange[1]).format('YYYY-MM-DD')
    }
    
    const res = await axios.get('/finance/payment-methods', { params })
    if (res.code === 200) {
      updatePaymentChart(res.data)
    }
  } catch (error) {
    message.error('获取支付方式数据失败')
  }
}

// 获取热销商品
const fetchHotProducts = async () => {
  try {
    const params = {}
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId
    }
    
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.value.dateRange[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(searchForm.value.dateRange[1]).format('YYYY-MM-DD')
    }
    
    const res = await axios.get('/finance/hot-products', { params })
    if (res.code === 200) {
      updateProductsChart(res.data)
    }
  } catch (error) {
    message.error('获取热销商品数据失败')
  }
}

// 初始化图表
const initCharts = () => {
  // 销售趋势图
  salesChartInstance = echarts.init(salesChart.value)
  salesChartInstance.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['销售额', '订单数']
    },
    xAxis: {
      type: 'category',
      data: ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', 
             '14:00', '16:00', '18:00', '20:00', '22:00']
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        axisLabel: {
          formatter: '¥{value}'
        }
      },
      {
        type: 'value',
        name: '订单数',
        position: 'right'
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
      }
    ]
  })

  // 支付方式占比
  paymentChartInstance = echarts.init(paymentChart.value)
  paymentChartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        type: 'pie',
        radius: '70%',
        data: [
          { value: 0, name: '微信支付' },
          { value: 0, name: '支付宝' },
          { value: 0, name: '现金' },
          { value: 0, name: '银行卡' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  })

  // 热销商品TOP10
  productsChartInstance = echarts.init(productsChart.value)
  productsChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: []
    },
    series: [
      {
        type: 'bar',
        data: []
      }
    ]
  })

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    salesChartInstance.resize()
    paymentChartInstance.resize()
    productsChartInstance.resize()
  })
}

// 更新销售趋势图
const updateSalesChart = (data) => {
  salesChartInstance.setOption({
    xAxis: {
      data: data.timeLabels || []
    },
    series: [
      {
        name: '销售额',
        data: data.salesData || []
      },
      {
        name: '订单数',
        data: data.ordersData || []
      }
    ]
  })
}

// 更新支付方式图
const updatePaymentChart = (data) => {
  paymentChartInstance.setOption({
    series: [
      {
        data: data || []
      }
    ]
  })
}

// 更新热销商品图
const updateProductsChart = (data) => {
  productsChartInstance.setOption({
    yAxis: {
      data: data.map(item => item.name) || []
    },
    series: [
      {
        data: data.map(item => item.sales) || []
      }
    ]
  })
}

// 获取支付方式文本
const getPaymentMethodText = (method) => {
  const texts = {
    wechat: '微信支付',
    alipay: '支付宝',
    cash: '现金',
    card: '银行卡'
  }
  return texts[method] || method
}

// 日期限制
const disabledDate = (current) => {
  return current && current > dayjs().endOf('day')
}

// 处理门店切换
const handleStoreChange = () => {
  handleSearch()
}

// 处理表格变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchSalesList()
}

// 处理搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.value = {
    storeId: undefined,
    dateRange: []
  }
  handleSearch()
}

// 获取所有数据
const fetchData = () => {
  fetchStatistics()
  fetchSalesList()
  fetchSalesTrend()
  fetchPaymentMethods()
  fetchHotProducts()
}

onMounted(() => {
  fetchStores()
  initCharts()
  fetchData()
})
</script>

<style lang="less" scoped>
.finance-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 16px;
  }

  .statistics-cards {
    margin-bottom: 16px;

    .growth {
      font-size: 12px;
      margin-left: 8px;

      &.up {
        color: #3f8600;
      }

      &.down {
        color: #cf1322;
      }
    }
  }

  .chart-section {
    margin-bottom: 16px;

    .chart {
      height: 400px;
    }
  }

  .table-section {
    margin-bottom: 16px;
  }
}
</style> 