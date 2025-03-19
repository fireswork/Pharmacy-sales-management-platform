<template>
  <div class="member-container">
    <a-card>
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入会员姓名/手机号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.level"
            style="width: 120px"
            placeholder="会员等级"
            allow-clear
          >
            <a-select-option v-for="level in memberLevels" :key="level.value" :value="level.value">
              {{ level.label }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="会员状态"
            allow-clear
          >
            <a-select-option value="active">正常</a-select-option>
            <a-select-option value="inactive">已停用</a-select-option>
          </a-select>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增会员
          </a-button>
        </a-space>
      </div>

      <!-- 会员列表 -->
      <a-table
        :columns="columns"
        :data-source="memberList"
        :pagination="pagination"
        :row-key="record => record.id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 会员等级列 -->
          <template v-if="column.key === 'level'">
            <a-tag :color="getMemberLevelColor(record.level)">
              {{ getMemberLevelText(record.level) }}
            </a-tag>
          </template>

          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'active' ? 'success' : 'default'">
              {{ record.status === 'active' ? '正常' : '已停用' }}
            </a-tag>
          </template>
          
          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleViewPoints(record)">积分记录</a-button>
              <a-button type="link" size="small" @click="handleViewOrders(record)">消费记录</a-button>
              <a-button 
                type="link" 
                size="small" 
                :danger="record.status === 'active'"
                @click="handleStatusChange(record)"
              >
                {{ record.status === 'active' ? '停用' : '启用' }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑会员' : '新增会员'"
      @ok="handleModalSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="会员姓名" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入会员姓名" />
        </a-form-item>

        <a-form-item label="手机号码" name="phone" required>
          <a-input v-model:value="formData.phone" placeholder="请输入手机号码" />
        </a-form-item>

        <a-form-item label="会员等级" name="level" required>
          <a-select v-model:value="formData.level" placeholder="请选择会员等级">
            <a-select-option v-for="level in memberLevels" :key="level.value" :value="level.value">
              {{ level.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="生日" name="birthday">
          <a-date-picker 
            v-model:value="formData.birthday"
            style="width: 100%"
            :disabledDate="disabledDate"
          />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱地址" />
        </a-form-item>

        <a-form-item label="地址" name="address">
          <a-textarea
            v-model:value="formData.address"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 积分记录弹窗 -->
    <a-modal
      v-model:visible="pointsVisible"
      title="积分记录"
      :footer="null"
      width="800px"
    >
      <div class="points-summary">
        <a-descriptions :column="3">
          <a-descriptions-item label="当前积分">{{ currentMember.points }}</a-descriptions-item>
          <a-descriptions-item label="累计积分">{{ currentMember.totalPoints }}</a-descriptions-item>
          <a-descriptions-item label="已使用积分">{{ currentMember.usedPoints }}</a-descriptions-item>
        </a-descriptions>
      </div>
      <a-table
        :columns="pointsColumns"
        :data-source="pointsRecords"
        :pagination="{ pageSize: 5 }"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 'earn' ? 'success' : 'warning'">
              {{ record.type === 'earn' ? '获得' : '使用' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 消费记录弹窗 -->
    <a-modal
      v-model:visible="ordersVisible"
      title="消费记录"
      :footer="null"
      width="900px"
    >
      <div class="orders-summary">
        <a-descriptions :column="3">
          <a-descriptions-item label="消费总额">¥{{ currentMember.totalAmount }}</a-descriptions-item>
          <a-descriptions-item label="订单总数">{{ currentMember.orderCount }}</a-descriptions-item>
          <a-descriptions-item label="最近消费">{{ currentMember.lastOrderTime }}</a-descriptions-item>
        </a-descriptions>
      </div>
      <a-table
        :columns="orderColumns"
        :data-source="orderRecords"
        :pagination="{ pageSize: 5 }"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getOrderStatusColor(record.status)">
              {{ getOrderStatusText(record.status) }}
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
  PlusOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

// 会员等级配置
const memberLevels = [
  { value: 'bronze', label: '普通会员', color: '' },
  { value: 'silver', label: '白银会员', color: 'cyan' },
  { value: 'gold', label: '黄金会员', color: 'gold' },
  { value: 'platinum', label: '铂金会员', color: 'purple' },
  { value: 'diamond', label: '钻石会员', color: 'blue' }
]

// 搜索表单
const searchForm = ref({
  keyword: '',
  level: undefined,
  status: undefined
})

// 表格列定义
const columns = [
  {
    title: '会员编号',
    dataIndex: 'code',
    width: 120
  },
  {
    title: '会员姓名',
    dataIndex: 'name',
    width: 120
  },
  {
    title: '手机号码',
    dataIndex: 'phone',
    width: 120
  },
  {
    title: '会员等级',
    dataIndex: 'level',
    key: 'level',
    width: 100
  },
  {
    title: '当前积分',
    dataIndex: 'points',
    width: 100
  },
  {
    title: '累计消费',
    dataIndex: 'totalAmount',
    width: 120
  },
  {
    title: '注册时间',
    dataIndex: 'registerTime',
    width: 180
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 280
  }
]

// 积分记录列定义
const pointsColumns = [
  {
    title: '时间',
    dataIndex: 'time',
    width: 180
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type',
    width: 100
  },
  {
    title: '积分变动',
    dataIndex: 'points',
    width: 100
  },
  {
    title: '来源/用途',
    dataIndex: 'source',
    width: 200
  },
  {
    title: '备注',
    dataIndex: 'remark'
  }
]

// 订单记录列定义
const orderColumns = [
  {
    title: '订单编号',
    dataIndex: 'code',
    width: 120
  },
  {
    title: '下单时间',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '订单金额',
    dataIndex: 'amount',
    width: 120
  },
  {
    title: '支付方式',
    dataIndex: 'paymentMethod',
    width: 120
  },
  {
    title: '订单状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '购买店铺',
    dataIndex: 'storeName',
    width: 150
  }
]

// 会员列表数据
const memberList = ref([
  {
    id: 1,
    code: 'M001',
    name: '张三',
    phone: '13800138000',
    level: 'gold',
    points: 1000,
    totalAmount: 5000,
    registerTime: '2024-01-01 10:00:00',
    status: 'active'
  },
  // ... 其他会员数据
])

// 分页配置
const pagination = {
  total: memberList.value.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`
}

// 当前选中的会员
const currentMember = ref({})

// 弹窗控制
const modalVisible = ref(false)
const pointsVisible = ref(false)
const ordersVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)

// 表单数据
const formData = ref({
  name: '',
  phone: '',
  level: 'bronze',
  birthday: null,
  email: '',
  address: '',
  remark: ''
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入会员姓名' }],
  phone: [
    { required: true, message: '请输入手机号码' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码' }
  ],
  level: [{ required: true, message: '请选择会员等级' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址' }]
}

// 生日日期限制
const disabledDate = (current) => {
  return current && current > dayjs().endOf('day')
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

// 获取订单状态颜色
const getOrderStatusColor = (status) => {
  const colors = {
    pending: 'warning',
    processing: 'processing',
    completed: 'success',
    cancelled: 'default'
  }
  return colors[status]
}

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const texts = {
    pending: '待付款',
    processing: '已付款',
    completed: '已完成',
    cancelled: '已取消'
  }
  return texts[status]
}

// 处理函数
const handleSearch = () => {
  message.success('搜索成功')
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    level: undefined,
    status: undefined
  }
  handleSearch()
}

const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    name: '',
    phone: '',
    level: 'bronze',
    birthday: null,
    email: '',
    address: '',
    remark: ''
  }
  modalVisible.value = true
}

const handleEdit = (record) => {
  isEdit.value = true
  formData.value = { ...record }
  modalVisible.value = true
}

const handleModalSubmit = () => {
  formRef.value.validate().then(() => {
    submitLoading.value = true
    setTimeout(() => {
      message.success('保存成功')
      modalVisible.value = false
      submitLoading.value = false
      handleSearch()
    }, 1000)
  })
}

const handleStatusChange = (record) => {
  const newStatus = record.status === 'active' ? 'inactive' : 'active'
  const action = newStatus === 'active' ? '启用' : '停用'
  record.status = newStatus
  message.success(`${action}成功`)
}

const handleViewPoints = (record) => {
  currentMember.value = {
    ...record,
    totalPoints: 2000,
    usedPoints: 1000
  }
  pointsVisible.value = true
}

const handleViewOrders = (record) => {
  currentMember.value = {
    ...record,
    orderCount: 10,
    lastOrderTime: '2024-03-15 14:30:00'
  }
  ordersVisible.value = true
}
</script>

<style lang="less" scoped>
.member-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
  }

  .points-summary,
  .orders-summary {
    margin-bottom: 16px;
    padding: 16px;
    background: #fafafa;
    border-radius: 4px;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}
</style> 