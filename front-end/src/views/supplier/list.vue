<template>
  <div class="supplier-container">
    <a-card>
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入供应商名称/编号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="状态"
            allow-clear
          >
            <a-select-option value="active">正常</a-select-option>
            <a-select-option value="inactive">停用</a-select-option>
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
            新增供应商
          </a-button>
        </a-space>
      </div>

      <!-- 供应商列表 -->
      <a-table
        :columns="columns"
        :data-source="supplierList"
        :pagination="pagination"
        :row-key="record => record.id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'active' ? 'success' : 'default'">
              {{ record.status === 'active' ? '正常' : '停用' }}
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
      :title="isEdit ? '编辑供应商' : '新增供应商'"
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
        <a-form-item label="供应商编号" name="code" required>
          <a-input 
            v-model:value="formData.code" 
            placeholder="请输入供应商编号"
            :disabled="isEdit"
          />
        </a-form-item>

        <a-form-item label="供应商名称" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入供应商名称" />
        </a-form-item>

        <a-form-item label="联系人" name="contact" required>
          <a-input v-model:value="formData.contact" placeholder="请输入联系人姓名" />
        </a-form-item>

        <a-form-item label="联系电话" name="phone" required>
          <a-input v-model:value="formData.phone" placeholder="请输入联系电话" />
        </a-form-item>

        <a-form-item label="电子邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入电子邮箱" />
        </a-form-item>

        <a-form-item label="详细地址" name="address" required>
          <a-textarea
            v-model:value="formData.address"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </a-form-item>

        <a-form-item label="经营范围" name="business">
          <a-textarea
            v-model:value="formData.business"
            :rows="2"
            placeholder="请输入经营范围"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </a-form-item>

        <a-form-item label="状态" name="status" required>
          <a-radio-group v-model:value="formData.status">
            <a-radio value="active">正常</a-radio>
            <a-radio value="inactive">停用</a-radio>
          </a-radio-group>
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
  status: undefined
})

// 表格列定义
const columns = [
  {
    title: '供应商编号',
    dataIndex: 'code',
    width: 120,
  },
  {
    title: '供应商名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '联系人',
    dataIndex: 'contact',
    width: 120,
  },
  {
    title: '联系电话',
    dataIndex: 'phone',
    width: 150,
  },
  {
    title: '地址',
    dataIndex: 'address',
    ellipsis: true,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right'
  }
]

// 模拟数据
const supplierList = ref([
  {
    id: 1,
    code: 'SP001',
    name: '广州医药有限公司',
    contact: '张三',
    phone: '13800138000',
    email: 'zhangsan@example.com',
    address: '广州市天河区珠江新城xxx路123号',
    business: '药品批发、医疗器械销售',
    status: 'active',
    remark: ''
  },
  {
    id: 2,
    code: 'SP002',
    name: '深圳医药集团',
    contact: '李四',
    phone: '13900139000',
    email: 'lisi@example.com',
    address: '深圳市南山区科技园xxx路456号',
    business: '药品生产、批发、零售',
    status: 'active',
    remark: ''
  }
])

// 分页配置
const pagination = {
  total: supplierList.value.length,
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
  contact: '',
  phone: '',
  email: '',
  address: '',
  business: '',
  status: 'active',
  remark: ''
})

// 表单验证规则
const rules = {
  code: [{ required: true, message: '请输入供应商编号' }],
  name: [{ required: true, message: '请输入供应商名称' }],
  contact: [{ required: true, message: '请输入联系人' }],
  phone: [
    { required: true, message: '请输入联系电话' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址' }
  ],
  address: [{ required: true, message: '请输入详细地址' }],
  status: [{ required: true, message: '请选择状态' }]
}

// 搜索
const handleSearch = () => {
  message.success('搜索成功')
}

// 重置
const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: undefined
  }
  handleSearch()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    code: '',
    name: '',
    contact: '',
    phone: '',
    email: '',
    address: '',
    business: '',
    status: 'active',
    remark: ''
  }
  modalVisible.value = true
}

// 编辑
const handleEdit = (record) => {
  isEdit.value = true
  formData.value = { ...record }
  modalVisible.value = true
}

// 提交表单
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

// 修改状态
const handleStatusChange = (record) => {
  const newStatus = record.status === 'active' ? 'inactive' : 'active'
  const action = newStatus === 'active' ? '启用' : '停用'
  record.status = newStatus
  message.success(`${action}成功`)
}
</script>

<style lang="less" scoped>
.supplier-container {
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