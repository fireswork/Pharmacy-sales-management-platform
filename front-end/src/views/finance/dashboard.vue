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
        :row-key="record => record.id"
        bordered
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
import { ref, onMounted } from 'vue'
import { message, Statistic } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

// 搜索表单
const searchForm = ref({
  storeId: undefined,
  dateRange: []
})

// 统计数据
const statistics = ref({
  todaySales: 12580.50,
  salesGrowth: 12.5,
  todayOrders: 156,
  ordersGrowth: -5.2,
  averageOrder: 80.65,
  grossProfitRate: 35.8
})

// 图表实例
const salesChart = ref(null)
const paymentChart = ref(null)
const productsChart = ref(null)

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

// 初始化图表
const initCharts = () => {
  // 销售趋势图
  const sales = echarts.init(salesChart.value)
  sales.setOption({
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
        data: [820, 932, 901, 934, 1290, 1330, 1520, 1200, 1100, 1400, 1800, 1380]
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: [10, 12, 11, 14, 25, 30, 35, 28, 26, 32, 40, 30]
      }
    ]
  })

  // 支付方式占比
  const payment = echarts.init(paymentChart.value)
  payment.setOption({
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
          { value: 1048, name: '微信支付' },
          { value: 735, name: '支付宝' },
          { value: 580, name: '现金' },
          { value: 484, name: '银行卡' }
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
  const products = echarts.init(productsChart.value)
  products.setOption({
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
      data: ['商品1', '商品2', '商品3', '商品4', '商品5', 
             '商品6', '商品7', '商品8', '商品9', '商品10']
    },
    series: [
      {
        type: 'bar',
        data: [320, 302, 301, 284, 250, 236, 210, 190, 180, 160]
      }
    ]
  })

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    sales.resize()
    payment.resize()
    products.resize()
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
  return texts[method]
}

// 日期限制
const disabledDate = (current) => {
  return current && current > dayjs().endOf('day')
}

// 处理函数
const handleSearch = () => {
  message.success('搜索成功')
}

const handleReset = () => {
  searchForm.value = {
    storeId: undefined,
    dateRange: []
  }
  handleSearch()
}

onMounted(() => {
  initCharts()
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