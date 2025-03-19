<template>
  <div class="purchase-container">
    <a-card>
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入采购名称/编号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.status"
            style="width: 160px"
            placeholder="审核状态"
            allow-clear
          >
            <a-select-option value="pending">待审核</a-select-option>
            <a-select-option value="approved">已通过</a-select-option>
            <a-select-option value="rejected">已拒绝</a-select-option>
          </a-select>
          <a-range-picker
            v-model:value="searchForm.dateRange"
            style="width: 240px"
          />
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
            新增采购
          </a-button>
        </a-space>
      </div>

      <!-- 采购列表 -->
      <a-table
        :columns="columns"
        :data-source="purchaseList"
        :pagination="pagination"
        :row-key="record => record.id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <template v-if="isAdmin && record.status === 'pending'">
                <a-button type="link" size="small" @click="handleApprove(record)">通过</a-button>
                <a-button type="link" size="small" danger @click="handleReject(record)">拒绝</a-button>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/查看弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
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
        <a-form-item label="采购名称" name="name" required>
          <a-input 
            v-model:value="formData.name" 
            placeholder="请输入采购名称"
            :disabled="isView" 
          />
        </a-form-item>

        <a-form-item label="供应商" name="supplierId" required>
          <a-select
            v-model:value="formData.supplierId"
            placeholder="请选择供应商"
            :disabled="isView"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="item in supplierOptions" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="采购药品" name="products" required>
          <div class="products-table">
            <a-table
              :columns="productColumns"
              :data-source="formData.products"
              :pagination="false"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action' && !isView">
                  <a-button type="link" danger size="small" @click="handleRemoveProduct(record)">
                    删除
                  </a-button>
                </template>
              </template>
            </a-table>
            <div class="add-product" v-if="!isView">
              <a-button type="dashed" block @click="handleAddProduct">
                <PlusOutlined /> 添加药品
              </a-button>
            </div>
          </div>
        </a-form-item>

        <a-form-item label="采购原因" name="reason" required>
          <a-textarea
            v-model:value="formData.reason"
            :rows="4"
            placeholder="请输入采购原因"
            :disabled="isView"
          />
        </a-form-item>

        <template v-if="isView">
          <a-form-item label="审核状态">
            <a-tag :color="getStatusColor(formData.status)">
              {{ getStatusText(formData.status) }}
            </a-tag>
          </a-form-item>

          <a-form-item label="审核意见" v-if="formData.comment">
            <span>{{ formData.comment }}</span>
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 添加药品弹窗 -->
    <a-modal
      v-model:visible="productModalVisible"
      title="添加药品"
      @ok="handleProductModalSubmit"
      :confirmLoading="productSubmitLoading"
      width="600px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="productFormRef"
        :model="productForm"
        :rules="productRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="选择药品" name="productId" required>
          <a-select
            v-model:value="productForm.productId"
            placeholder="请选择药品"
            @change="handleProductSelect"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="item in filteredProductOptions" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="采购数量" name="quantity" required>
          <a-input-number
            v-model:value="productForm.quantity"
            :min="1"
            style="width: 100%"
            placeholder="请输入采购数量"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-input v-model:value="productForm.remark" placeholder="请输入备注信息" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'

// 是否是管理员
const isAdmin = ref(true) // 这里应该根据实际登录用户角色判断

// 搜索表单
const searchForm = ref({
  keyword: '',
  status: undefined,
  dateRange: []
})

// 表格列定义
const columns = [
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
    title: '供应商',
    dataIndex: 'supplierName',
    width: 200,
  },
  {
    title: '申请人',
    dataIndex: 'applicant',
    width: 120,
  },
  {
    title: '申请时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right'
  }
]

// 药品表格列定义
const productColumns = [
  {
    title: '药品名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '采购数量',
    dataIndex: 'quantity',
    width: 120,
  },
  {
    title: '备注',
    dataIndex: 'remark',
  },
  {
    title: '操作',
    key: 'action',
    width: 80,
  }
]

// 状态相关
const getStatusColor = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'error'
  }
  return map[status]
}

const getStatusText = (status) => {
  const map = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return map[status]
}

// 模拟数据
const purchaseList = ref([
  {
    id: 1,
    code: 'PO001',
    name: '常用药品采购',
    supplierName: '广州医药有限公司',
    applicant: '张三',
    createTime: '2024-03-20 10:00:00',
    status: 'pending'
  },
  {
    id: 2,
    code: 'PO002',
    name: '季度药品补货',
    supplierName: '深圳医药集团',
    applicant: '李四',
    createTime: '2024-03-19 14:30:00',
    status: 'approved'
  }
])

// 分页配置
const pagination = {
  total: purchaseList.value.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`,
}

// 弹窗相关
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const isView = ref(false)
const modalTitle = computed(() => isView.value ? '查看采购' : '新增采购')

// 表单数据
const formData = ref({
  name: '',
  supplierId: undefined,
  products: [],
  reason: '',
  status: 'pending',
  comment: ''
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入采购名称' }],
  supplierId: [{ required: true, message: '请选择供应商' }],
  products: [{ required: true, message: '请添加采购药品', type: 'array', min: 1 }],
  reason: [{ required: true, message: '请输入采购原因' }]
}

// 药品选择相关
const productModalVisible = ref(false)
const productSubmitLoading = ref(false)
const productFormRef = ref()
const productForm = ref({
  productId: undefined,
  quantity: 1,
  remark: ''
})

const productRules = {
  productId: [{ required: true, message: '请选择药品' }],
  quantity: [{ required: true, message: '请输入采购数量' }]
}

// 模拟药品数据
const productOptions = [
  { id: 1, name: '布洛芬缓释胶囊' },
  { id: 2, name: '感冒灵颗粒' },
  { id: 3, name: '板蓝根颗粒' }
]

// 模拟供应商数据
const supplierOptions = [
  { id: 1, name: '广州医药有限公司' },
  { id: 2, name: '深圳医药集团' },
  { id: 3, name: '上海医药股份有限公司' }
]

// 模拟供应商药品关联数据
const supplierProducts = [
  { supplierId: 1, productIds: [1, 2] },
  { supplierId: 2, productIds: [2, 3] },
  { supplierId: 3, productIds: [1, 3] }
]

// 根据选择的供应商筛选可选药品
const filteredProductOptions = computed(() => {
  if (!formData.value.supplierId) return []
  const supplierProduct = supplierProducts.find(item => item.supplierId === formData.value.supplierId)
  if (!supplierProduct) return []
  return productOptions.filter(product => supplierProduct.productIds.includes(product.id))
})

// 搜索药品
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
    status: undefined,
    dateRange: []
  }
  handleSearch()
}

const handleAdd = () => {
  isView.value = false
  formData.value = {
    name: '',
    supplierId: undefined,
    products: [],
    reason: '',
    status: 'pending',
    comment: ''
  }
  modalVisible.value = true
}

const handleView = (record) => {
  isView.value = true
  formData.value = { ...record }
  modalVisible.value = true
}

const handleModalSubmit = () => {
  formRef.value.validate().then(() => {
    submitLoading.value = true
    setTimeout(() => {
      message.success('提交成功')
      modalVisible.value = false
      submitLoading.value = false
      handleSearch()
    }, 1000)
  })
}

const handleAddProduct = () => {
  if (!formData.value.supplierId) {
    message.warning('请先选择供应商')
    return
  }
  productForm.value = {
    productId: undefined,
    quantity: 1,
    remark: ''
  }
  productModalVisible.value = true
}

const handleRemoveProduct = (record) => {
  formData.value.products = formData.value.products.filter(item => item.id !== record.id)
}

const handleProductSelect = (value) => {
  const selected = productOptions.find(item => item.id === value)
  productForm.value.name = selected.name
}

const handleProductModalSubmit = () => {
  productFormRef.value.validate().then(() => {
    productSubmitLoading.value = true
    setTimeout(() => {
      const selectedProduct = productOptions.find(item => item.id === productForm.value.productId)
      const product = {
        id: Date.now(),
        name: selectedProduct.name,
        quantity: productForm.value.quantity,
        remark: productForm.value.remark
      }
      formData.value.products.push(product)
      productModalVisible.value = false
      productSubmitLoading.value = false
    }, 500)
  })
}

// 审核相关
const handleApprove = (record) => {
  record.status = 'approved'
  message.success('审核通过')
}

const handleReject = (record) => {
  record.status = 'rejected'
  message.success('已拒绝')
}
</script>

<style lang="less" scoped>
.purchase-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
  }

  .products-table {
    .add-product {
      margin-top: 16px;
    }
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}
</style> 