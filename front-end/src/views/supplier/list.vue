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
            @pressEnter="handleSearch"
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
        :loading="loading"
        :row-key="record => record.id"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
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
                v-if="userRole === 'ADMIN'"
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
        <a-form-item v-if="isEdit" label="供应商编号">
          <a-input v-model:value="formData.code" disabled />
        </a-form-item>

        <a-form-item label="供应商名称" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入供应商名称" />
        </a-form-item>

        <a-form-item label="联系人" name="contactPerson" required>
          <a-input v-model:value="formData.contactPerson" placeholder="请输入联系人姓名" />
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

        <a-form-item label="经营范围" name="businessScope">
          <a-textarea
            v-model:value="formData.businessScope"
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
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import axios from '@/utils/axios'

// 搜索表单
const searchForm = ref({
  keyword: '',
  status: undefined
})

// 列表数据
const supplierList = ref([])
const loading = ref(false)

// 权限
const userRole = ref(localStorage.getItem('userRole'))
// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`
})

// 获取供应商列表
const fetchSuppliers = async (params = {}) => {
  try {
    loading.value = true
    const { page = pagination.value.current - 1, size = pagination.value.pageSize } = params
    
    // 构建查询参数
    const queryParams = {
      page,
      size,
      sort: 'id',
      order: 'desc'
    }
    
    // 添加筛选条件
    if (searchForm.value.keyword?.trim()) {
      queryParams.keyword = searchForm.value.keyword.trim()
    }
    if (searchForm.value.status) {
      queryParams.status = searchForm.value.status
    }

    const response = await axios.get('/suppliers', { params: queryParams })
    
    if (response.code === 200) {
      supplierList.value = response.data.content
      pagination.value.total = response.data.totalElements
      pagination.value.current = page + 1
    }
  } catch (error) {
    console.error('获取供应商列表失败:', error)
    message.error('获取供应商列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.current = 1
  fetchSuppliers({
    page: 0,
    size: pagination.value.pageSize
  })
}

// 重置
const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: undefined
  }
  handleSearch()
}

// 处理表格变化
const handleTableChange = (pag, filters, sorter) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  fetchSuppliers({
    page: pag.current - 1,
    size: pag.pageSize
  })
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    name: '',
    contactPerson: '',
    phone: '',
    email: '',
    address: '',
    businessScope: '',
    status: 'active',
    remark: ''
  }
  modalVisible.value = true
}

// 编辑
const handleEdit = (record) => {
  isEdit.value = true
  formData.value = {
    id: record.id,
    name: record.name,
    contactPerson: record.contactPerson,
    phone: record.phone,
    email: record.email,
    address: record.address,
    businessScope: record.businessScope,
    status: record.status,
    remark: record.remark,
    code: record.code
  }
  modalVisible.value = true
}

// 提交表单
const handleModalSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true

    if (isEdit.value) {
      await axios.put(`/suppliers/${formData.value.id}`, formData.value)
      message.success('更新供应商成功')
    } else {
      await axios.post('/suppliers', formData.value)
      message.success('添加供应商成功')
    }

    modalVisible.value = false
    fetchSuppliers()
  } catch (error) {
    console.error('保存供应商失败:', error)
    message.error(error.response?.data?.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

// 修改状态
const handleStatusChange = async (record) => {
  try {
    const newStatus = record.status === 'active' ? 'inactive' : 'active'
    await axios.put(`/suppliers/${record.id}/status`, { status: newStatus })
    
    message.success(`${newStatus === 'active' ? '启用' : '停用'}成功`)
    fetchSuppliers()
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('操作失败')
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchSuppliers()
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
    dataIndex: 'contactPerson',
    width: 120,
  },
  {
    title: '联系电话',
    dataIndex: 'phone',
    width: 150,
  },
  {
    title: '邮箱地址',
    dataIndex: 'email',
    width: 200,
    customRender: ({text}) => text || '-'
  },
  {
    title: '地址',
    dataIndex: 'address',
    ellipsis: true,
  },
  {
    title: '经营范围',
    dataIndex: 'businessScope',
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

// 弹窗相关
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const isEdit = ref(false)

// 表单数据
const formData = ref({
  name: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  businessScope: '',
  status: 'active',
  remark: ''
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入供应商名称' }],
  contactPerson: [{ required: true, message: '请输入联系人' }],
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