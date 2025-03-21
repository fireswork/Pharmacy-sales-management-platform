<template>
  <div class="employee-container">
    <a-card>
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入员工姓名/工号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="在职状态"
            allow-clear
          >
            <a-select-option value="在职">在职</a-select-option>
            <a-select-option value="离职">离职</a-select-option>
          </a-select>
          <!-- 新增所属分店筛选 -->
          <a-select
            v-model:value="searchForm.storeId"
            style="width: 180px"
            placeholder="所属分店"
            allow-clear
          >
            <a-select-option v-for="store in storeList" :key="store.id" :value="store.id">
              {{ store.name }}
            </a-select-option>
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
            新增员工
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="employeeList"
        :pagination="pagination"
        :loading="loading"
        :row-key="record => record.code"
        bordered
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '在职' ? 'success' : 'default'">
              {{ record.status }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button 
                type="link" 
                size="small" 
                :danger="record.status === '在职'"
                @click="handleStatusChange(record)"
              >
                {{ record.status === '在职' ? '离职' : '复职' }}
              </a-button>
              <a-button type="link" size="small" @click="handleResetPassword(record)">重置密码</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑员工弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑员工' : '新增员工'"
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
        <a-form-item v-if="isEdit" label="员工工号">
          <a-input v-model:value="formData.code" disabled />
        </a-form-item>

        <a-form-item label="员工姓名" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入员工姓名" />
        </a-form-item>

        <a-form-item label="手机号码" name="phoneNumber" required>
          <a-input v-model:value="formData.phoneNumber" placeholder="请输入手机号码" />
        </a-form-item>

        <a-form-item label="电子邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入电子邮箱" />
        </a-form-item>

        <a-form-item label="所属分店" name="storeId" required>
          <a-select
            v-model:value="formData.storeId"
            placeholder="请选择所属分店"
            style="width: 100%"
            allow-clear
          >
            <a-select-option v-for="store in storeList" :key="store.id" :value="store.id">
              {{ store.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="入职日期" name="hireDate" required>
          <a-date-picker 
            v-model:value="formData.hireDate" 
            style="width: 100%"
            :disabledDate="disabledDate"
          />
        </a-form-item>

        <a-form-item v-if="isEdit" label="在职状态" name="status" required>
          <a-radio-group v-model:value="formData.status">
            <a-radio value="在职">在职</a-radio>
            <a-radio value="离职">离职</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
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
  status: undefined,
  storeId: undefined // 新增所属分店筛选
})

// 员工列表
const employeeList = ref([])

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

// 表格列定义
const columns = [
  {
    title: '工号',
    dataIndex: 'code',
    width: 100,
  },
  {
    title: '姓名',
    dataIndex: 'name',
    width: 120,
  },
  {
    title: '手机号码',
    dataIndex: 'phoneNumber',
    width: 150,
  },
  {
    title: '所属分店',
    dataIndex: ['store', 'name'],
    width: 150,
  },
  {
    title: '入职日期',
    dataIndex: 'hireDate',
    width: 120,
    customRender: ({text}) => {
      return text ? dayjs(text).format('YYYY-MM-DD') : ''
    }
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
    width: 200,
  }
]

// 加载状态
const loading = ref(false)
const submitLoading = ref(false)

// 弹窗控制
const modalVisible = ref(false)
const isEdit = ref(false)

// 表单引用
const formRef = ref(null)

// 表单数据
const formData = reactive({
  code: '',
  name: '',
  phoneNumber: '',
  email: '',
  storeId: undefined,
  hireDate: null,
  status: '在职'
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入员工姓名', trigger: 'blur' }
  ],
  phoneNumber: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  storeId: [
    { required: true, message: '请选择所属分店', trigger: 'change' }
  ],
  hireDate: [
    { required: true, message: '请选择入职日期', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择在职状态', trigger: 'change' }
  ]
}

// 禁用未来日期
const disabledDate = (current) => {
  return current && current > dayjs().endOf('day')
}

// 获取员工列表
const fetchEmployees = async () => {
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
    
    // 添加分店筛选参数
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId
    }
    
    const response = await axios.get('/employee', { params })
    
    if (response.data) {
      employeeList.value = response.data.content
      pagination.total = response.data.totalElements
    }
  } catch (error) {
    console.error('获取员工列表失败:', error)
    message.error('获取员工列表失败')
  } finally {
    loading.value = false
  }
}

// 获取分店列表
const fetchStores = async () => {
  try {
    // 获取营业中的分店
    const response = await axios.get('/store/all')
    
    if (response.data) {
      storeList.value = response.data
    }
  } catch (error) {
    console.error('获取分店列表失败:', error)
    message.error('获取分店列表失败')
  }
}

// 处理表格变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchEmployees()
}

// 处理搜索
const handleSearch = () => {
  pagination.current = 1
  fetchEmployees()
}

// 处理重置
const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: undefined,
    storeId: undefined // 重置分店筛选
  }
  pagination.current = 1
  fetchEmployees()
}

// 处理添加
const handleAdd = () => {
  isEdit.value = false
  formData.code = ''
  formData.name = ''
  formData.phoneNumber = ''
  formData.email = ''
  formData.storeId = undefined
  formData.hireDate = null
  formData.status = '在职'
  modalVisible.value = true
}

// 处理编辑
const handleEdit = (record) => {
  isEdit.value = true
  formData.code = record.code
  formData.name = record.name
  formData.phoneNumber = record.phoneNumber
  formData.email = record.email
  formData.storeId = record.store ? record.store.id : undefined
  formData.hireDate = record.hireDate ? dayjs(record.hireDate) : null
  formData.status = record.status
  formData.id = record.id
  modalVisible.value = true
}

// 处理表单提交
const handleModalSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    const requestData = {
      name: formData.name,
      phoneNumber: formData.phoneNumber,
      email: formData.email,
      storeId: formData.storeId,
      hireDate: formData.hireDate ? formData.hireDate.format('YYYY-MM-DD') : null,
      status: formData.status
    }
    
    if (isEdit.value) {
      // 编辑员工
      await axios.put(`/employee/${formData.id}`, requestData)
      message.success('员工信息更新成功')
    } else {
      // 添加员工
      await axios.post('/employee', requestData)
      message.success('员工添加成功')
    }
    
    modalVisible.value = false
    fetchEmployees()
  } catch (error) {
    console.error('保存员工信息失败:', error)
    message.error(error.response?.data?.message || '保存员工信息失败')
  } finally {
    submitLoading.value = false
  }
}

// 处理状态变更
const handleStatusChange = async (record) => {
  try {
    const newStatus = record.status === '在职' ? '离职' : '在职'
    const action = newStatus === '在职' ? '复职' : '离职'
    
    await axios.put(`/employee/${record.id}/status`, null, {
      params: { status: newStatus }
    })
    
    message.success(`${action}操作成功`)
    fetchEmployees()
  } catch (error) {
    console.error('更新员工状态失败:', error)
    message.error('更新员工状态失败')
  }
}

// 处理重置密码
const handleResetPassword = async (record) => {
  try {
    await axios.put(`/employee/${record.id}/reset-password`)
    message.success(`已重置 ${record.name} 的密码为: 123456`)
  } catch (error) {
    console.error('重置密码失败:', error)
    message.error('重置密码失败')
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchEmployees()
  fetchStores()
})
</script>

<style lang="less" scoped>
.employee-container {
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