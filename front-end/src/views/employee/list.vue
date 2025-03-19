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
            <a-select-option value="active">在职</a-select-option>
            <a-select-option value="inactive">离职</a-select-option>
          </a-select>
          <a-select
            v-model:value="searchForm.role"
            style="width: 120px"
            placeholder="员工角色"
            allow-clear
          >
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="staff">普通员工</a-select-option>
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
        :row-key="record => record.id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'active' ? 'success' : 'default'">
              {{ record.status === 'active' ? '在职' : '离职' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'role'">
            <a-tag :color="record.role === 'admin' ? 'blue' : ''">
              {{ record.role === 'admin' ? '管理员' : '普通员工' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button 
                type="link" 
                size="small" 
                :danger="record.status === 'active'"
                @click="handleStatusChange(record)"
              >
                {{ record.status === 'active' ? '离职' : '复职' }}
              </a-button>
              <a-button type="link" size="small" @click="handleResetPassword(record)">
                重置密码
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑员工' : '新增员工'"
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
        <a-form-item label="工号" name="code" required>
          <a-input 
            v-model:value="formData.code" 
            placeholder="请输入工号"
            :disabled="isEdit"
          />
        </a-form-item>

        <a-form-item label="姓名" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入姓名" />
        </a-form-item>

        <a-form-item label="手机号" name="phone" required>
          <a-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱" />
        </a-form-item>

        <a-form-item label="角色" name="role" required>
          <a-select v-model:value="formData.role" placeholder="请选择角色">
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="staff">普通员工</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="所属分店" name="storeId" required>
          <a-select v-model:value="formData.storeId" placeholder="请选择分店">
            <a-select-option v-for="store in storeOptions" :key="store.id" :value="store.id">
              {{ store.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="状态" name="status" required>
          <a-radio-group v-model:value="formData.status">
            <a-radio value="active">在职</a-radio>
            <a-radio value="inactive">离职</a-radio>
          </a-radio-group>
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
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'

// 搜索表单
const searchForm = ref({
  keyword: '',
  status: undefined,
  role: undefined
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
    title: '手机号',
    dataIndex: 'phone',
    width: 150,
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    width: 200,
  },
  {
    title: '角色',
    key: 'role',
    width: 100,
  },
  {
    title: '所属分店',
    dataIndex: 'storeName',
    width: 200,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right'
  }
]

// 模拟数据
const employeeList = ref([
  {
    id: 1,
    code: 'EMP001',
    name: '张三',
    phone: '13800138000',
    email: 'zhangsan@example.com',
    role: 'admin',
    storeId: 1,
    storeName: '总店',
    status: 'active',
    remark: ''
  },
  {
    id: 2,
    code: 'EMP002',
    name: '李四',
    phone: '13900139000',
    email: 'lisi@example.com',
    role: 'staff',
    storeId: 2,
    storeName: '分店1',
    status: 'active',
    remark: ''
  }
])

// 分店选项
const storeOptions = [
  { id: 1, name: '总店' },
  { id: 2, name: '分店1' },
  { id: 3, name: '分店2' }
]

// 分页配置
const pagination = {
  total: employeeList.value.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`,
}

// 弹窗相关
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)

// 表单数据
const formData = ref({
  code: '',
  name: '',
  phone: '',
  email: '',
  role: 'staff',
  storeId: undefined,
  status: 'active',
  remark: ''
})

// 表单验证规则
const rules = {
  code: [{ required: true, message: '请输入工号' }],
  name: [{ required: true, message: '请输入姓名' }],
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址' }
  ],
  role: [{ required: true, message: '请选择角色' }],
  storeId: [{ required: true, message: '请选择所属分店' }],
  status: [{ required: true, message: '请选择状态' }]
}

// 处理函数
const handleSearch = () => {
  message.success('搜索成功')
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: undefined,
    role: undefined
  }
  handleSearch()
}

const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    code: '',
    name: '',
    phone: '',
    email: '',
    role: 'staff',
    storeId: undefined,
    status: 'active',
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
  const action = newStatus === 'active' ? '复职' : '离职'
  record.status = newStatus
  message.success(`${action}成功`)
}

const handleResetPassword = (record) => {
  message.success(`已重置 ${record.name} 的密码`)
}
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