<template>
  <div class="store-container">
    <a-card>
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入分店名称/编号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="营业状态"
            allow-clear
          >
            <a-select-option value="active">营业中</a-select-option>
            <a-select-option value="inactive">已停业</a-select-option>
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
            新增分店
          </a-button>
        </a-space>
      </div>

      <!-- 分店列表 -->
      <a-table
        :columns="columns"
        :data-source="storeList"
        :pagination="pagination"
        :row-key="record => record.id"
        bordered
        :loading="loading"
        :scroll="{ x: 1000 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'active' ? 'success' : 'default'">
              {{ record.status === 'active' ? '营业中' : '已停业' }}
            </a-tag>
          </template>
          
          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button 
                type="link" 
                size="small" 
                :danger="record.status === 'active'"
                @click="handleStatusChange(record)"
              >
                {{ record.status === 'active' ? '停业' : '营业' }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑分店' : '新增分店'"
      @ok="handleModalSubmit"
      :confirmLoading="submitLoading"
      width="720px"
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
        <a-form-item label="分店名称" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入分店名称" />
        </a-form-item>

        <a-form-item label="联系电话" name="phoneNumber" required>
          <a-input v-model:value="formData.phoneNumber" placeholder="请输入联系电话" />
        </a-form-item>

        <a-form-item label="营业时间" required>
          <a-row :gutter="8">
            <a-col :span="11">
              <a-form-item name="openTime" :noStyle="true">
                <a-time-picker v-model:value="formData.openTime" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="2" style="text-align: center">至</a-col>
            <a-col :span="11">
              <a-form-item name="closeTime" :noStyle="true">
                <a-time-picker v-model:value="formData.closeTime" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item label="详细地址" name="address" required>
          <a-textarea
            v-model:value="formData.address"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </a-form-item>

        <a-form-item label="营业状态" name="status" required>
          <a-radio-group v-model:value="formData.status">
            <a-radio value="active">营业中</a-radio>
            <a-radio value="inactive">已停业</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 采购记录弹窗 -->
    <a-modal
      v-model:visible="purchaseVisible"
      title="采购记录"
      :footer="null"
      width="900px"
    >
      <a-table
        :columns="purchaseColumns"
        :data-source="purchaseRecords"
        :pagination="{ pageSize: 5 }"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getPurchaseStatusColor(record.status)">
              {{ getPurchaseStatusText(record.status) }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 库存查看弹窗 -->
    <a-modal
      v-model:visible="inventoryVisible"
      title="库存查看"
      :footer="null"
      width="900px"
    >
      <a-table
        :columns="inventoryColumns"
        :data-source="inventoryRecords"
        :pagination="{ pageSize: 5 }"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'stockStatus'">
            <a-tag :color="getStockStatusColor(record)">
              {{ getStockStatusText(record) }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 订单记录弹窗 -->
    <a-modal
      v-model:visible="orderVisible"
      title="订单记录"
      :footer="null"
      width="900px"
    >
      <a-table
        :columns="orderColumns"
        :data-source="orderRecords"
        :pagination="{ pageSize: 5 }"
        :scroll="{ x: 1000 }"
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
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import axios from '../../utils/axios'

// 搜索表单
const searchForm = ref({
  keyword: '',
  status: undefined
})

// 分店列表
const storeList = ref([])

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`
})

// 加载状态
const loading = ref(false)
const submitLoading = ref(false)

// 弹窗控制
const modalVisible = ref(false)
const isEdit = ref(false)

// 表单引用
const formRef = ref(null)

// 表单数据
const formData = ref({
  id: null,
  code: '',
  name: '',
  phoneNumber: '',
  openTime: null,
  closeTime: null,
  address: '',
  status: 'active'
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入分店名称', trigger: 'blur' }
  ],
  phoneNumber: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入分店地址', trigger: 'blur' }
  ]
}

// 表格列定义
const columns = [
  {
    title: '分店编号',
    dataIndex: 'code',
    width: 120,
  },
  {
    title: '分店名称',
    dataIndex: 'name',
    width: 150,
  },
  {
    title: '详细地址',
    dataIndex: 'address',
    width: 250,
  },
  {
    title: '联系电话',
    dataIndex: 'phoneNumber',
    width: 150,
  },
  {
    title: '营业时间',
    customRender: ({ record }) => {
      return `${record.openTime} - ${record.closeTime}`
    },
    width: 180,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    width: 350,
    fixed: 'right',
  }
]

// 采购记录列定义
const purchaseColumns = [
  {
    title: '采购编号',
    dataIndex: 'code',
    width: 120,
  },
  {
    title: '采购名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '采购金额',
    dataIndex: 'amount',
    width: 120,
  },
  {
    title: '申请时间',
    dataIndex: 'applyTime',
    width: 180,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
  }
]

// 库存列定义
const inventoryColumns = [
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
    title: '当前库存',
    dataIndex: 'stock',
    width: 100,
  },
  {
    title: '库存状态',
    key: 'stockStatus',
    width: 100,
  }
]

// 订单列定义
const orderColumns = [
  {
    title: '订单编号',
    dataIndex: 'code',
    width: 120,
  },
  {
    title: '订单金额',
    dataIndex: 'amount',
    width: 120,
  },
  {
    title: '下单时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
  }
]

// 记录弹窗
const purchaseVisible = ref(false)
const inventoryVisible = ref(false)
const orderVisible = ref(false)

// 记录数据
const purchaseRecords = ref([])
const inventoryRecords = ref([])
const orderRecords = ref([])

// 状态颜色和文本
const getPurchaseStatusColor = (status) => {
  const colors = {
    pending: 'warning',
    approved: 'success',
    rejected: 'error'
  }
  return colors[status]
}

const getPurchaseStatusText = (status) => {
  const texts = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return texts[status]
}

const getStockStatusColor = (record) => {
  if (record.stock === 0) return 'error'
  if (record.stock <= record.warningThreshold) return 'warning'
  return 'success'
}

const getStockStatusText = (record) => {
  if (record.stock === 0) return '无库存'
  if (record.stock <= record.warningThreshold) return '偏低'
  return '正常'
}

const getOrderStatusColor = (status) => {
  const colors = {
    pending: 'warning',
    processing: 'processing',
    completed: 'success',
    cancelled: 'default'
  }
  return colors[status]
}

const getOrderStatusText = (status) => {
  const texts = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return texts[status]
}

// 获取分店列表
const fetchStores = async () => {
  try {
    loading.value = true
    
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize
    }
    
    if (searchForm.value.keyword) {
      params.keyword = searchForm.value.keyword
    }
    
    if (searchForm.value.status) {
      params.status = searchForm.value.status
    }
    
    const response = await axios.get('/store', { params })
    
    if (response.data) {
      storeList.value = response.data.content
      pagination.total = response.data.totalElements
    }
  } catch (error) {
    console.error('获取分店列表失败:', error)
    message.error('获取分店列表失败')
  } finally {
    loading.value = false
  }
}

// 处理表格变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchStores()
}

// 处理搜索
const handleSearch = () => {
  pagination.current = 1
  fetchStores()
}

// 处理重置
const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: undefined
  }
  pagination.current = 1
  fetchStores()
}

// 处理添加
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    id: null,
    code: '',
    name: '',
    phoneNumber: '',
    openTime: null,
    closeTime: null,
    address: '',
    status: 'active'
  }
  modalVisible.value = true
}

// 处理编辑
const handleEdit = (record) => {
  isEdit.value = true
  
  // 深拷贝记录数据，避免直接修改原始数据
  formData.value = {
    id: record.id,
    code: record.code,
    name: record.name,
    phoneNumber: record.phoneNumber,
    // 如果有时间数据，转换为dayjs对象
    openTime: record.openTime ? dayjs(record.openTime, 'HH:mm:ss') : null,
    closeTime: record.closeTime ? dayjs(record.closeTime, 'HH:mm:ss') : null,
    address: record.address,
    status: record.status
  }
  
  modalVisible.value = true
}

// 处理状态变更
const handleStatusChange = async (record) => {
  try {
    const newStatus = record.status === 'active' ? 'inactive' : 'active'
    const action = newStatus === 'active' ? '营业' : '停业'
    
    await axios.put(`/store/${record.id}/status`, null, {
      params: { status: newStatus }
    })
    
    message.success(`${action}成功`)
    fetchStores()
  } catch (error) {
    console.error('更新分店状态失败:', error)
    message.error('更新分店状态失败')
  }
}

// 禁用日期选择
const disabledDate = (current) => {
  return current && current > dayjs().endOf('day')
}

// 处理表单提交
const handleModalSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    const requestData = {
      name: formData.value.name,
      phoneNumber: formData.value.phoneNumber,
      openTime: formData.value.openTime ? formData.value.openTime.format('HH:mm:ss') : null,
      closeTime: formData.value.closeTime ? formData.value.closeTime.format('HH:mm:ss') : null,
      address: formData.value.address,
      status: formData.value.status
    }
    
    if (isEdit.value) {
      await axios.put(`/store/${formData.value.id}`, requestData)
      message.success('分店信息更新成功')
    } else {
      await axios.post('/store', requestData)
      message.success('分店添加成功')
    }
    
    modalVisible.value = false
    fetchStores()
  } catch (error) {
    console.error('保存分店信息失败:', error)
    message.error(error.response?.data?.message || '保存分店信息失败')
  } finally {
    submitLoading.value = false
  }
}

// 查看记录
const handleViewPurchase = (record) => {
  purchaseRecords.value = [
    {
      id: 1,
      code: 'P001',
      name: '常规采购',
      amount: 10000,
      applyTime: '2024-03-15 09:20:00',
      status: 'approved'
    },
    {
      id: 2,
      code: 'P002',
      name: '紧急采购',
      amount: 5000,
      applyTime: '2024-03-14 14:30:00',
      status: 'pending'
    }
  ]
  purchaseVisible.value = true
}

const handleViewInventory = (record) => {
  inventoryRecords.value = [
    {
      id: 1,
      code: 'MED001',
      name: '布洛芬缓释胶囊',
      stock: 1000,
      warningThreshold: 100
    },
    {
      id: 2,
      code: 'MED002',
      name: '感冒灵颗粒',
      stock: 50,
      warningThreshold: 100
    }
  ]
  inventoryVisible.value = true
}

const handleViewOrder = (record) => {
  orderRecords.value = [
    {
      id: 1,
      code: 'O001',
      amount: 500,
      createTime: '2024-03-15 10:20:00',
      status: 'completed'
    },
    {
      id: 2,
      code: 'O002',
      amount: 300,
      createTime: '2024-03-15 11:30:00',
      status: 'processing'
    }
  ]
  orderVisible.value = true
}

// 组件挂载时获取数据
onMounted(() => {
  fetchStores()
})
</script>

<style lang="less" scoped>
.store-container {
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