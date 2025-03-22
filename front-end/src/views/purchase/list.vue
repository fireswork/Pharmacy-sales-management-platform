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
          <!-- 门店筛选，仅管理员可见 -->
          <a-select
            v-if="userRole === 'ADMIN'"
            v-model:value="searchForm.storeId"
            style="width: 180px"
            placeholder="选择门店"
            allow-clear
          >
            <a-select-option
              v-for="store in storeOptions"
              :key="store.id"
              :value="store.id"
            >
              {{ store.name }}
            </a-select-option>
          </a-select>
          <a-date-picker
            v-model:value="searchForm.date"
            style="width: 200px"
            placeholder="申请日期"
          />
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>

          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
          <a-button type="primary" @click="handleAdd" v-if="userRole === 'EMPLOYEE'">
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
        :row-key="(record) => record.id"
        bordered
        :loading="loading"
        :scroll="{ x: 1000 }"
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
                <a-button type="link" size="small" @click="handleApprove(record)" v-if="userRole === 'ADMIN'">通过</a-button>
                <a-button type="link" size="small" danger @click="handleReject(record)" v-if="userRole === 'ADMIN'">拒绝</a-button>
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
          <a-input v-model:value="formData.name" placeholder="请输入采购名称" :disabled="isView" />
        </a-form-item>

        <a-form-item label="供应商" name="supplierId" required>
          <a-select
            v-model:value="formData.supplierId"
            placeholder="请选择供应商"
            :disabled="isView"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="supplier in supplierOptions"
              :key="supplier.id"
              :value="supplier.id"
            >
              {{ supplier.name }}
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
            <a-select-option
              v-for="product in productOptions"
              :key="product.id"
              :value="product.id"
            >
              {{ product.name }}
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
import { ref, computed, onMounted, h } from 'vue'
import { message, Modal, Input } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import request from '@/utils/axios'

const userRole = ref(localStorage.getItem('userRole'))
console.log(userRole.value, 123)

// 是否是管理员
const isAdmin = ref(userRole.value === 'ADMIN')

// 搜索表单
const searchForm = ref({
  keyword: '',
  status: undefined,
  storeId: undefined,
  date: null
})

// 表格列定义
const columns = [
  {
    title: '采购编号',
    dataIndex: 'code',
    width: 120
  },
  {
    title: '采购名称',
    dataIndex: 'name',
    width: 200
  },
  {
    title: '供应商',
    dataIndex: 'supplierId',
    width: 200,
    customRender: ({ text }) => {
      const supplier = supplierOptions.value.find(s => s.id === text)
      return supplier ? supplier.name : '-'
    }
  },
  {
    title: '申请人',
    dataIndex: 'applicant',
    width: 120
  },
  // 添加门店列，只有管理员才会看到
  {
    title: '所属门店',
    dataIndex: 'storeName',
    width: 150,
  },
  {
    title: '申请时间',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '状态',
    key: 'status',
    width: 100
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
    width: 200
  },
  {
    title: '采购数量',
    dataIndex: 'quantity',
    width: 120
  },
  {
    title: '备注',
    dataIndex: 'remark'
  },
  {
    title: '操作',
    key: 'action',
    width: 80
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

// 列表数据
const purchaseList = ref([])
const loading = ref(false)

// 供应商、药品和门店选项
const supplierOptions = ref([])
const productOptions = ref([])
const storeOptions = ref([])

// 拒绝原因
const rejectReason = ref('')

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条记录`,
  onChange: (page, pageSize) => {
    pagination.value.current = page
    pagination.value.pageSize = pageSize
    fetchPurchaseList()
  }
})

// API 请求方法
const getSupplierList = () => {
  return request({
    url: '/suppliers',
    method: 'get'
  })
}

const getProductList = () => {
  return request({
    url: '/products',
    method: 'get'
  })
}

const getPurchaseList = (params) => {
  return request({
    url: '/purchases',
    method: 'get',
    params
  })
}

const createPurchase = (data) => {
  return request({
    url: '/purchases',
    method: 'post',
    data
  })
}

const approvePurchase = (id, comment) => {
  return request({
    url: `/purchases/${id}/approve`,
    method: 'patch',
    params: { comment }
  })
}

const rejectPurchase = (id, comment) => {
  return request({
    url: `/purchases/${id}/reject`,
    method: 'patch',
    params: { comment }
  })
}

// 获取门店列表
const fetchStoreList = async () => {
  try {
    const res = await request({
      url: '/store',
      method: 'get'
    })
    storeOptions.value = res.data.content || []
  } catch (error) {
    message.error('获取门店列表失败')
  }
}

// 获取采购列表
const fetchPurchaseList = async () => {
  loading.value = true
  try {
    const params = {
      keyword: searchForm.value.keyword,
      status: searchForm.value.status,
      storeId: searchForm.value.storeId,
      date: searchForm.value.date ? searchForm.value.date.format('YYYY-MM-DD') : undefined,
      page: pagination.value.current - 1, // 后端页码从 0 开始
      size: pagination.value.pageSize // 使用 size 而不是 pageSize
    }
    const res = await getPurchaseList(params)
    purchaseList.value = res.data.content
    pagination.value.total = res.data.totalElements // 使用 totalElements 而不是 total
  } catch (error) {
    message.error('获取采购列表失败')
  } finally {
    loading.value = false
  }
}

// 弹窗相关
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const isView = ref(false)
const modalTitle = computed(() => (isView.value ? '查看采购' : '新增采购'))

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

// 修改计算属性
const filteredProductOptions = computed(() => {
  return productOptions.value
})

// 搜索药品
const filterOption = (input, option) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// 处理函数
const handleSearch = () => {
  pagination.value.current = 1
  fetchPurchaseList()
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: undefined,
    storeId: undefined,
    date: null
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
  userRole.value === 'ADMIN' ? modalVisible.value = false : 
  formRef.value.validate().then(async () => {
    submitLoading.value = true
    try {
      // 构建符合后端期望的数据格式
      const submitData = {
        name: formData.value.name,
        supplierId: formData.value.supplierId,
        reason: formData.value.reason,
        products: formData.value.products
      }

      await createPurchase(submitData)
      message.success('提交成功')
      modalVisible.value = false
      fetchPurchaseList()
    } catch (error) {
      message.error('提交失败: ' + (error.response?.data?.message || error.message))
    } finally {
      submitLoading.value = false
    }
  })
}

const handleAddProduct = () => {
  productForm.value = {
    productId: undefined,
    quantity: 1,
    remark: ''
  }
  productModalVisible.value = true
}

const handleRemoveProduct = (record) => {
  formData.value.products = formData.value.products.filter((item) => item.id !== record.id)
}

const handleProductSelect = (value) => {
  const selected = productOptions.value.find((item) => item.id === value)
  if (selected) {
    productForm.value = {
      ...productForm.value,
      productId: selected.id,
      name: selected.name
    }
  }
}

const handleProductModalSubmit = () => {
  productFormRef.value.validate().then(() => {
    productSubmitLoading.value = true
    const selectedProduct = productOptions.value.find(
      (item) => item.id === productForm.value.productId
    )

    if (selectedProduct) {
      const product = {
        productId: productForm.value.productId,
        name: selectedProduct.name,
        quantity: productForm.value.quantity,
        remark: productForm.value.remark
      }

      formData.value.products.push(product)
      productModalVisible.value = false
      message.success('添加药品成功')
    }
    productSubmitLoading.value = false
  })
}

// 审核相关
const handleApprove = async (record) => {
  try {
    await request.put(`/purchases/${record.id}`, {
      status: 'approved',
      comment: ''
    });
    message.success('已通过');
    fetchPurchaseList();
  } catch (error) {
    message.error('操作失败');
  }
};

const handleReject = async (record) => {
  try {
    const comment = await new Promise((resolve) => {
      Modal.confirm({
        title: '拒绝采购申请',
        content: h(
          'div',
          {},
          [
            h('p', '请输入拒绝原因：'),
            h(Input.TextArea, {
              rows: 4,
              onChange: (e) => {
                rejectReason.value = e.target.value;
              },
            }),
          ]
        ),
        onOk: () => {
          resolve(rejectReason.value);
        },
        onCancel: () => {
          resolve(null);
        },
        okText: '确定',
        cancelText: '取消',
        width: 480,
      });
    });

    if (comment) {
      await request.put(`/purchases/${record.id}`, {
        status: 'rejected',
        comment
      });
      message.success('已拒绝');
      fetchPurchaseList();
    }
  } catch (error) {
    message.error('操作失败');
  }
};

// 获取供应商列表
const fetchSupplierList = async () => {
  try {
    const res = await getSupplierList()
    supplierOptions.value = res.data.content || []
  } catch (error) {
    message.error('获取供应商列表失败')
  }
}

// 获取药品列表
const fetchProductList = async () => {
  try {
    const res = await getProductList()
    productOptions.value = res.data.content || []
  } catch (error) {
    message.error('获取药品列表失败')
  }
}

// 初始化数据
onMounted(() => {
  fetchSupplierList()
  fetchProductList()
  fetchStoreList()
  fetchPurchaseList()
})
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
