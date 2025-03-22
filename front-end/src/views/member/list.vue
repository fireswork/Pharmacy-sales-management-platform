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
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="会员状态"
            allow-clear
          >
            <a-select-option value="正常">正常</a-select-option>
            <a-select-option value="已停用">已停用</a-select-option>
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
        :loading="loading"
        :row-key="record => record.username"
        bordered
        @change="handleTableChange"
        :scroll="{ x: 1300 }"
      >
        <template #bodyCell="{ column, record }">
          <!-- 会员等级列 -->
          <template v-if="column.key === 'memberLevel'">
            <a-tag :color="getMemberLevelColor(record.memberLevel)">
              {{ getMemberLevelText(record.memberLevel) }}
            </a-tag>
          </template>

          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '正常' ? 'success' : 'default'">
              {{ record.status }}
            </a-tag>
          </template>
          
          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button 
                type="link" 
                size="small" 
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button 
                type="link" 
                size="small" 
                :danger="record.status === '正常'"
                @click="handleStatusChange(record)"
              >
                {{ record.status === '正常' ? '停用' : '启用' }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑会员信息' : '新增会员'"
      @ok="handleModalSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      :okText="isEdit ? '保存' : '确定'"
      cancelText="取消"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item 
          label="会员编号" 
          v-if="isEdit"
        >
          <span>{{ formData.memberId }}</span>
        </a-form-item>
        
        <a-form-item label="会员姓名" name="username" required>
          <a-input v-model:value="formData.username" placeholder="请输入会员姓名" />
        </a-form-item>

        <a-form-item label="手机号码" name="phoneNumber" required>
          <a-input v-model:value="formData.phoneNumber" placeholder="请输入手机号码" />
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

        <a-form-item label="性别" name="gender">
          <a-radio-group v-model:value="formData.gender">
            <a-radio value="male">男</a-radio>
            <a-radio value="female">女</a-radio>
          </a-radio-group>
        </a-form-item>
        
        <template v-if="isEdit">
          <a-form-item label="会员等级">
            <a-tag :color="getMemberLevelColor(formData.memberLevel)">
              {{ getMemberLevelText(formData.memberLevel) }}
            </a-tag>
          </a-form-item>
          
          <a-form-item label="当前积分">
            <span>{{ formData.points }}</span>
          </a-form-item>
          
          <a-form-item label="累计消费">
            <span>¥{{ formData.totalSpending ? formData.totalSpending.toFixed(2) : '0.00' }}</span>
          </a-form-item>
        </template>
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
          <a-descriptions-item label="当前积分">{{ currentMember.points || 0 }}</a-descriptions-item>
          <a-descriptions-item label="累计积分">{{ currentMember.totalPoints || 0 }}</a-descriptions-item>
          <a-descriptions-item label="已使用积分">{{ currentMember.usedPoints || 0 }}</a-descriptions-item>
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
          <a-descriptions-item label="消费总额">¥{{ currentMember.totalSpending || 0 }}</a-descriptions-item>
          <a-descriptions-item label="订单总数">{{ currentMember.orderCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="最近消费">{{ currentMember.lastOrderTime || '无' }}</a-descriptions-item>
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { 
  SearchOutlined, 
  ReloadOutlined, 
  PlusOutlined 
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import axios from '@/utils/axios'

// 搜索表单
const searchForm = ref({
  keyword: '',
  level: undefined,
  status: undefined,
  storeId: undefined
})

// 会员等级配置
const memberLevels = [
  { value: 'bronze', label: '普通会员', color: '#d9d9d9' },
  { value: 'silver', label: '白银会员', color: '#bfbfbf' },
  { value: 'gold', label: '黄金会员', color: '#faad14' },
  { value: 'platinum', label: '铂金会员', color: '#1890ff' },
  { value: 'diamond', label: '钻石会员', color: '#722ed1' }
]

// 表格列配置
const columns = [
  {
    title: '会员编号',
    dataIndex: ['memberId'],
    width: 120,
  },
  {
    title: '会员姓名',
    dataIndex: ['name'],
    width: 140,
  },
  {
    title: '手机号',
    dataIndex: ['phoneNumber'],
    width: 150,
    customRender: ({ text }) => text || '-'
  },
  {
    title: '邮箱',
    dataIndex: ['email'],
    width: 180,
    customRender: ({text}) => text || '-'
  },
  {
    title: '性别',
    dataIndex: ['gender'],
    width: 80,
    customRender: ({ text }) => {
      if (text === 'male') return '男'
      if (text === 'female') return '女'
      return '未知'
    }
  },
  {
    title: '会员等级',
    dataIndex: ['memberLevel'],
    key: 'memberLevel',
    width: 120,
  },
  {
    title: '当前积分',
    dataIndex: ['points'],
    width: 150,
  },
  {
    title: '累计消费',
    dataIndex: ['totalSpending'],
    width: 120,
    customRender: ({ text }) => {
      return text ? `¥${text.toFixed(2)}` : '¥0.00'
    }
  },
  {
    title: '注册时间',
    dataIndex: ['registrationTime'],
    width: 200,
    customRender: ({ text }) => {
      return text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '无'
    }
  },
  {
    title: '状态',
    dataIndex: ['status'],
    key: 'status',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
  }
]

// 积分记录列配置
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
    width: 120
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

// 订单记录列配置
const orderColumns = [
  {
    title: '订单编号',
    dataIndex: 'orderNo',
    width: 180
  },
  {
    title: '下单时间',
    dataIndex: 'orderTime',
    width: 180
  },
  {
    title: '订单金额',
    dataIndex: 'amount',
    width: 120,
    customRender: ({ text }) => {
      return `¥${text}`
    }
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
const memberList = ref([])
const loading = ref(false)

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条记录`,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})

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
  username: '',
  phoneNumber: '',
  memberLevel: 'bronze',
  birthday: null,
  email: '',
  gender: ''
})

// 积分记录和订单记录
const pointsRecords = ref([])
const orderRecords = ref([])

// 表单验证规则
const rules = {
  username: [{ required: true, message: '请输入会员姓名' }],
  phoneNumber: [
    { required: true, message: '请输入手机号码' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码' }
  ],
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
  return levelConfig ? levelConfig.label : level
}

// 获取订单状态颜色
const getOrderStatusColor = (status) => {
  const colors = {
    pending: 'warning',
    processing: 'processing',
    completed: 'success',
    cancelled: 'default'
  }
  return colors[status] || 'default'
}

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const texts = {
    pending: '待付款',
    processing: '已付款',
    completed: '已完成',
    cancelled: '已取消'
  }
  return texts[status] || status
}

// 获取会员列表
const fetchMembers = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize
    }
    
    // 添加关键字搜索参数
    if (searchForm.value.keyword) {
      params.keyword = searchForm.value.keyword
    }
    
    // 添加状态筛选参数
    if (searchForm.value.status) {
      params.status = searchForm.value.status
    }
    
    // 添加门店筛选参数
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId
    }
    
    const response = await axios.get('/member', { params })
    
    if (response.code === 200) {
      const { data } = response
      memberList.value = data.content
      pagination.total = data.totalElements
    }
  } catch (error) {
    console.error('获取会员列表失败:', error)
    message.error('获取会员列表失败')
  } finally {
    loading.value = false
  }
}

// 处理表格变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchMembers()
}

// 处理函数
const handleSearch = () => {
  pagination.current = 1
  fetchMembers()
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    level: undefined,
    status: undefined,
    storeId: undefined
  }
  pagination.current = 1
  fetchMembers()
}

const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    username: '',
    phoneNumber: '',
    memberLevel: 'bronze',
    birthday: null,
    email: '',
    gender: ''
  }
  modalVisible.value = true
}

const handleEdit = (record) => {
  isEdit.value = true;
  currentMember.value = record.member;
  
  // 设置表单数据
  formData.value = {
    id: record.id,
    memberId: record.memberId,
    username: record.name,
    name: record.name,
    phoneNumber: record.phoneNumber || '',
    birthday: record.birthday ? dayjs(record.birthday) : null,
    email: record.email || '',
    gender: record.gender || '',
    // 保留现有值，不允许管理员直接修改
    memberLevel: record.memberLevel,
    points: record.points,
    totalSpending: record.totalSpending
  };
  
  modalVisible.value = true;
};

const handleModalSubmit = async () => {
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    
    // 构建提交的数据对象
    const memberData = {
      name: formData.value.username,
      phoneNumber: formData.value.phoneNumber,
      email: formData.value.email,
      gender: formData.value.gender,
      birthday: formData.value.birthday ? formData.value.birthday.toDate() : null,
    };
    
    let response;
    
    if (isEdit.value) {
      // 更新会员
      response = await axios.put(`/member/${formData.value.memberId}`, memberData);
      
      if (response.code === 200) {
        message.success('会员信息更新成功');
        modalVisible.value = false;
        fetchMembers();
      }
    } else {
      // 添加新会员
      const newMember = {
        username: formData.value.username,
        ...memberData
      };
      
      response = await axios.post('/member', newMember);
      
      if (response.code === 201) {
        message.success('会员添加成功');
        modalVisible.value = false;
        fetchMembers();
      }
    }
  } catch (error) {
    console.error(isEdit.value ? '更新会员失败:' : '添加会员失败:', error);
    message.error(error.response?.data?.message || (isEdit.value ? '更新会员失败' : '添加会员失败'));
  } finally {
    submitLoading.value = false;
  }
};

const handleStatusChange = async (record) => {
  const newStatus = record.status === '正常' ? '已停用' : '正常'
  const action = newStatus === '正常' ? '启用' : '停用'
  
  try {
    const response = await axios.put(`/member/${record.memberId}/status?status=${newStatus}`)
    
    if (response.code === 200) {
      message.success(`${action}成功`)
      fetchMembers() // 重新获取列表
    }
  } catch (error) {
    console.error(`${action}会员失败:`, error)
    message.error(`${action}会员失败`)
  }
}

const handleViewPoints = (record) => {
  currentMember.value = record
  
  // 模拟积分记录数据，实际应该从API获取
  pointsRecords.value = [
    {
      id: 1,
      time: '2024-03-20 14:30:00',
      type: 'earn',
      points: '+100',
      source: '购物奖励',
      remark: '订单号: 202403200001'
    },
    {
      id: 2,
      time: '2024-03-15 10:20:00',
      type: 'use',
      points: '-50',
      source: '积分兑换',
      remark: '兑换商品: 洗发水'
    }
  ]
  
  pointsVisible.value = true
}

const handleViewOrders = (record) => {
  currentMember.value = record
  
  // 模拟订单记录数据，实际应该从API获取
  orderRecords.value = [
    {
      id: 1,
      orderNo: '202403200001',
      orderTime: '2024-03-20 14:30:00',
      amount: 299.00,
      paymentMethod: '微信支付',
      status: 'completed',
      storeName: '总店'
    },
    {
      id: 2,
      orderNo: '202403150002',
      orderTime: '2024-03-15 10:20:00',
      amount: 158.50,
      paymentMethod: '支付宝',
      status: 'completed',
      storeName: '分店一'
    }
  ]
  
  ordersVisible.value = true
}

// 重置表单
const resetForm = () => {
  formData.value = {
    username: '',
    phoneNumber: '',
    memberLevel: 'bronze',
    birthday: null,
    email: '',
    gender: ''
  };
  
  if (formRef.value) {
    formRef.value.resetFields();
  }
};

// 组件挂载时获取会员列表
onMounted(() => {
  fetchMembers()
})
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